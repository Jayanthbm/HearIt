package com.jayanth.hearit.core.tts

import com.jayanth.hearit.core.common.model.AppCategoryChoice
import com.jayanth.hearit.core.common.model.DigitGroupingStyle
import com.jayanth.hearit.core.common.model.IntelligencePresets
import com.jayanth.hearit.core.common.model.PronunciationRule
import com.jayanth.hearit.core.common.model.ReactionFilterMode
import com.jayanth.hearit.core.common.model.UrlSummaryRule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SmartContentCleanerTest {

    @Before
    @After
    fun resetCleanerState() {
        SmartContentCleaner.isUrlSummarizerEnabled = true
        SmartContentCleaner.isCurrencyNormalizerEnabled = true
        SmartContentCleaner.isMarkdownStripperEnabled = true
        SmartContentCleaner.isEmojiCollapsingEnabled = true
        SmartContentCleaner.isReactionFilterEnabled = true
        SmartContentCleaner.reactionFilterMode = ReactionFilterMode.CONDENSE
        SmartContentCleaner.isBotSenderCleaningEnabled = true
        SmartContentCleaner.isPronunciationDictionaryEnabled = true
        SmartContentCleaner.isPronunciationOnSenderEnabled = false
        SmartContentCleaner.pronunciationRules = emptyList()
        SmartContentCleaner.urlSummaryRules = IntelligencePresets.DEFAULT_URL_SUMMARY_RULES
        SmartContentCleaner.isDigitFormattingEnabled = true
        SmartContentCleaner.isFormatPhoneNumbersEnabled = true
        SmartContentCleaner.isFormatOtpCodesEnabled = true
        SmartContentCleaner.digitGroupingStyle = DigitGroupingStyle.CHUNKS_OF_3
        SmartContentCleaner.isMessagingIntelligenceEnabled = true
        SmartContentCleaner.isSmsBankCleanerEnabled = true
        SmartContentCleaner.isCarrierSpamFilterEnabled = true
        SmartContentCleaner.carrierSpamPhrases = IntelligencePresets.DEFAULT_CARRIER_SPAM_PHRASES
        SmartContentCleaner.isEmailCleanupEnabled = true
        SmartContentCleaner.isContactSanitizerEnabled = true
        SmartContentCleaner.isStripSenderEmojisEnabled = true
        SmartContentCleaner.isStripSenderBracketsEnabled = true
        SmartContentCleaner.replaceAppAliases(emptyMap())
        SmartContentCleaner.replaceAppCategories(emptyMap())
    }

    @Test
    fun cleanSender_sanitizesContactNameEmojisAndBrackets() {
        assertEquals("Rahul", SmartContentCleaner.cleanSender("Rahul 🏏🔥"))
        assertEquals("Dr. Sharma", SmartContentCleaner.cleanSender("Dr. Sharma (Cardio)"))
        assertEquals("Ananya", SmartContentCleaner.cleanSender("Ananya (Work)"))
    }

    @Test
    fun cleanSender_respectsContactSanitizerSubToggles() {
        SmartContentCleaner.isStripSenderEmojisEnabled = false
        assertEquals("Rahul 🏏🔥", SmartContentCleaner.cleanSender("Rahul 🏏🔥"))

        SmartContentCleaner.isStripSenderEmojisEnabled = true
        SmartContentCleaner.isStripSenderBracketsEnabled = false
        assertEquals("Ananya (Work)", SmartContentCleaner.cleanSender("Ananya (Work) 🌟"))

        SmartContentCleaner.isContactSanitizerEnabled = false
        assertEquals("Ananya (Work) 🌟", SmartContentCleaner.cleanSender("Ananya (Work) 🌟"))
    }

    @Test
    fun cleanSender_normalizesShortcodesAndBots() {
        assertEquals("HDFCBK", SmartContentCleaner.cleanSender("VK-HDFCBK"))
        assertEquals("AIRTEL", SmartContentCleaner.cleanSender("AD-AIRTEL"))
        assertEquals("Uber Alert", SmartContentCleaner.cleanSender("no-reply@uber.com"))
        assertNull(SmartContentCleaner.cleanSender(null))
        assertNull(SmartContentCleaner.cleanSender("   "))
    }

    @Test
    fun cleanMessageText_summarizesUrlsAndRespectsCustomOverride() {
        val yt = "Check this out https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        assertEquals("Check this out YouTube video", SmartContentCleaner.cleanMessageText(yt))

        val insta = "See reel: https://instagram.com/reel/C3bA189zQ/?igsh=abc"
        assertEquals("See reel: Instagram reel", SmartContentCleaner.cleanMessageText(insta))

        val maps = "Meet here: https://maps.google.com/?q=12.97,77.59"
        assertEquals("Meet here: Google Maps location", SmartContentCleaner.cleanMessageText(maps))

        val generic = "Read article at https://news.bbc.co.uk/world/news-123"
        assertEquals("Read article at link to news.bbc.co.uk", SmartContentCleaner.cleanMessageText(generic))

        SmartContentCleaner.urlSummaryRules = SmartContentCleaner.urlSummaryRules + UrlSummaryRule(
            pattern = "notion.so",
            spokenPhrase = "Notion doc",
            isPredefined = false,
            enabled = true,
        )
        assertEquals("Open Notion doc", SmartContentCleaner.cleanMessageText("Open https://www.notion.so/roadmap-123"))
    }

    @Test
    fun cleanMessageText_normalizesCurrenciesAndUnits() {
        assertEquals("Paid 1,499 rupees for dinner", SmartContentCleaner.cleanMessageText("Paid ₹1,499 for dinner"))
        assertEquals("Price: 25 dollars", SmartContentCleaner.cleanMessageText("Price: $25"))
        assertEquals("Driving at 60 kilometers per hour", SmartContentCleaner.cleanMessageText("Driving at 60 km/h"))
        assertEquals("Remaining 12 gigabytes data", SmartContentCleaner.cleanMessageText("Remaining 12GB data"))
        assertEquals("Arriving in 15 minutes", SmartContentCleaner.cleanMessageText("Arriving in 15 mins"))
    }

    @Test
    fun cleanMessageText_appliesPronunciationDictionary() {
        SmartContentCleaner.pronunciationRules = IntelligencePresets.DEFAULT_PRONUNCIATION_RULES
        assertEquals(
            "O T P arriving by E T A",
            SmartContentCleaner.cleanMessageText("OTP arriving by ETA"),
        )
    }

    @Test
    fun cleanMessageText_formatsOtpAndPhoneDigitByDigitWithoutBreakingTimesOrCurrency() {
        assertEquals(
            "Your code is 4 8 2, 9 1 0 valid for 10 minutes",
            SmartContentCleaner.cleanMessageText("Your code is 482910 valid for 10 mins"),
        )
        assertEquals(
            "Call plus 9 1, 9 8 7, 6 5 4, 3 2 1 0 at 10:30",
            SmartContentCleaner.cleanMessageText("Call +91 9876543210 at 10:30"),
        )
        SmartContentCleaner.digitGroupingStyle = DigitGroupingStyle.INDIVIDUAL
        assertEquals("Code 4 8 2 9 1 0", SmartContentCleaner.cleanMessageText("Code 482910"))
    }

    @Test
    fun cleanMessageText_stripsMarkdownAndFormatting() {
        assertEquals("Important update now", SmartContentCleaner.cleanMessageText("**Important** update `now`"))
        assertEquals("Items: Milk Bread", SmartContentCleaner.cleanMessageText("Items:\n- Milk\n- Bread"))
    }

    @Test
    fun cleanMessage_filtersQuotedAndUnquotedReactionsAndRespectsSilenceMode() {
        val quoted = "Rahul reacted ❤️ to \"See you tomorrow\""
        assertEquals(CleanedMessage("Reacted to a message", suppress = false), SmartContentCleaner.cleanMessage(quoted))

        val unquoted = "Liked a photo"
        assertEquals(CleanedMessage("Reacted to a message", suppress = false), SmartContentCleaner.cleanMessage(unquoted))

        val androidUnquoted = "Mom reacted 👍 to your message"
        assertEquals(CleanedMessage("Reacted to a message", suppress = false), SmartContentCleaner.cleanMessage(androidUnquoted))

        SmartContentCleaner.reactionFilterMode = ReactionFilterMode.SILENCE
        assertTrue(SmartContentCleaner.cleanMessage(unquoted).suppress)
        assertTrue(SmartContentCleaner.cleanMessage(quoted).suppress)
    }

    @Test
    fun cleanMessage_convertsExtendedMessagingMediaTagsAndSuppressesSyncNoise() {
        assertEquals("Sent a photo", SmartContentCleaner.cleanMessageText("📷 Photo"))
        assertEquals("Sent a video", SmartContentCleaner.cleanMessageText("🎥 Video"))
        assertEquals("Sent a voice message", SmartContentCleaner.cleanMessageText("🎤 Voice message (0:45)"))
        assertEquals("Shared a location", SmartContentCleaner.cleanMessageText("📍 Location"))
        assertEquals("Sent a PDF document", SmartContentCleaner.cleanMessageText("📄 Document: invoice_nov.pdf"))
        assertEquals("Shared a contact card: Rohan", SmartContentCleaner.cleanMessageText("Contact: Rohan"))

        assertTrue(SmartContentCleaner.cleanMessage("Backup in progress...", "com.whatsapp").suppress)
        assertTrue(SmartContentCleaner.cleanMessage("Checking for new messages...", "com.whatsapp").suppress)
    }

    @Test
    fun cleanMessage_handlesEmailSubjectSignoffAndLegalDisclaimersScopedByCategory() {
        val email = "Re: Fwd: Project Sync\nSee you at 3.\n\nKind regards,\nJayanth\nCONFIDENTIALITY NOTICE: Do not share."
        val cleaned = SmartContentCleaner.cleanMessageText(email, "com.google.android.gm")
        assertEquals("Reply: Forward: Project Sync See you at 3.", cleaned)

        // When opted out of EMAIL category, Re: prefix is not rewritten
        SmartContentCleaner.replaceAppCategories(mapOf("com.google.android.gm" to AppCategoryChoice.NONE))
        val uncategorized = SmartContentCleaner.cleanMessageText("Re: Quick note", "com.google.android.gm")
        assertFalse(uncategorized.startsWith("Reply:"))
    }

    @Test
    fun cleanMessage_condensesBankSmsAndSuppressesCarrierSpam() {
        val bankSms = "Your A/c XX1234 is debited for INR 500.00 at Swiggy. Avl Bal INR 12,400."
        assertEquals(
            "500 rupees debited at Swiggy",
            SmartContentCleaner.cleanMessageText(bankSms, "com.google.android.apps.messaging"),
        )

        val promoSms = "Special offer! Recharge now with 2GB bonus data. Dial *121#"
        assertTrue(
            SmartContentCleaner.cleanMessage(promoSms, "com.google.android.apps.messaging").suppress,
        )
    }

    @Test
    fun appAlias_resolvesCustomSpokenName() {
        SmartContentCleaner.setAppAlias("com.whatsapp.w4b", "Work Chat")
        assertEquals("Work Chat", SmartContentCleaner.resolveAppName("com.whatsapp.w4b", "WhatsApp Business"))
        assertEquals("Telegram", SmartContentCleaner.resolveAppName("org.telegram.messenger", "Telegram"))
        SmartContentCleaner.setAppAlias("com.whatsapp.w4b", null)
        assertEquals("WhatsApp Business", SmartContentCleaner.resolveAppName("com.whatsapp.w4b", "WhatsApp Business"))
    }
}
