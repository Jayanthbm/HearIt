package com.jayanth.hearit.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShakeOscillationDetectorTest {

    @Test
    fun onSensorChanged_flatOnTable_returnsFalse() {
        val detector = ShakeOscillationDetector(ShakeSensitivity.BALANCED)
        // 1G pointing along Z
        val detected = detector.onSensorChanged(0f, 0f, 9.81f, nowMillis = 1000L)
        assertFalse(detected)
    }

    @Test
    fun onSensorChanged_oneWayPickup_returnsFalse() {
        val detector = ShakeOscillationDetector(ShakeSensitivity.BALANCED)
        var t = 1000L

        // Casual pickup adds upward acceleration then levels off without rapid reversals
        assertFalse(detector.onSensorChanged(0f, 0f, 16.0f, nowMillis = t))
        t += 50
        assertFalse(detector.onSensorChanged(0f, 2.0f, 15.0f, nowMillis = t))
        t += 50
        assertFalse(detector.onSensorChanged(0f, 3.0f, 13.0f, nowMillis = t))
        t += 50
        assertFalse(detector.onSensorChanged(0f, 1.0f, 10.0f, nowMillis = t))
    }

    @Test
    fun onSensorChanged_deliberateOscillatingShake_returnsTrue() {
        val detector = ShakeOscillationDetector(ShakeSensitivity.BALANCED)
        var t = 1000L

        // Shake along X axis: +X -> -X -> +X with dynamic accel > 12
        assertFalse(detector.onSensorChanged(15.0f, 0f, 9.81f, nowMillis = t))
        t += 100
        // First reversal (+ to -)
        assertFalse(detector.onSensorChanged(-16.0f, 0f, 9.81f, nowMillis = t))
        t += 100
        // Second reversal (- to +) within 400ms -> should trigger!
        val detected = detector.onSensorChanged(15.5f, 0f, 9.81f, nowMillis = t)
        assertTrue(detected)
    }

    @Test
    fun onSensorChanged_slowMovement_exceedingWindow_returnsFalse() {
        val detector = ShakeOscillationDetector(ShakeSensitivity.BALANCED)
        var t = 1000L

        // First motion
        assertFalse(detector.onSensorChanged(15.0f, 0f, 9.81f, nowMillis = t))
        t += 300
        // First reversal
        assertFalse(detector.onSensorChanged(-16.0f, 0f, 9.81f, nowMillis = t))
        t += 500 // Too slow (total 800ms > 400ms window)
        // Second reversal happened too late
        val detected = detector.onSensorChanged(15.5f, 0f, 9.81f, nowMillis = t)
        assertFalse(detected)
    }
}
