package com.jayanth.hearit.core.tts

import java.util.regex.Pattern

/**
 * Intelligent on-device content cleaner (Phase 2):
 * Pure Kotlin NLP cleaning engine that transforms raw notification payloads into natural,
 * pleasant, and conversational spoken text.
 *
 * Capabilities:
 *  1. Smart Link / URL Summarizer (YouTube, Instagram, Maps, generic domains)
 *  2. Emoji Spam Collapsing & Tap-Back Filter (Idea 12)
 *  3. Markdown & Formatting Syntax Stripper (Idea 27)
 *  4. Currency & Unit Spoken Normalizer (Idea 52)
 *  5. Bot & Automated Sender Identification (Idea 48)
 *  6. Contact Name Sanitizer (Idea 3)
 *  7. Messaging & Media Tag Conversions (WhatsApp, Telegram, Messages)
 *  8. Email Subject & Sign-off Truncation
 */
object SmartContentCleaner {

    private val URL_PATTERN = Pattern.compile("https?://[^\\s]+", Pattern.CASE_INSENSITIVE)
    private val REPEATED_EMOJI_PATTERN = Pattern.compile("([\\p{So}\\p{Cn}]){3,}")
    private val MARKDOWN_BOLD_ITALIC = Pattern.compile("[*_~`]{1,3}(.*?)[*_~`]{1,3}")
    private val MARKDOWN_BULLETS = Pattern.compile("(?m)^[ \\t]*[-*•][ \\t]+")

    private val RUPEE_PATTERN = Pattern.compile("₹\\s*([0-9,]+(?:\\.[0-9]+)?)")
    private val DOLLAR_PATTERN = Pattern.compile("\\$\\s*([0-9,]+(?:\\.[0-9]+)?)")
    private val EURO_PATTERN = Pattern.compile("€\\s*([0-9,]+(?:\\.[0-9]+)?)")
    private val POUND_PATTERN = Pattern.compile("£\\s*([0-9,]+(?:\\.[0-9]+)?)")

    private val SPEED_KMH = Pattern.compile("(?i)\\b(\\d+)\\s*(?:km/h|kmph|kph)\\b")
    private val SPEED_MPH = Pattern.compile("(?i)\\b(\\d+)\\s*mph\\b")

    private val DATA_GB = Pattern.compile("(?i)\\b(\\d+)\\s*gb\\b")
    private val DATA_MB = Pattern.compile("(?i)\\b(\\d+)\\s*mb\\b")
    private val DATA_KB = Pattern.compile("(?i)\\b(\\d+)\\s*kb\\b")

    private val TIME_MINS = Pattern.compile("(?i)\\b(\\d+)\\s*mins?\\b")
    private val TIME_HOURS = Pattern.compile("(?i)\\b(\\d+)\\s*hrs?\\b")
    private val TIME_SECS = Pattern.compile("(?i)\\b(\\d+)\\s*secs?\\b")

    private val EMAIL_RE_FWD = Pattern.compile("(?i)^(?:re|fwd?):\\s*")
    private val EMAIL_SIGNOFF = Pattern.compile(
        "(?i)\\n\\s*(?:best regards|regards|thanks and regards|warm regards|sincerely|sent from my (?:iphone|galaxy|android)|cheers)[\\s\\S]*$",
    )

    private val TAPBACK_PATTERN = Pattern.compile(
        "(?i)^.*?\\s+(?:reacted\\s+\\S+|loved|liked|emphasized|disliked)\\s+(?:to\\s+)?[\"'].*?[\"']$",
    )

    private val BOT_SENDER_PATTERN = Pattern.compile(
        "(?i)^(?:no-?reply|do-?not-?reply|alerts?|notifications?|mailer-daemon)@.*$",
    )
    private val SHORTCODE_SENDER_PATTERN = Pattern.compile("^[A-Z]{2}-([A-Z0-9]{3,})")

    private val appAliases = java.util.concurrent.ConcurrentHashMap<String, String>()

