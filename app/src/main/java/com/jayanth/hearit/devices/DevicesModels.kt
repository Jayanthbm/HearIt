package com.jayanth.hearit.devices

import com.jayanth.hearit.core.common.model.AudioOutputSettings

/**
 * One output row on the Devices screen (master prompt §6, phase P14).
 *
 * @property id Device identity; the label is used when the platform exposes no id, and such a
 *   row cannot be allow-listed ([canToggle] false).
 * @property connected True while the platform reports the output as connected.
 * @property allowed Whether an announcement would currently be spoken on this device, after
 *   the category and per-device rules are applied.
 * @property canToggle False for devices without an id and for non-Bluetooth outputs (wired
 *   and speaker are category-wide settings, not per-device ones).
 */
data class DeviceRow(
    val id: String,
    val label: String,
    val isBluetooth: Boolean,
    val connected: Boolean,
    val allowed: Boolean,
    val canToggle: Boolean,
)

/**
 * Immutable state of the Devices feature.
 *
 * @property connected Outputs connected right now.
 * @property remembered Outputs seen before that are not connected right now.
 * @property audioOutput Allowed output categories plus the Bluetooth allow-list.
 * @property unavailable True when the connected-output probe failed.
 */
data class DevicesUiState(
    val loaded: Boolean = false,
    val audioOutput: AudioOutputSettings = AudioOutputSettings(),
    val connected: List<DeviceRow> = emptyList(),
    val remembered: List<DeviceRow> = emptyList(),
    val unavailable: Boolean = false,
    // Ambient noise auto-volume (#34) - moved here from Voice & Speech
    val isAmbientNoiseBoostEnabled: Boolean = false,
    val ambientNoiseThresholdDb: Int = 70,
    val ambientNoiseBoostPercent: Int = 20,
    val hasRecordAudioPermission: Boolean = false,
) {

    /**
     * True when Bluetooth is allowed as a category but every remembered device is unchecked:
     * announcements on Bluetooth will be skipped, which is worth saying out loud.
     */
    val noBluetoothDeviceAllowed: Boolean
        get() = audioOutput.allowBluetooth && audioOutput.allowedBluetoothIds?.isEmpty() == true
}
