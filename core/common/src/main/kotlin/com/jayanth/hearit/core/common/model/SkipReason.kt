package com.jayanth.hearit.core.common.model

/**
 * Specific, actionable reason why a notification was not announced.
 *
 * These values are persisted in history (master prompt §12: "Record actionable reasons
 * such as screen on, unlocked, no allowed output, too long, keyword, DND, call, privacy,
 * muted, expired or overflow"), so they must be stable identifiers — user-facing wording
 * lives in the string resources, never here.
 *
 * Every implemented rule family has its own reason so history is never ambiguous. The
 * announcement queue (phase P05) additionally produces [EXPIRED], [OVERFLOW] and
 * [DUPLICATE_UPDATE].
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

    /** Text exceeded the configured length limit; the whole item is skipped, never truncated. */
    TOO_LONG,

    /** The text matched a configured keyword filter. */
    KEYWORD_FILTERED,

    /** Privacy rule excluded this app entirely. */
    PRIVACY_EXCLUDED_APP,

    /** Privacy rule: OTP/verification-code content. */
    PRIVACY_OTP,

    /** Privacy rule: password-related content. */
    PRIVACY_PASSWORD,

    /** Privacy rule: payment/transaction alert. */
    PRIVACY_PAYMENT,

    /** Privacy rule: banking/financial notification. */
    PRIVACY_BANKING,

    /** Privacy rule: custom sensitive keyword hit. */
    PRIVACY_SENSITIVE_KEYWORD,

    /** A phone call is active; never spoken (master prompt §6). */
    CALL_ACTIVE,

    /** Do Not Disturb is active and this app still respects it. */
    DND,

    /** The active audio output is not in this app's allowed set. */
    OUTPUT_NOT_ALLOWED,

    /** The item aged out of the queue before it could be announced (queue, phase P05). */
    EXPIRED,

    /** The item was evicted by a queue size limit under the configured overflow policy (queue, phase P05). */
    OVERFLOW,

    /** The item was an update of an already-announced notification and updates are not repeated (queue, phase P05 dedup). */
    DUPLICATE_UPDATE,
}
