package com.jayanth.hearit.diagnostics

import com.jayanth.hearit.dashboard.ServiceSnapshot
import com.jayanth.hearit.setup.DetectedOutput

/**
 * Snapshot of system diagnostics, probes, and service readiness (P19).
 */
data class DiagnosticsUiState(
    val serviceSnapshot: ServiceSnapshot = ServiceSnapshot(),
    val isNotificationAccessGranted: Boolean = false,
    val isPostNotificationGranted: Boolean = false,
    val isBluetoothPermissionGranted: Boolean = false,
    val isBluetoothPermissionRequired: Boolean = false,
    val isTtsAvailable: Boolean = false,
    val isTtsLocaleSupported: Boolean = false,
    val installedVoiceCount: Int = 0,
    val defaultLocaleLanguage: String = "",
    val connectedOutputs: List<DetectedOutput> = emptyList(),
    val enabledAppsCount: Int = 0,
    val priorityAppsCount: Int = 0,
    val totalHistoryCount: Int = 0,
    val isRefreshing: Boolean = false,
    val logFileCount: Int = 0,
    val totalLogSizeBytes: Long = 0L,
    val logRetentionDays: Int = 7,
    val logExportSuccessMessage: String? = null,
    val logErrorMessage: String? = null,
) {
    /** True when all required permissions and service access are granted. */
    val allPermissionsHealthy: Boolean
        get() = isNotificationAccessGranted && isPostNotificationGranted &&
            (!isBluetoothPermissionRequired || isBluetoothPermissionGranted)

    /** True when both permissions and the speech engine are ready. */
    val overallSystemHealthy: Boolean
        get() = allPermissionsHealthy && isTtsAvailable && isTtsLocaleSupported
}
