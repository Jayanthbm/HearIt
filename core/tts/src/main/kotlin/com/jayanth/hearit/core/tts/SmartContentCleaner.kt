package com.jayanth.hearit.core.tts

import com.jayanth.hearit.core.common.model.AppCategory
import com.jayanth.hearit.core.common.model.AppCategoryChoice
import com.jayanth.hearit.core.common.model.DefaultAppCategories
import com.jayanth.hearit.core.common.model.DigitGroupingStyle
import com.jayanth.hearit.core.common.model.IntelligencePresets
import com.jayanth.hearit.core.common.model.MatchTarget
import com.jayanth.hearit.core.common.model.MediaReplacementRule
import com.jayanth.hearit.core.common.model.MessagingPolishConfig
import com.jayanth.hearit.core.common.model.PronunciationRule
import com.jayanth.hearit.core.common.model.ReactionFilterMode
import com.jayanth.hearit.core.common.model.SyncNoiseRule
import com.jayanth.hearit.core.common.model.UrlSummaryRule
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Outcome of [SmartContentCleaner.cleanMessage].
 *
 * When [suppress] is true, the notification should be silently retired rather than spoken
 * (e.g., WhatsApp background sync banners, silenced reactions, or carrier promotional SMS).
 */
data class CleanedMessage(
    val text: String,
    val suppress: Boolean = false,
    /** True when this message matched a user noise phrase whose app setting skips history. */
    val skipHistory: Boolean = false,
)

/**
 * Intelligent on-device content cleaner:
 * Pure Kotlin NLP cleaning engine transforming notification payloads into natural spoken text.
 */
object SmartContentCleaner {

    private val URL_PATTERN = Pattern.compile("https?://[^\\s]+", Pattern.CASE_INSENSITIVE)
    private val REPEATED_EMOJI_PATTERN = Pattern.compile("([\\p{So}\\p{Cn}]){3,}")
    private val MARKDOWN_BOLD_ITALIC = Pattern.compile("[*_~`]{1,3}(.*?)[*_~`]{1,3}")
    private val MARKDOWN_BULLETS = Pattern.compile("(?m)^[ \\t]*[-*•][ \\t]+")

    private val RUPEE_PATTERN = Pattern.compile("(?:₹|(?i)\\b(?:inr|rs\\.?))\\s*([0-9,]+(?:\\.[0-9]+)?)")
    private val DOLLAR_PATTERN = Pattern.compile("(?:\\$|(?i)\\busd)\\s*([0-9,]+(?:\\.[0-9]+)?)")
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

    private val EMAIL_RE_PREFIX = Pattern.compile("(?i)^re:\\s*")
    private val EMAIL_FWD_PREFIX = Pattern.compile("(?i)^fw[d]?:\\s*")
    private val EMAIL_SIGNOFF = Pattern.compile(
        "(?i)(?:\\n+\\s*|(?<=[.!,])\\s+|\\s{2,}|^)(?:best regards|kind regards|warm regards|thanks and regards|thanks & regards|many thanks|regards|yours faithfully|yours sincerely|sincerely|sent from my (?:iphone|galaxy|android|pixel)|cheers)[\\s\\S]*$",
    )
    private val EMAIL_DISCLAIMER = Pattern.compile(
        "(?i)(?:\\n+\\s*|(?<=[.!,])\\s+|\\s{2,}|^)(?:disclaimer:|confidentiality notice:|confidential:|this email and any attachments|please consider the environment before printing)[\\s\\S]*$",
    )

    private val QUOTED_TAPBACK_PATTERN = Pattern.compile(
        "(?i)^.*?\\s+(?:reacted\\s+\\S+|loved|liked|emphasized|disliked|questioned|laughed at)\\s+(?:to\\s+)?[\"“'].*?[\"”']$",
    )
    private val UNQUOTED_REACTION_PATTERN = Pattern.compile(
        "(?i)^(?:.+?\\s+)?(?:(?:liked|loved|disliked|emphasized|questioned|laughed at)\\s+(?:a|an|your|the)\\s+(?:photo|image|picture|video|message|audio|voice message|attachment|gif|sticker)\\b.*|reacted\\s+\\S+\\s+to\\s+(?:a|an|your|the)\\s+.*)|sent a reaction$",
    )

