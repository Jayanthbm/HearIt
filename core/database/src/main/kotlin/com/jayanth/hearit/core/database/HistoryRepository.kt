package com.jayanth.hearit.core.database

import com.jayanth.hearit.core.common.model.AppPriority
import com.jayanth.hearit.core.common.model.HistorySettings
import com.jayanth.hearit.core.common.model.HistoryStatus
import com.jayanth.hearit.core.common.model.NotificationSnapshot
import com.jayanth.hearit.core.common.model.SkipReason
import com.jayanth.hearit.core.common.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * History storage API consumed by the pipeline and the history feature (master prompt §12).
 *
 * Recording is independent of eligibility (ADR-007): callers report every outcome with the
 * status and reason they produced, and this layer applies the app's storage mode, update
 * handling and retention.
 */
interface HistoryRepository {

    /**
     * Records one notification according to [settings].
     *
     * @return the persisted row id, or null when history is disabled for this app.
     */
    suspend fun record(
        snapshot: NotificationSnapshot,
        settings: HistorySettings,
        status: HistoryStatus,
        reason: SkipReason? = null,
        priority: AppPriority? = null,
    ): Long?

    /** Deletes rows older than the configured retention; returns how many were removed. */
    suspend fun prune(settings: HistorySettings, nowMillis: Long): Int

    /** Newest-first page matching [query]. */
    suspend fun page(query: HistoryQuery): List<HistoryEntry>

    /** Newest record for one notification identity, or null. */
    suspend fun latestFor(key: String): HistoryEntry?

    /** Deletes one entry ("clear individual"). */
    suspend fun delete(id: Long)

    /** Restores a previously deleted entry (e.g. on Undo). */
    suspend fun restore(entry: HistoryEntry): Long

    /** Deletes all history ("clear all"). */
    suspend fun deleteAll()

    /** Row count. */
    suspend fun count(): Long

    /** Reactive stream of total history entries. */
    fun observeCount(): Flow<Long>

    /** Reactive stream of history count filtered by status recorded since [sinceMillis]. */
    fun observeCountSinceAndStatus(sinceMillis: Long, status: HistoryStatus): Flow<Long>

    /** Reactive stream of history entries for one app package, newest first. */
    fun observeByPackage(packageName: String): Flow<List<HistoryEntry>>

    /** Deletes rows for a specific package older than [retentionDays]; returns count removed. */
    suspend fun prunePackage(packageName: String, retentionDays: Int?, nowMillis: Long): Int

    /** Deletes rows for a specific package older than [olderThanDays], or all if null; returns count removed. */
    suspend fun clearPackageHistory(packageName: String, olderThanDays: Int? = null): Int

    /** Returns recently announced entries recorded since [sinceMillis]. */
    suspend fun recentAnnounced(sinceMillis: Long): List<HistoryEntry>

    /** Returns distinct apps (packageName to appName) recorded in history. */
    suspend fun distinctApps(): List<Pair<String, String>>

    companion object {
        /**
         * How long an identical (app, sender, body) notification reuses its existing row.
         * 30 seconds absorbs rapid reposts and progress updates without dropping genuine
         * subsequent messages with identical content (e.g. "Ok", "Yes").
         */
        const val DUPLICATE_WINDOW_MILLIS: Long = 30_000L
    }
}

/**
 * [HistoryRepository] over [HistoryDao], applying [HistoryRecorderPolicy] and
 * [HistoryRetentionPolicy].
 *
 * Thin by design: all decisions are the pure policies' (JVM-tested); this class only
 * performs the lookup, the insert/update branch and the retention sweep.
 */
