package com.jayanth.hearit.core.common.model

/**
 * Functional app category used to scope intelligent notification cleaning rules
 * (Email vs Messaging vs SMS/Carrier).
 */
enum class AppCategory {
    EMAIL,
    MESSAGING,
    SMS,
}

/**
 * Sparse per-app category assignment stored in [HearItSettings].
 *
 * `null` (absent) uses [DefaultAppCategories.defaultCategoryFor], while [NONE] lets the user
 * explicitly opt a default app (e.g., Gmail) out of its category.
 */
enum class AppCategoryChoice {
    EMAIL,
    MESSAGING,
    SMS,
    NONE;

    fun toCategoryOrNull(): AppCategory? = when (this) {
        EMAIL -> AppCategory.EMAIL
        MESSAGING -> AppCategory.MESSAGING
        SMS -> AppCategory.SMS
        NONE -> null
    }

    companion object {
        fun fromCategory(category: AppCategory?): AppCategoryChoice = when (category) {
            AppCategory.EMAIL -> EMAIL
            AppCategory.MESSAGING -> MESSAGING
            AppCategory.SMS -> SMS
            null -> NONE
        }
    }
}

/** Recognized package defaults used before the user customizes category grouping. */
object DefaultAppCategories {
    val EMAIL_PACKAGES: Set<String> = setOf(
        "com.google.android.gm",
        "com.microsoft.office.outlook",
        "com.yahoo.mobile.client.android.mail",
        "com.samsung.android.email.provider",
        "com.readdle.spark",
        "me.bluemail.mail",
        "ch.protonmail.android",
        "com.fsck.k9",
        "app.k9mail",
        "com.fastmail.app",
        "com.apple.android.mail",
    )

    val MESSAGING_PACKAGES: Set<String> = setOf(
        "com.whatsapp",
        "com.whatsapp.w4b",
        "org.telegram.messenger",
        "org.telegram.messenger.web",
        "org.thunderdog.challegram",
        "org.thoughtcrime.securesms",
        "com.Slack",
        "com.microsoft.teams",
        "com.discord",
        "com.facebook.orca",
        "com.instagram.android",
        "com.viber.voip",
        "jp.naver.line.android",
        "com.snapchat.android",
    )

    val SMS_PACKAGES: Set<String> = setOf(
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.android.mms",
        "com.oneplus.mms",
        "com.coloros.mms",
        "com.sonyericsson.conversations",
        "com.textra",
        "org.thoughtcrime.securesms.sms",
    )

    fun defaultCategoryFor(packageName: String): AppCategory? = when (packageName) {
        in EMAIL_PACKAGES -> AppCategory.EMAIL
        in MESSAGING_PACKAGES -> AppCategory.MESSAGING
        in SMS_PACKAGES -> AppCategory.SMS
        else -> null
    }
}

/** User-configurable pronunciation / acronym replacement rule. */
data class PronunciationRule(
    val match: String,
    val spokenAs: String,
    val matchCase: Boolean = false,
    val wholeWord: Boolean = true,
    val enabled: Boolean = true,
)

/** User-configurable URL domain/path summary rule. */
data class UrlSummaryRule(
    val pattern: String,
    val spokenPhrase: String,
    val isPredefined: Boolean = false,
    val enabled: Boolean = true,
)

/** How tap-back & emoji reactions are handled when filtered. */
enum class ReactionFilterMode {
    CONDENSE,
    SILENCE,
}

/** Digit grouping format for OTPs, tracking codes, and phone numbers. */
enum class DigitGroupingStyle {
    INDIVIDUAL,
    CHUNKS_OF_3,
}

/** Action triggered by Bluetooth headset playback buttons via MediaSession. */
enum class HeadsetButtonAction {
    PAUSE_RESUME,
    SKIP,
    REPEAT,
    NONE,
}

/** Built-in starter presets for active intelligence features. */
object IntelligencePresets {
    val DEFAULT_PRONUNCIATION_RULES: List<PronunciationRule> = listOf(
        PronunciationRule(match = "OTP", spokenAs = "O T P", matchCase = false, wholeWord = true),
        PronunciationRule(match = "ETA", spokenAs = "E T A", matchCase = false, wholeWord = true),
        PronunciationRule(match = "FYI", spokenAs = "F Y I", matchCase = false, wholeWord = true),
        PronunciationRule(match = "ASAP", spokenAs = "A S A P", matchCase = false, wholeWord = true),
        PronunciationRule(match = "Dr.", spokenAs = "Doctor", matchCase = false, wholeWord = false),
        PronunciationRule(match = "vs.", spokenAs = "versus", matchCase = false, wholeWord = false),
    )

    val DEFAULT_URL_SUMMARY_RULES: List<UrlSummaryRule> = listOf(
        UrlSummaryRule("instagram.com/reel", "Instagram reel", isPredefined = true),
        UrlSummaryRule("instagram.com/p/", "Instagram post", isPredefined = true),
        UrlSummaryRule("youtube.com", "YouTube video", isPredefined = true),
        UrlSummaryRule("youtu.be", "YouTube video", isPredefined = true),
        UrlSummaryRule("maps.google.com", "Google Maps location", isPredefined = true),
        UrlSummaryRule("goo.gl/maps", "Google Maps location", isPredefined = true),
        UrlSummaryRule("facebook.com", "Facebook post", isPredefined = true),
        UrlSummaryRule("fb.watch", "Facebook video", isPredefined = true),
        UrlSummaryRule("twitter.com", "X post", isPredefined = true),
        UrlSummaryRule("x.com", "X post", isPredefined = true),
        UrlSummaryRule("linkedin.com", "LinkedIn post", isPredefined = true),
        UrlSummaryRule("github.com", "GitHub link", isPredefined = true),
        UrlSummaryRule("zoom.us/j", "Zoom meeting", isPredefined = true),
        UrlSummaryRule("meet.google.com", "Google Meet link", isPredefined = true),
        UrlSummaryRule("open.spotify.com", "Spotify track", isPredefined = true),
        UrlSummaryRule("reddit.com", "Reddit post", isPredefined = true),
    )

    val DEFAULT_CARRIER_SPAM_PHRASES: List<String> = listOf(
        "recharge now",
        "dial *",
        "validity expires",
        "bonus data",
        "unlimited pack",
        "claim your free",
        "special offer",
        "loan approved",
    )
}
