package com.jayanth.hearit.core.datastore

import com.jayanth.hearit.core.common.model.AudioOutputSettings
import com.jayanth.hearit.core.common.model.EligibilitySettings
import com.jayanth.hearit.core.common.model.KnownAudioDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P14: the Devices feature persists what it shows. These tests pin the two settings that
 * make that possible — the Bluetooth allow-list (already in [AudioOutputSettings]) and the
 * remembered device list added with the feature.
 */
class SettingsDeviceCodecTest {

    @Test
    fun roundTrip_rememberedDevicesSurvive() {
        val settings = HearItSettings(
            knownDevices = listOf(
                KnownAudioDevice("AA:BB:CC", "Car kit", isBluetooth = true),
                KnownAudioDevice("usb-1", "USB headset", isBluetooth = false),
            ),
        )

        val decoded = SettingsCodec.decode(SettingsCodec.encode(settings))

        assertEquals(settings.knownDevices, decoded.knownDevices)
    }

    @Test
    fun roundTrip_bluetoothAllowListSurvives() {
        val settings = HearItSettings(
            eligibility = EligibilitySettings(
                audioOutput = AudioOutputSettings(allowedBluetoothIds = setOf("AA:BB:CC", "DD:EE:FF")),
            ),
        )

        val decoded = SettingsCodec.decode(SettingsCodec.encode(settings))

        assertEquals(
            setOf("AA:BB:CC", "DD:EE:FF"),
            decoded.eligibility.audioOutput.allowedBluetoothIds,
        )
    }

    @Test
    fun roundTrip_emptyAllowListMeansNoBluetoothDeviceIsAllowed() {
        val settings = HearItSettings(
            eligibility = EligibilitySettings(
                audioOutput = AudioOutputSettings(allowedBluetoothIds = emptySet()),
            ),
        )

        val decoded = SettingsCodec.decode(SettingsCodec.encode(settings))

        assertEquals(emptySet<String>(), decoded.eligibility.audioOutput.allowedBluetoothIds)
    }

    @Test
    fun roundTrip_defaults_meanAnyDeviceAndNoMemory() {
        val decoded = SettingsCodec.decode(SettingsCodec.encode(HearItSettings()))

        assertNull(decoded.eligibility.audioOutput.allowedBluetoothIds)
        assertTrue(decoded.knownDevices.isEmpty())
    }

    @Test
    fun decode_malformedDeviceEntry_isIgnored() {
        val values = SettingsCodec.encode(HearItSettings()).toMutableMap()
        values["devices.known"] = "only-an-id\u001Fbroken"

        val decoded = SettingsCodec.decode(values)

        assertTrue(decoded.knownDevices.isEmpty())
    }

    @Test
    fun encode_sanitisesSeparatorsInsideDeviceValues() {
        val settings = HearItSettings(
            knownDevices = listOf(
                KnownAudioDevice("id\u001Fx", "Left\u001FRight", isBluetooth = true),
            ),
        )

        val device = SettingsCodec.decode(SettingsCodec.encode(settings)).knownDevices.single()

        assertEquals("id x", device.id)
        assertEquals("Left Right", device.label)
    }

    @Test
    fun decode_duplicateDeviceIds_keepTheFirst() {
        val settings = HearItSettings(
            knownDevices = listOf(
                KnownAudioDevice("same", "First", isBluetooth = true),
                KnownAudioDevice("same", "Second", isBluetooth = true),
            ),
        )

        val decoded = SettingsCodec.decode(SettingsCodec.encode(settings))

        assertEquals(listOf("First"), decoded.knownDevices.map { it.label })
    }
}
