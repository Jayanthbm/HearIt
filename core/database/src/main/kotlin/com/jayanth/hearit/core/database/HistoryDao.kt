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

    /**
     * Paged, filtered history (P18).
     *
     * `sortBy` is the enum **name** as a string: Room only accepts compile-time constants
     * in `@Query`, and binding an enum directly is not supported. Each of the three
     * `CASE` keys is NULL for the two unused modes, so exactly one ordering applies.
     *
     * `searchText` matches app name, sender or stored message text; SQLite's `LIKE` is
     * case-insensitive for ASCII, and rows whose body was never stored simply cannot match
     * on content (master prompt §12 storage modes).
     */
    @Query(
        "SELECT * FROM history_entries " +
            "WHERE (:packageName IS NULL OR packageName = :packageName) " +
            "AND (:status IS NULL OR status = :status) " +
            "AND (:appName IS NULL OR appName = :appName) " +
            "AND (:sender IS NULL OR sender = :sender) " +
            "AND (:searchText IS NULL OR (appName LIKE '%' || :searchText || '%' " +
            "OR sender LIKE '%' || :searchText || '%' " +
            "OR messageText LIKE '%' || :searchText || '%')) " +
            "AND (:dateFrom IS NULL OR recordedAtMillis >= :dateFrom) " +
            "AND (:dateTo IS NULL OR recordedAtMillis <= :dateTo) " +
            "ORDER BY " +
            "CASE WHEN :sortBy = 'OldestFirst' THEN recordedAtMillis END ASC, " +
            "CASE WHEN :sortBy = 'ByApp' THEN appName END ASC, " +
            "CASE WHEN :sortBy = 'RecentFirst' THEN recordedAtMillis END DESC, " +
            "recordedAtMillis DESC LIMIT :limit OFFSET :offset",
    )
    suspend fun page(
        packageName: String?,
        status: String?,
        appName: String?,
        searchText: String?,
        sender: String?,
        dateFrom: Long?,
        dateTo: Long?,
        sortBy: String,
        limit: Int,
        offset: Int,
    ): List<HistoryEntryEntity>

    /** Total row count (diagnostics/tests). */
    @Query("SELECT COUNT(*) FROM history_entries")
    suspend fun count(): Long
}