    private val BOT_SENDER_PATTERN = Pattern.compile(
        "(?i)^(?:no-?reply|do-?not-?reply|alerts?|notifications?|mailer-daemon)@.*$",
    )
    private val SHORTCODE_SENDER_PATTERN = Pattern.compile("^[A-Z]{2}-([A-Z0-9]{3,})")

    private val BANK_CUE_PATTERN = Pattern.compile(
        "(?i)(?:\\b(?:a/c|acct|account|card|upi|wallet|bank)\\b.*\\b(?:debited|credited|spent|paid|withdrawn|received)\\b|\\b(?:debited|credited)\\b\\s*(?:with|by|for)?\\s*(?:inr|rs\\.?|₹|\\$|usd|eur|€|£))",
    )
    private val BANK_TXN_PATTERN = Pattern.compile(
        "(?i)\\b(debited|credited|spent|paid|withdrawn|received)\\b[^\\d$₹€£]{0,20}(inr|rs\\.?|₹|usd|\\$|eur|€|gbp|£)\\s*([0-9,]+(?:\\.[0-9]+)?)|" +
            "(inr|rs\\.?|₹|usd|\\$|eur|€|gbp|£)\\s*([0-9,]+(?:\\.[0-9]+)?)\\s+(?:has been\\s+|was\\s+)?\\b(debited|credited|spent|paid|withdrawn|received)\\b",
    )
    private val BANK_MERCHANT_PATTERN = Pattern.compile(
        "(?i)\\b(?:at|to|from|via)\\s+([A-Za-z][A-Za-z0-9 &._'-]{1,36}?)(?=\\s*(?:\\.|$|;|\\bavl\\b|\\bavail\\b|\\bavailable\\b|\\bbal\\b|\\bref\\b|\\bupi ref\\b|\\bnot you\\b|\\bon\\s+\\d))",
    )

    private val PHONE_NUMBER_PATTERN = Pattern.compile(
        "(?<![/\\w:])(\\+?\\d[\\d\\s()-]{7,18}\\d)(?![\\w/.:])",
    )
    private val OTP_CODE_PATTERN = Pattern.compile(
        "(?<![$₹€£\\d.,:/-])\\b(\\d{4,8})\\b(?![.,:/-]\\d)",
    )

    private val appAliases = ConcurrentHashMap<String, String>()
    private val appCategoryOverrides = ConcurrentHashMap<String, AppCategoryChoice>()
    private val messagingPolishConfigs = ConcurrentHashMap<String, MessagingPolishConfig>()

    @Volatile var isUrlSummarizerEnabled: Boolean = true
    @Volatile var isCurrencyNormalizerEnabled: Boolean = true
    @Volatile var isMarkdownStripperEnabled: Boolean = true
    @Volatile var isEmojiCollapsingEnabled: Boolean = true
    @Volatile var isReactionFilterEnabled: Boolean = true
    @Volatile var reactionFilterMode: ReactionFilterMode = ReactionFilterMode.CONDENSE
    @Volatile var isBotSenderCleaningEnabled: Boolean = true

    @Volatile var isPronunciationDictionaryEnabled: Boolean = true
    @Volatile var pronunciationRules: List<PronunciationRule> = emptyList()

    @Volatile var urlSummaryRules: List<UrlSummaryRule> = IntelligencePresets.DEFAULT_URL_SUMMARY_RULES

    @Volatile var isDigitFormattingEnabled: Boolean = true
    @Volatile var isFormatPhoneNumbersEnabled: Boolean = true
    @Volatile var isFormatOtpCodesEnabled: Boolean = true

    @Volatile var isSmsBankCleanerEnabled: Boolean = true
    @Volatile var isCarrierSpamFilterEnabled: Boolean = true
    @Volatile var carrierSpamPhrases: List<String> = IntelligencePresets.DEFAULT_CARRIER_SPAM_PHRASES
    @Volatile var isEmailCleanupEnabled: Boolean = true

    @Volatile var isContactSanitizerEnabled: Boolean = true
    @Volatile var isSkipEmptyNotificationsEnabled: Boolean = false
    @Volatile var isSaveEmptyNotificationsToHistory: Boolean = false

    fun setAppAlias(packageName: String, alias: String?) {
        if (alias.isNullOrBlank()) {
            appAliases.remove(packageName)
        } else {
            appAliases[packageName] = alias.trim()
        }
    }

