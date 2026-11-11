package com.jayanth.hearit.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioDuckingMathTest {

    @Test
    fun calculateTargetVolume_scalesProportionallyBetweenMinAndCurrent() {
        // Range 0..10, 50% -> 5
        assertEquals(5, AudioDuckingMath.calculateTargetVolume(10, 0, 15, 50))
        // Range 0..10, 20% -> 2
        assertEquals(2, AudioDuckingMath.calculateTargetVolume(10, 0, 15, 20))
        // 100% leaves volume untouched
        assertEquals(10, AudioDuckingMath.calculateTargetVolume(10, 0, 15, 100))
        // Already at min leaves at min
        assertEquals(0, AudioDuckingMath.calculateTargetVolume(0, 0, 15, 50))
    }

    @Test
    fun ambientNoiseSampler_computesDbSplSafely() {
        val silent = AmbientNoiseSampler.computeDbSplFromRms(0.0)
        val moderate = AmbientNoiseSampler.computeDbSplFromRms(500.0)
        val loud = AmbientNoiseSampler.computeDbSplFromRms(5000.0)

        assertEquals(20.0, silent, 0.01)
        assertTrue(moderate < loud)
        assertTrue(loud >= 70.0)
    }
}
