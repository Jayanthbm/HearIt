package com.jayanth.hearit.voicespeech

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayanth.hearit.core.common.model.NotificationSnapshot
import com.jayanth.hearit.core.datastore.SettingsRepository
import com.jayanth.hearit.core.tts.AnnouncementTemplate
import com.jayanth.hearit.core.tts.BuiltAnnouncement
import com.jayanth.hearit.core.tts.SpeechSettings
import com.jayanth.hearit.core.tts.TextToSpeechEngine
import com.jayanth.hearit.core.tts.TtsError
import com.jayanth.hearit.core.tts.SmartContentCleaner
import com.jayanth.hearit.core.tts.UtteranceListener
import com.jayanth.hearit.service.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing Voice & Speech settings (#18, #24, #34).
 */
@HiltViewModel
class VoiceSpeechViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val engine: TextToSpeechEngine,
    private val sessionStore: SessionStore,
) : ViewModel() {

    private val _state = MutableStateFlow(
        VoiceSpeechUiState(
            isEarconEnabled = sessionStore.isEarconEnabled,
            earconDurationMs = sessionStore.earconDurationMs,
            antiCollisionDelayMs = sessionStore.antiCollisionDelayMs,
            isAdaptiveSpeedEnabled = sessionStore.isAdaptiveSpeedEnabled,
            adaptiveSpeedTier1Multiplier = sessionStore.adaptiveSpeedTier1Multiplier,
            adaptiveSpeedTier2Multiplier = sessionStore.adaptiveSpeedTier2Multiplier,
            adaptiveSpeedTier3Multiplier = sessionStore.adaptiveSpeedTier3Multiplier,
            isVoiceBoostEnabled = sessionStore.isVoiceBoostEnabled,
            voiceBoostPercent = sessionStore.voiceBoostPercent,
            isDigitFormattingEnabled = sessionStore.isDigitFormattingEnabled,
            isFormatOtpCodesEnabled = sessionStore.isFormatOtpCodesEnabled,
            isFormatPhoneNumbersEnabled = sessionStore.isFormatPhoneNumbersEnabled,
        ),
    )
    val state: StateFlow<VoiceSpeechUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update { current ->
                    current.copy(
                        speech = settings.speech,
                        format = settings.format,
                        isOtpShieldEnabled = settings.eligibility.privacy.skipOtp,
                        voices = engine.listVoices(),
                        isTtsAvailable = engine.isAvailable(),
                    )
                }
            }
        }
        ensureTtsPolling()
    }

    private fun ensureTtsPolling() {
        if (engine.isAvailable()) return
        viewModelScope.launch {
            repeat(10) {
                delay(500L)
                if (engine.isAvailable()) {
                    _state.update { current ->
                        current.copy(
                            isTtsAvailable = true,
                            voices = engine.listVoices(),
                        )
                    }
                    return@launch
                }
            }
        }
    }

    fun onSpeechRateChanged(rate: Float) {
        val clamped = rate.coerceIn(0.5f, 2.0f)
        updateSpeech { it.copy(speechRate = clamped) }
    }

    fun onPitchChanged(pitch: Float) {
        val clamped = pitch.coerceIn(0.5f, 2.0f)
        updateSpeech { it.copy(pitch = clamped) }
    }

    fun onVolumeChanged(volume: Float) {
        val clamped = volume.coerceIn(0.0f, 1.0f)
        updateSpeech { it.copy(volumeLevel = clamped) }
    }

    fun onVoiceChanged(voiceId: String?) {
        updateSpeech { it.copy(voiceId = voiceId) }
    }

    fun onResetToDefaults() {
        if (_state.value.isSpeaking) {
            engine.stop()
        }
        sessionStore.isEarconEnabled = true
        sessionStore.earconDurationMs = 120
        sessionStore.antiCollisionDelayMs = 300L
        sessionStore.isAdaptiveSpeedEnabled = true
        sessionStore.adaptiveSpeedTier1Multiplier = AdaptiveSpeedCalculator.DEFAULT_TIER_1_MULTIPLIER
        sessionStore.adaptiveSpeedTier2Multiplier = AdaptiveSpeedCalculator.DEFAULT_TIER_2_MULTIPLIER
        sessionStore.adaptiveSpeedTier3Multiplier = AdaptiveSpeedCalculator.DEFAULT_TIER_3_MULTIPLIER
        sessionStore.isVoiceBoostEnabled = false
        sessionStore.voiceBoostPercent = 20
        _state.update {
            it.copy(
                isEarconEnabled = true,
                earconDurationMs = 120,
                antiCollisionDelayMs = 300L,
                isAdaptiveSpeedEnabled = true,
                adaptiveSpeedTier1Multiplier = AdaptiveSpeedCalculator.DEFAULT_TIER_1_MULTIPLIER,
                adaptiveSpeedTier2Multiplier = AdaptiveSpeedCalculator.DEFAULT_TIER_2_MULTIPLIER,
                adaptiveSpeedTier3Multiplier = AdaptiveSpeedCalculator.DEFAULT_TIER_3_MULTIPLIER,
                isVoiceBoostEnabled = false,
                voiceBoostPercent = 20,
            )
        }
        viewModelScope.launch {
            settingsRepository.update { current ->
                current.copy(speech = SpeechSettings())
            }
        }
    }

    fun onTestVoice() {
        if (_state.value.isSpeaking) {
            onStopVoice()
            return
        }

        val currentFormat = _state.value.format
        val currentSpeech = _state.value.speech

        val sampleSnapshot = NotificationSnapshot(
            key = "preview_test",
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            sender = "Rahul",
            messageText = "Hey, are you coming today?",
            postTimeMillis = System.currentTimeMillis(),
        )

        val announcement = AnnouncementTemplate.build(sampleSnapshot, currentFormat)
        if (announcement.isEmpty) return

        _state.update { it.copy(isSpeaking = true) }

        val accepted = engine.speak(
            announcement = announcement,
            settings = currentSpeech,
            listener = object : UtteranceListener {
                override fun onStart(announcement: BuiltAnnouncement) {
                    _state.update { it.copy(isSpeaking = true) }
                }

                override fun onComplete(announcement: BuiltAnnouncement) {
                    _state.update { it.copy(isSpeaking = false) }
                }

                override fun onError(announcement: BuiltAnnouncement, error: TtsError) {
                    _state.update { it.copy(isSpeaking = false) }
                }
            },
        )

        if (!accepted) {
            _state.update { it.copy(isSpeaking = false) }
        }
    }

    fun onStopVoice() {
        engine.stop()
        _state.update { it.copy(isSpeaking = false) }
    }

    fun onToggleEarcon(enabled: Boolean) {
        sessionStore.isEarconEnabled = enabled
        _state.update { it.copy(isEarconEnabled = enabled) }
    }

    fun onEarconDurationChanged(durationMs: Int) {
        sessionStore.earconDurationMs = durationMs
        _state.update { it.copy(earconDurationMs = durationMs) }
    }

    fun onAntiCollisionDelayChanged(delayMs: Long) {
        sessionStore.antiCollisionDelayMs = delayMs
        _state.update { it.copy(antiCollisionDelayMs = delayMs) }
    }

    fun onToggleAdaptiveSpeed(enabled: Boolean) {
        sessionStore.isAdaptiveSpeedEnabled = enabled
        _state.update { it.copy(isAdaptiveSpeedEnabled = enabled) }
    }

    fun onAdaptiveSpeedTier1Changed(multiplier: Float) {
        sessionStore.adaptiveSpeedTier1Multiplier = multiplier
        _state.update { it.copy(adaptiveSpeedTier1Multiplier = multiplier) }
    }

    fun onAdaptiveSpeedTier2Changed(multiplier: Float) {
        sessionStore.adaptiveSpeedTier2Multiplier = multiplier
        _state.update { it.copy(adaptiveSpeedTier2Multiplier = multiplier) }
    }

    fun onAdaptiveSpeedTier3Changed(multiplier: Float) {
        sessionStore.adaptiveSpeedTier3Multiplier = multiplier
        _state.update { it.copy(adaptiveSpeedTier3Multiplier = multiplier) }
    }

    fun onToggleVoiceBoost(enabled: Boolean) {
        sessionStore.isVoiceBoostEnabled = enabled
        _state.update { it.copy(isVoiceBoostEnabled = enabled) }
    }

    fun onVoiceBoostPercentChanged(percent: Int) {
        sessionStore.voiceBoostPercent = percent
        _state.update { it.copy(voiceBoostPercent = percent) }
    }

    fun onToggleDigitFormatting(enabled: Boolean) {
        sessionStore.isDigitFormattingEnabled = enabled
        SmartContentCleaner.isDigitFormattingEnabled = enabled
        _state.update { it.copy(isDigitFormattingEnabled = enabled) }
    }

    fun onToggleFormatOtpCodes(enabled: Boolean) {
        sessionStore.isFormatOtpCodesEnabled = enabled
        SmartContentCleaner.isFormatOtpCodesEnabled = enabled
        _state.update { it.copy(isFormatOtpCodesEnabled = enabled) }
    }

    fun onToggleFormatPhoneNumbers(enabled: Boolean) {
        sessionStore.isFormatPhoneNumbersEnabled = enabled
        SmartContentCleaner.isFormatPhoneNumbersEnabled = enabled
        _state.update { it.copy(isFormatPhoneNumbersEnabled = enabled) }
    }

    override fun onCleared() {
        super.onCleared()
        if (_state.value.isSpeaking) {
            engine.stop()
        }
    }

    private fun updateSpeech(transform: (SpeechSettings) -> SpeechSettings) {
        viewModelScope.launch {
            settingsRepository.update { current ->
                current.copy(speech = transform(current.speech))
            }
        }
    }

}
