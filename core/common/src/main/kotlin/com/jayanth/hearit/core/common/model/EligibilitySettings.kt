package com.jayanth.hearit.core.common.model

/** Independent screen condition for announcement eligibility (master prompt §6). */
enum class ScreenCondition {
    /** Announce only while the screen is on. */
    ON,

    /** Announce only while the screen is off. Default. */
    OFF,

    /** Announce regardless of screen state. */
    BOTH,
}

/** Independent lock condition for announcement eligibility (master prompt §6). */
enum class LockCondition {
    /** Announce only while the device is locked. Default. */
    LOCKED,

    /** Announce only while the device is unlocked. */
    UNLOCKED,

    /** Announce regardless of lock state. */
    BOTH,
}

/**
 * Eligibility settings for one app after global defaults and per-app overrides have been
 * merged. Screen and lock are separate conditions that are combined with AND logic by
 * `EligibilityRules` (master prompt §6, global default: screen **off** AND phone **locked**).
 *
 * Single conditions ([respectDnd], [queueDuringCall]) inherit field-by-field like screen and
 * lock. Text length, keyword filtering, privacy and audio output are whole groups: an
 * override replaces a group or inherits it (null), never mixes partially with global values
 * inside a group — this keeps the inheritance rule uniform and auditable (master prompt §5/§6).
 */
data class EligibilitySettings(
    val screenCondition: ScreenCondition = ScreenCondition.OFF,
    val lockCondition: LockCondition = LockCondition.LOCKED,
    val respectDnd: Boolean = true,
    val queueDuringCall: Boolean = false,
    val textLength: TextLengthSettings = TextLengthSettings(),
    val keywordFilter: KeywordFilterSettings = KeywordFilterSettings(),
    val privacy: PrivacySettings = PrivacySettings(),
    val audioOutput: AudioOutputSettings = AudioOutputSettings(),
    /** What the player does with other apps' audio focus (P08, per-app inheritable). */
    val audioFocus: AudioFocusPolicy = AudioFocusPolicy.DUCK,
) {
    /**
     * Merges this app's override onto the global defaults. A null field in [override]
     * means "inherit the global value" (master prompt §5: app settings must clearly show
     * when a value inherits the global value). Each condition and each settings group
     * inherits independently.
     */
    fun resolveWith(override: AppEligibilityOverride?): EligibilitySettings {
        if (override == null) return this
        return copy(
            screenCondition = override.screenCondition ?: screenCondition,
            lockCondition = override.lockCondition ?: lockCondition,
            respectDnd = override.respectDnd ?: respectDnd,
            queueDuringCall = override.queueDuringCall ?: queueDuringCall,
            textLength = override.textLength ?: textLength,
            keywordFilter = override.keywordFilter ?: keywordFilter,
            privacy = override.privacy ?: privacy,
            audioOutput = override.audioOutput ?: audioOutput,
            audioFocus = override.audioFocus ?: audioFocus,
        )
    }
}

/**
 * Per-app override of [EligibilitySettings]. Absence of a value (null) means the app
 * inherits the global value for that condition or group; overrides never partially mutate
 * another condition or group.
 */
data class AppEligibilityOverride(
    val screenCondition: ScreenCondition? = null,
    val lockCondition: LockCondition? = null,
    val respectDnd: Boolean? = null,
    val queueDuringCall: Boolean? = null,
    val textLength: TextLengthSettings? = null,
    val keywordFilter: KeywordFilterSettings? = null,
    val privacy: PrivacySettings? = null,
    val audioOutput: AudioOutputSettings? = null,
    val audioFocus: AudioFocusPolicy? = null,
) {
    /** True when this app inherits every global value — shown in the UI as "Inherits global". */
    val inheritsEveryValue: Boolean
        get() = screenCondition == null &&
            lockCondition == null &&
            respectDnd == null &&
            queueDuringCall == null &&
            textLength == null &&
            keywordFilter == null &&
            privacy == null &&
            audioOutput == null &&
            audioFocus == null
}