    /** Atomically replaces all custom spoken app aliases from persisted settings. */
    fun replaceAppAliases(aliases: Map<String, String>) {
        appAliases.clear()
        aliases.forEach { (pkg, alias) ->
            if (alias.isNotBlank()) appAliases[pkg] = alias.trim()
        }
    }

    /** Atomically replaces explicit per-app category overrides from persisted settings. */
    fun replaceAppCategories(overrides: Map<String, AppCategoryChoice>) {
        appCategoryOverrides.clear()
        appCategoryOverrides.putAll(overrides)
    }

    /** Atomically replaces per-app messaging polish configs from persisted settings. */
    fun replaceMessagingPolishConfigs(configs: Map<String, MessagingPolishConfig>) {
        messagingPolishConfigs.clear()
        configs.forEach { (pkg, config) ->
            if (!config.inheritsEveryValue) messagingPolishConfigs[pkg] = config
        }
    }

    fun messagingPolishConfigFor(packageName: String?): MessagingPolishConfig {
        if (packageName.isNullOrBlank()) return MessagingPolishConfig()
        return messagingPolishConfigs[packageName] ?: MessagingPolishConfig()
    }

    fun isMessagingPolishEnabled(packageName: String?, category: AppCategory? = resolveCategory(packageName)): Boolean {
        if (packageName != null && category != null && category != AppCategory.MESSAGING && category != AppCategory.SMS) return false
        val config = messagingPolishConfigFor(packageName)
        return config.enabled ?: true
    }

    fun resolveCategory(packageName: String?): AppCategory? {
        if (packageName.isNullOrBlank()) return null
        val override = appCategoryOverrides[packageName]
        if (override != null) return override.toCategoryOrNull()
        return DefaultAppCategories.defaultCategoryFor(packageName)
    }

    fun resolveAppName(packageName: String, defaultName: String): String =
        appAliases[packageName]?.takeIf { it.isNotBlank() } ?: defaultName

    /**
     * Cleans contact or sender names according to bot/shortcode, contact sanitizer,
     * per-app placeholder mappings (apply-to-sender), and pronunciation rules.
     */
    fun cleanSender(sender: String?, packageName: String? = null): String? {
        if (sender.isNullOrBlank()) return null
        val trimmed = sender.trim()

        // Per-app placeholder mapping for sender takes priority when matched.
        val senderMapped = normalizeCustomMediaMappingsForPackage(trimmed, packageName, forSender = true)
        if (!senderMapped.equals(trimmed, ignoreCase = false)) {
            val result = if (isPronunciationDictionaryEnabled) applyPronunciationRules(senderMapped, forSender = true) else senderMapped
            return result.takeIf { it.isNotBlank() } ?: trimmed
        }

        if (isBotSenderCleaningEnabled && BOT_SENDER_PATTERN.matcher(trimmed).matches()) {
            val domain = trimmed.substringAfter("@").substringBefore(".").replaceFirstChar { it.uppercase() }
            return "$domain Alert"
        }

        if (isBotSenderCleaningEnabled) {
            val shortcodeMatcher = SHORTCODE_SENDER_PATTERN.matcher(trimmed)
            if (shortcodeMatcher.find()) {
                val shortcode = shortcodeMatcher.group(1)
                return if (isPronunciationDictionaryEnabled) applyPronunciationRules(shortcode, forSender = true) else shortcode
            }
        }

        var clean = trimmed
        if (isContactSanitizerEnabled) {
            clean = clean.replace(Regex("[\\p{So}\\p{Cn}\\uFE0F\\u200D]"), "").trim()
        }

        if (isPronunciationDictionaryEnabled) {
            clean = applyPronunciationRules(clean, forSender = true)
        }

        return clean.takeIf { it.isNotBlank() } ?: trimmed
    }

    /** Returns true if active intelligence rules mark [text] as non-speakable noise or silenced. */
    fun shouldSuppress(text: String?, packageName: String? = null): Boolean =
        cleanMessage(text, packageName).suppress

    /** Returns true when "skip empty notifications" is enabled and both sender and body are blank. */
    fun shouldSuppressEmptyContent(sender: String?, messageText: String?): Boolean =
        isSkipEmptyNotificationsEnabled && sender.isNullOrBlank() && messageText.isNullOrBlank()

