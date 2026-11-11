package com.jayanth.hearit.voicespeech

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayanth.hearit.core.common.model.NotificationSnapshot
import com.jayanth.hearit.core.datastore.SettingsRepository
import com.jayanth.hearit.core.tts.AnnouncementFormat
import com.jayanth.hearit.core.tts.AnnouncementTemplate
import com.jayanth.hearit.core.tts.BuiltAnnouncement
import com.jayanth.hearit.core.tts.SpeechSettings
import com.jayanth.hearit.core.tts.TextToSpeechEngine
import com.jayanth.hearit.core.tts.TtsError
import com.jayanth.hearit.core.tts.UtteranceListener
import com.jayanth.hearit.service.AmbientNoiseSampler
import com.jayanth.hearit.service.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(
        VoiceSpeechUiState(
            isEarconEnabled = sessionStore.isEarconEnabled,
            earconDurationMs = sessionStore.earconDurationMs,
            antiCollisionDelayMs = sessionStore.antiCollisionDelayMs,
            isAdaptiveSpeedEnabled = sessionStore.isAdaptiveSpeedEnabled,
            adaptiveSpeedWordThreshold = sessionStore.adaptiveSpeedWordThreshold,
            adaptiveSpeedMultiplier = sessionStore.adaptiveSpeedMultiplier,
            isMonoFoldDownEnabled = sessionStore.isMonoFoldDownEnabled,
            isVoiceBoostEnabled = sessionStore.isVoiceBoostEnabled,
            voiceBoostPercent = sessionStore.voiceBoostPercent,
            isAmbientNoiseBoostEnabled = sessionStore.isAmbientNoiseBoostEnabled &&
                AmbientNoiseSampler.hasRecordAudioPermission(context),
            ambientNoiseThresholdDb = sessionStore.ambientNoiseThresholdDb,
            ambientNoiseBoostPercent = sessionStore.ambientNoiseBoostPercent,
            hasRecordAudioPermission = AmbientNoiseSampler.hasRecordAudioPermission(context),
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

    fun onIncludeAppNameChanged(include: Boolean) {
        updateFormat { it.copy(includeAppName = include) }
    }

    fun onIncludeSenderChanged(include: Boolean) {
        updateFormat { it.copy(includeSender = include) }
    }

    fun onIncludeMessageChanged(include: Boolean) {
        updateFormat { it.copy(includeMessage = include) }
    }

    fun onIntroductionChanged(introduction: String?) {
        val normalized = introduction?.trim()?.takeIf { it.isNotEmpty() }
        updateFormat { it.copy(introduction = normalized) }
    }

    fun onPauseDurationChanged(pauseMillis: Long) {
        val valid = pauseMillis.coerceAtLeast(0L)
        updateFormat { it.copy(pauseBetweenSectionsMillis = valid) }
    }

    fun onResetToDefaults() {
        if (_state.value.isSpeaking) {
            engine.stop()
        }
        sessionStore.isEarconEnabled = true
        sessionStore.earconDurationMs = 120
        sessionStore.antiCollisionDelayMs = 300L
        sessionStore.isAdaptiveSpeedEnabled = true
        sessionStore.adaptiveSpeedWordThreshold = 30
        sessionStore.adaptiveSpeedMultiplier = 1.25f
        sessionStore.isMonoFoldDownEnabled = true
        sessionStore.isVoiceBoostEnabled = false
        sessionStore.voiceBoostPercent = 20
        sessionStore.isAmbientNoiseBoostEnabled = false
        sessionStore.ambientNoiseThresholdDb = 70
        sessionStore.ambientNoiseBoostPercent = 20
        _state.update {
            it.copy(
                isEarconEnabled = true,
                earconDurationMs = 120,
                antiCollisionDelayMs = 300L,
                isAdaptiveSpeedEnabled = true,
                adaptiveSpeedWordThreshold = 30,
                adaptiveSpeedMultiplier = 1.25f,
                isMonoFoldDownEnabled = true,
                isVoiceBoostEnabled = false,
                voiceBoostPercent = 20,
                isAmbientNoiseBoostEnabled = false,
                ambientNoiseThresholdDb = 70,
                ambientNoiseBoostPercent = 20,
            )
        }
        viewModelScope.launch {
            settingsRepository.update { current ->
                current.copy(
                    speech = SpeechSettings(),
                    format = AnnouncementFormat(),
                )
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

    fun onAdaptiveSpeedWordThresholdChanged(words: Int) {
        sessionStore.adaptiveSpeedWordThreshold = words
        _state.update { it.copy(adaptiveSpeedWordThreshold = words) }
    }

    fun onAdaptiveSpeedMultiplierChanged(multiplier: Float) {
        sessionStore.adaptiveSpeedMultiplier = multiplier
        _state.update { it.copy(adaptiveSpeedMultiplier = multiplier) }
    }

    fun onToggleMonoFoldDown(enabled: Boolean) {
        sessionStore.isMonoFoldDownEnabled = enabled
        _state.update { it.copy(isMonoFoldDownEnabled = enabled) }
    }

    fun onToggleVoiceBoost(enabled: Boolean) {
        sessionStore.isVoiceBoostEnabled = enabled
        _state.update { it.copy(isVoiceBoostEnabled = enabled) }
    }

    fun onVoiceBoostPercentChanged(percent: Int) {
        sessionStore.voiceBoostPercent = percent
        _state.update { it.copy(voiceBoostPercent = percent) }
    }

    fun refreshRecordAudioPermission() {
        val granted = AmbientNoiseSampler.hasRecordAudioPermission(context)
        _state.update { current ->
            current.copy(
                hasRecordAudioPermission = granted,
                isAmbientNoiseBoostEnabled = current.isAmbientNoiseBoostEnabled && granted,
            )
        }
    }

    fun onToggleAmbientNoiseBoost(enabled: Boolean) {
        val hasPerm = AmbientNoiseSampler.hasRecordAudioPermission(context)
        val effective = enabled && hasPerm
        sessionStore.isAmbientNoiseBoostEnabled = effective
        _state.update {
            it.copy(
                hasRecordAudioPermission = hasPerm,
                isAmbientNoiseBoostEnabled = effective,
            )
        }
    }

    fun onAmbientNoiseThresholdChanged(db: Int) {
        sessionStore.ambientNoiseThresholdDb = db
        _state.update { it.copy(ambientNoiseThresholdDb = db) }
    }

    fun onAmbientNoiseBoostPercentChanged(percent: Int) {
        sessionStore.ambientNoiseBoostPercent = percent
        _state.update { it.copy(ambientNoiseBoostPercent = percent) }
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

    private fun updateFormat(transform: (AnnouncementFormat) -> AnnouncementFormat) {
        viewModelScope.launch {
            settingsRepository.update { current ->
                current.copy(format = transform(current.format))
            }
        }
    }
}
