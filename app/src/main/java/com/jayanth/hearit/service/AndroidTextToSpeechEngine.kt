package com.jayanth.hearit.service

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.media.audiofx.LoudnessEnhancer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.jayanth.hearit.core.tts.BuiltAnnouncement
import com.jayanth.hearit.core.tts.SpeechSettings
import com.jayanth.hearit.core.tts.TextToSpeechEngine
import com.jayanth.hearit.core.tts.TtsError
import com.jayanth.hearit.core.tts.TtsErrorCode
import com.jayanth.hearit.core.tts.TtsVoice
import com.jayanth.hearit.core.tts.UtteranceListener
import com.jayanth.hearit.voicespeech.AdaptiveSpeedCalculator
import java.util.Locale

/**
 * Android `TextToSpeech` wrapper satisfying the P04 [TextToSpeechEngine] contract
 * and applying persisted Smart Audio Polish, Voice Boost, and optional Ambient Noise boost.
 */
class AndroidTextToSpeechEngine(context: Context) : TextToSpeechEngine {

    private val appContext = context.applicationContext
    private val sessionStore = SessionStore(appContext)
    private val audioManager = appContext.getSystemService(AudioManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var engine: TextToSpeech? = null
    private var ready = false
    private var request: Request? = null
    private var sessionToken = 0L
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var currentAudioSessionId: Int = AudioManager.AUDIO_SESSION_ID_GENERATE

    private data class Request(
        val announcement: BuiltAnnouncement,
        val listener: UtteranceListener,
        val token: Long,
        val settings: SpeechSettings = SpeechSettings(),
        var sectionIndex: Int = 0,
        var startNotified: Boolean = false,
        var ambientBoostPercent: Int = 0,
    )

    init {
        val created = TextToSpeech(appContext) { status ->
            ready = status == TextToSpeech.SUCCESS
        }
        created.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                mainHandler.post { onSectionStarted(utteranceId) }
            }

            override fun onDone(utteranceId: String?) {
                mainHandler.post { onSectionDone(utteranceId) }
            }

            @Suppress("OVERRIDE_DEPRECATION", "Deprecation")
            override fun onError(utteranceId: String?) {
                mainHandler.post { onSectionFailed(utteranceId) }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                mainHandler.post { onSectionFailed(utteranceId) }
            }
        })
        engine = created
    }

    override fun isAvailable(): Boolean = engine != null && ready

    override fun listVoices(): List<TtsVoice> {
        val voices = engine?.takeIf { ready }?.voices ?: return emptyList()
        return voices.map { voice ->
            TtsVoice(
                id = voice.name,
                displayName = voice.name,
                locale = voice.locale,
                isNetworkConnectionRequired = voice.isNetworkConnectionRequired,
            )
        }
    }

    override fun supportsLocale(locale: Locale): Boolean {
        val tts = engine?.takeIf { ready } ?: return false
        return tts.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE
    }

    override fun speak(
        announcement: BuiltAnnouncement,
        settings: SpeechSettings,
        listener: UtteranceListener,
    ): Boolean {
        val tts = engine?.takeIf { ready } ?: return false
        if (announcement.isEmpty) return false

        finishCurrent(TtsErrorCode.INTERRUPTED, notify = true)

        if (!supportsLocale(Locale.getDefault())) {
            listener.onError(announcement, TtsError(TtsErrorCode.LANGUAGE_DATA_MISSING))
            return true
        }

        val totalWords = AdaptiveSpeedCalculator.countWords(announcement.sections.map { it.text })
        val effectiveRate = AdaptiveSpeedCalculator.resolveRate(
            baseRate = settings.speechRate,
            wordCount = totalWords,
            enabled = sessionStore.isAdaptiveSpeedEnabled,
            tier1Multiplier = sessionStore.adaptiveSpeedTier1Multiplier,
            tier2Multiplier = sessionStore.adaptiveSpeedTier2Multiplier,
            tier3Multiplier = sessionStore.adaptiveSpeedTier3Multiplier,
        )
        tts.setSpeechRate(effectiveRate)
        tts.setPitch(settings.pitch)
        applyVoice(tts, settings.voiceId)

        if (announcement.isCritical) {
            try {
                ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
                    .startTone(ToneGenerator.TONE_SUP_PIP, 320)
            } catch (_: Throwable) {}
        } else if (sessionStore.isEarconEnabled) {
            val earconMs = sessionStore.earconDurationMs
            try {
                ToneGenerator(AudioManager.STREAM_NOTIFICATION, 45)
                    .startTone(ToneGenerator.TONE_PROP_BEEP2, earconMs)
            } catch (_: Throwable) {}
        }

        val newRequest = Request(announcement, listener, token = ++sessionToken, settings = settings)
        request = newRequest

        val delayMs = sessionStore.antiCollisionDelayMs.coerceAtLeast(0L)
        val shouldCheckAmbient = sessionStore.isAmbientNoiseBoostEnabled &&
            AmbientNoiseSampler.hasRecordAudioPermission(appContext)

        if (shouldCheckAmbient) {
            val thresholdDb = sessionStore.ambientNoiseThresholdDb
            val boostAmt = sessionStore.ambientNoiseBoostPercent
            val startAt = System.currentTimeMillis()
            Thread {
                val measuredDb = AmbientNoiseSampler.measureAmbientDb(appContext)
                if (measuredDb != null && measuredDb >= thresholdDb) {
                    newRequest.ambientBoostPercent = boostAmt
                }
                val elapsed = System.currentTimeMillis() - startAt
                val remainingDelay = (delayMs - elapsed).coerceAtLeast(0L)
                mainHandler.postDelayed({
                    if (request === newRequest) {
                        speakCurrentSection(tts, newRequest)
                    }
                }, remainingDelay)
            }.apply {
                name = "hearit-ambient-sample"
                isDaemon = true
                start()
            }
        } else if (delayMs > 0L) {
            mainHandler.postDelayed({
                if (request === newRequest) {
                    speakCurrentSection(tts, newRequest)
                }
            }, delayMs)
        } else {
            speakCurrentSection(tts, newRequest)
        }
        return true
    }

    override fun stop() {
        releaseLoudnessEnhancer()
        engine?.stop()
        finishCurrent(TtsErrorCode.INTERRUPTED, notify = true)
    }

    override fun shutdown() {
        releaseLoudnessEnhancer()
        finishCurrent(TtsErrorCode.INTERRUPTED, notify = true)
        engine?.shutdown()
        engine = null
        ready = false
    }

    private fun applyVoice(tts: TextToSpeech, voiceId: String?) {
        if (voiceId == null) return
        tts.voices?.firstOrNull { it.name == voiceId }?.let { tts.voice = it }
    }

    private fun speakCurrentSection(tts: TextToSpeech, active: Request) {
        val section = active.announcement.sections[active.sectionIndex]
        val utteranceId = utteranceId(active, active.sectionIndex)

        val totalBoostPercent = (if (sessionStore.isVoiceBoostEnabled) sessionStore.voiceBoostPercent else 0) +
            active.ambientBoostPercent
        val effectiveVolume = (active.settings.volumeLevel + (totalBoostPercent / 100f)).coerceIn(0f, 1f)

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, effectiveVolume)
            if (sessionStore.isMonoFoldDownEnabled) {
                putFloat(TextToSpeech.Engine.KEY_PARAM_PAN, 0.0f)
            }
            if (totalBoostPercent > 0) {
                prepareLoudnessEnhancer(totalBoostPercent)?.let { sessionId ->
                    putInt(TextToSpeech.Engine.KEY_PARAM_SESSION_ID, sessionId)
                }
            } else {
                releaseLoudnessEnhancer()
            }
        }
        val result = tts.speak(section.text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        if (result != TextToSpeech.SUCCESS) {
            finishCurrent(TtsErrorCode.SYNTHESIS_FAILED, notify = true)
        }
    }

    private fun prepareLoudnessEnhancer(boostPercent: Int): Int? {
        return runCatching {
            val sessionId = currentAudioSessionId.takeIf { it > 0 }
                ?: audioManager?.generateAudioSessionId()?.takeIf { it > 0 }
                ?: return null
            currentAudioSessionId = sessionId
            val targetGainMb = (boostPercent * 30).coerceIn(100, 1800)
            val enhancer = loudnessEnhancer ?: LoudnessEnhancer(sessionId).also { loudnessEnhancer = it }
            enhancer.setTargetGain(targetGainMb)
            enhancer.enabled = true
            sessionId
        }.getOrNull()
    }

    private fun releaseLoudnessEnhancer() {
        runCatching {
            loudnessEnhancer?.enabled = false
            loudnessEnhancer?.release()
        }
        loudnessEnhancer = null
        currentAudioSessionId = AudioManager.AUDIO_SESSION_ID_GENERATE
    }

    private fun onSectionStarted(utteranceId: String?) {
        val active = request ?: return
        if (utteranceId != utteranceId(active, active.sectionIndex)) return
        if (!active.startNotified) {
            active.startNotified = true
            active.listener.onStart(active.announcement)
        }
    }

    private fun onSectionDone(utteranceId: String?) {
        val active = request ?: return
        if (utteranceId != utteranceId(active, active.sectionIndex)) return

        val nextIndex = active.sectionIndex + 1
        if (nextIndex >= active.announcement.sections.size) {
            releaseLoudnessEnhancer()
            request = null
            active.listener.onComplete(active.announcement)
            return
        }

        active.sectionIndex = nextIndex
        val tts = engine ?: run {
            finishCurrent(TtsErrorCode.ENGINE_UNAVAILABLE, notify = true)
            return
        }
        val pause = active.announcement.pauseBetweenSectionsMillis
        if (pause > 0L) {
            mainHandler.postDelayed({ if (request === active) speakCurrentSection(tts, active) }, pause)
        } else {
            speakCurrentSection(tts, active)
        }
    }

    private fun onSectionFailed(utteranceId: String?) {
        val active = request ?: return
        if (utteranceId != utteranceId(active, active.sectionIndex)) return
        finishCurrent(TtsErrorCode.SYNTHESIS_FAILED, notify = true)
    }

    private fun finishCurrent(code: TtsErrorCode, notify: Boolean) {
        releaseLoudnessEnhancer()
        val active = request ?: return
        request = null
        if (notify) {
            active.listener.onError(active.announcement, TtsError(code))
        }
    }

    private fun utteranceId(active: Request, sectionIndex: Int): String =
        "hearit-${active.token}-$sectionIndex"
}
