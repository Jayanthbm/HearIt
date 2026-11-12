package com.jayanth.hearit.applications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayanth.hearit.core.common.model.AppCategory
import com.jayanth.hearit.core.common.model.AppCategoryChoice
import com.jayanth.hearit.core.common.model.AppPriority
import com.jayanth.hearit.core.common.model.AudioFocusPolicy
import com.jayanth.hearit.core.common.model.HistoryStorageMode
import com.jayanth.hearit.core.common.model.HistoryUpdateHandling
import com.jayanth.hearit.core.common.model.MediaReplacementRule
import com.jayanth.hearit.core.common.model.KeywordMatchMode
import com.jayanth.hearit.core.database.HistoryQuery
import com.jayanth.hearit.core.database.HistoryRepository
import com.jayanth.hearit.core.datastore.HearItSettings
import com.jayanth.hearit.core.tts.AnnouncementSection
import com.jayanth.hearit.core.tts.BuiltAnnouncement
import com.jayanth.hearit.core.tts.SectionKind
import com.jayanth.hearit.core.tts.TextToSpeechEngine
import com.jayanth.hearit.core.tts.UtteranceListener
import com.jayanth.hearit.core.database.HistoryEntry
import com.jayanth.hearit.core.datastore.SettingsRepository
import com.jayanth.hearit.service.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the Applications feature (master prompt §5, phase P13).
 *
 * Holds the catalog, the search query and the open app's settings. All editing goes through
 * [AppSettingsEditor] (pure) and `SettingsRepository`, and the state is rebuilt from the
 * settings flow, so a write can never leave the screen showing a stale inheritance label.
 *
 * Edits apply immediately (no Save button): these are switches and single choices, and the
 * settings repository is the single writer for the pipeline too.
 *
 * Privacy and permissions: the catalog only reads launcher-visible package metadata; no
 * notification content is touched and no permission is requested here.
 */
