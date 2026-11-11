package com.jayanth.hearit.voicespeech

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AudioPolishCallbacks(
    val onToggleEarcon: (Boolean) -> Unit = {},
    val onEarconDurationChanged: (Int) -> Unit = {},
    val onAntiCollisionDelayChanged: (Long) -> Unit = {},
    val onToggleAdaptiveSpeed: (Boolean) -> Unit = {},
    val onAdaptiveSpeedWordThresholdChanged: (Int) -> Unit = {},
    val onAdaptiveSpeedMultiplierChanged: (Float) -> Unit = {},
    val onToggleMonoFoldDown: (Boolean) -> Unit = {},
    val onToggleVoiceBoost: (Boolean) -> Unit = {},
    val onVoiceBoostPercentChanged: (Int) -> Unit = {},
    val onToggleAmbientNoiseBoost: (Boolean) -> Unit = {},
    val onAmbientNoiseThresholdChanged: (Int) -> Unit = {},
    val onAmbientNoiseBoostPercentChanged: (Int) -> Unit = {},
    val onRefreshMicPermission: () -> Unit = {},
) {
    companion object {
        fun from(viewModel: VoiceSpeechViewModel): AudioPolishCallbacks = AudioPolishCallbacks(
            onToggleEarcon = viewModel::onToggleEarcon,
            onEarconDurationChanged = viewModel::onEarconDurationChanged,
            onAntiCollisionDelayChanged = viewModel::onAntiCollisionDelayChanged,
            onToggleAdaptiveSpeed = viewModel::onToggleAdaptiveSpeed,
            onAdaptiveSpeedWordThresholdChanged = viewModel::onAdaptiveSpeedWordThresholdChanged,
            onAdaptiveSpeedMultiplierChanged = viewModel::onAdaptiveSpeedMultiplierChanged,
            onToggleMonoFoldDown = viewModel::onToggleMonoFoldDown,
            onToggleVoiceBoost = viewModel::onToggleVoiceBoost,
            onVoiceBoostPercentChanged = viewModel::onVoiceBoostPercentChanged,
            onToggleAmbientNoiseBoost = viewModel::onToggleAmbientNoiseBoost,
            onAmbientNoiseThresholdChanged = viewModel::onAmbientNoiseThresholdChanged,
            onAmbientNoiseBoostPercentChanged = viewModel::onAmbientNoiseBoostPercentChanged,
            onRefreshMicPermission = viewModel::refreshRecordAudioPermission,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PersistedSmartAudioPolishCard(
    state: VoiceSpeechUiState,
    callbacks: AudioPolishCallbacks,
    modifier: Modifier = Modifier,
) {
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        callbacks.onRefreshMicPermission()
        if (granted) {
            callbacks.onToggleAmbientNoiseBoost(true)
        }
    }

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
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Smart Audio Polish & Boost",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Earcon chimes, adaptive speed, voice boost, and ambient noise gain",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 1. Soft Pre-Speech Earcon Chime (#18)
                PolishSwitchRow(
                    title = "Soft Pre-Speech Earcon Chime",
                    subtitle = "Plays a gentle tone before speaking so music ducks smoothly",
                    checked = state.isEarconEnabled,
                    onCheckedChange = callbacks.onToggleEarcon,
                )
                if (state.isEarconEnabled) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(120, 200, 300).forEach { ms ->
                            FilterChip(
                                selected = state.earconDurationMs == ms,
                                onClick = { callbacks.onEarconDurationChanged(ms) },
                                label = { Text("${ms}ms chime", maxLines = 1, softWrap = false) },
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // 2. Post-chime Anti-Collision Delay (#18)
                Column {
                    Text(
                        text = "Anti-Collision Audio Delay",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Pause before speech so system notification chimes don't clip words",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    val delayOptions = listOf(0L to "0 ms", 300L to "300 ms", 500L to "500 ms")
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        delayOptions.forEachIndexed { index, (ms, label) ->
                            val isSelected = state.antiCollisionDelayMs == ms
                            SegmentedButton(
                                selected = isSelected,
                                onClick = { callbacks.onAntiCollisionDelayChanged(ms) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = delayOptions.size),
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // 3. Adaptive Speech Speed (#18)
                PolishSwitchRow(
                    title = "Adaptive Speed for Long Messages",
                    subtitle = "Automatically speeds up messages longer than threshold words",
                    checked = state.isAdaptiveSpeedEnabled,
                    onCheckedChange = callbacks.onToggleAdaptiveSpeed,
                )
                if (state.isAdaptiveSpeedEnabled) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(20, 30, 50).forEach { words ->
                            FilterChip(
                                selected = state.adaptiveSpeedWordThreshold == words,
                                onClick = { callbacks.onAdaptiveSpeedWordThresholdChanged(words) },
                                label = { Text(">$words words") },
                            )
                        }
                        listOf(1.15f to "1.15×", 1.25f to "1.25×", 1.5f to "1.5×").forEach { (mult, label) ->
                            FilterChip(
                                selected = kotlin.math.abs(state.adaptiveSpeedMultiplier - mult) < 0.02f,
                                onClick = { callbacks.onAdaptiveSpeedMultiplierChanged(mult) },
                                label = { Text(label) },
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // 4. Mono Audio Fold-Down (#18)
                PolishSwitchRow(
                    title = "Mono Audio Fold-Down",
                    subtitle = "Centers speech channels cleanly for single-earbud users",
                    checked = state.isMonoFoldDownEnabled,
                    onCheckedChange = callbacks.onToggleMonoFoldDown,
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // 5. Speech Volume Boost (#24)
                PolishSwitchRow(
                    title = "Speech Loudness Boost",
                    subtitle = "Applies hardware loudness gain above standard TTS volume",
                    checked = state.isVoiceBoostEnabled,
                    onCheckedChange = callbacks.onToggleVoiceBoost,
                )
                if (state.isVoiceBoostEnabled) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15, 30, 50).forEach { pct ->
                            FilterChip(
                                selected = state.voiceBoostPercent == pct,
                                onClick = { callbacks.onVoiceBoostPercentChanged(pct) },
                                label = { Text("+$pct% gain", maxLines = 1, softWrap = false) },
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // 6. Ambient Noise Auto-Volume Boost (#34)
                PolishSwitchRow(
                    title = "Ambient Noise Auto-Volume Boost",
                    subtitle = "120ms instantaneous room loudness check before speaking (never recorded or stored)",
                    checked = state.isAmbientNoiseBoostEnabled,
                    onCheckedChange = { wantEnable ->
                        if (wantEnable && !state.hasRecordAudioPermission) {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            callbacks.onToggleAmbientNoiseBoost(wantEnable)
                        }
                    },
                )
                if (state.isAmbientNoiseBoostEnabled) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(65, 70, 75).forEach { db ->
                            FilterChip(
                                selected = state.ambientNoiseThresholdDb == db,
                                onClick = { callbacks.onAmbientNoiseThresholdChanged(db) },
                                label = { Text(">$db dB") },
                            )
                        }
                        listOf(15, 20, 30).forEach { boost ->
                            FilterChip(
                                selected = state.ambientNoiseBoostPercent == boost,
                                onClick = { callbacks.onAmbientNoiseBoostPercentChanged(boost) },
                                label = { Text("+$boost% boost") },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PolishSwitchRow(
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
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
