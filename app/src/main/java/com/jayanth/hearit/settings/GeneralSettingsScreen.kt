package com.jayanth.hearit.settings

import android.app.Activity
import com.jayanth.hearit.readAppVersionName
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
        onPauseBetweenSectionsChanged = viewModel::onPauseBetweenSectionsChanged,
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
    onPauseBetweenSectionsChanged: (Long) -> Unit = {},
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
        // Section 1: Delivery - grouping, pacing, queue limits and update handling
        DeliveryCard(
            groupingWindowMillis = state.queue.groupingWindowMillis,
            maxMessagesPerAnnouncement = state.queue.maxMessagesPerAnnouncement,
            delayMillis = state.queue.delayBeforeAnnouncementMillis,
            minGapMillis = state.queue.minGapBetweenAnnouncementsMillis,
            pauseBetweenSectionsMillis = state.pauseBetweenSectionsMillis,
            globalMaxSize = state.queue.globalMaxSize,
            perAppMaxSize = state.queue.perAppMaxSize,
            expiryMillis = state.queue.expiryMillis,
            overflowPolicy = state.queue.overflowPolicy,
            repeatUpdates = state.queue.repeatNotificationUpdates,
            musicPauseHoldEnabled = state.isMusicPauseHoldEnabled,
            musicPauseHoldDurationMs = state.musicPauseHoldDurationMs,
            onGroupingWindowChanged = onGroupingWindowChanged,
            onMaxMessagesChanged = onMaxMessagesChanged,
            onDelayChanged = onDelayChanged,
            onMinGapChanged = onMinGapChanged,
            onPauseBetweenSectionsChanged = onPauseBetweenSectionsChanged,
            onGlobalMaxSizeChanged = onGlobalMaxSizeChanged,
            onPerAppMaxSizeChanged = onPerAppMaxSizeChanged,
            onExpiryChanged = onExpiryChanged,
            onOverflowPolicyChanged = onOverflowPolicyChanged,
            onRepeatUpdatesChanged = onRepeatNotificationUpdatesChanged,
            onToggleMusicPauseHold = onToggleMusicPauseHold,
            onMusicPauseHoldDurationChanged = intelligenceActions.onMusicPauseHoldDurationChanged,
        )

        // Section 2: Audio Focus & Maps Navigation Yield
        AudioFocusCard(
            selectedPolicy = state.audioFocus,
            onPolicySelected = onAudioFocusPolicyChanged,
            state = state,
            intelligenceActions = intelligenceActions,
        )

        // Section 3: Gestures
        GesturesAndControlsCard(
            state = state,
            intelligenceActions = intelligenceActions,
            isAutoDismissEnabled = state.isAutoDismissEnabled,
            isShakeToStopEnabled = state.isShakeToStopEnabled,
            isVolumeKeyToStopEnabled = state.isVolumeKeyToStopEnabled,
            onToggleAutoDismiss = onToggleAutoDismiss,
            onToggleShakeToStop = onToggleShakeToStop,
            onToggleVolumeKeyToStop = onToggleVolumeKeyToStop,
        )

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
        val appVersionName = remember(context) { context.readAppVersionName() }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "HearIt v$appVersionName",
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
                            text = "Ducking Level",
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
                                label = { Text(label, maxLines = 2) },
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
                            label = { Text(label, maxLines = 2) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Yield to Navigation",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Pauses active notifications immediately when turn-by-turn guidance or Maps alerts play",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.isNavigationYieldEnabled,
                    onCheckedChange = intelligenceActions.onToggleNavigationYield,
                )
            }

            if (state.isNavigationYieldEnabled) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Re-announce Afterward",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "Queues paused notification and speaks it again after navigation guidance finishes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.isReannounceAfterNavigationEnabled,
                        onCheckedChange = intelligenceActions.onToggleReannounceAfterNavigation,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Resume Delay After Turn Guidance",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        1_000L to "1s Smooth",
                        2_000L to "2s Balanced",
                        3_000L to "3s Extra",
                    ).forEach { (delayMs, label) ->
                        FilterChip(
                            selected = state.navigationYieldSettleDelayMs == delayMs,
                            onClick = { intelligenceActions.onNavigationYieldSettleDelayChanged(delayMs) },
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
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        maxLines = 2,
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
    onToggleAutoDismiss: (Boolean) -> Unit,
    onToggleShakeToStop: (Boolean) -> Unit,
    onToggleVolumeKeyToStop: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsCard(
        title = "Gestures",
        subtitle = "Shake, flip and volume-key shortcuts, plus auto-dismiss",
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
                        text = "Auto-Dismiss Alerts",
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

            // Tap-to-Repeat Last Announcement Shortcut (#25)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Repeat Last Shortcut (Home & Notification)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Shows 1-tap replay button on Home screen & ongoing notification",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.isRepeatLastShortcutEnabled,
                    onCheckedChange = intelligenceActions.onToggleRepeatLastShortcut,
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
                        text = "Shake to Repeat",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Shake within this window after speech to replay",
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
                    listOf(5_000L to "5 s", 10_000L to "10 s", 15_000L to "15 s").forEach { (ms, lbl) ->
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
                        text = "Volume Key Silence",
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
        title = "Spoken Check-in",
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
                    listOf(15 to "15 min", 30 to "30 min", 60 to "1 hr", 120 to "2 hr").forEach { (mins, label) ->
                        FilterChip(
                            selected = intervalMinutes == mins,
                            onClick = { onIntervalChanged(mins) },
                            label = {
                                Text(
                                    text = label,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            },
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


/**
 * Delivery: everything that decides when an announcement is spoken, how it is batched, and
 * which queued item survives. Queue limits sit behind an Advanced expander because they are
 * tuning, not day-to-day choices.
 */
@Composable
private fun DeliveryCard(
    groupingWindowMillis: Long,
    maxMessagesPerAnnouncement: Int,
    delayMillis: Long,
    minGapMillis: Long,
    pauseBetweenSectionsMillis: Long,
    globalMaxSize: Int,
    perAppMaxSize: Int,
    expiryMillis: Long,
    overflowPolicy: OverflowPolicy,
    repeatUpdates: Boolean,
    musicPauseHoldEnabled: Boolean,
    musicPauseHoldDurationMs: Long,
    onGroupingWindowChanged: (Long) -> Unit,
    onMaxMessagesChanged: (Int) -> Unit,
    onDelayChanged: (Long) -> Unit,
    onMinGapChanged: (Long) -> Unit,
    onPauseBetweenSectionsChanged: (Long) -> Unit,
    onGlobalMaxSizeChanged: (Int) -> Unit,
    onPerAppMaxSizeChanged: (Int) -> Unit,
    onExpiryChanged: (Long) -> Unit,
    onOverflowPolicyChanged: (OverflowPolicy) -> Unit,
    onRepeatUpdatesChanged: (Boolean) -> Unit,
    onToggleMusicPauseHold: (Boolean) -> Unit,
    onMusicPauseHoldDurationChanged: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var advancedExpanded by rememberSaveable { mutableStateOf(false) }

    SettingsCard(
        title = "Delivery",
        subtitle = "Grouping, timing, queue limits and updates",
        icon = Icons.Rounded.Timer,
        modifier = modifier,
    ) {
        Column {
            DeliveryLabel("Message Grouping", "Batch rapid notifications from the same app")
            DeliverySegmented(
                options = listOf(0L to "Off", 3_000L to "3 s", 5_000L to "5 s", 10_000L to "10 s"),
                selected = groupingWindowMillis,
                onSelect = onGroupingWindowChanged,
            )

            DeliveryGap()
            DeliveryLabel("Max Grouped Messages")
            DeliverySegmented(
                options = listOf(1 to "1", 2 to "2", 3 to "3", 5 to "5", 10 to "10"),
                selected = maxMessagesPerAnnouncement,
                onSelect = onMaxMessagesChanged,
            )

            DeliveryLine()

            DeliveryLabel("Pre-Speech Delay")
            DeliverySegmented(
                options = listOf(0L to "Off", 1_000L to "1 s", 2_000L to "2 s", 3_000L to "3 s"),
                selected = delayMillis,
                onSelect = onDelayChanged,
            )

            DeliveryGap()
            DeliveryLabel("Pause Between Announcements")
            DeliverySegmented(
                options = listOf(0L to "Off", 500L to "500 ms", 1_000L to "1 s", 2_000L to "2 s"),
                selected = minGapMillis,
                onSelect = onMinGapChanged,
            )

            DeliveryGap()
            DeliveryLabel("Pause Inside an Announcement", "Silence between app name, sender and message")
            DeliverySegmented(
                options = listOf(0L to "Off", 250L to "250 ms", 500L to "500 ms", 1_000L to "1 s"),
                selected = pauseBetweenSectionsMillis,
                onSelect = onPauseBetweenSectionsChanged,
            )

            DeliveryLine()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Music Pause Hold",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Waits before speaking while your music is paused",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
                Switch(checked = musicPauseHoldEnabled, onCheckedChange = onToggleMusicPauseHold)
            }
            if (musicPauseHoldEnabled) {
                DeliveryGap()
                DeliverySegmented(
                    options = listOf(
                        30_000L to "30 s",
                        60_000L to "60 s",
                        90_000L to "90 s",
                        120_000L to "120 s",
                    ),
                    selected = musicPauseHoldDurationMs,
                    onSelect = onMusicPauseHoldDurationChanged,
                )
            }

            DeliveryLine()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Repeat Updates",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Speaks again when an app updates a queued notification",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
                Switch(checked = repeatUpdates, onCheckedChange = onRepeatUpdatesChanged)
            }

            DeliveryLine()

            TextButton(
                onClick = { advancedExpanded = !advancedExpanded },
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
            ) {
                Text(
                    text = if (advancedExpanded) "Hide advanced" else "Advanced",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (advancedExpanded) {
                DeliveryGap()
                DeliveryLabel("Total Queue Capacity")
                DeliverySegmented(
                    options = listOf(20 to "20", 50 to "50", 100 to "100"),
                    selected = globalMaxSize,
                    onSelect = onGlobalMaxSizeChanged,
                )

                DeliveryGap()
                DeliveryLabel("Per-App Capacity")
                DeliverySegmented(
                    options = listOf(5 to "5", 10 to "10", 20 to "20"),
                    selected = perAppMaxSize,
                    onSelect = onPerAppMaxSizeChanged,
                )

                DeliveryGap()
                DeliveryLabel("Announcement Expiry")
                DeliverySegmented(
                    options = listOf(
                        5 * 60_000L to "5 min",
                        15 * 60_000L to "15 min",
                        30 * 60_000L to "30 min",
                        60 * 60_000L to "1 hour",
                    ),
                    selected = expiryMillis,
                    onSelect = onExpiryChanged,
                )

                DeliveryGap()
                DeliveryLabel("Overflow Policy")
                DeliverySegmented(
                    options = listOf(
                        OverflowPolicy.DISCARD_OLDEST to "Oldest",
                        OverflowPolicy.DISCARD_NEWEST to "Newest",
                        OverflowPolicy.KEEP_HIGHEST_PRIORITY to "Priority",
                    ),
                    selected = overflowPolicy,
                    onSelect = onOverflowPolicyChanged,
                )
            }
        }
    }
}

@Composable
private fun DeliveryLabel(title: String, hint: String? = null) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    if (hint != null) {
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun <T> DeliverySegmented(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = selected == value,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

@Composable
private fun DeliveryGap() {
    Spacer(modifier = Modifier.height(14.dp))
}

@Composable
private fun DeliveryLine() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 14.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}
