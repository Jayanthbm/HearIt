package com.jayanth.hearit.settings

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayanth.hearit.core.common.model.AudioFocusPolicy
import com.jayanth.hearit.core.common.model.OverflowPolicy
import com.jayanth.hearit.core.common.model.QueueSettings
import com.jayanth.hearit.core.datastore.SettingsRepository
import com.jayanth.hearit.security.BiometricAuthManager
import com.jayanth.hearit.security.BiometricAuthResult
import com.jayanth.hearit.service.AnnouncementNotification
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing General Settings (Queue, Grouping, Pacing, Audio Focus, Service Notification, and Security).
 *
 * Single-writer pattern over [SettingsRepository] (ADR-021).
 */
@HiltViewModel
class GeneralSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val biometricAuthManager: BiometricAuthManager,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(
        GeneralSettingsUiState(
            isServiceNotificationVisible = AnnouncementNotification.isNotificationVisible(context),
            isBiometricAvailable = biometricAuthManager.canAuthenticate(),
        ),
    )
    val state: StateFlow<GeneralSettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update { current ->
                    current.copy(
                        queue = settings.queue,
                        audioFocus = settings.eligibility.audioFocus,
                        security = settings.security,
                        isBiometricAvailable = biometricAuthManager.canAuthenticate(),
                    )
                }
            }
        }
    }

    fun refreshServiceNotificationStatus() {
        val visible = AnnouncementNotification.isNotificationVisible(context)
        val canAuth = biometricAuthManager.canAuthenticate()
        _state.update {
            it.copy(
                isServiceNotificationVisible = visible,
                isBiometricAvailable = canAuth,
            )
        }
    }

    fun openServiceNotificationSettings() {
        AnnouncementNotification.openNotificationSettings(context)
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

    fun onToggleSecureScreen(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.update { current ->
                current.copy(
                    security = current.security.copy(secureScreenEnabled = enabled),
                )
            }
        }
    }

    fun onToggleBiometricLock(activity: Activity, enabled: Boolean) {
        if (enabled) {
            if (!biometricAuthManager.canAuthenticate()) {
                _state.update {
                    it.copy(securityErrorMessage = "Device lock (PIN, pattern, password, or biometric) is not set up on this device.")
                }
                return
            }
            biometricAuthManager.authenticate(
                activity = activity,
                title = "Enable Biometric Gate",
                subtitle = "Authenticate to confirm device ownership",
            ) { result ->
                when (result) {
                    is BiometricAuthResult.Success -> {
                        viewModelScope.launch {
                            settingsRepository.update { current ->
                                current.copy(
                                    security = current.security.copy(biometricLockEnabled = true),
                                    history = current.history.copy(requireAuth = true),
                                )
                            }
                        }
                        _state.update { it.copy(securityErrorMessage = null) }
                    }
                    is BiometricAuthResult.Cancelled -> {}
                    is BiometricAuthResult.Failed -> {
                        _state.update { it.copy(securityErrorMessage = result.error) }
                    }
                    is BiometricAuthResult.Unavailable -> {
                        _state.update { it.copy(securityErrorMessage = result.reason) }
                    }
                }
            }
        } else {
            biometricAuthManager.authenticate(
                activity = activity,
                title = "Disable Biometric Gate",
                subtitle = "Authenticate to turn off biometric protection",
            ) { result ->
                when (result) {
                    is BiometricAuthResult.Success, is BiometricAuthResult.Unavailable -> {
                        viewModelScope.launch {
                            settingsRepository.update { current ->
                                current.copy(
                                    security = current.security.copy(biometricLockEnabled = false),
                                    history = current.history.copy(requireAuth = false),
                                )
                            }
                        }
                        _state.update { it.copy(securityErrorMessage = null) }
                    }
                    is BiometricAuthResult.Cancelled -> {}
                    is BiometricAuthResult.Failed -> {
                        _state.update { it.copy(securityErrorMessage = result.error) }
                    }
                }
            }
        }
    }

    fun onClearSecurityErrorMessage() {
        _state.update { it.copy(securityErrorMessage = null) }
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
