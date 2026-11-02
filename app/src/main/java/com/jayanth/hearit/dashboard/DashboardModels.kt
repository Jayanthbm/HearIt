package com.jayanth.hearit.dashboard

import com.jayanth.hearit.core.common.model.AudioOutputSettings
import com.jayanth.hearit.core.common.model.HistoryStatus
import com.jayanth.hearit.setup.TestAnnouncementState

/**
 * What the announcement service is doing right now (master prompt §13: "Service
 * Active/Inactive", "Current speaking item and queue status").
 *
 * Deliberately content-free: the dashboard is the most screen-visible surface, so it learns
 * **that** something is being read and **how many** announcements wait, never the sender or
 * message text. Nothing here is logged.
 *
 * @property isRunning True while [com.jayanth.hearit.service.AnnouncementService] is alive.
 * @property isSpeaking True while an announcement is being spoken.
 * @property pendingCount Items waiting in the queue (call-held items are excluded, as in
 *   the queue's own `pendingCount`).
 */
data class ServiceSnapshot(
    val isRunning: Boolean = false,
    val isSpeaking: Boolean = false,
    val pendingCount: Int = 0,
)

/**
 * One recent history row as the dashboard shows it.
 *
 * No message text or sender on purpose: the dashboard lists app, outcome and time, and the
 * history feature (P18) owns content display. That keeps the summary safe on a visible
 * screen even when full-content history is enabled.
 */
data class RecentAnnouncement(
    val id: Long,
    val appName: String,
    val status: HistoryStatus,
    val recordedAtMillis: Long,
)

/**
 * Immutable state of the dashboard shell.
 *
 * Carries answers only — every decision (which destination, what to enable, what a status
 * means) is made by the ViewModel, the destination enum or `:core:*`; composables just
 * render (global standards: no business logic in composables).
 *
 * @property loaded False until the first settings emission arrives.
 * @property service Live service state from [ServiceStatusMonitor].
 * @property enabledAppCount Apps the user enabled; the pipeline is silent while this is 0
 *   (`SettingsSource` placeholder until P13).
 * @property audioOutput Allowed output categories, shown as a summary row.
 * @property outputLabel Name of the first connected routed output, or null when none is
 *   connected right now.
 * @property temporaryMuteActive Mute override read from the session store; P16 owns the
 *   control that changes it.
 * @property recentHistory Newest history rows (at most [DashboardViewModel.RECENT_HISTORY_LIMIT]).
 * @property recentHistoryLoaded False until the first history read finishes.
 * @property historyUnavailable True when the history read failed; the section says so
 *   instead of showing a misleading empty list.
 * @property testAnnouncement Progress of the on-demand test announcement.
 */
data class DashboardUiState(
    val loaded: Boolean = false,
    val service: ServiceSnapshot = ServiceSnapshot(),
    val enabledAppCount: Int = 0,
    val audioOutput: AudioOutputSettings = AudioOutputSettings(),
    val outputLabel: String? = null,
    val temporaryMuteActive: Boolean = false,
    val recentHistory: List<RecentAnnouncement> = emptyList(),
    val recentHistoryLoaded: Boolean = false,
    val historyUnavailable: Boolean = false,
    val testAnnouncement: TestAnnouncementState = TestAnnouncementState.IDLE,
) {

    /** Master prompt §13 "Normal announcements ON/OFF": on exactly while the service runs. */
    val announcementsActive: Boolean get() = service.isRunning

    /**
     * Master prompt §13 "Priority Announce ON/OFF and remaining duration".
     *
     * Always false today: Priority Announce's toggle, expiry and tile belong to P17
     * (ADR-024). The dashboard shows the honest current value rather than a dead toggle, and
     * this property is the single place P17 replaces.
     */
    val priorityAnnounceActive: Boolean get() = false
}
