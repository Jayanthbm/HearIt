package com.jayanth.hearit.voicespeech

import java.util.Locale
import kotlin.math.roundToInt

/**
 * Resolves effective TTS speech rate across explicit word-count brackets (#18):
 * - Under 20 words (`< 20`): uses base speech rate unchanged (`1.0x`)
 * - 20 to under 30 words (`20..29`): applies [tier1Multiplier] (default `1.15x`)
 * - 30 to 40 words (`30..40`): applies [tier2Multiplier] (default `1.25x`)
 * - Over 40 words (`> 40`): applies [tier3Multiplier] (default `1.4x`)
 */
object AdaptiveSpeedCalculator {
    const val TIER_1_MIN_WORDS = 20
    const val TIER_2_MIN_WORDS = 30
    const val TIER_2_MAX_WORDS = 40
    const val MAX_TTS_RATE = 2.5f
    const val MIN_TTS_RATE = 0.5f

    const val DEFAULT_TIER_1_MULTIPLIER = 1.15f
    const val DEFAULT_TIER_2_MULTIPLIER = 1.25f
    const val DEFAULT_TIER_3_MULTIPLIER = 1.40f

    fun countWords(texts: Iterable<String>): Int =
        texts.sumOf { section ->
            section.split(Regex("\\s+")).count { it.isNotBlank() }
        }

    fun resolveMultiplier(
        wordCount: Int,
        enabled: Boolean,
        tier1Multiplier: Float = DEFAULT_TIER_1_MULTIPLIER,
        tier2Multiplier: Float = DEFAULT_TIER_2_MULTIPLIER,
        tier3Multiplier: Float = DEFAULT_TIER_3_MULTIPLIER,
    ): Float {
        if (!enabled || wordCount < TIER_1_MIN_WORDS) return 1.0f
        val selected = when {
            wordCount < TIER_2_MIN_WORDS -> tier1Multiplier
            wordCount <= TIER_2_MAX_WORDS -> tier2Multiplier
            else -> tier3Multiplier
        }
        return selected.coerceIn(1.0f, 2.0f)
    }

    fun resolveRate(
        baseRate: Float,
        wordCount: Int,
        enabled: Boolean,
        tier1Multiplier: Float = DEFAULT_TIER_1_MULTIPLIER,
        tier2Multiplier: Float = DEFAULT_TIER_2_MULTIPLIER,
        tier3Multiplier: Float = DEFAULT_TIER_3_MULTIPLIER,
    ): Float {
        val multiplier = resolveMultiplier(
            wordCount = wordCount,
            enabled = enabled,
            tier1Multiplier = tier1Multiplier,
            tier2Multiplier = tier2Multiplier,
            tier3Multiplier = tier3Multiplier,
        )
        val raw = baseRate * multiplier
        return ((raw * 100f).roundToInt() / 100f).coerceIn(MIN_TTS_RATE, MAX_TTS_RATE)
    }

    fun formatMultiplier(multiplier: Float): String {
        val rounded = ((multiplier * 100f).roundToInt() / 100f)
        val trimmed = String.format(Locale.US, "%.2f", rounded).trimEnd('0').trimEnd('.')
        return "${trimmed}×"
    }
}
