package com.jayanth.hearit.applications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayanth.hearit.core.common.model.AppPriority
import com.jayanth.hearit.core.common.model.AudioFocusPolicy
import com.jayanth.hearit.core.common.model.HistoryStorageMode
import com.jayanth.hearit.core.common.model.HistoryUpdateHandling
import com.jayanth.hearit.core.datastore.HearItSettings
import com.jayanth.hearit.core.datastore.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
) : ViewModel() {

    private val _state = MutableStateFlow(ApplicationsUiState())
    val state: StateFlow<ApplicationsUiState> = _state.asStateFlow()

    /** Icon source for the list rows; a data source, not a policy holder. */
    val icons: AppIconLoader get() = iconLoader

    private var installedApps: List<AppEntry> = emptyList()
    private var catalogLoaded = false
    private var latestSettings: HearItSettings? = null

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                latestSettings = settings
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

    /** Enables every currently visible app ("select all" honours the search). */
    fun onSelectAllVisible() {
        val visible = _state.value.visibleApps.map { it.packageName }
        update { settings -> AppSettingsEditor.enableAll(settings, visible) }
    }

    /** Disables every currently visible app. */
    fun onDeselectAllVisible() {
        val visible = _state.value.visibleApps.map { it.packageName }
        update { settings -> AppSettingsEditor.disableAll(settings, visible) }
    }

    /** Opens the per-app settings screen. */
    fun onOpenApp(packageName: String) {
        _state.update { it.copy(openPackage = packageName) }
        publish()
    }

    /** Returns to the list. */
    fun onCloseApp() {
        _state.update { it.copy(openPackage = null, settings = null) }
    }

    fun onPriorityChanged(priority: AppPriority?) {
        updateSelected { settings, packageName ->
            AppSettingsEditor.setPriority(settings, packageName, priority)
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
        _state.update { current ->
            val openPackage = current.openPackage
            current.copy(
                loading = !catalogLoaded,
                apps = merged,
                visibleApps = AppFilter.filter(merged, current.query),
                enabledApps = settings.enabledApps,
                settings = openPackage?.let { packageName ->
                    AppSettingsEditor.settingsFor(
                        settings = settings,
                        packageName = packageName,
                        label = merged.firstOrNull { it.packageName == packageName }?.label ?: packageName,
                    )
                },
            )
        }
    }
}
