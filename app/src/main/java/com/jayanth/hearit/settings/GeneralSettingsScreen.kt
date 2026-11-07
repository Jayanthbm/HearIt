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
import androidx.compose.material.icons.rounded.Info
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.text.style.TextOverflow
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

        Spacer(modifier = Modifier.height(100.dp))
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
    var showInfoDialog by remember { mutableStateOf(false) }

    SettingsCard(
        title = "Audio Focus",
        subtitle = "Ducking and pause behavior during media",
        icon = Icons.AutoMirrored.Rounded.VolumeUp,
        modifier = modifier,
        action = {
            IconButton(
                onClick = { showInfoDialog = true },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = "Audio Focus Details",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
    ) {
        Column {
            Text(
                text = "Media Playback Interaction",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(6.dp))

            val focusOptions = listOf(
                AudioFocusPolicy.DUCK to "Duck",
                AudioFocusPolicy.PAUSE to "Pause",
                AudioFocusPolicy.NONE to "Ignore",
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                focusOptions.forEachIndexed { index, (policy, label) ->
                    val isSelected = selectedPolicy == policy
                    SegmentedButton(
                        selected = isSelected,
                        onClick = { onPolicySelected(policy) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = focusOptions.size),
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val description = when (selectedPolicy) {
                AudioFocusPolicy.DUCK -> stringResource(R.string.settings_audio_focus_duck_desc)
                AudioFocusPolicy.PAUSE -> stringResource(R.string.settings_audio_focus_pause_desc)
                AudioFocusPolicy.NONE -> stringResource(R.string.settings_audio_focus_none_desc)
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 15.sp,
            )
        }
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            title = {
                Text(
                    text = "Audio Focus Policies",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    AudioFocusPolicyInfoItem(
                        title = "Duck (Recommended)",
                        description = "Temporarily lowers the volume of background music, podcasts, or videos while announcing notifications, then restores the original volume.",
                    )
                    AudioFocusPolicyInfoItem(
                        title = "Pause",
                        description = "Pauses background audio playback completely during speech, and automatically resumes playback once the announcement finishes.",
                    )
                    AudioFocusPolicyInfoItem(
                        title = "Ignore",
                        description = "Speaks over any currently playing media without requesting audio focus from the system or changing background volume levels.",
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text(text = "Got it", fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}

@Composable
private fun AudioFocusPolicyInfoItem(
    title: String,
    description: String,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp,
        )
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
        title = "Message Grouping",
        subtitle = "Batch rapid notifications from the same app",
        icon = Icons.Rounded.Layers,
        modifier = modifier,
    ) {
        Column {
            Text(
                text = "Grouping Window",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(6.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val windowOptions = listOf(
                    0L to "Off",
                    3_000L to "3s",
                    5_000L to "5s",
                    10_000L to "10s",
                )
                windowOptions.forEachIndexed { index, (millis, label) ->
                    val isSelected = groupingWindowMillis == millis
                    SegmentedButton(
                        selected = isSelected,
                        onClick = { onGroupingWindowChanged(millis) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = windowOptions.size),
                    ) {
                        Text(text = label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 14.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )

            Text(
                text = "Max Grouped Messages",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(6.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val messageOptions = listOf(
                    1 to "1",
                    2 to "2",
                    3 to "3",
                    5 to "5",
                    10 to "10",
                )
                messageOptions.forEachIndexed { index, (count, label) ->
                    val isSelected = maxMessagesPerAnnouncement == count
                    SegmentedButton(
                        selected = isSelected,
                        onClick = { onMaxMessagesChanged(count) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = messageOptions.size),
                    ) {
                        Text(text = label, style = MaterialTheme.typography.labelSmall)
                    }
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
        title = "Delivery Timing",
        subtitle = "Delays and pauses between announcements",
        icon = Icons.Rounded.Timer,
        modifier = modifier,
    ) {
        Column {
            Text(
                text = "Pre-Speech Delay",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(6.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val delayOptions = listOf(
                    0L to "0s",
                    1_000L to "1s",
                    2_000L to "2s",
                    3_000L to "3s",
                )
                delayOptions.forEachIndexed { index, (millis, label) ->
                    val isSelected = delayMillis == millis
                    SegmentedButton(
                        selected = isSelected,
                        onClick = { onDelayChanged(millis) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = delayOptions.size),
                    ) {
                        Text(text = label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 14.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )

            Text(
                text = "Pause Between Announcements",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(6.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val gapOptions = listOf(
                    0L to "0s",
                    500L to "500ms",
                    1_000L to "1s",
                    2_000L to "2s",
                )
                gapOptions.forEachIndexed { index, (millis, label) ->
                    val isSelected = minGapMillis == millis
                    SegmentedButton(
                        selected = isSelected,
                        onClick = { onMinGapChanged(millis) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = gapOptions.size),
                    ) {
                        Text(text = label, style = MaterialTheme.typography.labelSmall)
                    }
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
        title = "Queue Capacity",
        subtitle = "Buffer limits and eviction policies",
        icon = Icons.Rounded.Storage,
        modifier = modifier,
    ) {
        Column {
            Text(
                text = "Total Queue Capacity",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(6.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val globalOptions = listOf(20 to "20", 50 to "50", 100 to "100")
                globalOptions.forEachIndexed { index, (size, label) ->
                    val isSelected = globalMaxSize == size
                    SegmentedButton(
                        selected = isSelected,
                        onClick = { onGlobalMaxSizeChanged(size) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = globalOptions.size),
                    ) {
                        Text(text = label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )

            Text(
                text = "Per-App Capacity",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(6.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val perAppOptions = listOf(5 to "5", 10 to "10", 20 to "20")
                perAppOptions.forEachIndexed { index, (size, label) ->
                    val isSelected = perAppMaxSize == size
                    SegmentedButton(
                        selected = isSelected,
                        onClick = { onPerAppMaxSizeChanged(size) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = perAppOptions.size),
                    ) {
                        Text(text = label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )

            Text(
                text = "Announcement Expiry",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(6.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val expiryOptions = listOf(
                    5 * 60_000L to "5m",
                    15 * 60_000L to "15m",
                    30 * 60_000L to "30m",
                    60 * 60_000L to "1h",
                )
                expiryOptions.forEachIndexed { index, (millis, label) ->
                    val isSelected = expiryMillis == millis
                    SegmentedButton(
                        selected = isSelected,
                        onClick = { onExpiryChanged(millis) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = expiryOptions.size),
                    ) {
                        Text(text = label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )

            Text(
                text = "Overflow Policy",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(6.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val overflowOptions = listOf(
                    OverflowPolicy.DISCARD_OLDEST to "Oldest",
                    OverflowPolicy.DISCARD_NEWEST to "Newest",
                    OverflowPolicy.KEEP_HIGHEST_PRIORITY to "Priority",
                )
                overflowOptions.forEachIndexed { index, (policy, label) ->
                    val isSelected = overflowPolicy == policy
                    SegmentedButton(
                        selected = isSelected,
                        onClick = { onOverflowPolicyChanged(policy) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = overflowOptions.size),
                    ) {
                        Text(text = label, style = MaterialTheme.typography.labelSmall)
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
        title = "Notification Updates",
        subtitle = "Re-announce updated notifications",
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
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
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
        title = "Notification Panel",
        subtitle = "Hear It foreground service tile",
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
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
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
                fontSize = 11.sp,
                lineHeight = 15.sp,
            )

            // Direct button to open Android notification settings
            FilledTonalButton(
                onClick = onConfigureClicked,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Configure in Android Settings",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
        title = "Security & Privacy",
        subtitle = "Biometric lock and secure screen display",
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
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
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
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
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
    action: (@Composable () -> Unit)? = null,
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
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                Surface(
                    modifier = Modifier.size(34.dp),
                    shape = RoundedCornerShape(12.dp),
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
                Column(modifier = Modifier.weight(1f)) {
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
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                    )
                }
                if (action != null) {
                    action()
                }
            }
            content()
        }
    }
}
