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
import com.jayanth.hearit.core.common.model.DigitGroupingStyle
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
        title = "Smart Content Cleaning & NLP",
        subtitle = "Intelligent cleaning of message text before text-to-speech",
        icon = Icons.Rounded.GraphicEq,
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // 1. URL Summarizer (#13)
            NlpToggleRow(
                title = "Clean & Summarize URLs",
                subtitle = "Speaks 'YouTube video' or 'Link to domain.com' instead of raw URLs",
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
                    Text("Configure URL Rules ($activeUrlCount active)")
                }
            }
            DividerLine()

            // 2. Pronunciation & Acronym Dictionary (#3)
            NlpToggleRow(
                title = "Pronunciation & Acronym Dictionary",
                subtitle = "Replaces words & acronyms (e.g. ETA → E T A) before speech",
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
                    Text("Manage Dictionary ($activeRulesCount rules)")
                }
            }
            DividerLine()

            // 3. OTP & Phone Number Digit-by-Digit Formatting (#12)
            NlpToggleRow(
                title = "Speak Codes & Phones Digit-by-Digit",
                subtitle = "Breaks OTPs (4 8 2, 9 1 0) and phone numbers into clear digits",
                checked = state.isDigitFormattingEnabled,
                onCheckedChange = actions.onToggleDigitFormatting,
            )
            if (state.isDigitFormattingEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.isFormatOtpCodesEnabled,
                        onClick = { actions.onToggleFormatOtpCodes(!state.isFormatOtpCodesEnabled) },
                        label = { Text("OTP / PINs") },
                    )
                    FilterChip(
                        selected = state.isFormatPhoneNumbersEnabled,
                        onClick = { actions.onToggleFormatPhoneNumbers(!state.isFormatPhoneNumbersEnabled) },
                        label = { Text("Phone Numbers") },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.digitGroupingStyle == DigitGroupingStyle.CHUNKS_OF_3,
                        onClick = { actions.onDigitGroupingStyleChanged(DigitGroupingStyle.CHUNKS_OF_3) },
                        label = { Text("Pause chunks (4 8 2, 9 1 0)") },
                    )
                    FilterChip(
                        selected = state.digitGroupingStyle == DigitGroupingStyle.INDIVIDUAL,
                        onClick = { actions.onDigitGroupingStyleChanged(DigitGroupingStyle.INDIVIDUAL) },
                        label = { Text("Single digits") },
                    )
                }
            }
            DividerLine()

            // 4. Currency & Units
            NlpToggleRow(
                title = "Currency & Unit Normalization",
                subtitle = "Converts '$50' → '50 dollars', '₹100' → '100 rupees', 'km/h', 'GB'",
                checked = state.isCurrencyNormalizerEnabled,
                onCheckedChange = actions.onToggleCurrencyNormalizer,
            )
            DividerLine()

            // 5. Markdown Stripper
            NlpToggleRow(
                title = "Strip Markdown & Markup",
                subtitle = "Removes bold/italic asterisks (*bold*), backticks, and bullet markers",
                checked = state.isMarkdownStripperEnabled,
                onCheckedChange = actions.onToggleMarkdownStripper,
            )
            DividerLine()

            // 6. Emoji Spam Collapsing
            NlpToggleRow(
                title = "Collapse Repeated Emojis",
                subtitle = "Prevents repetitive emoji reading on 3+ consecutive emojis",
                checked = state.isEmojiCollapsingEnabled,
                onCheckedChange = actions.onToggleEmojiCollapsing,
            )
            DividerLine()

            // 7. Tapback Reaction Filter & Mode (#26)
            NlpToggleRow(
                title = "Filter Tapback Reactions",
                subtitle = "Handles 'Liked a photo' or 'Reacted ❤️ to your message'",
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
                        label = { Text("Condense ('Reacted')") },
                    )
                    FilterChip(
                        selected = state.reactionFilterMode == ReactionFilterMode.SILENCE,
                        onClick = { actions.onReactionFilterModeChanged(ReactionFilterMode.SILENCE) },
                        label = { Text("Silence completely") },
                    )
                }
            }
            DividerLine()

            // 8. Contact Name Sanitizer (#21)
            NlpToggleRow(
                title = "Sanitize Contact & Sender Names",
                subtitle = "Cleans emojis and bracketed notes like 'Rahul (Work)'",
                checked = state.isContactSanitizerEnabled,
                onCheckedChange = actions.onToggleContactSanitizer,
            )
            if (state.isContactSanitizerEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.isStripSenderEmojisEnabled,
                        onClick = { actions.onToggleStripSenderEmojis(!state.isStripSenderEmojisEnabled) },
                        label = { Text("Strip Emojis") },
                    )
                    FilterChip(
                        selected = state.isStripSenderBracketsEnabled,
                        onClick = { actions.onToggleStripSenderBrackets(!state.isStripSenderBracketsEnabled) },
                        label = { Text("Strip Brackets (Note)") },
                    )
                }
            }
            DividerLine()

            // 9. Bot & Shortcode Cleaning
            NlpToggleRow(
                title = "Clean Bot & SMS Sender Headers",
                subtitle = "Converts SMS shortcodes ('VK-HDFCBK' → 'HDFCBK') and bot emails",
                checked = state.isBotSenderCleaningEnabled,
                onCheckedChange = actions.onToggleBotSenderCleaning,
            )
        }
    }

    if (showPronunciationDialog) {
        PronunciationDictionaryDialog(
            rules = state.pronunciationRules,
            applyOnSender = state.isPronunciationOnSenderEnabled,
            onToggleApplyOnSender = actions.onTogglePronunciationOnSender,
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
internal fun DividerLine() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}
