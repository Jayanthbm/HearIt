package com.jayanth.hearit.service

import android.content.Context
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
import java.util.Locale

/**
 * Android `TextToSpeech` wrapper satisfying the P04 [TextToSpeechEngine] contract
 * (ADR-018: native TTS only, no translation, no network services of our own).
 *
 * Section handling mirrors [BuiltAnnouncement]: sections are spoken one at a time in order
 * with [BuiltAnnouncement.pauseBetweenSectionsMillis] of silence between them, because the
 * template's structure (not one flat string) is what the format settings control.
 *
 * Contract fidelity:
 *  - at most one request is in flight; [speak] or [stop] ends the previous one with
 *    `INTERRUPTED` before anything new starts;
 *  - every accepted request delivers exactly one terminal callback (`onComplete` or
 *    `onError`) on the main thread;
 *  - an unavailable engine or an empty announcement returns `false` **without** calling the
 *    listener (the caller retries on a later tick);
 *  - a missing language for the system locale is reported as
 *    [TtsErrorCode.LANGUAGE_DATA_MISSING], never silently substituted.
 *
 * Not locally verified — written under the cloud-agent restriction (no build/test run).
 */
class AndroidTextToSpeechEngine(context: Context) : TextToSpeechEngine {

    private val mainHandler = Handler(Looper.getMainLooper())

    private var engine: TextToSpeech? = null
    private var ready = false
    private var request: Request? = null
    private var sessionToken = 0L

    private data class Request(
        val announcement: BuiltAnnouncement,
        val listener: UtteranceListener,
        val token: Long,
        var sectionIndex: Int = 0,
        var startNotified: Boolean = false,
    )

    init {
        val created = TextToSpeech(context.applicationContext) { status ->
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

        // One announcement at a time: end the previous request explicitly.
        finishCurrent(TtsErrorCode.INTERRUPTED, notify = true)

        if (!supportsLocale(Locale.getDefault())) {
            listener.onError(announcement, TtsError(TtsErrorCode.LANGUAGE_DATA_MISSING))
            return true
        }

        tts.setSpeechRate(settings.speechRate)
        tts.setPitch(settings.pitch)
        applyVoice(tts, settings.voiceId)

        val newRequest = Request(announcement, listener, token = ++sessionToken)
        request = newRequest
        speakCurrentSection(tts, newRequest)
        return true
    }

    override fun stop() {
        engine?.stop()
        finishCurrent(TtsErrorCode.INTERRUPTED, notify = true)
    }

    override fun shutdown() {
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
        val result = tts.speak(section.text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        if (result != TextToSpeech.SUCCESS) {
            finishCurrent(TtsErrorCode.SYNTHESIS_FAILED, notify = true)
        }
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

    /** Ends the in-flight request exactly once, optionally notifying its listener. */
    private fun finishCurrent(code: TtsErrorCode, notify: Boolean) {
        val active = request ?: return
        request = null
        if (notify) {
            active.listener.onError(active.announcement, TtsError(code))
        }
    }

    private fun utteranceId(active: Request, sectionIndex: Int): String =
        "hearit-${active.token}-$sectionIndex"
}
