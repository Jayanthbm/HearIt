package com.jayanth.hearit.intelligence

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayanth.hearit.settings.GeneralSettingsUiState

/**
 * Hardware & Play Services AI capability status and enable controls (#15).
 */
@Composable
fun LocalAiEnginesCard(
    state: GeneralSettingsUiState,
    actions: IntelligenceActions,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(38.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Memory,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "On-Device AI Engines",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Tier 1 pattern NLP & Tier 2 system AI capabilities",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = actions.onRefreshLocalAiCapabilities) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Refresh AI engine check",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // Tier 1: Instant Pattern NLP
            EngineStatusRow(
                title = "Tier 1 · Instant Pattern NLP",
                subtitle = "0 MB overhead · Runs locally on every Android device",
                badgeText = "Active · On-Device",
                badgeContainer = Color(0xFF1B5E20).copy(alpha = 0.2f),
                badgeContent = Color(0xFF4CAF50),
                trailing = null,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Tier 2A: ML Kit Entity Extraction
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EngineStatusRow(
                    title = "Tier 2A · ML Kit Entity Extraction",
                    subtitle = "Detects tracking IDs, flight numbers, and IBAN codes cleanly before TTS",
                    badgeText = state.mlKitStatusLabel,
                    badgeContainer = if (state.mlKitModelReady) {
                        Color(0xFF1B5E20).copy(alpha = 0.2f)
                    } else if (state.mlKitSupported) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    badgeContent = if (state.mlKitModelReady) {
                        Color(0xFF4CAF50)
                    } else if (state.mlKitSupported) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    trailing = {
                        Switch(
                            checked = state.isMlKitEntityExtractionEnabled,
                            enabled = state.mlKitSupported,
                            onCheckedChange = actions.onToggleMlKitEntityExtraction,
                        )
                    },
                )
                if (state.mlKitSupported && !state.mlKitModelReady) {
                    FilledTonalButton(
                        onClick = actions.onDownloadMlKitModel,
                        enabled = !state.isMlKitDownloading,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.isMlKitDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Downloading Entity Model…")
                        } else {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Download Lightweight Entity Model")
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Tier 2B: Android AICore (System Gemini Nano)
            EngineStatusRow(
                title = "Tier 2B · Android AICore",
                subtitle = if (state.aiCoreSupported) {
                    "System Gemini Nano hardware acceleration available for long notification summaries"
                } else {
                    "Not available in OS on this phone; Tier 1 instant cleaning handles all messages seamlessly"
                },
                badgeText = state.aiCoreStatusLabel,
                badgeContainer = if (state.aiCoreSupported) {
                    Color(0xFF1B5E20).copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                badgeContent = if (state.aiCoreSupported) {
                    Color(0xFF4CAF50)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                trailing = {
                    Switch(
                        checked = state.isAiCoreSummarizationEnabled && state.aiCoreSupported,
                        enabled = state.aiCoreSupported,
                        onCheckedChange = actions.onToggleAiCoreSummarization,
                    )
                },
            )
        }
    }
}

@Composable
private fun EngineStatusRow(
    title: String,
    subtitle: String,
    badgeText: String,
    badgeContainer: Color,
    badgeContent: Color,
    trailing: (@Composable () -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = badgeContainer,
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeContent,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        trailing?.invoke()
    }
}
