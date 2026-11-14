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
        MESSAGING, SMS -> AppCategory.MESSAGING
        NONE -> null
    }

    companion object {
        fun fromCategory(category: AppCategory?): AppCategoryChoice = when (category) {
            AppCategory.EMAIL -> EMAIL
            AppCategory.MESSAGING, AppCategory.SMS -> MESSAGING
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
        in MESSAGING_PACKAGES, in SMS_PACKAGES -> AppCategory.MESSAGING
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
    val applyToSender: Boolean = true,
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

/** Field target for intelligent speech rules (matching against notification body, sender/title, or both). */
enum class MatchTarget {
    BODY,
    SENDER,
    BOTH;

    val label: String
        get() = when (this) {
            BODY -> "Body only"
            SENDER -> "Sender only"
            BOTH -> "Both (Body & Sender)"
        }

    companion object {
        fun fromString(value: String?): MatchTarget = when (value?.uppercase()) {
            "BODY" -> BODY
            "SENDER" -> SENDER
            "FALSE" -> BODY
            "TRUE" -> BOTH
            else -> BOTH
        }
    }
}

/** One sync / background noise rule matching against body, sender, or both. */
data class SyncNoiseRule(
    val phrase: String,
    val target: MatchTarget = MatchTarget.BOTH,
)

/** One spoken replacement for a matched media placeholder or shorthand token. */
data class MediaReplacementRule(
    val match: String,
    val spokenPhrase: String,
    val target: MatchTarget = MatchTarget.BODY,
) {
    constructor(match: String, spokenPhrase: String, applyToSender: Boolean) : this(
        match = match,
        spokenPhrase = spokenPhrase,
        target = if (applyToSender) MatchTarget.BOTH else MatchTarget.BODY,
    )

    /** Backward compatibility: when true, applies to sender names. */
    val applyToSender: Boolean get() = target == MatchTarget.SENDER || target == MatchTarget.BOTH
}

/**
 * Per-app messaging/content cleanup configuration.
 *
 * [enabled] == null means inherit the default behavior (enabled for messaging apps).
 */
data class MessagingPolishConfig(
    val enabled: Boolean? = null,
    val extraNoiseRules: List<SyncNoiseRule> = emptyList(),
    val mediaMappings: List<MediaReplacementRule> = emptyList(),
    val hideNoisePhraseMatches: Boolean = true,
) {
    val extraNoisePhrases: List<String> get() = extraNoiseRules.map { it.phrase }
    val inheritsEveryValue: Boolean
        get() = enabled == null && extraNoiseRules.isEmpty() && mediaMappings.isEmpty() && hideNoisePhraseMatches
}

/** Built-in starter presets for active intelligence features. */
object IntelligencePresets {
    val DEFAULT_PRONUNCIATION_RULES: List<PronunciationRule> = listOf(
        PronunciationRule(match = "OTP", spokenAs = "O T P", matchCase = false, wholeWord = true, applyToSender = false),
        PronunciationRule(match = "ETA", spokenAs = "E T A", matchCase = false, wholeWord = true, applyToSender = false),
        PronunciationRule(match = "FYI", spokenAs = "F Y I", matchCase = false, wholeWord = true, applyToSender = false),
        PronunciationRule(match = "ASAP", spokenAs = "A S A P", matchCase = false, wholeWord = true, applyToSender = false),
        PronunciationRule(match = "Dr.", spokenAs = "Doctor", matchCase = false, wholeWord = false, applyToSender = true),
        PronunciationRule(match = "vs.", spokenAs = "versus", matchCase = false, wholeWord = false, applyToSender = true),
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

    val WHATSAPP_SYNC_NOISE: List<SyncNoiseRule> = listOf(
        SyncNoiseRule(phrase = "Checking for new messages", target = MatchTarget.BOTH),
        SyncNoiseRule(phrase = "Backup in progress", target = MatchTarget.BOTH),
        SyncNoiseRule(phrase = "WhatsApp Web is currently active", target = MatchTarget.BOTH),
        SyncNoiseRule(phrase = "WhatsApp Web", target = MatchTarget.BOTH),
        SyncNoiseRule(phrase = "Connecting", target = MatchTarget.BOTH),
    )

    val MESSAGES_SYNC_NOISE: List<SyncNoiseRule> = listOf(
        SyncNoiseRule(phrase = "Messages is doing work in the background", target = MatchTarget.BOTH),
        SyncNoiseRule(phrase = "Device pairing", target = MatchTarget.BOTH),
        SyncNoiseRule(phrase = "Connecting", target = MatchTarget.BOTH),
    )

    val OUTLOOK_SYNC_NOISE: List<SyncNoiseRule> = listOf(
        SyncNoiseRule(phrase = "Optimising database...", target = MatchTarget.BODY),
    )

    val DEFAULT_SYNC_NOISE: List<SyncNoiseRule> = listOf(
        SyncNoiseRule(phrase = "Checking for new messages", target = MatchTarget.BOTH),
        SyncNoiseRule(phrase = "Restoring messages", target = MatchTarget.BOTH),
        SyncNoiseRule(phrase = "Connecting", target = MatchTarget.BOTH),
    )

    val DEFAULT_SYNC_NOISE_PHRASES: List<String> = DEFAULT_SYNC_NOISE.map { it.phrase }

    val WHATSAPP_REPLACEMENTS: List<MediaReplacementRule> = listOf(
        MediaReplacementRule(match = "📊", spokenPhrase = "Poll", target = MatchTarget.BODY),
        MediaReplacementRule(match = "📷", spokenPhrase = "Photo", target = MatchTarget.BODY),
        MediaReplacementRule(match = "🎤", spokenPhrase = "Voice note", target = MatchTarget.BODY),
        MediaReplacementRule(match = "🎥", spokenPhrase = "Video", target = MatchTarget.BODY),
        MediaReplacementRule(match = "📄", spokenPhrase = "Document", target = MatchTarget.BODY),
        MediaReplacementRule(match = "📍", spokenPhrase = "Location", target = MatchTarget.BODY),
    )

    val OUTLOOK_REPLACEMENTS: List<MediaReplacementRule> = listOf(
        MediaReplacementRule(match = "Re:", spokenPhrase = "Reply:", target = MatchTarget.BODY),
        MediaReplacementRule(match = "Fwd:", spokenPhrase = "Forward:", target = MatchTarget.BODY),
        MediaReplacementRule(match = "Sent from my iPhone", spokenPhrase = "", target = MatchTarget.BODY),
        MediaReplacementRule(match = "Sent from Outlook for Android", spokenPhrase = "", target = MatchTarget.BODY),
        MediaReplacementRule(match = "Get Outlook for iOS", spokenPhrase = "", target = MatchTarget.BODY),
    )

    val CREX_REPLACEMENTS: List<MediaReplacementRule> = listOf(
        // Countries
        MediaReplacementRule(match = "IND", spokenPhrase = "India", target = MatchTarget.BOTH),
        MediaReplacementRule(match = "NZ", spokenPhrase = "New Zealand", target = MatchTarget.BOTH),
        MediaReplacementRule(match = "SA", spokenPhrase = "South Africa", target = MatchTarget.BOTH),
        MediaReplacementRule(match = "PAK", spokenPhrase = "Pakistan", target = MatchTarget.BOTH),
        MediaReplacementRule(match = "BAN", spokenPhrase = "Bangladesh", target = MatchTarget.BOTH),
        MediaReplacementRule(match = "AUS", spokenPhrase = "Australia", target = MatchTarget.BOTH),
        MediaReplacementRule(match = "SL", spokenPhrase = "Sri Lanka", target = MatchTarget.BOTH),
        MediaReplacementRule(match = "ENG", spokenPhrase = "England", target = MatchTarget.BOTH),
        MediaReplacementRule(match = "AFG", spokenPhrase = "Afghanistan", target = MatchTarget.BOTH),
        MediaReplacementRule(match = "ZIM", spokenPhrase = "Zimbabwe", target = MatchTarget.BOTH),
        MediaReplacementRule(match = "WI", spokenPhrase = "West Indies", target = MatchTarget.BOTH),
        MediaReplacementRule(match = "SCO", spokenPhrase = "Scotland", target = MatchTarget.BOTH),
        MediaReplacementRule(match = "CAN", spokenPhrase = "Canada", target = MatchTarget.BOTH),
        MediaReplacementRule(match = "UAE", spokenPhrase = "U A E", target = MatchTarget.BOTH),
        MediaReplacementRule(match = "NAM", spokenPhrase = "Namibia", target = MatchTarget.BOTH),
        MediaReplacementRule(match = "IRE", spokenPhrase = "Ireland", target = MatchTarget.BOTH),
        MediaReplacementRule(match = "NEP", spokenPhrase = "Nepal", target = MatchTarget.BOTH),
        MediaReplacementRule(match = "OMA", spokenPhrase = "Oman", target = MatchTarget.BOTH),
        // Franchises
        MediaReplacementRule(match = "RCB", spokenPhrase = "R C B", target = MatchTarget.BOTH),
        MediaReplacementRule(match = "CSK", spokenPhrase = "C S K", target = MatchTarget.BOTH),
        MediaReplacementRule(match = "GT", spokenPhrase = "G T", target = MatchTarget.BOTH),
        MediaReplacementRule(match = "SRH", spokenPhrase = "S R H", target = MatchTarget.BOTH),
        MediaReplacementRule(match = "KKR", spokenPhrase = "K K R", target = MatchTarget.BOTH),
        MediaReplacementRule(match = "MI", spokenPhrase = "M I", target = MatchTarget.BOTH),
        MediaReplacementRule(match = "RR", spokenPhrase = "R R", target = MatchTarget.BOTH),
        MediaReplacementRule(match = "PBKS", spokenPhrase = "P B K S", target = MatchTarget.BOTH),
        MediaReplacementRule(match = "LSG", spokenPhrase = "L S G", target = MatchTarget.BOTH),
        MediaReplacementRule(match = "DC", spokenPhrase = "D C", target = MatchTarget.BOTH),
        MediaReplacementRule(match = "KXIP", spokenPhrase = "K X I P", target = MatchTarget.BOTH),
        // Match terms
        MediaReplacementRule(match = "U19", spokenPhrase = "Under 19", target = MatchTarget.BOTH),
        MediaReplacementRule(match = "ODI", spokenPhrase = "O D I", target = MatchTarget.BOTH),
        MediaReplacementRule(match = "ov", spokenPhrase = "Overs", target = MatchTarget.BOTH),
        MediaReplacementRule(match = "vs", spokenPhrase = "versus", target = MatchTarget.BOTH),
    )

    val DEFAULT_CHAT_REPLACEMENTS: List<MediaReplacementRule> = WHATSAPP_REPLACEMENTS
    val DEFAULT_EMAIL_REPLACEMENTS: List<MediaReplacementRule> = OUTLOOK_REPLACEMENTS

    fun defaultNoiseRulesFor(packageName: String?): List<SyncNoiseRule> {
        val pkg = packageName?.lowercase().orEmpty()
        return when {
            pkg.contains("whatsapp") -> WHATSAPP_SYNC_NOISE
            pkg.contains("messaging") || pkg.contains("mms") -> MESSAGES_SYNC_NOISE
            pkg.contains("outlook") -> OUTLOOK_SYNC_NOISE
            else -> DEFAULT_SYNC_NOISE
        }
    }

    fun defaultReplacementsFor(packageName: String?, category: AppCategory? = null): List<MediaReplacementRule> {
        val pkg = packageName?.lowercase().orEmpty()
        return when {
            pkg.contains("whatsapp") -> WHATSAPP_REPLACEMENTS
            pkg.contains("cricketexchange") || pkg.contains("crex") || pkg.contains("cricket") -> CREX_REPLACEMENTS
            pkg.contains("outlook") || category == AppCategory.EMAIL -> OUTLOOK_REPLACEMENTS
            category == AppCategory.MESSAGING || category == AppCategory.SMS -> WHATSAPP_REPLACEMENTS
            else -> emptyList()
        }
    }
}
