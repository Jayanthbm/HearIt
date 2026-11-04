package com.jayanth.hearit.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayanth.hearit.core.common.model.AudioOutputSettings
import com.jayanth.hearit.core.datastore.HearItSettings
import com.jayanth.hearit.core.datastore.SettingsRepository
import com.jayanth.hearit.core.datastore.SetupStateRepository
import com.jayanth.hearit.core.setup.SetupBlocker
import com.jayanth.hearit.core.setup.SetupFlow
import com.jayanth.hearit.core.setup.SetupPrerequisites
import com.jayanth.hearit.core.setup.SetupState
import com.jayanth.hearit.core.setup.SetupStep
import com.jayanth.hearit.core.tts.AnnouncementSection
import com.jayanth.hearit.core.tts.BuiltAnnouncement
import com.jayanth.hearit.core.tts.SectionKind
import com.jayanth.hearit.core.tts.SpeechSettings
import com.jayanth.hearit.core.tts.TextToSpeechEngine
import com.jayanth.hearit.core.tts.TtsError
import com.jayanth.hearit.core.tts.UtteranceListener
import com.jayanth.hearit.service.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the first-launch setup flow (master prompt §4, phase P11).
 *
 * All policy lives in `:core:setup`'s [SetupFlow]; this class only
 *  - reads the persisted [com.jayanth.hearit.core.setup.SetupState] and the settings,
 *  - gathers facts through the probes and the TTS engine,
 *  - persists navigation/skip/acknowledgement decisions,
 *  - and performs the explicit Start: complete the flow, persist the session flag and start
 *    the foreground service. Nothing here starts the service on its own — the Start button
 *    is the only path (P11 acceptance: "no silent service start").
 *
 * Privacy: no notification content is read, stored or logged here; the test announcement is
 * a fixed string supplied by the screen.
 */
