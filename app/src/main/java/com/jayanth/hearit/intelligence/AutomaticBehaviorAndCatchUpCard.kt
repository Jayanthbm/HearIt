package com.jayanth.hearit.intelligence

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jayanth.hearit.settings.GeneralSettingsUiState
import com.jayanth.hearit.settings.SettingsCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AutomaticBehaviorAndCatchUpCard(
    state: GeneralSettingsUiState,
    actions: IntelligenceActions,
    modifier: Modifier = Modifier,
) {
    SettingsCard(
        title = "Timing & Catch-Up",
        subtitle = "Cooldowns, time tags and connect summaries",
        icon = Icons.Rounded.AutoMode,
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // 1. App Name Deduplication (#19)
            NlpToggleRow(
                title = "Dedupe App Names",
                subtitle = "Omits repeating the app name when rapid alerts arrive from the same app",
                checked = state.isAppNameDedupEnabled,
                onCheckedChange = actions.onToggleAppNameDedup,
            )
            if (state.isAppNameDedupEnabled) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        15_000L to "15s",
                        30_000L to "30s",
                        45_000L to "45s",
                        60_000L to "60s",
                    ).forEach { (ms, label) ->
                        FilterChip(
                            selected = state.appNameDedupWindowMs == ms,
                            onClick = { actions.onAppNameDedupWindowChanged(ms) },
                            label = { Text(label) },
                        )
                    }
                }
            }
            DividerLine()

            // 2. Delayed Timestamp Context (#19 / Idea 40)
            NlpToggleRow(
                title = "Delayed Time Tag",
                subtitle = "Says 'Rahul sent 12 minutes ago:' for notifications held past threshold",
                checked = state.isDelayedTimestampTagEnabled,
                onCheckedChange = actions.onToggleDelayedTimestampTag,
            )
            if (state.isDelayedTimestampTagEnabled) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        60_000L to "1 m",
                        120_000L to "2 m",
                        300_000L to "5 m",
                    ).forEach { (ms, label) ->
                        FilterChip(
                            selected = state.delayedTimestampThresholdMs == ms,
                            onClick = { actions.onDelayedTimestampThresholdChanged(ms) },
                            label = { Text(label) },
                        )
                    }
                }
            }
            DividerLine()

            // 3. Connect Catch-Up Summary (#22)
            NlpToggleRow(
                title = "Connect Catch-Up",
                subtitle = "Summarizes pending + recently skipped alerts by app when earphones connect",
                checked = state.isCatchUpSummaryEnabled,
                onCheckedChange = actions.onToggleCatchUpSummary,
            )
            if (state.isCatchUpSummaryEnabled) {
                Text(
                    text = "Minimum Absence Before Greeting",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        0L to "Always",
                        5 * 60_000L to "5m+",
                        15 * 60_000L to "15m+",
                        30 * 60_000L to "30m+",
                    ).forEach { (ms, label) ->
                        FilterChip(
                            selected = state.catchUpAbsenceWindowMs == ms,
                            onClick = { actions.onCatchUpAbsenceWindowChanged(ms) },
                            label = { Text(label) },
                        )
                    }
                }
                NlpToggleRow(
                    title = "Include App Breakdown",
                    subtitle = "Lists app names (e.g. 'from WhatsApp and Gmail')",
                    checked = state.catchUpIncludeAppBreakdown,
                    onCheckedChange = actions.onToggleCatchUpIncludeApps,
                )
            }
            DividerLine()

            // 4. Chatty Group Flood Defense: Sender Burst Aggregator (#20)
            NlpToggleRow(
                title = "Burst Grouping",
                subtitle = "Optional sender burst aggregation ('Rahul sent 4 messages: ...'). Grouping Window handles rapid bursts by default.",
                checked = state.isSenderBurstAggregatorEnabled,
                onCheckedChange = actions.onToggleSenderBurstAggregator,
            )
            if (state.isSenderBurstAggregatorEnabled) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2, 3, 4).forEach { count ->
                        FilterChip(
                            selected = state.senderBurstMinCount == count,
                            onClick = { actions.onSenderBurstMinCountChanged(count) },
                            label = { Text("$count+ msgs") },
                        )
                    }
                    listOf(3_000L to "3s", 5_000L to "5s", 10_000L to "10s").forEach { (ms, lbl) ->
                        FilterChip(
                            selected = state.senderBurstWindowMs == ms,
                            onClick = { actions.onSenderBurstWindowChanged(ms) },
                            label = { Text(lbl) },
                        )
                    }
                }
            }
            DividerLine()

            // 5. Spam Cooldown Throttle (#28)
            NlpToggleRow(
                title = "Spam Cooldown",
                subtitle = "Prevents audio fatigue when a sender rapid-fires alerts, summarizing bursts",
                checked = state.isSpamCooldownEnabled,
                onCheckedChange = actions.onToggleSpamCooldown,
            )
            if (state.isSpamCooldownEnabled) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2, 3, 5).forEach { maxMsgs ->
                        FilterChip(
                            selected = state.spamCooldownMaxMessages == maxMsgs,
                            onClick = { actions.onSpamCooldownMaxMessagesChanged(maxMsgs) },
                            label = { Text("Max $maxMsgs") },
                        )
                    }
                    listOf(30_000L to "30s", 60_000L to "60s", 120_000L to "2m").forEach { (ms, lbl) ->
                        FilterChip(
                            selected = state.spamCooldownWindowMs == ms,
                            onClick = { actions.onSpamCooldownWindowChanged(ms) },
                            label = { Text(lbl) },
                        )
                    }
                }
            }
            DividerLine()

            // 6. Multi-App Smart Notification Batching Digest (#33)
            NlpToggleRow(
                title = "Multi-App Digest",
                subtitle = "Batch alerts across different apps into one spoken digest. Superseded by Grouping Window for single-app queues.",
                checked = state.isMultiAppDigestEnabled,
                onCheckedChange = actions.onToggleMultiAppDigest,
            )
            if (state.isMultiAppDigestEnabled) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val digestOptions = listOf(2_000L to "2 s", 3_000L to "3 s", 5_000L to "5 s")
                    // Stored values from an older build snap to the closest supported option so a
                    // chip is always selected.
                    val digestSelected = digestOptions.minByOrNull {
                        kotlin.math.abs(it.first - state.multiAppDigestWindowMs)
                    }?.first ?: 2_000L
                    digestOptions.forEach { (ms, lbl) ->
                        FilterChip(
                            selected = digestSelected == ms,
                            onClick = { actions.onMultiAppDigestWindowChanged(ms) },
                            label = { Text(lbl) },
                        )
                    }
                }
            }
        }
    }
}
