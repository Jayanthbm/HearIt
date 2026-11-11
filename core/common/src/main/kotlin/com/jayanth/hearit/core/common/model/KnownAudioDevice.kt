package com.jayanth.hearit.core.common.model

/**
 * One audio output HearIt has seen, remembered so the Devices feature can keep listing it
 * after it disconnects (master prompt §6, phase P14).
 *
 * Only the platform-provided identity and display name are kept — no notification content,
 * nothing off the device. Outputs the platform gives no id for are never remembered,
 * because they could not be allow-listed again later.
 *
 * @property id Stable device address/id; the value matched against
 *   [AudioOutputSettings.allowedBluetoothIds].
 * @property label Display name captured the last time the device was seen.
 * @property isBluetooth True for Bluetooth outputs — the only kind with per-device selection
 *   (wired and speaker are category-wide settings).
 */
data class KnownAudioDevice(
    val id: String,
    val label: String,
    val isBluetooth: Boolean,
) {
    init {
        require(id.isNotBlank()) { "KnownAudioDevice.id must not be blank" }
    }
}
