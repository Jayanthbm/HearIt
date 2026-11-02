package com.jayanth.hearit.dashboard

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
import com.jayanth.hearit.service.SessionStore
import com.jayanth.hearit.setup.AudioOutputProbe
import com.jayanth.hearit.setup.TestAnnouncementState
import dagger.hilt.android.lifecycle.HiltViewModel
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

    /** Reads the current temporary mute state and refreshes the dashboard. Called when the screen resumes. */
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
    fun onTemporaryMuteClicked(durationMin: Int) {
        val intent = when (durationMin) {
            15 -> android.content.Intent(AnnouncementService.ACTION_TEMPORARY_MUTE_15MIN)
            30 -> android.content.Intent(AnnouncementService.ACTION_TEMPORARY_MUTE_30MIN)
            60 -> android.content.Intent(AnnouncementService.ACTION_TEMPORARY_MUTE_60MIN)
            else -> android.content.Intent(AnnouncementService.ACTION_STOP)
        }
        intent.setClass(context, AnnouncementService::class.java)
        context.startService(intent)
    }

    /** Clears the temporary mute state. */
    fun onTemporaryMuteCleared() {
        val intent = android.content.Intent(AnnouncementService.ACTION_TEMPORARY_MUTE_TOGGLE)
        intent.setClass(context, AnnouncementService::class.java)
        context.startService(intent)
    }

    /** Explicit Start (master prompt §4): the service itself reports back through the monitor. */
    fun onStartService() = serviceControl.startAnnouncements()

    /** Explicit Stop (master prompt §4). */
    fun onStopService() = serviceControl.stopAnnouncements()

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
     * History recording is not wired into the pipeline yet (P18), so this is genuinely
     * empty on a fresh install — the empty copy says that rather than pretending. A read
     * failure is surfaced as [DashboardUiState.historyUnavailable] instead of being
     * swallowed, because an unreadable database is something the user should see.
     */
    private fun loadRecentHistory() {
        viewModelScope.launch {
            try {
                val entries = historyRepository.page(HistoryQuery(limit = RECENT_HISTORY_LIMIT))
                _state.update { current ->
                    current.copy(
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
            } catch (error: RuntimeException) {
                _state.update {
                    it.copy(recentHistory = emptyList(), recentHistoryLoaded = true, historyUnavailable = true)
                }
            }
        }
    }

    companion object {

        /** Rows shown in the dashboard summary; the history feature owns paging (P18). */
        const val RECENT_HISTORY_LIMIT: Int = 5
    }
}
