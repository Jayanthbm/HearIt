package com.jayanth.hearit.service

import com.jayanth.hearit.core.common.model.AudioFocusPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

/** The stored audio-focus policy must reach the platform as the matching request (L6). */
class AudioFocusPolicyMappingTest {

    @Test
    fun duckPolicy_requestsDuckingFocus() {
        assertEquals(AudioFocusGain.DUCK_OTHERS, AudioFocusPolicyMapping.gainFor(AudioFocusPolicy.DUCK))
    }

    @Test
    fun pausePolicy_requestsFocusWithoutDucking() {
        assertEquals(AudioFocusGain.PAUSE_OTHERS, AudioFocusPolicyMapping.gainFor(AudioFocusPolicy.PAUSE))
    }

    @Test
    fun nonePolicy_requestsNothing() {
        assertEquals(AudioFocusGain.NONE, AudioFocusPolicyMapping.gainFor(AudioFocusPolicy.NONE))
    }

    @Test
    fun everyPolicy_isMapped() {
        AudioFocusPolicy.entries.forEach { policy ->
            AudioFocusPolicyMapping.gainFor(policy) // exhaustive when: throws if a case is missed
        }
    }
}
