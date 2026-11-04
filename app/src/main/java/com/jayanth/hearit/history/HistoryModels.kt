package com.jayanth.hearit.history

import com.jayanth.hearit.core.common.model.HistoryStatus
import com.jayanth.hearit.core.common.model.HistoryStorageMode
import com.jayanth.hearit.core.common.model.HistoryUpdateHandling
import com.jayanth.hearit.core.common.model.SkipReason

/** Ordering the History feature offers (master prompt §12: newest, oldest, by app). */
enum class HistorySortOrder {
    RecentFirst,
    OldestFirst,
    ByApp,
}

/**
 * Everything the user filters history by (master prompt §12, P18).
 *
 * The free-text query searches app name, sender and stored message text in one field; the
 * database decides what can match, so the UI never builds SQL.
 */
data class HistoryFilter(
    val query: String = "",
    val status: HistoryStatus? = null,
    val sort: HistorySortOrder = HistorySortOrder.RecentFirst,
    val selectedPackageName: String? = null,
    val selectedAppName: String? = null,
)

/**
 * One history row as the feature renders it.
 *
 * [messageText] is null whenever the app's storage mode is `METADATA_ONLY` or `DISABLED`
 * for that entry, so the screen never shows a body history was told not to keep.
 */
data class HistoryItem(
    val id: Long,
    val appName: String,
    val packageName: String,
    val status: HistoryStatus,
    val reason: SkipReason? = null,
    val sender: String? = null,
    val messageText: String? = null,
    val recordedAtMillis: Long,
    val isUpdate: Boolean = false,
)

/** Global history settings the screen can edit (storage mode, update handling, retention). */
data class HistorySettingsState(
    val storageMode: HistoryStorageMode = HistoryStorageMode.FULL_CONTENT,
    val updateHandling: HistoryUpdateHandling = HistoryUpdateHandling.UPDATE_ENTRY,
    val retentionDays: Int? = null,
)

/**
 * Immutable state of the History feature.
 *
 * @property items Rows loaded so far, in the selected order.
 * @property loaded False until the first page read finishes.
 * @property loading True while a page is being read.
 * @property endReached True when the last read returned fewer than a full page.
 * @property unavailable True when a read failed; the screen says so instead of showing an
 *   empty list as if there were no history.
 */
data class HistoryUiState(
    val filter: HistoryFilter = HistoryFilter(),
    val settings: HistorySettingsState = HistorySettingsState(),
    val items: List<HistoryItem> = emptyList(),
    val availableApps: List<Pair<String, String>> = emptyList(),
    val playingItemId: Long? = null,
    val loaded: Boolean = false,
    val loading: Boolean = false,
    val endReached: Boolean = false,
    val unavailable: Boolean = false,
) {

    /** True when the query is filtered or searched, so an empty list needs different copy. */
    val isFiltered: Boolean
        get() = filter.query.isNotBlank() || filter.status != null || filter.selectedPackageName != null
}
