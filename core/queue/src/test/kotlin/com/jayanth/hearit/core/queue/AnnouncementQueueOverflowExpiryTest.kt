package com.jayanth.hearit.core.queue

import com.jayanth.hearit.core.common.model.AppPriority
import com.jayanth.hearit.core.common.model.HistoryStatus
import com.jayanth.hearit.core.common.model.OverflowPolicy
import com.jayanth.hearit.core.common.model.QueueSettings
import com.jayanth.hearit.core.common.model.SkipReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Global/per-app size limits, the three overflow policies and expiry (master prompt §8,
 * ADR-005/ADR-019). Discarded and expired items must keep history status and reason.
 * Not locally verified — written under the cloud-agent restriction (no build/test run).
 */
class AnnouncementQueueOverflowExpiryTest {

    @Test
    fun enqueue_overGlobalMaxSize_discardsOldest_withOverflowHistory() {
        val queue = AnnouncementQueue(FakeClock(), QueueSettings(globalMaxSize = 2))

        queue.enqueue(testSnapshot("a"), AppPriority.NORMAL)
        queue.enqueue(testSnapshot("b"), AppPriority.NORMAL)
        val events = queue.enqueue(testSnapshot("c"), AppPriority.NORMAL)

        assertEquals(2, events.size)
        assertTrue(events[0] is QueueEvent.Queued)
        val discarded = events[1] as QueueEvent.OverflowDiscarded
        assertEquals("a", discarded.snapshot.key)
        assertEquals(HistoryStatus.SKIPPED, discarded.historyStatus)
        assertEquals(SkipReason.OVERFLOW, discarded.reason)
        assertEquals(2, queue.pendingCount)
    }

    @Test
    fun enqueue_overPerAppMaxSize_evictsOnlyThatAppsItems() {
        val queue = AnnouncementQueue(FakeClock(), QueueSettings(perAppMaxSize = 1))

        queue.enqueue(testSnapshot("wa1", packageName = "com.whatsapp"), AppPriority.NORMAL)
        val second = queue.enqueue(testSnapshot("wa2", packageName = "com.whatsapp"), AppPriority.NORMAL)
        val other = queue.enqueue(testSnapshot("tg1", packageName = "com.telegram", appName = "Telegram"), AppPriority.NORMAL)

        assertEquals(2, second.size)
        assertEquals("wa1", (second[1] as QueueEvent.OverflowDiscarded).snapshot.key)
        assertEquals(1, other.size)
        assertTrue(other.single() is QueueEvent.Queued)
        assertEquals(2, queue.pendingCount)

        val first = queue.pollReady().announcement!!
        assertEquals("com.whatsapp", first.packageName)
        queue.markStarted(first.id)
        queue.markCompleted(first.id)

        val next = queue.pollReady().announcement!!
        assertEquals("com.telegram", next.packageName)
    }

    @Test
    fun enqueue_discardNewest_refusesTheIncomingItem() {
        val queue = AnnouncementQueue(
            FakeClock(),
            QueueSettings(globalMaxSize = 2, overflowPolicy = OverflowPolicy.DISCARD_NEWEST),
        )

        queue.enqueue(testSnapshot("a"), AppPriority.NORMAL)
        queue.enqueue(testSnapshot("b"), AppPriority.NORMAL)
        val events = queue.enqueue(testSnapshot("c"), AppPriority.NORMAL)

        assertEquals(2, events.size)
        assertEquals("c", (events[1] as QueueEvent.OverflowDiscarded).snapshot.key)
        assertEquals(2, queue.pendingCount)

        val announcement = queue.pollReady().announcement!!
        assertEquals(listOf("a", "b"), announcement.items.map { it.key })
    }

