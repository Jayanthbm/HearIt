package com.jayanth.hearit.diagnostics

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayanth.hearit.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Entry point for Diagnostics (master prompt §13, phase P19).
 */
@Composable
fun DiagnosticsRoute(
    viewModel: DiagnosticsViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // SAF CreateDocument Launcher for Diagnostic Log Export
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri != null) {
            viewModel.onExportLogsToUri(context, uri)
        }
    }

    DiagnosticsScreen(
        state = state,
        onRefresh = viewModel::refresh,
        onOpenNotificationAccess = viewModel::openNotificationAccessSettings,
        onOpenBatterySettings = viewModel::openBatteryOptimizationSettings,
        onExportLogsClicked = {
            exportLauncher.launch(
                "hearit_diagnostics_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())}.txt",
            )
        },
        onClearLogsClicked = viewModel::onClearLogs,
        onDismissStatusMessage = viewModel::onClearLogStatusMessage,
        modifier = modifier,
    )
}

/**
 * Modern Material 3 Expressive Diagnostics screen.
 */
@Composable
fun DiagnosticsScreen(
    state: DiagnosticsUiState,
    onRefresh: () -> Unit,
    onOpenNotificationAccess: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onExportLogsClicked: () -> Unit,
    onClearLogsClicked: () -> Unit,
    onDismissStatusMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Feedback banners
        state.logExportSuccessMessage?.let { success ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF064E3B).copy(alpha = 0.2f),
                border = BorderStroke(1.dp, Color(0xFF10B981)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = success,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    TextButton(onClick = onDismissStatusMessage) {
                        Text(
                            text = stringResource(R.string.diagnostics_logs_dismiss),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        state.logErrorMessage?.let { error ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                    TextButton(onClick = onDismissStatusMessage) {
                        Text(
                            text = stringResource(R.string.diagnostics_logs_dismiss),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        // Hero System Status Card
        SystemHealthHeroCard(healthy = state.overallSystemHealthy)

        // Section 1: Announcement Service Live State
        ServiceDiagnosticsCard(state = state)

        // Section 2: Permissions & System Access
        PermissionsDiagnosticsCard(
            state = state,
            onOpenNotificationAccess = onOpenNotificationAccess,
            onOpenBatterySettings = onOpenBatterySettings,
        )

        // Section 3: Speech Synthesis (TTS) Readiness
        TtsDiagnosticsCard(state = state)

        // Section 4: Audio Output Hardware Routing
        AudioDiagnosticsCard(state = state)

        // Section 5: Configuration & Storage Status
        StorageDiagnosticsCard(state = state)

        // Section 6: Diagnostic Event Logging & Export
        DiagnosticLogsCard(
            fileCount = state.logFileCount,
            totalSizeBytes = state.totalLogSizeBytes,
            retentionDays = state.logRetentionDays,
            onExportClicked = onExportLogsClicked,
            onClearClicked = { showClearDialog = true },
        )

        // Refresh Probes Button
        OutlinedButton(
            onClick = onRefresh,
            enabled = !state.isRefreshing,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 8.dp, bottomEnd = 16.dp, bottomStart = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            if (state.isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.diagnostics_run_probes),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.diagnostics_logs_clear_dialog_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.diagnostics_logs_clear_dialog_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearLogsClicked()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.diagnostics_logs_clear_confirm),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun SystemHealthHeroCard(healthy: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 10.dp, bottomEnd = 24.dp, bottomStart = 10.dp),
        color = if (healthy) Color(0xFF064E3B) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, if (healthy) Color(0xFF10B981) else MaterialTheme.colorScheme.error),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = if (healthy) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                contentDescription = null,
                tint = if (healthy) Color(0xFF34D399) else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp),
            )
            Column {
                Text(
                    text = if (healthy) "System Ready & Healthy" else "Attention Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (healthy) Color(0xFF34D399) else MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    text = if (healthy) {
                        "All permissions, TTS engine, and output probes are operating normally."
                    } else {
                        "One or more system permissions or hardware probes need attention."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (healthy) Color(0xFFA7F3D0) else MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                    lineHeight = 16.sp,
                )
            }
        }
    }
}

@Composable
private fun ServiceDiagnosticsCard(state: DiagnosticsUiState) {
    val service = state.serviceSnapshot
    DiagnosticsSectionCard(
        title = stringResource(R.string.diagnostics_service_title),
        subtitle = "Live in-process announcement daemon",
        icon = Icons.Rounded.RocketLaunch,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Running State
            DiagnosticRow(
                label = "Daemon Status",
                subtitle = "Foreground service lifecycle state",
                statusText = if (service.isRunning) "RUNNING" else "STOPPED",
                isOk = service.isRunning,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Speech State
            DiagnosticRow(
                label = "Speaking Activity",
                subtitle = "Current audio playback state",
                statusText = if (service.isSpeaking) "Reading out loud" else "Idle",
                isOk = true,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Queue Depth
            DiagnosticRow(
                label = "Pending Queue Items",
                subtitle = "Notifications buffered for announcement",
                statusText = "${service.pendingCount} queued",
                isOk = true,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Priority Announce
            DiagnosticRow(
                label = "Priority Announce Mode",
                subtitle = "Screen and lock rule bypass state",
                statusText = if (service.isPriorityAnnounceActive) "ACTIVE" else "INACTIVE",
                isOk = true,
            )
        }
    }
}

@Composable
private fun PermissionsDiagnosticsCard(
    state: DiagnosticsUiState,
    onOpenNotificationAccess: () -> Unit,
    onOpenBatterySettings: () -> Unit,
) {
    val context = LocalContext.current
    DiagnosticsSectionCard(
        title = stringResource(R.string.diagnostics_permissions_title),
        subtitle = "Hardware and framework permission health",
        icon = Icons.Rounded.Security,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Notification Access
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = stringResource(R.string.diagnostics_notification_access),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.diagnostics_notification_access_desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.isNotificationAccessGranted) {
                    StatusBadge(text = stringResource(R.string.diagnostics_status_granted), isOk = true)
                } else {
                    FilledTonalButton(
                        onClick = onOpenNotificationAccess,
                        shape = RoundedCornerShape(topStart = 10.dp, topEnd = 4.dp, bottomEnd = 10.dp, bottomStart = 4.dp),
                        modifier = Modifier.height(34.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.diagnostics_fix_action),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Post Notifications
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = stringResource(R.string.diagnostics_post_notifications),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.diagnostics_post_notifications_desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.isPostNotificationGranted) {
                    StatusBadge(text = stringResource(R.string.diagnostics_status_granted), isOk = true)
                } else {
                    FilledTonalButton(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    })
                                } catch (_: Exception) {}
                            }
                        },
                        shape = RoundedCornerShape(topStart = 10.dp, topEnd = 4.dp, bottomEnd = 10.dp, bottomStart = 4.dp),
                        modifier = Modifier.height(34.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.diagnostics_fix_action),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Bluetooth
            val btOk = !state.isBluetoothPermissionRequired || state.isBluetoothPermissionGranted
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = stringResource(R.string.diagnostics_bluetooth_permission),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.diagnostics_bluetooth_permission_desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (btOk) {
                    StatusBadge(text = stringResource(R.string.diagnostics_status_granted), isOk = true)
                } else {
                    FilledTonalButton(
                        onClick = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            } catch (_: Exception) {}
                        },
                        shape = RoundedCornerShape(topStart = 10.dp, topEnd = 4.dp, bottomEnd = 10.dp, bottomStart = 4.dp),
                        modifier = Modifier.height(34.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.diagnostics_fix_action),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Battery Optimization
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = stringResource(R.string.diagnostics_battery_optimization),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.diagnostics_battery_desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FilledTonalButton(
                    onClick = onOpenBatterySettings,
                    shape = RoundedCornerShape(topStart = 10.dp, topEnd = 4.dp, bottomEnd = 10.dp, bottomStart = 4.dp),
                    modifier = Modifier.height(34.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Configure", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TtsDiagnosticsCard(state: DiagnosticsUiState) {
    val context = LocalContext.current
    DiagnosticsSectionCard(
        title = stringResource(R.string.diagnostics_tts_title),
        subtitle = "Android native TextToSpeech engine",
        icon = Icons.AutoMirrored.Rounded.VolumeUp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Engine Ready
            DiagnosticRow(
                label = "TTS Engine Status",
                subtitle = if (state.isTtsAvailable) "Android TextToSpeech engine initialized" else "Synthesizer initializing or unavailable",
                statusText = if (state.isTtsAvailable) "Available" else "Unavailable",
                isOk = state.isTtsAvailable,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // System Locale Supported
            DiagnosticRow(
                label = "Default Language",
                subtitle = state.defaultLocaleLanguage.ifBlank { "System default locale" },
                statusText = if (state.isTtsLocaleSupported) "Supported" else "Missing Data",
                isOk = state.isTtsLocaleSupported,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Voices Count
            DiagnosticRow(
                label = "Installed Engine Voices",
                subtitle = "Available voice synthesis models on device",
                statusText = "${state.installedVoiceCount} voices",
                isOk = state.installedVoiceCount > 0,
            )

            if (!state.isTtsAvailable || !state.isTtsLocaleSupported) {
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(topStart = 14.dp, topEnd = 6.dp, bottomEnd = 14.dp, bottomStart = 6.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = if (!state.isTtsAvailable) "TTS Engine Issue Detected" else "Voice Data Missing",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                        Text(
                            text = if (!state.isTtsAvailable) {
                                "Text-to-Speech engine is unavailable or uninitialized. Open Text-to-Speech settings to verify Google Speech Services or your preferred synthesizer is installed and selected."
                            } else {
                                "Speech voice data for your locale (${state.defaultLocaleLanguage.ifBlank { "system" }}) is missing. Open settings to download speech files."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f),
                        )
                        FilledTonalButton(
                            onClick = {
                                try {
                                    context.startActivity(Intent("com.android.settings.TTS_SETTINGS").apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    })
                                } catch (_: Exception) {
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        })
                                    } catch (_: Exception) {}
                                }
                            },
                            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 6.dp, bottomEnd = 12.dp, bottomStart = 6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Open Text-to-Speech Settings",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 6.dp, bottomEnd = 12.dp, bottomStart = 6.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Speech synthesis is ready for spoken announcements in ${state.defaultLocaleLanguage}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioDiagnosticsCard(state: DiagnosticsUiState) {
    DiagnosticsSectionCard(
        title = stringResource(R.string.diagnostics_audio_title),
        subtitle = "Detected outputs via AudioManager",
        icon = Icons.Rounded.Headphones,
    ) {
        if (state.connectedOutputs.isEmpty()) {
            Text(
                text = "No connected headphones or headsets detected. Announcements will route to speaker if permitted.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (output in state.connectedOutputs) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = if (output.isBluetooth) Icons.Rounded.Bluetooth else Icons.Rounded.Headphones,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Column {
                                Text(
                                    text = output.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = if (output.isBluetooth) "Bluetooth Audio" else "Wired / USB Audio",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageDiagnosticsCard(state: DiagnosticsUiState) {
    DiagnosticsSectionCard(
        title = stringResource(R.string.diagnostics_app_state_title),
        subtitle = "Preferences, app rules and room database",
        icon = Icons.Rounded.Storage,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DiagnosticRow(
                label = "Enabled Apps for Reading",
                subtitle = "Applications allowed to trigger speech",
                statusText = "${state.enabledAppsCount} apps",
                isOk = state.enabledAppsCount > 0,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            DiagnosticRow(
                label = "Priority Announce Apps",
                subtitle = "Configured apps that bypass lock rules",
                statusText = "${state.priorityAppsCount} apps",
                isOk = true,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            DiagnosticRow(
                label = "Logged History Entries",
                subtitle = "Persisted announcement records in database",
                statusText = "${state.totalHistoryCount} entries",
                isOk = true,
            )

            if (state.enabledAppsCount == 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 6.dp, bottomEnd = 12.dp, bottomStart = 6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "No apps are currently enabled for reading announcements. Select apps in the Apps tab to start hearing notifications.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticRow(
    label: String,
    statusText: String,
    isOk: Boolean,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        StatusBadge(text = statusText, isOk = isOk)
    }
}

@Composable
private fun StatusBadge(text: String, isOk: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isOk) Color(0xFF064E3B) else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, if (isOk) Color(0xFF10B981) else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isOk) Color(0xFF34D399) else MaterialTheme.colorScheme.onSurfaceVariant),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                color = if (isOk) Color(0xFF34D399) else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DiagnosticLogsCard(
    fileCount: Int,
    totalSizeBytes: Long,
    retentionDays: Int,
    onExportClicked: () -> Unit,
    onClearClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sizeKb = (totalSizeBytes / 1024L).coerceAtLeast(if (fileCount > 0) 1L else 0L)
    DiagnosticsSectionCard(
        title = stringResource(R.string.diagnostics_logs_title),
        subtitle = stringResource(R.string.diagnostics_logs_subtitle),
        icon = Icons.Rounded.Terminal,
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Stat badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                ) {
                    Text(
                        text = stringResource(R.string.diagnostics_logs_stat_retention),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Text(
                        text = stringResource(R.string.diagnostics_logs_stat_files, fileCount, sizeKb),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }

            // Privacy Guarantee Pill
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF10B981).copy(alpha = 0.12f),
                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = stringResource(R.string.diagnostics_logs_stat_privacy),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF047857),
                    )
                }
            }

            // Explanatory note
            Text(
                text = stringResource(R.string.diagnostics_logs_explanation),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Action buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onExportClicked,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 8.dp, bottomEnd = 16.dp, bottomStart = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.diagnostics_logs_export_button),
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                OutlinedButton(
                    onClick = onClearClicked,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 8.dp, bottomEnd = 16.dp, bottomStart = 8.dp),
                    enabled = fileCount > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (fileCount > 0) {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.diagnostics_logs_clear_button),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticsSectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 10.dp, bottomEnd = 22.dp, bottomStart = 10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 14.dp),
            ) {
                Surface(
                    modifier = Modifier.size(34.dp),
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 6.dp, bottomEnd = 12.dp, bottomStart = 6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            content()
        }
    }
}
