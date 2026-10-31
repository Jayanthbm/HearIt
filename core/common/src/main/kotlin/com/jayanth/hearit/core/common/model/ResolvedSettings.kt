package com.jayanth.hearit.core.common.model

/**
 * All settings the eligibility rules need for one notification, already merged from
 * global defaults and per-app overrides (see `EligibilitySettings.resolveWith`).
 *
 * Built by the settings layer (phase P09) and passed as an immutable value so the rules
 * pipeline has no data-layer dependency.
 *
 * @property appEnabled True when the user has enabled this app for announcements.
 *   **All apps are disabled by default** (master prompt §5), so a fresh installation
 *   yields `false`.
 * @property priorityAnnounceListed True when the app is in the separate Priority Announce
 *   app list (master prompt §10). That list is distinct from the normal enabled-app list.
 * @property eligibility Global eligibility defaults merged with this app's overrides.
 */
data class ResolvedSettings(
    val appEnabled: Boolean = false,
    val priorityAnnounceListed: Boolean = false,
    val eligibility: EligibilitySettings = EligibilitySettings(),
)
