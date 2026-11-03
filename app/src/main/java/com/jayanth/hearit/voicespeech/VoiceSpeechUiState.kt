package com.jayanth.hearit.voicespeech

import com.jayanth.hearit.core.tts.AnnouncementFormat
import com.jayanth.hearit.core.tts.SpeechSettings
import com.jayanth.hearit.core.tts.TtsVoice

/**
 * State of the Voice & Speech settings feature (master prompt §7, phase P15).
 */
data class VoiceSpeechUiState(
    val speech: SpeechSettings = SpeechSettings(),
    val format: AnnouncementFormat = AnnouncementFormat(),
    val voices: List<TtsVoice> = emptyList(),
    val isTtsAvailable: Boolean = true,
    val isSpeaking: Boolean = false,
) {
    /** Human-readable live preview of what the current format will speak. */
    val previewText: String
        get() {
            val parts = mutableListOf<String>()
            if (!format.introduction.isNullOrBlank()) {
                parts.add("${format.introduction}.")
            }
            if (format.includeAppName) {
                parts.add("WhatsApp.")
            }
            if (format.includeSender) {
                if (format.includeMessage) {
                    parts.add("Rahul says,")
                } else {
                    parts.add("Rahul.")
                }
            }
            if (format.includeMessage) {
                parts.add("Hey, are you coming today?")
            }
            return if (parts.isEmpty()) "No sections enabled" else parts.joinToString(" ")
        }
}
