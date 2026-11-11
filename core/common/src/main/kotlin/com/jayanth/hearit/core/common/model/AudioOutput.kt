package com.jayanth.hearit.core.common.model

/**
 * Broad category of the device audio announcements are currently routed to
 * (master prompt §6, "Audio output and devices").
 */
enum class AudioOutputKind {
    /** Generic wired output (wired headset, USB audio) — the master prompt's "Wired" option. */
    WIRED,

    /** Any Bluetooth output (headphones, car kit, hearing aid). */
    BLUETOOTH,

    /** The phone's own speaker/earpiece. */
    PHONE_SPEAKER,

    /**
     * An output the platform did not classify (OEM-specific routes, remote submix, cast
     * and similar). Allowed by default so OEM variance can never silence announcements
     * silently — see ADR-017.
     */
    OTHER,
}

/**
 * Platform-free description of the active audio output, captured by the playback layer and
 * passed into the rules pipeline as part of [RuntimeState]. It contains no Android types so
 * the matching rules stay JVM-testable.
 *
 * @property kind Broad category used for allow/deny matching.
 * @property id Stable identity when the platform provides one (Bluetooth address or audio
 *   device address); drives per-device selection in [AudioOutputSettings]. Null when the
 *   platform exposes no identifier, which fails any explicit id allow-list.
 * @property name Human-readable name for settings/history; never used for matching.
 */
data class AudioOutput(
    val kind: AudioOutputKind,
    val id: String? = null,
    val name: String? = null,
)
