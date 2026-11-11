package com.jayanth.hearit.service

import com.jayanth.hearit.core.common.model.AudioFocusPolicy

/**
 * What the player asks the platform for while it speaks (master prompt §6).
 *
 * Kept separate from the policy enum so the Android mapping is testable on the JVM and the
 * settings model stays free of playback concerns.
 */
enum class AudioFocusGain {
    /** Transient focus with ducking: other playback lowers and resumes by itself. */
    DUCK_OTHERS,

    /** Transient focus without ducking: other playback pauses until the announcement ends. */
    PAUSE_OTHERS,

    /** Ask for nothing; HearIt speaks over whatever is playing. */
    NONE,
}

/** The pure mapping from stored policy to requested gain. */
object AudioFocusPolicyMapping {

    fun gainFor(policy: AudioFocusPolicy): AudioFocusGain = when (policy) {
        AudioFocusPolicy.DUCK -> AudioFocusGain.DUCK_OTHERS
        AudioFocusPolicy.PAUSE -> AudioFocusGain.PAUSE_OTHERS
        AudioFocusPolicy.NONE -> AudioFocusGain.NONE
    }
}
