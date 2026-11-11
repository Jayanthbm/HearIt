package com.jayanth.hearit.setup

import com.jayanth.hearit.core.common.model.AudioOutputSettings
import com.jayanth.hearit.core.setup.SetupBlocker
import com.jayanth.hearit.core.setup.SetupPrerequisites
import com.jayanth.hearit.core.setup.SetupStep

/** One selectable app in the setup app-selection step (full catalog arrives in P13). */
data class InstalledApp(
    val packageName: String,
    val label: String,
)

/**
 * One audio output observed right now, shown only for orientation in setup.
 *
 * Per-device routing and persistence belong to the Devices feature (P14); setup only lets
 * the user allow the wired/Bluetooth/speaker **categories** so announcements can start.
 */
data class DetectedOutput(
    val id: String?,
    val label: String,
    val isBluetooth: Boolean,
)

/** Progress of the on-demand test announcement (never recorded in history or the queue). */
enum class TestAnnouncementState {
    IDLE,
    SPEAKING,
    COMPLETED,
    FAILED,
}

/**
 * Immutable state of the setup flow, owned by `SetupViewModel` and rendered by
 * `SetupScreen`.
 *
 * Every readiness decision is computed by `:core:setup`'s `SetupFlow`; this type only
 * carries its answers so the composables never decide policy (global standards: no business
 * logic in composables).
 *
 * @property loaded False until the first persisted state and settings read complete.
 * @property setupCompleted True once the user pressed Start; the app then opens the
 *   dashboard (placeholder until P12).
 * @property step Step currently on screen, already resolved to a visible step.
 * @property visibleSteps Steps shown on this device, in order (Bluetooth hidden below API 31).
 * @property blockers Unmet prerequisites of [step], for the explanatory copy.
 * @property canAdvance True when the forward action is enabled (prerequisite met, or the
 *   step is skippable).
 * @property canOnlySkip True when the only way forward is skipping.
 * @property canGoBack False on the first visible step.
 * @property canStart True only when every Start prerequisite holds (review step).
 * @property startBlockers Reasons Start is unavailable, for the review step.
 * @property skippedSteps Steps the user skipped on this device.
 * @property apps Installed launcher apps offered for selection.
 * @property enabledApps Packages enabled for announcements (empty on a fresh install).
 * @property outputs Audio outputs connected right now (display only).
 * @property audioOutputSettings Allowed output categories, editable in setup.
 * @property batteryGuidanceAcknowledged Whether the user confirmed the background guidance.
 * @property notificationPermissionRequired True on API 33+, where the persistent
 *   notification needs `POST_NOTIFICATIONS`.
 * @property notificationPermissionGranted Current `POST_NOTIFICATIONS` state.
 * @property testAnnouncement Progress of the on-demand test announcement.
 * @property stepIndex One-based position of [step] in [visibleSteps], for "Step X of Y".
 * @property stepCount Number of visible steps.
 */
data class SetupUiState(
    val loaded: Boolean = false,
    val setupCompleted: Boolean = false,
    val step: SetupStep = SetupStep.WELCOME,
    val visibleSteps: List<SetupStep> = SetupStep.ordered,
    val prerequisites: SetupPrerequisites = SetupPrerequisites(),
    val blockers: List<SetupBlocker> = emptyList(),
    val canAdvance: Boolean = false,
    val canOnlySkip: Boolean = false,
    val canGoBack: Boolean = false,
    val canStart: Boolean = false,
    val startBlockers: List<SetupBlocker> = emptyList(),
    val skippedSteps: Set<SetupStep> = emptySet(),
    val apps: List<InstalledApp> = emptyList(),
    val appsLoading: Boolean = true,
    val enabledApps: Set<String> = emptySet(),
    val outputs: List<DetectedOutput> = emptyList(),
    val audioOutputSettings: AudioOutputSettings = AudioOutputSettings(),
    val batteryGuidanceAcknowledged: Boolean = false,
    val notificationPermissionRequired: Boolean = false,
    val notificationPermissionGranted: Boolean = false,
    val testAnnouncement: TestAnnouncementState = TestAnnouncementState.IDLE,
    val stepIndex: Int = 1,
    val stepCount: Int = SetupStep.ordered.size,
)
