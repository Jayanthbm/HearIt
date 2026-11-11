package com.jayanth.hearit.service

import android.content.Context
import com.jayanth.hearit.core.common.model.DigitGroupingStyle
import com.jayanth.hearit.core.common.model.IntelligencePresets
import com.jayanth.hearit.core.common.model.PronunciationRule
import com.jayanth.hearit.core.common.model.ReactionFilterMode
import com.jayanth.hearit.core.common.model.UrlSummaryRule
import org.json.JSONArray
import org.json.JSONObject

/**
 * Minimal persisted session state for the announcement service.
 *
 * Deliberately separate from `:core:datastore` (P09 settings): these are **session/runtime
 * state**, not user preferences, and the service start path needs them synchronously
 * without a DataStore read (ADR-021). It exists because two service behaviours need
 * durable state —
 *  - [isStarted]: the user explicitly started HearIt, so a process recreated by the system
 *    may resume, but a fresh install never announces on its own (architecture §4);
 *  - [isTemporaryMuteActive]: read by [RuntimeStateReader] as the mute override; the real
 *    mute control arrives with the reading controls (P16).
 *
 * Backed by `SharedPreferences` on purpose (synchronous, tiny, service-lifecycle friendly);
 * nothing else may read this file.
 */
class SessionStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    /** True when the user has explicitly started announcements (persisted). */
    var isStarted: Boolean
        get() = prefs.getBoolean(KEY_STARTED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_STARTED, value).apply()
        }

    /** True while temporary mute is active (P16 owns the control that changes it). */
    var isTemporaryMuteActive: Boolean
        get() = prefs.getBoolean(KEY_TEMPORARY_MUTE, false)
        set(value) {
            prefs.edit().putBoolean(KEY_TEMPORARY_MUTE, value).apply()
        }

    /** True if user manually dismissed the home test speech prompt. */
    var isTestSpeechDismissed: Boolean
        get() = prefs.getBoolean(KEY_TEST_SPEECH_DISMISSED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_TEST_SPEECH_DISMISSED, value).apply()
        }

    /** True if notifications should be cleared from status bar after speaking (Idea 39, default true). */
    var isAutoDismissEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_DISMISS, true)
        set(value) {
            prefs.edit().putBoolean(KEY_AUTO_DISMISS, value).apply()
        }

    /** True if service should auto-start on Bluetooth earphones connect and stop on disconnect (Idea 15, default true). */
    var isBluetoothAutoStartEnabled: Boolean
        get() = prefs.getBoolean(KEY_BT_AUTO_START, true)
        set(value) {
            prefs.edit().putBoolean(KEY_BT_AUTO_START, value).apply()
        }

    /** True if shake or flip phone should silence current speech (Feature A, default true). */
    var isShakeToStopEnabled: Boolean
        get() = prefs.getBoolean(KEY_SHAKE_TO_STOP, true)
        set(value) {
            prefs.edit().putBoolean(KEY_SHAKE_TO_STOP, value).apply()
        }

    /** True if physical volume key press should silence current speech (Idea 29, default true). */
    var isVolumeKeyToStopEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOLUME_KEY_TO_STOP, true)
        set(value) {
            prefs.edit().putBoolean(KEY_VOLUME_KEY_TO_STOP, value).apply()
        }

    /** True if speech should hold for 60s when music is manually paused (Idea 35, default true). */
    var isMusicPauseHoldEnabled: Boolean
        get() = prefs.getBoolean(KEY_MUSIC_PAUSE_HOLD, true)
        set(value) {
            prefs.edit().putBoolean(KEY_MUSIC_PAUSE_HOLD, value).apply()
        }

    /** Periodic check-in settings (Idea 43). */
    var isPeriodicCheckinEnabled: Boolean
        get() = prefs.getBoolean(KEY_PERIODIC_CHECKIN_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_PERIODIC_CHECKIN_ENABLED, value).apply()
        }

    var periodicCheckinIntervalMinutes: Int
        get() = prefs.getInt(KEY_PERIODIC_CHECKIN_INTERVAL, 30)
        set(value) {
            prefs.edit().putInt(KEY_PERIODIC_CHECKIN_INTERVAL, value).apply()
        }

    var periodicCheckinAnnounceTime: Boolean
        get() = prefs.getBoolean(KEY_PERIODIC_ANNOUNCE_TIME, true)
        set(value) {
            prefs.edit().putBoolean(KEY_PERIODIC_ANNOUNCE_TIME, value).apply()
        }

    var periodicCheckinAnnounceBattery: Boolean
        get() = prefs.getBoolean(KEY_PERIODIC_ANNOUNCE_BATTERY, true)
        set(value) {
            prefs.edit().putBoolean(KEY_PERIODIC_ANNOUNCE_BATTERY, value).apply()
        }

    /** Smart NLP Cleaning preferences (Phase 2). */
    var isUrlSummarizerEnabled: Boolean
        get() = prefs.getBoolean(KEY_URL_SUMMARIZER, true)
        set(value) {
            prefs.edit().putBoolean(KEY_URL_SUMMARIZER, value).apply()
        }

    var isCurrencyNormalizerEnabled: Boolean
        get() = prefs.getBoolean(KEY_CURRENCY_NORMALIZER, true)
        set(value) {
            prefs.edit().putBoolean(KEY_CURRENCY_NORMALIZER, value).apply()
        }

    var isMarkdownStripperEnabled: Boolean
        get() = prefs.getBoolean(KEY_MARKDOWN_STRIPPER, true)
        set(value) {
            prefs.edit().putBoolean(KEY_MARKDOWN_STRIPPER, value).apply()
        }

    var isEmojiCollapsingEnabled: Boolean
        get() = prefs.getBoolean(KEY_EMOJI_COLLAPSING, true)
        set(value) {
            prefs.edit().putBoolean(KEY_EMOJI_COLLAPSING, value).apply()
        }

    var isReactionFilterEnabled: Boolean
        get() = prefs.getBoolean(KEY_REACTION_FILTER, true)
        set(value) {
            prefs.edit().putBoolean(KEY_REACTION_FILTER, value).apply()
        }

    var isBotSenderCleaningEnabled: Boolean
        get() = prefs.getBoolean(KEY_BOT_SENDER_CLEANING, true)
        set(value) {
            prefs.edit().putBoolean(KEY_BOT_SENDER_CLEANING, value).apply()
        }

    var reactionFilterMode: ReactionFilterMode
        get() = prefs.getString(KEY_REACTION_FILTER_MODE, ReactionFilterMode.CONDENSE.name)
            ?.let { raw -> ReactionFilterMode.entries.firstOrNull { it.name == raw } }
            ?: ReactionFilterMode.CONDENSE
        set(value) {
            prefs.edit().putString(KEY_REACTION_FILTER_MODE, value.name).apply()
        }

    var isPronunciationDictionaryEnabled: Boolean
        get() = prefs.getBoolean(KEY_PRONUNCIATION_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_PRONUNCIATION_ENABLED, value).apply()
        }

    var isPronunciationOnSenderEnabled: Boolean
        get() = prefs.getBoolean(KEY_PRONUNCIATION_ON_SENDER, false)
        set(value) {
            prefs.edit().putBoolean(KEY_PRONUNCIATION_ON_SENDER, value).apply()
        }

    var pronunciationRules: List<PronunciationRule>
        get() {
            val raw = prefs.getString(KEY_PRONUNCIATION_RULES, null) ?: return emptyList()
            return decodePronunciationRules(raw)
        }
        set(value) {
            prefs.edit().putString(KEY_PRONUNCIATION_RULES, encodePronunciationRules(value)).apply()
        }

    var urlSummaryRules: List<UrlSummaryRule>
        get() {
            val raw = prefs.getString(KEY_URL_SUMMARY_RULES, null)
                ?: return IntelligencePresets.DEFAULT_URL_SUMMARY_RULES
            return decodeUrlSummaryRules(raw)
        }
        set(value) {
            prefs.edit().putString(KEY_URL_SUMMARY_RULES, encodeUrlSummaryRules(value)).apply()
        }

    var isDigitFormattingEnabled: Boolean
        get() = prefs.getBoolean(KEY_DIGIT_FORMATTING_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_DIGIT_FORMATTING_ENABLED, value).apply()
        }

    var isFormatPhoneNumbersEnabled: Boolean
        get() = prefs.getBoolean(KEY_FORMAT_PHONE_NUMBERS, true)
        set(value) {
            prefs.edit().putBoolean(KEY_FORMAT_PHONE_NUMBERS, value).apply()
        }

    var isFormatOtpCodesEnabled: Boolean
        get() = prefs.getBoolean(KEY_FORMAT_OTP_CODES, true)
        set(value) {
            prefs.edit().putBoolean(KEY_FORMAT_OTP_CODES, value).apply()
        }

    var digitGroupingStyle: DigitGroupingStyle
        get() = prefs.getString(KEY_DIGIT_GROUPING_STYLE, DigitGroupingStyle.CHUNKS_OF_3.name)
            ?.let { raw -> DigitGroupingStyle.entries.firstOrNull { it.name == raw } }
            ?: DigitGroupingStyle.CHUNKS_OF_3
        set(value) {
            prefs.edit().putString(KEY_DIGIT_GROUPING_STYLE, value.name).apply()
        }

    var isMessagingIntelligenceEnabled: Boolean
        get() = prefs.getBoolean(KEY_MESSAGING_INTELLIGENCE, true)
        set(value) {
            prefs.edit().putBoolean(KEY_MESSAGING_INTELLIGENCE, value).apply()
        }

    var isSmsBankCleanerEnabled: Boolean
        get() = prefs.getBoolean(KEY_SMS_BANK_CLEANER, true)
        set(value) {
            prefs.edit().putBoolean(KEY_SMS_BANK_CLEANER, value).apply()
        }

    var isCarrierSpamFilterEnabled: Boolean
        get() = prefs.getBoolean(KEY_CARRIER_SPAM_FILTER, true)
        set(value) {
            prefs.edit().putBoolean(KEY_CARRIER_SPAM_FILTER, value).apply()
        }

    var carrierSpamPhrases: List<String>
        get() {
            val raw = prefs.getString(KEY_CARRIER_SPAM_PHRASES, null)
                ?: return IntelligencePresets.DEFAULT_CARRIER_SPAM_PHRASES
            return decodeStringList(raw)
        }
        set(value) {
            prefs.edit().putString(KEY_CARRIER_SPAM_PHRASES, encodeStringList(value)).apply()
        }

    var isEmailCleanupEnabled: Boolean
        get() = prefs.getBoolean(KEY_EMAIL_CLEANUP, true)
        set(value) {
            prefs.edit().putBoolean(KEY_EMAIL_CLEANUP, value).apply()
        }

    var isContactSanitizerEnabled: Boolean
        get() = prefs.getBoolean(KEY_CONTACT_SANITIZER, true)
        set(value) {
            prefs.edit().putBoolean(KEY_CONTACT_SANITIZER, value).apply()
        }

    var isStripSenderEmojisEnabled: Boolean
        get() = prefs.getBoolean(KEY_STRIP_SENDER_EMOJIS, true)
        set(value) {
            prefs.edit().putBoolean(KEY_STRIP_SENDER_EMOJIS, value).apply()
        }

    var isStripSenderBracketsEnabled: Boolean
        get() = prefs.getBoolean(KEY_STRIP_SENDER_BRACKETS, true)
        set(value) {
            prefs.edit().putBoolean(KEY_STRIP_SENDER_BRACKETS, value).apply()
        }

    /** Smart Audio Polish & Voice Boost settings (#17, #18, #24, #34). */
    var isEarconEnabled: Boolean
        get() = prefs.getBoolean(KEY_EARCON_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_EARCON_ENABLED, value).apply()
        }

    var earconDurationMs: Int
        get() = prefs.getInt(KEY_EARCON_DURATION_MS, 120)
        set(value) {
            prefs.edit().putInt(KEY_EARCON_DURATION_MS, value.coerceIn(50, 500)).apply()
        }

    var antiCollisionDelayMs: Long
        get() = prefs.getLong(KEY_ANTI_COLLISION_DELAY_MS, 300L)
        set(value) {
            prefs.edit().putLong(KEY_ANTI_COLLISION_DELAY_MS, value.coerceIn(0L, 1500L)).apply()
        }

    var isAdaptiveSpeedEnabled: Boolean
        get() = prefs.getBoolean(KEY_ADAPTIVE_SPEED_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_ADAPTIVE_SPEED_ENABLED, value).apply()
        }

    var adaptiveSpeedTier1Multiplier: Float
        get() = prefs.getFloat(KEY_ADAPTIVE_SPEED_TIER_1_MULTIPLIER, 1.15f)
        set(value) {
            prefs.edit().putFloat(KEY_ADAPTIVE_SPEED_TIER_1_MULTIPLIER, value.coerceIn(1.0f, 2.0f)).apply()
        }

    var adaptiveSpeedTier2Multiplier: Float
        get() = prefs.getFloat(KEY_ADAPTIVE_SPEED_TIER_2_MULTIPLIER, 1.25f)
        set(value) {
            prefs.edit().putFloat(KEY_ADAPTIVE_SPEED_TIER_2_MULTIPLIER, value.coerceIn(1.0f, 2.0f)).apply()
        }

    var adaptiveSpeedTier3Multiplier: Float
        get() = prefs.getFloat(KEY_ADAPTIVE_SPEED_TIER_3_MULTIPLIER, 1.40f)
        set(value) {
            prefs.edit().putFloat(KEY_ADAPTIVE_SPEED_TIER_3_MULTIPLIER, value.coerceIn(1.0f, 2.0f)).apply()
        }

    var isMonoFoldDownEnabled: Boolean
        get() = prefs.getBoolean(KEY_MONO_FOLD_DOWN, true)
        set(value) {
            prefs.edit().putBoolean(KEY_MONO_FOLD_DOWN, value).apply()
        }

    var isVoiceBoostEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOICE_BOOST_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_VOICE_BOOST_ENABLED, value).apply()
        }

    var voiceBoostPercent: Int
        get() = prefs.getInt(KEY_VOICE_BOOST_PERCENT, 20)
        set(value) {
            prefs.edit().putInt(KEY_VOICE_BOOST_PERCENT, value.coerceIn(5, 50)).apply()
        }

    var isAmbientNoiseBoostEnabled: Boolean
        get() = prefs.getBoolean(KEY_AMBIENT_NOISE_BOOST_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_AMBIENT_NOISE_BOOST_ENABLED, value).apply()
        }

    var ambientNoiseThresholdDb: Int
        get() = prefs.getInt(KEY_AMBIENT_NOISE_THRESHOLD_DB, 70)
        set(value) {
            prefs.edit().putInt(KEY_AMBIENT_NOISE_THRESHOLD_DB, value.coerceIn(55, 85)).apply()
        }

    var ambientNoiseBoostPercent: Int
        get() = prefs.getInt(KEY_AMBIENT_NOISE_BOOST_PERCENT, 20)
        set(value) {
            prefs.edit().putInt(KEY_AMBIENT_NOISE_BOOST_PERCENT, value.coerceIn(5, 50)).apply()
        }

    /** Audio Ducking Level & Return Delay (#17). */
    var isDuckingLevelControlEnabled: Boolean
        get() = prefs.getBoolean(KEY_DUCKING_LEVEL_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_DUCKING_LEVEL_ENABLED, value).apply()
        }

    var duckingVolumePercent: Int
        get() = prefs.getInt(KEY_DUCKING_VOLUME_PERCENT, 50)
        set(value) {
            prefs.edit().putInt(KEY_DUCKING_VOLUME_PERCENT, value.coerceIn(10, 100)).apply()
        }

    var audioFocusReturnDelayMs: Long
        get() = prefs.getLong(KEY_AUDIO_FOCUS_RETURN_DELAY_MS, 500L)
        set(value) {
            prefs.edit().putLong(KEY_AUDIO_FOCUS_RETURN_DELAY_MS, value.coerceIn(0L, 1500L)).apply()
        }

    /** Google Maps & Navigation Yield Priority. */
    var isNavigationYieldEnabled: Boolean
        get() = prefs.getBoolean(KEY_NAVIGATION_YIELD_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_NAVIGATION_YIELD_ENABLED, value).apply()
        }

    var isReannounceAfterNavigationEnabled: Boolean
        get() = prefs.getBoolean(KEY_REANNOUNCE_AFTER_NAV_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_REANNOUNCE_AFTER_NAV_ENABLED, value).apply()
        }

    var navigationYieldSettleDelayMs: Long
        get() = prefs.getLong(KEY_NAVIGATION_SETTLE_DELAY_MS, 2_000L)
        set(value) {
            prefs.edit().putLong(KEY_NAVIGATION_SETTLE_DELAY_MS, value.coerceIn(500L, 5_000L)).apply()
        }

    /** Built-in Automatic Feature Configurations (#19). */
    var isAppNameDedupEnabled: Boolean
        get() = prefs.getBoolean(KEY_APP_NAME_DEDUP_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_APP_NAME_DEDUP_ENABLED, value).apply()
        }

    var appNameDedupWindowMs: Long
        get() = prefs.getLong(KEY_APP_NAME_DEDUP_WINDOW_MS, 45_000L)
        set(value) {
            prefs.edit().putLong(KEY_APP_NAME_DEDUP_WINDOW_MS, value.coerceIn(5_000L, 300_000L)).apply()
        }

    var isDelayedTimestampTagEnabled: Boolean
        get() = prefs.getBoolean(KEY_DELAYED_TIMESTAMP_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_DELAYED_TIMESTAMP_ENABLED, value).apply()
        }

    var delayedTimestampThresholdMs: Long
        get() = prefs.getLong(KEY_DELAYED_TIMESTAMP_THRESHOLD_MS, 120_000L)
        set(value) {
            prefs.edit().putLong(KEY_DELAYED_TIMESTAMP_THRESHOLD_MS, value.coerceIn(30_000L, 600_000L)).apply()
        }

    var musicPauseHoldDurationMs: Long
        get() = prefs.getLong(KEY_MUSIC_PAUSE_HOLD_DURATION_MS, 60_000L)
        set(value) {
            prefs.edit().putLong(KEY_MUSIC_PAUSE_HOLD_DURATION_MS, value.coerceIn(10_000L, 300_000L)).apply()
        }

    var isFlipToStopEnabled: Boolean
        get() = prefs.getBoolean(KEY_FLIP_TO_STOP, isShakeToStopEnabled)
        set(value) {
            prefs.edit().putBoolean(KEY_FLIP_TO_STOP, value).apply()
        }

    var isShakeToRepeatEnabled: Boolean
        get() = prefs.getBoolean(KEY_SHAKE_TO_REPEAT, isShakeToStopEnabled)
        set(value) {
            prefs.edit().putBoolean(KEY_SHAKE_TO_REPEAT, value).apply()
        }

    var shakeRepeatWindowMs: Long
        get() = prefs.getLong(KEY_SHAKE_REPEAT_WINDOW_MS, 10_000L)
        set(value) {
            prefs.edit().putLong(KEY_SHAKE_REPEAT_WINDOW_MS, value.coerceIn(3_000L, 30_000L)).apply()
        }

    /** Catch-Up Summary on Connect (#22). */
    var isCatchUpSummaryEnabled: Boolean
        get() = prefs.getBoolean(KEY_CATCH_UP_SUMMARY_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_CATCH_UP_SUMMARY_ENABLED, value).apply()
        }

    var catchUpAbsenceWindowMs: Long
        get() = prefs.getLong(KEY_CATCH_UP_ABSENCE_WINDOW_MS, 15 * 60_000L)
        set(value) {
            prefs.edit().putLong(KEY_CATCH_UP_ABSENCE_WINDOW_MS, value.coerceAtLeast(0L)).apply()
        }

    var catchUpIncludeAppBreakdown: Boolean
        get() = prefs.getBoolean(KEY_CATCH_UP_INCLUDE_APPS, true)
        set(value) {
            prefs.edit().putBoolean(KEY_CATCH_UP_INCLUDE_APPS, value).apply()
        }

    var lastHeadphoneDisconnectedAtMillis: Long
        get() = prefs.getLong(KEY_LAST_HEADPHONE_DISCONNECTED_AT, 0L)
        set(value) {
            prefs.edit().putLong(KEY_LAST_HEADPHONE_DISCONNECTED_AT, value).apply()
        }

    /** Separate Bluetooth Auto-Stop & Allowed Devices Filter (#27). */
    var isBluetoothAutoStopEnabled: Boolean
        get() = prefs.getBoolean(KEY_BT_AUTO_STOP, isBluetoothAutoStartEnabled)
        set(value) {
            prefs.edit().putBoolean(KEY_BT_AUTO_STOP, value).apply()
        }

    var isBluetoothAutoOnlyAllowedEnabled: Boolean
        get() = prefs.getBoolean(KEY_BT_AUTO_ONLY_ALLOWED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_BT_AUTO_ONLY_ALLOWED, value).apply()
        }

    var cachedAllowedBluetoothIds: Set<String>
        get() = prefs.getStringSet(KEY_CACHED_ALLOWED_BT_IDS, emptySet())?.toSet() ?: emptySet()
        set(value) {
            prefs.edit().putStringSet(KEY_CACHED_ALLOWED_BT_IDS, value).apply()
        }

    /** Periodic Spoken Check-in additional items (#32). */
    var periodicCheckinAnnounceEarphonesBattery: Boolean
        get() = prefs.getBoolean(KEY_PERIODIC_ANNOUNCE_EARPHONES_BATTERY, true)
        set(value) {
            prefs.edit().putBoolean(KEY_PERIODIC_ANNOUNCE_EARPHONES_BATTERY, value).apply()
        }

    var periodicCheckinAnnounceSummary: Boolean
        get() = prefs.getBoolean(KEY_PERIODIC_ANNOUNCE_SUMMARY, true)
        set(value) {
            prefs.edit().putBoolean(KEY_PERIODIC_ANNOUNCE_SUMMARY, value).apply()
        }

    /** Sender Burst Aggregator (#20 / Idea 2). */
    var isSenderBurstAggregatorEnabled: Boolean
        get() = prefs.getBoolean(KEY_SENDER_BURST_AGGREGATOR, true)
        set(value) {
            prefs.edit().putBoolean(KEY_SENDER_BURST_AGGREGATOR, value).apply()
        }

    var senderBurstMinCount: Int
        get() = prefs.getInt(KEY_SENDER_BURST_MIN_COUNT, 2)
        set(value) {
            prefs.edit().putInt(KEY_SENDER_BURST_MIN_COUNT, value.coerceIn(2, 6)).apply()
        }

    var senderBurstWindowMs: Long
        get() = prefs.getLong(KEY_SENDER_BURST_WINDOW_MS, 5_000L)
        set(value) {
            prefs.edit().putLong(KEY_SENDER_BURST_WINDOW_MS, value.coerceIn(2_000L, 15_000L)).apply()
        }

    /** Tap-to-Repeat Home & Notification shortcut (#25 / Idea 7). */
    var isRepeatLastShortcutEnabled: Boolean
        get() = prefs.getBoolean(KEY_REPEAT_LAST_SHORTCUT, true)
        set(value) {
            prefs.edit().putBoolean(KEY_REPEAT_LAST_SHORTCUT, value).apply()
        }

    /** Per-Sender Spam Cooldown Throttle (#28 / Idea 16). */
    var isSpamCooldownEnabled: Boolean
        get() = prefs.getBoolean(KEY_SPAM_COOLDOWN_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_SPAM_COOLDOWN_ENABLED, value).apply()
        }

    var spamCooldownMaxMessages: Int
        get() = prefs.getInt(KEY_SPAM_COOLDOWN_MAX_MESSAGES, 3)
        set(value) {
            prefs.edit().putInt(KEY_SPAM_COOLDOWN_MAX_MESSAGES, value.coerceIn(2, 10)).apply()
        }

    var spamCooldownWindowMs: Long
        get() = prefs.getLong(KEY_SPAM_COOLDOWN_WINDOW_MS, 60_000L)
        set(value) {
            prefs.edit().putLong(KEY_SPAM_COOLDOWN_WINDOW_MS, value.coerceIn(15_000L, 180_000L)).apply()
        }

    /** Multi-App Smart Batching Digest (#33 / Idea 53). */
    var isMultiAppDigestEnabled: Boolean
        get() = prefs.getBoolean(KEY_MULTI_APP_DIGEST_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_MULTI_APP_DIGEST_ENABLED, value).apply()
        }

    var multiAppDigestWindowMs: Long
        get() = prefs.getLong(KEY_MULTI_APP_DIGEST_WINDOW_MS, 2_500L)
        set(value) {
            prefs.edit().putLong(KEY_MULTI_APP_DIGEST_WINDOW_MS, value.coerceIn(1_500L, 5_000L)).apply()
        }

    /** Batch 6 (#15): Tier 2 Local AI toggles. */
    var isMlKitEntityExtractionEnabled: Boolean
        get() = prefs.getBoolean(KEY_MLKIT_ENTITY_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_MLKIT_ENTITY_ENABLED, value).apply()
        }

    var isAiCoreSummarizationEnabled: Boolean
        get() = prefs.getBoolean(KEY_AICORE_SUMMARIZATION_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_AICORE_SUMMARIZATION_ENABLED, value).apply()
        }

    // ---- Batch 7 (#35-#36): Wear OS Companion Settings (Max 4 Watch Apps, Option A) ----

    var isWatchForwardingEnabled: Boolean
        get() = prefs.getBoolean(KEY_WATCH_FORWARDING_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_WATCH_FORWARDING_ENABLED, value).apply()
        }

    var isWatchHapticsEnabled: Boolean
        get() = prefs.getBoolean(KEY_WATCH_HAPTICS_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_WATCH_HAPTICS_ENABLED, value).apply()
        }

    var watchFilterApps: List<String>
        get() = com.jayanth.hearit.wear.WearConfigHelper.normalizeWatchApps(
            decodeStringList(prefs.getString(KEY_WATCH_FILTER_APPS, null) ?: "[]"),
        )
        set(value) {
            val capped = com.jayanth.hearit.wear.WearConfigHelper.normalizeWatchApps(value)
            prefs.edit().putString(KEY_WATCH_FILTER_APPS, encodeStringList(capped)).apply()
        }

    /** True if user has permanent premium entitlement (Google Play IAP/Subscription). */
    var isPremiumEntitled: Boolean
        get() = prefs.getBoolean(KEY_PREMIUM_ENTITLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_PREMIUM_ENTITLED, value).apply()
        }

    /** Expiry timestamp of temporary premium pass earned by watching a rewarded ad. */
    var temporaryPremiumExpiryTimestamp: Long
        get() = prefs.getLong(KEY_TEMPORARY_PREMIUM_EXPIRY, 0L)
        set(value) {
            prefs.edit().putLong(KEY_TEMPORARY_PREMIUM_EXPIRY, value).apply()
        }

    /** Returns true if user has active permanent premium or an unexpired temporary pass. */
    val isPremiumActive: Boolean
        get() = isPremiumEntitled || temporaryPremiumExpiryTimestamp > System.currentTimeMillis()

    /** Grants hours of temporary premium pass, stacking up to maxStackHours. */
    fun grantTemporaryPremium(hours: Int = 12, maxStackHours: Int = 24) {
        val now = System.currentTimeMillis()
        val addMs = hours * 3600 * 1000L
        val maxMs = maxStackHours * 3600 * 1000L
        val currentExpiry = temporaryPremiumExpiryTimestamp
        val baseTime = if (currentExpiry > now) currentExpiry else now
        val newExpiry = Math.min(baseTime + addMs, now + maxMs)
        temporaryPremiumExpiryTimestamp = newExpiry
    }

    private fun encodePronunciationRules(rules: List<PronunciationRule>): String =
        JSONArray().apply {
            rules.forEach { r ->
                put(
                    JSONObject().apply {
                        put("match", r.match)
                        put("spokenAs", r.spokenAs)
                        put("matchCase", r.matchCase)
                        put("wholeWord", r.wholeWord)
                        put("enabled", r.enabled)
                    },
                )
            }
        }.toString()

    private fun decodePronunciationRules(raw: String): List<PronunciationRule> = try {
        val arr = JSONArray(raw)
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            PronunciationRule(
                match = o.optString("match", ""),
                spokenAs = o.optString("spokenAs", ""),
                matchCase = o.optBoolean("matchCase", false),
                wholeWord = o.optBoolean("wholeWord", true),
                enabled = o.optBoolean("enabled", true),
            )
        }.filter { it.match.isNotBlank() }
    } catch (_: Throwable) {
        emptyList()
    }

    private fun encodeUrlSummaryRules(rules: List<UrlSummaryRule>): String =
        JSONArray().apply {
            rules.forEach { r ->
                put(
                    JSONObject().apply {
                        put("pattern", r.pattern)
                        put("spokenPhrase", r.spokenPhrase)
                        put("isPredefined", r.isPredefined)
                        put("enabled", r.enabled)
                    },
                )
            }
        }.toString()

    private fun decodeUrlSummaryRules(raw: String): List<UrlSummaryRule> = try {
        val arr = JSONArray(raw)
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            UrlSummaryRule(
                pattern = o.optString("pattern", ""),
                spokenPhrase = o.optString("spokenPhrase", ""),
                isPredefined = o.optBoolean("isPredefined", false),
                enabled = o.optBoolean("enabled", true),
            )
        }.filter { it.pattern.isNotBlank() && it.spokenPhrase.isNotBlank() }
    } catch (_: Throwable) {
        IntelligencePresets.DEFAULT_URL_SUMMARY_RULES
    }

    private fun encodeStringList(items: List<String>): String =
        JSONArray().apply { items.forEach { put(it) } }.toString()

    private fun decodeStringList(raw: String): List<String> = try {
        val arr = JSONArray(raw)
        List(arr.length()) { i -> arr.optString(i, "") }.filter { it.isNotBlank() }
    } catch (_: Throwable) {
        emptyList()
    }

    private companion object {
        const val FILE_NAME = "hearit_session"
        const val KEY_STARTED = "started"
        const val KEY_TEMPORARY_MUTE = "temporary_mute"
        const val KEY_TEST_SPEECH_DISMISSED = "test_speech_dismissed"
        const val KEY_AUTO_DISMISS = "auto_dismiss"
        const val KEY_BT_AUTO_START = "bt_auto_start"
        const val KEY_SHAKE_TO_STOP = "shake_to_stop"
        const val KEY_VOLUME_KEY_TO_STOP = "volume_key_to_stop"
        const val KEY_MUSIC_PAUSE_HOLD = "music_pause_hold"
        const val KEY_PERIODIC_CHECKIN_ENABLED = "periodic_checkin_enabled"
        const val KEY_PERIODIC_CHECKIN_INTERVAL = "periodic_checkin_interval"
        const val KEY_PERIODIC_ANNOUNCE_TIME = "periodic_announce_time"
        const val KEY_PERIODIC_ANNOUNCE_BATTERY = "periodic_announce_battery"
        const val KEY_URL_SUMMARIZER = "url_summarizer"
        const val KEY_CURRENCY_NORMALIZER = "currency_normalizer"
        const val KEY_MARKDOWN_STRIPPER = "markdown_stripper"
        const val KEY_EMOJI_COLLAPSING = "emoji_collapsing"
        const val KEY_REACTION_FILTER = "reaction_filter"
        const val KEY_REACTION_FILTER_MODE = "reaction_filter_mode"
        const val KEY_BOT_SENDER_CLEANING = "bot_sender_cleaning"
        const val KEY_PRONUNCIATION_ENABLED = "pronunciation_enabled"
        const val KEY_PRONUNCIATION_ON_SENDER = "pronunciation_on_sender"
        const val KEY_PRONUNCIATION_RULES = "pronunciation_rules_json"
        const val KEY_URL_SUMMARY_RULES = "url_summary_rules_json"
        const val KEY_DIGIT_FORMATTING_ENABLED = "digit_formatting_enabled"
        const val KEY_FORMAT_PHONE_NUMBERS = "format_phone_numbers"
        const val KEY_FORMAT_OTP_CODES = "format_otp_codes"
        const val KEY_DIGIT_GROUPING_STYLE = "digit_grouping_style"
        const val KEY_MESSAGING_INTELLIGENCE = "messaging_intelligence"
        const val KEY_SMS_BANK_CLEANER = "sms_bank_cleaner"
        const val KEY_CARRIER_SPAM_FILTER = "carrier_spam_filter"
        const val KEY_CARRIER_SPAM_PHRASES = "carrier_spam_phrases_json"
        const val KEY_EMAIL_CLEANUP = "email_cleanup"
        const val KEY_CONTACT_SANITIZER = "contact_sanitizer"
        const val KEY_STRIP_SENDER_EMOJIS = "strip_sender_emojis"
        const val KEY_STRIP_SENDER_BRACKETS = "strip_sender_brackets"
        const val KEY_EARCON_ENABLED = "earcon_enabled"
        const val KEY_EARCON_DURATION_MS = "earcon_duration_ms"
        const val KEY_ANTI_COLLISION_DELAY_MS = "anti_collision_delay_ms"
        const val KEY_ADAPTIVE_SPEED_ENABLED = "adaptive_speed_enabled"
        const val KEY_ADAPTIVE_SPEED_TIER_1_MULTIPLIER = "adaptive_speed_tier1_multiplier"
        const val KEY_ADAPTIVE_SPEED_TIER_2_MULTIPLIER = "adaptive_speed_tier2_multiplier"
        const val KEY_ADAPTIVE_SPEED_TIER_3_MULTIPLIER = "adaptive_speed_tier3_multiplier"
        const val KEY_MONO_FOLD_DOWN = "mono_fold_down"
        const val KEY_VOICE_BOOST_ENABLED = "voice_boost_enabled"
        const val KEY_VOICE_BOOST_PERCENT = "voice_boost_percent"
        const val KEY_AMBIENT_NOISE_BOOST_ENABLED = "ambient_noise_boost_enabled"
        const val KEY_AMBIENT_NOISE_THRESHOLD_DB = "ambient_noise_threshold_db"
        const val KEY_AMBIENT_NOISE_BOOST_PERCENT = "ambient_noise_boost_percent"
        const val KEY_DUCKING_LEVEL_ENABLED = "ducking_level_enabled"
        const val KEY_DUCKING_VOLUME_PERCENT = "ducking_volume_percent"
        const val KEY_AUDIO_FOCUS_RETURN_DELAY_MS = "audio_focus_return_delay_ms"
        const val KEY_NAVIGATION_YIELD_ENABLED = "navigation_yield_enabled"
        const val KEY_REANNOUNCE_AFTER_NAV_ENABLED = "reannounce_after_nav_enabled"
        const val KEY_NAVIGATION_SETTLE_DELAY_MS = "navigation_settle_delay_ms"
        const val KEY_APP_NAME_DEDUP_ENABLED = "app_name_dedup_enabled"
        const val KEY_APP_NAME_DEDUP_WINDOW_MS = "app_name_dedup_window_ms"
        const val KEY_DELAYED_TIMESTAMP_ENABLED = "delayed_timestamp_enabled"
        const val KEY_DELAYED_TIMESTAMP_THRESHOLD_MS = "delayed_timestamp_threshold_ms"
        const val KEY_MUSIC_PAUSE_HOLD_DURATION_MS = "music_pause_hold_duration_ms"
        const val KEY_FLIP_TO_STOP = "flip_to_stop"
        const val KEY_SHAKE_TO_REPEAT = "shake_to_repeat"
        const val KEY_SHAKE_REPEAT_WINDOW_MS = "shake_repeat_window_ms"
        const val KEY_CATCH_UP_SUMMARY_ENABLED = "catch_up_summary_enabled"
        const val KEY_CATCH_UP_ABSENCE_WINDOW_MS = "catch_up_absence_window_ms"
        const val KEY_CATCH_UP_INCLUDE_APPS = "catch_up_include_apps"
        const val KEY_LAST_HEADPHONE_DISCONNECTED_AT = "last_headphone_disconnected_at"
        const val KEY_BT_AUTO_STOP = "bt_auto_stop"
        const val KEY_BT_AUTO_ONLY_ALLOWED = "bt_auto_only_allowed"
        const val KEY_CACHED_ALLOWED_BT_IDS = "cached_allowed_bt_ids"
        const val KEY_PERIODIC_ANNOUNCE_EARPHONES_BATTERY = "periodic_announce_earphones_battery"
        const val KEY_PERIODIC_ANNOUNCE_SUMMARY = "periodic_announce_summary"
        const val KEY_SENDER_BURST_AGGREGATOR = "sender_burst_aggregator"
        const val KEY_SENDER_BURST_MIN_COUNT = "sender_burst_min_count"
        const val KEY_SENDER_BURST_WINDOW_MS = "sender_burst_window_ms"
        const val KEY_REPEAT_LAST_SHORTCUT = "repeat_last_shortcut"
        const val KEY_SPAM_COOLDOWN_ENABLED = "spam_cooldown_enabled"
        const val KEY_SPAM_COOLDOWN_MAX_MESSAGES = "spam_cooldown_max_messages"
        const val KEY_SPAM_COOLDOWN_WINDOW_MS = "spam_cooldown_window_ms"
        const val KEY_MULTI_APP_DIGEST_ENABLED = "multi_app_digest_enabled"
        const val KEY_MULTI_APP_DIGEST_WINDOW_MS = "multi_app_digest_window_ms"
        const val KEY_MLKIT_ENTITY_ENABLED = "mlkit_entity_enabled"
        const val KEY_AICORE_SUMMARIZATION_ENABLED = "aicore_summarization_enabled"
        const val KEY_WATCH_FORWARDING_ENABLED = "watch_forwarding_enabled"
        const val KEY_WATCH_HAPTICS_ENABLED = "watch_haptics_enabled"
        const val KEY_WATCH_FILTER_APPS = "watch_filter_apps_json"
        const val KEY_PREMIUM_ENTITLED = "premium_entitled"
        const val KEY_TEMPORARY_PREMIUM_EXPIRY = "temporary_premium_expiry_ts"
    }
}
