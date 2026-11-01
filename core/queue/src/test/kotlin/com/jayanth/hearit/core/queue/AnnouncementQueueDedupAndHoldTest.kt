package com.jayanth.hearit.core.queue

import com.jayanth.hearit.core.common.model.AppPriority
import com.jayanth.hearit.core.common.model.HistoryStatus
import com.jayanth.hearit.core.common.model.QueueSettings
import com.jayanth.hearit.core.common.model.SkipReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Dedup of notification updates (master prompt §10) and call-hold storage/release for
 * `EligibilityDecision.Queue(reason)` (ADR-006/ADR-004, ADR-019). Not locally verified —
 * written under the cloud-agent restriction (no build/test run).
 */
class AnnouncementQueueDedupAndHoldTest {

    @Test
    fun enqueue_updateOfRecentlyAnnouncedKey_deduplicates() {
        val clock = FakeClock()
        val queue = AnnouncementQueue(clock)

        queue.enqueue(testSnapshot("k1"), AppPriority.NORMAL)
        val first = queue.pollReady().announcement!!
        queue.markStarted(first.id)
        queue.markCompleted(first.id)

        clock.advance(60_000L)
        val events = queue.enqueue(testSnapshot("k1", isUpdate = true), AppPriority.NORMAL)

        assertEquals(1, events.size)
        val deduplicated = events.single() as QueueEvent.Deduplicated
        assertEquals(HistoryStatus.SKIPPED, deduplicated.historyStatus)
        assertEquals(SkipReason.DUPLICATE_UPDATE, deduplicated.reason)
        assertEquals(0, queue.pendingCount)
    }

    @Test
    fun enqueue_updateOfAnnouncedKey_announcesAgainWhenConfigured() {
        val queue = AnnouncementQueue(
            FakeClock(),
            QueueSettings(repeatNotificationUpdates = true),
        )

        queue.enqueue(testSnapshot("k1"), AppPriority.NORMAL)
        val first = queue.pollReady().announcement!!
        queue.markStarted(first.id)
        queue.markCompleted(first.id)

        val events = queue.enqueue(testSnapshot("k1", isUpdate = true), AppPriority.NORMAL)

        assertEquals(1, events.size)
        assertTrue(events.single() is QueueEvent.Queued)
        assertEquals(1, queue.pendingCount)
    }

    @Test
    fun enqueue_updateWhilePending_replacesInPlace() {
        val queue = AnnouncementQueue(FakeClock())

        queue.enqueue(testSnapshot("k1", messageText = "old"), AppPriority.NORMAL)
        val events = queue.enqueue(
            testSnapshot("k1", messageText = "new", isUpdate = true),
            AppPriority.NORMAL,
        )

        assertEquals(1, events.size)
        assertTrue(events.single() is QueueEvent.Replaced)
        assertEquals(1, queue.pendingCount)

        val announcement = queue.pollReady().announcement!!
        assertEquals("new", announcement.items.single().messageText)
    }

    @Test
    fun enqueue_updateAfterDedupWindow_announcesAgain() {
        val clock = FakeClock()
        val queue = AnnouncementQueue(clock, QueueSettings(expiryMillis = 1_000L))

        queue.enqueue(testSnapshot("k1"), AppPriority.NORMAL)
        val first = queue.pollReady().announcement!!
        queue.markStarted(first.id)
        queue.markCompleted(first.id)

        clock.advance(1_500L) // past the window: dedup memory is forgotten
        val events = queue.enqueue(testSnapshot("k1", isUpdate = true), AppPriority.NORMAL)

        assertEquals(1, events.size)
        assertTrue(events.single() is QueueEvent.Queued)
        assertEquals(1, queue.pendingCount)
    }

    @Test
    fun enqueue_updateNeverAnnounced_isNotDeduplicated() {
        val queue = AnnouncementQueue(FakeClock())

        val events = queue.enqueue(testSnapshot("k1", isUpdate = true), AppPriority.NORMAL)

        assertEquals(1, events.size)
        assertTrue(events.single() is QueueEvent.Queued)
        assertEquals(1, queue.pendingCount)
    }

