package com.jayanth.hearit.core.rules

import com.jayanth.hearit.core.common.model.SkipReason

/**
 * Outcome of the eligibility pipeline for one notification (master prompt §6).
 *
 * `Announce` means the item may enter the announcement queue; grouping, delay, priority
 * ordering and expiry are applied later by the queue (phase P05) and do not belong here.
 * `Skip` carries the specific [SkipReason] that history records verbatim.
 */
sealed interface EligibilityDecision {
    /** The notification is eligible for announcement. */
    data object Announce : EligibilityDecision

    /** The notification must not be announced for the given [reason]. */
    data class Skip(val reason: SkipReason) : EligibilityDecision
}
