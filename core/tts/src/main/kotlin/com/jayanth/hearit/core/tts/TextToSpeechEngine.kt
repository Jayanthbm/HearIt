package com.jayanth.hearit.core.tts

import java.util.Locale

/**
 * Speech-level settings the engine applies where the installed engine supports them
 * (master prompt §7: "Configure speed, pitch and voice").
 *
 * Engine-neutral: [speechRate] and [pitch] are multipliers where `1.0` is the engine's
 * normal rate/pitch. The Android implementation passes them to `TextToSpeech.setSpeechRate`
 * / `setPitch` and clamps to the range that engine supports; invalid non-positive values
 * are rejected here so bad settings fail fast on any platform.
 *
 * @property speechRate Speaking speed multiplier. Default 1.0 (normal).
 * @property pitch Pitch multiplier. Default 1.0 (engine default).
 * @property voiceId Engine-specific voice id from [TextToSpeechEngine.listVoices], or
 *   null for the engine's default voice.
 * @property volumeLevel Speech volume multiplier between 0.0 and 1.0. Default 1.0 (full).
 */
data class SpeechSettings(
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f,
    val voiceId: String? = null,
    val volumeLevel: Float = 1.0f,
) {
    init {
        require(speechRate > 0f) { "SpeechSettings.speechRate must be > 0" }
        require(pitch > 0f) { "SpeechSettings.pitch must be > 0" }
        require(volumeLevel in 0f..1f) { "SpeechSettings.volumeLevel must be between 0 and 1" }
    }

    /** Returns these global settings with non-null fields from [override] applied (#6). */
    fun resolveWith(override: AppSpeechOverride?): SpeechSettings {
        if (override == null) return this
        return copy(
            speechRate = override.speechRate ?: speechRate,
            pitch = override.pitch ?: pitch,
            voiceId = override.voiceId ?: voiceId,
        )
    }
}

/**
 * Sparse per-app speech overrides (#6). Any null field inherits the global [SpeechSettings].
 */
data class AppSpeechOverride(
    val speechRate: Float? = null,
    val pitch: Float? = null,
    val voiceId: String? = null,
) {
    init {
        require(speechRate == null || speechRate > 0f) { "AppSpeechOverride.speechRate must be > 0" }
        require(pitch == null || pitch > 0f) { "AppSpeechOverride.pitch must be > 0" }
    }

    /** True when every field inherits the global voice/rate/pitch. */
    val inheritsEveryValue: Boolean
        get() = speechRate == null && pitch == null && voiceId == null
}

/**
 * One voice the installed TTS engine offers (master prompt §7). Platform-free so the
 * settings UI and tests can enumerate voices without Android types.
 *
 * @property id Stable id to pass as [SpeechSettings.voiceId].
 * @property displayName Human-readable name for the voice picker.
 * @property locale Locale this voice speaks; used to detect missing language data.
 * @property isNetworkConnectionRequired True when synthesis needs a network connection —
 *   surfaced to the user, never silently ignored.
 */
data class TtsVoice(
    val id: String,
    val displayName: String,
    val locale: Locale,
    val isNetworkConnectionRequired: Boolean = false,
)

/** Why a synthesis request failed; recorded as history reason detail, never as content. */
enum class TtsErrorCode {
    /** The engine is missing or not initialised — the item is not announced. */
    ENGINE_UNAVAILABLE,

    /** Installed voices have no usable data for the text's language; no fallback or
     *  translation is performed (master prompt §7: native TTS only, handle gracefully). */
    LANGUAGE_DATA_MISSING,

    /** The engine rejected or failed synthesis for any other reason. */
    SYNTHESIS_FAILED,

    /** Speech ended because [TextToSpeechEngine.stop] or a replacing speak was called. */
    INTERRUPTED,
}

/** Failure detail passed to [UtteranceListener.onError]; carries no notification content. */
data class TtsError(val code: TtsErrorCode)

/**
 * Lifecycle callbacks for one [TextToSpeechEngine.speak] request. All methods have empty
 * defaults; implementations must deliver exactly one terminal callback
 * (`onComplete` or `onError`) per started request.
 */
interface UtteranceListener {
    /** Synthesis of the announcement has started. */
    fun onStart(announcement: BuiltAnnouncement) {}

    /** Every section was spoken. */
    fun onComplete(announcement: BuiltAnnouncement) {}

    /** Speech ended early or never started for this request. */
    fun onError(announcement: BuiltAnnouncement, error: TtsError) {}
}

/**
 * Boundary the Android `TextToSpeech` wrapper will satisfy (master prompt §7; ADR-018).
 *
 * Pure Kotlin: no Android types leak through this contract, so the foreground service and
 * the settings UI can be tested against fakes. The contract requires native Android TTS
 * only — no translation — and treats unavailable language data as an explicit
 * [TtsErrorCode.LANGUAGE_DATA_MISSING] error rather than a silent fallback.
 *
 * Concurrency contract: at most one announcement is in progress per engine instance;
 * [stop] or a replacing [speak] ends the current request with
 * `onError(TtsError(INTERRUPTED))` for its listener before any new request starts.
 */
interface TextToSpeechEngine {

    /** True when synthesis can run at all (engine present and initialised). */
    fun isAvailable(): Boolean

    /** Voices the installed engine exposes; empty when unavailable. */
    fun listVoices(): List<TtsVoice>

    /** True when usable language data exists for [locale] (no translation fallback). */
    fun supportsLocale(locale: Locale): Boolean

    /**
     * Speaks [announcement] section by section, inserting
     * [BuiltAnnouncement.pauseBetweenSectionsMillis] of silence between them and applying
     * [settings] (speed/pitch/voice).
     *
     * @return true when the request was accepted; false when it was rejected without
     *   synthesis (empty announcement or unavailable engine), in which case [listener] is
     *   **not** invoked.
     */
    fun speak(
        announcement: BuiltAnnouncement,
        settings: SpeechSettings,
        listener: UtteranceListener,
    ): Boolean

    /** Stops any in-progress announcement immediately; its listener gets INTERRUPTED. */
    fun stop()

    /** Releases engine resources (service shutdown). The instance is unusable afterwards. */
    fun shutdown()
}
