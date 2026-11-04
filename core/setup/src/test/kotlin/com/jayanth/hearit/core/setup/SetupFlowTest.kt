package com.jayanth.hearit.core.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Navigation, skip rules and Start gating (written for P11, not executed in the cloud). */
class SetupFlowTest {

    private val ready = SetupPrerequisites(
        notificationAccessGranted = true,
        bluetoothPermissionRequired = true,
        bluetoothPermissionGranted = true,
        ttsEngineAvailable = true,
        ttsLanguageAvailable = true,
        enabledAppCount = 2,
        allowedOutputCount = 2,
    )

    private val android10 = ready.copy(bluetoothPermissionRequired = false, bluetoothPermissionGranted = false)

    @Test
    fun orderedSteps_areTheDocumentedNineInOrder() {
        assertEquals(
            listOf(
                SetupStep.WELCOME,
                SetupStep.NOTIFICATION_ACCESS,
                SetupStep.BLUETOOTH_PERMISSION,
                SetupStep.TEXT_TO_SPEECH,
                SetupStep.BATTERY_GUIDANCE,
                SetupStep.APP_SELECTION,
                SetupStep.AUDIO_DEVICES,
                SetupStep.TEST_ANNOUNCEMENT,
                SetupStep.REVIEW,
            ),
            SetupFlow.orderedSteps,
        )
    }

    @Test
    fun notificationAccess_missingIsABlocker() {
        val prerequisites = ready.copy(notificationAccessGranted = false)

        assertEquals(
            listOf(SetupBlocker.NotificationAccessMissing),
            SetupFlow.blockersFor(SetupStep.NOTIFICATION_ACCESS, prerequisites),
        )
        assertFalse(SetupFlow.canAdvanceFrom(SetupStep.NOTIFICATION_ACCESS, prerequisites))
    }

    @Test
    fun notificationAccess_grantedHasNoBlockers() {
        assertTrue(SetupFlow.blockersFor(SetupStep.NOTIFICATION_ACCESS, ready).isEmpty())
        assertTrue(SetupFlow.canAdvanceFrom(SetupStep.NOTIFICATION_ACCESS, ready))
    }

    @Test
    fun bluetoothStep_isHiddenWhenThePlatformDoesNotRequireThePermission() {
        assertFalse(SetupStep.BLUETOOTH_PERMISSION in SetupFlow.visibleSteps(android10))
        assertTrue(SetupStep.BLUETOOTH_PERMISSION in SetupFlow.visibleSteps(ready))
    }

    @Test
    fun bluetoothStep_isSkippableEvenWhenThePermissionIsMissing() {
        val prerequisites = ready.copy(bluetoothPermissionGranted = false)

        assertEquals(
            listOf(SetupBlocker.BluetoothPermissionMissing),
            SetupFlow.blockersFor(SetupStep.BLUETOOTH_PERMISSION, prerequisites),
        )
        assertTrue(SetupFlow.canAdvanceFrom(SetupStep.BLUETOOTH_PERMISSION, prerequisites))
        assertTrue(SetupFlow.canOnlySkip(SetupStep.BLUETOOTH_PERMISSION, prerequisites))
    }

    @Test
    fun bluetoothMissing_neverBlocksStart() {
        assertTrue(SetupFlow.canStart(ready.copy(bluetoothPermissionGranted = false)))
    }

    @Test
    fun tts_missingEngineBlocksWithTheEngineReason() {
        val prerequisites = ready.copy(ttsEngineAvailable = false, ttsLanguageAvailable = false)

        assertEquals(
            listOf(SetupBlocker.TtsEngineUnavailable),
            SetupFlow.blockersFor(SetupStep.TEXT_TO_SPEECH, prerequisites),
        )
    }

    @Test
    fun tts_missingLanguageDataBlocksWithTheLanguageReason() {
        assertEquals(
            listOf(SetupBlocker.TtsLanguageMissing),
            SetupFlow.blockersFor(SetupStep.TEXT_TO_SPEECH, ready.copy(ttsLanguageAvailable = false)),
        )
    }

    @Test
    fun tts_readyHasNoBlockers() {
        assertTrue(SetupFlow.blockersFor(SetupStep.TEXT_TO_SPEECH, ready).isEmpty())
    }

    @Test
    fun appSelection_blocksWhileEveryAppIsDisabled_butIsSkippable() {
        val prerequisites = ready.copy(enabledAppCount = 0)

        assertEquals(
            listOf(SetupBlocker.NoEnabledApps),
            SetupFlow.blockersFor(SetupStep.APP_SELECTION, prerequisites),
        )
        assertTrue(SetupFlow.canAdvanceFrom(SetupStep.APP_SELECTION, prerequisites))
        assertTrue(SetupFlow.canOnlySkip(SetupStep.APP_SELECTION, prerequisites))
    }

    @Test
    fun audioDevices_blocksWhenNoOutputCategoryIsAllowed() {
        assertEquals(
            listOf(SetupBlocker.NoAllowedOutput),
            SetupFlow.blockersFor(SetupStep.AUDIO_DEVICES, ready.copy(allowedOutputCount = 0)),
        )
    }

    @Test
    fun audioDevices_isSkippableBecauseTheDefaultsAreAlreadyAllowed() {
        assertTrue(SetupFlow.canAdvanceFrom(SetupStep.AUDIO_DEVICES, ready))
        assertTrue(SetupStep.AUDIO_DEVICES.isSkippable)
    }

