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
 */
data class RuntimeState(
    val screenOn: Boolean,
    val isLocked: Boolean,
    val temporaryMuteActive: Boolean,
    val priorityAnnounceActive: Boolean,
)