    /** Full NLP cleaning pass returning spoken text and suppression flag. */
    fun cleanMessage(text: String?, packageName: String? = null, sender: String? = null): CleanedMessage {
        var result = text?.trim().orEmpty()
        val category = resolveCategory(packageName)
        val messagingConfig = messagingPolishConfigFor(packageName)
        val messagingPolishEnabled = isMessagingPolishEnabled(packageName, category)

        // 1. Sync/background noise suppression (checking body, sender, or both):
        // - built-in sync noise phrases are messaging-polish (chat-app) behavior
        // - custom extra noise phrases are enabled for all apps
        if (isCustomNoiseRule(result, sender, messagingConfig)) {
            // An app can also keep noise-phrase matches out of its notification history.
            return CleanedMessage("", suppress = true, skipHistory = messagingConfig.hideNoisePhraseMatches)
        }
        if (messagingPolishEnabled && isBuiltInSyncBackgroundNoise(result, sender, packageName)) {
            return CleanedMessage("", suppress = true, skipHistory = messagingConfig.hideNoisePhraseMatches)
        }
        if (result.isBlank()) return CleanedMessage("")

        // 2. Carrier & Promo SMS/Chat filter
        if (isCarrierSpamFilterEnabled && (packageName == null || category == AppCategory.SMS || category == AppCategory.MESSAGING)) {
            if (matchesCarrierSpam(result)) {
                return CleanedMessage("", suppress = true)
            }
        }

        // 3. Tap-back & social reaction filter (Idea 12)
        if (isReactionFilterEnabled && isReactionMessage(result)) {
            return if (reactionFilterMode == ReactionFilterMode.SILENCE) {
                CleanedMessage("", suppress = true)
            } else {
                CleanedMessage("Reacted to a message", suppress = false)
            }
        }

        // 4. Placeholder/media normalization:
        // - custom placeholder mappings apply to all apps
        // - built-in messaging media placeholders are messaging-polish (chat-app) behavior
        result = normalizeCustomMediaMappings(result, messagingConfig, forSender = false)
        if (messagingPolishEnabled) {
            result = normalizeBuiltInMediaTags(result)
        }

        // 5. Bank & transactional SMS/Chat condensation
        if (isSmsBankCleanerEnabled && (packageName == null || category == AppCategory.SMS || category == AppCategory.MESSAGING)) {
            result = condenseBankSmsIfApplicable(result)
        }

        // 6. Email subject & disclaimer/sign-off cleanup (runs BEFORE markdown flattens newlines)
        if (isEmailCleanupEnabled && (packageName == null || category == AppCategory.EMAIL)) {
            result = cleanEmailContent(result)
        }

        // 7. Markdown syntax stripping (Idea 27)
        if (isMarkdownStripperEnabled) {
            result = stripMarkdown(result)
        }

        // 8. Smart URL summarization
        if (isUrlSummarizerEnabled) {
            result = summarizeUrls(result)
        }

        // 9. Currency & Units spoken normalizer (Idea 52)
        if (isCurrencyNormalizerEnabled) {
            result = normalizeUnitsAndCurrencies(result)
        }

        // 10. Pronunciation & Acronym Dictionary (Item 3.3)
        if (isPronunciationDictionaryEnabled) {
            result = applyPronunciationRules(result, forSender = false)
        }

        // 11. OTP & Phone digit-by-digit formatting (Item 9.2 A.3)
        if (isDigitFormattingEnabled) {
            result = formatDigitsForSpeech(result)
        }

        // 12. Emoji spam collapsing
        if (isEmojiCollapsingEnabled) {
            result = collapseEmojiSpam(result)
        }

        return CleanedMessage(result.trim(), suppress = false)
    }

    /** Convenience accessor returning cleaned spoken text without suppression flag. */
    fun cleanMessageText(text: String?, packageName: String? = null): String =
        cleanMessage(text, packageName).text

    private fun isBuiltInSyncBackgroundNoise(
        text: String,
        sender: String? = null,
        packageName: String? = null,
    ): Boolean {
        val textLower = text.lowercase().trim()
        val senderLower = sender?.lowercase()?.trim().orEmpty()
        val defaultRules = IntelligencePresets.defaultNoiseRulesFor(packageName)
        return defaultRules.any { rule ->
            val phrase = rule.phrase.trim().lowercase()
            if (phrase.isEmpty()) return@any false
            when (rule.target) {
                MatchTarget.BODY -> textLower.startsWith(phrase) || textLower.contains(phrase)
                MatchTarget.SENDER -> senderLower.startsWith(phrase) || senderLower.contains(phrase)
                MatchTarget.BOTH -> textLower.contains(phrase) || senderLower.contains(phrase)
            }
        }
    }

