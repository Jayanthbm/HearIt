package com.jayanth.hearit.core.queue

import com.jayanth.hearit.core.common.model.AppPriority
import com.jayanth.hearit.core.common.model.NavigationPriorityDetector
import com.jayanth.hearit.core.common.model.NotificationSnapshot
import com.jayanth.hearit.core.common.model.QueueSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationPriorityAndDeferTest {

    @Test
    fun `requeueSpeaking restores active announcement back to pending and prioritizes navigation`() {
        val clock = FakeClock(1_000L)
        val queue = AnnouncementQueue(
            clock = clock,
            settings = QueueSettings(
                delayBeforeAnnouncementMillis = 0L,
                minGapBetweenAnnouncementsMillis = 2_000L,
            ),
        )

        val whatsapp = testSnapshot(key = "wa|1", packageName = "com.whatsapp", appName = "WhatsApp", sender = "Rahul", messageText = "Dinner at 8?")
        queue.enqueue(whatsapp, AppPriority.NORMAL)

        val firstPoll = queue.pollReady()
        val firstBatch = firstPoll.announcement
        assertNotNull(firstBatch)
        queue.markStarted(firstBatch!!.id)
        assertEquals(0, queue.pendingCount)

        // Incoming Maps navigation notification arrives mid-WhatsApp speech
        val mapsTurn = NotificationSnapshot(
            key = "maps|nav|1",
            packageName = "com.google.android.apps.maps",
            appName = "Maps",
            sender = null,
            messageText = "Turn right onto MG Road",
            category = "navigation",
            postTimeMillis = clock.nowMillis(),
        )
        assertTrue(NavigationPriorityDetector.isNavigationNotification(mapsTurn))
        queue.enqueue(mapsTurn, AppPriority.CRITICAL)

        // Defer active WhatsApp announcement for navigation priority
        val requeued = queue.requeueSpeaking(firstBatch.id)
        assertEquals(1, requeued)
        assertEquals(2, queue.pendingCount)

        // Immediate poll skips minGap and picks Maps navigation first (CRITICAL beats NORMAL)
        val navBatch = queue.pollReady().announcement
        assertNotNull(navBatch)
        assertEquals("com.google.android.apps.maps", navBatch!!.packageName)
        queue.markStarted(navBatch.id)
        queue.markCompleted(navBatch.id)

        // After gap elapses, deferred WhatsApp message is announced afterward
        clock.advance(2_100L)
        val resumedBatch = queue.pollReady().announcement
        assertNotNull(resumedBatch)
        assertEquals("com.whatsapp", resumedBatch!!.packageName)
        assertEquals("wa|1", resumedBatch.items.first().key)
    }

    @Test
    fun `pollReadyDigest dispatches critical navigation alert immediately without waiting for digest window`() {
        val clock = FakeClock(5_000L)
        val queue = AnnouncementQueue(
            clock = clock,
            settings = QueueSettings(
                delayBeforeAnnouncementMillis = 0L,
                minGapBetweenAnnouncementsMillis = 0L,
            ),
        )
        val mapsTurn = NotificationSnapshot(
            key = "maps|nav|2",
            packageName = "com.google.android.apps.maps",
            appName = "Maps",
            messageText = "In 200 meters, take the exit",
            category = "navigation",
            postTimeMillis = 5_000L,
        )
        queue.enqueue(mapsTurn, AppPriority.CRITICAL)

        // Even at t=5000ms (< 2500ms digest window), CRITICAL navigation alert fires immediately
        val result = queue.pollReadyDigest(multiAppEnabled = true, digestWindowMillis = 2_500L)
        assertNotNull(result.batch)
        assertEquals("com.google.android.apps.maps", result.batch!!.primaryPackageName)
        assertFalse(result.batch!!.isMultiApp)
    }
}
