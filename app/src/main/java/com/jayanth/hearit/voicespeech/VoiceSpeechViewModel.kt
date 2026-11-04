package com.jayanth.hearit.voicespeech

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayanth.hearit.core.common.model.NotificationSnapshot
import com.jayanth.hearit.core.datastore.HearItSettings
import com.jayanth.hearit.core.datastore.SettingsRepository
import com.jayanth.hearit.core.tts.AnnouncementFormat
import com.jayanth.hearit.core.tts.AnnouncementTemplate
import com.jayanth.hearit.core.tts.BuiltAnnouncement
import com.jayanth.hearit.core.tts.SpeechSettings
import com.jayanth.hearit.core.tts.TextToSpeechEngine
import com.jayanth.hearit.core.tts.TtsError
import com.jayanth.hearit.core.tts.UtteranceListener
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing Voice & Speech settings (master prompt §7, phase P15).
 *
 * Persists all speech tuning and announcement format changes through [SettingsRepository].
 * Provides live speech preview testing directly via [TextToSpeechEngine] without polluting
 * the notification queue or announcement history (master prompt §14).
 */
@HiltViewModel
class VoiceSpeechViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val engine: TextToSpeechEngine,
) : ViewModel() {

    private val _state = MutableStateFlow(VoiceSpeechUiState())
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
