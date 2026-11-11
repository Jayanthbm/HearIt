package com.jayanth.hearit.applications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Floating batch selection action bar shown at the bottom when multi-select mode is active (#7).
 */
@Composable
fun ApplicationsBatchActionBar(
    selectedCount: Int,
    allVisibleSelected: Boolean,
    onToggleSelectAll: () -> Unit,
    onEnableSelected: () -> Unit,
    onDisableSelected: () -> Unit,
    onAddToPriority: () -> Unit,
    onResetSelected: () -> Unit,
    onExitSelectionMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 92.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "$selectedCount selected",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = onToggleSelectAll) {
                        Text(if (allVisibleSelected) "Deselect Visible" else "Select Visible")
                    }
                    TextButton(onClick = onExitSelectionMode) {
                        Text("Done")
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onEnableSelected,
                    enabled = selectedCount > 0,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Enable", maxLines = 1, softWrap = false)
                }
                OutlinedButton(
                    onClick = onDisableSelected,
                    enabled = selectedCount > 0,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Rounded.NotificationsOff, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Silence", maxLines = 1, softWrap = false)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onAddToPriority,
                    enabled = selectedCount > 0,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Rounded.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Priority +", maxLines = 1, softWrap = false)
                }
                OutlinedButton(
                    onClick = onResetSelected,
                    enabled = selectedCount > 0,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reset", maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

/**
 * Per-app Priority Announce membership card (#5), Speech Overrides card (#6), and Auto-Dismiss override card (#31).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PerAppPriorityVoiceAndDismissSection(
    state: AppSettingsUiState,
    onTogglePriorityAnnounce: () -> Unit,
    onSpeechRateChanged: (Float?) -> Unit,
    onSpeechPitchChanged: (Float?) -> Unit,
    onSpeechVoiceChanged: (String?) -> Unit,
    onAutoDismissChanged: (Boolean?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showVoicePickerDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Priority Announce Quick-Access Membership (#5)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(
                1.dp,
                if (state.priorityAnnounceListed) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                },
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = if (state.priorityAnnounceListed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column {
                        Text(
                            text = "Priority Announce List",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (state.priorityAnnounceListed) {
                                "Included when Priority Announce Focus Mode is active"
                            } else {
                                "Muted during Priority Announce Focus Mode unless starred"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Switch(
                    checked = state.priorityAnnounceListed,
                    onCheckedChange = { onTogglePriorityAnnounce() },
                )
            }
        }

        // Per-App Voice / Pitch / Speed Overrides (#6)
        val hasSpeechOverride = !state.speechRate.isInherited || !state.speechPitch.isInherited || !state.speechVoiceId.isInherited
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RecordVoiceOver,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "Voice, Speed & Pitch",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    if (hasSpeechOverride) {
                        TextButton(onClick = {
                            onSpeechRateChanged(null)
                            onSpeechPitchChanged(null)
                            onSpeechVoiceChanged(null)
                        }) {
                            Text("Inherit Global", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Speed Override Slider + Quick Chips
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (state.speechRate.isInherited) {
                                "Speech Speed (Global: ${String.format(Locale.US, "%.1f×", state.speechRate.effective)})"
                            } else {
                                "Custom Speed: ${String.format(Locale.US, "%.1f×", state.speechRate.effective)}"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (!state.speechRate.isInherited) {
                            TextButton(onClick = { onSpeechRateChanged(null) }) {
                                Text("Inherit", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    Slider(
                        value = state.speechRate.effective,
                        onValueChange = { raw ->
                            val snapped = (raw * 20f).roundToInt() / 20f
                            onSpeechRateChanged(snapped.coerceIn(0.5f, 2.5f))
                        },
                        valueRange = 0.5f..2.5f,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0.8f, 1.0f, 1.25f, 1.5f).forEach { preset ->
                            FilterChip(
                                selected = !state.speechRate.isInherited &&
                                    (state.speechRate.effective * 20).roundToInt() == (preset * 20).roundToInt(),
                                onClick = { onSpeechRateChanged(preset) },
                                label = { Text("${String.format(Locale.US, "%.2g×", preset)}") },
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Pitch Override Slider + Quick Chips
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (state.speechPitch.isInherited) {
                                "Voice Pitch (Global: ${String.format(Locale.US, "%.1f×", state.speechPitch.effective)})"
                            } else {
                                "Custom Pitch: ${String.format(Locale.US, "%.1f×", state.speechPitch.effective)}"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (!state.speechPitch.isInherited) {
                            TextButton(onClick = { onSpeechPitchChanged(null) }) {
                                Text("Inherit", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    Slider(
                        value = state.speechPitch.effective,
                        onValueChange = { raw ->
                            val snapped = (raw * 20f).roundToInt() / 20f
                            onSpeechPitchChanged(snapped.coerceIn(0.5f, 1.5f))
                        },
                        valueRange = 0.5f..1.5f,
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Per-App Engine Voice Selector
                val currentVoice = state.availableVoices.firstOrNull { it.id == state.speechVoiceId.effective }
                val voiceSummary = when {
                    state.speechVoiceId.isInherited && currentVoice != null -> "Inherits Global (${currentVoice.displayName})"
                    state.speechVoiceId.isInherited -> "Inherits Global Voice"
                    currentVoice != null -> "Custom: ${currentVoice.displayName}"
                    else -> "Custom Voice: ${state.speechVoiceId.effective}"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showVoicePickerDialog = true }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "App Voice",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = voiceSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    OutlinedButton(onClick = { showVoicePickerDialog = true }, shape = RoundedCornerShape(10.dp)) {
                        Text("Choose", maxLines = 1)
                    }
                }
            }
        }

        // Per-App Auto-Dismiss Override (#31)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Auto-Dismiss After Speaking",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    if (!state.autoDismiss.isInherited) {
                        TextButton(onClick = { onAutoDismissChanged(null) }) {
                            Text("Inherit Global", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Text(
                    text = when (state.autoDismiss.override) {
                        null -> "Inheriting global setting (${if (state.autoDismiss.global) "Clear after speaking" else "Keep in shade"})"
                        true -> "Always clear notifications from this app after speaking"
                        false -> "Always keep notifications from this app in the shade"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        listOf(
                            null to "Inherit",
                            true to "Always",
                            false to "Never",
                        ).forEach { (choice, label) ->
                            val selected = state.autoDismiss.override == choice
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                                    )
                                    .clickable { onAutoDismissChanged(choice) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showVoicePickerDialog) {
        AlertDialog(
            onDismissRequest = { showVoicePickerDialog = false },
            title = { Text("Choose Voice for ${state.label}") },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    item {
                        OutlinedButton(
                            onClick = {
                                onSpeechVoiceChanged(null)
                                showVoicePickerDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Inherit Global Voice")
                        }
                    }
                    if (state.availableVoices.isEmpty()) {
                        item {
                            Text(
                                text = "TTS voices will appear here after the speech engine initializes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        items(state.availableVoices.take(40), key = { it.id }) { voice ->
                            val isSelected = !state.speechVoiceId.isInherited && state.speechVoiceId.effective == voice.id
                            Surface(
                                onClick = {
                                    onSpeechVoiceChanged(voice.id)
                                    showVoicePickerDialog = false
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Text(
                                        text = voice.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    )
                                    Text(
                                        text = voice.locale.displayName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVoicePickerDialog = false }) {
                    Text("Close")
                }
            },
        )
    }
}