    @Test
    fun enqueue_keepHighestPriority_evictsTheLowestPriorityItem() {
        val queue = AnnouncementQueue(
            FakeClock(),
            QueueSettings(globalMaxSize = 2, overflowPolicy = OverflowPolicy.KEEP_HIGHEST_PRIORITY),
        )

        queue.enqueue(testSnapshot("normal"), AppPriority.NORMAL)
        queue.enqueue(testSnapshot("low"), AppPriority.LOW)
        val events = queue.enqueue(testSnapshot("high"), AppPriority.HIGH)

        assertEquals("low", (events[1] as QueueEvent.OverflowDiscarded).snapshot.key)
        assertEquals(2, queue.pendingCount)

        val announcement = queue.pollReady().announcement!!
        assertEquals(AppPriority.HIGH, announcement.priority)
        assertTrue(announcement.items.none { it.key == "low" })
    }

    @Test
    fun enqueue_keepHighestPriority_tiesEvictTheNewestAmongEquals() {
        val queue = AnnouncementQueue(
            FakeClock(),
            QueueSettings(globalMaxSize = 2, overflowPolicy = OverflowPolicy.KEEP_HIGHEST_PRIORITY),
        )

        queue.enqueue(testSnapshot("a"), AppPriority.NORMAL)
        queue.enqueue(testSnapshot("b"), AppPriority.NORMAL)
        val events = queue.enqueue(testSnapshot("c"), AppPriority.NORMAL)

        assertEquals("c", (events[1] as QueueEvent.OverflowDiscarded).snapshot.key)
        assertEquals(2, queue.pendingCount)
    }

    @Test
    fun enqueue_exactlyAtLimit_doesNotDiscard() {
        val queue = AnnouncementQueue(FakeClock(), QueueSettings(globalMaxSize = 2))

        val first = queue.enqueue(testSnapshot("a"), AppPriority.NORMAL)
        val second = queue.enqueue(testSnapshot("b"), AppPriority.NORMAL)

        assertEquals(1, first.size)
        assertEquals(1, second.size)
        assertTrue(first.none { it is QueueEvent.OverflowDiscarded })
        assertTrue(second.none { it is QueueEvent.OverflowDiscarded })
    }

    @Test
    fun enqueue_atExpiryBoundary_emitsExpiredEvent_withHistoryStatusAndReason() {
        val clock = FakeClock()
        val queue = AnnouncementQueue(clock, QueueSettings(expiryMillis = 1_000L))

        queue.enqueue(testSnapshot("a"), AppPriority.NORMAL)
        clock.advance(1_000L) // exactly at the limit — expired (>=, ADR-019)
        val events = queue.enqueue(testSnapshot("b"), AppPriority.NORMAL)

        assertEquals(2, events.size)
        val expired = events[0] as QueueEvent.Expired
        assertEquals("a", expired.snapshot.key)
        assertEquals(HistoryStatus.SKIPPED, expired.historyStatus)
        assertEquals(SkipReason.EXPIRED, expired.reason)
        assertTrue(events[1] is QueueEvent.Queued)
        assertEquals(1, queue.pendingCount)
    }

    @Test
    fun poll_afterExpiry_returnsNoAnnouncement_andReportsExpiry() {
        val clock = FakeClock()
        val queue = AnnouncementQueue(clock, QueueSettings(expiryMillis = 1_000L))
        queue.enqueue(testSnapshot("a"), AppPriority.NORMAL)

        clock.advance(2_000L)
        val result = queue.pollReady()

        assertNull(result.announcement)
        assertEquals(1, result.events.size)
        assertTrue(result.events.single() is QueueEvent.Expired)
        assertEquals(0, queue.pendingCount)
    }

    @Test
    fun updateSettings_shrinkingGlobalLimit_enforcesImmediately() {
        val queue = AnnouncementQueue(FakeClock())
        queue.enqueue(testSnapshot("a"), AppPriority.NORMAL)
        queue.enqueue(testSnapshot("b"), AppPriority.NORMAL)
        queue.enqueue(testSnapshot("c"), AppPriority.NORMAL)
        assertEquals(3, queue.pendingCount)

        val events = queue.updateSettings(QueueSettings(globalMaxSize = 1))

        assertEquals(2, events.size)
        assertEquals("a", (events[0] as QueueEvent.OverflowDiscarded).snapshot.key)
        assertEquals("b", (events[1] as QueueEvent.OverflowDiscarded).snapshot.key)
        assertEquals(1, queue.pendingCount)
    }
}
