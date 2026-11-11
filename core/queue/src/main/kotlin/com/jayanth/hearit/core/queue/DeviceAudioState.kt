package com.jayanth.hearit.core.queue

/**
 * Device audio silence as the announcement queue needs it (ADR-017).
 *
 * `:core:queue` is a pure Kotlin/JVM module with **no Android dependency**, so the queue
 * cannot read `AudioManager` itself. The Android layer supplies this abstraction:
 *
 * ```
 * DeviceAudioState { audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT }
 * ```
 *
 * Silent means nothing may be spoken until silence ends; Vibrate-only keeps the standard
 * notification sound and is therefore *not* silent (ADR-017).
 */
fun interface DeviceAudioState {

    /** True while the device is silent (ringer mode Silent). */
    fun isSilent(): Boolean
}
