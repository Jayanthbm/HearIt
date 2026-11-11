package com.jayanth.hearit.wear

import org.junit.Assert.assertEquals
import org.junit.Test

class WearConfigHelperTest {

    @Test
    fun normalizeWatchApps_deduplicatesAndCapsAtFour() {
        val normalized = WearConfigHelper.normalizeWatchApps(
            listOf("com.a", "com.b", "com.a", "com.c", "com.d", "com.e"),
        )
        assertEquals(listOf("com.a", "com.b", "com.c", "com.d"), normalized)
    }

    @Test
    fun toggleWatchAppSelection_blocksAddingFifthAppAndAllowsRemovingExisting() {
        var list = listOf("com.a", "com.b", "com.c", "com.d")
        // Attempt adding 5th app -> ignored
        list = WearConfigHelper.toggleWatchAppSelection(list, "com.e")
        assertEquals(listOf("com.a", "com.b", "com.c", "com.d"), list)

        // Removing one frees a slot -> then another can be added
        list = WearConfigHelper.toggleWatchAppSelection(list, "com.b")
        assertEquals(listOf("com.a", "com.c", "com.d"), list)
        list = WearConfigHelper.toggleWatchAppSelection(list, "com.e")
        assertEquals(listOf("com.a", "com.c", "com.d", "com.e"), list)
    }
}