    private fun isCustomNoiseRule(
        text: String,
        sender: String?,
        config: MessagingPolishConfig,
    ): Boolean {
        val textLower = text.lowercase().trim()
        val senderLower = sender?.lowercase()?.trim().orEmpty()
        return config.extraNoiseRules.any { rule ->
            val phrase = rule.phrase.trim().lowercase()
            if (phrase.isEmpty()) return@any false
            when (rule.target) {
                MatchTarget.BODY -> textLower.contains(phrase)
                MatchTarget.SENDER -> senderLower.contains(phrase)
                MatchTarget.BOTH -> textLower.contains(phrase) || senderLower.contains(phrase)
            }
        }
    }

    private fun isSyncBackgroundNoise(
        text: String,
        config: MessagingPolishConfig = MessagingPolishConfig(),
        sender: String? = null,
        packageName: String? = null,
    ): Boolean = isBuiltInSyncBackgroundNoise(text, sender, packageName) || isCustomNoiseRule(text, sender, config)

    private fun matchesCarrierSpam(text: String): Boolean {
        val lower = text.lowercase()
        return carrierSpamPhrases.any { phrase ->
            val clean = phrase.trim().lowercase()
            clean.isNotEmpty() && lower.contains(clean)
        }
    }

    private fun isReactionMessage(text: String): Boolean =
        QUOTED_TAPBACK_PATTERN.matcher(text).matches() ||
            UNQUOTED_REACTION_PATTERN.matcher(text).matches()

    private fun normalizeCustomMediaMappings(
        input: String,
        config: MessagingPolishConfig,
        forSender: Boolean = false,
    ): String {
        val rules = config.mediaMappings.filter { rule ->
            rule.match.isNotBlank() && when (rule.target) {
                MatchTarget.BODY -> !forSender
                MatchTarget.SENDER -> forSender
                MatchTarget.BOTH -> true
            }
        }
        if (rules.isEmpty()) return input

        val trimmed = input.trim()
        // The whole text is a placeholder on its own ("Photo", "NZ"): speak the mapping only.
        rules.firstOrNull { rule -> trimmed.equals(rule.match.trim(), ignoreCase = true) }
            ?.let { return it.spokenPhrase }

        // The placeholder appears inside a longer text ("NZ 265/6 (40 ov)"): replace whole
        // tokens only, longest match first, so a short mapping can't corrupt a longer one.
        var result = input
        rules.sortedByDescending { it.match.trim().length }.forEach { rule ->
            val trimmedMatch = rule.match.trim()
            if (trimmedMatch == "/") {
                val cricketScorePattern = Regex("""\b(\d{1,4})\s*/\s*(10|[0-9])\b(?!\s*/)""")
                result = cricketScorePattern.replace(result) { mr ->
                    "${mr.groupValues[1]} ${rule.spokenPhrase.trim()} ${mr.groupValues[2]}".trim()
                }
                return@forEach
            }
            val pattern = wholeTokenPattern(trimmedMatch) ?: return@forEach
            result = pattern.replace(result) { rule.spokenPhrase }
        }
        return result
    }

    /** Case-insensitive pattern matching [token] only when it stands alone in the text. */
    private fun wholeTokenPattern(token: String): Regex? {
        if (token.isEmpty()) return null
        val prefix = if (token.first().isLetterOrDigit()) "(?<![\\p{L}\\p{N}])" else ""
        val suffix = if (token.last().isLetterOrDigit()) "(?![\\p{L}\\p{N}])" else ""
        return Regex(prefix + Regex.escape(token) + suffix, RegexOption.IGNORE_CASE)
    }

    private fun normalizeCustomMediaMappingsForPackage(
        input: String,
        packageName: String?,
        forSender: Boolean,
    ): String {
        if (packageName == null) return input
        val config = messagingPolishConfigFor(packageName)
        return normalizeCustomMediaMappings(input, config, forSender)
    }

