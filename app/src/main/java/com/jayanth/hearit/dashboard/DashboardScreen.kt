package com.jayanth.hearit.dashboard

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.jayanth.hearit.R
import com.jayanth.hearit.applications.ApplicationsRoute
import com.jayanth.hearit.applications.ApplicationsViewModel
import com.jayanth.hearit.core.common.model.AudioOutputSettings
import com.jayanth.hearit.core.common.model.HistoryStatus
import com.jayanth.hearit.setup.TestAnnouncementState

/**
 * Entry point of the dashboard shell (master prompt §13, phase P12).
 *
 * Owns the only piece of transient UI state in the phase — the selected destination — and
 * keeps it in `rememberSaveable`, so both a configuration change and a system-initiated
 * process recreation restore the same screen (P12 acceptance). It also refreshes the
 * values that have no reactive source when the screen resumes (connected outputs, session
 * mute flag, recent history).
 *
 * Back from a secondary screen returns to Home instead of leaving the app, which matches
 * the tab model; the bottom tabs themselves are top-level destinations.
 */
@Composable
fun DashboardRoute(
    viewModel: DashboardViewModel,
    applicationsViewModel: ApplicationsViewModel,
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
        appsContent = { ApplicationsRoute(viewModel = applicationsViewModel) },
        modifier = modifier,
    )
}

/**
 * The shell: header, content and bottom navigation.
 *
 * Stateless — it renders [state] and emits events, so no navigation or service policy
 * lives here (global standards). Every visible string is a resource, every interactive
 * target is at least 48 dp, all status is written out as text (never colour alone), and
 * the bottom bar exposes each tab as a single `Role.Tab` selectable with a selected state,
 * which is what TalkBack announces.
 * @param appsContent Content of the Apps tab, supplied by the caller so the shell stays
 *   unaware of the applications feature (P13).
 */
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    destination: DashboardDestination,
    onSelectDestination: (DashboardDestination) -> Unit,
    onBackToHome: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onTestAnnouncement: (String) -> Unit,
    onHistorySearch: (DashboardFilterState) -> Unit,
    onHistoryClearAll: () -> Unit,
    appsContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            DashboardBottomBar(selected = destination, onSelect = onSelectDestination)
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            DashboardHeader(
                title = stringResource(destination.labelRes),
                showBack = !destination.isBottomBarDestination,
                onBack = onBackToHome,
            )
            HorizontalDivider()

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    !state.loaded -> Text(
                        text = stringResource(R.string.dashboard_loading),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(24.dp),
                    )

                    destination == DashboardDestination.HOME -> DashboardHome(
                        state = state,
                        onStartService = onStartService,
                        onStopService = onStopService,
                        onTestAnnouncement = onTestAnnouncement,
                        onSelectDestination = onSelectDestination,
                    )

                    destination == DashboardDestination.APPS -> appsContent()

                    else -> DestinationPlaceholder(destination = destination)
                }
            }
        }
    }
}

@Composable
private fun DashboardHeader(title: String, showBack: Boolean, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBack) {
            TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(text = stringResource(R.string.dashboard_back))
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f).padding(start = if (showBack) 4.dp else 12.dp),
        )
    }
}

/**
 * Bottom navigation (master prompt §13).
 *
 * Plain `Surface` + selectable columns rather than `NavigationBar`: the Material
 * `NavigationBarItem` needs an icon per destination, and the icon set with Devices/History
 * glyphs is a separate artifact that cannot be compile-verified in the cloud (ADR-024).
 * Text labels are the accessible names either way.
 */
