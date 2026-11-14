package com.jayanth.hearit.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAiEnhancerTest {

    @Test
    fun formatStructuredEntities_spellsTrackingAndFlightsCleanly() {
        val enhanced = LocalAiEnhancer.formatStructuredEntities(
            text = "Package shipped via 1Z999AA and flight AI202",
            trackingNumbers = listOf("1Z999AA"),
            flightNumbers = listOf("AI202"),
        )
        assertTrue(enhanced.contains("tracking code 1 Z 9 9 9 A A"))
        assertTrue(enhanced.contains("flight A I 2 0 2"))
    }

    @Test
    fun capabilityReport_derivesSupportFlagsCorrectly() {
        val unsupported = LocalAiCapabilityReport(
            mlKitStatus = MlKitModelStatus.DOWNLOAD_REQUIRED,
            mlKitStatusLabel = "Supported · Model Not Downloaded",
        )
        assertTrue(unsupported.mlKitSupported)
        assertFalse(unsupported.mlKitModelReady)

        val ready = unsupported.copy(
            mlKitStatus = MlKitModelStatus.READY,
        )
        assertEquals(MlKitModelStatus.READY, ready.mlKitStatus)
        assertTrue(ready.mlKitModelReady)
    }
}
