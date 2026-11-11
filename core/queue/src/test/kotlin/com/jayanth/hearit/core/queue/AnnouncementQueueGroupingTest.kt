package com.jayanth.hearit.core.queue

import com.jayanth.hearit.core.common.model.AppPriority
import com.jayanth.hearit.core.common.model.AppQueueOverride
import com.jayanth.hearit.core.common.model.QueueSettings
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Same-app grouping: window measurement, cap, priority selection and the per-app window
 * override (master prompt §8, ADR-019). Not locally verified — written under the
 * cloud-agent restriction (no build/test run).
 */
class AnnouncementQueueGroupingTest {

    @Test
    fun poll_sameAppWithinWindow_groupsIntoOneAnnouncement() {
        val clock = FakeClock()
        val queue = AnnouncementQueue(clock, QueueSettings(groupingWindowMillis = 5_000L))

        queue.enqueue(testSnapshot("a"), AppPriority.NORMAL)
        clock.advance(1_000L)
        queue.enqueue(testSnapshot("b"), AppPriority.NORMAL)
        clock.advance(1_000L)
        queue.enqueue(testSnapshot("c"), AppPriority.NORMAL)

        val announcement = queue.pollReady().announcement!!

        assertEquals(listOf("a", "b", "c"), announcement.items.map { it.key })
        assertEquals("com.whatsapp", announcement.packageName)
        assertEquals("WhatsApp", announcement.appName)
    }

    @Test
    fun poll_groupItems_keepArrivalOrderNotAlphabetical() {
        val queue = AnnouncementQueue(FakeClock(), QueueSettings(groupingWindowMillis = 5_000L))

        queue.enqueue(testSnapshot("z"), AppPriority.NORMAL)
        queue.enqueue(testSnapshot("a"), AppPriority.NORMAL)
        queue.enqueue(testSnapshot("m"), AppPriority.NORMAL)

        val announcement = queue.pollReady().announcement!!

        assertEquals(listOf("z", "a", "m"), announcement.items.map { it.key })
    }

    @Test
    fun poll_maxMessagesPerAnnouncement_capsGroup_andLeftoversWaitForNext() {
        val queue = AnnouncementQueue(
            FakeClock(),
            QueueSettings(groupingWindowMillis = 10_000L, maxMessagesPerAnnouncement = 2),
        )
        queue.enqueue(testSnapshot("a"), AppPriority.NORMAL)
        queue.enqueue(testSnapshot("b"), AppPriority.NORMAL)
        queue.enqueue(testSnapshot("c"), AppPriority.NORMAL)
        queue.enqueue(testSnapshot("d"), AppPriority.NORMAL)

        val first = queue.pollReady().announcement!!
        assertEquals(listOf("a", "b"), first.items.map { it.key })
        assertEquals(4, queue.pendingCount)

        queue.markStarted(first.id)
        queue.markCompleted(first.id)

        val second = queue.pollReady().announcement!!
        assertEquals(listOf("c", "d"), second.items.map { it.key })
        queue.markStarted(second.id)
        assertEquals(0, queue.pendingCount)
    }

    @Test
    fun poll_windowBoundary_isInclusive() {
        val clock = FakeClock()
        val queue = AnnouncementQueue(clock, QueueSettings(groupingWindowMillis = 1_000L))

        queue.enqueue(testSnapshot("a"), AppPriority.NORMAL) // enqueued at t=0
        clock.advance(1_000L) // exactly the window end — inclusive (ADR-019)
        queue.enqueue(testSnapshot("b"), AppPriority.NORMAL)

        val announcement = queue.pollReady().announcement!!

        assertEquals(listOf("a", "b"), announcement.items.map { it.key })
    }