    @Test
    fun review_reportsEveryStartBlockerInAStableOrder() {
        val blocked = SetupPrerequisites()

        assertEquals(
            listOf(
                SetupBlocker.NotificationAccessMissing,
                SetupBlocker.TtsEngineUnavailable,
                SetupBlocker.NoEnabledApps,
                SetupBlocker.NoAllowedOutput,
            ),
            SetupFlow.startBlockers(blocked),
        )
        assertEquals(SetupFlow.startBlockers(blocked), SetupFlow.blockersFor(SetupStep.REVIEW, blocked))
        assertFalse(SetupFlow.canAdvanceFrom(SetupStep.REVIEW, blocked))
    }

    @Test
    fun canStart_trueOnlyWhenEveryRequiredPrerequisiteHolds() {
        assertTrue(SetupFlow.canStart(ready))
        assertFalse(SetupFlow.canStart(ready.copy(notificationAccessGranted = false)))
        assertFalse(SetupFlow.canStart(ready.copy(ttsEngineAvailable = false)))
        assertFalse(SetupFlow.canStart(ready.copy(ttsLanguageAvailable = false)))
        assertFalse(SetupFlow.canStart(ready.copy(enabledAppCount = 0)))
        assertFalse(SetupFlow.canStart(ready.copy(allowedOutputCount = 0)))
    }

    @Test
    fun nextStep_walksTheVisibleOrderAndSkipsHiddenSteps() {
        assertEquals(SetupStep.NOTIFICATION_ACCESS, SetupFlow.nextStep(SetupStep.WELCOME, android10))
        assertEquals(SetupStep.TEXT_TO_SPEECH, SetupFlow.nextStep(SetupStep.NOTIFICATION_ACCESS, android10))
        assertEquals(SetupStep.BLUETOOTH_PERMISSION, SetupFlow.nextStep(SetupStep.NOTIFICATION_ACCESS, ready))
        assertEquals(SetupStep.TEXT_TO_SPEECH, SetupFlow.nextStep(SetupStep.BLUETOOTH_PERMISSION, ready))
    }

    @Test
    fun nextStep_isNullOnTheReviewStep() {
        assertNull(SetupFlow.nextStep(SetupStep.REVIEW, ready))
        assertTrue(SetupFlow.isFinalStep(SetupStep.REVIEW))
    }

    @Test
    fun previousStep_isNullOnWelcomeAndSkipsHiddenSteps() {
        assertNull(SetupFlow.previousStep(SetupStep.WELCOME, ready))
        assertEquals(SetupStep.NOTIFICATION_ACCESS, SetupFlow.previousStep(SetupStep.TEXT_TO_SPEECH, android10))
        assertEquals(SetupStep.BLUETOOTH_PERMISSION, SetupFlow.previousStep(SetupStep.TEXT_TO_SPEECH, ready))
    }

    @Test
    fun resolveVisibleStep_fallsBackToThePreviousVisibleStep() {
        assertEquals(
            SetupStep.NOTIFICATION_ACCESS,
            SetupFlow.resolveVisibleStep(SetupStep.BLUETOOTH_PERMISSION, android10),
        )
        assertEquals(SetupStep.BLUETOOTH_PERMISSION, SetupFlow.resolveVisibleStep(SetupStep.BLUETOOTH_PERMISSION, ready))
        assertEquals(SetupStep.REVIEW, SetupFlow.resolveVisibleStep(SetupStep.REVIEW, ready))
    }

    @Test
    fun onlySkippableStepsCanBePassedWithoutTheirPrerequisite() {
        val blocked = SetupPrerequisites()

        assertFalse(SetupFlow.canOnlySkip(SetupStep.WELCOME, blocked))
        assertFalse(SetupFlow.canOnlySkip(SetupStep.NOTIFICATION_ACCESS, blocked))
        assertFalse(SetupFlow.canOnlySkip(SetupStep.TEXT_TO_SPEECH, blocked))
        assertFalse(SetupFlow.canOnlySkip(SetupStep.REVIEW, blocked))
        assertTrue(SetupFlow.canOnlySkip(SetupStep.AUDIO_DEVICES, blocked))
        assertFalse(SetupFlow.canOnlySkip(SetupStep.TEST_ANNOUNCEMENT, blocked))
    }

    @Test
    fun informationalSteps_neverBlock() {
        val blocked = SetupPrerequisites()

        assertTrue(SetupFlow.blockersFor(SetupStep.WELCOME, blocked).isEmpty())
        assertTrue(SetupFlow.blockersFor(SetupStep.BATTERY_GUIDANCE, blocked).isEmpty())
        assertTrue(SetupFlow.blockersFor(SetupStep.TEST_ANNOUNCEMENT, blocked).isEmpty())
        assertTrue(SetupFlow.canAdvanceFrom(SetupStep.BATTERY_GUIDANCE, blocked))
    }

    @Test
    fun stepClassification_matchesTheDocumentedSkipRules() {
        assertEquals(
            setOf(SetupStep.BLUETOOTH_PERMISSION, SetupStep.BATTERY_GUIDANCE, SetupStep.APP_SELECTION, SetupStep.AUDIO_DEVICES, SetupStep.TEST_ANNOUNCEMENT),
            SetupStep.ordered.filter { it.isSkippable }.toSet(),
        )
        assertTrue(SetupStep.ordered.filter { it.isRequired }.none { it.isSkippable })
    }
}