    private fun normalizeBuiltInMediaTags(input: String): String {
        var text = input
        text = text.replace(Regex("^📊\\s*"), "Poll: ")
        text = text.replace(Regex("(?i)^(?:📊\\s*)?(?:Poll|\\[Poll\\])$"), "Created a poll")
        text = text.replace(Regex("(?i)^(?:📷\\s*)?(?:Photo|\\[Photo\\])$"), "Sent a photo")
        text = text.replace(Regex("(?i)^(?:🎥\\s*|🎬\\s*|📹\\s*)?(?:Video|\\[Video\\])$"), "Sent a video")
        text = text.replace(
            Regex("(?i)^(?:🎤\\s*)?(?:Voice message|\\[Voice message\\])(?:\\s*\\([0-9:]+\\))?$"),
            "Sent a voice message",
        )
        text = text.replace(Regex("(?i)^(?:📍\\s*)?(?:Location|Live location|\\[Location\\])$"), "Shared a location")
        text = text.replace(Regex("(?i)^(?:GIF|\\[GIF\\])$"), "Sent a GIF")
        text = text.replace(Regex("(?i)^(?:Sticker|\\[Sticker\\])$"), "Sent a sticker")

        val docWithDetail = Regex("(?i)^(?:📄\\s*|📎\\s*)?(?:Document|\\[Document\\])[:\\-\\s]+(.+)$")
            .matchEntire(text)
        if (docWithDetail != null) {
            val detail = docWithDetail.groupValues[1].trim().lowercase()
            return when {
                detail.endsWith(".pdf") -> "Sent a PDF document"
                detail.endsWith(".doc") || detail.endsWith(".docx") -> "Sent a Word document"
                detail.endsWith(".xls") || detail.endsWith(".xlsx") || detail.endsWith(".csv") -> "Sent a spreadsheet"
                detail.endsWith(".ppt") || detail.endsWith(".pptx") -> "Sent a presentation"
                else -> "Sent a document"
            }
        }
        text = text.replace(Regex("(?i)^(?:📄\\s*|📎\\s*)?(?:Document|\\[Document\\])$"), "Sent a document")

        val contactMatch = Regex("(?i)^(?:👤\\s*)?(?:Contact|Contact card|\\[Contact\\])(?:[:\\-\\s]+(.*))?$")
            .matchEntire(text)
        if (contactMatch != null) {
            val name = contactMatch.groupValues.getOrNull(1)?.trim().orEmpty()
            return if (name.isNotEmpty()) "Shared a contact card: $name" else "Shared a contact card"
        }
        return text
    }

    private fun normalizeMediaTags(input: String, config: MessagingPolishConfig = MessagingPolishConfig()): String {
        val mapped = normalizeCustomMediaMappings(input, config)
        return if (mapped != input) mapped else normalizeBuiltInMediaTags(mapped)
    }

    private fun condenseBankSmsIfApplicable(input: String): String {
        if (!BANK_CUE_PATTERN.matcher(input).find()) return input
        val txnMatcher = BANK_TXN_PATTERN.matcher(input)
        if (!txnMatcher.find()) return input

        val action: String
        val currencyToken: String
        val amount: String
        if (txnMatcher.group(1) != null) {
            action = txnMatcher.group(1)!!.lowercase()
            currencyToken = txnMatcher.group(2)!!
            amount = txnMatcher.group(3)!!
        } else {
            currencyToken = txnMatcher.group(4)!!
            amount = txnMatcher.group(5)!!
            action = txnMatcher.group(6)!!.lowercase()
        }

        val unit = currencyWord(currencyToken)
        val cleanAmount = amount.removeSuffix(".00")
        val merchantMatcher = BANK_MERCHANT_PATTERN.matcher(input)
        val merchant = if (merchantMatcher.find()) {
            merchantMatcher.group(1)?.trim()?.trimEnd('.', ',', ';')
        } else {
            null
        }
        return if (!merchant.isNullOrBlank()) {
            "$cleanAmount $unit $action at $merchant"
        } else {
            "$cleanAmount $unit $action"
        }
    }

    private fun currencyWord(token: String): String = when (token.trim().lowercase().trimEnd('.')) {
        "₹", "inr", "rs" -> "rupees"
        "$", "usd" -> "dollars"
        "€", "eur" -> "euros"
        "£", "gbp" -> "pounds"
        else -> "units"
    }

