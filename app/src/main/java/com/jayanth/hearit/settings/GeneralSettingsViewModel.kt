package com.jayanth.hearit.settings

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayanth.hearit.applications.AppEntry
import com.jayanth.hearit.applications.AppSettingsEditor
import com.jayanth.hearit.applications.ApplicationCatalog
import com.jayanth.hearit.core.common.model.AppCategory
import com.jayanth.hearit.core.common.model.AppCategoryChoice
import com.jayanth.hearit.core.common.model.AudioFocusPolicy
import com.jayanth.hearit.core.common.model.EligibilitySettings
import com.jayanth.hearit.core.common.model.IntelligencePresets
import com.jayanth.hearit.core.common.model.OverflowPolicy
import com.jayanth.hearit.core.common.model.PronunciationRule
import com.jayanth.hearit.core.common.model.QueueSettings
import com.jayanth.hearit.core.common.model.ReactionFilterMode
import com.jayanth.hearit.core.common.model.UrlSummaryRule
import com.jayanth.hearit.core.datastore.SettingsRepository
import com.jayanth.hearit.core.tts.AnnouncementSection
import com.jayanth.hearit.core.tts.BuiltAnnouncement
import com.jayanth.hearit.core.tts.SectionKind
import com.jayanth.hearit.core.tts.SmartContentCleaner
import com.jayanth.hearit.core.tts.TextToSpeechEngine
import com.jayanth.hearit.core.tts.TtsError
import com.jayanth.hearit.core.tts.UtteranceListener
import com.jayanth.hearit.intelligence.IntelligentSettingsSync
import com.jayanth.hearit.security.BiometricAuthManager
import com.jayanth.hearit.security.BiometricAuthResult
import com.jayanth.hearit.service.AnnouncementNotification
import com.jayanth.hearit.service.SettingsSource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing General Settings & Active Intelligence controls.
 *
 * Single-writer pattern over [SettingsRepository] (ADR-021).
 */
