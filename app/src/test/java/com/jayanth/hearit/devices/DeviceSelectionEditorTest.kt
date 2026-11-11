package com.jayanth.hearit.devices

import com.jayanth.hearit.core.common.model.AudioOutputKind
import com.jayanth.hearit.core.common.model.AudioOutputSettings
import com.jayanth.hearit.core.common.model.EligibilitySettings
import com.jayanth.hearit.core.common.model.KnownAudioDevice
import com.jayanth.hearit.core.datastore.HearItSettings
import com.jayanth.hearit.setup.DetectedOutput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P14: the Devices screen must never leave the allow-list in a state the user cannot
 * understand — "any device" (`null`) or an explicit set of ids, never both meanings.
 */
class DeviceSelectionEditorTest {

    private val car = KnownAudioDevice("car", "Car kit", isBluetooth = true)
    private val buds = KnownAudioDevice("buds", "Earbuds", isBluetooth = true)
    private val usb = KnownAudioDevice("usb", "USB headset", isBluetooth = false)
    private val knownBluetoothIds = setOf("car", "buds")

    // ---- remembering devices ----------------------------------------------------------

    @Test
    fun mergeRemembered_putsConnectedFirstWithFreshLabels() {
        val merged = DeviceSelectionEditor.mergeRemembered(
            remembered = listOf(car, buds, usb),
            connected = listOf(DetectedOutput("buds", "Earbuds Pro", isBluetooth = true)),
        )

        assertEquals(listOf("buds", "car", "usb"), merged.map { it.id })
        assertEquals("Earbuds Pro", merged.first().label)
    }

    @Test
    fun mergeRemembered_skipsOutputsWithoutId() {
        val merged = DeviceSelectionEditor.mergeRemembered(
            remembered = listOf(car),
            connected = listOf(DetectedOutput(null, "Unknown route", isBluetooth = true)),
        )

        assertEquals(listOf("car"), merged.map { it.id })
    }

    @Test
    fun mergeRemembered_keepsDevicesThatAreNoLongerConnected() {
        val merged = DeviceSelectionEditor.mergeRemembered(
            remembered = listOf(car),
            connected = emptyList(),
        )

        assertEquals(listOf("car"), merged.map { it.id })
    }

    @Test
    fun mergeRemembered_neverDuplicatesADevice() {
        val merged = DeviceSelectionEditor.mergeRemembered(
            remembered = listOf(car, buds),
            connected = listOf(DetectedOutput("car", "Car kit", isBluetooth = true)),
        )

        assertEquals(listOf("car", "buds"), merged.map { it.id })
    }

    @Test
    fun mergeRemembered_capsTheList() {
        val many = (1..15).map { KnownAudioDevice("id$it", "Device $it", isBluetooth = true) }

        val merged = DeviceSelectionEditor.mergeRemembered(remembered = many, connected = emptyList())

        assertEquals(DeviceSelectionEditor.MAX_REMEMBERED_DEVICES, merged.size)
        assertEquals("id1", merged.first().id)
    }

    // ---- per-device allow-list --------------------------------------------------------

    @Test
    fun isDeviceAllowed_nullAllowListAllowsEveryDevice() {
        assertTrue(DeviceSelectionEditor.isDeviceAllowed(AudioOutputSettings(), "anything"))
    }

    @Test
    fun toggleOff_oneOfTwo_materialisesTheAllowList() {
        val audio = DeviceSelectionEditor.toggleDeviceAllowed(
            audio = AudioOutputSettings(),
            deviceId = "buds",
            allowed = false,
            knownBluetoothIds = knownBluetoothIds,
        )

        assertEquals(setOf("car"), audio.allowedBluetoothIds)
    }

    @Test
    fun toggleOn_theLastMissingDevice_collapsesBackToAny() {
        val audio = DeviceSelectionEditor.toggleDeviceAllowed(
            audio = AudioOutputSettings(allowedBluetoothIds = setOf("car")),
            deviceId = "buds",
            allowed = true,
            knownBluetoothIds = knownBluetoothIds,
        )

        assertNull(audio.allowedBluetoothIds)
    }

    @Test
    fun toggleOff_everyDevice_leavesAnEmptyAllowList() {
        var audio = AudioOutputSettings()
        audio = DeviceSelectionEditor.toggleDeviceAllowed(audio, "car", false, knownBluetoothIds)
        audio = DeviceSelectionEditor.toggleDeviceAllowed(audio, "buds", false, knownBluetoothIds)

        assertEquals(emptySet<String>(), audio.allowedBluetoothIds)
    }

    @Test
    fun toggleOn_anAlreadyAllowedDevice_keepsAny() {
        val audio = DeviceSelectionEditor.toggleDeviceAllowed(
            audio = AudioOutputSettings(),
            deviceId = "car",
            allowed = true,
            knownBluetoothIds = knownBluetoothIds,
        )

        assertNull(audio.allowedBluetoothIds)
    }

    // ---- forgetting and categories ----------------------------------------------------

    @Test
    fun forgetDevice_removesItFromMemoryAndFromTheAllowList() {
        val settings = HearItSettings(
            knownDevices = listOf(car, buds),
            eligibility = EligibilitySettings(
                audioOutput = AudioOutputSettings(allowedBluetoothIds = setOf("car", "buds")),
            ),
        )

        val updated = DeviceSelectionEditor.forgetDevice(settings, "car")

        assertEquals(listOf("buds"), updated.knownDevices.map { it.id })
        assertEquals(setOf("buds"), updated.eligibility.audioOutput.allowedBluetoothIds)
    }

    @Test
    fun forgetDevice_keepsEveryCategoryFlag() {
        val settings = HearItSettings(
            eligibility = EligibilitySettings(
                audioOutput = AudioOutputSettings(
                    allowWired = false,
                    allowBluetooth = true,
                    allowPhoneSpeaker = true,
                    allowUnidentifiedOutput = false,
                ),
            ),
        )

        val audio = DeviceSelectionEditor.forgetDevice(settings, "car").eligibility.audioOutput

        assertFalse(audio.allowWired)
        assertTrue(audio.allowBluetooth)
        assertTrue(audio.allowPhoneSpeaker)
        assertFalse(audio.allowUnidentifiedOutput)
    }

    @Test
    fun setCategoryAllowed_touchesOnlyTheSelectedCategory() {
        val base = AudioOutputSettings()

        assertEquals(false, DeviceSelectionEditor.setCategoryAllowed(base, AudioOutputKind.WIRED, false).allowWired)
        assertEquals(false, DeviceSelectionEditor.setCategoryAllowed(base, AudioOutputKind.BLUETOOTH, false).allowBluetooth)
        assertEquals(
            true,
            DeviceSelectionEditor.setCategoryAllowed(base, AudioOutputKind.PHONE_SPEAKER, true).allowPhoneSpeaker,
        )
        assertEquals(
            false,
            DeviceSelectionEditor.setCategoryAllowed(base, AudioOutputKind.OTHER, false).allowUnidentifiedOutput,
        )
        // Untouched fields keep their values.
        assertTrue(DeviceSelectionEditor.setCategoryAllowed(base, AudioOutputKind.WIRED, false).allowBluetooth)
    }
}