@HiltViewModel
class SetupViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val setupStateRepository: SetupStateRepository,
    private val sessionStore: SessionStore,
    private val notificationAccess: NotificationAccessProbe,
    private val postNotifications: PostNotificationProbe,
    private val bluetoothPermission: BluetoothPermissionProbe,
    private val appCatalog: AppCatalogProbe,
    private val audioOutputs: AudioOutputProbe,
    private val launcher: SetupLauncher,
    private val engine: TextToSpeechEngine,
) : ViewModel() {

    private val _state = MutableStateFlow(SetupUiState())
    val state: StateFlow<SetupUiState> = _state.asStateFlow()

    private var ttsPollJob: Job? = null

    init {
        viewModelScope.launch {
            reload()
            loadApps()
            loadOutputs()
            ensureTtsPolling()
        }
    }

    /** Re-reads everything the flow gates on; called when the screen resumes. */
    fun onRefresh() {
        viewModelScope.launch {
            reload()
            loadOutputs()
            ensureTtsPolling()
        }
    }

    /** Advances to the next visible step, recording a skip when the step stays unmet. */
    fun onNext() {
        viewModelScope.launch {
            val current = _state.value
            val prerequisites = current.prerequisites
            val next = SetupFlow.nextStep(current.step, prerequisites) ?: return@launch
            val steppingWithoutMeeting = SetupFlow.blockersFor(current.step, prerequisites).isNotEmpty()

            setupStateRepository.update { setup ->
                val afterSkip = if (steppingWithoutMeeting && current.step.isSkippable) {
                    setup.skipping(current.step)
                } else {
                    setup
                }
                afterSkip.visiting(next)
            }
            reload()
        }
    }

    /** Records the current step as skipped (only offered for skippable steps). */
    fun onSkip() {
        viewModelScope.launch {
            val current = _state.value
            val prerequisites = current.prerequisites
            val next = SetupFlow.nextStep(current.step, prerequisites) ?: return@launch
            setupStateRepository.update { setup -> setup.skipping(current.step).visiting(next) }
            reload()
        }
    }

    /** Returns to the previous visible step; state is persisted so process death is safe. */
    fun onBack() {
        viewModelScope.launch {
            val current = _state.value
            val previous = SetupFlow.previousStep(current.step, current.prerequisites) ?: return@launch
            setupStateRepository.update { setup -> setup.visiting(previous) }
            reload()
        }
    }

    /** Enables/disables one app; every app starts disabled (master prompt §5). */
    fun onToggleApp(packageName: String) {
        viewModelScope.launch {
            settingsRepository.update { settings ->
                val enabled = if (packageName in settings.enabledApps) {
                    settings.enabledApps - packageName
                } else {
                    settings.enabledApps + packageName
                }
                settings.copy(enabledApps = enabled)
            }
            reload()
        }
    }

    /** Persists allowed output categories from the audio-device step (P14 owns devices). */
    fun onAudioOutputSettingsChanged(audioOutput: AudioOutputSettings) {
        viewModelScope.launch {
            settingsRepository.update { settings ->
                settings.copy(eligibility = settings.eligibility.copy(audioOutput = audioOutput))
            }
            reload()
        }
    }

    /** Records that the user read and accepted the background-execution guidance. */
    fun onBatteryGuidanceAcknowledged() {
        viewModelScope.launch {
            setupStateRepository.update { setup -> setup.copy(batteryGuidanceAcknowledged = true) }
            reload()
        }
    }

    /** Called after a runtime-permission dialog or a return from system settings. */
    fun onPermissionsChanged() {
        viewModelScope.launch {
            reload()
            loadOutputs()
            ensureTtsPolling()
        }
    }

    /** Opens the system notification-access list. */
    fun onOpenNotificationAccessSettings() = launcher.openNotificationAccessSettings()

    /** Opens the battery-optimisation list (guidance only, no promise of exemption). */
    fun onOpenBatterySettings() = launcher.openBatteryOptimizationSettings()

    /**
     * Speaks the fixed test announcement through the current engine and settings.
     *
     * Bypasses the queue and history on purpose (master prompt §4 step 8 / acceptance: the
     * test announcement must not create a history entry or touch the queue).
     */
    fun onTestAnnouncement(text: String) {
        val announcement = BuiltAnnouncement(
            sections = listOf(AnnouncementSection(SectionKind.MESSAGE, text)),
            pauseBetweenSectionsMillis = 0L,
        )
        _state.update { it.copy(testAnnouncement = TestAnnouncementState.SPEAKING) }

        val accepted = engine.speak(
            announcement = announcement,
            settings = SpeechSettings(),
            listener = object : UtteranceListener {
                override fun onComplete(announcement: BuiltAnnouncement) {
                    _state.update { it.copy(testAnnouncement = TestAnnouncementState.COMPLETED) }
                }

                override fun onError(announcement: BuiltAnnouncement, error: TtsError) {
                    _state.update { it.copy(testAnnouncement = TestAnnouncementState.FAILED) }
                }
            },
        )
        if (!accepted) {
            _state.update { it.copy(testAnnouncement = TestAnnouncementState.FAILED) }
        }
    }

    /**
     * The explicit Start (master prompt §4, P11 acceptance): re-checks [SetupFlow.canStart],
     * marks setup complete, persists the session flag **before** starting the service and
     * then starts it. Re-checking here means a revoked permission or a removed app cannot
     * start announcements.
     */
    fun onStart() {
        viewModelScope.launch {
            reload()
            if (!_state.value.canStart) return@launch

            setupStateRepository.update { setup -> setup.completing() }
            sessionStore.isStarted = true
            launcher.startAnnouncements()
            reload()
        }
    }

    // ---- internals -------------------------------------------------------------------

    private suspend fun reload() {
        render(setupStateRepository.current(), settingsRepository.current())
        ensureTtsPolling()
    }

    private fun render(setup: SetupState, settings: HearItSettings) {
        val prerequisites = readPrerequisites(settings)
        val step = SetupFlow.resolveVisibleStep(setup.currentStep, prerequisites)
        val visibleSteps = SetupFlow.visibleSteps(prerequisites)

        val baseStartBlockers = SetupFlow.startBlockers(prerequisites)
        val startBlockers = if (SetupStep.APP_SELECTION in setup.skippedSteps || settings.enabledApps.isEmpty()) {
            baseStartBlockers.filterNot { it is SetupBlocker.NoEnabledApps }
        } else {
            baseStartBlockers
        }

        _state.update { current ->
            current.copy(
                loaded = true,
                setupCompleted = setup.completed,
                step = step,
                visibleSteps = visibleSteps,
                stepIndex = visibleSteps.indexOf(step) + 1,
                stepCount = visibleSteps.size,
                prerequisites = prerequisites,
                blockers = SetupFlow.blockersFor(step, prerequisites),
                canAdvance = SetupFlow.canAdvanceFrom(step, prerequisites),
                canOnlySkip = SetupFlow.canOnlySkip(step, prerequisites),
                canGoBack = SetupFlow.previousStep(step, prerequisites) != null,
                canStart = startBlockers.isEmpty(),
                startBlockers = startBlockers,
                skippedSteps = setup.skippedSteps,
                enabledApps = settings.enabledApps,
                audioOutputSettings = settings.eligibility.audioOutput,
                batteryGuidanceAcknowledged = setup.batteryGuidanceAcknowledged,
                notificationPermissionRequired = postNotifications.isRequired,
                notificationPermissionGranted = postNotifications.isGranted(),
            )
        }
    }

    private fun readPrerequisites(settings: HearItSettings): SetupPrerequisites {
        val audio = settings.eligibility.audioOutput
        return SetupPrerequisites(
            notificationAccessGranted = notificationAccess.isGranted(),
            bluetoothPermissionRequired = bluetoothPermission.isRequired,
            bluetoothPermissionGranted = bluetoothPermission.isGranted(),
            ttsEngineAvailable = engine.isAvailable(),
            ttsLanguageAvailable = engine.isAvailable() && engine.supportsLocale(Locale.getDefault()),
            enabledAppCount = settings.enabledApps.size,
            allowedOutputCount = listOf(
                audio.allowWired,
                audio.allowBluetooth,
                audio.allowPhoneSpeaker,
            ).count { allowed -> allowed },
        )
    }

    private fun loadApps() {
        viewModelScope.launch {
            val apps = appCatalog.installedApps()
            _state.update { it.copy(apps = apps, appsLoading = false) }
        }
    }

    private fun loadOutputs() {
        _state.update { it.copy(outputs = audioOutputs.connectedOutputs()) }
    }

    /**
     * TTS initialisation is asynchronous, so a fresh install can reach the TTS step before
     * the engine reports ready. Poll briefly rather than telling the user "unavailable"
     * when it is merely still starting; the poll never blocks the main thread.
     */
    private fun ensureTtsPolling() {
        if (engine.isAvailable() || ttsPollJob?.isActive == true) return
        ttsPollJob = viewModelScope.launch {
            repeat(TTS_POLL_ATTEMPTS) {
                delay(TTS_POLL_INTERVAL_MILLIS)
                if (engine.isAvailable()) {
                    render(setupStateRepository.current(), settingsRepository.current())
                    return@launch
                }
            }
            render(setupStateRepository.current(), settingsRepository.current())
        }
    }

    private companion object {
        /** Half-second polls for up to five seconds; TTS engines normally report much sooner. */
        const val TTS_POLL_INTERVAL_MILLIS = 500L
        const val TTS_POLL_ATTEMPTS = 10
    }
}