class RoomHistoryRepository(
    private val dao: HistoryDao,
    private val clock: Clock,
) : HistoryRepository {

    override suspend fun record(
        snapshot: NotificationSnapshot,
        settings: HistorySettings,
        status: HistoryStatus,
        reason: SkipReason?,
        priority: AppPriority?,
    ): Long? {
        val now = clock.nowMillis()
        val existing = dao.latestFor(snapshot.key)
        val existingStatus = existing?.status?.let { raw ->
            runCatching { HistoryStatus.valueOf(raw) }.getOrNull()
        }
        val action = HistoryRecorderPolicy.decide(
            snapshot = snapshot,
            settings = settings,
            status = status,
            reason = reason,
            recordedAtMillis = now,
            priority = priority,
            existingEntryForSameKey = existing != null,
            existingEntryStatus = existingStatus,
        )

        // Content-level duplicate collapsing: an app that re-posts the same body keeps one row
        // instead of writing a new history entry for every repeat.
        if (action is HistoryRecordAction.Insert && snapshot.messageText.isNotBlank()) {
            val duplicate = dao.latestIdenticalContent(
                packageName = snapshot.packageName,
                sender = snapshot.sender,
                messageText = snapshot.messageText,
                sinceMillis = now - HistoryRepository.DUPLICATE_WINDOW_MILLIS,
            )
            if (duplicate != null) {
                if (duplicate.status == HistoryStatus.QUEUED.name &&
                    (status == HistoryStatus.ANNOUNCED || status == HistoryStatus.INTERRUPTED || status == HistoryStatus.SKIPPED)
                ) {
                    dao.update(action.entry.copy(id = duplicate.id).toEntity())
                }
                return duplicate.id
            }
        }

        return when (action) {
            HistoryRecordAction.Discard -> null
            is HistoryRecordAction.Insert -> dao.insert(action.entry.toEntity())
            is HistoryRecordAction.ReplaceExisting -> {
                val rowId = existing?.id
                if (rowId == null) {
                    dao.insert(action.entry.toEntity())
                } else {
                    dao.update(action.entry.copy(id = rowId).toEntity())
                    rowId
                }
            }
        }
    }

    override suspend fun prune(settings: HistorySettings, nowMillis: Long): Int {
        val cutoff = HistoryRetentionPolicy.cutoffMillis(nowMillis, settings.retentionDays) ?: return 0
        return dao.deleteOlderThan(cutoff)
    }

    override suspend fun page(query: HistoryQuery): List<HistoryEntry> =
        dao.page(
            packageName = query.packageName,
            status = query.status?.let(HistoryEntryMapper::statusToName),
            appName = query.appName,
            searchText = query.searchText,
            sender = query.sender,
            dateFrom = query.dateFrom,
            dateTo = query.dateTo,
            sortBy = query.sortBy.name,
            limit = query.limit,
            offset = query.offset,
        ).map { it.toDomain() }

    override suspend fun latestFor(key: String): HistoryEntry? = dao.latestFor(key)?.toDomain()

    override suspend fun delete(id: Long) = dao.deleteById(id)

    override suspend fun restore(entry: HistoryEntry): Long = dao.insert(entry.copy(id = 0L).toEntity())

    override suspend fun deleteAll() = dao.deleteAll()

    override suspend fun count(): Long = dao.count()

    override fun observeCount(): Flow<Long> = dao.observeCount()

    override fun observeCountSinceAndStatus(sinceMillis: Long, status: HistoryStatus): Flow<Long> =
        dao.observeCountSinceAndStatus(sinceMillis, status.name)

    override fun observeByPackage(packageName: String): Flow<List<HistoryEntry>> =
        dao.observeByPackage(packageName).map { entities -> entities.map { it.toDomain() } }

    override suspend fun prunePackage(packageName: String, retentionDays: Int?, nowMillis: Long): Int {
        val cutoff = HistoryRetentionPolicy.cutoffMillis(nowMillis, retentionDays) ?: return 0
        return dao.deleteOlderThanForPackage(packageName, cutoff)
    }

    override suspend fun clearPackageHistory(packageName: String, olderThanDays: Int?): Int {
        return if (olderThanDays == null) {
            dao.deleteAllForPackage(packageName)
        } else {
            val cutoff = clock.nowMillis() - (olderThanDays.toLong() * 24 * 60 * 60 * 1000L)
            dao.deleteOlderThanForPackage(packageName, cutoff)
        }
    }

    override suspend fun recentAnnounced(sinceMillis: Long): List<HistoryEntry> =
        dao.recentEntriesByStatus(HistoryStatus.ANNOUNCED.name, sinceMillis).map { it.toDomain() }

    override suspend fun distinctApps(): List<Pair<String, String>> =
        dao.distinctApps().map { it.packageName to it.appName }
}
