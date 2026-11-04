package com.jayanth.hearit.settings

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jayanth.hearit.R
import com.jayanth.hearit.core.common.model.AudioFocusPolicy
import com.jayanth.hearit.core.common.model.OverflowPolicy

/**
 * Entry point for General Settings (Queue, Grouping, Pacing, Audio Focus & Service Notification).
 *
 * Master prompt §8 and phase P15.
 */
@Composable
fun GeneralSettingsRoute(
    viewModel: GeneralSettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    // Refresh notification channel status whenever user resumes from Android settings
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshServiceNotificationStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    GeneralSettingsScreen(
        state = state,
        onGroupingWindowChanged = viewModel::onGroupingWindowChanged,
        onMaxMessagesChanged = viewModel::onMaxMessagesChanged,
        onDelayChanged = viewModel::onDelayChanged,
        onMinGapChanged = viewModel::onMinGapChanged,
        onExpiryChanged = viewModel::onExpiryChanged,
        onGlobalMaxSizeChanged = viewModel::onGlobalMaxSizeChanged,
        onPerAppMaxSizeChanged = viewModel::onPerAppMaxSizeChanged,
        onOverflowPolicyChanged = viewModel::onOverflowPolicyChanged,
        onRepeatNotificationUpdatesChanged = viewModel::onRepeatNotificationUpdatesChanged,
        onAudioFocusPolicyChanged = viewModel::onAudioFocusPolicyChanged,
        onOpenServiceNotificationSettings = viewModel::openServiceNotificationSettings,
        onToggleSecureScreen = viewModel::onToggleSecureScreen,
        onToggleBiometric = {
            activity?.let {
                viewModel.onToggleBiometricLock(it, !state.security.biometricLockEnabled)
            }
        },
        onDismissSecurityError = viewModel::onClearSecurityErrorMessage,
        onResetToDefaults = viewModel::onResetToDefaults,
        modifier = modifier,
    )
}

/**
 * Modern Material 3 Expressive General Settings screen.
 */
