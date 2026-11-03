package com.jayanth.hearit.devices

import com.jayanth.hearit.core.common.model.AudioOutputKind
import com.jayanth.hearit.core.common.model.AudioOutputSettings
import com.jayanth.hearit.core.common.model.KnownAudioDevice
import com.jayanth.hearit.core.datastore.HearItSettings
import com.jayanth.hearit.setup.DetectedOutput

/**
 * Pure device-selection edits over [HearItSettings] (master prompt §6, phase P14).
 *
 * The only non-obvious rule is the meaning of [AudioOutputSettings.allowedBluetoothIds]:
 * `null` means "any Bluetooth output", a set means "only these". This object converts
 * between the two without ever leaving the user with a rule they cannot see the meaning of:
 * re-allowing the last device collapses the set back to `null` (any), so a headset connected
 * tomorrow is not silently excluded.
 *
 * Every function is a pure transform, so the ViewModel only hands them to
 * `SettingsRepository.update` and the screen never mutates settings itself.
 */
object DeviceSelectionEditor {

    /** Remembered devices are a convenience list, not an unbounded log. */
    const val MAX_REMEMBERED_DEVICES: Int = 12

    /**
     * Merges the currently connected outputs into the remembered list.
     *
     * Connected devices come first (newest label wins), remembered ones follow in their
     * previous order, and outputs without an id are skipped because they could never be
     * allow-listed again.
     */
    fun mergeRemembered(
        remembered: List<KnownAudioDevice>,
        connected: List<DetectedOutput>,
        limit: Int = MAX_REMEMBERED_DEVICES,
    ): List<KnownAudioDevice> {
        val connectedKnown = connected.mapNotNull { output ->
            val id = output.id?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            KnownAudioDevice(
                id = id,
                label = output.label.ifBlank { id },
                isBluetooth = output.isBluetooth,
            )
        }
        return (connectedKnown + remembered).distinctBy { it.id }.take(limit)
    }

    /** True when [deviceId] is allowed by the current Bluetooth allow-list. */
    fun isDeviceAllowed(audio: AudioOutputSettings, deviceId: String): Boolean =
        audio.allowedBluetoothIds?.contains(deviceId) ?: true

    /**
     * Allows or disallows one Bluetooth device.
     *
     * @param knownBluetoothIds Every remembered Bluetooth device id, so the allow-list can be
     *   materialised from "any" when the first device is switched off.
     */
    fun toggleDeviceAllowed(
        audio: AudioOutputSettings,
        deviceId: String,
        allowed: Boolean,
        knownBluetoothIds: Set<String>,
    ): AudioOutputSettings {
        val universe = knownBluetoothIds + deviceId
        val current = audio.allowedBluetoothIds ?: universe
        val next = if (allowed) current + deviceId else current - deviceId
        return audio.copy(allowedBluetoothIds = if (next.containsAll(universe)) null else next)
    }

    /** Turns one output category on or off. */
    fun setCategoryAllowed(
        audio: AudioOutputSettings,
        kind: AudioOutputKind,
        allowed: Boolean,
    ): AudioOutputSettings = when (kind) {
        AudioOutputKind.WIRED -> audio.copy(allowWired = allowed)
        AudioOutputKind.BLUETOOTH -> audio.copy(allowBluetooth = allowed)
        AudioOutputKind.PHONE_SPEAKER -> audio.copy(allowPhoneSpeaker = allowed)
        AudioOutputKind.OTHER -> audio.copy(allowUnidentifiedOutput = allowed)
    }

    /** Forgets one remembered device and drops it from the Bluetooth allow-list. */
    fun forgetDevice(settings: HearItSettings, deviceId: String): HearItSettings {
        val audio = settings.eligibility.audioOutput
        return settings.copy(
            knownDevices = settings.knownDevices.filterNot { it.id == deviceId },
            eligibility = settings.eligibility.copy(
                audioOutput = audio.copy(
                    allowedBluetoothIds = audio.allowedBluetoothIds?.minus(deviceId),
                ),
            ),
        )
    }
}
