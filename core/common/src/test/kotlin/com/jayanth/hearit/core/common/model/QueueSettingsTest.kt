package com.jayanth.hearit.core.common.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validation and inheritance rules for [QueueSettings] (master prompt §8, ADR-019).
 * Not locally verified — written under the cloud-agent restriction (no build/test run).
 */
class QueueSettingsTest {

    @Test
    fun defaults_matchSuggestedMasterPromptValues() {
        val settings = QueueSettings()

        assertEquals(50, settings.globalMaxSize)
        assertEquals(10, settings.perAppMaxSize)
        assertEquals(30L * 60_000L, settings.expiryMillis)
        assertEquals(OverflowPolicy.DISCARD_OLDEST, settings.overflowPolicy)
    }

    @Test
    fun defaults_neutralSchedulingValues() {
        val settings = QueueSettings()

        assertEquals(5_000L, settings.groupingWindowMillis)
        assertEquals(0L, settings.delayBeforeAnnouncementMillis)
        assertEquals(3, settings.maxMessagesPerAnnouncement)
        assertEquals(0L, settings.minGapBetweenAnnouncementsMillis)
        assertFalse(settings.repeatNotificationUpdates)
    }

    @Test
    fun negativeGroupingWindow_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            QueueSettings(groupingWindowMillis = -1L)
        }
    }

    @Test
    fun nonPositiveMaxMessagesPerAnnouncement_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            QueueSettings(maxMessagesPerAnnouncement = 0)
        }
    }

    @Test
    fun nonPositiveExpiry_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            QueueSettings(expiryMillis = 0L)
        }
    }

    @Test
    fun nonPositiveSizeLimits_areRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            QueueSettings(globalMaxSize = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            QueueSettings(perAppMaxSize = 0)
        }
    }

    @Test
    fun resolveWith_nullOverride_inheritsEverything() {
        val settings = QueueSettings()

        assertSame(settings, settings.resolveWith(null))
    }

    @Test
    fun resolveWith_windowOverride_appliesOnlyThatField() {
        val settings = QueueSettings()

        val resolved = settings.resolveWith(AppQueueOverride(groupingWindowMillis = 250L))

        assertEquals(250L, resolved.groupingWindowMillis)
        assertEquals(settings.delayBeforeAnnouncementMillis, resolved.delayBeforeAnnouncementMillis)
        assertEquals(settings.maxMessagesPerAnnouncement, resolved.maxMessagesPerAnnouncement)
        assertEquals(settings.minGapBetweenAnnouncementsMillis, resolved.minGapBetweenAnnouncementsMillis)
        assertEquals(settings.expiryMillis, resolved.expiryMillis)
        assertEquals(settings.globalMaxSize, resolved.globalMaxSize)
        assertEquals(settings.perAppMaxSize, resolved.perAppMaxSize)
        assertEquals(settings.overflowPolicy, resolved.overflowPolicy)
        assertEquals(settings.repeatNotificationUpdates, resolved.repeatNotificationUpdates)
    }

    @Test
    fun override_inheritsEveryValue_isTrueOnlyWhenWindowNull() {
        assertTrue(AppQueueOverride().inheritsEveryValue)
        assertTrue(AppQueueOverride(groupingWindowMillis = null).inheritsEveryValue)
        assertFalse(AppQueueOverride(groupingWindowMillis = 1_000L).inheritsEveryValue)
    }

    @Test
    fun override_negativeWindow_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            AppQueueOverride(groupingWindowMillis = -1L)
        }
    }
}
