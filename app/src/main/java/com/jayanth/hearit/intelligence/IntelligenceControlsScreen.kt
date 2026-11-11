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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayanth.hearit.devices.WearOsCompanionCard
import com.jayanth.hearit.settings.GeneralSettingsUiState
import com.jayanth.hearit.settings.GeneralSettingsViewModel
import com.jayanth.hearit.settings.GesturesAndControlsCard
import com.jayanth.hearit.settings.PeriodicCheckinCard
import com.jayanth.hearit.settings.SmartContentCleaningCard

/**
 * Route wrapping [IntelligenceControlsScreen] with [GeneralSettingsViewModel].
 */
@Composable
fun IntelligenceControlsRoute(
    viewModel: GeneralSettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()

    IntelligenceControlsScreen(
        state = state,
        onToggleAutoDismiss = viewModel::onToggleAutoDismiss,
        onToggleShakeToStop = viewModel::onToggleShakeToStop,
        onToggleVolumeKeyToStop = viewModel::onToggleVolumeKeyToStop,
        onToggleMusicPauseHold = viewModel::onToggleMusicPauseHold,
        onToggleBluetoothAutoStart = viewModel::onToggleBluetoothAutoStart,
        onTogglePeriodicCheckin = viewModel::onTogglePeriodicCheckin,
        onPeriodicCheckinIntervalChanged = viewModel::onPeriodicCheckinIntervalChanged,
        onTogglePeriodicCheckinTime = viewModel::onTogglePeriodicCheckinTime,
        onTogglePeriodicCheckinBattery = viewModel::onTogglePeriodicCheckinBattery,
        onToggleUrlSummarizer = viewModel::onToggleUrlSummarizer,
        onToggleCurrencyNormalizer = viewModel::onToggleCurrencyNormalizer,
        onToggleMarkdownStripper = viewModel::onToggleMarkdownStripper,
        onToggleEmojiCollapsing = viewModel::onToggleEmojiCollapsing,
        onToggleReactionFilter = viewModel::onToggleReactionFilter,
        onToggleBotSenderCleaning = viewModel::onToggleBotSenderCleaning,
        modifier = modifier,
    )
}

/**
 * Dedicated Full Screen for Active Intelligence & Controls.
 *
 * Houses real-time NLP text cleaning, hands-free hardware gestures, smartwatch companion,
 * and periodic check-ins. Designed for seamless future integration with premium gating.
 */
@Composable
fun IntelligenceControlsScreen(
    state: GeneralSettingsUiState,
    onToggleAutoDismiss: (Boolean) -> Unit,
    onToggleShakeToStop: (Boolean) -> Unit,
    onToggleVolumeKeyToStop: (Boolean) -> Unit,
    onToggleMusicPauseHold: (Boolean) -> Unit,
    onToggleBluetoothAutoStart: (Boolean) -> Unit,
    onTogglePeriodicCheckin: (Boolean) -> Unit,
    onPeriodicCheckinIntervalChanged: (Int) -> Unit,
    onTogglePeriodicCheckinTime: (Boolean) -> Unit,
    onTogglePeriodicCheckinBattery: (Boolean) -> Unit,
    onToggleUrlSummarizer: (Boolean) -> Unit,
    onToggleCurrencyNormalizer: (Boolean) -> Unit,
    onToggleMarkdownStripper: (Boolean) -> Unit,
    onToggleEmojiCollapsing: (Boolean) -> Unit,
    onToggleReactionFilter: (Boolean) -> Unit,
    onToggleBotSenderCleaning: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Pro Intelligence Header / Future Premium Banner ─────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(38.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Intelligence & Controls Suite",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Real-time speech & hardware enhancements",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // Active Status / Future Premium Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                text = "ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp,
                            )
                        }
                    }
                }

                Text(
                    text = "Zero-latency on-device processing. Clean links, normalize currency, control speech with physical motion, and synchronize seamlessly with Wear OS.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                )
            }
        }

        // ── Section 1: Smart Content Cleaning (NLP) ───────────────────────
        SmartContentCleaningCard(
            isUrlSummarizerEnabled = state.isUrlSummarizerEnabled,
            isCurrencyNormalizerEnabled = state.isCurrencyNormalizerEnabled,
            isMarkdownStripperEnabled = state.isMarkdownStripperEnabled,
            isEmojiCollapsingEnabled = state.isEmojiCollapsingEnabled,
            isReactionFilterEnabled = state.isReactionFilterEnabled,
            isBotSenderCleaningEnabled = state.isBotSenderCleaningEnabled,
            onToggleUrlSummarizer = onToggleUrlSummarizer,
            onToggleCurrencyNormalizer = onToggleCurrencyNormalizer,
            onToggleMarkdownStripper = onToggleMarkdownStripper,
            onToggleEmojiCollapsing = onToggleEmojiCollapsing,
            onToggleReactionFilter = onToggleReactionFilter,
            onToggleBotSenderCleaning = onToggleBotSenderCleaning,
        )

        // ── Section 2: Gestures & Playback Controls ────────────────────────
        GesturesAndControlsCard(
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

        // ── Section 3: Periodic Spoken Check-in ─────────────────────────────
        PeriodicCheckinCard(
            isEnabled = state.isPeriodicCheckinEnabled,
            intervalMinutes = state.periodicCheckinIntervalMinutes,
            announceTime = state.periodicCheckinAnnounceTime,
            announceBattery = state.periodicCheckinAnnounceBattery,
            onToggleEnabled = onTogglePeriodicCheckin,
            onIntervalChanged = onPeriodicCheckinIntervalChanged,
            onToggleTime = onTogglePeriodicCheckinTime,
            onToggleBattery = onTogglePeriodicCheckinBattery,
        )

        // ── Section 4: Wear OS Companion Smartwatch ────────────────────────
        WearOsCompanionCard()

        // ── Informative Footer Tip ─────────────────────────────────────────
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "All intelligence features execute locally on your device for absolute privacy and maximum battery efficiency.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
