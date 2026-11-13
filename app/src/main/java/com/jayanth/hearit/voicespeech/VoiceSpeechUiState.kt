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
    // Digit-by-digit reading (#12) - moved here from Active Intelligence
    val isDigitFormattingEnabled: Boolean = true,
    val isFormatOtpCodesEnabled: Boolean = true,
    val isFormatPhoneNumbersEnabled: Boolean = true,
    /** Global Rules "OTP & Verification Codes" shield; it drops OTP alerts before speech. */
    val isOtpShieldEnabled: Boolean = false,
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
