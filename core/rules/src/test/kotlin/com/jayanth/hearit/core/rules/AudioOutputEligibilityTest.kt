package com.jayanth.hearit.core.rules

import com.jayanth.hearit.core.common.model.AudioOutput
import com.jayanth.hearit.core.common.model.AudioOutputKind
import com.jayanth.hearit.core.common.model.AudioOutputSettings
import com.jayanth.hearit.core.common.model.EligibilitySettings
import com.jayanth.hearit.core.common.model.LockCondition
import com.jayanth.hearit.core.common.model.NotificationSnapshot
import com.jayanth.hearit.core.common.model.ResolvedSettings
import com.jayanth.hearit.core.common.model.RuntimeState
import com.jayanth.hearit.core.common.model.ScreenCondition
import com.jayanth.hearit.core.common.model.SkipReason
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ADR-006 step 10: allowed-audio-output matching (master prompt §6, ADR-017).
 *
 * Defaults under test: wired and Bluetooth allowed, phone speaker off, unidentified outputs
 * allowed, unobserved output treated as allowed. Priority Announce bypasses only
 * screen/lock, so it can never force speech onto a disallowed output.
 */
class AudioOutputEligibilityTest {

    private val rules = EligibilityRules()

    private fun snapshot() = NotificationSnapshot(
        key = "com.example.app|tag|1",
        packageName = "com.example.app",
        appName = "Example",
        messageText = "Are you coming today?",
        postTimeMillis = 1_000L,
    )

    private fun settings(
        audioOutput: AudioOutputSettings = AudioOutputSettings(),
        screen: ScreenCondition = ScreenCondition.BOTH,
        priorityAnnounceListed: Boolean = false,
    ) = ResolvedSettings(
        appEnabled = true,
        priorityAnnounceListed = priorityAnnounceListed,
        eligibility = EligibilitySettings(
            screenCondition = screen,
            lockCondition = LockCondition.BOTH,
            audioOutput = audioOutput,
        ),
    )

    private fun state(
        activeOutput: AudioOutput? = null,
        screenOn: Boolean = false,
        temporaryMuteActive: Boolean = false,
        callActive: Boolean = false,
        dndActive: Boolean = false,
        priorityAnnounceActive: Boolean = false,
    ) = RuntimeState(
        screenOn = screenOn,
        isLocked = true,
        temporaryMuteActive = temporaryMuteActive,
        priorityAnnounceActive = priorityAnnounceActive,
        isCallActive = callActive,
        isDndActive = dndActive,
        activeOutput = activeOutput,
    )

    // --- Default policy --------------------------------------------------------------------

    @Test
    fun evaluate_defaultSettings_wiredOutput_announces() {
        assertEquals(
            EligibilityDecision.Announce,
            rules.evaluate(snapshot(), settings(), state(AudioOutput(AudioOutputKind.WIRED))),
        )
    }

    @Test
    fun evaluate_defaultSettings_bluetoothOutput_announces() {
        assertEquals(
            EligibilityDecision.Announce,
            rules.evaluate(
                snapshot(),
                settings(),
                state(AudioOutput(AudioOutputKind.BLUETOOTH, id = "00:11:22:33:44:55", name = "Buds")),
            ),
        )
    }

    @Test
    fun evaluate_defaultSettings_phoneSpeaker_skipsWithOutputNotAllowed() {
        assertEquals(false, AudioOutputSettings().allowPhoneSpeaker)
        assertEquals(
            EligibilityDecision.Skip(SkipReason.OUTPUT_NOT_ALLOWED),
            rules.evaluate(
                snapshot(),
                settings(),
                state(AudioOutput(AudioOutputKind.PHONE_SPEAKER)),
            ),
        )
    }

    @Test
    fun evaluate_activeOutputNotObserved_announces() {
        assertEquals(
            EligibilityDecision.Announce,
            rules.evaluate(snapshot(), settings(), state(activeOutput = null)),
        )
    }

    // --- Per-category toggles ---------------------------------------------------------------

    @Test
    fun evaluate_phoneSpeakerAllowed_announces() {
        val output = AudioOutputSettings(allowPhoneSpeaker = true)
        assertEquals(
            EligibilityDecision.Announce,
            rules.evaluate(snapshot(), settings(output), state(AudioOutput(AudioOutputKind.PHONE_SPEAKER))),
        )
    }

    @Test
    fun evaluate_wiredNotAllowed_skips() {
        val output = AudioOutputSettings(allowWired = false)
        assertEquals(
            EligibilityDecision.Skip(SkipReason.OUTPUT_NOT_ALLOWED),
            rules.evaluate(snapshot(), settings(output), state(AudioOutput(AudioOutputKind.WIRED))),
        )
    }

