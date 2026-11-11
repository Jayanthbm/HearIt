package com.jayanth.hearit.core.queue

import com.jayanth.hearit.core.common.model.AppPriority
import com.jayanth.hearit.core.common.model.HistoryStatus
import com.jayanth.hearit.core.common.model.QueueSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Priority ordering, FIFO, delay, minimum gap and never-interrupt rules of
 * [AnnouncementQueue] (master prompt §8, ADR-005/ADR-019). Items that must be announced
 * separately are either from different packages or enqueued outside the grouping window.
 * Not locally verified — written under the cloud-agent restriction (no build/test run).
 */
class AnnouncementQueueOrderingTest {

    @Test
    fun enqueue_returnsQueuedEventWithQueuedStatusAndNoReason() {
        val queue = AnnouncementQueue(FakeClock())

        val events = queue.enqueue(testSnapshot("k1"), AppPriority.NORMAL)

        assertEquals(1, events.size)
        val event = events.single()
        assertTrue(event is QueueEvent.Queued)
        assertEquals(HistoryStatus.QUEUED, event.historyStatus)
        assertNull(event.reason)
        assertEquals(1, queue.pendingCount)
    }

    @Test
    fun poll_withEmptyQueue_returnsNoAnnouncement() {
        val result = AnnouncementQueue(FakeClock()).pollReady()

        assertNull(result.announcement)
        assertTrue(result.events.isEmpty())
    }

    @Test
    fun poll_equalPriorityItems_comeOutInArrivalOrder() {
        val clock = FakeClock()
        val queue = AnnouncementQueue(clock)

        // Same app, but each arrival is outside the previous one's grouping window, so
        // every announcement holds exactly one item and FIFO order is observable.
        queue.enqueue(testSnapshot("a"), AppPriority.NORMAL)
        clock.advance(6_000L)
        queue.enqueue(testSnapshot("b"), AppPriority.NORMAL)
        clock.advance(6_000L)
        queue.enqueue(testSnapshot("c"), AppPriority.NORMAL)

        val first = queue.pollReady().announcement!!
        assertEquals(listOf("a"), first.items.map { it.key })
        queue.markStarted(first.id)
        queue.markCompleted(first.id)

        val second = queue.pollReady().announcement!!
        assertEquals(listOf("b"), second.items.map { it.key })
        queue.markStarted(second.id)
        queue.markCompleted(second.id)

        val third = queue.pollReady().announcement!!
        assertEquals(listOf("c"), third.items.map { it.key })
    }

    @Test
    fun poll_higherPriority_jumpsAheadOfEarlierItems() {
        val queue = AnnouncementQueue(FakeClock())
        // Different packages: the earlier Normal item must not ride along as a companion.
        queue.enqueue(testSnapshot("older", packageName = "com.whatsapp"), AppPriority.NORMAL)
        queue.enqueue(
            testSnapshot("newer", packageName = "com.telegram", appName = "Telegram"),
            AppPriority.HIGH,
        )

        val announcement = queue.pollReady().announcement!!

        assertEquals(listOf("newer"), announcement.items.map { it.key })
        assertEquals(AppPriority.HIGH, announcement.priority)
    }

    @Test
    fun poll_priorityOrder_isCriticalThenHighThenNormalThenLow() {
        val queue = AnnouncementQueue(FakeClock())
        // One package per tier so grouping never merges two tiers into one announcement.
        queue.enqueue(testSnapshot("normal", packageName = "com.whatsapp"), AppPriority.NORMAL)
        queue.enqueue(testSnapshot("critical", packageName = "com.telegram", appName = "Telegram"), AppPriority.CRITICAL)
        queue.enqueue(testSnapshot("high", packageName = "com.signal", appName = "Signal"), AppPriority.HIGH)
        queue.enqueue(testSnapshot("low", packageName = "com.slack", appName = "Slack"), AppPriority.LOW)

        val spoken = mutableListOf<String>()
        repeat(4) {
            val announcement = queue.pollReady().announcement!!
            spoken += announcement.items.map { it.key }
            queue.markStarted(announcement.id)
            queue.markCompleted(announcement.id)
        }

        assertEquals(listOf("critical", "high", "normal", "low"), spoken)
    }

    @Test
    fun poll_beforeDelayElapses_returnsNoAnnouncement() {
        val clock = FakeClock()
        val queue = AnnouncementQueue(clock, QueueSettings(delayBeforeAnnouncementMillis = 1_000L))
        queue.enqueue(testSnapshot("k1"), AppPriority.NORMAL)

        clock.advance(999L)
        assertNull(queue.pollReady().announcement)
    }

