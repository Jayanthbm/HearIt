package com.jayanth.hearit.intelligence

import com.jayanth.hearit.core.common.logger.HearItLogger
import com.jayanth.hearit.core.common.logger.LogCategory
import com.jayanth.hearit.core.datastore.HearItSettings
import com.jayanth.hearit.core.datastore.SettingsRepository
import com.jayanth.hearit.core.tts.SmartContentCleaner
import com.jayanth.hearit.service.SessionStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Hydrates [SmartContentCleaner] on process startup and keeps it synchronized with
 * both [SessionStore] flags/rules and [SettingsRepository] per-app aliases & categories.
 */
@Singleton
class IntelligentSettingsSync @Inject constructor(
    private val sessionStore: SessionStore,
    private val settingsRepository: SettingsRepository,
) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var observingStarted = false

    /** Synchronously applies all persisted NLP flags, rules, and filters from [SessionStore]. */
    fun hydrateSessionFlags() {
        SmartContentCleaner.isUrlSummarizerEnabled = sessionStore.isUrlSummarizerEnabled
        SmartContentCleaner.isCurrencyNormalizerEnabled = sessionStore.isCurrencyNormalizerEnabled
        SmartContentCleaner.isMarkdownStripperEnabled = sessionStore.isMarkdownStripperEnabled
        SmartContentCleaner.isEmojiCollapsingEnabled = sessionStore.isEmojiCollapsingEnabled
        SmartContentCleaner.isReactionFilterEnabled = sessionStore.isReactionFilterEnabled
        SmartContentCleaner.reactionFilterMode = sessionStore.reactionFilterMode
        SmartContentCleaner.isBotSenderCleaningEnabled = sessionStore.isBotSenderCleaningEnabled
        SmartContentCleaner.isPronunciationDictionaryEnabled = sessionStore.isPronunciationDictionaryEnabled
        SmartContentCleaner.pronunciationRules = sessionStore.pronunciationRules
        SmartContentCleaner.urlSummaryRules = sessionStore.urlSummaryRules
        SmartContentCleaner.isDigitFormattingEnabled = sessionStore.isDigitFormattingEnabled
        SmartContentCleaner.isFormatPhoneNumbersEnabled = sessionStore.isFormatPhoneNumbersEnabled
        SmartContentCleaner.isFormatOtpCodesEnabled = sessionStore.isFormatOtpCodesEnabled
        SmartContentCleaner.isMessagingIntelligenceEnabled = sessionStore.isMessagingIntelligenceEnabled
        SmartContentCleaner.isSmsBankCleanerEnabled = sessionStore.isSmsBankCleanerEnabled
        SmartContentCleaner.isCarrierSpamFilterEnabled = sessionStore.isCarrierSpamFilterEnabled
        SmartContentCleaner.carrierSpamPhrases = sessionStore.carrierSpamPhrases
        SmartContentCleaner.isEmailCleanupEnabled = sessionStore.isEmailCleanupEnabled
        SmartContentCleaner.isContactSanitizerEnabled = sessionStore.isContactSanitizerEnabled
    }

    /** Atomically applies per-app spoken aliases and category overrides from [settings]. */
    fun applySettings(settings: HearItSettings) {
        val aliases = settings.appOverrides.mapNotNull { (pkg, override) ->
            override.spokenName?.takeIf { it.isNotBlank() }?.let { pkg to it }
        }.toMap()
        val categories = settings.appOverrides.mapNotNull { (pkg, override) ->
            override.categoryOverride?.let { pkg to it }
        }.toMap()
        SmartContentCleaner.replaceAppAliases(aliases)
        SmartContentCleaner.replaceAppCategories(categories)
        sessionStore.cachedAllowedBluetoothIds = settings.eligibility.audioOutput.allowedBluetoothIds.orEmpty()
    }

    /** Hydrates synchronous flags immediately and begins observing DataStore settings in background. */
    fun start() {
        hydrateSessionFlags()
        if (observingStarted) return
        observingStarted = true
        appScope.launch {
            runCatching { applySettings(settingsRepository.current()) }
            settingsRepository.settings.collect { settings ->
                applySettings(settings)
            }
        }
        HearItLogger.info(LogCategory.SERVICE, "IntelligentSettingsSync initialized")
    }
}
