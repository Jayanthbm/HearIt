package com.jayanth.hearit.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayanth.hearit.core.common.model.HistoryStatus
import com.jayanth.hearit.core.common.model.HistoryStorageMode
import com.jayanth.hearit.core.common.model.HistoryUpdateHandling
import com.jayanth.hearit.core.database.HistoryRepository
import com.jayanth.hearit.core.datastore.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the History feature (master prompt §12, phase P18).
 *
 * Reads one page at a time through [HistoryRepository] (never the whole table), applies the
 * filter/sort through [HistoryQueryMapper], and writes the global history settings back
 * through [SettingsRepository] so the service's recorder picks them up on its next
 * notification.
 *
 * A read failure is surfaced as [HistoryUiState.unavailable] rather than shown as an empty
 * list, and destructive actions are only performed when the screen has already asked for
 * confirmation.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update { current ->
                    current.copy(
                        settings = HistorySettingsState(
                            storageMode = settings.history.storageMode,
                            updateHandling = settings.history.updateHandling,
                            retentionDays = settings.history.retentionDays,
                        ),
                    )
                }
            }
        }
        load(reset = true)
    }

    /** Re-reads the first page; called when the screen resumes (the service keeps writing). */
    fun onRefresh() = load(reset = true)

    fun onQueryChanged(query: String) = updateFilter { it.copy(query = query) }

    fun onStatusChanged(status: HistoryStatus?) = updateFilter { it.copy(status = status) }

    fun onSortChanged(sort: HistorySortOrder) = updateFilter { it.copy(sort = sort) }

    /** Appends the next page; a no-op at the end or while a page is already loading. */
    fun onLoadMore() = load(reset = false)

    /** Deletes one entry (the screen confirms first). */
    fun onDeleteEntry(id: Long) {
        viewModelScope.launch {
            if (safely { historyRepository.delete(id) } != null) load(reset = true)
        }
    }

    /** Deletes every entry (the screen confirms first). */
    fun onClearAll() {
        viewModelScope.launch {
            if (safely { historyRepository.deleteAll() } != null) load(reset = true)
        }
    }

    fun onStorageModeChanged(storageMode: HistoryStorageMode) =
        updateSettings { it.copy(storageMode = storageMode) }

    fun onUpdateHandlingChanged(updateHandling: HistoryUpdateHandling) =
        updateSettings { it.copy(updateHandling = updateHandling) }

    fun onRetentionChanged(retentionDays: Int?) =
        updateSettings { it.copy(retentionDays = retentionDays) }

    // ---- internals ---------------------------------------------------------------------

    private fun updateFilter(transform: (HistoryFilter) -> HistoryFilter) {
        _state.update { it.copy(filter = transform(it.filter)) }
        load(reset = true)
    }

    private fun updateSettings(transform: (com.jayanth.hearit.core.common.model.HistorySettings) -> com.jayanth.hearit.core.common.model.HistorySettings) {
        viewModelScope.launch {
            safely {
                settingsRepository.update { settings ->
                    settings.copy(history = transform(settings.history))
                }
            }
        }
    }

    /**
     * Reads one page.
     *
     * A reset cancels any in-flight read and starts at offset 0, so a filter change can never
     * append rows from the previous filter; a load-more appends after the current rows.
     */
    private fun load(reset: Boolean) {
        if (reset) {
            loadJob?.cancel()
        } else if (_state.value.loading || _state.value.endReached) {
            return
        }

        val filter = _state.value.filter
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val offset = if (reset) 0 else _state.value.items.size
            val page = safely { historyRepository.page(HistoryQueryMapper.toQuery(filter, offset)) }
            if (page == null) {
                _state.update { it.copy(loading = false, loaded = true, unavailable = true) }
                return@launch
            }
            val items = HistoryQueryMapper.toItems(page)
            _state.update { current ->
                current.copy(
                    items = if (reset) items else current.items + items,
                    loaded = true,
                    loading = false,
                    endReached = page.size < HistoryQueryMapper.PAGE_SIZE,
                    unavailable = false,
                )
            }
        }
    }

    /**
     * Runs [block], reporting failure through [HistoryUiState.unavailable].
     *
     * Cancellation is rethrown so a superseded page read never marks history as broken.
     */
    private suspend fun <T> safely(block: suspend () -> T): T? = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        _state.update { it.copy(unavailable = true) }
        null
    }
}