    var isUrlSummarizerEnabled: Boolean = true
    var isCurrencyNormalizerEnabled: Boolean = true
    var isMarkdownStripperEnabled: Boolean = true
    var isEmojiCollapsingEnabled: Boolean = true
    var isReactionFilterEnabled: Boolean = true
    var isBotSenderCleaningEnabled: Boolean = true

    fun setAppAlias(packageName: String, alias: String?) {
        if (alias.isNullOrBlank()) {
            appAliases.remove(packageName)
        } else {
            appAliases[packageName] = alias.trim()
        }
    }

    fun resolveAppName(packageName: String, defaultName: String): String {
        return appAliases[packageName] ?: defaultName
    }

    /**
     * Cleans contact or sender names:
     * - Strips emojis from names ("Rahul 🏏🔥" -> "Rahul")
     * - Strips bracketed company/notes ("Dr. Sharma (Cardio)" -> "Dr. Sharma")
     * - Normalizes shortcodes ("VK-HDFCBK" -> "HDFCBK")
     * - Identifies bot senders ("no-reply@uber.com" -> "Uber Alert")
     */
    fun cleanSender(sender: String?): String? {
        if (sender.isNullOrBlank()) return null
        val trimmed = sender.trim()

        if (isBotSenderCleaningEnabled && BOT_SENDER_PATTERN.matcher(trimmed).matches()) {
            val domain = trimmed.substringAfter("@").substringBefore(".").replaceFirstChar { it.uppercase() }
            return "$domain Alert"
        }

        if (isBotSenderCleaningEnabled) {
            val shortcodeMatcher = SHORTCODE_SENDER_PATTERN.matcher(trimmed)
            if (shortcodeMatcher.find()) {
                return shortcodeMatcher.group(1)
            }
        }

        // Strip bracketed nicknames or notes: "Rahul (Work)" -> "Rahul"
        var clean = trimmed.replace(Regex("\\s*\\([^)]*\\)"), "")

        // Strip trailing or leading emojis from contact name
        clean = clean.replace(Regex("[\\p{So}\\p{Cn}]"), "").trim()

        return clean.takeIf { it.isNotBlank() } ?: trimmed
    }

    /**
     * Full NLP cleaning pass on notification message text.
     */
    fun cleanMessageText(text: String?): String {
        if (text.isNullOrBlank()) return ""
        var result = text.trim()

        // 1. WhatsApp / Sync background noise suppression
        if (result.equals("Checking for new messages…", ignoreCase = true) ||
            result.equals("Checking for new messages...", ignoreCase = true) ||
            result.startsWith("WhatsApp Web is currently active", ignoreCase = true)
        ) {
            return ""
        }

        // 2. Tap-back reaction filtering (Idea 12)
        if (isReactionFilterEnabled && TAPBACK_PATTERN.matcher(result).matches()) {
            return "Reacted to a message"
        }

        // 3. Media tags normalization
        result = normalizeMediaTags(result)

        // 4. Smart URL summarization
        if (isUrlSummarizerEnabled) {
            result = summarizeUrls(result)
        }

        // 5. Currency & Units spoken normalizer (Idea 52)
        if (isCurrencyNormalizerEnabled) {
            result = normalizeUnitsAndCurrencies(result)
        }

        // 6. Markdown syntax stripping (Idea 27)
        if (isMarkdownStripperEnabled) {
            result = stripMarkdown(result)
        }

        // 7. Email subject & sign-off cleaning
        result = cleanEmailContent(result)

        // 8. Emoji spam collapsing
        if (isEmojiCollapsingEnabled) {
            result = collapseEmojiSpam(result)
        }

        return result.trim()
    }

