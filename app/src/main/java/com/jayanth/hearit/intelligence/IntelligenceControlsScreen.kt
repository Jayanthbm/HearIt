package com.jayanth.hearit.intelligence

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jayanth.hearit.devices.WearOsCompanionCard
import com.jayanth.hearit.settings.GeneralSettingsUiState
import com.jayanth.hearit.settings.GeneralSettingsViewModel
import com.jayanth.hearit.settings.GesturesAndControlsCard
import com.jayanth.hearit.settings.PeriodicCheckinCard

/**
 * Route wrapping [IntelligenceControlsScreen] with [GeneralSettingsViewModel].
 */
@Composable
fun IntelligenceControlsRoute(
    viewModel: GeneralSettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val intelligenceActions = remember(viewModel) { IntelligenceActions.from(viewModel) }

    IntelligenceControlsScreen(
        state = state,
        intelligenceActions = intelligenceActions,
        onToggleAutoDismiss = viewModel::onToggleAutoDismiss,
        onToggleShakeToStop = viewModel::onToggleShakeToStop,
        onToggleVolumeKeyToStop = viewModel::onToggleVolumeKeyToStop,
        onToggleMusicPauseHold = viewModel::onToggleMusicPauseHold,
        onToggleBluetoothAutoStart = viewModel::onToggleBluetoothAutoStart,
        onTogglePeriodicCheckin = viewModel::onTogglePeriodicCheckin,
        onPeriodicCheckinIntervalChanged = viewModel::onPeriodicCheckinIntervalChanged,
        onTogglePeriodicCheckinTime = viewModel::onTogglePeriodicCheckinTime,
        onTogglePeriodicCheckinBattery = viewModel::onTogglePeriodicCheckinBattery,
        modifier = modifier,
    )
}

/**
 * Modern Material 3 Expressive Active Intelligence Screen.
 */
@Composable
fun IntelligenceControlsScreen(
    state: GeneralSettingsUiState,
    intelligenceActions: IntelligenceActions,
    onToggleAutoDismiss: (Boolean) -> Unit,
    onToggleShakeToStop: (Boolean) -> Unit,
    onToggleVolumeKeyToStop: (Boolean) -> Unit,
    onToggleMusicPauseHold: (Boolean) -> Unit,
    onToggleBluetoothAutoStart: (Boolean) -> Unit,
    onTogglePeriodicCheckin: (Boolean) -> Unit,
    onPeriodicCheckinIntervalChanged: (Int) -> Unit,
    onTogglePeriodicCheckinTime: (Boolean) -> Unit,
    onTogglePeriodicCheckinBattery: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Explanatory Intro Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Active Intelligence",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Real-time speech & hardware enhancements",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // Section 1: Smart Content Cleaning (NLP)
        SmartContentCleaningCard(
            state = state,
            actions = intelligenceActions,
        )

        // Section 2: Category Intelligence & App Grouping
        CategoryIntelligenceCard(
            state = state,
            actions = intelligenceActions,
        )

        // Section 3: Gestures & Playback Controls
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

        // Section 4: Timing & Catch-Up Automation (#19, #22)
        AutomaticBehaviorAndCatchUpCard(
            state = state,
            actions = intelligenceActions,
        )

        // Section 5: Proximity Pocket Detection (#30)
        com.jayanth.hearit.applications.PocketDetectionCard(
            eligibility = state.eligibility,
            onEligibilityChanged = intelligenceActions.onUpdateEligibility,
        )

        // Section 6: Periodic Spoken Check-in (#32)
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

        // Section 5: Wear OS Companion Smartwatch
        WearOsCompanionCard()

        Spacer(modifier = Modifier.height(100.dp))
    }
}
