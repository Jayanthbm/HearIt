package com.jayanth.hearit.dashboard

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Cable
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationImportant
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.jayanth.hearit.R
import com.jayanth.hearit.applications.ApplicationsRoute
import com.jayanth.hearit.applications.ApplicationsViewModel
import com.jayanth.hearit.applications.GlobalSettingsRoute
import com.jayanth.hearit.applications.GlobalSettingsViewModel
import com.jayanth.hearit.core.common.model.AudioOutputSettings
import com.jayanth.hearit.core.common.model.HistoryStatus
import com.jayanth.hearit.devices.DevicesRoute
import com.jayanth.hearit.devices.DevicesViewModel
import com.jayanth.hearit.diagnostics.DiagnosticsRoute
import com.jayanth.hearit.diagnostics.DiagnosticsViewModel
import com.jayanth.hearit.backup.BackupRestoreRoute
import com.jayanth.hearit.backup.BackupRestoreViewModel
import com.jayanth.hearit.history.HistoryRoute
import com.jayanth.hearit.history.HistoryViewModel
import com.jayanth.hearit.settings.GeneralSettingsRoute
import com.jayanth.hearit.settings.GeneralSettingsViewModel
import com.jayanth.hearit.setup.TestAnnouncementState
import com.jayanth.hearit.voicespeech.VoiceSpeechRoute
import com.jayanth.hearit.voicespeech.VoiceSpeechViewModel

/**
 * Entry point of the dashboard shell (master prompt §13, phase P12).
 *
 * Modern Material 3 Expressive Dashboard with hero service monitor, quick controls,
 * clean recent announcements timeline, and interactive bottom navigation.
 */
@Composable
fun DashboardRoute(
    viewModel: DashboardViewModel,
    applicationsViewModel: ApplicationsViewModel,
    globalSettingsViewModel: GlobalSettingsViewModel,
    historyViewModel: HistoryViewModel,
    devicesViewModel: DevicesViewModel,
    voiceSpeechViewModel: VoiceSpeechViewModel,
    generalSettingsViewModel: GeneralSettingsViewModel,
    backupRestoreViewModel: BackupRestoreViewModel,
    diagnosticsViewModel: DiagnosticsViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    var destinationId by rememberSaveable { mutableStateOf(DashboardDestination.HOME.id) }
    val destination = DashboardDestination.fromId(destinationId)
    val context = LocalContext.current
    val lifecycleOwner = context as? LifecycleOwner

    DisposableEffect(lifecycleOwner) {
        if (lifecycleOwner == null) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) viewModel.onRefresh()
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
    }

    BackHandler(enabled = !destination.isBottomBarDestination) {
        destinationId = DashboardDestination.HOME.id
    }

    DashboardScreen(
        state = state,
        destination = destination,
        onSelectDestination = { selected -> destinationId = selected.id },
        onBackToHome = { destinationId = DashboardDestination.HOME.id },
        onStartService = viewModel::onStartService,
        onStopService = viewModel::onStopService,
        onTestAnnouncement = viewModel::onTestAnnouncement,
        onTemporaryMuteClicked = viewModel::onTemporaryMuteClicked,
        onTemporaryMuteCleared = viewModel::onTemporaryMuteCleared,
        onPriorityAnnounceToggled = viewModel::onPriorityAnnounceToggled,
        onHistorySearch = viewModel::onHistorySearch,
        onHistoryClearAll = viewModel::onHistoryClearAll,
        appsContent = { ApplicationsRoute(viewModel = applicationsViewModel) },
        globalRulesContent = { GlobalSettingsRoute(viewModel = globalSettingsViewModel) },
        voiceSpeechContent = { VoiceSpeechRoute(viewModel = voiceSpeechViewModel) },
        settingsContent = { GeneralSettingsRoute(viewModel = generalSettingsViewModel) },
        backupRestoreContent = { BackupRestoreRoute(viewModel = backupRestoreViewModel) },
        diagnosticsContent = { DiagnosticsRoute(viewModel = diagnosticsViewModel) },
        historyContent = { HistoryRoute(viewModel = historyViewModel) },
        devicesContent = { DevicesRoute(viewModel = devicesViewModel) },
        modifier = modifier,
    )
}

