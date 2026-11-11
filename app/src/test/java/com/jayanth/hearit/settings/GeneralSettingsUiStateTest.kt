package com.jayanth.hearit.settings

import com.jayanth.hearit.core.common.model.AudioFocusPolicy
import com.jayanth.hearit.core.common.model.OverflowPolicy
import com.jayanth.hearit.core.common.model.QueueSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class GeneralSettingsUiStateTest {

    @Test
    fun defaultState_matchesMasterPromptDefaults() {
        val state = GeneralSettingsUiState()

        assertEquals(AudioFocusPolicy.DUCK, state.audioFocus)
        assertEquals(5_000L, state.queue.groupingWindowMillis)
        assertEquals(0L, state.queue.delayBeforeAnnouncementMillis)
        assertEquals(3, state.queue.maxMessagesPerAnnouncement)
        assertEquals(0L, state.queue.minGapBetweenAnnouncementsMillis)
        assertEquals(30L * 60_000L, state.queue.expiryMillis)
        assertEquals(50, state.queue.globalMaxSize)
        assertEquals(10, state.queue.perAppMaxSize)
        assertEquals(OverflowPolicy.DISCARD_OLDEST, state.queue.overflowPolicy)
        assertFalse(state.queue.repeatNotificationUpdates)
        assertEquals(true, state.isServiceNotificationVisible)
        assertFalse(state.security.biometricLockEnabled)
        assertFalse(state.security.secureScreenEnabled)
    }

    @Test
    fun customState_isPreserved() {
        val customQueue = QueueSettings(
            groupingWindowMillis = 10_000L,
            delayBeforeAnnouncementMillis = 2_000L,
            maxMessagesPerAnnouncement = 5,
            minGapBetweenAnnouncementsMillis = 1_000L,
            expiryMillis = 15L * 60_000L,
            globalMaxSize = 100,
            perAppMaxSize = 20,
            overflowPolicy = OverflowPolicy.KEEP_HIGHEST_PRIORITY,
            repeatNotificationUpdates = true,
        )
        val state = GeneralSettingsUiState(
            queue = customQueue,
            audioFocus = AudioFocusPolicy.PAUSE,
            isServiceNotificationVisible = false,
        )

        assertEquals(AudioFocusPolicy.PAUSE, state.audioFocus)
        assertEquals(10_000L, state.queue.groupingWindowMillis)
        assertEquals(2_000L, state.queue.delayBeforeAnnouncementMillis)
        assertEquals(5, state.queue.maxMessagesPerAnnouncement)
        assertEquals(1_000L, state.queue.minGapBetweenAnnouncementsMillis)
        assertEquals(15L * 60_000L, state.queue.expiryMillis)
        assertEquals(100, state.queue.globalMaxSize)
        assertEquals(20, state.queue.perAppMaxSize)
        assertEquals(OverflowPolicy.KEEP_HIGHEST_PRIORITY, state.queue.overflowPolicy)
        assertEquals(true, state.queue.repeatNotificationUpdates)
        assertEquals(false, state.isServiceNotificationVisible)
        assertEquals(
            com.jayanth.hearit.core.common.model.ReactionFilterMode.CONDENSE,
            state.reactionFilterMode,
        )
        assertEquals(
            com.jayanth.hearit.core.common.model.DigitGroupingStyle.CHUNKS_OF_3,
            state.digitGroupingStyle,
        )
    }
}
