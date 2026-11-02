package com.jayanth.hearit.core.datastore

import com.jayanth.hearit.core.setup.SetupState
import com.jayanth.hearit.core.setup.SetupStep
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Persistence wiring around [SetupStateCodec] (written for P11, not executed in the cloud). */
class SetupStateRepositoryTest {

    /** Minimal in-memory [PreferenceStore]; deliberately not shared with the settings tests. */
    private class InMemorySetupPreferenceStore(
        initial: Map<String, String> = emptyMap(),
    ) : PreferenceStore {
        private val state = MutableStateFlow(initial)

        override val values: Flow<Map<String, String>> = state

        override suspend fun read(): Map<String, String> = state.value

        override suspend fun replaceAll(values: Map<String, String>) {
            state.value = values
        }
    }

    @Test
    fun current_onEmptyStore_isFreshInstall() = runBlocking {
        val repository = DefaultSetupStateRepository(InMemorySetupPreferenceStore())

        val state = repository.current()

        assertEquals(SetupState(), state)
        assertFalse(state.completed)
        assertEquals(SetupStep.WELCOME, state.currentStep)
    }

    @Test
    fun update_persistsAndEmits() = runBlocking {
        val repository = DefaultSetupStateRepository(InMemorySetupPreferenceStore())

        repository.update { it.visiting(SetupStep.NOTIFICATION_ACCESS).skipping(SetupStep.BLUETOOTH_PERMISSION) }

        val state = repository.state.first()
        assertEquals(SetupStep.NOTIFICATION_ACCESS, state.currentStep)
        assertTrue(state.wasSkipped(SetupStep.BLUETOOTH_PERMISSION))
    }

    @Test
    fun update_isReadModifyWrite() = runBlocking {
        val repository = DefaultSetupStateRepository(InMemorySetupPreferenceStore())

        repository.update { it.visiting(SetupStep.APP_SELECTION) }
        repository.update { it.copy(batteryGuidanceAcknowledged = true) }

        val state = repository.current()
        assertEquals(SetupStep.APP_SELECTION, state.currentStep)
        assertTrue(state.batteryGuidanceAcknowledged)
    }

    @Test
    fun corruptedValues_decodeToDefaultsInsteadOfThrowing() = runBlocking {
        val repository = DefaultSetupStateRepository(
            InMemorySetupPreferenceStore(
                mapOf(
                    "setup.completed" to "not-a-boolean",
                    "setup.step" to "UNKNOWN_STEP",
                    "setup.visited" to "\u001F\u001F",
                ),
            ),
        )

        assertEquals(SetupState(), repository.current())
    }
}
