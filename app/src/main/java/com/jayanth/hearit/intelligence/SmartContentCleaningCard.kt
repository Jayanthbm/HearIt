package com.jayanth.hearit.intelligence

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jayanth.hearit.core.common.model.ReactionFilterMode
import com.jayanth.hearit.settings.GeneralSettingsUiState
import com.jayanth.hearit.settings.SettingsCard

@Composable
internal fun SmartContentCleaningCard(
    state: GeneralSettingsUiState,
    actions: IntelligenceActions,
    modifier: Modifier = Modifier,
) {
    var showPronunciationDialog by remember { mutableStateOf(false) }
    var showUrlRulesDialog by remember { mutableStateOf(false) }

    SettingsCard(
        title = "Intelligent cleanup",
        subtitle = "Intelligently cleans up the message text before speech",
        icon = Icons.Rounded.GraphicEq,
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // 1. URL Summarizer (#13)
            NlpToggleRow(
                title = "URL Detection",
                subtitle = "Speaks in natural words instead of raw URLs",
                checked = state.isUrlSummarizerEnabled,
                onCheckedChange = actions.onToggleUrlSummarizer,
            )
            if (state.isUrlSummarizerEnabled) {
                val activeUrlCount = state.urlSummaryRules.count { it.enabled }
                OutlinedButton(
                    onClick = { showUrlRulesDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("URL Rules ($activeUrlCount)")
                }
            }
            DividerLine()

            // 2. Pronunciation & Acronym Dictionary (#3)
            NlpToggleRow(
                title = "User Dictionary",
                subtitle = "Replaces words & acronyms before speech",
                checked = state.isPronunciationDictionaryEnabled,
                onCheckedChange = actions.onTogglePronunciationDictionary,
            )
            if (state.isPronunciationDictionaryEnabled) {
                val activeRulesCount = state.pronunciationRules.count { it.enabled }
                OutlinedButton(
                    onClick = { showPronunciationDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("User Dictionary ($activeRulesCount)")
                }
            }
            DividerLine()

            // 3. Currency & Units
            NlpToggleRow(
                title = "Currency & units",
                subtitle = "Speaks symbols and units as words",
                checked = state.isCurrencyNormalizerEnabled,
                onCheckedChange = actions.onToggleCurrencyNormalizer,
            )
            DividerLine()

            // 5. Markdown Stripper
            NlpToggleRow(
                title = "Remove formatting",
                subtitle = "Skips markdown symbols when speaking",
                checked = state.isMarkdownStripperEnabled,
                onCheckedChange = actions.onToggleMarkdownStripper,
            )
            DividerLine()

            // 6. Emoji Spam Collapsing
            NlpToggleRow(
                title = "Repeated emojis",
                subtitle = "Avoids reading long emoji streaks",
                checked = state.isEmojiCollapsingEnabled,
                onCheckedChange = actions.onToggleEmojiCollapsing,
            )
            DividerLine()

            // 7. Tapback Reaction Filter & Mode (#26)
            NlpToggleRow(
                title = "Reactions",
                subtitle = "Shortens or silences reaction alerts",
                checked = state.isReactionFilterEnabled,
                onCheckedChange = actions.onToggleReactionFilter,
            )
            if (state.isReactionFilterEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.reactionFilterMode == ReactionFilterMode.CONDENSE,
                        onClick = { actions.onReactionFilterModeChanged(ReactionFilterMode.CONDENSE) },
                        label = { Text("Condense") },
                    )
                    FilterChip(
                        selected = state.reactionFilterMode == ReactionFilterMode.SILENCE,
                        onClick = { actions.onReactionFilterModeChanged(ReactionFilterMode.SILENCE) },
                        label = { Text("Silence") },
                    )
                }
            }
            DividerLine()

            // 8. Contact Name Sanitizer (#21)
            NlpToggleRow(
                title = "Remove sender emojis",
                subtitle = "Removes emojis from sender names",
                checked = state.isContactSanitizerEnabled,
                onCheckedChange = actions.onToggleContactSanitizer,
            )
            DividerLine()

            // 9. Bot & Shortcode Cleaning
            NlpToggleRow(
                title = "Bot & SMS senders",
                subtitle = "Shortens SMS codes and bot sender names",
                checked = state.isBotSenderCleaningEnabled,
                onCheckedChange = actions.onToggleBotSenderCleaning,
            )
        }
    }

    if (showPronunciationDialog) {
        PronunciationDictionaryDialog(
            rules = state.pronunciationRules,
            onSaveRule = actions.onSavePronunciationRule,
            onToggleRule = actions.onTogglePronunciationRule,
            onDeleteRule = actions.onDeletePronunciationRule,
            onLoadPresets = actions.onLoadPronunciationPresets,
            onTestPronunciation = actions.onTestPronunciation,
            onDismiss = { showPronunciationDialog = false },
        )
    }

    if (showUrlRulesDialog) {
        UrlSummaryRulesDialog(
            rules = state.urlSummaryRules,
            onSaveRule = actions.onSaveUrlSummaryRule,
            onToggleRule = actions.onToggleUrlSummaryRule,
            onDeleteRule = actions.onDeleteUrlSummaryRule,
            onLoadPredefined = actions.onLoadPredefinedUrlRules,
            onDismiss = { showUrlRulesDialog = false },
        )
    }
}

@Composable
internal fun NlpToggleRow(
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
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
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
internal fun DividerLine() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}