    private fun summarizeUrls(input: String): String {
        val activeRules = urlSummaryRules
            .filter { it.enabled && it.pattern.isNotBlank() && it.spokenPhrase.isNotBlank() }
            .sortedWith(
                compareBy<UrlSummaryRule> { it.isPredefined }
                    .thenByDescending { it.pattern.length },
            )

        val matcher = URL_PATTERN.matcher(input)
        val sb = StringBuffer()
        while (matcher.find()) {
            val url = matcher.group()
            val matchedRule = activeRules.firstOrNull { rule ->
                url.contains(rule.pattern.trim(), ignoreCase = true)
            }
            val replacement = matchedRule?.spokenPhrase?.trim() ?: run {
                val host = try {
                    val withoutProto = url.substringAfter("://")
                    val domainPart = withoutProto.substringBefore("/").removePrefix("www.")
                    domainPart.takeIf { it.isNotBlank() } ?: "link"
                } catch (_: Throwable) {
                    "link"
                }
                "link to $host"
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement))
        }
        matcher.appendTail(sb)
        return sb.toString()
    }

    private fun normalizeUnitsAndCurrencies(input: String): String {
        var text = input

        text = RUPEE_PATTERN.matcher(text).replaceAll("$1 rupees")
        text = DOLLAR_PATTERN.matcher(text).replaceAll("$1 dollars")
        text = EURO_PATTERN.matcher(text).replaceAll("$1 euros")
        text = POUND_PATTERN.matcher(text).replaceAll("$1 pounds")

        text = SPEED_KMH.matcher(text).replaceAll("$1 kilometers per hour")
        text = SPEED_MPH.matcher(text).replaceAll("$1 miles per hour")

        text = DATA_GB.matcher(text).replaceAll("$1 gigabytes")
        text = DATA_MB.matcher(text).replaceAll("$1 megabytes")
        text = DATA_KB.matcher(text).replaceAll("$1 kilobytes")

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
        var text = input
        while (true) {
            val reMatcher = EMAIL_RE_PREFIX.matcher(text)
            if (reMatcher.find()) {
                text = "Reply: " + text.substring(reMatcher.end())
                // Only unwrap once if next prefix isn't another Fwd/Re; break on Reply loop
                val afterReply = text.removePrefix("Reply: ")
                val innerFwd = EMAIL_FWD_PREFIX.matcher(afterReply)
                if (innerFwd.find()) {
                    text = "Reply: Forward: " + afterReply.substring(innerFwd.end())
                }
                break
            }
            val fwdMatcher = EMAIL_FWD_PREFIX.matcher(text)
            if (fwdMatcher.find()) {
                text = "Forward: " + text.substring(fwdMatcher.end())
                val afterFwd = text.removePrefix("Forward: ")
                val innerRe = EMAIL_RE_PREFIX.matcher(afterFwd)
                if (innerRe.find()) {
                    text = "Forward: Reply: " + afterFwd.substring(innerRe.end())
                }
                break
            }
            break
        }
        text = EMAIL_DISCLAIMER.matcher(text).replaceFirst("")
        text = EMAIL_SIGNOFF.matcher(text).replaceFirst("")
        return text.trimEnd()
    }

    private fun applyPronunciationRules(input: String, forSender: Boolean): String {
        val active = pronunciationRules.filter {
            it.enabled && it.match.isNotBlank() && (!forSender || it.applyToSender)
        }
        if (active.isEmpty()) return input
        var text = input
        for (rule in active.sortedByDescending { it.match.length }) {
            val escaped = Pattern.quote(rule.match.trim())
            val needsLeftBoundary = rule.wholeWord && rule.match.trim().firstOrNull()?.isLetterOrDigit() == true
            val needsRightBoundary = rule.wholeWord && rule.match.trim().lastOrNull()?.isLetterOrDigit() == true
            val patternStr = buildString {
                if (!rule.matchCase) append("(?i)")
                if (needsLeftBoundary) append("\\b")
                append(escaped)
                if (needsRightBoundary) append("\\b")
            }
            text = Pattern.compile(patternStr).matcher(text)
                .replaceAll(Matcher.quoteReplacement(rule.spokenAs))
        }
        return text
    }

    private fun formatDigitsForSpeech(input: String): String {
        // Protect existing URLs (if URL summarizer was toggled OFF) from digit rewriting
        val urlSpans = ArrayList<IntRange>()
        val urlMatcher = URL_PATTERN.matcher(input)
        while (urlMatcher.find()) {
            urlSpans += urlMatcher.start() until urlMatcher.end()
        }
        fun insideUrl(start: Int, end: Int): Boolean =
            urlSpans.any { span -> start < span.last + 1 && end > span.first }

        var text = input
        if (isFormatPhoneNumbersEnabled) {
            val sb = StringBuffer()
            val matcher = PHONE_NUMBER_PATTERN.matcher(text)
            while (matcher.find()) {
                val raw = matcher.group(1)!!
                val digitsOnly = raw.filter { it.isDigit() }
                // Phone numbers have 9..15 digits and must not be pure 4..8 digit codes or currency/dates
                if (digitsOnly.length in 9..15 && !insideUrl(matcher.start(1), matcher.end(1))) {
                    val trimmedRaw = raw.trimStart()
                    val spoken = if (trimmedRaw.startsWith("+")) {
                        val countryMatch = Regex("^\\+(\\d{1,3})[\\s-]+(.+)$").matchEntire(trimmedRaw)
                        if (countryMatch != null) {
                            val cc = countryMatch.groupValues[1].toCharArray().joinToString(" ")
                            val restDigits = countryMatch.groupValues[2].filter { it.isDigit() }
                            val spokenRest = formatDigitGroups(restDigits, DigitGroupingStyle.CHUNKS_OF_3)
                            "plus $cc, $spokenRest"
                        } else {
                            "plus ${formatDigitGroups(digitsOnly, DigitGroupingStyle.CHUNKS_OF_3)}"
                        }
                    } else {
                        formatDigitGroups(digitsOnly, DigitGroupingStyle.CHUNKS_OF_3)
                    }
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(spoken))
                } else {
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)))
                }
            }
            matcher.appendTail(sb)
            text = sb.toString()
        }

        if (isFormatOtpCodesEnabled) {
            // Recompute URL spans after phone pass
            val otpUrlSpans = ArrayList<IntRange>()
            val uMatcher = URL_PATTERN.matcher(text)
            while (uMatcher.find()) otpUrlSpans += uMatcher.start() until uMatcher.end()

            val sb = StringBuffer()
            val matcher = OTP_CODE_PATTERN.matcher(text)
            while (matcher.find()) {
                val start = matcher.start(1)
                val end = matcher.end(1)
                val digits = matcher.group(1)!!
                val inUrl = otpUrlSpans.any { start < it.last + 1 && end > it.first }
                val trailingWord = text.substring(end).trimStart().lowercase()
                val followedByUnitOrTime = trailingWord.startsWith("rupees") ||
                    trailingWord.startsWith("dollars") ||
                    trailingWord.startsWith("euros") ||
                    trailingWord.startsWith("pounds") ||
                    trailingWord.startsWith("minutes") ||
                    trailingWord.startsWith("hours") ||
                    trailingWord.startsWith("seconds") ||
                    trailingWord.startsWith("kilometers") ||
                    trailingWord.startsWith("miles") ||
                    trailingWord.startsWith("gigabytes") ||
                    trailingWord.startsWith("megabytes") ||
                    trailingWord.startsWith("kilobytes") ||
                    trailingWord.startsWith("am") ||
                    trailingWord.startsWith("pm") ||
                    trailingWord.startsWith("%")
                if (!inUrl && !followedByUnitOrTime) {
                    val spoken = formatDigitGroups(digits, DigitGroupingStyle.CHUNKS_OF_3)
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(spoken))
                } else {
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(digits))
                }
            }
            matcher.appendTail(sb)
            text = sb.toString()
        }
        return text
    }

    private fun formatDigitGroups(digits: String, style: DigitGroupingStyle): String {
        if (style == DigitGroupingStyle.INDIVIDUAL || digits.length <= 4) {
            return digits.toCharArray().joinToString(" ")
        }
        val chunks = if (digits.length >= 7 && digits.length % 3 == 1) {
            val head = digits.dropLast(4).chunked(3)
            head + digits.takeLast(4)
        } else {
            digits.chunked(3)
        }
        return chunks.joinToString(", ") { chunk ->
            chunk.toCharArray().joinToString(" ")
        }
    }

    private fun collapseEmojiSpam(input: String): String =
        REPEATED_EMOJI_PATTERN.matcher(input).replaceAll("multiple emojis")
}
