package com.jayanth.hearit.service

import android.content.Context

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
        const val KEY_BOT_SENDER_CLEANING = "bot_sender_cleaning"
        const val KEY_PREMIUM_ENTITLED = "premium_entitled"
        const val KEY_TEMPORARY_PREMIUM_EXPIRY = "temporary_premium_expiry_ts"
    }
}
