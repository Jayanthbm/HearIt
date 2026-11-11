package com.jayanth.hearit.voicespeech

import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveSpeedCalculatorTest {

    @Test
    fun `short messages under 20 words always keep base rate`() {
        assertEquals(
            1.0f,
            AdaptiveSpeedCalculator.resolveRate(
                baseRate = 1.0f,
                wordCount = 19,
                enabled = true,
                tier1Multiplier = 1.2f,
                tier2Multiplier = 1.35f,
                tier3Multiplier = 1.5f,
            ),
            0.001f,
        )
    }

    @Test
    fun `20 to 29 words applies tier 1 multiplier`() {
        assertEquals(
            1.15f,
            AdaptiveSpeedCalculator.resolveRate(
                baseRate = 1.0f,
                wordCount = 20,
                enabled = true,
            ),
            0.001f,
        )
        assertEquals(
            1.15f,
            AdaptiveSpeedCalculator.resolveRate(
                baseRate = 1.0f,
                wordCount = 29,
                enabled = true,
            ),
            0.001f,
        )
    }

    @Test
    fun `30 to 40 words applies tier 2 multiplier`() {
        assertEquals(
            1.25f,
            AdaptiveSpeedCalculator.resolveRate(
                baseRate = 1.0f,
                wordCount = 30,
                enabled = true,
            ),
            0.001f,
        )
        assertEquals(
            1.25f,
            AdaptiveSpeedCalculator.resolveRate(
                baseRate = 1.0f,
                wordCount = 40,
                enabled = true,
            ),
            0.001f,
        )
    }

    @Test
    fun `over 40 words applies tier 3 multiplier`() {
        assertEquals(
            1.40f,
            AdaptiveSpeedCalculator.resolveRate(
                baseRate = 1.0f,
                wordCount = 41,
                enabled = true,
            ),
            0.001f,
        )
    }

    @Test
    fun `disabled adaptive speed ignores all tiers`() {
        assertEquals(
            1.2f,
            AdaptiveSpeedCalculator.resolveRate(
                baseRate = 1.2f,
                wordCount = 65,
                enabled = false,
                tier1Multiplier = 1.25f,
                tier2Multiplier = 1.5f,
                tier3Multiplier = 1.75f,
            ),
            0.001f,
        )
    }

    @Test
    fun `effective rate clamps to engine maximum`() {
        assertEquals(
            AdaptiveSpeedCalculator.MAX_TTS_RATE,
            AdaptiveSpeedCalculator.resolveRate(
                baseRate = 2.0f,
                wordCount = 50,
                enabled = true,
                tier3Multiplier = 1.75f,
            ),
            0.001f,
        )
    }
}
