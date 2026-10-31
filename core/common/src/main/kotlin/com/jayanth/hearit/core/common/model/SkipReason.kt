package com.jayanth.hearit.core.common.model

/**
 * Specific, actionable reason why a notification was not announced.
 *
 * These values are persisted in history (master prompt §12: "Record actionable reasons
 * such as screen on, unlocked, no allowed output, too long, keyword, DND, call, privacy,
 * muted, expired or overflow"), so they must be stable identifiers — user-facing wording
 * lives in the string resources, never here.
 *
 * The set grows as the rule families are implemented (P02: privacy/length/keywords,
 * P03: DND/call/output, P05: expiry/overflow); every rule that can skip an item must have
 * its own reason so history is never ambiguous.
 */
enum class SkipReason {
    /** App is not in the enabled-app list (all apps are disabled by default). */
    APP_DISABLED,

    /** Temporary mute is active; mute is a global override and also applies in Priority mode. */
    MUTED,

    /** Screen state did not match the configured screen condition (it was on). */
    SCREEN_ON,

    /** Screen state did not match the configured screen condition (it was off). */
    SCREEN_OFF,

    /** Lock state did not match the configured lock condition (it was unlocked). */
    UNLOCKED,

    /** Lock state did not match the configured lock condition (it was locked). */
    LOCKED,
}
