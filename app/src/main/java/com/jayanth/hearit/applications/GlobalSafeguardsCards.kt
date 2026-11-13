package com.jayanth.hearit.applications

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayanth.hearit.core.common.model.EligibilitySettings

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LowBatterySafeguardCard(
    eligibility: EligibilitySettings,
    onEligibilityChanged: (EligibilitySettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    SafeguardCardShell(
        title = "Low Battery",
        subtitle = "Pause speech on low charge or Battery Saver",
        icon = Icons.Rounded.BatteryAlert,
        trailing = {
            Switch(
                checked = eligibility.lowBatterySafeguardEnabled,
                onCheckedChange = {
                    onEligibilityChanged(eligibility.copy(lowBatterySafeguardEnabled = it))
                },
            )
        },
        modifier = modifier,
    ) {
        if (eligibility.lowBatterySafeguardEnabled) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Pause Below Battery %",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(10, 15, 20).forEach { pct ->
                        FilterChip(
                            selected = eligibility.lowBatteryThresholdPercent == pct,
                            onClick = {
                                onEligibilityChanged(eligibility.copy(lowBatteryThresholdPercent = pct))
                            },
                            label = { Text("$pct%") },
                        )
                    }
                }
                SafeguardToggleRow(
                    title = "Battery Saver mode",
                    subtitle = "Pause when system Battery Saver is on",
                    checked = eligibility.lowBatteryPauseOnSaverEnabled,
                    onCheckedChange = {
                        onEligibilityChanged(eligibility.copy(lowBatteryPauseOnSaverEnabled = it))
                    },
                )
                SafeguardToggleRow(
                    title = "Low battery warning",
                    subtitle = "Announce once when threshold is crossed",
                    checked = eligibility.lowBatteryWarningSpeechEnabled,
                    onCheckedChange = {
                        onEligibilityChanged(eligibility.copy(lowBatteryWarningSpeechEnabled = it))
                    },
                )
            }
        } else {
            Text(
                text = "Low battery auto-pause is disabled. Priority alerts always announce.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CriticalBreakthroughCard(
    eligibility: EligibilitySettings,
    onEligibilityChanged: (EligibilitySettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    var keywordInput by remember { mutableStateOf("") }

    SafeguardCardShell(
        title = "Breakthrough",
        subtitle = "Bypasses mute, DND, battery, and pocket",
        icon = Icons.Rounded.NotificationsActive,
        trailing = {
            Switch(
                checked = eligibility.criticalBreakthroughEnabled,
                onCheckedChange = {
                    onEligibilityChanged(eligibility.copy(criticalBreakthroughEnabled = it))
                },
            )
        },
        modifier = modifier,
    ) {
        if (eligibility.criticalBreakthroughEnabled) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SafeguardToggleRow(
                    title = "Allow for disabled apps",
                    subtitle = "Match keywords even in disabled apps",
                    checked = eligibility.criticalBreakthroughAllowDisabledApps,
                    onCheckedChange = {
                        onEligibilityChanged(eligibility.copy(criticalBreakthroughAllowDisabledApps = it))
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = keywordInput,
                        onValueChange = { keywordInput = it },
                        label = { Text("Critical keyword") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(
                        onClick = {
                            val clean = keywordInput.trim()
                            if (clean.isNotEmpty() && eligibility.criticalKeywords.none { it.equals(clean, true) }) {
                                onEligibilityChanged(
                                    eligibility.copy(criticalKeywords = eligibility.criticalKeywords + clean),
                                )
                                keywordInput = ""
                            }
                        },
                        enabled = keywordInput.isNotBlank(),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Add")
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    eligibility.criticalKeywords.forEach { kw ->
                        InputChip(
                            selected = true,
                            onClick = {
                                onEligibilityChanged(
                                    eligibility.copy(
                                        criticalKeywords = eligibility.criticalKeywords.filterNot {
                                            it.equals(kw, ignoreCase = true)
                                        },
                                    ),
                                )
                            },
                            label = { Text(kw, fontSize = 12.sp) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Remove keyword",
                                    modifier = Modifier.size(14.dp),
                                )
                            },
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = {
                            val merged = eligibility.criticalKeywords +
                                EligibilitySettings.DEFAULT_CRITICAL_KEYWORDS.filter { def ->
                                    eligibility.criticalKeywords.none { it.equals(def, ignoreCase = true) }
                                }
                            onEligibilityChanged(eligibility.copy(criticalKeywords = merged))
                        },
                    ) {
                        Text("Load Defaults")
                    }
                }
            }
        }
    }
}

@Composable
fun PocketDetectionCard(
    eligibility: EligibilitySettings,
    onEligibilityChanged: (EligibilitySettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val hasProximitySensor = remember(context) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        sm?.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null
    }

    SafeguardCardShell(
        title = "Pocket",
        subtitle = "Speak only when covered (unless Priority Announce)",
        icon = Icons.Rounded.PhoneAndroid,
        trailing = {
            Switch(
                checked = eligibility.pocketDetectionEnabled,
                enabled = hasProximitySensor,
                onCheckedChange = {
                    onEligibilityChanged(eligibility.copy(pocketDetectionEnabled = it))
                },
            )
        },
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (hasProximitySensor) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                } else {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
                },
            ) {
                Text(
                    text = if (hasProximitySensor) {
                        "Hardware status: Proximity sensor available on device"
                    } else {
                        "Hardware status: No proximity sensor detected on this device"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (hasProximitySensor) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
            if (eligibility.pocketDetectionEnabled && hasProximitySensor) {
                SafeguardToggleRow(
                    title = "Allow in-hand speech when headphones connected",
                    subtitle = "If disabled (strict), holding phone in front of face stays quiet even on earphones",
                    checked = eligibility.pocketDetectionHeadphoneBypass,
                    onCheckedChange = {
                        onEligibilityChanged(eligibility.copy(pocketDetectionHeadphoneBypass = it))
                    },
                )
            }
        }
    }
}

@Composable
private fun SafeguardCardShell(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 10.dp),
            ) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                trailing()
            }
            content()
        }
    }
}

@Composable
private fun SafeguardToggleRow(
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
