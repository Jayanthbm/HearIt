package com.jayanth.hearit.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Navigation model tests (P12). Written, **not executed** — cloud-agent restriction.
 *
 * These cover the restore path behind the acceptance criterion "navigation state survives
 * process recreation": the persisted id is a plain string, and resolving it must be
 * total and stable.
 */
class DashboardDestinationTest {

    @Test
    fun bottomBar_hasTheThreePillBarTabs_inOrder() {
        assertEquals(
            listOf(
                DashboardDestination.HOME,
                DashboardDestination.APPS,
                DashboardDestination.SETTINGS,
            ),
            DashboardDestination.bottomBar,
        )
    }

    @Test
    fun secondary_hasTheSevenSecondaryEntries_inOrder() {
        assertEquals(
            listOf(
                DashboardDestination.DEVICES,
                DashboardDestination.HISTORY,
                DashboardDestination.GLOBAL_RULES,
                DashboardDestination.VOICE_SPEECH,
                DashboardDestination.PREFERENCES,
                DashboardDestination.BACKUP_RESTORE,
                DashboardDestination.DIAGNOSTICS,
            ),
            DashboardDestination.secondary,
        )
    }

    @Test
    fun bottomBarAndSecondary_partitionEveryDestination() {
        val covered = DashboardDestination.bottomBar + DashboardDestination.secondary
        assertEquals(DashboardDestination.entries.toSet(), covered.toSet())
        assertEquals(DashboardDestination.entries.size, covered.size)
    }

    @Test
    fun fromId_restoresEveryDestinationExactly() {
        for (destination in DashboardDestination.entries) {
            assertEquals(destination, DashboardDestination.fromId(destination.id))
        }
    }

    @Test
    fun fromId_unknownOrMissingId_fallsBackToHome() {
        assertEquals(DashboardDestination.HOME, DashboardDestination.fromId("NOT_A_DESTINATION"))
        assertEquals(DashboardDestination.HOME, DashboardDestination.fromId(null))
        assertEquals(DashboardDestination.HOME, DashboardDestination.fromId(""))
    }

    @Test
    fun ids_areUniqueAndNeverBlank() {
        val ids = DashboardDestination.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.none { it.isBlank() })
        assertNotEquals(DashboardDestination.HOME.id, DashboardDestination.APPS.id)
    }

    @Test
    fun everyDestination_hasADistinctLabelResource() {
        val labels = DashboardDestination.entries.map { it.labelRes }
        assertEquals(labels.size, labels.toSet().size)
    }

    @Test
    fun onlyHomeIsSelectedByDefault() {
        val restored = DashboardDestination.fromId(null)
        assertTrue(restored.isBottomBarDestination)
        assertFalse(DashboardDestination.DIAGNOSTICS.isBottomBarDestination)
    }
}