    private fun normalizeMediaTags(input: String): String {
        var text = input
        text = text.replace(Regex("(?i)^(?:📷\\s*)?(?:Photo|\\[Photo\\])$"), "Sent a photo")
        text = text.replace(Regex("(?i)^(?:🎤\\s*)?(?:Voice message|\\[Voice message\\])(?:\\s*\\([0-9:]+\\))?$"), "Sent a voice message")
        text = text.replace(Regex("(?i)^(?:📍\\s*)?(?:Location|Live location|\\[Location\\])$"), "Shared a location")
        text = text.replace(Regex("(?i)^(?:GIF|\\[GIF\\])$"), "Sent a GIF")
        text = text.replace(Regex("(?i)^(?:Sticker|\\[Sticker\\])$"), "Sent a sticker")
        text = text.replace(Regex("(?i)^(?:Document|\\[Document\\])$"), "Sent a document")
        return text
    }

    private fun summarizeUrls(input: String): String {
        val matcher = URL_PATTERN.matcher(input)
        val sb = StringBuffer()
        while (matcher.find()) {
            val url = matcher.group()
            val replacement = when {
                url.contains("instagram.com/reel", ignoreCase = true) -> "Instagram reel"
                url.contains("instagram.com/p/", ignoreCase = true) -> "Instagram post"
                url.contains("youtube.com", ignoreCase = true) || url.contains("youtu.be", ignoreCase = true) -> "YouTube video"
                url.contains("maps.google.com", ignoreCase = true) || url.contains("goo.gl/maps", ignoreCase = true) -> "Google Maps location"
                url.contains("facebook.com", ignoreCase = true) || url.contains("fb.watch", ignoreCase = true) -> "Facebook post"
                url.contains("twitter.com", ignoreCase = true) || url.contains("x.com", ignoreCase = true) -> "X post"
                url.contains("linkedin.com", ignoreCase = true) -> "LinkedIn post"
                else -> {
                    // Extract domain name: https://news.bbc.co.uk/page -> "link to news.bbc.co.uk"
                    val host = try {
                        val withoutProto = url.substringAfter("://")
                        val domainPart = withoutProto.substringBefore("/").removePrefix("www.")
                        domainPart.takeIf { it.isNotBlank() } ?: "link"
                    } catch (_: Throwable) {
                        "link"
                    }
                    "link to $host"
                }
            }
            matcher.appendReplacement(sb, replacement)
        }
        matcher.appendTail(sb)
        return sb.toString()
    }

    private fun normalizeUnitsAndCurrencies(input: String): String {
        var text = input

        // Currency
        text = RUPEE_PATTERN.matcher(text).replaceAll("$1 rupees")
        text = DOLLAR_PATTERN.matcher(text).replaceAll("$1 dollars")
        text = EURO_PATTERN.matcher(text).replaceAll("$1 euros")
        text = POUND_PATTERN.matcher(text).replaceAll("$1 pounds")

        // Speeds
        text = SPEED_KMH.matcher(text).replaceAll("$1 kilometers per hour")
        text = SPEED_MPH.matcher(text).replaceAll("$1 miles per hour")

        // Data
        text = DATA_GB.matcher(text).replaceAll("$1 gigabytes")
        text = DATA_MB.matcher(text).replaceAll("$1 megabytes")
        text = DATA_KB.matcher(text).replaceAll("$1 kilobytes")

        // Time
        text = TIME_MINS.matcher(text).replaceAll("$1 minutes")
        text = TIME_HOURS.matcher(text).replaceAll("$1 hours")
        text = TIME_SECS.matcher(text).replaceAll("$1 seconds")

        return text
    }

    private fun stripMarkdown(input: String): String {
        var text = input
        text = MARKDOWN_BOLD_ITALIC.matcher(text).replaceAll("$1")
        text = MARKDOWN_BULLETS.matcher(text).replaceAll("")
        text = text.replace(Regex("\\n\\s*"), " ")
        return text
    }

    private fun cleanEmailContent(input: String): String {
        var text = EMAIL_RE_FWD.matcher(input).replaceFirst("")
        text = EMAIL_SIGNOFF.matcher(text).replaceFirst("")
        return text
    }

    private fun collapseEmojiSpam(input: String): String {
        // Collapses 3+ repeated emojis into single occurrence
        return REPEATED_EMOJI_PATTERN.matcher(input).replaceAll("multiple emojis")
    }
}
