package com.jayanth.hearit.dashboard

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-process observation seam between the announcement service and the UI (P12).
 *
 * Architecture §6 asks the UI to observe service state as a hot flow; this is that flow.
 * It is deliberately **read-only for the UI**: only
 * [com.jayanth.hearit.service.AnnouncementService] calls the `mark*`/`updateSpeech`
 * methods, and the dashboard never infers a start from what it sees (master prompt §4:
 * no silent service start).
 *
 * Why not poll `ActivityManager`/`SessionStore` instead: a poll answers "was the service
 * ever started", not "is it running and speaking now", and `getRunningServices` is
 * deprecated and unreliable. A hot flow updated by the service itself is exact while the
 * process lives; after process death both sides reset together, so a dead service reads as
 * stopped — which is the truth the dashboard must show.
 *
 * Threading: safe to read from any thread (`StateFlow`); writers are the service's
 * main-thread loop only, so updates stay ordered with speech state.
 */
class ServiceStatusMonitor {

    private val _state = MutableStateFlow(ServiceSnapshot())

    /** Current service state; starts stopped until the service reports otherwise. */
    val state: StateFlow<ServiceSnapshot> = _state.asStateFlow()

    /** Called by the service once it is in the foreground. */
    fun markRunning() {
        _state.update { it.copy(isRunning = true) }
    }

    /** Called on explicit stop and in `onDestroy`; also clears speaking/queue state. */
    fun markStopped() {
        _state.value = ServiceSnapshot()
    }

    /**
     * Publishes speech progress after every tick.
     *
     * @param pendingCount negative values are clamped: the dashboard must never show a
     *   nonsensical queue depth even if a caller miscomputes it.
     */
    fun updateSpeech(isSpeaking: Boolean, pendingCount: Int) {
        _state.update {
            it.copy(isSpeaking = isSpeaking, pendingCount = pendingCount.coerceAtLeast(0))
        }
    }
}
