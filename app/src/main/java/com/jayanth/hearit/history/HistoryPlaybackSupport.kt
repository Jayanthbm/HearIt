package com.jayanth.hearit.history

import com.jayanth.hearit.core.common.model.LockCondition
import com.jayanth.hearit.core.common.model.ResolvedSettings
import com.jayanth.hearit.core.common.model.RuntimeState
import com.jayanth.hearit.core.common.model.ScreenCondition
import com.jayanth.hearit.core.common.model.SkipReason
import com.jayanth.hearit.core.datastore.HearItSettings
import com.jayanth.hearit.core.tts.SmartContentCleaner
import com.jayanth.hearit.service.SessionStore

/**
 * Applies the user-confirmed bypasses of a blocked manual replay, clearing only the confirmed
 * reason(s) so every other rule is still evaluated afterwards.
 */
internal fun applyPlaybackBypasses(
    resolved: ResolvedSettings,
    runtimeState: RuntimeState,
    bypassedReasons: Set<SkipReason>,
): Pair<ResolvedSettings, RuntimeState> {
    var r = resolved
    var s = runtimeState
    var e = r.eligibility
    if (SkipReason.APP_DISABLED in bypassedReasons) r = r.copy(appEnabled = true)
    if (SkipReason.MUTED in bypassedReasons) s = s.copy(temporaryMuteActive = false)
    if (SkipReason.CALL_ACTIVE in bypassedReasons) s = s.copy(isCallActive = false)
    if (SkipReason.DND in bypassedReasons) {
        s = s.copy(isDndActive = false)
        e = e.copy(respectDnd = false)
    }
    if (SkipReason.SCREEN_ON in bypassedReasons || SkipReason.SCREEN_OFF in bypassedReasons) {
        e = e.copy(screenCondition = ScreenCondition.BOTH)
    }
    if (SkipReason.LOCKED in bypassedReasons || SkipReason.UNLOCKED in bypassedReasons) {
        e = e.copy(lockCondition = LockCondition.BOTH)
    }
    if (SkipReason.TOO_LONG in bypassedReasons) {
        e = e.copy(textLength = e.textLength.copy(maxLengthChars = null))
    }
    if (SkipReason.KEYWORD_FILTERED in bypassedReasons) {
        e = e.copy(keywordFilter = e.keywordFilter.copy(enabled = false))
    }
    if (SkipReason.LOW_BATTERY in bypassedReasons) {
        s = s.copy(isBatteryLowOrSaver = false, batteryPercent = 100, isPowerSave = false)
        e = e.copy(lowBatterySafeguardEnabled = false)
    }
    if (SkipReason.PROXIMITY_UNCOVERED in bypassedReasons) {
        s = s.copy(isProximityCovered = true)
        e = e.copy(pocketDetectionEnabled = false)
    }
    if (SkipReason.OUTPUT_NOT_ALLOWED in bypassedReasons) {
        e = e.copy(
            audioOutput = e.audioOutput.copy(
                allowPhoneSpeaker = true,
                allowWired = true,
                allowBluetooth = true,
                allowUnidentifiedOutput = true,
                allowedBluetoothIds = null,
            ),
        )
        s = s.copy(activeOutput = null)
    }
    if (SkipReason.PRIVACY_EXCLUDED_APP in bypassedReasons ||
        SkipReason.PRIVACY_OTP in bypassedReasons ||
        SkipReason.PRIVACY_PASSWORD in bypassedReasons ||
        SkipReason.PRIVACY_PAYMENT in bypassedReasons ||
        SkipReason.PRIVACY_BANKING in bypassedReasons ||
        SkipReason.PRIVACY_SENSITIVE_KEYWORD in bypassedReasons
    ) {
        val privacy = e.privacy.copy(
            excludeApp = if (SkipReason.PRIVACY_EXCLUDED_APP in bypassedReasons) false else e.privacy.excludeApp,
            skipOtp = if (SkipReason.PRIVACY_OTP in bypassedReasons) false else e.privacy.skipOtp,
            skipPassword = if (SkipReason.PRIVACY_PASSWORD in bypassedReasons) false else e.privacy.skipPassword,
            skipPayment = if (SkipReason.PRIVACY_PAYMENT in bypassedReasons) false else e.privacy.skipPayment,
            skipBanking = if (SkipReason.PRIVACY_BANKING in bypassedReasons) false else e.privacy.skipBanking,
            sensitiveKeywords = if (SkipReason.PRIVACY_SENSITIVE_KEYWORD in bypassedReasons) emptyList() else e.privacy.sensitiveKeywords,
        )
        e = e.copy(privacy = privacy)
    }
    if (e !== r.eligibility) r = r.copy(eligibility = e)
    return r to s
}