@Composable
private fun DashboardBottomBar(
    selected: DashboardDestination,
    onSelect: (DashboardDestination) -> Unit,
) {
    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for (destination in DashboardDestination.bottomBar) {
                val isSelected = destination == selected
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .selectable(
                            selected = isSelected,
                            role = Role.Tab,
                            onClick = { onSelect(destination) },
                        )
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(destination.labelRes),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardHome(
    state: DashboardUiState,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onTestAnnouncement: (String) -> Unit,
    onSelectDestination: (DashboardDestination) -> Unit,
) {
    val testAnnouncementText = stringResource(R.string.dashboard_test_announcement_spoken)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        ServiceCard(
            state = state,
            onStartService = onStartService,
            onStopService = onStopService,
        )

        Spacer(Modifier.height(20.dp))
        SectionTitle(R.string.dashboard_section_status)
        StatusRow(
            label = stringResource(R.string.dashboard_normal_announcements),
            value = onOff(state.announcementsActive),
        )
        PriorityAnnounceRow(
            isActive = state.priorityAnnounce.isActive,
            onToggled = { onPriorityAnnounceToggled(it) },
        ){
            supporting = stringResource(R.string.dashboard_priority_announce_note),
        }
        TemporaryMuteRow(
            isMuted = state.temporaryMuteActive,
            onMuteClicked = { onTemporaryMuteClicked(it) },
            onUnmuteClicked = { onTemporaryMuteCleared(it) },
        ){
            supporting = stringResource(R.string.dashboard_temporary_mute_note),
        }
        StatusRow(
            label = stringResource(R.string.dashboard_output),
            value = state.outputLabel ?: stringResource(R.string.dashboard_output_none),
        )
        StatusRow(
            label = stringResource(R.string.dashboard_allowed_outputs),
            value = allowedOutputSummary(state.audioOutput),
        )
        StatusRow(
            label = stringResource(R.string.dashboard_apps_enabled),
            value = stringResource(R.string.dashboard_apps_count, state.enabledAppCount),
        )

        Spacer(Modifier.height(20.dp))
        SectionTitle(R.string.dashboard_section_recent)
        RecentHistoryContent(state = state, onSelectDestination = onSelectDestination)

        Spacer(Modifier.height(20.dp))
        SectionTitle(R.string.dashboard_section_actions)
        ActionButton(
            label = stringResource(R.string.dashboard_action_test),
            onClick = { onTestAnnouncement(testAnnouncementText) },
            enabled = state.testAnnouncement != TestAnnouncementState.SPEAKING,
        )
        TestAnnouncementStatus(state = state.testAnnouncement)
        ActionButton(
            label = stringResource(R.string.dashboard_action_apps),
            onClick = { onSelectDestination(DashboardDestination.APPS) },
        )
        ActionButton(
            label = stringResource(R.string.dashboard_action_devices),
            onClick = { onSelectDestination(DashboardDestination.DEVICES) },
        )
        ActionButton(
            label = stringResource(R.string.dashboard_action_diagnostics),
            onClick = { onSelectDestination(DashboardDestination.DIAGNOSTICS) },
        )

        Spacer(Modifier.height(20.dp))
        SectionTitle(R.string.dashboard_section_tools)
        for (destination in DashboardDestination.secondary) {
            NavigationRow(
                label = stringResource(destination.labelRes),
                onClick = { onSelectDestination(destination) },
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ServiceCard(
    state: DashboardUiState,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.dashboard_service_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(
                    if (state.service.isRunning) R.string.dashboard_status_active else R.string.dashboard_status_inactive,
                ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (state.service.isRunning) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (state.service.isSpeaking) {
                    stringResource(R.string.dashboard_speaking_now)
                } else {
                    stringResource(R.string.dashboard_idle)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.dashboard_queue_waiting,
                    state.service.pendingCount,
                    state.service.pendingCount,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))

            if (state.service.isRunning) {
                OutlinedButton(
                    onClick = onStopService,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(text = stringResource(R.string.dashboard_stop_service))
                }
            } else {
                Button(
                    onClick = onStartService,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(text = stringResource(R.string.dashboard_start_service))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(@StringRes titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

/** One read-only status line; [supporting] explains a deliberately missing control. */
@Composable
private fun StatusRow(label: String, value: String, supporting: String? = null) {
    Column(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).padding(end = 12.dp),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
            )
        }
        if (supporting != null) {
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Temporary mute control row (P16).
 * Shows the current mute state and provides 15min/30min/60min/toggle controls.
 */
@Composable
private fun TemporaryMuteRow(
    isMuted: Boolean,
    onMuteClicked: (Int) -> Unit, 
    onUnmuteClicked: (() -> Unit),
    supporting: String? = null,
) {
    Column(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.dashboard_temporary_mute),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).padding(end = 12.dp),
            )
            Text(
                text = if (isMuted) {
                    stringResource(R.string.dashboard_status_active)
                } else {
                    stringResource(R.string.dashboard_status_inactive)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
            )
        }
        if (!isMuted) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { onMuteClicked(15) },
                    modifier = Modifier.width(60.dp).heightIn(min = 36.dp),
                ) {
                    Text(text = stringResource(R.string.mute_15min))
                }
                OutlinedButton(
                    onClick = { onMuteClicked(30) },
                    modifier = Modifier.width(60.dp).heightIn(min = 36.dp), start = 4.dp
                ) {
                    Text(text = stringResource(R.string.mute_30min))
                }
                OutlinedButton(
                    onClick = { onMuteClicked(60) },
                    modifier = Modifier.width(60.dp).heightIn(min = 36.dp), start = 4.dp
                ) {
                    Text(text = stringResource(R.string.mute_60min))
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { onUnmuteClicked() },
                    modifier = Modifier.width(80.dp).heightIn(min = 36.dp),
                ) {
                    Text(text = stringResource(R.string.unmute))
                }
            }
        }
        if (supporting != null) {
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecentHistoryContent(
    state: DashboardUiState,
    onSelectDestination: (DashboardDestination) -> Unit,
) {
    val filterState = remember { DashboardFilterState() }
    var filterState by remember { mutableStateOf(filterState) }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.dashboard_recent_history),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        OutlinedTextField(
            value = filterState.query,
            onValueChange = { filterState = filterState.copy(query = it) },
            modifier = Modifier.weight(1f),
            label = { Text(text = stringResource(R.string.dashboard_search_history)) },
            placeholder = { Text(text = stringResource(R.string.dashboard_search_placeholder)) },
            trailingIcon = { if (filterState.query.isNotEmpty()) {
                IconButton(
                    onClick = { filterState = filterState.copy(query = "") },
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                }
            } else {
                null
            },
        )

        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = { onHistorySearch(filterState) },
                modifier = Modifier.width(80.dp).padding(end = 4.dp),
            ) {
                Text(text = "Search")
            }
            OutlinedButton(
                onClick = { onHistoryClearAll() },
                modifier = Modifier.width(80.dp).padding(start = 4.dp)
            ) {
                Text(text = "Clear all")
            }
        }

        when {
            state.historyUnavailable -> Text(
                text = stringResource(R.string.dashboard_recent_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )

            !state.recentHistoryLoaded -> Text(
                text = stringResource(R.string.dashboard_loading),
                style = MaterialTheme.typography.bodyMedium,
            )

            state.recentHistory.isEmpty() -> Text(
                text = stringResource(R.string.dashboard_recent_empty),
                style = MaterialTheme.typography.bodyMedium,
            )

            else -> {
                for (entry in state.recentHistory) {
                    RecentRow(entry = entry)
                }
                TextButton(
                    onClick = { onSelectDestination(DashboardDestination.HISTORY) },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(text = stringResource(R.string.dashboard_view_all_history))
                }
            }
        }
    }
}

@Composable
private fun RecentRow(entry: RecentAnnouncement) {
    val relativeTime = DateUtils.getRelativeTimeSpanString(
        entry.recordedAtMillis,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
    )
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(
                R.string.dashboard_recent_row,
                entry.appName,
                statusLabel(entry.status),
                relativeTime,
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ActionButton(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).heightIn(min = 48.dp),
    ) {
        Text(text = label)
    }
}

@Composable
private fun NavigationRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun TestAnnouncementStatus(state: TestAnnouncementState) {
    val statusRes = when (state) {
        TestAnnouncementState.IDLE -> null
        TestAnnouncementState.SPEAKING -> R.string.setup_test_speaking
        TestAnnouncementState.COMPLETED -> R.string.setup_test_completed
        TestAnnouncementState.FAILED -> R.string.setup_test_failed
    }
    if (statusRes != null) {
        Text(
            text = stringResource(statusRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp),
        )
    }
}

/**
 * Placeholder for a destination whose feature phase has not landed yet (P13–P19).
 *
 * The shell ships the entry so navigation, back handling and accessibility can be
 * verified now; the copy says the screen is not implemented instead of showing dead
 * controls.
 */
@Composable
private fun DestinationPlaceholder(destination: DashboardDestination) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(text = stringResource(destination.labelRes), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.dashboard_placeholder_body),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun PriorityAnnounceRow(
    isActive: Boolean,
    onToggled: (Boolean) -> Unit,
) {
    val remainingMillis = 0L // TODO: read from service status in P17
    val remainingStr = when {
        remainingMillis <= 0 -> stringResource(R.string.priority_announce_no_expiry)
        else -> {
            val minutes = (remainingMillis / 60000).coerceAtLeast(1)
            stringResource(R.string.priority_announce_remaining, minutes)
        }
    }
    Column(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.dashboard_priority_announce),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).padding(end = 12.dp),
            )
            Text(
                text = if (isActive) {
                    stringResource(R.string.priority_announce_on)
                } else {
                    stringResource(R.string.priority_announce_off)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = { onToggled(true) },
                modifier = Modifier.width(48.dp).heightIn(min = 32.dp),
            ) {
                Text(text = stringResource(R.string.priority_announce_on))
            }
            OutlinedButton(
                onClick = { onToggled(false) },
                modifier = Modifier.width(48.dp).heightIn(min = 32.dp), start = 4.dp
            ) {
                Text(text = stringResource(R.string.priority_announce_off))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, start = 4.dp) {
            Text(
                text = remainingStr,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun onOff(active: Boolean): String =
    stringResource(if (active) R.string.dashboard_status_on else R.string.dashboard_status_off)

@Composable
private fun allowedOutputSummary(audio: AudioOutputSettings): String {
    val allowed = mutableListOf<String>()
    if (audio.allowBluetooth) allowed += stringResource(R.string.setup_devices_allow_bluetooth)
    if (audio.allowWired) allowed += stringResource(R.string.setup_devices_allow_wired)
    if (audio.allowPhoneSpeaker) allowed += stringResource(R.string.setup_devices_allow_speaker)
    return if (allowed.isEmpty()) {
        stringResource(R.string.dashboard_allowed_outputs_none)
    } else {
        allowed.joinToString(", ")
    }
}

@Composable
private fun statusLabel(status: HistoryStatus): String = stringResource(
    when (status) {
        HistoryStatus.ANNOUNCED -> R.string.dashboard_history_announced
        HistoryStatus.SKIPPED -> R.string.dashboard_history_skipped
        HistoryStatus.QUEUED -> R.string.dashboard_history_queued
        HistoryStatus.PENDING -> R.string.dashboard_history_pending
        HistoryStatus.INTERRUPTED -> R.string.dashboard_history_interrupted
    },
)
