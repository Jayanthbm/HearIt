package com.jayanth.hearit.core.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * Contract invariants of the `:core:tts` boundary (master prompt §7, ADR-018): speech
 * defaults match the master prompt, invalid settings fail fast on any platform, and the
 * built announcement reports emptiness so the speaker can skip gracefully.
 */
class TextToSpeechContractTest {

    @Test
    fun speechSettings_defaults_matchMasterPrompt() {
        // §7: normal speed and pitch, engine's default voice — nothing configured.
        assertEquals(SpeechSettings(speechRate = 1.0f, pitch = 1.0f, voiceId = null), SpeechSettings())
    }

    @Test
    fun speechSettings_zeroRate_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            SpeechSettings(speechRate = 0f)
        }
    }

    @Test
    fun speechSettings_negativePitch_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            SpeechSettings(pitch = -0.5f)
        }
    }

    @Test
    fun speechSettings_customValues_arePreserved() {
        val settings = SpeechSettings(speechRate = 1.5f, pitch = 0.9f, voiceId = "voice-42")
        assertEquals(1.5f, settings.speechRate, 0.0f)
        assertEquals(0.9f, settings.pitch, 0.0f)
        assertEquals("voice-42", settings.voiceId)
    }

    @Test
    fun builtAnnouncement_negativePause_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            BuiltAnnouncement(emptyList(), -1L)
        }
    }

    @Test
    fun builtAnnouncement_noSections_isEmpty() {
        val announcement = BuiltAnnouncement(emptyList(), pauseBetweenSectionsMillis = 300L)
        assertTrue(announcement.isEmpty)
    }

    @Test
    fun builtAnnouncement_withSections_isNotEmpty() {
        val announcement = BuiltAnnouncement(
            sections = listOf(AnnouncementSection(SectionKind.APP_NAME, "WhatsApp.")),
            pauseBetweenSectionsMillis = 0L,
        )
        assertFalse(announcement.isEmpty)
    }

    @Test
    fun ttsVoice_networkRequirement_defaultsToFalse() {
        val voice = TtsVoice(id = "v1", displayName = "English (US)", locale = Locale.US)
        assertFalse(voice.isNetworkConnectionRequired)
    }
}