/**
 * Aligns [SmartContentCleaner] with the persisted session flags and per-app overrides so a manual
 * replay is cleaned exactly like a live announcement (dictionary, aliases, mappings, noise).
 */
internal fun syncCleanerForReplay(settings: HearItSettings, sessionStore: SessionStore) {
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
    SmartContentCleaner.isSmsBankCleanerEnabled = sessionStore.isSmsBankCleanerEnabled
    SmartContentCleaner.isCarrierSpamFilterEnabled = sessionStore.isCarrierSpamFilterEnabled
    SmartContentCleaner.carrierSpamPhrases = sessionStore.carrierSpamPhrases
    SmartContentCleaner.isEmailCleanupEnabled = sessionStore.isEmailCleanupEnabled
    SmartContentCleaner.isContactSanitizerEnabled = sessionStore.isContactSanitizerEnabled
    SmartContentCleaner.isSkipEmptyNotificationsEnabled = sessionStore.isSkipEmptyNotificationsEnabled

    val aliases = settings.appOverrides.mapNotNull { (pkg, override) ->
        override.spokenName?.takeIf { it.isNotBlank() }?.let { pkg to it }
    }.toMap()
    val categories = settings.appOverrides.mapNotNull { (pkg, override) ->
        override.categoryOverride?.let { pkg to it }
    }.toMap()
    val messagingConfigs = settings.appOverrides.mapNotNull { (pkg, override) ->
        if (!override.messagingPolish.inheritsEveryValue) pkg to override.messagingPolish else null
    }.toMap()
    SmartContentCleaner.replaceAppAliases(aliases)
    SmartContentCleaner.replaceAppCategories(categories)
    SmartContentCleaner.replaceMessagingPolishConfigs(messagingConfigs)
}

/** Human-readable explanation of a blocked manual replay, mirroring the global History screen. */
internal fun playbackBlockMessage(reason: SkipReason, appName: String): String {
    val reasonText = when (reason) {
        SkipReason.APP_DISABLED -> "$appName is disabled for announcements."
        SkipReason.SCREEN_ON -> "Screen is on."
        SkipReason.SCREEN_OFF -> "Screen is off."
        SkipReason.UNLOCKED -> "Device is unlocked."
        SkipReason.LOCKED -> "Device is locked."
        SkipReason.MUTED -> "Announcements are temporarily muted."
        SkipReason.CALL_ACTIVE -> "A call is in progress."
        SkipReason.DND -> "Do Not Disturb is on."
        SkipReason.TOO_LONG -> "Message is too long."
        SkipReason.KEYWORD_FILTERED -> "Filtered by keyword."
        SkipReason.OUTPUT_NOT_ALLOWED -> "Audio output is not allowed."
        SkipReason.PRIVACY_EXCLUDED_APP -> "Excluded by privacy settings."
        SkipReason.PRIVACY_OTP -> "OTP/code content is hidden."
        SkipReason.PRIVACY_PASSWORD -> "Password content is hidden."
        SkipReason.PRIVACY_PAYMENT -> "Payment content is hidden."
        SkipReason.PRIVACY_BANKING -> "Banking content is hidden."
        SkipReason.PRIVACY_SENSITIVE_KEYWORD -> "Sensitive keyword matched."
        SkipReason.LOW_BATTERY -> "Low battery or battery saver is blocking playback."
        SkipReason.PROXIMITY_UNCOVERED -> "Pocket/proximity check blocked playback."
        SkipReason.SUPPRESSED_INTELLIGENCE -> "Filtered by active intelligence."
        else -> "Can't play: ${reason.name.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() }}."
    }
    val note = if (reason == SkipReason.APP_DISABLED) {
        " Still want to play this notification? Other rules and filters will still be checked."
    } else {
        " Play anyway? Other rules and filters will still be checked."
    }
    return reasonText + note
}
