package com.jayanth.hearit.dashboard

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Cable
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Nightlight
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
import androidx.compose.material.icons.rounded.WbSunny
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
import androidx.compose.ui.graphics.graphicsLayer
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
import com.jayanth.hearit.core.common.model.LockCondition
import com.jayanth.hearit.core.common.model.ScreenCondition
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
    var backStack by rememberSaveable { mutableStateOf(listOf(DashboardDestination.HOME.id)) }
    val currentDestinationId = backStack.lastOrNull() ?: DashboardDestination.HOME.id
    val destination = DashboardDestination.fromId(currentDestinationId)
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

    val onNavigateBack = {
        if (backStack.size > 1) {
            backStack = backStack.dropLast(1)
        } else if (destination != DashboardDestination.HOME) {
            backStack = listOf(DashboardDestination.HOME.id)
        }
    }

    val onNavigateToHistoryWithFilter = { status: HistoryStatus? ->
        historyViewModel.onStatusChanged(status)
        backStack = backStack + DashboardDestination.HISTORY.id
    }

    BackHandler(enabled = backStack.size > 1 || destination != DashboardDestination.HOME) {
        onNavigateBack()
    }

    DashboardScreen(
        state = state,
        destination = destination,
        onSelectDestination = { selected ->
            if (selected.isBottomBarDestination) {
                backStack = listOf(selected.id)
            } else {
                backStack = backStack + selected.id
            }
        },
        onNavigateToHistoryWithFilter = onNavigateToHistoryWithFilter,
        onBackToHome = onNavigateBack,
        onStartService = viewModel::onStartService,
        onStopService = viewModel::onStopService,
        onTestAnnouncement = viewModel::onTestAnnouncement,
        onDismissTestSpeech = viewModel::onDismissTestSpeech,
        onTemporaryMuteClicked = viewModel::onTemporaryMuteClicked,
        onTemporaryMuteCleared = viewModel::onTemporaryMuteCleared,
        onPriorityAnnounceToggled = viewModel::onPriorityAnnounceToggled,
        onScreenConditionChanged = viewModel::onScreenConditionChanged,
        onLockConditionChanged = viewModel::onLockConditionChanged,
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
    onNavigateToHistoryWithFilter: (HistoryStatus?) -> Unit = {},
    onBackToHome: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onTestAnnouncement: (String) -> Unit,
    onDismissTestSpeech: () -> Unit = {},
    onTemporaryMuteClicked: (Int) -> Unit,
    onTemporaryMuteCleared: () -> Unit,
    onPriorityAnnounceToggled: (Boolean) -> Unit,
    onScreenConditionChanged: (ScreenCondition) -> Unit = {},
    onLockConditionChanged: (LockCondition) -> Unit = {},
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
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .consumeWindowInsets(PaddingValues(top = innerPadding.calculateTopPadding())),
        ) {
            if (!state.loaded) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                }
            } else {
                AnimatedContent(
                    targetState = destination,
                    transitionSpec = {
                        val fromOrder = getDestinationOrder(initialState)
                        val toOrder = getDestinationOrder(targetState)
                        if (toOrder > fromOrder) {
                            slideInHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) { it } +
                                fadeIn(animationSpec = tween(280)) togetherWith
                                slideOutHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) { -it } +
                                fadeOut(animationSpec = tween(280))
                        } else if (toOrder < fromOrder) {
                            slideInHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) { -it } +
                                fadeIn(animationSpec = tween(280)) togetherWith
                                slideOutHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) { it } +
                                fadeOut(animationSpec = tween(280))
                        } else {
                            fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
                        }
                    },
                    label = "DashboardTabTransition",
                    modifier = Modifier.fillMaxSize(),
                ) { currentDest ->
                    when (currentDest) {
                        DashboardDestination.HOME -> DashboardHome(
                            state = state,
                            onStartService = onStartService,
                            onStopService = onStopService,
                            onTestAnnouncement = onTestAnnouncement,
                            onDismissTestSpeech = onDismissTestSpeech,
                            onTemporaryMuteClicked = onTemporaryMuteClicked,
                            onTemporaryMuteCleared = onTemporaryMuteCleared,
                            onPriorityAnnounceToggled = onPriorityAnnounceToggled,
                            onScreenConditionChanged = onScreenConditionChanged,
                            onLockConditionChanged = onLockConditionChanged,
                            onHistorySearch = onHistorySearch,
                            onHistoryClearAll = onHistoryClearAll,
                            onSelectDestination = onSelectDestination,
                            onNavigateToHistoryWithFilter = onNavigateToHistoryWithFilter,
                        )

                        DashboardDestination.DEVICES -> devicesContent()
                        DashboardDestination.APPS -> appsContent()
                        DashboardDestination.SETTINGS -> SettingsHubScreen(onSelectDestination = onSelectDestination)
                        DashboardDestination.HISTORY -> historyContent()
                        DashboardDestination.GLOBAL_RULES -> globalRulesContent()
                        DashboardDestination.VOICE_SPEECH -> voiceSpeechContent()
                        DashboardDestination.PREFERENCES -> settingsContent()
                        DashboardDestination.BACKUP_RESTORE -> backupRestoreContent()
                        DashboardDestination.DIAGNOSTICS -> diagnosticsContent()
                    }
                }
            }

            ModernBottomNav(
                selected = destination,
                onSelect = onSelectDestination,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

private fun getDestinationOrder(dest: DashboardDestination): Int = when (dest) {
    DashboardDestination.HOME -> 0
    DashboardDestination.APPS -> 1
    DashboardDestination.SETTINGS -> 2
    DashboardDestination.DEVICES -> 3
    DashboardDestination.VOICE_SPEECH -> 4
    DashboardDestination.PREFERENCES -> 5
    DashboardDestination.GLOBAL_RULES -> 6
    DashboardDestination.HISTORY -> 7
    DashboardDestination.BACKUP_RESTORE -> 8
    DashboardDestination.DIAGNOSTICS -> 9
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
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.padding(start = 4.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(38.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.dashboard_back),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
    )
}

// ============================================================================
// Modern Floating Pill Navigation Bar (Morphe Style)
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModernBottomNav(
    selected: DashboardDestination,
    onSelect: (DashboardDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isImeVisible = WindowInsets.isImeVisible
    AnimatedVisibility(
        visible = !isImeVisible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.90f),
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .widthIn(max = 312.dp)
                    .height(64.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    for (destination in DashboardDestination.bottomBar) {
                        val isSelected = destination == selected ||
                            (destination == DashboardDestination.SETTINGS && !selected.isBottomBarDestination)
                        val icon = when (destination) {
                            DashboardDestination.HOME -> Icons.Rounded.Home
                            DashboardDestination.APPS -> Icons.Rounded.Apps
                            DashboardDestination.SETTINGS -> Icons.Rounded.Settings
                            else -> Icons.Rounded.Home
                        }

                        FloatingPillItem(
                            selected = isSelected,
                            icon = icon,
                            label = stringResource(destination.labelRes),
                            onClick = { onSelect(destination) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingPillItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pillBgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        else Color.Transparent,
        label = "pillBg",
    )
    val pillBorderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        else Color.Transparent,
        label = "pillBorder",
    )
    val iconTint by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
        label = "pillIconTint",
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
        label = "pillTextColor",
    )

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = pillBgColor,
        border = BorderStroke(1.dp, pillBorderColor),
        modifier = modifier.fillMaxHeight(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = textColor,
                fontSize = 11.sp,
                maxLines = 1,
            )
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
    onDismissTestSpeech: () -> Unit = {},
    onTemporaryMuteClicked: (Int) -> Unit,
    onTemporaryMuteCleared: () -> Unit,
    onPriorityAnnounceToggled: (Boolean) -> Unit,
    onScreenConditionChanged: (ScreenCondition) -> Unit = {},
    onLockConditionChanged: (LockCondition) -> Unit = {},
    onHistorySearch: (DashboardFilterState) -> Unit,
    onHistoryClearAll: () -> Unit,
    onSelectDestination: (DashboardDestination) -> Unit,
    onNavigateToHistoryWithFilter: (HistoryStatus?) -> Unit,
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
        val greeting = remember {
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            when (hour) {
                in 5..11 -> "Good Morning"
                in 12..16 -> "Good Afternoon"
                in 17..20 -> "Good Evening"
                else -> "Good Night"
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = greeting,
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
            ServiceStatusPill(isRunning = state.service.isRunning, isSpeaking = state.service.isSpeaking)
        }

        // Hero Service Monitor Card
        HeroServiceCard(
            state = state,
            onStart = onStartService,
            onStop = onStopService,
            onSelectDestination = onSelectDestination,
        )

        // ── Today's Activity Section (Interactive Stats with History Deep Link) ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Today's Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Surface(
                onClick = { onNavigateToHistoryWithFilter(null) },
                shape = RoundedCornerShape(8.dp),
                color = Color.Transparent,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "History",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Spoken Today Stat Card
            ActivityStatCard(
                count = state.todayAnnouncedCount,
                label = "Spoken",
                caption = "Announcements read",
                icon = Icons.AutoMirrored.Rounded.VolumeUp,
                accentColor = Color(0xFF10B981),
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                onClick = { onNavigateToHistoryWithFilter(HistoryStatus.ANNOUNCED) },
                modifier = Modifier.weight(1f),
            )

            // Skipped Today Stat Card
            ActivityStatCard(
                count = state.todaySkippedCount,
                label = "Skipped",
                caption = "Rules or privacy filter",
                icon = Icons.Rounded.NotificationsNone,
                accentColor = MaterialTheme.colorScheme.secondary,
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                onClick = { onNavigateToHistoryWithFilter(HistoryStatus.SKIPPED) },
                modifier = Modifier.weight(1f),
            )
        }

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
            // Priority Announce Card with Spring Animations
            val priorityInteractionSource = remember { MutableInteractionSource() }
            val priorityPressed by priorityInteractionSource.collectIsPressedAsState()
            val priorityScale by animateFloatAsState(
                targetValue = if (priorityPressed && priorityEnabled) 0.97f else 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "priorityScale",
            )
            val priorityCardBg by animateColorAsState(
                targetValue = when {
                    !isRunning || muteActive -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f)
                    state.priorityAnnounceActive -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    else -> MaterialTheme.colorScheme.surfaceContainer
                },
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "priorityCardBg",
            )
            val priorityCardBorder by animateColorAsState(
                targetValue = when {
                    !isRunning || muteActive -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    state.priorityAnnounceActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    else -> MaterialTheme.colorScheme.outlineVariant
                },
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "priorityCardBorder",
            )
            val priorityIconBg by animateColorAsState(
                targetValue = when {
                    !isRunning || muteActive -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    state.priorityAnnounceActive -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "priorityIconBg",
            )
            val priorityIconTint by animateColorAsState(
                targetValue = when {
                    !isRunning || muteActive -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    state.priorityAnnounceActive -> MaterialTheme.colorScheme.onPrimary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "priorityIconTint",
            )

            Card(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer {
                        scaleX = priorityScale
                        scaleY = priorityScale
                    }
                    .clickable(
                        enabled = priorityEnabled,
                        interactionSource = priorityInteractionSource,
                        indication = null,
                    ) { onPriorityAnnounceToggled(!state.priorityAnnounceActive) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = priorityCardBg),
                border = BorderStroke(1.dp, priorityCardBorder),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.size(28.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = priorityIconBg,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Star,
                                    contentDescription = null,
                                    tint = priorityIconTint,
                                    modifier = Modifier.size(16.dp),
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
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Priority Announce",
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (priorityEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(1.dp))
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
                        fontSize = 10.5.sp,
                        lineHeight = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Temporary Mute Card with Spring Animations & Dialog Opener
            val muteInteractionSource = remember { MutableInteractionSource() }
            val mutePressed by muteInteractionSource.collectIsPressedAsState()
            val muteScale by animateFloatAsState(
                targetValue = if (mutePressed && isRunning) 0.97f else 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "muteScale",
            )
            val muteCardBg by animateColorAsState(
                targetValue = when {
                    !isRunning -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f)
                    muteActive -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                    else -> MaterialTheme.colorScheme.surfaceContainer
                },
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "muteCardBg",
            )
            val muteCardBorder by animateColorAsState(
                targetValue = when {
                    !isRunning -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    muteActive -> MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                    else -> MaterialTheme.colorScheme.outlineVariant
                },
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "muteCardBorder",
            )
            val muteIconBg by animateColorAsState(
                targetValue = when {
                    !isRunning -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    muteActive -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "muteIconBg",
            )
            val muteIconTint by animateColorAsState(
                targetValue = when {
                    !isRunning -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    muteActive -> MaterialTheme.colorScheme.onError
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "muteIconTint",
            )

            Card(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer {
                        scaleX = muteScale
                        scaleY = muteScale
                    }
                    .clickable(
                        enabled = isRunning,
                        interactionSource = muteInteractionSource,
                        indication = null,
                    ) { showMuteDurationDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = muteCardBg),
                border = BorderStroke(1.dp, muteCardBorder),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.size(28.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = muteIconBg,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.VolumeOff,
                                    contentDescription = null,
                                    tint = muteIconTint,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                        Switch(
                            checked = muteActive,
                            enabled = isRunning,
                            onCheckedChange = { active ->
                                if (active) {
                                    showMuteDurationDialog = true
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
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Temporary Mute",
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isRunning) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    val muteSubtitle = when {
                        !isRunning -> "Requires service"
                        muteActive -> when (state.temporaryMuteMinutes) {
                            15 -> "Muted · 15m timer"
                            30 -> "Muted · 30m timer"
                            60 -> "Muted · 1h timer"
                            120 -> "Muted · 2h timer"
                            -1 -> "Muted until turned off"
                            else -> "Muted until unmuted"
                        }
                        else -> "Normal audio active"
                    }
                    Text(
                        text = muteSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            !isRunning -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            muteActive -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontSize = 10.5.sp,
                        lineHeight = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // Quick Action Section
        Text(
            text = "Quick Action",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        val quickActionsEnabled = !state.priorityAnnounceActive
        val isScreenOffActive = state.screenCondition == ScreenCondition.OFF || state.screenCondition == ScreenCondition.BOTH
        val isScreenOnActive = state.screenCondition == ScreenCondition.ON || state.screenCondition == ScreenCondition.BOTH
        val isLockedActive = state.lockCondition == LockCondition.LOCKED || state.lockCondition == LockCondition.BOTH
        val isUnlockedActive = state.lockCondition == LockCondition.UNLOCKED || state.lockCondition == LockCondition.BOTH

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QuickActionCard(
                    title = "Screen Off",
                    icon = Icons.Rounded.Nightlight,
                    isActive = isScreenOffActive,
                    enabled = quickActionsEnabled,
                    onClick = {
                        val nextCondition = when (state.screenCondition) {
                            ScreenCondition.OFF -> ScreenCondition.ON
                            ScreenCondition.ON -> ScreenCondition.BOTH
                            ScreenCondition.BOTH -> ScreenCondition.ON
                        }
                        onScreenConditionChanged(nextCondition)
                    },
                    modifier = Modifier.weight(1f),
                )
                QuickActionCard(
                    title = "Screen On",
                    icon = Icons.Rounded.WbSunny,
                    isActive = isScreenOnActive,
                    enabled = quickActionsEnabled,
                    onClick = {
                        val nextCondition = when (state.screenCondition) {
                            ScreenCondition.ON -> ScreenCondition.OFF
                            ScreenCondition.OFF -> ScreenCondition.BOTH
                            ScreenCondition.BOTH -> ScreenCondition.OFF
                        }
                        onScreenConditionChanged(nextCondition)
                    },
                    modifier = Modifier.weight(1f),
                )
                QuickActionCard(
                    title = "Locked",
                    icon = Icons.Rounded.Lock,
                    isActive = isLockedActive,
                    enabled = quickActionsEnabled,
                    onClick = {
                        val nextCondition = when (state.lockCondition) {
                            LockCondition.LOCKED -> LockCondition.UNLOCKED
                            LockCondition.UNLOCKED -> LockCondition.BOTH
                            LockCondition.BOTH -> LockCondition.UNLOCKED
                        }
                        onLockConditionChanged(nextCondition)
                    },
                    modifier = Modifier.weight(1f),
                )
                QuickActionCard(
                    title = "Unlocked",
                    icon = Icons.Rounded.LockOpen,
                    isActive = isUnlockedActive,
                    enabled = quickActionsEnabled,
                    onClick = {
                        val nextCondition = when (state.lockCondition) {
                            LockCondition.UNLOCKED -> LockCondition.LOCKED
                            LockCondition.LOCKED -> LockCondition.BOTH
                            LockCondition.BOTH -> LockCondition.LOCKED
                        }
                        onLockConditionChanged(nextCondition)
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            if (!quickActionsEnabled) {
                Text(
                    text = "Bypassed while Priority Announce is active",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }

        // Test Voice Announcement Bar — hidden once 5 announcements have occurred or manually dismissed
        if (!state.testSpeechDismissed && state.totalAnnouncementsCount < 5) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 42.dp, top = 12.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            Surface(
                                modifier = Modifier.size(36.dp),
                                shape = RoundedCornerShape(12.dp),
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
                            Column(modifier = Modifier.weight(1f)) {
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

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { onTestAnnouncement(testAnnouncementText) },
                            enabled = state.testAnnouncement != TestAnnouncementState.SPEAKING,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(38.dp),
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

                    IconButton(
                        onClick = onDismissTestSpeech,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Dismiss test announcement",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
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
                    val durations = listOf(
                        15 to "15 minutes",
                        30 to "30 minutes",
                        60 to "1 hour",
                        120 to "2 hours",
                        -1 to "Until I turn off",
                    )
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

@Composable
private fun ActivityStatCard(
    count: Long,
    label: String,
    caption: String,
    icon: ImageVector,
    accentColor: Color,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "statCardScale",
    )

    Card(
        modifier = modifier
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top Row: Icon vertically centered on left, Number centered in middle, small subtle arrow
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accentColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle spanning from left to right
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: ImageVector,
    isActive: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "quickActionScale",
    )

    val containerBg by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f)
            isActive -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.surfaceContainer
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "quickActionBg",
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
            isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            else -> MaterialTheme.colorScheme.outlineVariant
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "quickActionBorder",
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
            isActive -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "quickActionContentColor",
    )

    Card(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                modifier = Modifier.size(30.dp),
                shape = RoundedCornerShape(8.dp),
                color = when {
                    !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isActive && enabled) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp,
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
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
    onSelectDestination: (DashboardDestination) -> Unit,
) {
    val isRunning = state.service.isRunning

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        border = BorderStroke(
            1.5.dp,
            if (isRunning) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Apps count shortcut badge & Queued pill row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Surface(
                    modifier = Modifier.clickable { onSelectDestination(DashboardDestination.APPS) },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Apps,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${state.enabledAppCount} apps enabled for speech",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }

                if (state.service.pendingCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
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

            Spacer(modifier = Modifier.height(14.dp))

            // Dedicated Audio Output Tile (Full Width - ample room for earphone name without "...")
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectDestination(DashboardDestination.DEVICES) },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val isConnected = state.outputLabel != null
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isConnected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isConnected) Icons.Rounded.Headphones else Icons.AutoMirrored.Rounded.VolumeOff,
                                contentDescription = null,
                                tint = if (isConnected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Audio Output",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = state.outputLabel ?: "No Output Device",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isConnected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isConnected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (isConnected) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981)),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = if (isConnected) "Active" else "Route",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Animated Big CTA Button — smooth morph between Start and Stop states with tactile press feedback
            val ctaInteractionSource = remember { MutableInteractionSource() }
            val ctaPressed by ctaInteractionSource.collectIsPressedAsState()
            val ctaScale by animateFloatAsState(
                targetValue = if (ctaPressed) 0.96f else 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "ctaScale",
            )
            val ctaBgColor by animateColorAsState(
                targetValue = if (isRunning) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.primary,
                animationSpec = tween(350, easing = FastOutSlowInEasing),
                label = "ctaBgColor",
            )
            val ctaContentColor by animateColorAsState(
                targetValue = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimary,
                animationSpec = tween(350, easing = FastOutSlowInEasing),
                label = "ctaContentColor",
            )
            val ctaBorderColor by animateColorAsState(
                targetValue = if (isRunning) MaterialTheme.colorScheme.error.copy(alpha = 0.8f) else Color.Transparent,
                animationSpec = tween(350, easing = FastOutSlowInEasing),
                label = "ctaBorderColor",
            )

            Surface(
                onClick = { if (isRunning) onStop() else onStart() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .graphicsLayer {
                        scaleX = ctaScale
                        scaleY = ctaScale
                    },
                shape = RoundedCornerShape(16.dp),
                color = ctaBgColor,
                border = BorderStroke(1.2.dp, ctaBorderColor),
                interactionSource = ctaInteractionSource,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedContent(
                        targetState = isRunning,
                        transitionSpec = {
                            (fadeIn(tween(260)) + scaleIn(initialScale = 0.88f, animationSpec = tween(260))) togetherWith
                                (fadeOut(tween(180)) + scaleOut(targetScale = 0.88f, animationSpec = tween(180)))
                        },
                        label = "ctaContent",
                    ) { running ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = if (running) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = ctaContentColor,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(if (running) R.string.dashboard_stop_service else R.string.dashboard_start_service),
                                fontWeight = FontWeight.Bold,
                                color = ctaContentColor,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceStatusPill(
    isRunning: Boolean,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier,
) {
    val greenColor = Color(0xFF10B981)
    val stoppedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val label = when {
        isSpeaking -> "Speaking…"
        isRunning -> "Running"
        else -> "Stopped"
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isRunning) greenColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, if (isRunning) greenColor.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (isRunning) greenColor else stoppedColor.copy(alpha = 0.5f)),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isRunning) greenColor else stoppedColor,
                fontSize = 12.sp,
            )
        }
    }
}


// ============================================================================
// Settings Hub Screen (Morphe Style Control Center)
// ============================================================================

@Composable
private fun SettingsHubScreen(
    onSelectDestination: (DashboardDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Section 1: Audio & Hardware
        SettingsCategorySection(
            categoryIcon = Icons.Rounded.Headphones,
            categoryTitle = "Audio & Hardware",
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column {
                    SettingsHubRow(
                        icon = Icons.Rounded.Headphones,
                        title = "Devices & Audio Routing",
                        subtitle = "Bluetooth, wired, and speaker routing",
                        onClick = { onSelectDestination(DashboardDestination.DEVICES) },
                    )
                    SettingsHubDivider()
                    SettingsHubRow(
                        icon = Icons.Rounded.RecordVoiceOver,
                        title = "Voice & Speech Engine",
                        subtitle = "Speed, pitch, voices, and spoken format",
                        onClick = { onSelectDestination(DashboardDestination.VOICE_SPEECH) },
                    )
                    SettingsHubDivider()
                    SettingsHubRow(
                        icon = Icons.AutoMirrored.Rounded.VolumeUp,
                        title = "Audio Focus & Media",
                        subtitle = "Ducking, media pausing, and message grouping",
                        onClick = { onSelectDestination(DashboardDestination.PREFERENCES) },
                    )
                }
            }
        }

        // Section 2: Rules & Activity
        SettingsCategorySection(
            categoryIcon = Icons.Rounded.Tune,
            categoryTitle = "Rules & Activity",
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column {
                    SettingsHubRow(
                        icon = Icons.Rounded.Tune,
                        title = "Global Eligibility Rules",
                        subtitle = "Screen, lock, OTP shield, character limit",
                        onClick = { onSelectDestination(DashboardDestination.GLOBAL_RULES) },
                    )
                    SettingsHubDivider()
                    SettingsHubRow(
                        icon = Icons.Rounded.History,
                        title = "Notification History",
                        subtitle = "Browse, filter, and replay announcements",
                        onClick = { onSelectDestination(DashboardDestination.HISTORY) },
                    )
                }
            }
        }

        // Section 3: System & Maintenance
        SettingsCategorySection(
            categoryIcon = Icons.Rounded.Settings,
            categoryTitle = "System & Maintenance",
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column {
                    SettingsHubRow(
                        icon = Icons.Rounded.Backup,
                        title = "Backup & Restore",
                        subtitle = "Export and import configuration files",
                        onClick = { onSelectDestination(DashboardDestination.BACKUP_RESTORE) },
                    )
                    SettingsHubDivider()
                    SettingsHubRow(
                        icon = Icons.Rounded.BugReport,
                        title = "System Diagnostics",
                        subtitle = "Hardware probes, TTS engine, and error logs",
                        onClick = { onSelectDestination(DashboardDestination.DIAGNOSTICS) },
                    )
                }
            }
        }

        // App Version & Branding Footer
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981)),
                    )
                    Text(
                        text = "Hear It v$appVersionName ($appVersionCode)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Text(
                text = "Hands-free notification announcer · 100% On-Device",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 11.sp,
            )
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun SettingsCategorySection(
    categoryIcon: ImageVector,
    categoryTitle: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 4.dp),
        ) {
            Icon(
                imageVector = categoryIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = categoryTitle,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        content()
    }
}

@Composable
private fun SettingsHubRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun SettingsHubDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    )
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
