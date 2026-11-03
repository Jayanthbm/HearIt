package com.jayanth.hearit.diagnostics

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
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayanth.hearit.R

/**
 * Entry point for Diagnostics (master prompt §13, phase P19).
 */
@Composable
fun DiagnosticsRoute(
    viewModel: DiagnosticsViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()

    DiagnosticsScreen(
        state = state,
        onRefresh = viewModel::refresh,
        onOpenNotificationAccess = viewModel::openNotificationAccessSettings,
        onOpenBatterySettings = viewModel::openBatteryOptimizationSettings,
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
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
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

        // Refresh Probes Button
        OutlinedButton(
            onClick = onRefresh,
            enabled = !state.isRefreshing,
            shape = RoundedCornerShape(12.dp),
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
}

@Composable
private fun SystemHealthHeroCard(healthy: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
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
                statusText = if (service.isRunning) "RUNNING" else "STOPPED",
                isOk = service.isRunning,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Speech State
            DiagnosticRow(
                label = "Speaking Activity",
                statusText = if (service.isSpeaking) "Reading out loud" else "Idle",
                isOk = true,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Queue Depth
            DiagnosticRow(
                label = "Pending Queue Items",
                statusText = "${service.pendingCount} queued",
                isOk = true,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Priority Announce
            DiagnosticRow(
                label = "Priority Announce Mode",
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
                    Button(
                        onClick = onOpenNotificationAccess,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                        modifier = Modifier.height(34.dp),
                    ) {
                        Text(text = stringResource(R.string.diagnostics_fix_action), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Post Notifications
            DiagnosticRow(
                label = stringResource(R.string.diagnostics_post_notifications),
                statusText = if (state.isPostNotificationGranted) {
                    stringResource(R.string.diagnostics_status_granted)
                } else {
                    stringResource(R.string.diagnostics_status_missing)
                },
                isOk = state.isPostNotificationGranted,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Bluetooth
            DiagnosticRow(
                label = stringResource(R.string.diagnostics_bluetooth_permission),
                statusText = if (!state.isBluetoothPermissionRequired || state.isBluetoothPermissionGranted) {
                    stringResource(R.string.diagnostics_status_granted)
                } else {
                    stringResource(R.string.diagnostics_status_missing)
                },
                isOk = !state.isBluetoothPermissionRequired || state.isBluetoothPermissionGranted,
            )

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
                OutlinedButton(
                    onClick = onOpenBatterySettings,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp),
                ) {
                    Text(text = "Settings", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun TtsDiagnosticsCard(state: DiagnosticsUiState) {
    DiagnosticsSectionCard(
        title = stringResource(R.string.diagnostics_tts_title),
        subtitle = "Android native TextToSpeech engine",
        icon = Icons.AutoMirrored.Rounded.VolumeUp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Engine Ready
            DiagnosticRow(
                label = "TTS Engine Ready",
                statusText = if (state.isTtsAvailable) "Available" else "Initializing / Missing",
                isOk = state.isTtsAvailable,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // System Locale Supported
            DiagnosticRow(
                label = "Default Locale (${state.defaultLocaleLanguage})",
                statusText = if (state.isTtsLocaleSupported) "Supported" else "Missing Data",
                isOk = state.isTtsLocaleSupported,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Voices Count
            DiagnosticRow(
                label = "Installed Engine Voices",
                statusText = "${state.installedVoiceCount} voices",
                isOk = state.installedVoiceCount > 0,
            )
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
                statusText = "${state.enabledAppsCount} apps",
                isOk = state.enabledAppsCount > 0,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            DiagnosticRow(
                label = "Priority Announce Apps",
                statusText = "${state.priorityAppsCount} apps",
                isOk = true,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            DiagnosticRow(
                label = "Logged History Entries",
                statusText = "${state.totalHistoryCount} entries",
                isOk = true,
            )
        }
    }
}

@Composable
private fun DiagnosticRow(
    label: String,
    statusText: String,
    isOk: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
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
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
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
                color = if (isOk) Color(0xFF34D399) else MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 14.dp),
            ) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = RoundedCornerShape(8.dp),
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
