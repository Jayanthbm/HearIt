package com.jayanth.hearit.settings

import com.jayanth.hearit.applications.AppEntry
import com.jayanth.hearit.core.common.model.AppCategory
import com.jayanth.hearit.core.common.model.AudioFocusPolicy
import com.jayanth.hearit.core.common.model.DigitGroupingStyle
import com.jayanth.hearit.core.common.model.HeadsetButtonAction
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
    // Audio Ducking Level Control (#17)
    val isDuckingLevelControlEnabled: Boolean = true,
    val duckingVolumePercent: Int = 50,
    val audioFocusReturnDelayMs: Long = 500L,
    // Bluetooth Headset MediaSession Controls (#1)
    val isHeadsetMediaControlsEnabled: Boolean = true,
    val headsetSinglePressAction: HeadsetButtonAction = HeadsetButtonAction.PAUSE_RESUME,
    val headsetDoublePressAction: HeadsetButtonAction = HeadsetButtonAction.SKIP,
    val headsetTriplePressAction: HeadsetButtonAction = HeadsetButtonAction.REPEAT,
)
