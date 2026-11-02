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
        if (intent?.action == ACTION_STOP) {
            sessionStore.isStarted = false
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent == null && !sessionStore.isStarted) {
            // System recreation after a stop: never announce without an explicit start.
            stopSelf()
            return START_NOT_STICKY
        }

        sessionStore.isStarted = true
        startInForeground()
        handler.removeCallbacks(tickRunnable)
        handler.post(tickRunnable)
        return START_STICKY
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
            return
        }

        val next = pipeline.tick(state) { packageName -> settingsSource.forApp(packageName) }
        if (next != null) speak(next)
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
    }

    private fun finishSpeech(action: AnnouncementPipeline.Action.Speak) {
        if (currentAction !== action) return
        currentAction = null
        pipeline.onSpeechFinished()
        startInForeground()
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

        /** Tick cadence: fast enough for ADR-011 stop-on-change, cheap enough to stay idle. */
        private const val TICK_INTERVAL_MILLIS = 750L
    }
}
