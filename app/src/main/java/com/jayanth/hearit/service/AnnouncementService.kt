package com.jayanth.hearit.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.jayanth.hearit.core.common.model.NotificationSnapshot
import com.jayanth.hearit.core.common.time.Clock
import com.jayanth.hearit.core.notifications.NotificationRelay
import com.jayanth.hearit.core.notifications.NotificationSink
import com.jayanth.hearit.core.queue.AnnouncementQueue
import com.jayanth.hearit.core.queue.DeviceAudioState
import com.jayanth.hearit.core.tts.BuiltAnnouncement
import com.jayanth.hearit.core.tts.TextToSpeechEngine
import com.jayanth.hearit.core.tts.TtsError
import com.jayanth.hearit.core.tts.UtteranceListener
import com.jayanth.hearit.dashboard.ServiceStatusMonitor
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The single foreground announcement service (ADR-010): hosts the queue + TTS loop and the
 * persistent notification, and is the only component allowed to speak.
 *
 * Responsibilities kept deliberately thin — everything decidable is in
 * [AnnouncementPipeline] (pure, JVM-tested); this class only:
 *  - runs the tick loop on the main thread every [TICK_INTERVAL_MILLIS] (no blocking work:
 *    rules/queue are pure and cheap, engine calls are asynchronous);
 *  - reads device state per tick and asks the pipeline whether speech must stop
 *    (screen/lock, mute, call, DND, output, silence — ADR-011/ADR-017);
 *  - speaks via the injected [TextToSpeechEngine] and reports completion exactly once;
 *  - receives snapshots from the listener through [NotificationRelay];
 *  - stops speech when the active audio output disappears.
 *
 * Lifecycle: started only by an explicit user action ([ACTION_START]) and stopped only by
 * [ACTION_STOP] or the user; [SessionStore.isStarted] is persisted first, so a
 * system-initiated recreation never starts announcing on its own (architecture §4).
 *
 * Privacy: nothing is logged anywhere in this class.
 *
 * Not locally verified — written under the cloud-agent restriction (no build/test run).
 */
@AndroidEntryPoint
class AnnouncementService : Service(), NotificationSink {

    @Inject
    lateinit var engine: TextToSpeechEngine

    @Inject
    lateinit var audioManager: AudioManager

    /** Dashboard observation seam (P12). The service is the only writer. */
    @Inject
    lateinit var serviceStatus: ServiceStatusMonitor

    private lateinit var sessionStore: SessionStore
    private lateinit var settingsSource: SettingsSource
    private lateinit var stateReader: RuntimeStateReader
    private lateinit var pipeline: AnnouncementPipeline

    /** The announcement currently being spoken, or null. */
    private var currentAction: AnnouncementPipeline.Action.Speak? = null

    private val handler = Handler(Looper.getMainLooper())

    private val tickRunnable = object : Runnable {
        override fun run() {
            tickOnce()
            handler.postDelayed(this, TICK_INTERVAL_MILLIS)
        }
    }