    @Test
    fun evaluate_bluetoothNotAllowed_skips() {
        val output = AudioOutputSettings(allowBluetooth = false)
        assertEquals(
            EligibilityDecision.Skip(SkipReason.OUTPUT_NOT_ALLOWED),
            rules.evaluate(
                snapshot(),
                settings(output),
                state(AudioOutput(AudioOutputKind.BLUETOOTH, id = "00:11:22:33:44:55")),
            ),
        )
    }

    @Test
    fun evaluate_unidentifiedOutput_allowedByDefault_announces() {
        assertEquals(true, AudioOutputSettings().allowUnidentifiedOutput)
        assertEquals(
            EligibilityDecision.Announce,
            rules.evaluate(snapshot(), settings(), state(AudioOutput(AudioOutputKind.OTHER, name = "OEM route"))),
        )
    }

    @Test
    fun evaluate_unidentifiedOutputNotAllowed_skips() {
        val output = AudioOutputSettings(allowUnidentifiedOutput = false)
        assertEquals(
            EligibilityDecision.Skip(SkipReason.OUTPUT_NOT_ALLOWED),
            rules.evaluate(snapshot(), settings(output), state(AudioOutput(AudioOutputKind.OTHER))),
        )
    }

    // --- Per-device Bluetooth allow-list -----------------------------------------------------

    @Test
    fun evaluate_bluetoothAllowlist_matchingId_announces() {
        val output = AudioOutputSettings(allowedBluetoothIds = setOf("AA:BB:CC:DD:EE:FF"))
        assertEquals(
            EligibilityDecision.Announce,
            rules.evaluate(
                snapshot(),
                settings(output),
                state(AudioOutput(AudioOutputKind.BLUETOOTH, id = "AA:BB:CC:DD:EE:FF")),
            ),
        )
    }

    @Test
    fun evaluate_bluetoothAllowlist_otherId_skips() {
        val output = AudioOutputSettings(allowedBluetoothIds = setOf("AA:BB:CC:DD:EE:FF"))
        assertEquals(
            EligibilityDecision.Skip(SkipReason.OUTPUT_NOT_ALLOWED),
            rules.evaluate(
                snapshot(),
                settings(output),
                state(AudioOutput(AudioOutputKind.BLUETOOTH, id = "11:22:33:44:55:66")),
            ),
        )
    }

    @Test
    fun evaluate_bluetoothAllowlist_deviceWithoutId_skips() {
        val output = AudioOutputSettings(allowedBluetoothIds = setOf("AA:BB:CC:DD:EE:FF"))
        assertEquals(
            EligibilityDecision.Skip(SkipReason.OUTPUT_NOT_ALLOWED),
            rules.evaluate(
                snapshot(),
                settings(output),
                state(AudioOutput(AudioOutputKind.BLUETOOTH, id = null)),
            ),
        )
    }

    // --- Precedence --------------------------------------------------------------------------

    @Test
    fun evaluate_priorityModeActive_phoneSpeakerStillSkips() {
        // Priority Announce bypasses screen/lock only — never the output rule.
        assertEquals(
            EligibilityDecision.Skip(SkipReason.OUTPUT_NOT_ALLOWED),
            rules.evaluate(
                snapshot(),
                settings(priorityAnnounceListed = true),
                state(
                    activeOutput = AudioOutput(AudioOutputKind.PHONE_SPEAKER),
                    screenOn = true,
                    priorityAnnounceActive = true,
                ),
            ),
        )
    }

    @Test
    fun evaluate_mutedAndNotAllowedOutput_reportsMutedFirst() {
        assertEquals(
            EligibilityDecision.Skip(SkipReason.MUTED),
            rules.evaluate(
                snapshot(),
                settings(),
                state(
                    activeOutput = AudioOutput(AudioOutputKind.PHONE_SPEAKER),
                    temporaryMuteActive = true,
                ),
            ),
        )
    }

    @Test
    fun evaluate_callActiveAndNotAllowedOutput_reportsCallActiveFirst() {
        assertEquals(
            EligibilityDecision.Skip(SkipReason.CALL_ACTIVE),
            rules.evaluate(
                snapshot(),
                settings(),
                state(activeOutput = AudioOutput(AudioOutputKind.PHONE_SPEAKER), callActive = true),
            ),
        )
    }

    @Test
    fun evaluate_dndActiveAndNotAllowedOutput_reportsDndFirst() {
        assertEquals(
            EligibilityDecision.Skip(SkipReason.DND),
            rules.evaluate(
                snapshot(),
                settings(),
                state(activeOutput = AudioOutput(AudioOutputKind.PHONE_SPEAKER), dndActive = true),
            ),
        )
    }

    @Test
    fun evaluate_screenOnAndNotAllowedOutput_reportsScreenOnFirst() {
        assertEquals(
            EligibilityDecision.Skip(SkipReason.SCREEN_ON),
            rules.evaluate(
                snapshot(),
                settings(screen = ScreenCondition.OFF),
                state(activeOutput = AudioOutput(AudioOutputKind.PHONE_SPEAKER), screenOn = true),
            ),
        )
    }
}
