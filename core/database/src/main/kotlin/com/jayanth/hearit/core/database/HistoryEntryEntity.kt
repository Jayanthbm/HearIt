package com.jayanth.hearit.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room row for one history record (master prompt §12).
 *
 * Enums are stored by **name** (`status`, `reason`, `priority`) so the schema survives enum
 * addition; [HistoryEntryMapper] decodes tolerantly. Indices match the documented query
 * shapes: newest-first paging (`recordedAtMillis`), app/status filtering
 * (`packageName`,`status`) and update lookup by notification identity (`key`, then
 * `key`+`recordedAtMillis` for newest-per-key).
 *
 * @property messageText Null in `METADATA_ONLY` storage mode.
 */
@Entity(
    tableName = "history_entries",
    indices = [
        Index(value = ["key"]),
        Index(value = ["recordedAtMillis"]),
        Index(value = ["packageName", "status"]),
        Index(value = ["key", "recordedAtMillis"]),
    ],
)
data class HistoryEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val key: String,
    val packageName: String,
    val appName: String,
    val sender: String?,
    val messageText: String?,
    val channelId: String?,
    val category: String?,
    val postTimeMillis: Long,
    val recordedAtMillis: Long,
    val status: String,
    val reason: String?,
    val isUpdate: Boolean,
    val priority: String?,
)

/** Domain → row. */
fun HistoryEntry.toEntity(): HistoryEntryEntity = HistoryEntryEntity(
    id = id,
    key = key,
    packageName = packageName,
    appName = appName,
    sender = sender,
    messageText = messageText,
    channelId = channelId,
    category = category,
    postTimeMillis = postTimeMillis,
    recordedAtMillis = recordedAtMillis,
    status = HistoryEntryMapper.statusToName(status),
    reason = HistoryEntryMapper.reasonToName(reason),
    isUpdate = isUpdate,
    priority = HistoryEntryMapper.priorityToName(priority),
)

/** Row → domain, tolerating unknown enum names written by another build. */
fun HistoryEntryEntity.toDomain(): HistoryEntry = HistoryEntry(
    id = id,
    key = key,
    packageName = packageName,
    appName = appName,
    sender = sender,
    messageText = messageText,
    channelId = channelId,
    category = category,
    postTimeMillis = postTimeMillis,
    recordedAtMillis = recordedAtMillis,
    status = HistoryEntryMapper.statusFromName(status),
    reason = HistoryEntryMapper.reasonFromName(reason),
    isUpdate = isUpdate,
    priority = HistoryEntryMapper.priorityFromName(priority),
)
