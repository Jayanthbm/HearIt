package com.jayanth.hearit.history

import com.jayanth.hearit.core.database.HistoryEntry
import com.jayanth.hearit.core.database.HistoryQuery

/**
 * Pure mapping between the History feature and `:core:database` (master prompt §12, P18).
 *
 * Keeping it here means the screen and ViewModel never build a `HistoryQuery` by hand and
 * the paging/filter rules are JVM-testable without a database.
 */
object HistoryQueryMapper {

    /** Rows per page: small enough to keep the first paint quick, large enough to scroll. */
    const val PAGE_SIZE: Int = 50

    /**
     * Builds the query for [filter] at [offset].
     *
     * The free-text query is trimmed and sent only when it has content, so a stray space
     * never becomes a filter that matches nothing.
     */
    fun toQuery(filter: HistoryFilter, offset: Int = 0): HistoryQuery = HistoryQuery(
        packageName = filter.selectedPackageName,
        status = filter.status,
        searchText = filter.query.trim().takeIf { it.isNotEmpty() },
        sortBy = when (filter.sort) {
            HistorySortOrder.RecentFirst -> HistoryQuery.SortBy.RecentFirst
            HistorySortOrder.OldestFirst -> HistoryQuery.SortBy.OldestFirst
            HistorySortOrder.ByApp -> HistoryQuery.SortBy.ByApp
        },
        limit = PAGE_SIZE,
        offset = offset,
    )

    /** Domain rows → screen items, preserving order and never inventing content. */
    fun toItems(entries: List<HistoryEntry>): List<HistoryItem> = entries.map { entry ->
        HistoryItem(
            id = entry.id,
            appName = entry.appName,
            packageName = entry.packageName,
            status = entry.status,
            reason = entry.reason,
            sender = entry.sender,
            messageText = entry.messageText,
            recordedAtMillis = entry.recordedAtMillis,
            isUpdate = entry.isUpdate,
        )
    }
}
