package com.jayanth.hearit.devices

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayanth.hearit.core.common.model.AudioOutputKind
import com.jayanth.hearit.core.common.model.AudioOutputSettings
import com.jayanth.hearit.core.datastore.HearItSettings
import com.jayanth.hearit.core.datastore.SettingsRepository
import com.jayanth.hearit.service.AmbientNoiseSampler
import com.jayanth.hearit.service.SessionStore
import com.jayanth.hearit.setup.AudioOutputProbe
import com.jayanth.hearit.setup.DetectedOutput
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the Devices feature (master prompt §6, phase P14).
 *
 * Reads the connected outputs through [AudioOutputProbe], remembers them in settings, and
 * writes every allow/forget decision back through [SettingsRepository] — the same store the
 * eligibility rules read, so what the screen shows is what the pipeline enforces.
 *
 * Privacy: only platform device ids and names are touched; no notification content, no
 * permissions requested and nothing leaves the device.
 */
@HiltViewModel
class DevicesViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val audioOutputs: AudioOutputProbe,
    private val sessionStore: SessionStore,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(
        DevicesUiState(
            isAmbientNoiseBoostEnabled = sessionStore.isAmbientNoiseBoostEnabled &&
                AmbientNoiseSampler.hasRecordAudioPermission(context),
            ambientNoiseThresholdDb = sessionStore.ambientNoiseThresholdDb,
            ambientNoiseBoostPercent = sessionStore.ambientNoiseBoostPercent,
            hasRecordAudioPermission = AmbientNoiseSampler.hasRecordAudioPermission(context),
        ),
    )
    val state: StateFlow<DevicesUiState> = _state.asStateFlow()

    private var connected: List<DetectedOutput> = emptyList()
    private var latestSettings: HearItSettings? = null

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                latestSettings = settings
                publish(settings)
            }
        }
        onRefresh()
    }

    /** Re-reads the connected outputs and remembers any new ones. */
    fun onRefresh() {
        viewModelScope.launch {
            val detected = try {
                audioOutputs.connectedOutputs()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update { it.copy(loaded = true, unavailable = true) }
                return@launch
            }
            connected = detected

            val settings = latestSettings
            if (settings == null) {
                _state.update { it.copy(loaded = true, unavailable = false) }
                return@launch
            }
            val merged = DeviceSelectionEditor.mergeRemembered(settings.knownDevices, detected)
            if (merged != settings.knownDevices) {
                // Persist only when something actually changed, so the settings flow does not
                // re-emit on every resume.
                settingsRepository.update { it.copy(knownDevices = merged) }
            }
            publish(settings)
        }
    }

    /** Turns one output category on or off. */
    fun onCategoryAllowed(kind: AudioOutputKind, allowed: Boolean) {
        updateAudio { audio, _ -> DeviceSelectionEditor.setCategoryAllowed(audio, kind, allowed) }
    }

    /** Allows or disallows one Bluetooth device. */
    fun onDeviceAllowed(deviceId: String, allowed: Boolean) {
        updateAudio { audio, settings ->
            DeviceSelectionEditor.toggleDeviceAllowed(
                audio = audio,
                deviceId = deviceId,
                allowed = allowed,
                knownBluetoothIds = settings.knownDevices.filter { it.isBluetooth }.map { it.id }.toSet(),
            )
        }
    }

    /** Removes one device from the remembered list and from the allow-list. */
    fun onForgetDevice(deviceId: String) {
        viewModelScope.launch {
            settingsRepository.update { settings ->
                DeviceSelectionEditor.forgetDevice(settings, deviceId)
            }
        }
    }

    fun refreshRecordAudioPermission() {
        val granted = AmbientNoiseSampler.hasRecordAudioPermission(context)
        _state.update { current ->
            current.copy(
                hasRecordAudioPermission = granted,
                isAmbientNoiseBoostEnabled = current.isAmbientNoiseBoostEnabled && granted,
            )
        }
    }

    /** Turns ambient auto-volume on or off; it stays off without microphone permission. */
    fun onToggleAmbientNoiseBoost(enabled: Boolean) {
        val granted = AmbientNoiseSampler.hasRecordAudioPermission(context)
        val effective = enabled && granted
        sessionStore.isAmbientNoiseBoostEnabled = effective
        _state.update {
            it.copy(hasRecordAudioPermission = granted, isAmbientNoiseBoostEnabled = effective)
        }
    }

    fun onAmbientNoiseThresholdChanged(db: Int) {
        sessionStore.ambientNoiseThresholdDb = db
        _state.update { it.copy(ambientNoiseThresholdDb = db) }
    }

    fun onAmbientNoiseBoostPercentChanged(percent: Int) {
        sessionStore.ambientNoiseBoostPercent = percent
        _state.update { it.copy(ambientNoiseBoostPercent = percent) }
    }

    // ---- internals ---------------------------------------------------------------------

    private fun updateAudio(
        transform: (AudioOutputSettings, HearItSettings) -> AudioOutputSettings,
    ) {
        viewModelScope.launch {
            settingsRepository.update { settings ->
                settings.copy(
                    eligibility = settings.eligibility.copy(
                        audioOutput = transform(settings.eligibility.audioOutput, settings),
                    ),
                )
            }
        }
    }

    /** Case/whitespace-insensitive key so two rows never show the same visible name. */
    private fun normalizedLabel(label: String): String =
        label.trim().lowercase().replace(Regex("\\s+"), " ")

    /**
     * Collapses rows that would render with the same name. When several ids share a label the
     * allowed row wins so the switch does not claim a device is muted while a twin is allowed.
     */
    private fun distinctByLabel(rows: List<DeviceRow>): List<DeviceRow> =
        rows.groupBy { normalizedLabel(it.label) }
            .values
            .map { group -> group.firstOrNull { it.allowed } ?: group.first() }

    /** Rebuilds both lists from the cached connected outputs and the latest settings. */
    private fun publish(settings: HearItSettings) {
        val audio = settings.eligibility.audioOutput

        val connectedRows = connected.map { output ->
            val id = output.id
            DeviceRow(
                id = id ?: output.label,
                label = output.label,
                isBluetooth = output.isBluetooth,
                connected = true,
                allowed = when {
                    id == null -> true
                    output.isBluetooth -> DeviceSelectionEditor.isDeviceAllowed(audio, id)
                    else -> audio.allowWired
                },
                canToggle = id != null && output.isBluetooth,
            )
        }
        val uniqueConnectedRows = distinctByLabel(connectedRows)
        val connectedIds = connectedRows.map { it.id }.toSet()
        val connectedLabels = uniqueConnectedRows.map { normalizedLabel(it.label) }.toSet()

        val rememberedRows = distinctByLabel(
            settings.knownDevices
                .filterNot { it.id in connectedIds }
                .map { device ->
                    DeviceRow(
                        id = device.id,
                        label = device.label,
                        isBluetooth = device.isBluetooth,
                        connected = false,
                        allowed = if (device.isBluetooth) {
                            DeviceSelectionEditor.isDeviceAllowed(audio, device.id)
                        } else {
                            audio.allowWired
                        },
                        canToggle = device.isBluetooth,
                    )
                },
        ).filterNot { normalizedLabel(it.label) in connectedLabels }

        _state.update { current ->
            current.copy(
                loaded = true,
                audioOutput = audio,
                connected = uniqueConnectedRows,
                remembered = rememberedRows,
            )
        }
    }
}
