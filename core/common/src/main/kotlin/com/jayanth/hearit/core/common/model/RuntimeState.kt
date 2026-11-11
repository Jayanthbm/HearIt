package com.jayanth.hearit.core.common.model

/**
 * Device and service state at the moment a notification is evaluated.
 *
 * Produced by the platform layer (screen/lock observer, mute tracking, Priority Announce
 * mode) and passed in as an immutable snapshot so the rules pipeline stays deterministic
 * and JVM-testable. Fields are deliberately explicit with no defaults: a missing piece of
 * state must be a visible construction error rather than a silent "false".
 *
 * Conditions that change **while** speech is in progress (screen turns on, lock released,
 * mute enabled) are re-evaluated with the same pipeline so speech can be stopped
 * immediately (master prompt §6, ADR-011).
 *
 * @property screenOn True while the screen is on.
 * @property isLocked True while the device is in a locked/secure state.
 * @property temporaryMuteActive True while temporary mute is active (15 min / 30 min /
 *   1 h / until re-enabled). Mute is a **global override** (master prompt §11).
 * @property priorityAnnounceActive True while Priority Announce mode is switched on
 *   (Dashboard toggle, Quick Settings tile or notification action), ignoring its expiry
 *   which is tracked by the service layer.
 * @property isCallActive True while a phone call is active (master prompt §6: never speak
 *   during a call; the per-app queueing flag only converts the skip into a queue entry).
 * @property isDndActive True while the device's Do Not Disturb policy is considered active
 *   for announcement purposes. The exact mapping from the platform interruption filter is
 *   the platform layer's concern (P07/P10); the rule itself only ever reads this flag — a
 *   silent notification channel is a different thing and is never treated as DND (ADR-017).
 * @property activeOutput The audio output speech would be routed to, or null when it is not
 *   observed yet; null is treated as allowed so an unobserved output can never block
 *   announcements (ADR-017).
 */
data class RuntimeState(
    val screenOn: Boolean,
    val isLocked: Boolean,
    val temporaryMuteActive: Boolean,
    val priorityAnnounceActive: Boolean,
    val isCallActive: Boolean,
    val isDndActive: Boolean,
    val activeOutput: AudioOutput?,
    val isBatteryLowOrSaver: Boolean = false,
    val isProximityCovered: Boolean = true,
    val batteryPercent: Int = 100,
    val isPowerSave: Boolean = false,
)