@Composable
fun GeneralSettingsScreen(
    state: GeneralSettingsUiState,
    onGroupingWindowChanged: (Long) -> Unit,
    onMaxMessagesChanged: (Int) -> Unit,
    onDelayChanged: (Long) -> Unit,
    onMinGapChanged: (Long) -> Unit,
    onExpiryChanged: (Long) -> Unit,
    onGlobalMaxSizeChanged: (Int) -> Unit,
    onPerAppMaxSizeChanged: (Int) -> Unit,
    onOverflowPolicyChanged: (OverflowPolicy) -> Unit,
    onRepeatNotificationUpdatesChanged: (Boolean) -> Unit,
    onAudioFocusPolicyChanged: (AudioFocusPolicy) -> Unit,
    onOpenServiceNotificationSettings: () -> Unit,
    onToggleSecureScreen: (Boolean) -> Unit,
    onToggleBiometric: () -> Unit,
    onDismissSecurityError: () -> Unit,
    onResetToDefaults: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showResetDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Optional Security Error Banner
        state.securityErrorMessage?.let { error ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
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
                    TextButton(onClick = onDismissSecurityError) {
                        Text(
                            text = stringResource(R.string.diagnostics_logs_dismiss),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
        // Section 1: Audio Focus Card
        AudioFocusCard(
            selectedPolicy = state.audioFocus,
            onPolicySelected = onAudioFocusPolicyChanged,
        )

        // Section 2: Grouping & Batching Card
        GroupingCard(
            groupingWindowMillis = state.queue.groupingWindowMillis,
            maxMessagesPerAnnouncement = state.queue.maxMessagesPerAnnouncement,
            onGroupingWindowChanged = onGroupingWindowChanged,
            onMaxMessagesChanged = onMaxMessagesChanged,
        )

        // Section 3: Delivery Pacing Card
        PacingCard(
            delayMillis = state.queue.delayBeforeAnnouncementMillis,
            minGapMillis = state.queue.minGapBetweenAnnouncementsMillis,
            onDelayChanged = onDelayChanged,
            onMinGapChanged = onMinGapChanged,
        )

        // Section 4: Queue Capacity & Expiry Card
        QueueCapacityCard(
            globalMaxSize = state.queue.globalMaxSize,
            perAppMaxSize = state.queue.perAppMaxSize,
            expiryMillis = state.queue.expiryMillis,
            overflowPolicy = state.queue.overflowPolicy,
            onGlobalMaxSizeChanged = onGlobalMaxSizeChanged,
            onPerAppMaxSizeChanged = onPerAppMaxSizeChanged,
            onExpiryChanged = onExpiryChanged,
            onOverflowPolicyChanged = onOverflowPolicyChanged,
        )

        // Section 5: Notification Updates Card
        NotificationUpdatesCard(
            repeatUpdates = state.queue.repeatNotificationUpdates,
            onRepeatUpdatesChanged = onRepeatNotificationUpdatesChanged,
        )

        // Section 6: Persistent Service Notification Card
        ServiceNotificationCard(
            isVisible = state.isServiceNotificationVisible,
            onToggleClicked = { showNotificationDialog = true },
            onConfigureClicked = onOpenServiceNotificationSettings,
        )

        // Section 7: Security & Device Privacy Card (Master Prompt §15)
        SecurityPrivacyCard(
            biometricLockEnabled = state.security.biometricLockEnabled,
            secureScreenEnabled = state.security.secureScreenEnabled,
            isBiometricAvailable = state.isBiometricAvailable,
            onToggleBiometric = onToggleBiometric,
            onToggleSecureScreen = onToggleSecureScreen,
        )

        // Reset to Defaults Button
        OutlinedButton(
            onClick = { showResetDialog = true },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.RestartAlt,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.settings_reset),
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.settings_reset_confirm_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.settings_reset_confirm_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        onResetToDefaults()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.settings_reset),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showNotificationDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.settings_service_notification_dialog_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.settings_service_notification_dialog_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNotificationDialog = false
                        onOpenServiceNotificationSettings()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.settings_service_notification_dialog_confirm),
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationDialog = false }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

/**
 * Section Card for Audio Focus & Media interaction.
 */
@Composable
private fun AudioFocusCard(
    selectedPolicy: AudioFocusPolicy,
    onPolicySelected: (AudioFocusPolicy) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsCard(
        title = stringResource(R.string.settings_audio_focus_title),
        subtitle = stringResource(R.string.settings_audio_focus_desc),
        icon = Icons.AutoMirrored.Rounded.VolumeUp,
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val options = listOf(
                Triple(
                    AudioFocusPolicy.DUCK,
                    stringResource(R.string.settings_audio_focus_duck),
                    stringResource(R.string.settings_audio_focus_duck_desc),
                ),
                Triple(
                    AudioFocusPolicy.PAUSE,
                    stringResource(R.string.settings_audio_focus_pause),
                    stringResource(R.string.settings_audio_focus_pause_desc),
                ),
                Triple(
                    AudioFocusPolicy.NONE,
                    stringResource(R.string.settings_audio_focus_none),
                    stringResource(R.string.settings_audio_focus_none_desc),
                ),
            )

            for ((policy, label, desc) in options) {
                val isSelected = selectedPolicy == policy
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPolicySelected(policy) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onPolicySelected(policy) },
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Grouping and Batching Card.
 */
@Composable
private fun GroupingCard(
    groupingWindowMillis: Long,
    maxMessagesPerAnnouncement: Int,
    onGroupingWindowChanged: (Long) -> Unit,
    onMaxMessagesChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsCard(
        title = stringResource(R.string.settings_grouping_title),
        subtitle = stringResource(R.string.settings_grouping_desc),
        icon = Icons.Rounded.Layers,
        modifier = modifier,
    ) {
        Column {
            // Grouping Window
            Text(
                text = stringResource(R.string.settings_grouping_window_label),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.settings_grouping_window_desc),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
            )

            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val windowOptions = listOf(
                    0L to "0s (Off)",
                    3_000L to "3s",
                    5_000L to "5s (Default)",
                    10_000L to "10s",
                )
                for ((millis, label) in windowOptions) {
                    val isSelected = groupingWindowMillis == millis
                    FilterChip(
                        selected = isSelected,
                        onClick = { onGroupingWindowChanged(millis) },
                        label = { Text(text = label, style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 14.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )

            // Max Messages
            Text(
                text = stringResource(R.string.settings_max_messages_label),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.settings_max_messages_desc),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
            )

            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val messageOptions = listOf(
                    1 to "1 (Single)",
                    2 to "2",
                    3 to "3 (Default)",
                    5 to "5",
                    10 to "10",
                )
                for ((count, label) in messageOptions) {
                    val isSelected = maxMessagesPerAnnouncement == count
                    FilterChip(
                        selected = isSelected,
                        onClick = { onMaxMessagesChanged(count) },
                        label = { Text(text = label, style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * Pacing and Timing Card.
 */
@Composable
private fun PacingCard(
    delayMillis: Long,
    minGapMillis: Long,
    onDelayChanged: (Long) -> Unit,
    onMinGapChanged: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsCard(
        title = stringResource(R.string.settings_timing_title),
        subtitle = stringResource(R.string.settings_timing_desc),
        icon = Icons.Rounded.Timer,
        modifier = modifier,
    ) {
        Column {
            // Delay Before Announcement
            Text(
                text = stringResource(R.string.settings_delay_label),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.settings_delay_desc),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
            )

            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val delayOptions = listOf(
                    0L to "0s (Immediate)",
                    1_000L to "1s",
                    2_000L to "2s",
                    3_000L to "3s",
                )
                for ((millis, label) in delayOptions) {
                    val isSelected = delayMillis == millis
                    FilterChip(
                        selected = isSelected,
                        onClick = { onDelayChanged(millis) },
                        label = { Text(text = label, style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 14.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )

            // Minimum Gap
            Text(
                text = stringResource(R.string.settings_min_gap_label),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.settings_min_gap_desc),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
            )

            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val gapOptions = listOf(
                    0L to "0s (None)",
                    500L to "500ms",
                    1_000L to "1s",
                    2_000L to "2s",
                )
                for ((millis, label) in gapOptions) {
                    val isSelected = minGapMillis == millis
                    FilterChip(
                        selected = isSelected,
                        onClick = { onMinGapChanged(millis) },
                        label = { Text(text = label, style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * Queue Capacity and Eviction Card.
 */
@Composable
private fun QueueCapacityCard(
    globalMaxSize: Int,
    perAppMaxSize: Int,
    expiryMillis: Long,
    overflowPolicy: OverflowPolicy,
    onGlobalMaxSizeChanged: (Int) -> Unit,
    onPerAppMaxSizeChanged: (Int) -> Unit,
    onExpiryChanged: (Long) -> Unit,
    onOverflowPolicyChanged: (OverflowPolicy) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsCard(
        title = stringResource(R.string.settings_limits_title),
        subtitle = stringResource(R.string.settings_limits_desc),
        icon = Icons.Rounded.Storage,
        modifier = modifier,
    ) {
        Column {
            // Total Queue Capacity
            Text(
                text = stringResource(R.string.settings_global_max_label),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val globalOptions = listOf(20 to "20", 50 to "50 (Default)", 100 to "100")
                for ((size, label) in globalOptions) {
                    val isSelected = globalMaxSize == size
                    FilterChip(
                        selected = isSelected,
                        onClick = { onGlobalMaxSizeChanged(size) },
                        label = { Text(text = label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )

            // Per App Capacity
            Text(
                text = stringResource(R.string.settings_per_app_max_label),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val perAppOptions = listOf(5 to "5", 10 to "10 (Default)", 20 to "20")
                for ((size, label) in perAppOptions) {
                    val isSelected = perAppMaxSize == size
                    FilterChip(
                        selected = isSelected,
                        onClick = { onPerAppMaxSizeChanged(size) },
                        label = { Text(text = label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )

            // Expiry
            Text(
                text = stringResource(R.string.settings_expiry_label),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.settings_expiry_desc),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 6.dp),
            )

            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val expiryOptions = listOf(
                    5 * 60_000L to "5m",
                    15 * 60_000L to "15m",
                    30 * 60_000L to "30m (Default)",
                    60 * 60_000L to "1h",
                )
                for ((millis, label) in expiryOptions) {
                    val isSelected = expiryMillis == millis
                    FilterChip(
                        selected = isSelected,
                        onClick = { onExpiryChanged(millis) },
                        label = { Text(text = label, style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )

            // Eviction Policy
            Text(
                text = stringResource(R.string.settings_overflow_label),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.settings_overflow_desc),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val overflowOptions = listOf(
                    OverflowPolicy.DISCARD_OLDEST to stringResource(R.string.settings_overflow_discard_oldest),
                    OverflowPolicy.DISCARD_NEWEST to stringResource(R.string.settings_overflow_discard_newest),
                    OverflowPolicy.KEEP_HIGHEST_PRIORITY to stringResource(R.string.settings_overflow_keep_priority),
                )

                for ((policy, label) in overflowOptions) {
                    val isSelected = overflowPolicy == policy
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOverflowPolicyChanged(policy) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onOverflowPolicyChanged(policy) },
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Section Card for Re-announcing Notification Updates.
 */
@Composable
private fun NotificationUpdatesCard(
    repeatUpdates: Boolean,
    onRepeatUpdatesChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsCard(
        title = stringResource(R.string.settings_updates_title),
        subtitle = "Master prompt §10 notification update handling",
        icon = Icons.Rounded.Sync,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onRepeatUpdatesChanged(!repeatUpdates) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = stringResource(R.string.settings_repeat_updates_label),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.settings_repeat_updates_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = repeatUpdates,
                onCheckedChange = onRepeatUpdatesChanged,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}

/**
 * Section Card for Persistent Service Notification in Notification Panel.
 */
@Composable
private fun ServiceNotificationCard(
    isVisible: Boolean,
    onToggleClicked: () -> Unit,
    onConfigureClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsCard(
        title = stringResource(R.string.settings_service_notification_title),
        subtitle = stringResource(R.string.settings_service_notification_desc),
        icon = if (isVisible) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Live Status Pill
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isVisible) {
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                } else {
                    Color(0xFF10B981).copy(alpha = 0.15f)
                },
                border = BorderStroke(
                    1.dp,
                    if (isVisible) {
                        MaterialTheme.colorScheme.outlineVariant
                    } else {
                        Color(0xFF10B981).copy(alpha = 0.4f)
                    },
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = if (isVisible) Icons.Rounded.NotificationsActive else Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = if (isVisible) MaterialTheme.colorScheme.primary else Color(0xFF10B981),
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(
                            if (isVisible) {
                                R.string.settings_service_notification_status_visible
                            } else {
                                R.string.settings_service_notification_status_hidden
                            },
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isVisible) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            Color(0xFF047857)
                        },
                    )
                }
            }

            // Interactive Switch Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleClicked() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = stringResource(R.string.settings_service_notification_visible_label),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(
                            if (isVisible) {
                                R.string.settings_service_notification_visible_on
                            } else {
                                R.string.settings_service_notification_visible_off
                            },
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = isVisible,
                    onCheckedChange = { onToggleClicked() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
            }

            // Explanatory note
            Text(
                text = stringResource(R.string.settings_service_notification_explanation),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Direct button to open Android notification settings
            FilledTonalButton(
                onClick = onConfigureClicked,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 44.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.settings_service_notification_configure_button),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            // Tip underneath
            Text(
                text = stringResource(R.string.settings_service_notification_tip),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SecurityPrivacyCard(
    biometricLockEnabled: Boolean,
    secureScreenEnabled: Boolean,
    isBiometricAvailable: Boolean,
    onToggleBiometric: () -> Unit,
    onToggleSecureScreen: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsCard(
        title = stringResource(R.string.settings_security_title),
        subtitle = stringResource(R.string.settings_security_subtitle),
        icon = Icons.Rounded.Security,
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Item 1: Biometric & Device Credential Gate
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = isBiometricAvailable || biometricLockEnabled) { onToggleBiometric() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = stringResource(R.string.settings_security_biometric_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(
                            if (isBiometricAvailable) {
                                R.string.settings_security_biometric_desc
                            } else {
                                R.string.settings_security_biometric_unavailable
                            },
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isBiometricAvailable) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                }
                Switch(
                    checked = biometricLockEnabled,
                    onCheckedChange = { onToggleBiometric() },
                    enabled = isBiometricAvailable || biometricLockEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 1.dp,
            )

            // Item 2: Secure Screen Mode (FLAG_SECURE)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleSecureScreen(!secureScreenEnabled) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = stringResource(R.string.settings_security_secure_screen_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.settings_security_secure_screen_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = secureScreenEnabled,
                    onCheckedChange = onToggleSecureScreen,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
            }
        }
    }
}

/**
 * Reusable M3 section card wrapper with squircle icon badge.
 */
@Composable
private fun SettingsCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 10.dp, bottomEnd = 24.dp, bottomStart = 10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp),
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
