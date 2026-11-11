package com.jayanth.hearit.core.queue

import com.jayanth.hearit.core.common.model.AppPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Device-silence gate (ADR-017) at the queue boundary — written, not executed in the cloud. */
class AnnouncementQueueSilenceTest {

    @Test
    fun canAnnounce_tracksDeviceSilence() {
        var silent = true
        val queue = AnnouncementQueue(FakeClock(), deviceAudio = DeviceAudioState { silent })

        assertFalse(queue.canAnnounce)

        silent = false

        assertTrue(queue.canAnnounce)
    }

    @Test
    fun poll_silentDevice_returnsNoAnnouncement() {
        val queue = AnnouncementQueue(FakeClock(), deviceAudio = DeviceAudioState { true })
        queue.enqueue(testSnapshot("com.whatsapp|t|1"), AppPriority.NORMAL)

        assertNull(queue.pollReady().announcement)
    }

    @Test
    fun poll_afterSilenceEnds_announcesTheWaitingItem() {
        var silent = true
        val queue = AnnouncementQueue(FakeClock(), deviceAudio = DeviceAudioState { silent })
        queue.enqueue(testSnapshot("com.whatsapp|t|1"), AppPriority.NORMAL)

        assertNull(queue.pollReady().announcement)

        silent = false

        assertEquals(
            listOf("com.whatsapp|t|1"),
            queue.pollReady().announcement?.items?.map { it.key },
        )
    }

    @Test
    fun silentDevice_holdsTheSelectionBack_soNoIdIsIssued() {
        val queue = AnnouncementQueue(FakeClock(), deviceAudio = DeviceAudioState { true })
        queue.enqueue(testSnapshot("com.whatsapp|t|1"), AppPriority.NORMAL)

        repeat(3) {
            assertNull(queue.pollReady().announcement)
        }
    }
}
