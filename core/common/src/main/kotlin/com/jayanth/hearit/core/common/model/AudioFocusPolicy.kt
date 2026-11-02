package com.jayanth.hearit.core.common.model

/**
 * What the announcement service does with other apps' audio focus while HearIt speaks
 * (master prompt §6, P08 scope). Per-app inheritable like the other single conditions.
 *
 * This is a policy the player applies, never a rule: eligibility is unchanged.
 */
enum class AudioFocusPolicy {

    /**
     * Request transient focus and duck others (music lowers, resumes afterwards).
     * Default — least disruptive for an app that speaks short announcements.
     */
    DUCK,

    /** Request transient focus and pause other playback until the announcement ends. */
    PAUSE,

    /** Do not request audio focus at all; speak over whatever is playing. */
    NONE,
}