    @Test
    fun markStarted_recordsAnnouncement_forLaterDedup() {
        val clock = FakeClock()
        val queue = AnnouncementQueue(clock)

        queue.enqueue(testSnapshot("k1"), AppPriority.NORMAL)
        val first = queue.pollReady().announcement!!
        queue.markStarted(first.id)

        clock.advance(100L)
        val events = queue.enqueue(testSnapshot("k1", isUpdate = true), AppPriority.NORMAL)

        assertTrue(events.single() is QueueEvent.Deduplicated)
        assertEquals(0, queue.pendingCount)
    }

    @Test
    fun hold_emitsQueuedEvent_withCallReasonAndQueuedStatus() {
        val queue = AnnouncementQueue(FakeClock())

        val events = queue.hold(testSnapshot("k1"), AppPriority.NORMAL, SkipReason.CALL_ACTIVE)

        assertEquals(1, events.size)
        val queued = events.single() as QueueEvent.Queued
        assertEquals(SkipReason.CALL_ACTIVE, queued.reason)
        assertEquals(HistoryStatus.QUEUED, queued.historyStatus)
        assertEquals(1, queue.heldCount)
        assertEquals(0, queue.pendingCount)
    }

    @Test
    fun hold_isNotPollable_andDoesNotCountTowardSizeLimits() {
        val queue = AnnouncementQueue(FakeClock(), QueueSettings(globalMaxSize = 1))

        val first = queue.hold(testSnapshot("x"), AppPriority.NORMAL, SkipReason.CALL_ACTIVE)
        val second = queue.hold(testSnapshot("y"), AppPriority.NORMAL, SkipReason.CALL_ACTIVE)

        assertTrue(first.none { it is QueueEvent.OverflowDiscarded })
        assertTrue(second.none { it is QueueEvent.OverflowDiscarded })
        assertEquals(0, queue.pendingCount)
        assertEquals(2, queue.heldCount)
        assertNull(queue.pollReady().announcement)
    }

    @Test
    fun hold_updateOfSameHeldKey_replacesInPlace() {
        val queue = AnnouncementQueue(FakeClock())

        queue.hold(testSnapshot("k1", messageText = "old"), AppPriority.NORMAL, SkipReason.CALL_ACTIVE)
        val events = queue.hold(
            testSnapshot("k1", messageText = "new", isUpdate = true),
            AppPriority.NORMAL,
            SkipReason.CALL_ACTIVE,
        )

        assertEquals(1, events.size)
        assertTrue(events.single() is QueueEvent.Replaced)
        assertEquals(1, queue.heldCount)
    }

    @Test
    fun releaseBlocked_returnsHeldItemsInHoldOrder_andClears() {
        val clock = FakeClock()
        val queue = AnnouncementQueue(clock)

        queue.hold(testSnapshot("k1"), AppPriority.NORMAL, SkipReason.CALL_ACTIVE)
        clock.advance(100L)
        queue.hold(testSnapshot("k2"), AppPriority.NORMAL, SkipReason.CALL_ACTIVE)

        val result = queue.releaseBlocked()

        assertTrue(result.events.isEmpty())
        assertEquals(listOf("k1", "k2"), result.blocked.map { it.snapshot.key })
        assertEquals(0, queue.heldCount)
    }

    @Test
    fun releaseBlocked_expiredHeldItems_areSweptNotReturned() {
        val clock = FakeClock()
        val queue = AnnouncementQueue(clock, QueueSettings(expiryMillis = 1_000L))

        queue.hold(testSnapshot("k1"), AppPriority.NORMAL, SkipReason.CALL_ACTIVE)
        clock.advance(1_000L)

        val result = queue.releaseBlocked()

        assertTrue(result.blocked.isEmpty())
        assertEquals(1, result.events.size)
        val expired = result.events.single() as QueueEvent.Expired
        assertEquals("k1", expired.snapshot.key)
        assertEquals(SkipReason.EXPIRED, expired.reason)
        assertEquals(0, queue.heldCount)
    }
}
