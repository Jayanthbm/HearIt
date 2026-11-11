package com.jayanth.hearit.core.database

import com.jayanth.hearit.core.common.model.AppPriority
import com.jayanth.hearit.core.common.model.HistoryStatus
import com.jayanth.hearit.core.common.model.SkipReason

/**
 * One stored history record (master prompt §12), independent of Room.
 *
 * A record is keyed by the notification's stable identity [key] but keeps its own [id]:
 * with `UPDATE_ENTRY` handling an update rewrites the entry, with `NEW_ENTRY` every post
 * becomes a separate row while sharing the same [key].
 *
 * @property id Room row id, `0` before insertion.
 * @property key Stable notification identity (`package|tag|id`), never blank.
 * @property messageText Message body, or null in `METADATA_ONLY` storage mode — history
 *   never stores a body it was told not to keep.
 * @property recordedAtMillis When the record was written (history time, not post time).
 * @property status Status the pipeline reported at write time.
 * @property reason Actionable reason for a non-announcement, if any.
 * @property isUpdate True when the notification updated an already-seen key.
 */
data class HistoryEntry(
    val id: Long = 0L,
    val key: String,
    val packageName: String,
    val appName: String,
    val sender: String? = null,
    val messageText: String? = null,
    val channelId: String? = null,
    val category: String? = null,
    val postTimeMillis: Long,
    val recordedAtMillis: Long,
    val status: HistoryStatus,
    val reason: SkipReason? = null,
    val isUpdate: Boolean = false,
    val priority: AppPriority? = null,
) {
    init {
        require(key.isNotBlank()) { "HistoryEntry.key must not be blank" }
        require(packageName.isNotBlank()) { "HistoryEntry.packageName must not be blank" }
        require(appName.isNotBlank()) { "HistoryEntry.appName must not be blank" }
        require(postTimeMillis >= 0L) { "HistoryEntry.postTimeMillis must not be negative" }
        require(recordedAtMillis >= 0L) { "HistoryEntry.recordedAtMillis must not be negative" }
    }
}

/**
 * Enum-name mapping between [HistoryEntry] and its persisted form.
 *
 * Statuses and reasons are stored by **name** so the schema stays stable while the enums
 * evolve. Unknown names decode tolerantly (`SKIPPED` for status, null for reason) instead
 * of throwing: a history row written by a newer build must never crash an older one.
 */
object HistoryEntryMapper {

    fun statusToName(status: HistoryStatus): String = status.name

    fun reasonToName(reason: SkipReason?): String? = reason?.name

    fun priorityToName(priority: AppPriority?): String? = priority?.name

    fun statusFromName(name: String): HistoryStatus =
        HistoryStatus.entries.firstOrNull { it.name == name } ?: HistoryStatus.SKIPPED

    fun reasonFromName(name: String?): SkipReason? =
        name?.let { stored -> SkipReason.entries.firstOrNull { it.name == stored } }

    fun priorityFromName(name: String?): AppPriority? =
        name?.let { stored -> AppPriority.entries.firstOrNull { it.name == stored } }
}
