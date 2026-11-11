package com.jayanth.hearit.core.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** `NotificationKeyTracker` semantics (written for P07, not executed in the cloud). */
class NotificationKeyTrackerTest {

    @Test
    fun markSeen_firstTime_returnsFalse() {
        val tracker = NotificationKeyTracker()

        assertFalse(tracker.markSeen("com.app|tag|1"))
        assertEquals(1, tracker.size)
    }

    @Test
    fun markSeen_secondTime_returnsTrue() {
        val tracker = NotificationKeyTracker()
        tracker.markSeen("com.app|tag|1")

        assertTrue(tracker.markSeen("com.app|tag|1"))
        assertEquals(1, tracker.size)
    }

    @Test
    fun markSeen_thirdTime_returnsTrue() {
        val tracker = NotificationKeyTracker()
        tracker.markSeen("com.app|tag|1")
        tracker.markSeen("com.app|tag|1")

        assertTrue(tracker.markSeen("com.app|tag|1"))
    }

    @Test
    fun forget_makesTheNextPostNew() {
        val tracker = NotificationKeyTracker()
        tracker.markSeen("com.app|tag|1")
        tracker.forget("com.app|tag|1")

        assertFalse(tracker.markSeen("com.app|tag|1"))
        assertEquals(1, tracker.size)
    }

    @Test
    fun exceedingCapacity_evictsTheLeastRecentlyMarked() {
        val tracker = NotificationKeyTracker(maxEntries = 2)
        tracker.markSeen("a")
        tracker.markSeen("b")
        tracker.markSeen("c") // evicts a (least recently marked)

        assertEquals(2, tracker.size)
        assertFalse(tracker.markSeen("a")) // evicted: reported as new again
        assertEquals(2, tracker.size)
    }

    @Test
    fun reMarking_refreshesRecency() {
        val tracker = NotificationKeyTracker(maxEntries = 2)
        tracker.markSeen("a")
        tracker.markSeen("b")
        tracker.markSeen("a") // refresh a
        tracker.markSeen("c") // must evict b, not a

        assertTrue(tracker.markSeen("a"))
        assertEquals(2, tracker.size)
    }

    @Test
    fun clear_resetsEverything() {
        val tracker = NotificationKeyTracker()
        tracker.markSeen("a")
        tracker.markSeen("b")

        tracker.clear()

        assertEquals(0, tracker.size)
        assertFalse(tracker.markSeen("a"))
    }

    @Test
    fun nonPositiveCapacity_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            NotificationKeyTracker(maxEntries = 0)
        }
    }
}
