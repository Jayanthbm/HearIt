package com.jayanth.hearit.voicespeech

import com.jayanth.hearit.core.tts.AnnouncementFormat
import com.jayanth.hearit.core.tts.SpeechSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceSpeechUiStateTest {

    @Test
    fun defaultPreview_matchesMasterPromptDefault() {
        val state = VoiceSpeechUiState()
        assertEquals(
            "WhatsApp. Rahul says, Hey, are you coming today?",
            state.previewText,
        )
    }

    @Test
    fun preview_withoutAppName() {
        val state = VoiceSpeechUiState(
            format = AnnouncementFormat(includeAppName = false),
        )
        assertEquals(
            "Rahul says, Hey, are you coming today?",
            state.previewText,
        )
    }

    @Test
    fun preview_withoutMessage_usesPeriodForSenderInsteadOfSays() {
        val state = VoiceSpeechUiState(
            format = AnnouncementFormat(includeMessage = false),
        )
        assertEquals(
            "WhatsApp. Rahul.",
            state.previewText,
        )
    }

    @Test
    fun preview_withoutSender() {
        val state = VoiceSpeechUiState(
            format = AnnouncementFormat(includeSender = false),
        )
        assertEquals(
            "WhatsApp. Hey, are you coming today?",
            state.previewText,
        )
    }

    @Test
    fun preview_withIntroduction() {
        val state = VoiceSpeechUiState(
            format = AnnouncementFormat(introduction = "Attention"),
        )
        assertEquals(
            "Attention. WhatsApp. Rahul says, Hey, are you coming today?",
            state.previewText,
        )
    }

    @Test
    fun preview_allSectionsDisabled_returnsPlaceholder() {
        val state = VoiceSpeechUiState(
            format = AnnouncementFormat(
                includeAppName = false,
                includeSender = false,
                includeMessage = false,
                introduction = null,
            ),
        )
        assertEquals(
            "No sections enabled",
            state.previewText,
        )
    }

    @Test
    fun speechSettings_clampedDefaults() {
        val defaultSettings = SpeechSettings()
        assertEquals(1.0f, defaultSettings.speechRate)
        assertEquals(1.0f, defaultSettings.pitch)
    }
}
