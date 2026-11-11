package com.jayanth.hearit.core.setup

/**
 * Why a setup step (or Start) is not ready yet (master prompt §4).
 *
 * Typed rather than a string so the UI owns the wording (no hardcoded user-facing strings,
 * global standards), TalkBack can announce a precise reason, and the gating logic stays
 * testable. One blocker per unmet prerequisite; several can apply to the review step.
 */
sealed interface SetupBlocker {

    /** Notification access is not granted: Hear It cannot see notifications. */
    data object NotificationAccessMissing : SetupBlocker

    /** Android 12+ asks for `BLUETOOTH_CONNECT` and the user has not granted it yet. */
    data object BluetoothPermissionMissing : SetupBlocker

    /** No TTS engine initialised on this device. */
    data object TtsEngineUnavailable : SetupBlocker

    /** An engine exists but has no voice data for the device language. */
    data object TtsLanguageMissing : SetupBlocker

    /** No app is enabled; every app starts disabled (master prompt §5). */
    data object NoEnabledApps : SetupBlocker

    /** Every output category is disallowed, so nothing could ever be spoken. */
    data object NoAllowedOutput : SetupBlocker
}
