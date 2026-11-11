package com.jayanth.hearit.settings

import com.jayanth.hearit.core.common.model.AudioFocusPolicy
import com.jayanth.hearit.core.common.model.QueueSettings
import com.jayanth.hearit.core.common.model.SecuritySettings

/**
 * UI State for the General Settings feature (Queue, Pacing, Eviction, Audio Focus, Service Notification, and Security).
 *
 * Master prompt §8 and §15, phase P15 and P19.
 */
data class GeneralSettingsUiState(
    val queue: QueueSettings = QueueSettings(),
    val audioFocus: AudioFocusPolicy = AudioFocusPolicy.DUCK,
    val isServiceNotificationVisible: Boolean = true,
    val security: SecuritySettings = SecuritySettings(),
    val isBiometricAvailable: Boolean = false,
    val securityErrorMessage: String? = null,
    val isAutoDismissEnabled: Boolean = true,
    val isShakeToStopEnabled: Boolean = true,
    val isVolumeKeyToStopEnabled: Boolean = true,
    val isMusicPauseHoldEnabled: Boolean = true,
    val isBluetoothAutoStartEnabled: Boolean = true,
    val isPeriodicCheckinEnabled: Boolean = false,
    val periodicCheckinIntervalMinutes: Int = 30,
    val periodicCheckinAnnounceTime: Boolean = true,
    val periodicCheckinAnnounceBattery: Boolean = true,
    val isUrlSummarizerEnabled: Boolean = true,
    val isCurrencyNormalizerEnabled: Boolean = true,
    val isMarkdownStripperEnabled: Boolean = true,
    val isEmojiCollapsingEnabled: Boolean = true,
    val isReactionFilterEnabled: Boolean = true,
    val isBotSenderCleaningEnabled: Boolean = true,
)
