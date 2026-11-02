package com.jayanth.hearit.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

/**
 * History table access (master prompt §12: filter, search, paginate, clear, retention).
 *
 * All methods are suspending so Room dispatches off the main thread (architecture §5).
 * Filtering uses nullable parameters (`:param IS NULL OR ...`) so one query serves every
 * combination without string-concatenated SQL.
 */
@Dao
interface HistoryDao {

    /** Inserts a row and returns its generated id. */
    @Insert
    suspend fun insert(entity: HistoryEntryEntity): Long

    /** Rewrites an existing row (used by `UPDATE_ENTRY` update handling). */
    @Update
    suspend fun update(entity: HistoryEntryEntity)

    /** Newest row for one notification identity, if history already saw it. */
    @Query("SELECT * FROM history_entries WHERE key = :key ORDER BY recordedAtMillis DESC LIMIT 1")
    suspend fun latestFor(key: String): HistoryEntryEntity?

    /** Deletes one row. */
    @Query("DELETE FROM history_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Clears all history (master prompt §12 "clear all"). */
    @Query("DELETE FROM history_entries")
    suspend fun deleteAll()

    /** Retention cleanup: returns the number of deleted rows. */
    @Query("DELETE FROM history_entries WHERE recordedAtMillis < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long): Int

    /** Newest-first page with optional app/status/filters (P18 pagination). */
    @Query(
        "SELECT * FROM history_entries " +
            "WHERE (:packageName IS NULL OR packageName = :packageName) " +
            "AND (:status IS NULL OR status = :status) " +
            "AND (:appName IS NULL OR appName = :appName) " +
            "AND (:sender IS NULL OR sender = :sender) " +
            "AND (:dateFrom IS NULL OR recordedAtMillis >= :dateFrom) " +
            "AND (:dateTo IS NULL OR recordedAtMillis <= :dateTo) " +
            "ORDER BY case when :sortBy = SortBy.RecentFirst then recordedAtMillis desc 
                     when :sortBy = SortBy.OldestFirst then recordedAtMillis
                     when :sortBy = SortBy.ByApp then appName
                     else recordedAtMillis desc end LIMIT :limit OFFSET :offset",
    )
    suspend fun page(
        packageName: String?,
        status: String?,
        appName: String?,
        sender: String?,
        dateFrom: Long?,
        dateTo: Long?,
        sortBy: SortBy,
        limit: Int,
        offset: Int,
    ): List<HistoryEntryEntity>

    /** Total row count (diagnostics/tests). */
    @Query("SELECT COUNT(*) FROM history_entries")
    suspend fun count(): Long

    /** Enum for sorting history results. */
    enum class SortBy {
        RecentFirst,
        OldestFirst,
        ByApp,
    }
}
