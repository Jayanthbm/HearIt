package com.jayanth.hearit.dashboard

import com.jayanth.hearit.core.common.model.AudioOutputSettings
import com.jayanth.hearit.core.common.model.HistoryStatus
import com.jayanth.hearit.core.common.model.LockCondition
import com.jayanth.hearit.core.common.model.ScreenCondition
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
 * @property isPriorityAnnounceActive True while Priority Announce mode is on (P17/P19b).
 */
data class ServiceSnapshot(
    val isRunning: Boolean = false,
    val isSpeaking: Boolean = false,
    val pendingCount: Int = 0,
    val isPriorityAnnounceActive: Boolean = false,
    val priorityAnnounceExpiryMillis: Long? = null,
    val hasLastAnnouncement: Boolean = false,
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

/** How the history list is ordered (master prompt §12: newest/oldest). */
enum class DashboardSortBy {
    RecentFirst,
    OldestFirst,
    ByApp,
}

/**
 * Filter state for history search (P18).
 *
 * Holds the current query text and selected filter options; the ViewModel maps it onto
 * `HistoryQuery`, so the UI never builds SQL or knows about the database package.
 */
data class DashboardFilterState(
    val query: String = "",
    val filterPackageName: String? = null,
    val filterStatus: HistoryStatus? = null,
    val filterAppName: String? = null,
    val filterSender: String? = null,
    val filterDateFrom: Long? = null,
    val filterDateTo: Long? = null,
    val filterSortBy: DashboardSortBy = DashboardSortBy.RecentFirst,
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
 * @property enabledAppCount Apps the user enabled; the pipeline is silent while this is 0.
 * @property audioOutput Allowed output categories, shown as a summary row.
 * @property outputLabel Name of the first connected routed output, or null when none is
 *   connected right now.
 * @property temporaryMuteActive Mute override read from the session store.
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
    val temporaryMuteMinutes: Int? = null,
    val recentHistory: List<RecentAnnouncement> = emptyList(),
    val recentHistoryLoaded: Boolean = false,
    val historyUnavailable: Boolean = false,
    val totalAnnouncementsCount: Long = 0L,
    val todayAnnouncedCount: Long = 0L,
    val todaySkippedCount: Long = 0L,
    val screenCondition: ScreenCondition = ScreenCondition.OFF,
    val lockCondition: LockCondition = LockCondition.LOCKED,
    val testAnnouncement: TestAnnouncementState = TestAnnouncementState.IDLE,
    val testSpeechDismissed: Boolean = false,
    val isRepeatShortcutEnabled: Boolean = true,
    val showNoAudioOutputWarning: Boolean = false,
    val speechRate: Float = 1.0f,
    val speechVolume: Float = 1.0f,
    val isAdaptiveSpeedEnabled: Boolean = true,
    val isVoiceBoostEnabled: Boolean = false,
    val isBatteryOptimizationExempt: Boolean = true,
    val isBatteryWarningDismissed: Boolean = false,
    val lastStopReason: String? = null,
) {

    /** Master prompt §13 "Normal announcements ON/OFF": on exactly while the service runs. */
    val announcementsActive: Boolean get() = service.isRunning

    /** Priority Announce state (P17): on/off, owned by the service and its tile. */
    val priorityAnnounceActive: Boolean get() = service.isPriorityAnnounceActive
}
