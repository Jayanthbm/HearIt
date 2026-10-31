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
 * Later phases extend this type with text-length, keyword, privacy, DND/call and
 * audio-output fields; the merge behaviour in [resolveWith] stays the same.
 */
data class EligibilitySettings(
    val screenCondition: ScreenCondition = ScreenCondition.OFF,
    val lockCondition: LockCondition = LockCondition.LOCKED,
) {
    /**
     * Merges this app's override onto the global defaults. A null field in [override]
     * means "inherit the global value" (master prompt §5: app settings must clearly show
     * when a value inherits the global value). Each condition inherits independently.
     */
    fun resolveWith(override: AppEligibilityOverride?): EligibilitySettings {
        if (override == null) return this
        return copy(
            screenCondition = override.screenCondition ?: screenCondition,
            lockCondition = override.lockCondition ?: lockCondition,
        )
    }
}

/**
 * Per-app override of [EligibilitySettings]. Absence of a value (null) means the app
 * inherits the global value for that condition; overrides never partially mutate another
 * condition.
 */
data class AppEligibilityOverride(
    val screenCondition: ScreenCondition? = null,
    val lockCondition: LockCondition? = null,
) {
    /** True when this app inherits every global value — shown in the UI as "Inherits global". */
    val inheritsEveryValue: Boolean
        get() = screenCondition == null && lockCondition == null
}
