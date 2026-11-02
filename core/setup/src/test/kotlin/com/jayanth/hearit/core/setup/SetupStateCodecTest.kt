package com.jayanth.hearit.core.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Setup-state persistence contract (written for P11, not executed in the cloud). */
class SetupStateCodecTest {

    private val state = SetupState(
        completed = true,
        currentStep = SetupStep.TEST_ANNOUNCEMENT,
        visitedSteps = setOf(SetupStep.WELCOME, SetupStep.NOTIFICATION_ACCESS, SetupStep.APP_SELECTION),
        skippedSteps = setOf(SetupStep.BLUETOOTH_PERMISSION),
        batteryGuidanceAcknowledged = true,
    )

    @Test
    fun encode_alwaysWritesEveryKey() {
        assertEquals(
            setOf(
                "setup.version",
                "setup.completed",
                "setup.step",
                "setup.visited",
                "setup.skipped",
                "setup.batteryAcknowledged",
            ),
            SetupStateCodec.encode(state).keys,
        )
    }

    @Test
    fun roundTrip_preservesEveryField() {
        assertEquals(state, SetupStateCodec.decode(SetupStateCodec.encode(state)))
    }

    @Test
    fun decode_ofAnEmptyStore_returnsFreshInstallDefaults() {
        assertEquals(SetupState(), SetupStateCodec.decode(emptyMap()))
        assertEquals(SetupStep.WELCOME, SetupStateCodec.decode(emptyMap()).currentStep)
        assertFalse(SetupStateCodec.decode(emptyMap()).completed)
    }

    @Test
    fun unknownStepName_fallsBackToWelcome() {
        val decoded = SetupStateCodec.decode(mapOf("setup.step" to "STEP_FROM_A_NEWER_BUILD"))

        assertEquals(SetupStep.WELCOME, decoded.currentStep)
    }

    @Test
    fun unknownStepNamesInSets_areIgnored() {
        val decoded = SetupStateCodec.decode(
            mapOf("setup.visited" to "${SetupStep.WELCOME.name}\u001FNOT_A_STEP"),
        )

        assertEquals(setOf(SetupStep.WELCOME), decoded.visitedSteps)
    }

    @Test
    fun invalidBoolean_fallsBackToFalse() {
        val decoded = SetupStateCodec.decode(mapOf("setup.completed" to "yes please"))

        assertFalse(decoded.completed)
    }

    @Test
    fun encode_sortsStepsSoTheStoredFormIsStable() {
        val encoded = SetupStateCodec.encode(
            state.copy(visitedSteps = setOf(SetupStep.REVIEW, SetupStep.WELCOME)),
        )

        val parts = encoded.getValue("setup.visited").split('\u001F')
        assertEquals(listOf(SetupStep.REVIEW.name, SetupStep.WELCOME.name), parts.sorted())
    }

    @Test
    fun skippedSteps_roundTrip() {
        assertEquals(setOf(SetupStep.BLUETOOTH_PERMISSION), SetupStateCodec.decode(SetupStateCodec.encode(state)).skippedSteps)
    }

    @Test
    fun visiting_recordsTheStepAndMovesTheCursor() {
        val next = SetupState().visiting(SetupStep.NOTIFICATION_ACCESS)

        assertEquals(SetupStep.NOTIFICATION_ACCESS, next.currentStep)
        assertTrue(SetupStep.NOTIFICATION_ACCESS in next.visitedSteps)
        assertFalse(next.completed)
    }

    @Test
    fun skipping_recordsTheStepAsSkippedAndVisited() {
        val next = SetupState().skipping(SetupStep.BATTERY_GUIDANCE)

        assertTrue(next.wasSkipped(SetupStep.BATTERY_GUIDANCE))
        assertTrue(SetupStep.BATTERY_GUIDANCE in next.visitedSteps)
        assertFalse(next.wasSkipped(SetupStep.TEST_ANNOUNCEMENT))
    }

    @Test
    fun completing_marksTheFlowDoneAndEndsOnReview() {
        val next = SetupState().completing()

        assertTrue(next.completed)
        assertEquals(SetupStep.REVIEW, next.currentStep)
        assertTrue(SetupStep.REVIEW in next.visitedSteps)
    }
}
