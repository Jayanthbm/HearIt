package com.jayanth.hearit.core.setup

/**
 * The nine first-launch steps, in the order the master prompt (§4) lists them.
 *
 * The order is the contract: the setup UI walks [SetupFlow.visibleSteps], never the raw
 * enum, because [SetupStep.BLUETOOTH_PERMISSION] is hidden on Android versions that do not
 * need the runtime permission. [isSkippable] is the only place that decides whether a step
 * may be passed without meeting its requirement (master prompt §4: "Optional steps may be
 * skipped where safe"), and the setup UI reads it — never a hardcoded list in Compose code.
 */
enum class SetupStep {
    /** Welcome and explanation of what the app does. Never skipped. */
    WELCOME,

    /** Notification access, required for the app to function at all. */
    NOTIFICATION_ACCESS,

    /** Bluetooth/nearby-device permission, requested only from API 31 upward. */
    BLUETOOTH_PERMISSION,

    /** TTS availability plus a language/voice sanity check. */
    TEXT_TO_SPEECH,

    /** Background-execution and battery guidance. Informational; may be skipped. */
    BATTERY_GUIDANCE,

    /** App selection; starts with every app disabled (master prompt §5). */
    APP_SELECTION,

    /** Audio-device selection; defaults are wired + Bluetooth allowed, speaker off. */
    AUDIO_DEVICES,

    /** Test announcement through the current settings; never touches history or the queue. */
    TEST_ANNOUNCEMENT,

    /** Review and the explicit Start action. Never skipped. */
    REVIEW,
    ;

    /**
     * True when the user may continue without meeting this step's prerequisite.
     *
     * Skippable steps are the ones whose default state is already safe: no Bluetooth
     * permission still leaves announcements working, battery guidance is informational,
     * the default output set is already selected and a test announcement is optional.
     */
    val isSkippable: Boolean
        get() = this == BLUETOOTH_PERMISSION ||
            this == BATTERY_GUIDANCE ||
            this == AUDIO_DEVICES ||
            this == TEST_ANNOUNCEMENT

    /** True when Start cannot be offered before this step is satisfied. */
    val isRequired: Boolean
        get() = !isSkippable

    /** Index in the documented order; used only for stable presentation/analytics-free logs. */
    val order: Int
        get() = ordinal

    companion object {
        /** The documented nine steps, in order. */
        val ordered: List<SetupStep> = entries.toList()
    }
}
