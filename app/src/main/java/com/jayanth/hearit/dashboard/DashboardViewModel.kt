package com.jayanth.hearit.dashboard

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayanth.hearit.core.database.HistoryQuery
import com.jayanth.hearit.core.database.HistoryRepository
import com.jayanth.hearit.core.datastore.SettingsRepository
import com.jayanth.hearit.core.tts.AnnouncementSection
import com.jayanth.hearit.core.tts.BuiltAnnouncement
import com.jayanth.hearit.core.tts.SectionKind
import com.jayanth.hearit.core.tts.SpeechSettings
import com.jayanth.hearit.core.tts.TextToSpeechEngine
import com.jayanth.hearit.core.tts.TtsError
import com.jayanth.hearit.core.tts.UtteranceListener
import com.jayanth.hearit.service.AnnouncementService
import com.jayanth.hearit.service.SessionStore
import com.jayanth.hearit.setup.AudioOutputProbe
import com.jayanth.hearit.setup.TestAnnouncementState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the dashboard (master prompt §13, phase P12).
 *
 * Reads live settings observations and the service-status flow, loads the newest history
 * rows, and exposes explicit start/stop/test events. It never starts the service on its
 * own: only [onStartService] (the Start button) does that, and only the service reports
 * itself running afterwards.
 *
 * Privacy: the dashboard state carries app names and outcomes from history, never message
 * text or senders; nothing is logged here (master prompt §17).
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val historyRepository: HistoryRepository,
    private val sessionStore: SessionStore,
    private val serviceStatus: ServiceStatusMonitor,
    private val serviceControl: ServiceControl,
    private val audioOutputs: AudioOutputProbe,
    private val engine: TextToSpeechEngine,
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update { current ->
                    current.copy(
                        loaded = true,
                        enabledAppCount = settings.enabledApps.size,
                        audioOutput = settings.eligibility.audioOutput,
                    )
                }
            }
        }
        viewModelScope.launch {
            serviceStatus.state.collect { snapshot ->
                _state.update { current -> current.copy(service = snapshot) }
            }
        }
        onRefresh()
    }

    /**
     * Re-reads the values that have no reactive source yet (session mute flag, connected
     * outputs) and reloads the history summary. Called when the screen resumes.
     */
    fun onRefresh() {
        _state.update {
            it.copy(
                temporaryMuteActive = sessionStore.isTemporaryMuteActive,
                outputLabel = audioOutputs.connectedOutputs().firstOrNull()?.label,
            )
        }
        loadRecentHistory()
    }

    /** Sets temporary mute for the given duration in minutes (P16). */
    fun onTemporaryMuteClicked(durationMinutes: Int) {
        val action = when (durationMinutes) {
            15 -> AnnouncementService.ACTION_TEMPORARY_MUTE_15MIN
            30 -> AnnouncementService.ACTION_TEMPORARY_MUTE_30MIN
            60 -> AnnouncementService.ACTION_TEMPORARY_MUTE_60MIN
            else -> AnnouncementService.ACTION_TEMPORARY_MUTE_TOGGLE
        }
        sendServiceAction(action)
        _state.update { it.copy(temporaryMuteActive = true) }
    }

    /** Clears the temporary mute state. */
    fun onTemporaryMuteCleared() {
        sendServiceAction(AnnouncementService.ACTION_TEMPORARY_MUTE_TOGGLE)
        _state.update { it.copy(temporaryMuteActive = false) }
    }

    /** Toggles Priority Announce on/off (P17). */
    fun onPriorityAnnounceToggled(active: Boolean) {
        sendServiceAction(
            if (active) {
                AnnouncementService.ACTION_PRIORITY_ANNOUNCE_ON
            } else {
                AnnouncementService.ACTION_PRIORITY_ANNOUNCE_OFF
            },
        )
    }

    /** Explicit Start (master prompt §4): the service reports back through the monitor. */
    fun onStartService() = serviceControl.startAnnouncements()

    /** Explicit Stop (master prompt §4). */
    fun onStopService() = serviceControl.stopAnnouncements()

    /** Searches history with the dashboard's filter state (P18). */
    fun onHistorySearch(filter: DashboardFilterState) {
        viewModelScope.launch {
            val entries = readHistory(toQuery(filter))
            entries?.let { publishHistory(it) } ?: run { }
        }
    }

    /** Clears all history. The screen asks for confirmation before calling this (P18). */
    fun onHistoryClearAll() {
        viewModelScope.launch {
            runCatching { historyRepository.deleteAll() }
                .onSuccess { loadRecentHistory() }
                .onFailure {
                    _state.update { it.copy(historyUnavailable = true, recentHistoryLoaded = true) }
                }
        }
    }

    /**
     * Speaks the fixed test announcement through the configured engine.
     *
     * Like the setup test, it bypasses the queue and history entirely, so it can never
     * create a history entry or disturb a pending announcement (master prompt §14).
     */
    fun onTestAnnouncement(text: String) {
        val announcement = BuiltAnnouncement(
            sections = listOf(AnnouncementSection(SectionKind.MESSAGE, text)),
            pauseBetweenSectionsMillis = 0L,
        )
        _state.update { it.copy(testAnnouncement = TestAnnouncementState.SPEAKING) }

        val accepted = engine.speak(
            announcement = announcement,
            settings = SpeechSettings(),
            listener = object : UtteranceListener {
                override fun onComplete(announcement: BuiltAnnouncement) {
                    _state.update { it.copy(testAnnouncement = TestAnnouncementState.COMPLETED) }
                }

                override fun onError(announcement: BuiltAnnouncement, error: TtsError) {
                    _state.update { it.copy(testAnnouncement = TestAnnouncementState.FAILED) }
                }
            },
        )
        if (!accepted) {
            _state.update { it.copy(testAnnouncement = TestAnnouncementState.FAILED) }
        }
    }

    /**
     * Loads the newest history rows for the dashboard summary.
     *
     * The service records every pipeline outcome (L3), so this is empty only when nothing
     * has been announced yet. A read failure is surfaced as
     * [DashboardUiState.historyUnavailable] instead of being swallowed, because an
     * unreadable database is something the user should see.
     */
    private fun loadRecentHistory() {
        viewModelScope.launch {
            val entries = readHistory(HistoryQuery(limit = RECENT_HISTORY_LIMIT))
            entries?.let { publishHistory(it) } ?: run { }
        }
    }

    /** Reads one page, reporting failure through state instead of throwing. */
    private suspend fun readHistory(query: HistoryQuery) =
        runCatching { historyRepository.page(query) }.getOrElse { error ->
            _state.update {
                it.copy(
                    recentHistory = emptyList(),
                    recentHistoryLoaded = true,
                    historyUnavailable = true,
                )
            }
            null
        }

    private fun publishHistory(entries: List<com.jayanth.hearit.core.database.HistoryEntry>) {
        _state.update {
            it.copy(
                recentHistory = entries.map { entry ->
                    RecentAnnouncement(
                        id = entry.id,
                        appName = entry.appName,
                        status = entry.status,
                        recordedAtMillis = entry.recordedAtMillis,
                    )
                },
                recentHistoryLoaded = true,
                historyUnavailable = false,
            )
        }
    }

    /**
     * Maps the UI filter onto the database query.
     *
     * The free-text query goes to `searchText`, which matches app name, sender or stored
     * message text — the same field the History screen searches, so both surfaces agree.
     */
    private fun toQuery(filter: DashboardFilterState): HistoryQuery = HistoryQuery(
        packageName = filter.filterPackageName,
        status = filter.filterStatus,
        appName = filter.filterAppName,
        searchText = filter.query.trim().takeIf { it.isNotEmpty() },
        sender = filter.filterSender,
        dateFrom = filter.filterDateFrom,
        dateTo = filter.filterDateTo,
        sortBy = when (filter.filterSortBy) {
            DashboardSortBy.RecentFirst -> HistoryQuery.SortBy.RecentFirst
            DashboardSortBy.OldestFirst -> HistoryQuery.SortBy.OldestFirst
            DashboardSortBy.ByApp -> HistoryQuery.SortBy.ByApp
        },
    )

    private fun sendServiceAction(action: String) {
        context.startService(Intent(context, AnnouncementService::class.java).setAction(action))
    }

    companion object {

        /** Rows shown in the dashboard summary; the history feature owns paging. */
        const val RECENT_HISTORY_LIMIT: Int = 5

        /** Number of rows per page when browsing history (P18). */
        const val HISTORY_PAGE_SIZE: Int = 20
    }
}
