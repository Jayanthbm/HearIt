package com.jayanth.hearit.core.common.model

/**
 * Which outputs announcements are allowed on (master prompt §6, "Audio output and devices"),
 * usable as the global value and as a whole per-app group override inside
 * [EligibilitySettings] (ADR-016).
 *
 * Defaults (ADR-017): wired and Bluetooth allowed, **phone speaker off** (master-prompt
 * mandated), unidentified outputs allowed. The settings UI must present this as an
 * opt-in narrowing: the more outputs the user disallows, the more often announcements are
 * suppressed with `OUTPUT_NOT_ALLOWED`.
 *
 * @property allowWired Generic wired output (headset/USB). Default on.
 * @property allowBluetooth Bluetooth outputs as a category. Default on.
 * @property allowPhoneSpeaker The phone's own speaker. Default **off** (master prompt:
 *   "Optional phone speaker output; disabled by default").
 * @property allowedBluetoothIds Null = any connected Bluetooth output. When set, only the
 *   listed device ids are allowed; an output without an observable id fails the allow-list.
 *   Applies within a per-app group only after that app overrides the group, because
 *   overrides are whole-group replacements (ADR-016).
 * @property allowUnidentifiedOutput Outputs the platform could not classify. Default on —
 *   a default-deny here would silence announcements on OEM-specific routes with no user
 *   action available to fix it (ADR-017).
 */
data class AudioOutputSettings(
    val allowWired: Boolean = true,
    val allowBluetooth: Boolean = true,
    val allowPhoneSpeaker: Boolean = false,
    val allowedBluetoothIds: Set<String>? = null,
    val allowUnidentifiedOutput: Boolean = true,
)