    /** Any output going away may be the one speech is routed to: stop and let rules decide. */
    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            if (!removedDevices.isNullOrEmpty()) {
                engine.stop()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        sessionStore = SessionStore(this)
        settingsSource = SettingsSource(sessionStore)
        stateReader = RuntimeStateReader(this, audioManager, sessionStore)

        val clock = Clock { SystemClock.elapsedRealtime() }
        val queue = AnnouncementQueue(
            clock = clock,
            deviceAudio = DeviceAudioState {
                audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT
            },
        )
        pipeline = AnnouncementPipeline(queue)

        AnnouncementNotification.ensureChannel(this)
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, handler)
        NotificationRelay.sink = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when {
            intent?.action == ACTION_STOP -> {
                sessionStore.isStarted = false
                serviceStatus.markStopped()
                stopSelf()
                return START_NOT_STICKY
            }
            intent?.action == ACTION_TEMPORARY_MUTE_15MIN -> {
                sessionStore.isTemporaryMuteActive = true
                handler.removeCallbacks(muteToggleRunnable)
                handler.postDelayed(muteUnlockRunnable, 15 * 60 * 1000L)
                publishStatus()
                return START_STICKY
            }
            intent?.action == ACTION_TEMPORARY_MUTE_30MIN -> {
                sessionStore.isTemporaryMuteActive = true
                handler.removeCallbacks(muteToggleRunnable)
                handler.postDelayed(muteUnlockRunnable, 30 * 60 * 1000L)
                publishStatus()
                return START_STICKY
            }
            intent?.action == ACTION_TEMPORARY_MUTE_60MIN -> {
                sessionStore.isTemporaryMuteActive = true
                handler.removeCallbacks(muteToggleRunnable)
                handler.postDelayed(muteUnlockRunnable, 60 * 60 * 1000L)
                publishStatus()
                return START_STICKY
            }
            intent?.action == ACTION_TEMPORARY_MUTE_TOGGLE -> {
                sessionStore.isTemporaryMuteActive = !sessionStore.isTemporaryMuteActive
                handler.removeCallbacks(muteToggleRunnable)
                publishStatus()
                return START_STICKY
            }
            intent?.action == ACTION_PRIORITY_ANNOUNCE_ON -> {
                // Enable Priority Announce mode
                serviceStatus.markPriorityAnnounceActive(true)
                handler.postDelayed(priorityAnnounceExpiryRunnable, 24 * 60 * 60 * 1000L) // 24h default expiry
                publishStatus()
                return START_STICKY
            }
            intent?.action == ACTION_PRIORITY_ANNOUNCE_OFF -> {
                // Disable Priority Announce mode
                serviceStatus.markPriorityAnnounceActive(false)
                handler.removeCallbacks(priorityAnnounceExpiryRunnable)
                publishStatus()
                return START_STICKY
            }
            intent?.action == ACTION_PRIORITY_ANNOUNCE_TOGGLE -> {
                // Toggle Priority Announce on/off
                if (serviceStatus.isPriorityAnnounceActive) {
                    serviceStatus.markPriorityAnnounceActive(false)
                    handler.removeCallbacks(priorityAnnounceExpiryRunnable)
                } else {
                    serviceStatus.markPriorityAnnounceActive(true)
                    handler.postDelayed(priorityAnnounceExpiryRunnable, 24 * 60 * 60 * 1000L) // 24h default expiry
                }
                publishStatus()
                return START_STICKY
            }
            intent?.action == ACTION_PAUSE -> {
                if (currentAction != null) {
                    engine.pause()
                }
                publishStatus()
                return START_STICKY
            }
            intent?.action == ACTION_RESUME -> {
                if (currentAction != null) {
                    engine.resume()
                }
                publishStatus()
                return START_STICKY
            }
            intent?.action == ACTION_SKIP -> {
                // Skip the current announcement and move to the next
                if (currentAction != null) {
                    finishSpeech(currentAction)
                }
                pipeline.tick(stateReader.read()) { packageName -> settingsSource.forApp(packageName) }
                    ?.let { speak(it)
                }
                publishStatus()
                return START_STICKY
            }
            intent?.action == ACTION_REPEAT -> {
                // Re-speak the last announcement
                if (currentAction != null) {
                    speak(currentAction)
                }
                publishStatus()
                return START_STICKY
            }
            sessionStore.isStarted = true
            startInForeground()
            // Publish before the first tick so the dashboard shows Active immediately (P12).
            serviceStatus.markRunning()
            publishStatus()
            handler.removeCallbacks(tickRunnable)
            handler.post(tickRunnable)
            return START_STICKY
        }
    }

    override fun onPosted(snapshot: NotificationSnapshot) {
        val state = stateReader.read()
        pipeline.onNotification(
            snapshot = snapshot,
            settings = settingsSource.forApp(snapshot.packageName),
            state = state,
        )
        // Queue events carry status/reason for history; history storage lands in P10.
    }

    override fun onRemoved(key: String) {
        // The listener already forgot the extraction identity; the queue owns dedup and
        // expiry for pending items. History (P10) records the removal.
    }

    override fun onDestroy() {
        serviceStatus.markStopped()
        handler.removeCallbacksAndMessages(null)
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        NotificationRelay.sink = null
        engine.shutdown()
        super.onDestroy()
    }

    private fun tickOnce() {
        val state = stateReader.read()

        val speaking = currentAction
        if (speaking != null) {
            pipeline.onStateChanged(state) { packageName -> settingsSource.forApp(packageName) }
                .forEach { action ->
                    if (action is AnnouncementPipeline.Action.StopSpeech) engine.stop()
                }
            publishStatus()
            return
        }

        val next = pipeline.tick(state) { packageName -> settingsSource.forApp(packageName) }
        if (next != null) speak(next)
        publishStatus()
    }

    private fun speak(action: AnnouncementPipeline.Action.Speak) {
        currentAction = action
        val accepted = engine.speak(
            announcement = action.announcement,
            settings = settingsSource.speech(),
            listener = object : UtteranceListener {
                override fun onComplete(announcement: BuiltAnnouncement) {
                    handler.post { finishSpeech(action) }
                }

                override fun onError(announcement: BuiltAnnouncement, error: TtsError) {
                    handler.post { finishSpeech(action) }
                }
            },
        )
        if (!accepted) {
            // Engine busy/unavailable: retire the selection so it is not stuck "speaking".
            finishSpeech(action)
        }
        publishStatus()
    }

    private fun finishSpeech(action: AnnouncementPipeline.Action.Speak) {
        if (currentAction !== action) return
        currentAction = null
        pipeline.onSpeechFinished()
        startInForeground()
        publishStatus()
    }

    /**
     * Publishes speaking/queue state to the dashboard (P12).
     *
     * Content-free by design: the monitor learns that something is being read and how many
     * announcements wait, never what they say (master prompt §17).
     */
    private fun publishStatus() {
        serviceStatus.updateSpeech(
            isSpeaking = pipeline.isSpeaking,
            pendingCount = pipeline.pendingCount,
        )
    }

    private fun startInForeground() {
        val notification = AnnouncementNotification.build(this, speaking = currentAction != null)
        startForeground(
            AnnouncementNotification.NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )
    }

    companion object {
        /** Explicit start (setup flow / dashboard, P11/P12). */
        const val ACTION_START = "com.jayanth.hearit.action.START_ANNOUNCEMENTS"

        /** Explicit stop (notification action / dashboard, P08/P12). */
    const val ACTION_STOP = "com.jayanth.hearit.action.STOP_ANNOUNCEMENTS"

    /** Temporary mute actions (P16): 15 min, 30 min, 1 h, or until re-enabled. */
    const val ACTION_TEMPORARY_MUTE_15MIN = "com.jayanth.hearit.action.TEMPORARY_MUTE_15MIN"
    const val ACTION_TEMPORARY_MUTE_30MIN = "com.jayanth.hearit.action.TEMPORARY_MUTE_30MIN"
    const val ACTION_TEMPORARY_MUTE_60MIN = "com.jayanth.hearit.action.TEMPORARY_MUTE_60MIN"
    const val ACTION_TEMPORARY_MUTE_TOGGLE = "com.jayanth.hearit.action.TEMPORARY_MUTE_TOGGLE"

    /** Priority Announce actions (P17). */    const val ACTION_PRIORITY_ANNOUNCE_ON = "com.jayanth.hearit.action.PRIORITY_ANNOUNCE_ON"
    const val ACTION_PRIORITY_ANNOUNCE_OFF = "com.jayanth.hearit.action.PRIORITY_ANNOUNCE_OFF"
    const val ACTION_PRIORITY_ANNOUNCE_TOGGLE = "com.jayanth.hearit.action.PRIORITY_ANNOUNCE_TOGGLE"
    /** Pause/Resume/Repeat/Skip current announcement (P16). */
    const val ACTION_PAUSE = "com.jayanth.hearit.action.PAUSE"
    const val ACTION_RESUME = "com.jayanth.hearit.action.RESUME"
    const val ACTION_SKIP = "com.jayanth.hearit.action.SKIP"
    const val ACTION_REPEAT = "com.jayanth.hearit.action.REPEAT"

    /** Tick cadence: fast enough for ADR-011 stop-on-change, cheap enough to stay idle. */
    private const val TICK_INTERVAL_MILLIS = 750L
    }

    /** Temporary mute timer callbacks (P16). */
    private val muteToggleRunnable = object : Runnable {
        override fun run() {
            sessionStore.isTemporaryMuteActive = false
        }
    }

    private val muteUnlockRunnable = object : Runnable {
        override fun run() {
            sessionStore.isTemporaryMuteActive = false
        }
    }

    private val priorityAnnounceExpiryRunnable = object : Runnable {
        override fun run() {
            serviceStatus.markPriorityAnnounceActive(false)
        }
    }
}
