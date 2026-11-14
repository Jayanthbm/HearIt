package com.jayanth.hearit.core.database

import com.jayanth.hearit.core.common.model.AppPriority
import com.jayanth.hearit.core.common.model.HistorySettings
import com.jayanth.hearit.core.common.model.HistoryStatus
import com.jayanth.hearit.core.common.model.HistoryStorageMode
import com.jayanth.hearit.core.common.model.HistoryUpdateHandling
import com.jayanth.hearit.core.common.model.NotificationSnapshot
import com.jayanth.hearit.core.common.model.SkipReason

/** What to do with one notification once history settings are applied. */
sealed interface HistoryRecordAction {

    /** History is disabled for this app (`HistoryStorageMode.DISABLED`). */
    data object Discard : HistoryRecordAction

    /** Write a new row. */
    data class Insert(val entry: HistoryEntry) : HistoryRecordAction

    /** Rewrite the existing row for this key (master prompt §12 `UPDATE_ENTRY`). */
    data class ReplaceExisting(val entry: HistoryEntry) : HistoryRecordAction
}

/**
 * Decides **whether and how** a notification is recorded (master prompt §12, P10).
 *
 * Pure: no database, no clock reads — the caller passes the record time and whether a row
 * for the same key already exists. Rules:
 *  - `DISABLED` storage mode discards everything for that app (speech is unaffected);
 *  - `METADATA_ONLY` drops the message body but keeps app, sender, times, status, reason;
 *  - `FULL_CONTENT` keeps the body exactly as extracted (never truncated);
 *  - `UPDATE_ENTRY` rewrites the existing row for the same key, `NEW_ENTRY` always inserts
 *    (both keep [HistoryEntry.isUpdate] so updates stay distinguishable);
 *  - history is recorded for **every** pipeline outcome, including skipped, queued,
 *    expired and interrupted items (ADR-007: collection is independent of eligibility).
 */
object HistoryRecorderPolicy {

    fun decide(
        snapshot: NotificationSnapshot,
        settings: HistorySettings,
        status: HistoryStatus,
        reason: SkipReason?,
        recordedAtMillis: Long,
        priority: AppPriority? = null,
        existingEntryForSameKey: Boolean = false,
        existingEntryStatus: HistoryStatus? = null,
    ): HistoryRecordAction {
        if (settings.storageMode == HistoryStorageMode.DISABLED) {
            return HistoryRecordAction.Discard
        }

        val entry = HistoryEntry(
            key = snapshot.key,
            packageName = snapshot.packageName,
            appName = snapshot.appName,
            sender = snapshot.sender,
            messageText = when (settings.storageMode) {
                HistoryStorageMode.FULL_CONTENT -> snapshot.messageText.takeIf { it.isNotBlank() }
                HistoryStorageMode.METADATA_ONLY, HistoryStorageMode.DISABLED -> null
            },
            channelId = snapshot.channelId,
            category = snapshot.category,
            postTimeMillis = snapshot.postTimeMillis,
            recordedAtMillis = recordedAtMillis,
            status = status,
            reason = reason,
            isUpdate = snapshot.isUpdate,
            priority = priority,
        )

        val isQueuedLifecycleTransition = existingEntryStatus == HistoryStatus.QUEUED &&
            (status == HistoryStatus.ANNOUNCED || status == HistoryStatus.INTERRUPTED || status == HistoryStatus.SKIPPED || status == HistoryStatus.QUEUED)

        return if (isQueuedLifecycleTransition) {
            HistoryRecordAction.ReplaceExisting(entry)
        } else {
            HistoryRecordAction.Insert(entry)
        }
    }
}

/**
 * Retention maths for history cleanup (master prompt §12: 1/3/7/10/15/30/60/90 days or
 * never). Pure and clock-injected; the repository passes `now`.
 */
object HistoryRetentionPolicy {

    /** Milliseconds in one day; retained as a constant so tests and callers agree. */
    const val MILLIS_PER_DAY: Long = 86_400_000L

    /**
     * Rows recorded strictly before this instant may be deleted, or null when retention is
     * `Never` (no automatic deletion).
     *
     * @throws IllegalArgumentException when [retentionDays] is negative.
     */
    fun cutoffMillis(nowMillis: Long, retentionDays: Int?): Long? {
        require(nowMillis >= 0L) { "nowMillis must not be negative" }
        if (retentionDays == null) return null
        require(retentionDays >= 0) { "retentionDays must not be negative" }
        return nowMillis - retentionDays.toLong() * MILLIS_PER_DAY
    }
}

/**
 * Paged history query (master prompt §12: filter by app/status, lazy loading).
 *
 * @property packageName Restrict to one app, or null for all apps.
 * @property status Restrict to one status, or null for all statuses.
 * @property appName Restrict to one app name, or null for all apps.
 * @property searchText Free-text search across app name, sender and stored message text
 *   (case-insensitive `LIKE`), or null for no text filter. Message text that was never
 *   stored (`METADATA_ONLY`) cannot match, by design.
 * @property sender Filter by sender, or null for all senders.
 * @property dateFrom Start of date range (inclusive), null for no start.
 * @property dateTo End of date range (inclusive), null for no end.
 * @property sortBy Sort direction.
 * @property limit Page size; capped so a malformed caller cannot ask for the whole table.
 * @property offset Rows to skip for pagination.
 */
data class HistoryQuery(
    val packageName: String? = null,
    val status: HistoryStatus? = null,
    val appName: String? = null,
    val searchText: String? = null,
    val sender: String? = null,
    val dateFrom: Long? = null,
    val dateTo: Long? = null,
    val sortBy: SortBy = SortBy.RecentFirst,
    val limit: Int = 50,
    val offset: Int = 0,
) {
    init {
        require(limit in 1..MAX_PAGE_SIZE) {
            "HistoryQuery.limit must be between 1 and $MAX_PAGE_SIZE, was $limit"
        }
        require(offset >= 0) { "HistoryQuery.offset must not be negative, was $offset" }
        if (packageName != null) {
            require(packageName.isNotBlank()) { "HistoryQuery.packageName must not be blank when set" }
        }
        if (searchText != null) {
            require(searchText.isNotBlank()) { "HistoryQuery.searchText must not be blank when set" }
        }
    }

    /** How to sort the history page results. */
    enum class SortBy {
        RecentFirst,
        OldestFirst,
        ByApp,
    }

    companion object {
        /** Upper bound for one page; the UI paginates instead of loading everything. */
        const val MAX_PAGE_SIZE: Int = 500
    }
}
