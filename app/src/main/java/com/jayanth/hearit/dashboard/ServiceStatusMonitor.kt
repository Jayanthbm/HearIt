package com.jayanth.hearit.dashboard

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-process observation seam between the announcement service and the UI (#4, #25).
 */
class ServiceStatusMonitor {

    private val _state = MutableStateFlow(ServiceSnapshot())

    val state: StateFlow<ServiceSnapshot> = _state.asStateFlow()

    val isPriorityAnnounceActive: Boolean
        get() = _state.value.isPriorityAnnounceActive

    fun markRunning() {
        _state.update { it.copy(isRunning = true) }
    }

    fun markStopped() {
        _state.value = ServiceSnapshot()
    }

    fun updateSpeech(
        isSpeaking: Boolean,
        pendingCount: Int,
        hasLastAnnouncement: Boolean = _state.value.hasLastAnnouncement,
    ) {
        _state.update {
            it.copy(
                isSpeaking = isSpeaking,
                pendingCount = pendingCount.coerceAtLeast(0),
                hasLastAnnouncement = hasLastAnnouncement,
            )
        }
    }

    fun markPriorityAnnounceActive(
        active: Boolean,
        expiryWallClockMillis: Long? = null,
    ) {
        _state.update {
            it.copy(
                isPriorityAnnounceActive = active,
                priorityAnnounceExpiryMillis = if (active) expiryWallClockMillis else null,
            )
        }
    }
}
