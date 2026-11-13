package com.jayanth.hearit.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jayanth.hearit.intelligence.AutomaticBehaviorAndCatchUpCard
import com.jayanth.hearit.intelligence.IntelligenceActions

/**
 * Automation: everything the app does on its own - earphone connect/disconnect, catch-up
 * summaries and the periodic spoken check-in. Moved out of Playback & Gestures, which is now
 * only about how a single announcement is delivered.
 */
@Composable
fun AutomationRoute(
    viewModel: GeneralSettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val actions = remember(viewModel) { IntelligenceActions.from(viewModel) }

    AutomationScreen(
        state = state,
        actions = actions,
        onToggleAutoStart = viewModel::onToggleBluetoothAutoStart,
        onToggleCheckin = viewModel::onTogglePeriodicCheckin,
        onCheckinIntervalChanged = viewModel::onPeriodicCheckinIntervalChanged,
        onToggleCheckinTime = viewModel::onTogglePeriodicCheckinTime,
        onToggleCheckinBattery = viewModel::onTogglePeriodicCheckinBattery,
        modifier = modifier,
    )
}

@Composable
fun AutomationScreen(
    state: GeneralSettingsUiState,
    actions: IntelligenceActions,
    onToggleAutoStart: (Boolean) -> Unit,
    onToggleCheckin: (Boolean) -> Unit,
    onCheckinIntervalChanged: (Int) -> Unit,
    onToggleCheckinTime: (Boolean) -> Unit,
    onToggleCheckinBattery: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ConnectivityCard(
            autoStart = state.isBluetoothAutoStartEnabled,
            autoStop = state.isBluetoothAutoStopEnabled,
            onToggleAutoStart = onToggleAutoStart,
            onToggleAutoStop = actions.onToggleBluetoothAutoStop,
        )

        AutomaticBehaviorAndCatchUpCard(state = state, actions = actions)

        PeriodicCheckinCard(
            isEnabled = state.isPeriodicCheckinEnabled,
            intervalMinutes = state.periodicCheckinIntervalMinutes,
            announceTime = state.periodicCheckinAnnounceTime,
            announceBattery = state.periodicCheckinAnnounceBattery,
            announceEarphonesBattery = state.periodicCheckinAnnounceEarphonesBattery,
            announceSummary = state.periodicCheckinAnnounceSummary,
            onToggleEnabled = onToggleCheckin,
            onIntervalChanged = onCheckinIntervalChanged,
            onToggleTime = onToggleCheckinTime,
            onToggleBattery = onToggleCheckinBattery,
            onToggleEarphonesBattery = actions.onTogglePeriodicCheckinEarphonesBattery,
            onToggleSummary = actions.onTogglePeriodicCheckinSummary,
        )

        Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text(
                text = "Which earphones may play announcements is set in Devices & Audio.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun ConnectivityCard(
    autoStart: Boolean,
    autoStop: Boolean,
    onToggleAutoStart: (Boolean) -> Unit,
    onToggleAutoStop: (Boolean) -> Unit,
) {
    SettingsCard(
        title = "Earphones",
        subtitle = "Start and stop listening with your headset",
        icon = Icons.Rounded.Bluetooth,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AutomationSwitchRow(
                title = "Start on Connect",
                subtitle = "Starts HearIt when Bluetooth audio connects",
                checked = autoStart,
                onCheckedChange = onToggleAutoStart,
            )
            AutomationDivider()
            AutomationSwitchRow(
                title = "Stop on Disconnect",
                subtitle = "Stops the service cleanly when earphones disconnect",
                checked = autoStop,
                onCheckedChange = onToggleAutoStop,
            )
        }
    }
}

@Composable
private fun AutomationSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun AutomationDivider() {
    androidx.compose.material3.HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}
