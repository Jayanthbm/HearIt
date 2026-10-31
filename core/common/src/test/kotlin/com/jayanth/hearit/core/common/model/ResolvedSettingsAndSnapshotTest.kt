package com.jayanth.hearit.core.common.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.Assert.assertThrows

/**
 * Domain-model invariants: "all apps disabled by default" (master prompt §5) and snapshot
 * identity validation used by deduplication in later phases.
 */
class ResolvedSettingsAndSnapshotTest {

    @Test
    fun resolvedSettings_allAppsDisabledByDefault() {
        assertFalse(ResolvedSettings().appEnabled)
        assertFalse(ResolvedSettings().priorityAnnounceListed)
    }

    @Test
    fun snapshot_defaults_leaveOptionalContentEmpty() {
        val snapshot = NotificationSnapshot(
            key = "com.example.app|tag|7",
            packageName = "com.example.app",
            appName = "Example",
            postTimeMillis = 0L,
        )
        assertEquals(null, snapshot.sender)
        assertEquals("", snapshot.messageText)
        assertFalse(snapshot.isUpdate)
    }

    @Test
    fun snapshot_blankKey_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            NotificationSnapshot(
                key = " ",
                packageName = "com.example.app",
                appName = "Example",
                postTimeMillis = 0L,
            )
        }
    }

    @Test
    fun snapshot_blankPackageName_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            NotificationSnapshot(
                key = "com.example.app|tag|7",
                packageName = "",
                appName = "Example",
                postTimeMillis = 0L,
            )
        }
    }

    @Test
    fun snapshot_negativePostTime_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            NotificationSnapshot(
                key = "com.example.app|tag|7",
                packageName = "com.example.app",
                appName = "Example",
                postTimeMillis = -1L,
            )
        }
    }
}
