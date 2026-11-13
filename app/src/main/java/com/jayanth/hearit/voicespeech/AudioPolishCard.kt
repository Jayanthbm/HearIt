package com.jayanth.hearit.voicespeech

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
    val onAdaptiveSpeedTier1Changed: (Float) -> Unit = {},
    val onAdaptiveSpeedTier2Changed: (Float) -> Unit = {},
    val onAdaptiveSpeedTier3Changed: (Float) -> Unit = {},
    val onToggleMonoFoldDown: (Boolean) -> Unit = {},
    val onToggleVoiceBoost: (Boolean) -> Unit = {},
    val onVoiceBoostPercentChanged: (Int) -> Unit = {},
) {
    companion object {
        fun from(viewModel: VoiceSpeechViewModel): AudioPolishCallbacks = AudioPolishCallbacks(
            onToggleEarcon = viewModel::onToggleEarcon,
            onEarconDurationChanged = viewModel::onEarconDurationChanged,
            onAntiCollisionDelayChanged = viewModel::onAntiCollisionDelayChanged,
            onToggleAdaptiveSpeed = viewModel::onToggleAdaptiveSpeed,
            onAdaptiveSpeedTier1Changed = viewModel::onAdaptiveSpeedTier1Changed,
            onAdaptiveSpeedTier2Changed = viewModel::onAdaptiveSpeedTier2Changed,
            onAdaptiveSpeedTier3Changed = viewModel::onAdaptiveSpeedTier3Changed,
            onToggleMonoFoldDown = viewModel::onToggleMonoFoldDown,
            onToggleVoiceBoost = viewModel::onToggleVoiceBoost,
            onVoiceBoostPercentChanged = viewModel::onVoiceBoostPercentChanged,
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
                        text = "Smart Audio",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Chime, speed and loudness tuning",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 1. Soft Pre-Speech Earcon Chime (#18)
                PolishSwitchRow(
                    title = "Soft Pre Chime",
                    subtitle = "Plays a gentle tone before speaking so music ducks smoothly",
                    checked = state.isEarconEnabled,
                    onCheckedChange = callbacks.onToggleEarcon,
                )
                if (state.isEarconEnabled) {
                    val chimeOptions = listOf(120, 200, 300)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        chimeOptions.forEachIndexed { index, ms ->
                            val isSelected = state.earconDurationMs == ms
                            SegmentedButton(
                                selected = isSelected,
                                onClick = { callbacks.onEarconDurationChanged(ms) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = chimeOptions.size),
                            ) {
                                Text(
                                    text = "$ms ms",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
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
                    title = "Adaptive Speed",
                    subtitle = "Automatically scales reading speed across word-count brackets",
                    checked = state.isAdaptiveSpeedEnabled,
                    onCheckedChange = callbacks.onToggleAdaptiveSpeed,
                )
                if (state.isAdaptiveSpeedEnabled) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Under 20 words",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "Base speed (${AdaptiveSpeedCalculator.formatMultiplier(state.speech.speechRate)})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            AdaptiveSpeedTierControl(
                                bracketLabel = "20 – 30 words",
                                baseRate = state.speech.speechRate,
                                wordSample = 25,
                                selectedMultiplier = state.adaptiveSpeedTier1Multiplier,
                                options = listOf(1.0f to "1.0×", 1.1f to "1.1×", 1.15f to "1.15×", 1.25f to "1.25×"),
                                onSelect = callbacks.onAdaptiveSpeedTier1Changed,
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            AdaptiveSpeedTierControl(
                                bracketLabel = "30 – 40 words",
                                baseRate = state.speech.speechRate,
                                wordSample = 35,
                                selectedMultiplier = state.adaptiveSpeedTier2Multiplier,
                                options = listOf(1.15f to "1.15×", 1.25f to "1.25×", 1.35f to "1.35×", 1.5f to "1.5×"),
                                onSelect = callbacks.onAdaptiveSpeedTier2Changed,
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            AdaptiveSpeedTierControl(
                                bracketLabel = "Over 40 words",
                                baseRate = state.speech.speechRate,
                                wordSample = 50,
                                selectedMultiplier = state.adaptiveSpeedTier3Multiplier,
                                options = listOf(1.25f to "1.25×", 1.4f to "1.4×", 1.5f to "1.5×", 1.75f to "1.75×"),
                                onSelect = callbacks.onAdaptiveSpeedTier3Changed,
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

@Composable
private fun AdaptiveSpeedTierControl(
    bracketLabel: String,
    baseRate: Float,
    wordSample: Int,
    selectedMultiplier: Float,
    options: List<Pair<Float, String>>,
    onSelect: (Float) -> Unit,
) {
    val effectiveRate = AdaptiveSpeedCalculator.resolveRate(
        baseRate = baseRate,
        wordCount = wordSample,
        enabled = true,
        tier1Multiplier = selectedMultiplier,
        tier2Multiplier = selectedMultiplier,
        tier3Multiplier = selectedMultiplier,
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = bracketLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Speaks at ${AdaptiveSpeedCalculator.formatMultiplier(effectiveRate)}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (multiplier, label) ->
                val isSelected = kotlin.math.abs(selectedMultiplier - multiplier) < 0.02f
                SegmentedButton(
                    selected = isSelected,
                    onClick = { onSelect(multiplier) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}
