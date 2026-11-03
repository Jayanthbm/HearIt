package com.jayanth.hearit.backup

/**
 * UI State for Backup & Restore settings (master prompt §15, phase P19).
 *
 * Tracks export state, import status messages, and raw JSON configurations.
 */
data class BackupRestoreUiState(
    val currentSettingsJson: String = "",
    val statusMessage: String? = null,
    val isError: Boolean = false,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val lastExportTimeMillis: Long? = null,
)
