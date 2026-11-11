package com.jayanth.hearit.service

import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import com.jayanth.hearit.core.common.logger.HearItLogger
import com.jayanth.hearit.core.common.logger.LogCategory
import com.jayanth.hearit.core.common.model.HeadsetButtonAction
import com.jayanth.hearit.core.common.model.NotificationSnapshot
import com.jayanth.hearit.core.common.time.Clock
import com.jayanth.hearit.core.database.HistoryRepository
import com.jayanth.hearit.core.datastore.HearItSettings
import com.jayanth.hearit.core.notifications.HearItNotificationListenerService
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

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

    /** Persisted settings snapshot (L2); the rules/queue/format/speech all read from it. */
    @Inject
    lateinit var settingsSource: SettingsSource

    /** History storage (L3); written from a single consumer coroutine, never the main thread. */
    @Inject
    lateinit var history: HistoryRepository

    @Inject
    lateinit var intelligentSettingsSync: com.jayanth.hearit.intelligence.IntelligentSettingsSync

    private lateinit var sessionStore: SessionStore
    private lateinit var stateReader: RuntimeStateReader
    private lateinit var queue: AnnouncementQueue
    private lateinit var pipeline: AnnouncementPipeline

    /** Collects the settings flow only while the service is alive. */
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * Ordered hand-off from the pipeline's synchronous loop to the history consumer.
     * Unlimited so the main thread never waits, single-consumer so rows land in pipeline
     * order (update handling compares against the previous row for the same key).
     */
    private val historyQueue = Channel<AnnouncementPipeline.HistoryRecord>(Channel.UNLIMITED)

    /** Snapshot the queue was last configured with (identity compare, no deep equality). */
    private var appliedSettings: HearItSettings? = null

    /** The announcement currently being spoken, or null. */
    private var currentAction: AnnouncementPipeline.Action.Speak? = null

    /** Last accepted announcement, so Pause/Resume can re-speak it (P16). */
    private var lastAction: AnnouncementPipeline.Action.Speak? = null

    /** Active audio-focus request while an announcement is being spoken, or null. */
    private var focusRequest: AudioFocusRequest? = null
    private var pendingAbandonFocusRequest: AudioFocusRequest? = null
    private var savedMusicVolumeIndex: Int? = null
    private var isPausedMidUtterance: Boolean = false
    private lateinit var headsetMediaController: HeadsetMediaSessionController

    private val abandonFocusRunnable = Runnable {
        restoreSavedMusicVolume()
        pendingAbandonFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        pendingAbandonFocusRequest = null
    }

    /**
     * Another app taking focus away ends the utterance: HearIt never talks over a caller
     * or a competing player. Losing focus *transiently while ducking* is our own request
     * working as intended, so it is ignored.
     */
    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { change ->
        handler.post {
            if (currentAction == null) return@post
            when (change) {
                AudioManager.AUDIOFOCUS_LOSS,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> engine.stop()
                else -> Unit
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())

    private val tickRunnable = object : Runnable {
        override fun run() {
            tickOnce()
            handler.postDelayed(this, TICK_INTERVAL_MILLIS)
        }
    }

    /** Any output going away may be the one speech is routed to: stop and let rules decide. */
    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            if (!addedDevices.isNullOrEmpty()) {
                val hasHeadphones = addedDevices.any {
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                        it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                        it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                }
                if (hasHeadphones && pipeline.pendingCount > 0) {
                    val count = pipeline.pendingCount
                    val text = if (count == 1) "You have 1 pending notification." else "You have $count pending notifications."
                    val built = BuiltAnnouncement(
                        listOf(
                            com.jayanth.hearit.core.tts.AnnouncementSection(
                                com.jayanth.hearit.core.tts.SectionKind.INTRODUCTION,
                                "HearIt connected.",
                            ),
                            com.jayanth.hearit.core.tts.AnnouncementSection(
                                com.jayanth.hearit.core.tts.SectionKind.MESSAGE,
                                text,
                            ),
                        ),
                        pauseBetweenSectionsMillis = 200L,
                    )
                    engine.speak(built, settingsSource.speech(), object : UtteranceListener {
                        override fun onStart(announcement: BuiltAnnouncement) {}
                        override fun onComplete(announcement: BuiltAnnouncement) {}
                        override fun onError(announcement: BuiltAnnouncement, error: TtsError) {}
                    })
                }
            }
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            if (!removedDevices.isNullOrEmpty()) {
                engine.stop()
            }
        }
    }

    private var lastShakeTime = 0L
    private var lastSpeechEndTimeMillis = 0L
    private var wasMusicActive = false
    private var musicPausedAtMillis = 0L
    private var tickCount = 0L

    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // 1. Flip check: phone lying flat face down (z < -8.5 m/s^2)
            if (z < -8.5f && currentAction != null && sessionStore.isShakeToStopEnabled) {
                HearItLogger.info(LogCategory.SERVICE, "Flip gesture detected: stopping speech")
                engine.stop()
                return
            }

            // 2. Shake check: sudden acceleration spike > 15.5 m/s^2
            val acceleration = kotlin.math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val now = SystemClock.elapsedRealtime()
            if (acceleration > 15.5f && now - lastShakeTime > 1_000L) {
                lastShakeTime = now
                if (currentAction != null && sessionStore.isShakeToStopEnabled) {
                    HearItLogger.info(LogCategory.SERVICE, "Shake gesture detected: stopping speech")
                    engine.stop()
                } else if (currentAction == null && now - lastSpeechEndTimeMillis <= 10_000L && sessionStore.isShakeToStopEnabled) {
                    HearItLogger.info(LogCategory.SERVICE, "Shake gesture detected within 10s: repeating announcement")
                    lastAction?.let { speak(it) }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "android.media.VOLUME_CHANGED_ACTION" && currentAction != null && sessionStore.isVolumeKeyToStopEnabled) {
                HearItLogger.info(LogCategory.SERVICE, "Volume key pressed while speaking: stopping speech")
                engine.stop()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        sessionStore = SessionStore(this)
        intelligentSettingsSync.start()
        stateReader = RuntimeStateReader(this, audioManager, sessionStore)

        val clock = Clock { SystemClock.elapsedRealtime() }
        queue = AnnouncementQueue(
            clock = clock,
            deviceAudio = DeviceAudioState {
                audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT
            },
        )
        pipeline = AnnouncementPipeline(
            queue = queue,
            onRecord = { record -> historyQueue.trySend(record) },
        )

        // The pipeline is synchronous (main-thread tick loop) while DataStore is not: mirror
        // the persisted settings into the source's snapshot. Before the first emission every
        // app is disabled, so a slow read can only lose an announcement, never invent one.
        settingsSource.observe(serviceScope)
        applyQueueSettings()

        // Every pipeline outcome becomes a history row (ADR-007): collection never depends on
        // eligibility, and the storage mode/update handling come from the app's own settings.
        serviceScope.launch {
            for (record in historyQueue) {
                history.record(
                    snapshot = record.snapshot,
                    settings = settingsSource.settings.historyFor(record.snapshot.packageName),
                    status = record.status,
                    reason = record.reason,
                    priority = record.priority,
                )
            }
        }

        AnnouncementNotification.ensureChannel(this)
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, handler)
        NotificationRelay.sink = this

        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accel = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accel != null) {
            sensorManager.registerListener(sensorEventListener, accel, SensorManager.SENSOR_DELAY_UI)
        }
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        try {
            registerReceiver(volumeReceiver, filter)
        } catch (_: Throwable) {}
        HearItNotificationListenerService.requestRebindIfDisconnected(this)
        headsetMediaController = HeadsetMediaSessionController(this, sessionStore) { buttonAction ->
            handleHeadsetButtonAction(buttonAction)
        }

        HearItLogger.info(LogCategory.SERVICE, "AnnouncementService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        HearItLogger.info(LogCategory.SERVICE, "AnnouncementService onStartCommand action=$action")
        HearItNotificationListenerService.requestRebindIfDisconnected(this)
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
            ACTION_TEMPORARY_MUTE_120MIN -> return muteFor(120 * 60 * 1000L)
            ACTION_TEMPORARY_MUTE_INDEFINITE -> {
                sessionStore.isTemporaryMuteActive = true
                handler.removeCallbacks(muteUnlockRunnable)
                publishStatus()
                return START_STICKY
            }

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
                if (currentAction != null) {
                    isPausedMidUtterance = true
                    engine.stop()
                }
                publishStatus()
                return START_STICKY
            }

            ACTION_RESUME -> {
                val last = lastAction
                if (currentAction == null && last != null) {
                    isPausedMidUtterance = false
                    speak(last)
                }
                publishStatus()
                return START_STICKY
            }

            ACTION_SKIP -> {
                currentAction?.let { finishSpeech(it, spoken = false) }
                pipeline.tick(
                    state = currentState(),
                    settingsOf = { packageName -> settingsSource.forApp(packageName) },
                    announcementFormat = settingsSource.format(),
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
            priority = settingsSource.priorityFor(snapshot.packageName),
        )
        com.jayanth.hearit.wear.WearDataLayerSync.syncNotification(this, snapshot)
    }

    override fun onRemoved(key: String) {
        // The listener already forgot the extraction identity; the queue owns dedup and
        // expiry for pending items.
    }

    override fun onDestroy() {
        serviceStatus.markStopped()
        serviceScope.cancel()
        handler.removeCallbacksAndMessages(null)
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        try {
            unregisterReceiver(volumeReceiver)
        } catch (_: Throwable) {}
        (getSystemService(Context.SENSOR_SERVICE) as? SensorManager)?.unregisterListener(sensorEventListener)
        NotificationRelay.sink = null
        headsetMediaController.release()
        abandonAudioFocus(immediate = true)
        engine.shutdown()
        HearItLogger.info(LogCategory.SERVICE, "AnnouncementService destroyed")
        super.onDestroy()
    }

    /**
     * Applies the stored audio-focus policy (master prompt §6, ADR-017).
     *
     * `NONE` asks for nothing at all, so other audio keeps playing underneath — that is the
     * setting's promise. A refused request is not fatal: speech still runs, it just no longer
     * pauses or ducks the other app.
     */
    private fun requestAudioFocus(action: AnnouncementPipeline.Action.Speak) {
        val policy = settingsSource.forApp(action.selection.packageName).eligibility.audioFocus
        val gain = when (AudioFocusPolicyMapping.gainFor(policy)) {
            AudioFocusGain.DUCK_OTHERS -> AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            AudioFocusGain.PAUSE_OTHERS -> AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            AudioFocusGain.NONE -> {
                abandonAudioFocus(immediate = true)
                return
            }
        }
        handler.removeCallbacks(abandonFocusRunnable)
        pendingAbandonFocusRequest?.let {
            audioManager.abandonAudioFocusRequest(it)
            pendingAbandonFocusRequest = null
        }
        focusRequest?.let {
            audioManager.abandonAudioFocusRequest(it)
            focusRequest = null
        }

        val request = AudioFocusRequest.Builder(gain)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .setWillPauseWhenDucked(false)
            .setOnAudioFocusChangeListener(audioFocusListener, handler)
            .build()
        audioManager.requestAudioFocus(request)
        focusRequest = request

        if (gain == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK &&
            sessionStore.isDuckingLevelControlEnabled &&
            sessionStore.duckingVolumePercent < 100 &&
            !audioManager.isVolumeFixed &&
            audioManager.isMusicActive
        ) {
            runCatching {
                val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (savedMusicVolumeIndex == null) {
                    savedMusicVolumeIndex = currentVol
                }
                val minVol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC)
                } else {
                    0
                }
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val targetVol = AudioDuckingMath.calculateTargetVolume(
                    currentVolume = currentVol,
                    minVolume = minVol,
                    maxVolume = maxVol,
                    duckPercent = sessionStore.duckingVolumePercent,
                )
                if (targetVol < currentVol) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                }
            }
        }
    }

    /** Idempotent: releasing focus twice is a no-op, always restores any ducked volume. */
    private fun abandonAudioFocus(immediate: Boolean = false) {
        handler.removeCallbacks(abandonFocusRunnable)
        val request = focusRequest ?: pendingAbandonFocusRequest
        focusRequest = null
        if (request == null) {
            restoreSavedMusicVolume()
            return
        }
        val returnDelay = if (immediate) 0L else sessionStore.audioFocusReturnDelayMs.coerceAtLeast(0L)
        if (returnDelay == 0L) {
            pendingAbandonFocusRequest = null
            restoreSavedMusicVolume()
            audioManager.abandonAudioFocusRequest(request)
        } else {
            pendingAbandonFocusRequest = request
            handler.postDelayed(abandonFocusRunnable, returnDelay)
        }
    }

    private fun restoreSavedMusicVolume() {
        val original = savedMusicVolumeIndex ?: return
        savedMusicVolumeIndex = null
        runCatching {
            if (!audioManager.isVolumeFixed) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, original, 0)
            }
        }
    }

    private fun handleHeadsetButtonAction(action: HeadsetButtonAction) {
        when (action) {
            HeadsetButtonAction.PAUSE_RESUME -> {
                if (currentAction != null) {
                    isPausedMidUtterance = true
                    engine.stop()
                } else if (isPausedMidUtterance && lastAction != null) {
                    isPausedMidUtterance = false
                    lastAction?.let { speak(it) }
                }
                publishStatus()
            }
            HeadsetButtonAction.SKIP -> {
                isPausedMidUtterance = false
                currentAction?.let { finishSpeech(it, spoken = false) }
                pipeline.tick(
                    state = currentState(),
                    settingsOf = { packageName -> settingsSource.forApp(packageName) },
                    announcementFormat = settingsSource.format(),
                )?.let { speak(it) }
                publishStatus()
            }
            HeadsetButtonAction.REPEAT -> {
                isPausedMidUtterance = false
                val target = currentAction ?: lastAction
                target?.let { speak(it) }
                publishStatus()
            }
            HeadsetButtonAction.NONE -> Unit
        }
    }

    private fun tickOnce() {
        applyQueueSettings()
        val state = currentState()

        if (tickCount++ % 30L == 0L) {
            HearItNotificationListenerService.requestRebindIfDisconnected(this)
        }

        val isMusicActiveNow = audioManager.isMusicActive
        if (wasMusicActive && !isMusicActiveNow) {
            musicPausedAtMillis = SystemClock.elapsedRealtime()
        }
        wasMusicActive = isMusicActiveNow

        val isMusicHoldActive = sessionStore.isMusicPauseHoldEnabled &&
            (SystemClock.elapsedRealtime() - musicPausedAtMillis < 60_000L)
        if (isMusicHoldActive && currentAction == null) {
            publishStatus()
            return
        }

        // A call that has ended hands its held notifications back for full re-evaluation
        // (ADR-004/ADR-006): nothing is announced on the strength of its held decision.
        if (!state.isCallActive && pipeline.heldCount > 0) {
            pipeline.releaseHeld(state) { packageName -> settingsSource.forApp(packageName) }
        }
        val speaking = currentAction
        if (speaking != null) {
            pipeline.onStateChanged(state) { packageName -> settingsSource.forApp(packageName) }
                .forEach { action -> if (action is AnnouncementPipeline.Action.StopSpeech) engine.stop() }
            publishStatus()
            return
        }

        pipeline.tick(
            state = state,
            settingsOf = { packageName -> settingsSource.forApp(packageName) },
            announcementFormat = settingsSource.format(),
        )?.let { speak(it) }
        publishStatus()
    }

    /**
     * Reconfigures the queue when the persisted settings changed.
     *
     * The codec builds a fresh immutable value per emission, so identity comparison is
     * exact and an unchanged store never re-triggers limit enforcement.
     */
    private fun applyQueueSettings() {
        val snapshot = settingsSource.settings
        if (snapshot === appliedSettings) return
        appliedSettings = snapshot
        queue.updateSettings(
            settings = settingsSource.queueSettings(),
            appOverrides = settingsSource.queueOverrides(),
        )

        // Retention follows the settings that produced the sweep, and runs off the main thread.
        serviceScope.launch {
            history.prune(snapshot.history, System.currentTimeMillis())
        }
    }

    private fun speak(action: AnnouncementPipeline.Action.Speak) {
        isPausedMidUtterance = false
        currentAction = action
        lastAction = action
        requestAudioFocus(action)
        HearItLogger.info(
            LogCategory.TTS,
            "Starting speech announcement: pkg=${action.selection.packageName}, sectionCount=${action.announcement.sections.size}",
        )
        val accepted = engine.speak(
            announcement = action.announcement,
            settings = settingsSource.speech(),
            listener = object : UtteranceListener {
                override fun onComplete(announcement: BuiltAnnouncement) {
                    handler.post { finishSpeech(action) }
                }

                override fun onError(announcement: BuiltAnnouncement, error: TtsError) {
                    // Stopped, replaced or failed: history records INTERRUPTED, not ANNOUNCED.
                    handler.post { finishSpeech(action, spoken = false) }
                }
            },
        )
        if (!accepted) {
            // Engine busy/unavailable: retire the selection so it is not stuck "speaking".
            finishSpeech(action, spoken = false)
        }
        publishStatus()
    }

    /**
     * Ends the current utterance exactly once.
     *
     * @param spoken false when it was stopped, skipped, replaced or never started, so the
     *   pipeline records `INTERRUPTED` instead of `ANNOUNCED`.
     */
    private fun finishSpeech(action: AnnouncementPipeline.Action.Speak, spoken: Boolean = true) {
        if (currentAction !== action) return
        currentAction = null
        if (spoken) {
            isPausedMidUtterance = false
        }
        lastSpeechEndTimeMillis = SystemClock.elapsedRealtime()
        abandonAudioFocus()
        pipeline.onSpeechFinished(spoken)
        HearItLogger.info(
            LogCategory.TTS,
            "Finished speech announcement: pkg=${action.selection.packageName}, spoken=$spoken",
        )
        if (spoken && sessionStore.isAutoDismissEnabled) {
            action.selection.items.forEach { snapshot ->
                HearItNotificationListenerService.dismissNotification(
                    key = snapshot.key,
                    packageName = snapshot.packageName,
                )
            }
        }
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
        if (::headsetMediaController.isInitialized) {
            headsetMediaController.updateState(
                isSpeaking = pipeline.isSpeaking,
                isPausedMidUtterance = isPausedMidUtterance,
            )
        }
        serviceStatus.updateSpeech(
            isSpeaking = pipeline.isSpeaking,
            pendingCount = pipeline.pendingCount,
        )
        updateNotification()
    }

    private fun startInForeground() {
        val notification = AnnouncementNotification.build(
            this,
            speaking = currentAction != null,
            isPriorityAnnounce = serviceStatus.state.value.isPriorityAnnounceActive,
            isMuted = sessionStore.isTemporaryMuteActive,
        )
        startForeground(
            AnnouncementNotification.NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )
    }

    private fun updateNotification() {
        val notification = AnnouncementNotification.build(
            this,
            speaking = currentAction != null,
            isPriorityAnnounce = serviceStatus.state.value.isPriorityAnnounceActive,
            isMuted = sessionStore.isTemporaryMuteActive,
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(AnnouncementNotification.NOTIFICATION_ID, notification)
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
        fun createStartIntent(context: Context): Intent =
            Intent(context, AnnouncementService::class.java).setAction(ACTION_START)

        fun createStopIntent(context: Context): Intent =
            Intent(context, AnnouncementService::class.java).setAction(ACTION_STOP)

        /** Explicit start (setup flow / dashboard, P11/P12). */
        const val ACTION_START = "com.jayanth.hearit.action.START_ANNOUNCEMENTS"

        /** Explicit stop (notification action / dashboard, P08/P12). */
        const val ACTION_STOP = "com.jayanth.hearit.action.STOP_ANNOUNCEMENTS"

        /** Temporary mute actions (P16): 15 min, 30 min, 1 h, or until re-enabled. */
        const val ACTION_TEMPORARY_MUTE_15MIN = "com.jayanth.hearit.action.TEMPORARY_MUTE_15MIN"
        const val ACTION_TEMPORARY_MUTE_30MIN = "com.jayanth.hearit.action.TEMPORARY_MUTE_30MIN"
        const val ACTION_TEMPORARY_MUTE_60MIN = "com.jayanth.hearit.action.TEMPORARY_MUTE_60MIN"
        const val ACTION_TEMPORARY_MUTE_120MIN = "com.jayanth.hearit.action.TEMPORARY_MUTE_120MIN"
        const val ACTION_TEMPORARY_MUTE_INDEFINITE = "com.jayanth.hearit.action.TEMPORARY_MUTE_INDEFINITE"
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
