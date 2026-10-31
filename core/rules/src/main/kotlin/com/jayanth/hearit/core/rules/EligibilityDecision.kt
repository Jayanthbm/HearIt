package com.jayanth.hearit.core.rules

import com.jayanth.hearit.core.common.model.SkipReason

/**
 * Outcome of the eligibility pipeline for one notification (master prompt §6).
 *
 * `Announce` means the item may enter the announcement queue; grouping, delay, priority
 * ordering and expiry are applied later by the queue (phase P05) and do not belong here.
 * `Skip` carries the specific [SkipReason] that history records verbatim. `Queue` means the
 * item is held instead of announced or dropped.
 */
sealed interface EligibilityDecision {
    /** The notification is eligible for announcement. */
    data object Announce : EligibilityDecision

    /** The notification must not be announced for the given [reason]. */
    data class Skip(val reason: SkipReason) : EligibilityDecision

    /**
     * The item must be held rather than announced or dropped — currently only for a call
     * while the per-app call-queueing flag is on (master prompt §6: queueing is **off** by
     * default; it never bypasses any rule).
     *
     * Contract for the queue (P05): store the original [com.jayanth.hearit.core.common.model.NotificationSnapshot]
     * and, once the blocking condition clears, re-evaluate it through the **full** pipeline
     * with fresh state — screen/lock, mute, privacy, length, keywords, DND and output are
     * all applied again (master prompt §6, ADR-004/ADR-005).
     */
    data class Queue(val reason: SkipReason) : EligibilityDecision
}
