package com.jayanth.hearit.service

import kotlin.math.roundToInt

/**
 * Pure helper calculating the temporary target music stream index when background
 * audio ducking level is explicitly controlled during an announcement.
 */
object AudioDuckingMath {

    /**
     * Scales [currentVolume] proportionally toward [minVolume] using [duckPercent] (`20..100`).
     * `duckPercent == 100` preserves [currentVolume] unchanged.
     */
    fun calculateTargetVolume(
        currentVolume: Int,
        minVolume: Int,
        maxVolume: Int,
        duckPercent: Int,
    ): Int {
        if (currentVolume <= minVolume || maxVolume <= minVolume) return currentVolume
        val clampedPercent = duckPercent.coerceIn(10, 100)
        if (clampedPercent >= 100) return currentVolume
        val range = currentVolume - minVolume
        val scaled = minVolume + (range * (clampedPercent / 100f)).roundToInt()
        return scaled.coerceIn(minVolume, currentVolume)
    }
}
