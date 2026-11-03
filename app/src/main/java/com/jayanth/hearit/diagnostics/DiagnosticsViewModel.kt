package com.jayanth.hearit.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayanth.hearit.core.database.HistoryRepository
import com.jayanth.hearit.core.datastore.SettingsRepository
import com.jayanth.hearit.core.tts.TextToSpeechEngine
import com.jayanth.hearit.dashboard.ServiceStatusMonitor
import com.jayanth.hearit.setup.AudioOutputProbe
import com.jayanth.hearit.setup.BluetoothPermissionProbe
import com.jayanth.hearit.setup.NotificationAccessProbe
import com.jayanth.hearit.setup.PostNotificationProbe
import com.jayanth.hearit.setup.SetupLauncher
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel aggregating real-time diagnostics from system probes, service monitor, and repositories.
 *
 * Master prompt §13 and phase P19.
 */
@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val serviceStatusMonitor: ServiceStatusMonitor,
    private val notificationAccess: NotificationAccessProbe,
    private val postNotifications: PostNotificationProbe,
    private val bluetoothPermission: BluetoothPermissionProbe,
    private val audioOutputs: AudioOutputProbe,
    private val engine: TextToSpeechEngine,
    private val settingsRepository: SettingsRepository,
    private val historyRepository: HistoryRepository,
    private val setupLauncher: SetupLauncher,
) : ViewModel() {

    private val _state = MutableStateFlow(DiagnosticsUiState())
    val state: StateFlow<DiagnosticsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            serviceStatusMonitor.state.collect { snapshot ->
                _state.update { it.copy(serviceSnapshot = snapshot) }
            }
        }
        refresh()
        ensureTtsPolling()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }

            val settings = settingsRepository.current()
            val historyCount = historyRepository.count().toInt()
            val voices = engine.listVoices()
            val isTtsAvail = engine.isAvailable()
            val defaultLocale = Locale.getDefault()
            val localeSupported = isTtsAvail && engine.supportsLocale(defaultLocale)
            val outputs = audioOutputs.connectedOutputs()

            _state.update { current ->
                current.copy(
                    isNotificationAccessGranted = notificationAccess.isGranted(),
                    isPostNotificationGranted = postNotifications.isGranted(),
                    isBluetoothPermissionGranted = bluetoothPermission.isGranted(),
                    isBluetoothPermissionRequired = bluetoothPermission.isRequired,
                    isTtsAvailable = isTtsAvail,
                    isTtsLocaleSupported = localeSupported,
                    installedVoiceCount = voices.size,
                    defaultLocaleLanguage = defaultLocale.displayName,
                    connectedOutputs = outputs,
                    enabledAppsCount = settings.enabledApps.size,
                    priorityAppsCount = settings.priorityAnnounceApps.size,
                    totalHistoryCount = historyCount,
                    isRefreshing = false,
                )
            }
            if (!isTtsAvail) {
                ensureTtsPolling()
            }
        }
    }

    fun openNotificationAccessSettings() {
        setupLauncher.openNotificationAccessSettings()
    }

    fun openBatteryOptimizationSettings() {
        setupLauncher.openBatteryOptimizationSettings()
    }

    private fun ensureTtsPolling() {
        if (engine.isAvailable()) return
        viewModelScope.launch {
            repeat(10) {
                delay(500L)
                if (engine.isAvailable()) {
                    val voices = engine.listVoices()
                    val defaultLocale = Locale.getDefault()
                    val localeSupported = engine.supportsLocale(defaultLocale)
                    _state.update { current ->
                        current.copy(
                            isTtsAvailable = true,
                            isTtsLocaleSupported = localeSupported,
                            installedVoiceCount = voices.size,
                        )
                    }
                    return@launch
                }
            }
        }
    }
}
