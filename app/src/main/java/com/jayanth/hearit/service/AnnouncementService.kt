package com.jayanth.hearit.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.IBinder
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

    /** Last accepted announcement, so Pause/Resume can re-speak it (P16). */
    private var lastAction: AnnouncementPipeline.Action.Speak? = null

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
        val action = intent?.action
        when (action) {
            ACTION_STOP -> {
                sessionStore.isStarted = false
                serviceStatus.markStopped()
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_TEMPORARY_MUTE_15MIN -> return muteFor(15 * 60 * 1000L)
            ACTION_TEMPORARY_MUTE_30MIN -> return muteFor(30 * 60 * 1000L)
            ACTION_TEMPORARY_MUTE_60MIN -> return muteFor(60 * 60 * 1000L)

            ACTION_TEMPORARY_MUTE_TOGGLE -> {
                sessionStore.isTemporaryMuteActive = !sessionStore.isTemporaryMuteActive
                handler.removeCallbacks(muteUnlockRunnable)
                publishStatus()
                return START_STICKY
            }

            ACTION_PRIORITY_ANNOUNCE_ON -> {
                setPriorityAnnounce(active = true)
                return START_STICKY
            }

            ACTION_PRIORITY_ANNOUNCE_OFF -> {
                setPriorityAnnounce(active = false)
                return START_STICKY
            }

            ACTION_PRIORITY_ANNOUNCE_TOGGLE -> {
                setPriorityAnnounce(active = !serviceStatus.state.value.isPriorityAnnounceActive)
                return START_STICKY
            }

            ACTION_PAUSE -> {
                // Android TTS has no native pause; pausing stops the current utterance and
                // Resume speaks it again from the start (documented limitation, P16).
                if (currentAction != null) {
                    engine.stop()
                }
                publishStatus()
                return START_STICKY
            }

            ACTION_RESUME -> {
                val last = lastAction
                if (currentAction == null && last != null) {
                    speak(last)
                }
                publishStatus()
                return START_STICKY
            }

            ACTION_SKIP -> {
                currentAction?.let { finishSpeech(it) }
                pipeline.tick(
                    state = currentState(),
                    settingsOf = { packageName -> settingsSource.forApp(packageName) },
                )?.let { speak(it) }
                publishStatus()
                return START_STICKY
            }

            ACTION_REPEAT -> {
                val current = currentAction
                if (current != null) {
                    speak(current)
                } else {
                    lastAction?.let { speak(it) }
                }
                publishStatus()
                return START_STICKY
            }
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

    /** Started service; there is no bound-client contract, so no binder is returned. */
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onPosted(snapshot: NotificationSnapshot) {
        pipeline.onNotification(
            snapshot = snapshot,
            settings = settingsSource.forApp(snapshot.packageName),
            state = currentState(),
        )
    }

    override fun onRemoved(key: String) {
        // The listener already forgot the extraction identity; the queue owns dedup and
        // expiry for pending items.
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
        val state = currentState()
        val speaking = currentAction
        if (speaking != null) {
            pipeline.onStateChanged(state) { packageName -> settingsSource.forApp(packageName) }
                .forEach { action -> if (action is AnnouncementPipeline.Action.StopSpeech) engine.stop() }
            publishStatus()
            return
        }

        pipeline.tick(state) { packageName -> settingsSource.forApp(packageName) }?.let { speak(it) }
        publishStatus()
    }

    private fun speak(action: AnnouncementPipeline.Action.Speak) {
        currentAction = action
        lastAction = action
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

    /** Applies temporary mute for [durationMillis] and schedules the automatic clear. */
    private fun muteFor(durationMillis: Long): Int {
        sessionStore.isTemporaryMuteActive = true
        handler.removeCallbacks(muteUnlockRunnable)
        handler.postDelayed(muteUnlockRunnable, durationMillis)
        publishStatus()
        return START_STICKY
    }

    /** Turns Priority Announce on/off; the 24-hour expiry is fixed for now (P17). */
    private fun setPriorityAnnounce(active: Boolean) {
        serviceStatus.markPriorityAnnounceActive(active)
        handler.removeCallbacks(priorityAnnounceExpiryRunnable)
        if (active) {
            handler.postDelayed(priorityAnnounceExpiryRunnable, PRIORITY_ANNOUNCE_EXPIRY_MILLIS)
        }
        publishStatus()
    }

    /** Fresh device state, including the mode the service itself owns. */
    private fun currentState() = stateReader.read(
        priorityAnnounceActive = serviceStatus.state.value.isPriorityAnnounceActive,
    )

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

    /** Temporary mute timer callbacks (P16). */
    private val muteUnlockRunnable = Runnable {
        sessionStore.isTemporaryMuteActive = false
        publishStatus()
    }

    /** Priority Announce expiry callback (P17). */
    private val priorityAnnounceExpiryRunnable = Runnable {
        serviceStatus.markPriorityAnnounceActive(false)
        publishStatus()
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

        /** Priority Announce actions (P17, Quick Settings tile P19b). */
        const val ACTION_PRIORITY_ANNOUNCE_ON = "com.jayanth.hearit.action.PRIORITY_ANNOUNCE_ON"
        const val ACTION_PRIORITY_ANNOUNCE_OFF = "com.jayanth.hearit.action.PRIORITY_ANNOUNCE_OFF"
        const val ACTION_PRIORITY_ANNOUNCE_TOGGLE = "com.jayanth.hearit.action.PRIORITY_ANNOUNCE_TOGGLE"

        /** Pause/Resume/Repeat/Skip current announcement (P16). */
        const val ACTION_PAUSE = "com.jayanth.hearit.action.PAUSE"
        const val ACTION_RESUME = "com.jayanth.hearit.action.RESUME"
        const val ACTION_SKIP = "com.jayanth.hearit.action.SKIP"
        const val ACTION_REPEAT = "com.jayanth.hearit.action.REPEAT"

        /** Tick cadence: fast enough for ADR-011 stop-on-change, cheap enough to stay idle. */
        private const val TICK_INTERVAL_MILLIS = 750L

        /** Fixed Priority Announce expiry (master prompt §10: configurable duration is P17 UI). */
        private const val PRIORITY_ANNOUNCE_EXPIRY_MILLIS = 24 * 60 * 60 * 1000L
    }
}
