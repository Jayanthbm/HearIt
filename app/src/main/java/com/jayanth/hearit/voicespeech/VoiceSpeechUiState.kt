package com.jayanth.hearit.voicespeech

import com.jayanth.hearit.core.tts.AnnouncementFormat
import com.jayanth.hearit.core.tts.SpeechSettings
import com.jayanth.hearit.core.tts.TtsVoice

/**
 * State of the Voice & Speech settings feature (#18, #24, #34).
 */
data class VoiceSpeechUiState(
    val speech: SpeechSettings = SpeechSettings(),
    val format: AnnouncementFormat = AnnouncementFormat(),
    val voices: List<TtsVoice> = emptyList(),
    val isTtsAvailable: Boolean = true,
    val isSpeaking: Boolean = false,
    // Persisted Smart Audio Polish (#18)
    val isEarconEnabled: Boolean = true,
    val earconDurationMs: Int = 120,
    val antiCollisionDelayMs: Long = 300L,
    val isAdaptiveSpeedEnabled: Boolean = true,
    val adaptiveSpeedTier1Multiplier: Float = AdaptiveSpeedCalculator.DEFAULT_TIER_1_MULTIPLIER,
    val adaptiveSpeedTier2Multiplier: Float = AdaptiveSpeedCalculator.DEFAULT_TIER_2_MULTIPLIER,
    val adaptiveSpeedTier3Multiplier: Float = AdaptiveSpeedCalculator.DEFAULT_TIER_3_MULTIPLIER,
    val isMonoFoldDownEnabled: Boolean = true,
    // Voice Loudness Boost (#24)
    val isVoiceBoostEnabled: Boolean = false,
    val voiceBoostPercent: Int = 20,
    // Ambient Noise Auto-Volume Boost (#34)
    val isAmbientNoiseBoostEnabled: Boolean = false,
    val ambientNoiseThresholdDb: Int = 70,
    val ambientNoiseBoostPercent: Int = 20,
    val hasRecordAudioPermission: Boolean = false,
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
