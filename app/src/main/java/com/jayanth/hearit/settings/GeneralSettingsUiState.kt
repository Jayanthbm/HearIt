package com.jayanth.hearit.settings

import com.jayanth.hearit.core.common.model.AudioFocusPolicy
import com.jayanth.hearit.core.common.model.QueueSettings

/**
 * UI State for the General Settings feature (Queue, Pacing, Eviction, and Audio Focus).
 *
 * Master prompt §8 and phase P15.
 */
data class GeneralSettingsUiState(
    val queue: QueueSettings = QueueSettings(),
    val audioFocus: AudioFocusPolicy = AudioFocusPolicy.DUCK,
)