@HiltViewModel
class GeneralSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val biometricAuthManager: BiometricAuthManager,
    private val applicationCatalog: ApplicationCatalog,
    private val ttsEngine: TextToSpeechEngine,
    private val settingsSource: SettingsSource,
    private val intelligentSettingsSync: IntelligentSettingsSync,
    private val sessionStore: com.jayanth.hearit.service.SessionStore,
    private val capabilityProbe: com.jayanth.hearit.intelligence.IntelligenceCapabilityProbe,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private var installedCatalog: List<AppEntry> = emptyList()

    private val _state = MutableStateFlow(
        GeneralSettingsUiState(
            isServiceNotificationVisible = AnnouncementNotification.isNotificationVisible(context),
            isBiometricAvailable = biometricAuthManager.canAuthenticate(),
            isAutoDismissEnabled = sessionStore.isAutoDismissEnabled,
            isShakeToStopEnabled = sessionStore.isShakeToStopEnabled,
            isVolumeKeyToStopEnabled = sessionStore.isVolumeKeyToStopEnabled,
            isMusicPauseHoldEnabled = sessionStore.isMusicPauseHoldEnabled,
            isBluetoothAutoStartEnabled = sessionStore.isBluetoothAutoStartEnabled,
            isPeriodicCheckinEnabled = sessionStore.isPeriodicCheckinEnabled,
            periodicCheckinIntervalMinutes = sessionStore.periodicCheckinIntervalMinutes,
            periodicCheckinAnnounceTime = sessionStore.periodicCheckinAnnounceTime,
            periodicCheckinAnnounceBattery = sessionStore.periodicCheckinAnnounceBattery,
            isUrlSummarizerEnabled = sessionStore.isUrlSummarizerEnabled,
            urlSummaryRules = sessionStore.urlSummaryRules,
            isCurrencyNormalizerEnabled = sessionStore.isCurrencyNormalizerEnabled,
            isMarkdownStripperEnabled = sessionStore.isMarkdownStripperEnabled,
            isEmojiCollapsingEnabled = sessionStore.isEmojiCollapsingEnabled,
            isReactionFilterEnabled = sessionStore.isReactionFilterEnabled,
            reactionFilterMode = sessionStore.reactionFilterMode,
            isBotSenderCleaningEnabled = sessionStore.isBotSenderCleaningEnabled,
            isPronunciationDictionaryEnabled = sessionStore.isPronunciationDictionaryEnabled,
            pronunciationRules = sessionStore.pronunciationRules,
            isDigitFormattingEnabled = sessionStore.isDigitFormattingEnabled,
            isFormatPhoneNumbersEnabled = sessionStore.isFormatPhoneNumbersEnabled,
            isFormatOtpCodesEnabled = sessionStore.isFormatOtpCodesEnabled,
            isSmsBankCleanerEnabled = sessionStore.isSmsBankCleanerEnabled,
            isCarrierSpamFilterEnabled = sessionStore.isCarrierSpamFilterEnabled,
            carrierSpamPhrases = sessionStore.carrierSpamPhrases,
            isEmailCleanupEnabled = sessionStore.isEmailCleanupEnabled,
            isContactSanitizerEnabled = sessionStore.isContactSanitizerEnabled,
            isSkipEmptyNotificationsEnabled = sessionStore.isSkipEmptyNotificationsEnabled,
            isDuckingLevelControlEnabled = sessionStore.isDuckingLevelControlEnabled,
            duckingVolumePercent = sessionStore.duckingVolumePercent,
            audioFocusReturnDelayMs = sessionStore.audioFocusReturnDelayMs,
            isNavigationYieldEnabled = sessionStore.isNavigationYieldEnabled,
            isReannounceAfterNavigationEnabled = sessionStore.isReannounceAfterNavigationEnabled,
            navigationYieldSettleDelayMs = sessionStore.navigationYieldSettleDelayMs,
            isAppNameDedupEnabled = sessionStore.isAppNameDedupEnabled,
            appNameDedupWindowMs = sessionStore.appNameDedupWindowMs,
            isDelayedTimestampTagEnabled = sessionStore.isDelayedTimestampTagEnabled,
            delayedTimestampThresholdMs = sessionStore.delayedTimestampThresholdMs,
            musicPauseHoldDurationMs = sessionStore.musicPauseHoldDurationMs,
            isFlipToStopEnabled = sessionStore.isFlipToStopEnabled,
            isShakeToRepeatEnabled = sessionStore.isShakeToRepeatEnabled,
            shakeRepeatWindowMs = sessionStore.shakeRepeatWindowMs,
            isCatchUpSummaryEnabled = sessionStore.isCatchUpSummaryEnabled,
            catchUpAbsenceWindowMs = sessionStore.catchUpAbsenceWindowMs,
            catchUpIncludeAppBreakdown = sessionStore.catchUpIncludeAppBreakdown,
            isBluetoothAutoStopEnabled = sessionStore.isBluetoothAutoStopEnabled,
            isBluetoothAutoOnlyAllowedEnabled = sessionStore.isBluetoothAutoOnlyAllowedEnabled,
            periodicCheckinAnnounceEarphonesBattery = sessionStore.periodicCheckinAnnounceEarphonesBattery,
            periodicCheckinAnnounceSummary = sessionStore.periodicCheckinAnnounceSummary,
            isSenderBurstAggregatorEnabled = sessionStore.isSenderBurstAggregatorEnabled,
            senderBurstMinCount = sessionStore.senderBurstMinCount,
            senderBurstWindowMs = sessionStore.senderBurstWindowMs,
            isRepeatLastShortcutEnabled = sessionStore.isRepeatLastShortcutEnabled,
            isSpamCooldownEnabled = sessionStore.isSpamCooldownEnabled,
            spamCooldownMaxMessages = sessionStore.spamCooldownMaxMessages,
            spamCooldownWindowMs = sessionStore.spamCooldownWindowMs,
            isMultiAppDigestEnabled = sessionStore.isMultiAppDigestEnabled,
            multiAppDigestWindowMs = sessionStore.multiAppDigestWindowMs,
            isMlKitEntityExtractionEnabled = sessionStore.isMlKitEntityExtractionEnabled,
            isAiCoreSummarizationEnabled = sessionStore.isAiCoreSummarizationEnabled,
        ),
    )
    val state: StateFlow<GeneralSettingsUiState> = _state.asStateFlow()

    init {
        intelligentSettingsSync.hydrateSessionFlags()
        onRefreshLocalAiCapabilities()
        viewModelScope.launch {
            installedCatalog = runCatching { applicationCatalog.installedApps() }.getOrDefault(emptyList())
            val currentSettings = runCatching { settingsRepository.current() }.getOrNull()
            val categories = currentSettings?.let { settings ->
                installedCatalog.associate { it.packageName to settings.categoryFor(it.packageName) }
            } ?: emptyMap()
            _state.update { it.copy(installedApps = installedCatalog, appCategories = categories) }
        }
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                intelligentSettingsSync.applySettings(settings)
                val categories = installedCatalog.associate { entry ->
                    entry.packageName to settings.categoryFor(entry.packageName)
                }
                _state.update { current ->
                    current.copy(
                        queue = settings.queue,
                        pauseBetweenSectionsMillis = settings.format.pauseBetweenSectionsMillis,
                        audioFocus = settings.eligibility.audioFocus,
                        eligibility = settings.eligibility,
                        security = settings.security,
                        isBiometricAvailable = biometricAuthManager.canAuthenticate(),
                        appCategories = categories,
                    )
                }
            }
        }
    }

    fun refreshServiceNotificationStatus() {
        val visible = AnnouncementNotification.isNotificationVisible(context)
        val canAuth = biometricAuthManager.canAuthenticate()
        _state.update {
            it.copy(
                isServiceNotificationVisible = visible,
                isBiometricAvailable = canAuth,
            )
        }
    }

    fun openServiceNotificationSettings() {
        AnnouncementNotification.openNotificationSettings(context)
    }

    fun onToggleAutoDismiss(enabled: Boolean) {
        sessionStore.isAutoDismissEnabled = enabled
        _state.update { it.copy(isAutoDismissEnabled = enabled) }
    }

    fun onToggleShakeToStop(enabled: Boolean) {
        sessionStore.isShakeToStopEnabled = enabled
        _state.update { it.copy(isShakeToStopEnabled = enabled) }
    }

    fun onToggleVolumeKeyToStop(enabled: Boolean) {
        sessionStore.isVolumeKeyToStopEnabled = enabled
        _state.update { it.copy(isVolumeKeyToStopEnabled = enabled) }
    }

    fun onToggleMusicPauseHold(enabled: Boolean) {
        sessionStore.isMusicPauseHoldEnabled = enabled
        _state.update { it.copy(isMusicPauseHoldEnabled = enabled) }
    }

    fun onToggleBluetoothAutoStart(enabled: Boolean) {
        sessionStore.isBluetoothAutoStartEnabled = enabled
        _state.update { it.copy(isBluetoothAutoStartEnabled = enabled) }
    }

    fun onTogglePeriodicCheckin(enabled: Boolean) {
        sessionStore.isPeriodicCheckinEnabled = enabled
        _state.update { it.copy(isPeriodicCheckinEnabled = enabled) }
    }

    fun onPeriodicCheckinIntervalChanged(minutes: Int) {
        sessionStore.periodicCheckinIntervalMinutes = minutes
        _state.update { it.copy(periodicCheckinIntervalMinutes = minutes) }
    }

    fun onTogglePeriodicCheckinTime(enabled: Boolean) {
        sessionStore.periodicCheckinAnnounceTime = enabled
        _state.update { it.copy(periodicCheckinAnnounceTime = enabled) }
    }

    fun onTogglePeriodicCheckinBattery(enabled: Boolean) {
        sessionStore.periodicCheckinAnnounceBattery = enabled
        _state.update { it.copy(periodicCheckinAnnounceBattery = enabled) }
    }

    fun onToggleUrlSummarizer(enabled: Boolean) {
        sessionStore.isUrlSummarizerEnabled = enabled
        com.jayanth.hearit.core.tts.SmartContentCleaner.isUrlSummarizerEnabled = enabled
        _state.update { it.copy(isUrlSummarizerEnabled = enabled) }
    }

    fun onToggleCurrencyNormalizer(enabled: Boolean) {
        sessionStore.isCurrencyNormalizerEnabled = enabled
        com.jayanth.hearit.core.tts.SmartContentCleaner.isCurrencyNormalizerEnabled = enabled
        _state.update { it.copy(isCurrencyNormalizerEnabled = enabled) }
    }

    fun onToggleMarkdownStripper(enabled: Boolean) {
        sessionStore.isMarkdownStripperEnabled = enabled
        com.jayanth.hearit.core.tts.SmartContentCleaner.isMarkdownStripperEnabled = enabled
        _state.update { it.copy(isMarkdownStripperEnabled = enabled) }
    }

    fun onToggleEmojiCollapsing(enabled: Boolean) {
        sessionStore.isEmojiCollapsingEnabled = enabled
        com.jayanth.hearit.core.tts.SmartContentCleaner.isEmojiCollapsingEnabled = enabled
        _state.update { it.copy(isEmojiCollapsingEnabled = enabled) }
    }

    fun onToggleReactionFilter(enabled: Boolean) {
        sessionStore.isReactionFilterEnabled = enabled
        com.jayanth.hearit.core.tts.SmartContentCleaner.isReactionFilterEnabled = enabled
        _state.update { it.copy(isReactionFilterEnabled = enabled) }
    }

    fun onToggleBotSenderCleaning(enabled: Boolean) {
        sessionStore.isBotSenderCleaningEnabled = enabled
        SmartContentCleaner.isBotSenderCleaningEnabled = enabled
        _state.update { it.copy(isBotSenderCleaningEnabled = enabled) }
    }

    fun onReactionFilterModeChanged(mode: ReactionFilterMode) {
        sessionStore.reactionFilterMode = mode
        SmartContentCleaner.reactionFilterMode = mode
        _state.update { it.copy(reactionFilterMode = mode) }
    }

    fun onTogglePronunciationDictionary(enabled: Boolean) {
        sessionStore.isPronunciationDictionaryEnabled = enabled
        SmartContentCleaner.isPronunciationDictionaryEnabled = enabled
        _state.update { it.copy(isPronunciationDictionaryEnabled = enabled) }
    }

    fun onSavePronunciationRule(oldMatch: String?, rule: PronunciationRule) {
        val cleanMatch = rule.match.trim()
        val cleanSpoken = rule.spokenAs.trim()
        if (cleanMatch.isEmpty() || cleanSpoken.isEmpty()) return
        val normalized = rule.copy(match = cleanMatch, spokenAs = cleanSpoken)
        val filtered = sessionStore.pronunciationRules.filterNot { existing ->
            existing.match.equals(oldMatch ?: cleanMatch, ignoreCase = true) ||
                existing.match.equals(cleanMatch, ignoreCase = true)
        } + normalized
        sessionStore.pronunciationRules = filtered
        SmartContentCleaner.pronunciationRules = filtered
        _state.update { it.copy(pronunciationRules = filtered) }
    }

    fun onTogglePronunciationRule(match: String, enabled: Boolean) {
        val updated = sessionStore.pronunciationRules.map { rule ->
            if (rule.match.equals(match, ignoreCase = true)) rule.copy(enabled = enabled) else rule
        }
        sessionStore.pronunciationRules = updated
        SmartContentCleaner.pronunciationRules = updated
        _state.update { it.copy(pronunciationRules = updated) }
    }

    fun onDeletePronunciationRule(match: String) {
        val updated = sessionStore.pronunciationRules.filterNot { it.match.equals(match, ignoreCase = true) }
        sessionStore.pronunciationRules = updated
        SmartContentCleaner.pronunciationRules = updated
        _state.update { it.copy(pronunciationRules = updated) }
    }

    fun onLoadPronunciationPresets() {
        val current = sessionStore.pronunciationRules
        val merged = current + IntelligencePresets.DEFAULT_PRONUNCIATION_RULES.filter { preset ->
            current.none { it.match.equals(preset.match, ignoreCase = true) }
        }
        sessionStore.pronunciationRules = merged
        SmartContentCleaner.pronunciationRules = merged
        _state.update { it.copy(pronunciationRules = merged) }
    }

    fun onTestPronunciation(sampleText: String) {
        val cleaned = SmartContentCleaner.cleanMessageText(sampleText)
        if (cleaned.isBlank()) return
        val announcement = BuiltAnnouncement(
            sections = listOf(AnnouncementSection(SectionKind.MESSAGE, cleaned)),
            pauseBetweenSectionsMillis = 0L,
        )
        ttsEngine.speak(
            announcement = announcement,
            settings = settingsSource.speech(),
            listener = object : UtteranceListener {
                override fun onStart(announcement: BuiltAnnouncement) {}
                override fun onComplete(announcement: BuiltAnnouncement) {}
                override fun onError(announcement: BuiltAnnouncement, error: TtsError) {}
            },
        )
    }

    fun onSaveUrlSummaryRule(oldPattern: String?, rule: UrlSummaryRule) {
        val cleanPattern = rule.pattern.trim()
        val cleanPhrase = rule.spokenPhrase.trim()
        if (cleanPattern.isEmpty() || cleanPhrase.isEmpty()) return
        val normalized = rule.copy(pattern = cleanPattern, spokenPhrase = cleanPhrase)
        val updated = sessionStore.urlSummaryRules.filterNot { existing ->
            existing.pattern.equals(oldPattern ?: cleanPattern, ignoreCase = true) ||
                existing.pattern.equals(cleanPattern, ignoreCase = true)
        } + normalized
        sessionStore.urlSummaryRules = updated
        SmartContentCleaner.urlSummaryRules = updated
        _state.update { it.copy(urlSummaryRules = updated) }
    }

    fun onToggleUrlSummaryRule(pattern: String, enabled: Boolean) {
        val updated = sessionStore.urlSummaryRules.map { rule ->
            if (rule.pattern.equals(pattern, ignoreCase = true)) rule.copy(enabled = enabled) else rule
        }
        sessionStore.urlSummaryRules = updated
        SmartContentCleaner.urlSummaryRules = updated
        _state.update { it.copy(urlSummaryRules = updated) }
    }

    fun onDeleteUrlSummaryRule(pattern: String) {
        val updated = sessionStore.urlSummaryRules.filterNot { it.pattern.equals(pattern, ignoreCase = true) }
        sessionStore.urlSummaryRules = updated
        SmartContentCleaner.urlSummaryRules = updated
        _state.update { it.copy(urlSummaryRules = updated) }
    }

    fun onLoadPredefinedUrlRules() {
        val current = sessionStore.urlSummaryRules
        val merged = current + IntelligencePresets.DEFAULT_URL_SUMMARY_RULES.filter { preset ->
            current.none { it.pattern.equals(preset.pattern, ignoreCase = true) }
        }
        sessionStore.urlSummaryRules = merged
        SmartContentCleaner.urlSummaryRules = merged
        _state.update { it.copy(urlSummaryRules = merged) }
    }

    fun onToggleDigitFormatting(enabled: Boolean) {
        sessionStore.isDigitFormattingEnabled = enabled
        SmartContentCleaner.isDigitFormattingEnabled = enabled
        _state.update { it.copy(isDigitFormattingEnabled = enabled) }
    }

    fun onToggleFormatPhoneNumbers(enabled: Boolean) {
        sessionStore.isFormatPhoneNumbersEnabled = enabled
        SmartContentCleaner.isFormatPhoneNumbersEnabled = enabled
        _state.update { it.copy(isFormatPhoneNumbersEnabled = enabled) }
    }

    fun onToggleFormatOtpCodes(enabled: Boolean) {
        sessionStore.isFormatOtpCodesEnabled = enabled
        SmartContentCleaner.isFormatOtpCodesEnabled = enabled
        _state.update { it.copy(isFormatOtpCodesEnabled = enabled) }
    }


    fun onToggleSmsBankCleaner(enabled: Boolean) {
        sessionStore.isSmsBankCleanerEnabled = enabled
        SmartContentCleaner.isSmsBankCleanerEnabled = enabled
        _state.update { it.copy(isSmsBankCleanerEnabled = enabled) }
    }

    fun onToggleCarrierSpamFilter(enabled: Boolean) {
        sessionStore.isCarrierSpamFilterEnabled = enabled
        SmartContentCleaner.isCarrierSpamFilterEnabled = enabled
        _state.update { it.copy(isCarrierSpamFilterEnabled = enabled) }
    }

    fun onAddCarrierSpamPhrase(phrase: String) {
        val clean = phrase.trim()
        if (clean.isEmpty()) return
        val updated = (sessionStore.carrierSpamPhrases.filterNot { it.equals(clean, ignoreCase = true) }) + clean
        sessionStore.carrierSpamPhrases = updated
        SmartContentCleaner.carrierSpamPhrases = updated
        _state.update { it.copy(carrierSpamPhrases = updated) }
    }

    fun onRemoveCarrierSpamPhrase(phrase: String) {
        val updated = sessionStore.carrierSpamPhrases.filterNot { it.equals(phrase, ignoreCase = true) }
        sessionStore.carrierSpamPhrases = updated
        SmartContentCleaner.carrierSpamPhrases = updated
        _state.update { it.copy(carrierSpamPhrases = updated) }
    }

    fun onLoadDefaultCarrierSpamPhrases() {
        val current = sessionStore.carrierSpamPhrases
        val merged = current + IntelligencePresets.DEFAULT_CARRIER_SPAM_PHRASES.filter { preset ->
            current.none { it.equals(preset, ignoreCase = true) }
        }
        sessionStore.carrierSpamPhrases = merged
        SmartContentCleaner.carrierSpamPhrases = merged
        _state.update { it.copy(carrierSpamPhrases = merged) }
    }

    fun onToggleEmailCleanup(enabled: Boolean) {
        sessionStore.isEmailCleanupEnabled = enabled
        SmartContentCleaner.isEmailCleanupEnabled = enabled
        _state.update { it.copy(isEmailCleanupEnabled = enabled) }
    }

    fun onToggleContactSanitizer(enabled: Boolean) {
        sessionStore.isContactSanitizerEnabled = enabled
        SmartContentCleaner.isContactSanitizerEnabled = enabled
        _state.update { it.copy(isContactSanitizerEnabled = enabled) }
    }

    fun onToggleSkipEmptyNotifications(enabled: Boolean) {
        sessionStore.isSkipEmptyNotificationsEnabled = enabled
        SmartContentCleaner.isSkipEmptyNotificationsEnabled = enabled
        _state.update { it.copy(isSkipEmptyNotificationsEnabled = enabled) }
    }

    fun onSetAppCategory(packageName: String, category: AppCategory?) {
        val choice = AppCategoryChoice.fromCategory(category)
        viewModelScope.launch {
            settingsRepository.update { current ->
                AppSettingsEditor.setCategoryChoice(current, packageName, choice)
            }
        }
    }

    fun onResetAppCategory(packageName: String) {
        viewModelScope.launch {
            settingsRepository.update { current ->
                AppSettingsEditor.setCategoryChoice(current, packageName, null)
            }
        }
    }

    fun onToggleDuckingLevelControl(enabled: Boolean) {
        sessionStore.isDuckingLevelControlEnabled = enabled
        _state.update { it.copy(isDuckingLevelControlEnabled = enabled) }
    }

    fun onDuckingVolumePercentChanged(percent: Int) {
        sessionStore.duckingVolumePercent = percent
        _state.update { it.copy(duckingVolumePercent = percent) }
    }

    fun onAudioFocusReturnDelayChanged(delayMs: Long) {
        sessionStore.audioFocusReturnDelayMs = delayMs
        _state.update { it.copy(audioFocusReturnDelayMs = delayMs) }
    }

    fun onToggleNavigationYield(enabled: Boolean) {
        sessionStore.isNavigationYieldEnabled = enabled
        _state.update { it.copy(isNavigationYieldEnabled = enabled) }
    }

    fun onToggleReannounceAfterNavigation(enabled: Boolean) {
        sessionStore.isReannounceAfterNavigationEnabled = enabled
        _state.update { it.copy(isReannounceAfterNavigationEnabled = enabled) }
    }

    fun onNavigationYieldSettleDelayChanged(delayMs: Long) {
        sessionStore.navigationYieldSettleDelayMs = delayMs
        _state.update { it.copy(navigationYieldSettleDelayMs = delayMs) }
    }

    fun onToggleAppNameDedup(enabled: Boolean) {
        sessionStore.isAppNameDedupEnabled = enabled
        _state.update { it.copy(isAppNameDedupEnabled = enabled) }
    }

    fun onAppNameDedupWindowChanged(windowMs: Long) {
        sessionStore.appNameDedupWindowMs = windowMs
        _state.update { it.copy(appNameDedupWindowMs = windowMs) }
    }

    fun onToggleDelayedTimestampTag(enabled: Boolean) {
        sessionStore.isDelayedTimestampTagEnabled = enabled
        _state.update { it.copy(isDelayedTimestampTagEnabled = enabled) }
    }

    fun onDelayedTimestampThresholdChanged(thresholdMs: Long) {
        sessionStore.delayedTimestampThresholdMs = thresholdMs
        _state.update { it.copy(delayedTimestampThresholdMs = thresholdMs) }
    }

    fun onMusicPauseHoldDurationChanged(durationMs: Long) {
        sessionStore.musicPauseHoldDurationMs = durationMs
        _state.update { it.copy(musicPauseHoldDurationMs = durationMs) }
    }

    fun onToggleFlipToStop(enabled: Boolean) {
        sessionStore.isFlipToStopEnabled = enabled
        _state.update { it.copy(isFlipToStopEnabled = enabled) }
    }

    fun onToggleShakeToRepeat(enabled: Boolean) {
        sessionStore.isShakeToRepeatEnabled = enabled
        _state.update { it.copy(isShakeToRepeatEnabled = enabled) }
    }

    fun onShakeRepeatWindowChanged(windowMs: Long) {
        sessionStore.shakeRepeatWindowMs = windowMs
        _state.update { it.copy(shakeRepeatWindowMs = windowMs) }
    }

    fun onToggleCatchUpSummary(enabled: Boolean) {
        sessionStore.isCatchUpSummaryEnabled = enabled
        _state.update { it.copy(isCatchUpSummaryEnabled = enabled) }
    }

    fun onCatchUpAbsenceWindowChanged(windowMs: Long) {
        sessionStore.catchUpAbsenceWindowMs = windowMs
        _state.update { it.copy(catchUpAbsenceWindowMs = windowMs) }
    }

    fun onToggleCatchUpIncludeApps(include: Boolean) {
        sessionStore.catchUpIncludeAppBreakdown = include
        _state.update { it.copy(catchUpIncludeAppBreakdown = include) }
    }

    fun onToggleBluetoothAutoStop(enabled: Boolean) {
        sessionStore.isBluetoothAutoStopEnabled = enabled
        _state.update { it.copy(isBluetoothAutoStopEnabled = enabled) }
    }

    fun onToggleBluetoothAutoOnlyAllowed(enabled: Boolean) {
        sessionStore.isBluetoothAutoOnlyAllowedEnabled = enabled
        _state.update { it.copy(isBluetoothAutoOnlyAllowedEnabled = enabled) }
    }

    fun onTogglePeriodicCheckinEarphonesBattery(enabled: Boolean) {
        sessionStore.periodicCheckinAnnounceEarphonesBattery = enabled
        _state.update { it.copy(periodicCheckinAnnounceEarphonesBattery = enabled) }
    }

    fun onTogglePeriodicCheckinSummary(enabled: Boolean) {
        sessionStore.periodicCheckinAnnounceSummary = enabled
        _state.update { it.copy(periodicCheckinAnnounceSummary = enabled) }
    }

    fun onUpdateEligibility(updated: EligibilitySettings) {
        viewModelScope.launch {
            settingsRepository.update { current -> current.copy(eligibility = updated) }
        }
    }

    fun onToggleSenderBurstAggregator(enabled: Boolean) {
        sessionStore.isSenderBurstAggregatorEnabled = enabled
        _state.update { it.copy(isSenderBurstAggregatorEnabled = enabled) }
    }

    fun onSenderBurstMinCountChanged(count: Int) {
        sessionStore.senderBurstMinCount = count
        _state.update { it.copy(senderBurstMinCount = count) }
    }

    fun onSenderBurstWindowChanged(windowMs: Long) {
        sessionStore.senderBurstWindowMs = windowMs
        _state.update { it.copy(senderBurstWindowMs = windowMs) }
    }

    fun onToggleRepeatLastShortcut(enabled: Boolean) {
        sessionStore.isRepeatLastShortcutEnabled = enabled
        _state.update { it.copy(isRepeatLastShortcutEnabled = enabled) }
    }

    fun onRefreshLocalAiCapabilities() {
        viewModelScope.launch {
            val report = capabilityProbe.probe()
            _state.update {
                it.copy(
                    mlKitStatusLabel = report.mlKitStatusLabel,
                    mlKitSupported = report.mlKitSupported,
                    mlKitModelReady = report.mlKitModelReady,
                    aiCoreStatusLabel = report.aiCoreStatusLabel,
                    aiCoreSupported = report.aiCoreSupported,
                )
            }
        }
    }

    fun onToggleMlKitEntityExtraction(enabled: Boolean) {
        sessionStore.isMlKitEntityExtractionEnabled = enabled
        _state.update { it.copy(isMlKitEntityExtractionEnabled = enabled) }
        if (enabled && !_state.value.mlKitModelReady && _state.value.mlKitSupported) {
            onDownloadMlKitModel()
        }
    }

    fun onDownloadMlKitModel() {
        if (_state.value.isMlKitDownloading) return
        _state.update { it.copy(isMlKitDownloading = true) }
        viewModelScope.launch {
            val result = capabilityProbe.downloadMlKitModel()
            _state.update { it.copy(isMlKitDownloading = false) }
            if (result.isSuccess && result.getOrDefault(false)) {
                sessionStore.isMlKitEntityExtractionEnabled = true
                _state.update { it.copy(isMlKitEntityExtractionEnabled = true) }
            }
            onRefreshLocalAiCapabilities()
        }
    }

    fun onToggleAiCoreSummarization(enabled: Boolean) {
        sessionStore.isAiCoreSummarizationEnabled = enabled
        _state.update { it.copy(isAiCoreSummarizationEnabled = enabled) }
    }

    fun onToggleSpamCooldown(enabled: Boolean) {
        sessionStore.isSpamCooldownEnabled = enabled
        _state.update { it.copy(isSpamCooldownEnabled = enabled) }
    }

    fun onSpamCooldownMaxMessagesChanged(maxMessages: Int) {
        sessionStore.spamCooldownMaxMessages = maxMessages
        _state.update { it.copy(spamCooldownMaxMessages = maxMessages) }
    }

    fun onSpamCooldownWindowChanged(windowMs: Long) {
        sessionStore.spamCooldownWindowMs = windowMs
        _state.update { it.copy(spamCooldownWindowMs = windowMs) }
    }

    fun onToggleMultiAppDigest(enabled: Boolean) {
        sessionStore.isMultiAppDigestEnabled = enabled
        _state.update { it.copy(isMultiAppDigestEnabled = enabled) }
    }

    fun onMultiAppDigestWindowChanged(windowMs: Long) {
        sessionStore.multiAppDigestWindowMs = windowMs
        _state.update { it.copy(multiAppDigestWindowMs = windowMs) }
    }

    fun onGroupingWindowChanged(millis: Long) {
        val valid = millis.coerceAtLeast(0L)
        updateQueue { it.copy(groupingWindowMillis = valid) }
    }

    fun onMaxMessagesChanged(max: Int) {
        val valid = max.coerceAtLeast(1)
        updateQueue { it.copy(maxMessagesPerAnnouncement = valid) }
    }

    fun onDelayChanged(millis: Long) {
        val valid = millis.coerceAtLeast(0L)
        updateQueue { it.copy(delayBeforeAnnouncementMillis = valid) }
    }

    fun onMinGapChanged(millis: Long) {
        val valid = millis.coerceAtLeast(0L)
        updateQueue { it.copy(minGapBetweenAnnouncementsMillis = valid) }
    }

    fun onPauseBetweenSectionsChanged(millis: Long) {
        val valid = millis.coerceAtLeast(0L)
        viewModelScope.launch {
            settingsRepository.update { settings ->
                settings.copy(
                    format = settings.format.copy(pauseBetweenSectionsMillis = valid),
                )
            }
        }
    }

    fun onExpiryChanged(millis: Long) {
        val valid = millis.coerceAtLeast(60_000L)
        updateQueue { it.copy(expiryMillis = valid) }
    }

    fun onGlobalMaxSizeChanged(max: Int) {
        val valid = max.coerceAtLeast(1)
        updateQueue { it.copy(globalMaxSize = valid) }
    }

    fun onPerAppMaxSizeChanged(max: Int) {
        val valid = max.coerceAtLeast(1)
        updateQueue { it.copy(perAppMaxSize = valid) }
    }

    fun onOverflowPolicyChanged(policy: OverflowPolicy) {
        updateQueue { it.copy(overflowPolicy = policy) }
    }

    fun onRepeatNotificationUpdatesChanged(repeat: Boolean) {
        updateQueue { it.copy(repeatNotificationUpdates = repeat) }
    }

    fun onAudioFocusPolicyChanged(policy: AudioFocusPolicy) {
        viewModelScope.launch {
            settingsRepository.update { current ->
                current.copy(
                    eligibility = current.eligibility.copy(audioFocus = policy),
                )
            }
        }
    }

    fun onToggleSecureScreen(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.update { current ->
                current.copy(
                    security = current.security.copy(secureScreenEnabled = enabled),
                )
            }
        }
    }

    fun onToggleBiometricLock(activity: Activity, enabled: Boolean) {
        if (enabled) {
            if (!biometricAuthManager.canAuthenticate()) {
                _state.update {
                    it.copy(securityErrorMessage = "Device lock (PIN, pattern, password, or biometric) is not set up on this device.")
                }
                return
            }
            biometricAuthManager.authenticate(
                activity = activity,
                title = "Enable Biometric Lock",
                subtitle = "Authenticate to confirm device ownership",
            ) { result ->
                when (result) {
                    is BiometricAuthResult.Success -> {
                        viewModelScope.launch {
                            settingsRepository.update { current ->
                                current.copy(
                                    security = current.security.copy(biometricLockEnabled = true),
                                    history = current.history.copy(requireAuth = true),
                                )
                            }
                        }
                        _state.update { it.copy(securityErrorMessage = null) }
                    }
                    is BiometricAuthResult.Cancelled -> {}
                    is BiometricAuthResult.Failed -> {
                        _state.update { it.copy(securityErrorMessage = result.error) }
                    }
                    is BiometricAuthResult.Unavailable -> {
                        _state.update { it.copy(securityErrorMessage = result.reason) }
                    }
                }
            }
        } else {
            biometricAuthManager.authenticate(
                activity = activity,
                title = "Disable Biometric Lock",
                subtitle = "Authenticate to turn off biometric protection",
            ) { result ->
                when (result) {
                    is BiometricAuthResult.Success, is BiometricAuthResult.Unavailable -> {
                        viewModelScope.launch {
                            settingsRepository.update { current ->
                                current.copy(
                                    security = current.security.copy(biometricLockEnabled = false),
                                    history = current.history.copy(requireAuth = false),
                                )
                            }
                        }
                        _state.update { it.copy(securityErrorMessage = null) }
                    }
                    is BiometricAuthResult.Cancelled -> {}
                    is BiometricAuthResult.Failed -> {
                        _state.update { it.copy(securityErrorMessage = result.error) }
                    }
                }
            }
        }
    }

    fun onClearSecurityErrorMessage() {
        _state.update { it.copy(securityErrorMessage = null) }
    }

    fun onResetToDefaults() {
        viewModelScope.launch {
            settingsRepository.update { current ->
                current.copy(
                    queue = QueueSettings(),
                    eligibility = current.eligibility.copy(audioFocus = AudioFocusPolicy.DUCK),
                )
            }
        }
    }

    private fun updateQueue(transform: (QueueSettings) -> QueueSettings) {
        viewModelScope.launch {
            settingsRepository.update { current ->
                current.copy(queue = transform(current.queue))
            }
        }
    }
}
