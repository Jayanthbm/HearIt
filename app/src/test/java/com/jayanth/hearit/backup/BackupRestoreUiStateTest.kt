package com.jayanth.hearit.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRestoreUiStateTest {

    @Test
    fun defaultState_hasEmptyJsonAndNoErrors() {
        val state = BackupRestoreUiState()

        assertEquals("", state.currentSettingsJson)
        assertNull(state.statusMessage)
        assertFalse(state.isError)
        assertFalse(state.isExporting)
        assertFalse(state.isImporting)
        assertNull(state.lastExportTimeMillis)
    }

    @Test
    fun stateCopy_updatesFieldsCorrectly() {
        val state = BackupRestoreUiState(
            currentSettingsJson = "{\"version\":1}",
            statusMessage = "Export complete",
            isError = false,
            isExporting = false,
            isImporting = true,
            lastExportTimeMillis = 123456789L,
        )

        assertEquals("{\"version\":1}", state.currentSettingsJson)
        assertEquals("Export complete", state.statusMessage)
        assertFalse(state.isError)
        assertFalse(state.isExporting)
        assertTrue(state.isImporting)
        assertEquals(123456789L, state.lastExportTimeMillis)
    }

    @Test
    fun errorState_tracksFailure() {
        val state = BackupRestoreUiState(
            statusMessage = "Import failed: file corrupted",
            isError = true,
        )

        assertTrue(state.isError)
        assertEquals("Import failed: file corrupted", state.statusMessage)
    }
}
