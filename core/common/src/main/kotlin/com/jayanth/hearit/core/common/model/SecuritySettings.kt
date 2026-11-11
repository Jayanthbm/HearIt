package com.jayanth.hearit.core.common.model

/**
 * App security and device privacy controls (master prompt §15).
 *
 * @property biometricLockEnabled Require biometric or device credential (PIN/pattern/password)
 *   to access sensitive screens (Settings & History). Off by default.
 * @property secureScreenEnabled Hide app content from Android Recents and block screenshots
 *   using FLAG_SECURE. Off by default.
 */
data class SecuritySettings(
    val biometricLockEnabled: Boolean = false,
    val secureScreenEnabled: Boolean = false,
)
