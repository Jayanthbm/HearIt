package com.jayanth.hearit.core.setup

/**
 * Everything the setup flow must know about the device at one moment (master prompt §4).
 *
 * Captured by the platform probes in `:app` and passed in as an immutable value, so the
 * gating rules are pure and JVM-testable. Every field is a **fact**, not a decision:
 * decisions live in [SetupFlow]. Defaults describe a fresh install with nothing granted.
 *
 * @property notificationAccessGranted True when Hear It is in the system's enabled
 *   notification-listener list.
 * @property bluetoothPermissionRequired True on API 31+ (`BLUETOOTH_CONNECT` runtime
 *   permission); the Bluetooth step is hidden entirely when false.
 * @property bluetoothPermissionGranted Result of the `BLUETOOTH_CONNECT` check; ignored
 *   when [bluetoothPermissionRequired] is false.
 * @property ttsEngineAvailable True when an installed engine initialised successfully.
 * @property ttsLanguageAvailable True when that engine has usable data for the device
 *   locale (master prompt §4 step 4; no translation fallback).
 * @property enabledAppCount Number of apps the user enabled; Start needs at least one
 *   (master prompt §4: "Start is available only after at least one app is enabled").
 * @property allowedOutputCount How many output categories are allowed (wired, Bluetooth,
 *   phone speaker). Defaults are wired + Bluetooth, so a fresh install is 2; zero means
 *   nothing could ever be announced and Start is blocked.
 */
data class SetupPrerequisites(
    val notificationAccessGranted: Boolean = false,
    val bluetoothPermissionRequired: Boolean = false,
    val bluetoothPermissionGranted: Boolean = false,
    val ttsEngineAvailable: Boolean = false,
    val ttsLanguageAvailable: Boolean = false,
    val enabledAppCount: Int = 0,
    val allowedOutputCount: Int = 0,
) {
    init {
        require(enabledAppCount >= 0) { "SetupPrerequisites.enabledAppCount must not be negative" }
        require(allowedOutputCount >= 0) { "SetupPrerequisites.allowedOutputCount must not be negative" }
    }

    /** True only when the engine exists **and** can speak the device language. */
    val ttsReady: Boolean
        get() = ttsEngineAvailable && ttsLanguageAvailable

    /** True when this Android version asks for the Bluetooth runtime permission. */
    val bluetoothStepRequired: Boolean
        get() = bluetoothPermissionRequired

    /** True when the platform requires the permission and the user has not granted it. */
    val bluetoothPermissionMissing: Boolean
        get() = bluetoothPermissionRequired && !bluetoothPermissionGranted

    /** True when at least one app is enabled for announcements. */
    val hasEnabledApps: Boolean
        get() = enabledAppCount > 0

    /** True when at least one output category is allowed. */
    val hasAllowedOutput: Boolean
        get() = allowedOutputCount > 0
}
