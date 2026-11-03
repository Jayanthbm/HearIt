package com.jayanth.hearit.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayanth.hearit.core.common.model.AudioFocusPolicy
import com.jayanth.hearit.core.common.model.OverflowPolicy
import com.jayanth.hearit.core.common.model.QueueSettings
import com.jayanth.hearit.core.datastore.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing General Settings (Queue, Grouping, Pacing, and Audio Focus).
 *
 * Single-writer pattern over [SettingsRepository] (ADR-021).
 */
@HiltViewModel
class GeneralSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(GeneralSettingsUiState())
    val state: StateFlow<GeneralSettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update { current ->
                    current.copy(
                        queue = settings.queue,
                        audioFocus = settings.eligibility.audioFocus,
                    )
                }
            }
        }
    }

    fun onGroupingWindowChanged(millis: Long) {
        val valid = millis.coerceAtLeast(0L)
        updateQueue { it.copy(groupingWindowMillis = valid) }
    }

    fun onMaxMessagesChanged(max: Int) {
        val valid = max.coerceAtLeast(1)
        updateQueue { it.copy(maxMessagesPerAnnouncement = valid) }
    }

    fun onDelayChanged(millis: Long) {
        val valid = millis.coerceAtLeast(0L)
        updateQueue { it.copy(delayBeforeAnnouncementMillis = valid) }
    }

    fun onMinGapChanged(millis: Long) {
        val valid = millis.coerceAtLeast(0L)
        updateQueue { it.copy(minGapBetweenAnnouncementsMillis = valid) }
    }

    fun onExpiryChanged(millis: Long) {
        val valid = millis.coerceAtLeast(60_000L)
        updateQueue { it.copy(expiryMillis = valid) }
    }

    fun onGlobalMaxSizeChanged(max: Int) {
        val valid = max.coerceAtLeast(1)
        updateQueue { it.copy(globalMaxSize = valid) }
    }

    fun onPerAppMaxSizeChanged(max: Int) {
        val valid = max.coerceAtLeast(1)
        updateQueue { it.copy(perAppMaxSize = valid) }
    }

    fun onOverflowPolicyChanged(policy: OverflowPolicy) {
        updateQueue { it.copy(overflowPolicy = policy) }
    }

    fun onRepeatNotificationUpdatesChanged(repeat: Boolean) {
        updateQueue { it.copy(repeatNotificationUpdates = repeat) }
    }

    fun onAudioFocusPolicyChanged(policy: AudioFocusPolicy) {
        viewModelScope.launch {
            settingsRepository.update { current ->
                current.copy(
                    eligibility = current.eligibility.copy(audioFocus = policy),
                )
            }
        }
    }

    fun onResetToDefaults() {
        viewModelScope.launch {
            settingsRepository.update { current ->
                current.copy(
                    queue = QueueSettings(),
                    eligibility = current.eligibility.copy(audioFocus = AudioFocusPolicy.DUCK),
                )
            }
        }
    }

    private fun updateQueue(transform: (QueueSettings) -> QueueSettings) {
        viewModelScope.launch {
            settingsRepository.update { current ->
                current.copy(queue = transform(current.queue))
            }
        }
    }
}
