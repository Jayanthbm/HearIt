package com.jayanth.hearit.settings

import android.app.Activity
import android.os.Build
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
    val intelligenceActions = remember(viewModel) {
        com.jayanth.hearit.intelligence.IntelligenceActions.from(viewModel)
    }
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
        onToggleAutoDismiss = viewModel::onToggleAutoDismiss,
        onToggleShakeToStop = viewModel::onToggleShakeToStop,
        onToggleVolumeKeyToStop = viewModel::onToggleVolumeKeyToStop,
        onToggleMusicPauseHold = viewModel::onToggleMusicPauseHold,
        onToggleBluetoothAutoStart = viewModel::onToggleBluetoothAutoStart,
        onTogglePeriodicCheckin = viewModel::onTogglePeriodicCheckin,
        onPeriodicCheckinIntervalChanged = viewModel::onPeriodicCheckinIntervalChanged,
        onTogglePeriodicCheckinTime = viewModel::onTogglePeriodicCheckinTime,
        onTogglePeriodicCheckinBattery = viewModel::onTogglePeriodicCheckinBattery,
        intelligenceActions = intelligenceActions,
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
    onToggleAutoDismiss: (Boolean) -> Unit = {},
    onToggleShakeToStop: (Boolean) -> Unit = {},
    onToggleVolumeKeyToStop: (Boolean) -> Unit = {},
    onToggleMusicPauseHold: (Boolean) -> Unit = {},
    onToggleBluetoothAutoStart: (Boolean) -> Unit = {},
    onTogglePeriodicCheckin: (Boolean) -> Unit = {},
    onPeriodicCheckinIntervalChanged: (Int) -> Unit = {},
    onTogglePeriodicCheckinTime: (Boolean) -> Unit = {},
    onTogglePeriodicCheckinBattery: (Boolean) -> Unit = {},
    intelligenceActions: com.jayanth.hearit.intelligence.IntelligenceActions =
        com.jayanth.hearit.intelligence.IntelligenceActions(),
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
        // Section 0: Gestures & Playback Controls Card
        GesturesAndControlsCard(
            state = state,
            intelligenceActions = intelligenceActions,
            isAutoDismissEnabled = state.isAutoDismissEnabled,
            isShakeToStopEnabled = state.isShakeToStopEnabled,
            isVolumeKeyToStopEnabled = state.isVolumeKeyToStopEnabled,
            isMusicPauseHoldEnabled = state.isMusicPauseHoldEnabled,
            isBluetoothAutoStartEnabled = state.isBluetoothAutoStartEnabled,
            onToggleAutoDismiss = onToggleAutoDismiss,
            onToggleShakeToStop = onToggleShakeToStop,
            onToggleVolumeKeyToStop = onToggleVolumeKeyToStop,
            onToggleMusicPauseHold = onToggleMusicPauseHold,
            onToggleBluetoothAutoStart = onToggleBluetoothAutoStart,
        )

        // Section 0B: Periodic Spoken Check-in Card (Idea 43)
        PeriodicCheckinCard(
            isEnabled = state.isPeriodicCheckinEnabled,
            intervalMinutes = state.periodicCheckinIntervalMinutes,
            announceTime = state.periodicCheckinAnnounceTime,
            announceBattery = state.periodicCheckinAnnounceBattery,
            announceEarphonesBattery = state.periodicCheckinAnnounceEarphonesBattery,
            announceSummary = state.periodicCheckinAnnounceSummary,
            onToggleEnabled = onTogglePeriodicCheckin,
            onIntervalChanged = onPeriodicCheckinIntervalChanged,
            onToggleTime = onTogglePeriodicCheckinTime,
            onToggleBattery = onTogglePeriodicCheckinBattery,
            onToggleEarphonesBattery = intelligenceActions.onTogglePeriodicCheckinEarphonesBattery,
            onToggleSummary = intelligenceActions.onTogglePeriodicCheckinSummary,
        )

        // Section 0C: Smart Content Cleaning (NLP) & Category Intelligence
        com.jayanth.hearit.intelligence.SmartContentCleaningCard(
            state = state,
            actions = intelligenceActions,
        )

        com.jayanth.hearit.intelligence.CategoryIntelligenceCard(
            state = state,
            actions = intelligenceActions,
        )

        // Section 1: Audio Focus Card
        AudioFocusCard(
            selectedPolicy = state.audioFocus,
            onPolicySelected = onAudioFocusPolicyChanged,
            state = state,
            intelligenceActions = intelligenceActions,
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

        // Section 8: Wear OS Companion Smartwatch (Item 5.1)
        com.jayanth.hearit.devices.WearOsCompanionCard()

        val hasQueueChanges = state.queue != com.jayanth.hearit.core.common.model.QueueSettings() ||
            state.audioFocus != AudioFocusPolicy.DUCK

        // Reset to Defaults Button
        OutlinedButton(
            onClick = { showResetDialog = true },
            enabled = hasQueueChanges,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Icon(
                imageVector = if (hasQueueChanges) Icons.Rounded.RestartAlt else Icons.Rounded.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (hasQueueChanges) {
                    stringResource(R.string.settings_reset)
                } else {
                    "App Preferences at Default"
                },
                fontWeight = FontWeight.SemiBold,
            )
        }

        // App Version Info
        val context = LocalContext.current
        val packageInfo = remember(context) {
            try {
                context.packageManager.getPackageInfo(context.packageName, 0)
            } catch (_: Exception) {
                null
            }
        }
        val appVersionName = packageInfo?.versionName ?: "2.0.0"
        val appVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo?.longVersionCode?.toString() ?: "5"
        } else {
            @Suppress("DEPRECATION")
            packageInfo?.versionCode?.toString() ?: "5"
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Hear It v$appVersionName ($appVersionCode)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
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
    state: GeneralSettingsUiState = GeneralSettingsUiState(),
    intelligenceActions: com.jayanth.hearit.intelligence.IntelligenceActions =
        com.jayanth.hearit.intelligence.IntelligenceActions(),
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

            if (selectedPolicy == AudioFocusPolicy.DUCK) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Custom Media Ducking Level",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Proportionally lower background music volume during speech",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.isDuckingLevelControlEnabled,
                        onCheckedChange = intelligenceActions.onToggleDuckingLevelControl,
                    )
                }

                if (state.isDuckingLevelControlEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            20 to "Quiet (20%)",
                            50 to "Half (50%)",
                            80 to "Subtle (80%)",
                            100 to "OS Default",
                        ).forEach { (pct, label) ->
                            FilterChip(
                                selected = state.duckingVolumePercent == pct,
                                onClick = { intelligenceActions.onDuckingVolumePercentChanged(pct) },
                                label = { Text(label) },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Smooth Audio Return Fade Delay",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0L to "Instant", 300L to "300ms", 500L to "500ms Smooth").forEach { (delay, label) ->
                        FilterChip(
                            selected = state.audioFocusReturnDelayMs == delay,
                            onClick = { intelligenceActions.onAudioFocusReturnDelayChanged(delay) },
                            label = { Text(label) },
                        )
                    }
                }
            }
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
                        text = "App Lock (Biometric / PIN)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (isBiometricAvailable) {
                            "Require authentication to open Hear It"
                        } else {
                            "Set up device PIN or Biometrics in Settings"
                        },
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
                        text = "Secure Screen Display",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Blocks screenshots and app switcher previews",
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
internal fun SettingsCard(
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GesturesAndControlsCard(
    state: GeneralSettingsUiState = GeneralSettingsUiState(),
    intelligenceActions: com.jayanth.hearit.intelligence.IntelligenceActions =
        com.jayanth.hearit.intelligence.IntelligenceActions(),
    isAutoDismissEnabled: Boolean,
    isShakeToStopEnabled: Boolean,
    isVolumeKeyToStopEnabled: Boolean,
    isMusicPauseHoldEnabled: Boolean,
    isBluetoothAutoStartEnabled: Boolean,
    onToggleAutoDismiss: (Boolean) -> Unit,
    onToggleShakeToStop: (Boolean) -> Unit,
    onToggleVolumeKeyToStop: (Boolean) -> Unit,
    onToggleMusicPauseHold: (Boolean) -> Unit,
    onToggleBluetoothAutoStart: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsCard(
        title = "Gestures & Playback Controls",
        subtitle = "Motion shortcuts and smart auto-dismiss actions",
        icon = Icons.Rounded.GraphicEq,
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Auto-Dismiss
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Auto-Dismiss Spoken Alerts",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Clears notification from status bar once announced",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = isAutoDismissEnabled,
                    onCheckedChange = onToggleAutoDismiss,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Flip Face-Down to Stop (#19)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Flip Face-Down to Silence",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Turning the phone face-down immediately stops speaking",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.isFlipToStopEnabled,
                    onCheckedChange = intelligenceActions.onToggleFlipToStop,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Shake to Silence (#19)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Shake Phone to Silence",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Shaking while speaking stops the current announcement",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = isShakeToStopEnabled,
                    onCheckedChange = onToggleShakeToStop,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Shake After Speech to Repeat (#19 / Idea 41)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Shake After Speech to Repeat",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Shake gently within window after speech ends to replay",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.isShakeToRepeatEnabled,
                    onCheckedChange = intelligenceActions.onToggleShakeToRepeat,
                )
            }
            if (state.isShakeToRepeatEnabled) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5_000L to "5s window", 10_000L to "10s window", 15_000L to "15s window").forEach { (ms, lbl) ->
                        FilterChip(
                            selected = state.shakeRepeatWindowMs == ms,
                            onClick = { intelligenceActions.onShakeRepeatWindowChanged(ms) },
                            label = { Text(lbl) },
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Physical Volume Key Silence
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Physical Volume Key Silence",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Press volume up or down to immediately stop speech",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = isVolumeKeyToStopEnabled,
                    onCheckedChange = onToggleVolumeKeyToStop,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Music Pause Hold (#19 configurable duration)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Music Paused Hold",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Delays announcements when you pause music to talk",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = isMusicPauseHoldEnabled,
                    onCheckedChange = onToggleMusicPauseHold,
                )
            }
            if (isMusicPauseHoldEnabled) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        30_000L to "30s",
                        60_000L to "60s",
                        90_000L to "90s",
                        120_000L to "2m",
                    ).forEach { (ms, label) ->
                        FilterChip(
                            selected = state.musicPauseHoldDurationMs == ms,
                            onClick = { intelligenceActions.onMusicPauseHoldDurationChanged(ms) },
                            label = { Text(label) },
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Bluetooth Auto-Start / Auto-Stop / Filter (#27)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Auto-Start on Earphones Connect",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Starts HearIt automatically when Bluetooth audio connects",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = isBluetoothAutoStartEnabled,
                    onCheckedChange = onToggleBluetoothAutoStart,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Auto-Stop on Disconnect",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Stops service cleanly when Bluetooth earphones disconnect",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.isBluetoothAutoStopEnabled,
                    onCheckedChange = intelligenceActions.onToggleBluetoothAutoStop,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Only Allowed Earphones",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Ignore unrecognized Bluetooth headsets unless allowed in Devices",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.isBluetoothAutoOnlyAllowedEnabled,
                    onCheckedChange = intelligenceActions.onToggleBluetoothAutoOnlyAllowed,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Bluetooth Headset Button Controls (#1 MediaSession)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Bluetooth Headset Button Controls",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Pause, skip, or repeat via headset buttons only while HearIt speaks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.isHeadsetMediaControlsEnabled,
                    onCheckedChange = intelligenceActions.onToggleHeadsetMediaControls,
                )
            }
            if (state.isHeadsetMediaControlsEnabled) {
                HeadsetTapMappingRow(
                    tapLabel = "1× Tap",
                    selected = state.headsetSinglePressAction,
                    onSelect = intelligenceActions.onHeadsetSinglePressActionChanged,
                )
                HeadsetTapMappingRow(
                    tapLabel = "2× Tap / Next",
                    selected = state.headsetDoublePressAction,
                    onSelect = intelligenceActions.onHeadsetDoublePressActionChanged,
                )
                HeadsetTapMappingRow(
                    tapLabel = "3× Tap / Prev",
                    selected = state.headsetTriplePressAction,
                    onSelect = intelligenceActions.onHeadsetTriplePressActionChanged,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeadsetTapMappingRow(
    tapLabel: String,
    selected: com.jayanth.hearit.core.common.model.HeadsetButtonAction,
    onSelect: (com.jayanth.hearit.core.common.model.HeadsetButtonAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = tapLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                com.jayanth.hearit.core.common.model.HeadsetButtonAction.PAUSE_RESUME to "Pause/Resume",
                com.jayanth.hearit.core.common.model.HeadsetButtonAction.SKIP to "Skip",
                com.jayanth.hearit.core.common.model.HeadsetButtonAction.REPEAT to "Repeat",
                com.jayanth.hearit.core.common.model.HeadsetButtonAction.NONE to "Off",
            ).forEach { (action, title) ->
                FilterChip(
                    selected = selected == action,
                    onClick = { onSelect(action) },
                    label = { Text(title, fontSize = 11.sp) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PeriodicCheckinCard(
    isEnabled: Boolean,
    intervalMinutes: Int,
    announceTime: Boolean,
    announceBattery: Boolean,
    announceEarphonesBattery: Boolean = true,
    announceSummary: Boolean = true,
    onToggleEnabled: (Boolean) -> Unit,
    onIntervalChanged: (Int) -> Unit,
    onToggleTime: (Boolean) -> Unit,
    onToggleBattery: (Boolean) -> Unit,
    onToggleEarphonesBattery: (Boolean) -> Unit = {},
    onToggleSummary: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    SettingsCard(
        title = "Periodic Spoken Check-in",
        subtitle = "Speaks time and battery status at regular intervals",
        icon = Icons.Rounded.Timer,
        modifier = modifier,
        action = {
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggleEnabled,
            )
        },
    ) {
        if (isEnabled) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Check-in Interval",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(15 to "15m", 30 to "30m", 60 to "1h", 120 to "2h").forEach { (mins, label) ->
                        FilterChip(
                            selected = intervalMinutes == mins,
                            onClick = { onIntervalChanged(mins) },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Text(
                    text = "Include in Announcement",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = announceTime,
                        onClick = { onToggleTime(!announceTime) },
                        label = { Text("Time") },
                    )
                    FilterChip(
                        selected = announceBattery,
                        onClick = { onToggleBattery(!announceBattery) },
                        label = { Text("Phone Battery %") },
                    )
                    FilterChip(
                        selected = announceEarphonesBattery,
                        onClick = { onToggleEarphonesBattery(!announceEarphonesBattery) },
                        label = { Text("Earphones Battery %") },
                    )
                    FilterChip(
                        selected = announceSummary,
                        onClick = { onToggleSummary(!announceSummary) },
                        label = { Text("Summary Count") },
                    )
                }
            }
        } else {
            Text(
                text = "Turn on to receive hands-free time and battery updates periodically through your earphones.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

