package com.jayanth.hearit.core.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmartContentCleanerTest {

    @Test
    fun cleanSender_sanitizesContactNameEmojisAndBrackets() {
        assertEquals("Rahul", SmartContentCleaner.cleanSender("Rahul 🏏🔥"))
        assertEquals("Dr. Sharma", SmartContentCleaner.cleanSender("Dr. Sharma (Cardio)"))
        assertEquals("Ananya", SmartContentCleaner.cleanSender("Ananya (Work)"))
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
    fun cleanMessageText_summarizesUrls() {
        val yt = "Check this out https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        assertEquals("Check this out YouTube video", SmartContentCleaner.cleanMessageText(yt))

        val insta = "See reel: https://instagram.com/reel/C3bA189zQ/?igsh=abc"
        assertEquals("See reel: Instagram reel", SmartContentCleaner.cleanMessageText(insta))

        val maps = "Meet here: https://maps.google.com/?q=12.97,77.59"
        assertEquals("Meet here: Google Maps location", SmartContentCleaner.cleanMessageText(maps))

        val generic = "Read article at https://news.bbc.co.uk/world/news-123"
        assertEquals("Read article at link to news.bbc.co.uk", SmartContentCleaner.cleanMessageText(generic))
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
    fun cleanMessageText_stripsMarkdownAndFormatting() {
        assertEquals("Important update now", SmartContentCleaner.cleanMessageText("**Important** update `now`"))
        assertEquals("Items: Milk Bread", SmartContentCleaner.cleanMessageText("Items:\n- Milk\n- Bread"))
    }

    @Test
    fun cleanMessageText_filtersTapBackReactionSpam() {
        val reaction = "Rahul reacted ❤️ to \"See you tomorrow\""
        assertEquals("Reacted to a message", SmartContentCleaner.cleanMessageText(reaction))
    }

    @Test
    fun cleanMessageText_convertsMediaTags() {
        assertEquals("Sent a photo", SmartContentCleaner.cleanMessageText("📷 Photo"))
        assertEquals("Sent a voice message", SmartContentCleaner.cleanMessageText("🎤 Voice message (0:45)"))
        assertEquals("Shared a location", SmartContentCleaner.cleanMessageText("📍 Location"))
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