    @Test
    fun poll_itemJustOutsideWindow_startsNextGroup() {
        val clock = FakeClock()
        val queue = AnnouncementQueue(clock, QueueSettings(groupingWindowMillis = 1_000L))

        queue.enqueue(testSnapshot("a"), AppPriority.NORMAL)
        clock.advance(1_001L)
        queue.enqueue(testSnapshot("b"), AppPriority.NORMAL)

        val first = queue.pollReady().announcement!!
        assertEquals(listOf("a"), first.items.map { it.key })
        queue.markStarted(first.id)
        queue.markCompleted(first.id)

        val second = queue.pollReady().announcement!!
        assertEquals(listOf("b"), second.items.map { it.key })
    }

    @Test
    fun poll_differentApps_neverGroup() {
        val queue = AnnouncementQueue(FakeClock(), QueueSettings(groupingWindowMillis = 5_000L))
        queue.enqueue(testSnapshot("wa", packageName = "com.whatsapp"), AppPriority.NORMAL)
        queue.enqueue(
            testSnapshot("tg", packageName = "com.telegram", appName = "Telegram"),
            AppPriority.NORMAL,
        )

        val first = queue.pollReady().announcement!!
        assertEquals("com.whatsapp", first.packageName)
        assertEquals(listOf("wa"), first.items.map { it.key })
        queue.markStarted(first.id)
        queue.markCompleted(first.id)

        val second = queue.pollReady().announcement!!
        assertEquals("com.telegram", second.packageName)
        assertEquals(listOf("tg"), second.items.map { it.key })
    }

    @Test
    fun poll_companionNotYetReady_isExcludedUntilDelayElapses() {
        val clock = FakeClock()
        val queue = AnnouncementQueue(
            clock,
            QueueSettings(groupingWindowMillis = 5_000L, delayBeforeAnnouncementMillis = 1_000L),
        )

        queue.enqueue(testSnapshot("a"), AppPriority.NORMAL) // t=0, ready at t=1000
        clock.advance(600L)
        queue.enqueue(testSnapshot("b"), AppPriority.NORMAL) // t=600, ready at t=1600

        clock.advance(400L) // t=1000: only "a" is ready
        val early = queue.pollReady().announcement!!
        assertEquals(listOf("a"), early.items.map { it.key })

        clock.advance(600L) // t=1600: "b" is ready and inside the window of "a"
        val later = queue.pollReady().announcement!!
        assertEquals(listOf("a", "b"), later.items.map { it.key })
    }

    @Test
    fun poll_perAppGroupingWindowOverride_isApplied() {
        val clock = FakeClock()
        val queue = AnnouncementQueue(
            clock,
            QueueSettings(groupingWindowMillis = 5_000L),
            mapOf("com.whatsapp" to AppQueueOverride(groupingWindowMillis = 100L)),
        )

        queue.enqueue(testSnapshot("a"), AppPriority.NORMAL)
        clock.advance(500L) // beyond the 100 ms override window, within the 5 s default
        queue.enqueue(testSnapshot("b"), AppPriority.NORMAL)

        val first = queue.pollReady().announcement!!
        assertEquals(listOf("a"), first.items.map { it.key })
        queue.markStarted(first.id)
        queue.markCompleted(first.id)

        val second = queue.pollReady().announcement!!
        assertEquals(listOf("b"), second.items.map { it.key })
    }

    @Test
    fun poll_selectionTakesHighestPriorityApp_andOnlyItsCompanions() {
        val queue = AnnouncementQueue(FakeClock(), QueueSettings(groupingWindowMillis = 5_000L))

        queue.enqueue(
            testSnapshot("wa1", packageName = "com.whatsapp"),
            AppPriority.NORMAL,
        )
        queue.enqueue(
            testSnapshot("tg1", packageName = "com.telegram", appName = "Telegram"),
            AppPriority.HIGH,
        )
        queue.enqueue(
            testSnapshot("tg2", packageName = "com.telegram", appName = "Telegram"),
            AppPriority.HIGH,
        )

        val first = queue.pollReady().announcement!!

        assertEquals("com.telegram", first.packageName)
        assertEquals(listOf("tg1", "tg2"), first.items.map { it.key })
        assertEquals(AppPriority.HIGH, first.priority)
    }
}
