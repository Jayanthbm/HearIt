package com.jayanth.hearit.service

import android.os.SystemClock
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Shake sensitivity presets configuring dynamic acceleration threshold and reversal requirements.
 */
enum class ShakeSensitivity(
    val dynamicAccelThreshold: Float,
    val requiredReversals: Int,
    val windowMs: Long,
) {
    GENTLE(dynamicAccelThreshold = 8.0f, requiredReversals = 2, windowMs = 500L),
    BALANCED(dynamicAccelThreshold = 12.0f, requiredReversals = 2, windowMs = 400L),
    FIRM(dynamicAccelThreshold = 16.0f, requiredReversals = 3, windowMs = 400L),
}

/**
 * Multi-sample shake detector with baseline gravity subtraction and directional reversal filter.
 * Rejects unidirectional movements (phone pickups, tilts, pocket jostling) while reliably
 * detecting deliberate back-and-forth shaking.
 */
class ShakeOscillationDetector(
    var sensitivity: ShakeSensitivity = ShakeSensitivity.BALANCED,
) {
    private var lastSignX = 0
    private var lastSignY = 0
    private var lastSignZ = 0
    private val reversalTimes = LongArray(8)
    private var reversalCount = 0

    fun onSensorChanged(
        x: Float,
        y: Float,
        z: Float,
        nowMillis: Long = SystemClock.elapsedRealtime(),
    ): Boolean {
        // Dynamic acceleration on each axis (subtracting 1G baseline from Z)
        val absX = abs(x)
        val absY = abs(y)
        val absZ = abs(z - 9.81f)
        val dynamicPeak = maxOf(absX, absY, absZ)

        if (dynamicPeak < sensitivity.dynamicAccelThreshold) {
            return false
        }

        val currentSign: Int
        val previousSign: Int

        if (absX >= absY && absX >= absZ) {
            currentSign = if (x > 0) 1 else -1
            previousSign = lastSignX
            lastSignX = currentSign
        } else if (absY >= absX && absY >= absZ) {
            currentSign = if (y > 0) 1 else -1
            previousSign = lastSignY
            lastSignY = currentSign
        } else {
            currentSign = if (z - 9.81f > 0) 1 else -1
            previousSign = lastSignZ
            lastSignZ = currentSign
        }

        // Direction reversal detected on dominant axis
        if (previousSign != 0 && currentSign != previousSign) {
            recordReversal(nowMillis)
            if (countRecentReversals(nowMillis, sensitivity.windowMs) >= sensitivity.requiredReversals) {
                reset()
                return true
            }
        }
        return false
    }

    private fun recordReversal(now: Long) {
        val index = reversalCount % reversalTimes.size
        reversalTimes[index] = now
        reversalCount++
    }

    private fun countRecentReversals(now: Long, windowMs: Long): Int {
        var count = 0
        val totalRecorded = minOf(reversalCount, reversalTimes.size)
        for (i in 0 until totalRecorded) {
            val idx = (reversalCount - 1 - i + reversalTimes.size) % reversalTimes.size
            if (now - reversalTimes[idx] <= windowMs) {
                count++
            }
        }
        return count
    }

    fun reset() {
        lastSignX = 0
        lastSignY = 0
        lastSignZ = 0
        reversalCount = 0
        reversalTimes.fill(0L)
    }
}
