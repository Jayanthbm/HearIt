package com.jayanth.hearit.history

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayanth.hearit.core.database.HistoryQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.jayanth.hearit.core.common.model.HistoryStatus
import com.jayanth.hearit.core.common.model.HistoryStorageMode
import com.jayanth.hearit.core.common.model.HistoryUpdateHandling
import com.jayanth.hearit.core.common.model.KeywordFilterSettings
import com.jayanth.hearit.core.common.model.KeywordMatchMode
import com.jayanth.hearit.core.database.HistoryRepository
import com.jayanth.hearit.core.datastore.SettingsRepository
import com.jayanth.hearit.core.tts.AnnouncementSection
import com.jayanth.hearit.core.tts.BuiltAnnouncement
import com.jayanth.hearit.core.tts.SectionKind
import com.jayanth.hearit.core.tts.TextToSpeechEngine
import com.jayanth.hearit.core.tts.UtteranceListener
import com.jayanth.hearit.applications.AppSettingsEditor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    private val engine: TextToSpeechEngine,
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

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
        viewModelScope.launch {
            historyRepository.observeCount().collect {
                load(reset = true)
            }
        }
    }

    /** Re-reads the first page; called when the screen resumes (the service keeps writing). */
    fun onRefresh() = load(reset = true)

    fun onQueryChanged(query: String) = updateFilter { it.copy(query = query) }

    fun onStatusChanged(status: HistoryStatus?) = updateFilter { it.copy(status = status) }

    fun onSortChanged(sort: HistorySortOrder) = updateFilter { it.copy(sort = sort) }

    fun onDatePresetSelected(preset: HistoryDatePreset) {
        val (fromMillis, toMillis) = HistoryExporter.dateBoundsForPreset(preset)
        updateFilter {
            it.copy(
                datePreset = preset,
                dateFromMillis = fromMillis,
                dateToMillis = toMillis,
            )
        }
    }

    fun onCustomDateRangeSelected(startUtcMillis: Long, endUtcMillis: Long) {
        val (fromMillis, toMillis) = HistoryExporter.normalizePickerRange(startUtcMillis, endUtcMillis)
        updateFilter {
            it.copy(
                datePreset = HistoryDatePreset.CUSTOM,
                dateFromMillis = fromMillis,
                dateToMillis = toMillis,
            )
        }
    }

    /** Exports every entry matching the active filter to CSV or TXT via SAF (#10). */
    fun exportToUri(contentResolver: ContentResolver, uri: Uri, asCsv: Boolean) {
        val currentFilter = _state.value.filter
        viewModelScope.launch {
            val entries = safely {
                val all = mutableListOf<com.jayanth.hearit.core.database.HistoryEntry>()
                var offset = 0
                while (true) {
                    val page = historyRepository.page(
                        HistoryQueryMapper.toQuery(currentFilter, offset)
                            .copy(limit = HistoryQuery.MAX_PAGE_SIZE),
                    )
                    all += page
                    if (page.size < HistoryQuery.MAX_PAGE_SIZE) break
                    offset += page.size
                }
                all.toList()
            } ?: return@launch

            val content = withContext(Dispatchers.Default) {
                if (asCsv) HistoryExporter.formatCsv(entries) else HistoryExporter.formatTxt(entries)
            }
            val wrote = withContext(Dispatchers.IO) {
                runCatching {
                    contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(content.toByteArray(Charsets.UTF_8))
                        stream.flush()
                    } ?: error("Cannot open output stream")
                }.isSuccess
            }
            if (wrote) {
                _userMessage.emit("Exported ${entries.size} history ${if (entries.size == 1) "entry" else "entries"}")
            } else {
                _userMessage.emit("Could not write export file")
            }
        }
    }

    /** Appends the next page; a no-op at the end or while a page is already loading. */
    fun onLoadMore() = load(reset = false)

    /** Deletes one entry (the screen confirms first). */
    fun onDeleteEntry(id: Long) {
        viewModelScope.launch {
            if (safely { historyRepository.delete(id) } != null) load(reset = true)
        }
    }

    /** Deletes multiple selected entries. */
    fun onDeleteMultiple(ids: Set<Long>) {
        viewModelScope.launch {
            ids.forEach { id ->
                safely { historyRepository.delete(id) }
            }
            load(reset = true)
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

    fun onPlayEntry(item: HistoryItem) {
        if (_state.value.playingItemId == item.id) {
            engine.stop()
            _state.update { it.copy(playingItemId = null) }
            return
        }

        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val textToSpeak = item.messageText?.takeIf { it.isNotBlank() } ?: "Notification from ${item.appName}"
            val sections = buildList {
                add(AnnouncementSection(SectionKind.APP_NAME, item.appName))
                item.sender?.takeIf { it.isNotBlank() }?.let {
                    add(AnnouncementSection(SectionKind.SENDER, "$it says,"))
                }
                add(AnnouncementSection(SectionKind.MESSAGE, textToSpeak))
            }
            val announcement = BuiltAnnouncement(
                sections = sections,
                pauseBetweenSectionsMillis = settings.format.pauseBetweenSectionsMillis,
            )

            _state.update { it.copy(playingItemId = item.id) }
            val accepted = engine.speak(
                announcement = announcement,
                settings = settings.speech,
                listener = object : UtteranceListener {
                    override fun onComplete(announcement: BuiltAnnouncement) {
                        _state.update { if (it.playingItemId == item.id) it.copy(playingItemId = null) else it }
                    }

                    override fun onError(announcement: BuiltAnnouncement, error: com.jayanth.hearit.core.tts.TtsError) {
                        _state.update { if (it.playingItemId == item.id) it.copy(playingItemId = null) else it }
                    }
                },
            )
            if (!accepted) {
                _state.update { if (it.playingItemId == item.id) it.copy(playingItemId = null) else it }
            }
        }
    }

    fun onStopPlaying() {
        engine.stop()
        _state.update { it.copy(playingItemId = null) }
    }

    fun onAppFilterChanged(packageName: String?, appName: String?) {
        updateFilter { it.copy(selectedPackageName = packageName, selectedAppName = appName) }
    }

    fun onAddFilter(keyword: String, matchMode: KeywordMatchMode, isSender: Boolean = false, targetAppPackage: String?) {
        viewModelScope.launch {
            safely {
                val formatted = AppSettingsEditor.formatKeyword(keyword, matchMode, isSender)
                val parsedNew = AppSettingsEditor.parseFilterItem(formatted)
                val settings = settingsRepository.current()

                val existingKeywords = if (targetAppPackage == null) {
                    settings.eligibility.keywordFilter.keywords
                } else {
                    (settings.appOverrides[targetAppPackage]?.eligibility?.keywordFilter ?: settings.eligibility.keywordFilter).keywords
                }

                val alreadyExists = existingKeywords.any { existing ->
                    val parsedExisting = AppSettingsEditor.parseFilterItem(existing)
                    parsedExisting.type.equals(parsedNew.type, ignoreCase = true) &&
                        parsedExisting.text.equals(parsedNew.text, ignoreCase = true)
                }

                if (alreadyExists) {
                    _userMessage.emit("Filter '${parsedNew.text}' already exists")
                    return@safely
                }

                settingsRepository.update { current ->
                    if (targetAppPackage == null) {
                        val existing = current.eligibility.keywordFilter
                        val newKeywords = if (formatted in existing.keywords) existing.keywords else existing.keywords + formatted
                        current.copy(
                            eligibility = current.eligibility.copy(
                                keywordFilter = existing.copy(
                                    enabled = true,
                                    keywords = newKeywords,
                                    mode = matchMode,
                                )
                            )
                        )
                    } else {
                        AppSettingsEditor.addKeyword(current, targetAppPackage, keyword, matchMode, isSender)
                    }
                }
                _userMessage.emit(
                    if (targetAppPackage == null) "Added global ${parsedNew.type.lowercase()} filter for '${parsedNew.text}'"
                    else "Added ${parsedNew.type.lowercase()} filter for '${parsedNew.text}'"
                )
            }
        }
    }

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
            val distinctApps = items.map { it.packageName to it.appName }
            _state.update { current ->
                val mergedApps = (current.availableApps + distinctApps).distinct()
                current.copy(
                    items = if (reset) items else current.items + items,
                    availableApps = mergedApps,
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
