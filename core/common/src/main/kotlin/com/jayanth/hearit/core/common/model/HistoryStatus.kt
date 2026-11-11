package com.jayanth.hearit.core.common.model

/**
 * History status of one notification (master prompt §12 vocabulary), persisted verbatim
 * by history storage (phase P10).
 *
 * Several layers produce it: the announcement queue emits [QUEUED] for accepted and
 * call-held items and [SKIPPED] for expired, overflow-discarded or deduplicated items
 * (each with a [SkipReason]); the foreground service (P08) emits [ANNOUNCED] and
 * [INTERRUPTED]. [PENDING] is reserved for items that remain waiting in the queue across
 * time beyond their initial accept event.
 */
enum class HistoryStatus {

    /** The announcement was spoken. */
    ANNOUNCED,

    /** The item never reached speech; a [SkipReason] says why. */
    SKIPPED,

    /** The item was accepted into the queue (or the call-hold list). */
    QUEUED,

    /** The item is still waiting in the queue beyond its initial accept. */
    PENDING,

    /** Speech started and was stopped mid-utterance. */
    INTERRUPTED,
}
