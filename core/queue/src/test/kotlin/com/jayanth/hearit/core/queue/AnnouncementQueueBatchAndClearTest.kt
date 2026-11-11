package com.jayanth.hearit.core.queue

import com.jayanth.hearit.core.common.model.AppPriority
import com.jayanth.hearit.core.common.model.NotificationSnapshot
import com.jayanth.hearit.core.common.model.QueueSettings
import com.jayanth.hearit.core.common.model.SkipReason
import com.jayanth.hearit.core.common.time.Clock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnnouncementQueueBatchAndClearTest {

    private class TestClock(var now: Long = 0L) : Clock {
        override fun nowMillis(): Long = now
    }

    private fun snap(
        key: String,
        pkg: String = "com.whatsapp",
        appName: String = "WhatsApp",
    ) = NotificationSnapshot(
        key = key,
        packageName = pkg,
        appName = appName,
        sender = "Alex",
        messageText = "Msg $key",
        postTimeMillis = 1000L,
    )

    @Test
    fun clearPending_removesAllPendingAndHeldAndEmitsManuallyClearedEvents() {
        val clock = TestClock()
        val queue = AnnouncementQueue(clock = clock)
        queue.enqueue(snap("p1"), AppPriority.NORMAL)
        queue.hold(snap("h1"), AppPriority.HIGH, SkipReason.CALL_ACTIVE)

        assertEquals(1, queue.pendingCount)
        assertEquals(1, queue.heldCount)

        val events = queue.clearPending()

        assertEquals(0, queue.pendingCount)
        assertEquals(0, queue.heldCount)
        assertEquals(2, events.size)
        assertTrue(events.all { it is QueueEvent.ManuallyCleared && it.reason == SkipReason.MANUALLY_CLEARED })
    }

    @Test
    fun pollReadyDigest_collectsMultiAppNotificationsAfterWindowAndStartsCleanly() {
        val clock = TestClock(now = 1_000L)
        val queue = AnnouncementQueue(clock = clock, settings = QueueSettings(maxMessagesPerAnnouncement = 4))
        queue.enqueue(snap("wa1", "com.whatsapp", "WhatsApp"), AppPriority.NORMAL)

        // Still inside 2500ms digest window with only 1 item -> holds briefly
        clock.now = 2_000L
        assertNull(queue.pollReadyDigest(multiAppEnabled = true, digestWindowMillis = 2_500L).batch)

        // Second notification from another app arrives
        queue.enqueue(snap("sms1", "com.google.android.apps.messaging", "Messages"), AppPriority.NORMAL)

        // Digest window expires -> emits combined multi-app batch
        clock.now = 3_600L
        val poll = queue.pollReadyDigest(multiAppEnabled = true, digestWindowMillis = 2_500L)
        val batch = poll.batch
        assertNotNull(batch)
        assertTrue(batch!!.isMultiApp)
        assertEquals(2, batch.items.size)

        queue.markStarted(batch.id)
        val done = queue.markCompleted(batch.id)
        assertEquals(2, done.filterIsInstance<QueueEvent.Announced>().size)
    }
}
