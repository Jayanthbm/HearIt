package com.jayanth.hearit.intelligence

import com.jayanth.hearit.core.common.model.AppCategory
import com.jayanth.hearit.core.common.model.DigitGroupingStyle
import com.jayanth.hearit.core.common.model.PronunciationRule
import com.jayanth.hearit.core.common.model.ReactionFilterMode
import com.jayanth.hearit.core.common.model.UrlSummaryRule
import com.jayanth.hearit.settings.GeneralSettingsViewModel

/**
 * Grouped callback holder for Active Intelligence & Smart Content NLP cards so
 * composables stay decoupled from the ViewModel without sprawling parameter lists.
 */
data class IntelligenceActions(
    val onToggleUrlSummarizer: (Boolean) -> Unit = {},
    val onSaveUrlSummaryRule: (oldPattern: String?, rule: UrlSummaryRule) -> Unit = { _, _ -> },
    val onToggleUrlSummaryRule: (pattern: String, enabled: Boolean) -> Unit = { _, _ -> },
    val onDeleteUrlSummaryRule: (pattern: String) -> Unit = {},
    val onLoadPredefinedUrlRules: () -> Unit = {},
    val onToggleCurrencyNormalizer: (Boolean) -> Unit = {},
    val onToggleMarkdownStripper: (Boolean) -> Unit = {},
    val onToggleEmojiCollapsing: (Boolean) -> Unit = {},
    val onToggleReactionFilter: (Boolean) -> Unit = {},
    val onReactionFilterModeChanged: (ReactionFilterMode) -> Unit = {},
    val onToggleBotSenderCleaning: (Boolean) -> Unit = {},
    val onTogglePronunciationDictionary: (Boolean) -> Unit = {},
    val onTogglePronunciationOnSender: (Boolean) -> Unit = {},
    val onSavePronunciationRule: (oldMatch: String?, rule: PronunciationRule) -> Unit = { _, _ -> },
    val onTogglePronunciationRule: (match: String, enabled: Boolean) -> Unit = { _, _ -> },
    val onDeletePronunciationRule: (match: String) -> Unit = {},
    val onLoadPronunciationPresets: () -> Unit = {},
    val onTestPronunciation: (String) -> Unit = {},
    val onToggleDigitFormatting: (Boolean) -> Unit = {},
    val onToggleFormatPhoneNumbers: (Boolean) -> Unit = {},
    val onToggleFormatOtpCodes: (Boolean) -> Unit = {},
    val onDigitGroupingStyleChanged: (DigitGroupingStyle) -> Unit = {},
    val onToggleMessagingIntelligence: (Boolean) -> Unit = {},
    val onToggleSmsBankCleaner: (Boolean) -> Unit = {},
    val onToggleCarrierSpamFilter: (Boolean) -> Unit = {},
    val onAddCarrierSpamPhrase: (String) -> Unit = {},
    val onRemoveCarrierSpamPhrase: (String) -> Unit = {},
    val onLoadDefaultCarrierSpamPhrases: () -> Unit = {},
    val onToggleEmailCleanup: (Boolean) -> Unit = {},
    val onToggleContactSanitizer: (Boolean) -> Unit = {},
    val onToggleStripSenderEmojis: (Boolean) -> Unit = {},
    val onToggleStripSenderBrackets: (Boolean) -> Unit = {},
    val onSetAppCategory: (packageName: String, category: AppCategory?) -> Unit = { _, _ -> },
    val onResetAppCategory: (packageName: String) -> Unit = {},
) {
    companion object {
        fun from(viewModel: GeneralSettingsViewModel): IntelligenceActions = IntelligenceActions(
            onToggleUrlSummarizer = viewModel::onToggleUrlSummarizer,
            onSaveUrlSummaryRule = viewModel::onSaveUrlSummaryRule,
            onToggleUrlSummaryRule = viewModel::onToggleUrlSummaryRule,
            onDeleteUrlSummaryRule = viewModel::onDeleteUrlSummaryRule,
            onLoadPredefinedUrlRules = viewModel::onLoadPredefinedUrlRules,
            onToggleCurrencyNormalizer = viewModel::onToggleCurrencyNormalizer,
            onToggleMarkdownStripper = viewModel::onToggleMarkdownStripper,
            onToggleEmojiCollapsing = viewModel::onToggleEmojiCollapsing,
            onToggleReactionFilter = viewModel::onToggleReactionFilter,
            onReactionFilterModeChanged = viewModel::onReactionFilterModeChanged,
            onToggleBotSenderCleaning = viewModel::onToggleBotSenderCleaning,
            onTogglePronunciationDictionary = viewModel::onTogglePronunciationDictionary,
            onTogglePronunciationOnSender = viewModel::onTogglePronunciationOnSender,
            onSavePronunciationRule = viewModel::onSavePronunciationRule,
            onTogglePronunciationRule = viewModel::onTogglePronunciationRule,
            onDeletePronunciationRule = viewModel::onDeletePronunciationRule,
            onLoadPronunciationPresets = viewModel::onLoadPronunciationPresets,
            onTestPronunciation = viewModel::onTestPronunciation,
            onToggleDigitFormatting = viewModel::onToggleDigitFormatting,
            onToggleFormatPhoneNumbers = viewModel::onToggleFormatPhoneNumbers,
            onToggleFormatOtpCodes = viewModel::onToggleFormatOtpCodes,
            onDigitGroupingStyleChanged = viewModel::onDigitGroupingStyleChanged,
            onToggleMessagingIntelligence = viewModel::onToggleMessagingIntelligence,
            onToggleSmsBankCleaner = viewModel::onToggleSmsBankCleaner,
            onToggleCarrierSpamFilter = viewModel::onToggleCarrierSpamFilter,
            onAddCarrierSpamPhrase = viewModel::onAddCarrierSpamPhrase,
            onRemoveCarrierSpamPhrase = viewModel::onRemoveCarrierSpamPhrase,
            onLoadDefaultCarrierSpamPhrases = viewModel::onLoadDefaultCarrierSpamPhrases,
            onToggleEmailCleanup = viewModel::onToggleEmailCleanup,
            onToggleContactSanitizer = viewModel::onToggleContactSanitizer,
            onToggleStripSenderEmojis = viewModel::onToggleStripSenderEmojis,
            onToggleStripSenderBrackets = viewModel::onToggleStripSenderBrackets,
            onSetAppCategory = viewModel::onSetAppCategory,
            onResetAppCategory = viewModel::onResetAppCategory,
        )
    }
}
