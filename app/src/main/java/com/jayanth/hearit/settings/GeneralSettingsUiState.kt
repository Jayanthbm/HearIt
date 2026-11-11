package com.jayanth.hearit.settings

import com.jayanth.hearit.applications.AppEntry
import com.jayanth.hearit.core.common.model.AppCategory
import com.jayanth.hearit.core.common.model.AudioFocusPolicy
import com.jayanth.hearit.core.common.model.DigitGroupingStyle
import com.jayanth.hearit.core.common.model.EligibilitySettings
import com.jayanth.hearit.core.common.model.IntelligencePresets
import com.jayanth.hearit.core.common.model.PronunciationRule
import com.jayanth.hearit.core.common.model.QueueSettings
import com.jayanth.hearit.core.common.model.ReactionFilterMode
import com.jayanth.hearit.core.common.model.SecuritySettings
import com.jayanth.hearit.core.common.model.UrlSummaryRule

/**
 * UI State for General Settings & Active Intelligence controls.
 */
data class GeneralSettingsUiState(
    val queue: QueueSettings = QueueSettings(),
    val audioFocus: AudioFocusPolicy = AudioFocusPolicy.DUCK,
    val isServiceNotificationVisible: Boolean = true,
    val security: SecuritySettings = SecuritySettings(),
    val isBiometricAvailable: Boolean = false,
    val securityErrorMessage: String? = null,
    val isAutoDismissEnabled: Boolean = true,
    val isShakeToStopEnabled: Boolean = true,
    val isVolumeKeyToStopEnabled: Boolean = true,
    val isMusicPauseHoldEnabled: Boolean = true,
    val isBluetoothAutoStartEnabled: Boolean = true,
    val isPeriodicCheckinEnabled: Boolean = false,
    val periodicCheckinIntervalMinutes: Int = 30,
    val periodicCheckinAnnounceTime: Boolean = true,
    val periodicCheckinAnnounceBattery: Boolean = true,
    val isUrlSummarizerEnabled: Boolean = true,
    val urlSummaryRules: List<UrlSummaryRule> = IntelligencePresets.DEFAULT_URL_SUMMARY_RULES,
    val isCurrencyNormalizerEnabled: Boolean = true,
    val isMarkdownStripperEnabled: Boolean = true,
    val isEmojiCollapsingEnabled: Boolean = true,
    val isReactionFilterEnabled: Boolean = true,
    val reactionFilterMode: ReactionFilterMode = ReactionFilterMode.CONDENSE,
    val isBotSenderCleaningEnabled: Boolean = true,
    val isPronunciationDictionaryEnabled: Boolean = true,
    val isPronunciationOnSenderEnabled: Boolean = false,
    val pronunciationRules: List<PronunciationRule> = emptyList(),
    val isDigitFormattingEnabled: Boolean = true,
    val isFormatPhoneNumbersEnabled: Boolean = true,
    val isFormatOtpCodesEnabled: Boolean = true,
    val digitGroupingStyle: DigitGroupingStyle = DigitGroupingStyle.CHUNKS_OF_3,
    val isMessagingIntelligenceEnabled: Boolean = true,
    val isSmsBankCleanerEnabled: Boolean = true,
    val isCarrierSpamFilterEnabled: Boolean = true,
    val carrierSpamPhrases: List<String> = IntelligencePresets.DEFAULT_CARRIER_SPAM_PHRASES,
    val isEmailCleanupEnabled: Boolean = true,
    val isContactSanitizerEnabled: Boolean = true,
    val isStripSenderEmojisEnabled: Boolean = true,
    val isStripSenderBracketsEnabled: Boolean = true,
    val installedApps: List<AppEntry> = emptyList(),
    val appCategories: Map<String, AppCategory?> = emptyMap(),
    // Audio Ducking Level Control (#17) & Maps Navigation Yield
    val isDuckingLevelControlEnabled: Boolean = true,
    val duckingVolumePercent: Int = 50,
    val audioFocusReturnDelayMs: Long = 500L,
    val isNavigationYieldEnabled: Boolean = true,
    val isReannounceAfterNavigationEnabled: Boolean = true,
    val navigationYieldSettleDelayMs: Long = 2_000L,
    // Batch 3: Automatic behavior configs (#19), Catch-Up (#22), BT Filtering (#27), Check-in (#32)
    val isAppNameDedupEnabled: Boolean = true,
    val appNameDedupWindowMs: Long = 45_000L,
    val isDelayedTimestampTagEnabled: Boolean = true,
    val delayedTimestampThresholdMs: Long = 120_000L,
    val musicPauseHoldDurationMs: Long = 60_000L,
    val isFlipToStopEnabled: Boolean = true,
    val isShakeToRepeatEnabled: Boolean = true,
    val shakeRepeatWindowMs: Long = 10_000L,
    val isCatchUpSummaryEnabled: Boolean = true,
    val catchUpAbsenceWindowMs: Long = 15 * 60_000L,
    val catchUpIncludeAppBreakdown: Boolean = true,
    val isBluetoothAutoStopEnabled: Boolean = true,
    val isBluetoothAutoOnlyAllowedEnabled: Boolean = false,
    val periodicCheckinAnnounceEarphonesBattery: Boolean = true,
    val periodicCheckinAnnounceSummary: Boolean = true,
    val eligibility: EligibilitySettings = EligibilitySettings(),
    // Batch 4: Bursts (#20), Repeat shortcut (#25), Spam cooldown (#28), Multi-app digest (#33)
    val isSenderBurstAggregatorEnabled: Boolean = true,
    val senderBurstMinCount: Int = 2,
    val senderBurstWindowMs: Long = 5_000L,
    val isRepeatLastShortcutEnabled: Boolean = true,
    val isSpamCooldownEnabled: Boolean = true,
    val spamCooldownMaxMessages: Int = 3,
    val spamCooldownWindowMs: Long = 60_000L,
    val isMultiAppDigestEnabled: Boolean = true,
    val multiAppDigestWindowMs: Long = 2_500L,
    // Batch 6 (#15): Tier 2 Local AI Strategy & Support Status
    val isMlKitEntityExtractionEnabled: Boolean = false,
    val isAiCoreSummarizationEnabled: Boolean = false,
    val mlKitStatusLabel: String = "Checking device support…",
    val mlKitSupported: Boolean = false,
    val mlKitModelReady: Boolean = false,
    val isMlKitDownloading: Boolean = false,
    val aiCoreStatusLabel: String = "Checking Android AICore…",
    val aiCoreSupported: Boolean = false,
)