    @Test
    fun poll_afterDelayElapses_returnsAnnouncement() {
        val clock = FakeClock()
        val queue = AnnouncementQueue(clock, QueueSettings(delayBeforeAnnouncementMillis = 1_000L))
        queue.enqueue(testSnapshot("k1"), AppPriority.NORMAL)

        clock.advance(999L)
        assertNull(queue.pollReady().announcement)

        clock.advance(1L)
        assertEquals("k1", queue.pollReady().announcement!!.items.single().key)
    }

    @Test
    fun poll_beforeMinGapElapsed_returnsNoAnnouncement() {
        val clock = FakeClock()
        val queue = AnnouncementQueue(clock, QueueSettings(minGapBetweenAnnouncementsMillis = 500L))
        queue.enqueue(testSnapshot("a"), AppPriority.NORMAL)
        clock.advance(6_000L) // "b" outside "a"'s grouping window → separate announcements
        queue.enqueue(testSnapshot("b"), AppPriority.NORMAL)

        val first = queue.pollReady().announcement!!
        assertEquals(listOf("a"), first.items.map { it.key })
        queue.markStarted(first.id)
        queue.markCompleted(first.id)

        clock.advance(499L)
        assertNull(queue.pollReady().announcement)
    }

    @Test
    fun poll_afterMinGapElapsed_returnsNextAnnouncement() {
        val clock = FakeClock()
        val queue = AnnouncementQueue(clock, QueueSettings(minGapBetweenAnnouncementsMillis = 500L))
        queue.enqueue(testSnapshot("a"), AppPriority.NORMAL)
        clock.advance(6_000L)
        queue.enqueue(testSnapshot("b"), AppPriority.NORMAL)

        val first = queue.pollReady().announcement!!
        queue.markStarted(first.id)
        queue.markCompleted(first.id)

        clock.advance(500L)
        val second = queue.pollReady().announcement!!
        assertEquals(listOf("b"), second.items.map { it.key })
    }

    @Test
    fun poll_whileSpeaking_returnsNoAnnouncement_neverInterrupts() {
        val queue = AnnouncementQueue(FakeClock())
        // Different packages: the pending Normal item must not be swallowed into the
        // speaking High announcement, so the post-completion poll has something left.
        queue.enqueue(testSnapshot("a", packageName = "com.whatsapp"), AppPriority.NORMAL)
        queue.enqueue(
            testSnapshot("b", packageName = "com.telegram", appName = "Telegram"),
            AppPriority.HIGH,
        )

        val first = queue.pollReady().announcement!!
        assertEquals(listOf("b"), first.items.map { it.key })
        queue.markStarted(first.id)

        val during = queue.pollReady()
        assertNull(during.announcement)
        assertTrue(during.events.isEmpty())

        queue.markCompleted(first.id)
        val after = queue.pollReady().announcement!!
        assertEquals(listOf("a"), after.items.map { it.key })
    }

    @Test
    fun markStarted_withStaleIdFromOlderPoll_isRejected() {
        val queue = AnnouncementQueue(FakeClock())
        queue.enqueue(testSnapshot("a"), AppPriority.NORMAL)
        val staleId = queue.pollReady().announcement!!.id

        queue.enqueue(testSnapshot("b"), AppPriority.CRITICAL)
        val latestId = queue.pollReady().announcement!!.id

        assertTrue(latestId != staleId)
        assertThrows(IllegalArgumentException::class.java) {
            queue.markStarted(staleId)
        }
    }

    @Test
    fun markStarted_twiceWithoutCompletion_isRejected() {
        val queue = AnnouncementQueue(FakeClock())
        queue.enqueue(testSnapshot("a"), AppPriority.NORMAL)
        val id = queue.pollReady().announcement!!.id
        queue.markStarted(id)

        assertThrows(IllegalArgumentException::class.java) {
            queue.markStarted(id)
        }
    }

    @Test
    fun markCompleted_withNonSpeakingId_isRejected() {
        val queue = AnnouncementQueue(FakeClock())
        queue.enqueue(testSnapshot("a"), AppPriority.NORMAL)
        val id = queue.pollReady().announcement!!.id
        queue.markStarted(id)

        assertThrows(IllegalArgumentException::class.java) {
            queue.markCompleted(id + 1)
        }
    }
}