/**
 * The shell: modern header, content and bottom navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    destination: DashboardDestination,
    onSelectDestination: (DashboardDestination) -> Unit,
    onBackToHome: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onTestAnnouncement: (String) -> Unit,
    onTemporaryMuteClicked: (Int) -> Unit,
    onTemporaryMuteCleared: () -> Unit,
    onPriorityAnnounceToggled: (Boolean) -> Unit,
    onHistorySearch: (DashboardFilterState) -> Unit,
    onHistoryClearAll: () -> Unit,
    appsContent: @Composable () -> Unit,
    globalRulesContent: @Composable () -> Unit,
    voiceSpeechContent: @Composable () -> Unit = {},
    settingsContent: @Composable () -> Unit = {},
    backupRestoreContent: @Composable () -> Unit = {},
    diagnosticsContent: @Composable () -> Unit = {},
    historyContent: @Composable () -> Unit,
    devicesContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (destination != DashboardDestination.HOME) {
                ModernDashboardTopBar(
                    destination = destination,
                    isRunning = state.service.isRunning,
                    onBack = onBackToHome,
                )
            }
        },
        bottomBar = {
            ModernBottomNav(selected = destination, onSelect = onSelectDestination)
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
        ) {
            when {
                !state.loaded -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                }

                destination == DashboardDestination.HOME -> DashboardHome(
                    state = state,
                    onStartService = onStartService,
                    onStopService = onStopService,
                    onTestAnnouncement = onTestAnnouncement,
                    onTemporaryMuteClicked = onTemporaryMuteClicked,
                    onTemporaryMuteCleared = onTemporaryMuteCleared,
                    onPriorityAnnounceToggled = onPriorityAnnounceToggled,
                    onHistorySearch = onHistorySearch,
                    onHistoryClearAll = onHistoryClearAll,
                    onSelectDestination = onSelectDestination,
                )

                destination == DashboardDestination.DEVICES -> devicesContent()
                destination == DashboardDestination.APPS -> appsContent()
                destination == DashboardDestination.HISTORY -> historyContent()
                destination == DashboardDestination.GLOBAL_RULES -> globalRulesContent()
                destination == DashboardDestination.VOICE_SPEECH -> voiceSpeechContent()
                destination == DashboardDestination.SETTINGS -> settingsContent()
                destination == DashboardDestination.BACKUP_RESTORE -> backupRestoreContent()
                destination == DashboardDestination.DIAGNOSTICS -> diagnosticsContent()
                else -> DestinationPlaceholder(destination = destination)
            }
        }
    }
}

// ============================================================================
// Top App Bar
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernDashboardTopBar(
    destination: DashboardDestination,
    isRunning: Boolean,
    onBack: () -> Unit,
) {
    // For HOME destination, the header is embedded in DashboardHome — show nothing here.
    if (destination == DashboardDestination.HOME) return

    TopAppBar(
        title = {
            Text(
                text = stringResource(destination.labelRes),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        },
        navigationIcon = {
            if (!destination.isBottomBarDestination) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.dashboard_back),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
    )
}

// ============================================================================
// Modern Bottom Navigation Bar
// ============================================================================

@Composable
private fun ModernBottomNav(
    selected: DashboardDestination,
    onSelect: (DashboardDestination) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        ) {
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (destination in DashboardDestination.bottomBar) {
                    val isSelected = destination == selected
                    val icon = when (destination) {
                        DashboardDestination.HOME -> Icons.Rounded.Home
                        DashboardDestination.APPS -> Icons.Rounded.Apps
                        DashboardDestination.DEVICES -> Icons.Rounded.Headphones
                        DashboardDestination.HISTORY -> Icons.Rounded.History
                        else -> Icons.Rounded.Home
                    }
                    val iconColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "navIconColor",
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "navTextColor",
                    )

                    Column(
                        modifier = Modifier
                            .clickable(role = Role.Tab) { onSelect(destination) }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .defaultMinSize(minWidth = 56.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        // Morphed indicator surface when selected
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 16.dp, topEnd = 8.dp,
                                bottomEnd = 16.dp, bottomStart = 8.dp,
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else Color.Transparent,
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 32.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = stringResource(destination.labelRes),
                                    modifier = Modifier.size(22.dp),
                                    tint = iconColor,
                                )
                            }
                        }
                        Text(
                            text = stringResource(destination.labelRes),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = textColor,
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// Dashboard Home Surface
// ============================================================================

@Composable
private fun DashboardHome(
    state: DashboardUiState,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onTestAnnouncement: (String) -> Unit,
    onTemporaryMuteClicked: (Int) -> Unit,
    onTemporaryMuteCleared: () -> Unit,
    onPriorityAnnounceToggled: (Boolean) -> Unit,
    onHistorySearch: (DashboardFilterState) -> Unit,
    onHistoryClearAll: () -> Unit,
    onSelectDestination: (DashboardDestination) -> Unit,
) {
    val testAnnouncementText = stringResource(R.string.dashboard_test_announcement_spoken)
    var showMuteDurationDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Embedded Home Header (replaces TopAppBar for HOME) ──────────────
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "Good day",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            StatusPill(status = if (state.service.isRunning) HistoryStatus.ANNOUNCED else HistoryStatus.SKIPPED)
        }

        // Hero Service Monitor Card
        HeroServiceCard(
            state = state,
            onStart = onStartService,
            onStop = onStopService,
        )

        // Quick Controls & Status
        Text(
            text = "Quick Controls",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        val isRunning = state.service.isRunning
        val muteActive = state.temporaryMuteActive && isRunning
        val priorityEnabled = isRunning && !muteActive

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Priority Announce Card — morphed squircle (top-heavy)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(enabled = priorityEnabled) { onPriorityAnnounceToggled(!state.priorityAnnounceActive) },
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 8.dp, bottomEnd = 20.dp, bottomStart = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        !isRunning || muteActive -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f)
                        state.priorityAnnounceActive -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.surfaceContainer
                    },
                ),
                border = BorderStroke(
                    1.dp,
                    when {
                        !isRunning || muteActive -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        state.priorityAnnounceActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.outlineVariant
                    },
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.size(34.dp),
                            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 4.dp, bottomEnd = 12.dp, bottomStart = 4.dp),
                            color = when {
                                !isRunning || muteActive -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                state.priorityAnnounceActive -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Star,
                                    contentDescription = null,
                                    tint = when {
                                        !isRunning || muteActive -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                        state.priorityAnnounceActive -> MaterialTheme.colorScheme.onPrimary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        Switch(
                            checked = state.priorityAnnounceActive,
                            enabled = priorityEnabled,
                            onCheckedChange = onPriorityAnnounceToggled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                    Column {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Priority Announce",
                            style = MaterialTheme.typography.titleSmall,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (priorityEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when {
                                !isRunning -> "Requires service"
                                muteActive && state.priorityAnnounceActive -> "Muted · Resumes on unmute"
                                muteActive -> "Muted by timer"
                                state.priorityAnnounceActive -> "Bypassing lock rules"
                                else -> "Normal rules active"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (priorityEnabled) 1f else 0.5f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // Temporary Mute Card — alternating morphed squircle
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(enabled = isRunning) { showMuteDurationDialog = true },
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 20.dp, bottomEnd = 8.dp, bottomStart = 20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        !isRunning -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f)
                        muteActive -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                        else -> MaterialTheme.colorScheme.surfaceContainer
                    },
                ),
                border = BorderStroke(
                    1.dp,
                    when {
                        !isRunning -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        muteActive -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.outlineVariant
                    },
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.size(34.dp),
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 12.dp, bottomEnd = 4.dp, bottomStart = 12.dp),
                            color = when {
                                !isRunning -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                muteActive -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.VolumeOff,
                                    contentDescription = null,
                                    tint = when {
                                        !isRunning -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                        muteActive -> MaterialTheme.colorScheme.onError
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        Switch(
                            checked = muteActive,
                            enabled = isRunning,
                            onCheckedChange = { active ->
                                if (active) {
                                    onTemporaryMuteClicked(30)
                                } else {
                                    onTemporaryMuteCleared()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onError,
                                checkedTrackColor = MaterialTheme.colorScheme.error,
                            ),
                        )
                    }
                    Column {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Temporary Mute",
                            style = MaterialTheme.typography.titleSmall,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isRunning) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when {
                                !isRunning -> "Requires service"
                                muteActive -> "Announcements muted"
                                else -> "Normal audio active"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                !isRunning -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                muteActive -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        // Test Voice Announcement Bar — morphed CTA style
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 8.dp, bottomEnd = 20.dp, bottomStart = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 4.dp, bottomEnd = 12.dp, bottomStart = 4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Test Speech",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (state.testAnnouncement == TestAnnouncementState.SPEAKING) "Speaking sample…" else "Play test phrase",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Button(
                    onClick = { onTestAnnouncement(testAnnouncementText) },
                    enabled = state.testAnnouncement != TestAnnouncementState.SPEAKING,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 6.dp, bottomEnd = 16.dp, bottomStart = 6.dp),
                    modifier = Modifier.height(40.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    if (state.testAnnouncement == TestAnnouncementState.SPEAKING) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Play", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Recent Announcements Feed
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Recent Announcements",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            TextButton(onClick = { onSelectDestination(DashboardDestination.HISTORY) }) {
                Text("View all", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            }
        }

        RecentHistorySection(
            state = state,
            onSelectDestination = onSelectDestination,
        )

        // Settings and Tools
        Text(
            text = "Settings & Rules",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        ToolsGroupCard(onSelectDestination = onSelectDestination)

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showMuteDurationDialog) {
        AlertDialog(
            onDismissRequest = { showMuteDurationDialog = false },
            title = {
                Text(
                    text = "Temporary Mute",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Silence all speech announcements temporarily without disabling apps.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val durations = listOf(15 to "15 minutes", 30 to "30 minutes", 60 to "1 hour")
                    durations.forEach { (mins, label) ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onTemporaryMuteClicked(mins)
                                    showMuteDurationDialog = false
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                    if (state.temporaryMuteActive) {
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(
                            onClick = {
                                onTemporaryMuteCleared()
                                showMuteDurationDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Unmute now", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMuteDurationDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            shape = RoundedCornerShape(24.dp),
        )
    }
}

// ============================================================================
// Hero Service Card
// ============================================================================

@Composable
private fun HeroServiceCard(
    state: DashboardUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val isRunning = state.service.isRunning
    val isSpeaking = state.service.isSpeaking

    // Animated pulse for the running state dot
    val infiniteTransition = rememberInfiniteTransition(label = "heroPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    val runningGreen = Color(0xFF10B981)
    val runningGreenText = Color(0xFF34D399)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 12.dp, bottomEnd = 28.dp, bottomStart = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        border = BorderStroke(
            1.5.dp,
            if (isRunning) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Live Status Row with animated pulsing dot
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Pulsing dot container
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(20.dp),
                    ) {
                        if (isRunning) {
                            // Glowing animated ring
                            Box(
                                modifier = Modifier
                                    .size((10 * pulseScale).dp)
                                    .clip(CircleShape)
                                    .background(runningGreen.copy(alpha = pulseAlpha * 0.3f)),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isRunning) runningGreen else Color(0xFF94A3B8)),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSpeaking) "Speaking Announcement…" else if (isRunning) "Active & Listening" else "Service Inactive",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isRunning) runningGreenText else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (state.service.pendingCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(topStart = 10.dp, topEnd = 4.dp, bottomEnd = 10.dp, bottomStart = 4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = "${state.service.pendingCount} queued",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hero title
            Text(
                text = if (isRunning) "HearIt is running" else "HearIt is stopped",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Main Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MetricChip(
                    icon = Icons.Rounded.Headphones,
                    label = "Audio Output",
                    value = state.outputLabel ?: "No Output",
                    modifier = Modifier.weight(1f),
                )
                MetricChip(
                    icon = Icons.Rounded.Apps,
                    label = "Enabled Apps",
                    value = "${state.enabledAppCount} apps",
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Big CTA Button — morphed squircle shape
            if (isRunning) {
                OutlinedButton(
                    onClick = onStop,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 10.dp, bottomEnd = 24.dp, bottomStart = 10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.dashboard_stop_service), fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 10.dp, bottomEnd = 24.dp, bottomStart = 10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.dashboard_start_service), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MetricChip(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 6.dp, bottomEnd = 14.dp, bottomStart = 6.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(28.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ============================================================================
// Recent History Section
// ============================================================================

@Composable
private fun RecentHistorySection(
    state: DashboardUiState,
    onSelectDestination: (DashboardDestination) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 8.dp, bottomStart = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            when {
                state.historyUnavailable -> {
                    Text(
                        text = stringResource(R.string.dashboard_recent_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                !state.recentHistoryLoaded -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }

                state.recentHistory.isEmpty() -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsNone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.dashboard_recent_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                else -> {
                    state.recentHistory.forEachIndexed { index, entry ->
                        RecentRowItem(entry = entry)
                        if (index < state.recentHistory.size - 1) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentRowItem(entry: RecentAnnouncement) {
    val relativeTime = DateUtils.getRelativeTimeSpanString(
        entry.recordedAtMillis,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 4.dp, bottomEnd = 12.dp, bottomStart = 4.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = entry.appName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = relativeTime.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                )
            }
        }

        // Status Chip
        StatusPill(status = entry.status)
    }
}

@Composable
private fun StatusPill(status: HistoryStatus) {
    val (bgColor, textColor, label) = when (status) {
        HistoryStatus.ANNOUNCED -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, "Running")
        HistoryStatus.SKIPPED -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, "Stopped")
        HistoryStatus.QUEUED -> Triple(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, "Queued")
        HistoryStatus.PENDING -> Triple(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, "Pending")
        HistoryStatus.INTERRUPTED -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, "Interrupted")
    }

    Surface(
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 4.dp, bottomEnd = 12.dp, bottomStart = 4.dp),
        color = bgColor,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

// ============================================================================
// Tools and Settings Group
// ============================================================================

@Composable
private fun ToolsGroupCard(onSelectDestination: (DashboardDestination) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 8.dp, bottomEnd = 20.dp, bottomStart = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            for ((index, destination) in DashboardDestination.secondary.withIndex()) {
                val icon = when (destination) {
                    DashboardDestination.GLOBAL_RULES -> Icons.Rounded.Tune
                    DashboardDestination.VOICE_SPEECH -> Icons.Rounded.RecordVoiceOver
                    DashboardDestination.SETTINGS -> Icons.Rounded.Settings
                    DashboardDestination.BACKUP_RESTORE -> Icons.Rounded.Backup
                    DashboardDestination.DIAGNOSTICS -> Icons.Rounded.BugReport
                    else -> Icons.Rounded.Settings
                }

                ToolRow(
                    icon = icon,
                    label = stringResource(destination.labelRes),
                    onClick = { onSelectDestination(destination) },
                )

                if (index < DashboardDestination.secondary.size - 1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Tinted icon container with morphed squircle
            Surface(
                modifier = Modifier.size(34.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

// ============================================================================
// Placeholder
// ============================================================================

@Composable
private fun DestinationPlaceholder(destination: DashboardDestination) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Text(
            text = stringResource(destination.labelRes),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.dashboard_placeholder_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
