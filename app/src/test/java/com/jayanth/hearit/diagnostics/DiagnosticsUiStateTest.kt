package com.jayanth.hearit.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsUiStateTest {

    @Test
    fun defaultState_hasSafeDefaultsAndUnhealthy() {
        val state = DiagnosticsUiState()

        assertFalse(state.isNotificationAccessGranted)
        assertFalse(state.isPostNotificationGranted)
        assertFalse(state.isBluetoothPermissionGranted)
        assertFalse(state.isBluetoothPermissionRequired)
        assertFalse(state.isTtsAvailable)
        assertFalse(state.isTtsLocaleSupported)
        assertEquals(0, state.installedVoiceCount)
        assertEquals("", state.defaultLocaleLanguage)
        assertTrue(state.connectedOutputs.isEmpty())
        assertEquals(0, state.enabledAppsCount)
        assertEquals(0, state.priorityAppsCount)
        assertEquals(0, state.totalHistoryCount)
        assertFalse(state.isRefreshing)
        assertEquals(0, state.logFileCount)
        assertEquals(0L, state.totalLogSizeBytes)
        assertEquals(7, state.logRetentionDays)
        assertEquals(null, state.logExportSuccessMessage)
        assertEquals(null, state.logErrorMessage)

        assertFalse(state.allPermissionsHealthy)
        assertFalse(state.overallSystemHealthy)
    }

    @Test
    fun allPermissionsHealthy_whenPermissionsGranted_andBluetoothNotRequired() {
        val state = DiagnosticsUiState(
            isNotificationAccessGranted = true,
            isPostNotificationGranted = true,
            isBluetoothPermissionRequired = false,
            isBluetoothPermissionGranted = false,
        )

        assertTrue(state.allPermissionsHealthy)
    }

    @Test
    fun allPermissionsHealthy_whenBluetoothRequired_checksBluetoothStatus() {
        val stateMissingBt = DiagnosticsUiState(
            isNotificationAccessGranted = true,
            isPostNotificationGranted = true,
            isBluetoothPermissionRequired = true,
            isBluetoothPermissionGranted = false,
        )
        assertFalse(stateMissingBt.allPermissionsHealthy)

        val stateWithBt = stateMissingBt.copy(isBluetoothPermissionGranted = true)
        assertTrue(stateWithBt.allPermissionsHealthy)
    }

    @Test
    fun overallSystemHealthy_requiresAllPermissionsAndTtsAndLocale() {
        val baseState = DiagnosticsUiState(
            isNotificationAccessGranted = true,
            isPostNotificationGranted = true,
            isBluetoothPermissionRequired = false,
            isTtsAvailable = false,
            isTtsLocaleSupported = false,
        )
        assertFalse(baseState.overallSystemHealthy)

        val ttsWithoutLocale = baseState.copy(isTtsAvailable = true, isTtsLocaleSupported = false)
        assertFalse(ttsWithoutLocale.overallSystemHealthy)

        val fullyHealthy = baseState.copy(isTtsAvailable = true, isTtsLocaleSupported = true)
        assertTrue(fullyHealthy.overallSystemHealthy)
    }
}