@HiltViewModel
class ApplicationsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val catalog: ApplicationCatalog,
    private val iconLoader: AppIconLoader,
    private val historyRepository: HistoryRepository,
    private val engine: TextToSpeechEngine,
    private val sessionStore: SessionStore,
) : ViewModel() {

    private val _state = MutableStateFlow(ApplicationsUiState())
    val state: StateFlow<ApplicationsUiState> = _state.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    private var historyObservationJob: Job? = null

    /** Icon source for the list rows; a data source, not a policy holder. */
    val icons: AppIconLoader get() = iconLoader

    private var installedApps: List<AppEntry> = emptyList()
    private var catalogLoaded = false
    private var latestSettings: HearItSettings? = null

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                latestSettings = settings
                val aliases = settings.appOverrides.mapNotNull { (pkg, override) ->
                    override.spokenName?.takeIf { it.isNotBlank() }?.let { pkg to it }
                }.toMap()
                val categoryOverrides = settings.appOverrides.mapNotNull { (pkg, override) ->
                    override.categoryOverride?.let { pkg to it }
                }.toMap()
                val messagingConfigs = settings.appOverrides.mapNotNull { (pkg, override) ->
                    if (override.messagingPolish.inheritsEveryValue) null else pkg to override.messagingPolish
                }.toMap()
                com.jayanth.hearit.core.tts.SmartContentCleaner.replaceAppAliases(aliases)
                com.jayanth.hearit.core.tts.SmartContentCleaner.replaceAppCategories(categoryOverrides)
                com.jayanth.hearit.core.tts.SmartContentCleaner.replaceMessagingPolishConfigs(messagingConfigs)
                publish()
            }
        }
        viewModelScope.launch {
            val loaded = runCatching { catalog.installedApps() }
            installedApps = loaded.getOrDefault(emptyList())
            catalogLoaded = true
            _state.update { it.copy(catalogUnavailable = loaded.isFailure) }
            publish()
        }
    }

    /** Search text; the filter is applied by [AppFilter]. */
    fun onQueryChange(query: String) {
        _state.update { current ->
            current.copy(query = query, visibleApps = AppFilter.filter(current.apps, query))
        }
    }

    /** Enables or disables one app. */
    fun onToggleApp(packageName: String) {
        update { settings -> AppSettingsEditor.toggleEnabled(settings, packageName) }
    }

    /** Enables every currently visible app ("select all" honours search, tab & system filter). */
    fun onSelectAllVisible(packageNames: Collection<String> = _state.value.visibleApps.map { it.packageName }) {
        update { settings -> AppSettingsEditor.enableAll(settings, packageNames) }
    }

    /** Disables every currently visible app. */
    fun onDeselectAllVisible(packageNames: Collection<String> = _state.value.visibleApps.map { it.packageName }) {
        update { settings -> AppSettingsEditor.disableAll(settings, packageNames) }
    }

    /** Toggles Priority Announce quick-access membership from the list or details (#5). */
    fun onTogglePriorityAnnounce(packageName: String) {
        update { settings ->
            val current = settings.priorityAnnounceFor(packageName)
            AppSettingsEditor.setPriorityAnnounceListed(settings, packageName, !current)
        }
    }

    /** Enters or exits multi-select batch mode (#7). */
    fun onToggleSelectionMode(initialPackage: String? = null) {
        _state.update { current ->
            val nextMode = if (initialPackage != null) true else !current.isSelectionMode
            val nextSelected = when {
                !nextMode -> emptySet()
                initialPackage != null -> current.selectedPackages + initialPackage
                else -> current.selectedPackages
            }
            current.copy(isSelectionMode = nextMode, selectedPackages = nextSelected)
        }
    }

    fun onToggleSelectPackage(packageName: String) {
        _state.update { current ->
            val next = if (packageName in current.selectedPackages) {
                current.selectedPackages - packageName
            } else {
                current.selectedPackages + packageName
            }
            current.copy(selectedPackages = next)
        }
    }

    fun onSelectAllForBatch(packageNames: Collection<String>) {
        _state.update { current ->
            val target = packageNames.toSet()
            val allSelected = target.isNotEmpty() && target.all { it in current.selectedPackages }
            current.copy(
                selectedPackages = if (allSelected) current.selectedPackages - target else current.selectedPackages + target,
            )
        }
    }

    fun onClearSelection() {
        _state.update { it.copy(isSelectionMode = false, selectedPackages = emptySet()) }
    }

    fun onBatchSetEnabled(enabled: Boolean) {
        val selected = _state.value.selectedPackages.takeIf { it.isNotEmpty() } ?: return
        update { settings ->
            if (enabled) AppSettingsEditor.enableAll(settings, selected)
            else AppSettingsEditor.disableAll(settings, selected)
        }
        viewModelScope.launch {
            _userMessage.emit("${if (enabled) "Enabled" else "Silenced"} ${selected.size} apps")
        }
        onClearSelection()
    }

    fun onBatchSetPriorityAnnounce(listed: Boolean) {
        val selected = _state.value.selectedPackages.takeIf { it.isNotEmpty() } ?: return
        update { settings -> AppSettingsEditor.batchSetPriorityAnnounce(settings, selected, listed) }
        viewModelScope.launch {
            _userMessage.emit(
                "${if (listed) "Added" else "Removed"} ${selected.size} apps ${if (listed) "to" else "from"} Priority List",
            )
        }
        onClearSelection()
    }

    fun onBatchResetToGlobal() {
        val selected = _state.value.selectedPackages.takeIf { it.isNotEmpty() } ?: return
        update { settings -> AppSettingsEditor.batchResetToGlobal(settings, selected) }
        viewModelScope.launch {
            _userMessage.emit("Reset ${selected.size} apps to global rules")
        }
        onClearSelection()
    }

    /** Opens the per-app settings screen and starts live notification history observation. */
    fun onOpenApp(packageName: String) {
        _state.update { it.copy(openPackage = packageName) }
        publish()
        observeAppHistory(packageName)
    }

    /** Returns to the list. */
    fun onCloseApp() {
        historyObservationJob?.cancel()
        historyObservationJob = null
        onStopPlaying()
        _state.update { it.copy(openPackage = null, settings = null) }
    }

    private fun observeAppHistory(packageName: String) {
        historyObservationJob?.cancel()
        historyObservationJob = viewModelScope.launch {
            _state.update { current ->
                current.copy(settings = current.settings?.copy(historyLoading = true))
            }
            historyRepository.observeByPackage(packageName).collect { entries ->
                _state.update { current ->
                    if (current.openPackage == packageName) {
                        current.copy(
                            settings = current.settings?.copy(
                                recentHistory = entries,
                                historyLoading = false,
                            )
                        )
                    } else current
                }
            }
        }
    }

    fun onPlayHistoryEntry(entry: HistoryEntry) {
        viewModelScope.launch {
            val settings = settingsRepository.current()
            val textToSpeak = entry.messageText ?: entry.sender ?: entry.appName
            val sections = buildList {
                entry.sender?.takeIf { it.isNotBlank() }?.let {
                    add(AnnouncementSection(SectionKind.SENDER, "$it says,"))
                }
                add(AnnouncementSection(SectionKind.MESSAGE, textToSpeak))
            }
            val announcement = BuiltAnnouncement(
                sections = sections,
                pauseBetweenSectionsMillis = settings.format.pauseBetweenSectionsMillis,
            )

            _state.update { current ->
                current.copy(settings = current.settings?.copy(playingHistoryId = entry.id))
            }
            val accepted = engine.speak(
                announcement = announcement,
                settings = settings.speech,
                listener = object : UtteranceListener {
                    override fun onComplete(announcement: BuiltAnnouncement) {
                        _state.update { current ->
                            if (current.settings?.playingHistoryId == entry.id) {
                                current.copy(settings = current.settings?.copy(playingHistoryId = null))
                            } else current
                        }
                    }

                    override fun onError(announcement: BuiltAnnouncement, error: com.jayanth.hearit.core.tts.TtsError) {
                        _state.update { current ->
                            if (current.settings?.playingHistoryId == entry.id) {
                                current.copy(settings = current.settings?.copy(playingHistoryId = null))
                            } else current
                        }
                    }
                },
            )
            if (!accepted) {
                _state.update { current ->
                    if (current.settings?.playingHistoryId == entry.id) {
                        current.copy(settings = current.settings?.copy(playingHistoryId = null))
                    } else current
                }
            }
        }
    }

    fun onStopPlaying() {
        engine.stop()
        _state.update { current ->
            current.copy(settings = current.settings?.copy(playingHistoryId = null))
        }
    }

    fun onDeleteHistoryEntry(id: Long) {
        viewModelScope.launch {
            historyRepository.delete(id)
            _userMessage.emit("Deleted notification record")
        }
    }

    fun onClearAppHistory(olderThanDays: Int? = null) {
        val packageName = _state.value.openPackage ?: return
        viewModelScope.launch {
            val deleted = historyRepository.clearPackageHistory(packageName, olderThanDays)
            val msg = if (deleted == 0) {
                "No records matched to delete"
            } else if (olderThanDays == null) {
                "Cleared all notification history ($deleted records)"
            } else {
                "Cleared $deleted records older than $olderThanDays day${if (olderThanDays > 1) "s" else ""}"
            }
            _userMessage.emit(msg)
        }
    }

    override fun onCleared() {
        super.onCleared()
        historyObservationJob?.cancel()
        engine.stop()
    }

    fun onMessagingPolishEnabledChanged(enabled: Boolean?) {
        updateSelected { settings, packageName ->
            AppSettingsEditor.setMessagingPolishEnabled(settings, packageName, enabled)
        }
    }

    fun onAddMessagingNoisePhrase(phrase: String) {
        updateSelected { settings, packageName ->
            AppSettingsEditor.addMessagingNoisePhrase(settings, packageName, phrase)
        }
    }

    fun onRemoveMessagingNoisePhrase(phrase: String) {
        updateSelected { settings, packageName ->
            AppSettingsEditor.removeMessagingNoisePhrase(settings, packageName, phrase)
        }
    }

    fun onSetMessagingMediaMapping(oldMatch: String?, match: String, spokenPhrase: String) {
        updateSelected { settings, packageName ->
            AppSettingsEditor.setMessagingMediaMapping(
                settings,
                packageName,
                oldMatch,
                MediaReplacementRule(match, spokenPhrase),
            )
        }
    }

    fun onRemoveMessagingMediaMapping(match: String) {
        updateSelected { settings, packageName ->
            AppSettingsEditor.removeMessagingMediaMapping(settings, packageName, match)
        }
    }

    fun onAddFilter(keyword: String, mode: KeywordMatchMode, isSender: Boolean = false) {
        val packageName = _state.value.openPackage ?: return
        val currentFilter = _state.value.settings?.keywordFilter?.effective ?: com.jayanth.hearit.core.common.model.KeywordFilterSettings()

        val formatted = AppSettingsEditor.formatKeyword(keyword, mode, isSender)
        val parsedNew = AppSettingsEditor.parseFilterItem(formatted)

        val alreadyExists = currentFilter.keywords.any { existing ->
            val parsedExisting = AppSettingsEditor.parseFilterItem(existing)
            parsedExisting.type.equals(parsedNew.type, ignoreCase = true) &&
                parsedExisting.text.equals(parsedNew.text, ignoreCase = true)
        }

        if (alreadyExists) {
            viewModelScope.launch {
                _userMessage.emit("Filter '${parsedNew.text}' already exists")
            }
            return
        }

        updateSelected { settings, pkg ->
            AppSettingsEditor.addKeyword(settings, pkg, keyword, mode, isSender)
        }
        viewModelScope.launch {
            _userMessage.emit("Added ${parsedNew.type.lowercase()} filter for '${parsedNew.text}'")
        }
    }

    fun onRemoveFilter(keyword: String) {
        val parsed = AppSettingsEditor.parseFilterItem(keyword)
        updateSelected { settings, packageName ->
            AppSettingsEditor.removeKeyword(settings, packageName, keyword)
        }
        viewModelScope.launch {
            _userMessage.emit("Removed filter '${parsed.text}'")
        }
    }

    fun onFilterModeChanged(mode: KeywordMatchMode) {
        updateSelected { settings, packageName ->
            AppSettingsEditor.setKeywordMatchMode(settings, packageName, mode)
        }
    }

    fun onToggleFilterEnabled(enabled: Boolean) {
        updateSelected { settings, packageName ->
            AppSettingsEditor.toggleKeywordFilterEnabled(settings, packageName, enabled)
        }
    }

    fun onPriorityChanged(priority: AppPriority?) {
        updateSelected { settings, packageName ->
            AppSettingsEditor.setPriority(settings, packageName, priority)
        }
    }

    fun onSpokenNameChanged(spokenName: String?) {
        val targetPackage = _state.value.openPackage ?: return
        com.jayanth.hearit.core.tts.SmartContentCleaner.setAppAlias(targetPackage, spokenName)
        updateSelected { settings, packageName ->
            AppSettingsEditor.setSpokenName(settings, packageName, spokenName)
        }
    }

    fun onSetAppCategory(packageName: String, category: AppCategory?) {
        val choice = AppCategoryChoice.fromCategory(category)
        update { settings ->
            AppSettingsEditor.setCategoryChoice(settings, packageName, choice)
        }
    }

    fun onResetAppCategory(packageName: String) {
        update { settings ->
            AppSettingsEditor.setCategoryChoice(settings, packageName, null)
        }
    }

    fun onHistoryStorageModeChanged(storageMode: HistoryStorageMode?) {
        updateSelected { settings, packageName ->
            AppSettingsEditor.setHistoryStorageMode(settings, packageName, storageMode)
        }
    }

    fun onHistoryUpdateHandlingChanged(updateHandling: HistoryUpdateHandling?) {
        updateSelected { settings, packageName ->
            AppSettingsEditor.setHistoryUpdateHandling(settings, packageName, updateHandling)
        }
    }

    fun onHistoryRetentionChanged(retentionDays: Int?) {
        updateSelected { settings, packageName ->
            AppSettingsEditor.setHistoryRetentionDays(settings, packageName, retentionDays)
        }
    }

    fun onRespectDndChanged(respectDnd: Boolean?) {
        updateSelected { settings, packageName ->
            AppSettingsEditor.setRespectDnd(settings, packageName, respectDnd)
        }
    }

    fun onQueueDuringCallChanged(queueDuringCall: Boolean?) {
        updateSelected { settings, packageName ->
            AppSettingsEditor.setQueueDuringCall(settings, packageName, queueDuringCall)
        }
    }

    fun onAudioFocusChanged(audioFocus: AudioFocusPolicy?) {
        updateSelected { settings, packageName ->
            AppSettingsEditor.setAudioFocus(settings, packageName, audioFocus)
        }
    }

    fun onSpeechRateChanged(speechRate: Float?) {
        updateSelected { settings, packageName ->
            AppSettingsEditor.setSpeechRate(settings, packageName, speechRate)
        }
    }

    fun onSpeechPitchChanged(pitch: Float?) {
        updateSelected { settings, packageName ->
            AppSettingsEditor.setSpeechPitch(settings, packageName, pitch)
        }
    }

    fun onSpeechVoiceChanged(voiceId: String?) {
        updateSelected { settings, packageName ->
            AppSettingsEditor.setSpeechVoiceId(settings, packageName, voiceId)
        }
    }

    fun onAutoDismissChanged(autoDismiss: Boolean?) {
        updateSelected { settings, packageName ->
            AppSettingsEditor.setAutoDismiss(settings, packageName, autoDismiss)
        }
    }

    /** Clears every override of the open app (its announcement switch is kept). */
    fun onResetToGlobal() {
        updateSelected { settings, packageName ->
            AppSettingsEditor.resetToGlobal(settings, packageName)
        }
    }

    // ---- internals -------------------------------------------------------------------

    private fun update(transform: (HearItSettings) -> HearItSettings) {
        viewModelScope.launch {
            settingsRepository.update(transform)
        }
    }

    private fun updateSelected(transform: (HearItSettings, String) -> HearItSettings) {
        val packageName = _state.value.openPackage ?: return
        update { settings -> transform(settings, packageName) }
    }

    /**
     * Rebuilds the list and the open app's settings from the latest catalog + settings.
     *
     * Both sources arrive independently (catalog once, settings on every write), so every
     * publish recomputes from the two cached inputs instead of patching the previous state.
     */
    private fun publish() {
        val settings = latestSettings ?: return
        val merged = AppCatalogMerge.merge(installedApps, settings.enabledApps)
        val categoriesMap = merged.associate { entry ->
            entry.packageName to settings.categoryFor(entry.packageName)
        }
        val prioritySet = merged.filter { settings.priorityAnnounceFor(it.packageName) }
            .map { it.packageName }
            .toSet() + settings.priorityAnnounceApps
        val voices = runCatching { engine.listVoices() }.getOrDefault(emptyList())
        _state.update { current ->
            val openPackage = current.openPackage
            current.copy(
                loading = !catalogLoaded,
                apps = merged,
                visibleApps = AppFilter.filter(merged, current.query),
                enabledApps = settings.enabledApps,
                priorityAnnounceApps = prioritySet,
                customizedApps = settings.appOverrides.filterValues { !it.inheritsEveryValue }.keys,
                appCategories = categoriesMap,
                availableVoices = voices,
                settings = openPackage?.let { packageName ->
                    val previousHistory = current.settings?.recentHistory ?: emptyList()
                    val previousLoading = current.settings?.historyLoading ?: false
                    AppSettingsEditor.settingsFor(
                        settings = settings,
                        packageName = packageName,
                        label = merged.firstOrNull { it.packageName == packageName }?.label ?: packageName,
                        globalAutoDismiss = sessionStore.isAutoDismissEnabled,
                        availableVoices = voices,
                    ).copy(
                        recentHistory = previousHistory,
                        historyLoading = previousLoading,
                    )
                },
            )
        }
    }
}
