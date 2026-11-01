package com.jayanth.hearit.core.queue

import com.jayanth.hearit.core.common.model.HistoryStatus
import com.jayanth.hearit.core.common.model.NotificationSnapshot
import com.jayanth.hearit.core.common.model.SkipReason

/**
 * A fact about one queued notification that the foreground service records in history
 * (master prompt §12). Every event carries the snapshot, the history status to store and,
 * for terminal non-announcements, the actionable reason — expired and discarded items keep
 * their history status and reason (master prompt §8, ADR-005/ADR-019).
 */
sealed interface QueueEvent {

    /** The notification this event is about. */
    val snapshot: NotificationSnapshot

    /** History status to record for [snapshot] at this point in its lifecycle. */
    val historyStatus: HistoryStatus

    /** Actionable reason for a non-announcement, or null when there is none. */
    val reason: SkipReason?

    /** The item entered the queue (or the call-hold list when [reason] is set). */
    data class Queued(
        override val snapshot: NotificationSnapshot,
        override val reason: SkipReason? = null,
    ) : QueueEvent {
        override val historyStatus: HistoryStatus = HistoryStatus.QUEUED
    }

    /** An update replaced the pending or held content of the same notification key in place; still queued. */
    data class Replaced(
        override val snapshot: NotificationSnapshot,
    ) : QueueEvent {
        override val historyStatus: HistoryStatus = HistoryStatus.QUEUED
        override val reason: SkipReason? = null
    }

    /** The item was not repeated because it was already announced (master prompt §10 dedup). */
    data class Deduplicated(
        override val snapshot: NotificationSnapshot,
    ) : QueueEvent {
        override val historyStatus: HistoryStatus = HistoryStatus.SKIPPED
        override val reason: SkipReason = SkipReason.DUPLICATE_UPDATE
    }

    /** The item aged out of the queue before announcement (ADR-019). */
    data class Expired(
        override val snapshot: NotificationSnapshot,
    ) : QueueEvent {
        override val historyStatus: HistoryStatus = HistoryStatus.SKIPPED
        override val reason: SkipReason = SkipReason.EXPIRED
    }

    /** A queue size limit evicted the item under the configured overflow policy. */
    data class OverflowDiscarded(
        override val snapshot: NotificationSnapshot,
    ) : QueueEvent {
        override val historyStatus: HistoryStatus = HistoryStatus.SKIPPED
        override val reason: SkipReason = SkipReason.OVERFLOW
    }
}
