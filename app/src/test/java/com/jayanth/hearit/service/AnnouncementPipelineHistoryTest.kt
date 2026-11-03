package com.jayanth.hearit.service

import com.jayanth.hearit.core.common.model.AppPriority
import com.jayanth.hearit.core.common.model.HistoryStatus
import com.jayanth.hearit.core.common.model.NotificationSnapshot
import com.jayanth.hearit.core.common.model.ResolvedSettings
import com.jayanth.hearit.core.common.model.RuntimeState
import com.jayanth.hearit.core.common.model.SkipReason
import com.jayanth.hearit.core.common.time.Clock
import com.jayanth.hearit.core.queue.AnnouncementQueue
import com.jayanth.hearit.core.tts.AnnouncementFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * L3: every pipeline outcome must reach the history sink with the status ADR-007 and the
 * master prompt §12 vocabulary require — including skips, and without claiming ANNOUNCED for
 * something that was never spoken.
 */
class AnnouncementPipelineHistoryTest {

    private class TestClock(var now: Long = 0L) : Clock {
        override fun nowMillis(): Long = now
    }

    private val enabled = ResolvedSettings(appEnabled = true)
    private val disabled = ResolvedSettings(appEnabled = false)

    private val recorded = mutableListOf<AnnouncementPipeline.HistoryRecord>()

    private fun snapshot(
        key: String = "com.whatsapp|t|1",
        packageName: String = "com.whatsapp",
        messageText: String = "hi",
    ) = NotificationSnapshot(
        key = key,
        packageName = packageName,
        appName = "WhatsApp",
        sender = "Rahul",
        messageText = messageText,
        postTimeMillis = 0L,
    )

    private fun state(screenOn: Boolean = false, locked: Boolean = true) = RuntimeState(
        screenOn = screenOn,
        isLocked = locked,
        temporaryMuteActive = false,
        priorityAnnounceActive = false,
        isCallActive = false,
        isDndActive = false,
        activeOutput = null,
    )

    private fun pipeline(
        format: AnnouncementFormat = AnnouncementFormat(),
    ) = AnnouncementPipeline(
        queue = AnnouncementQueue(clock = TestClock()),
        format = format,
        onRecord = { record -> recorded += record },
    )

    private fun statuses() = recorded.map { it.status }

    @Test
    fun onNotification_disabledApp_recordsSkippedWithReason() {
        pipeline().onNotification(snapshot(), disabled, state())

        val record = recorded.single()
        assertEquals(HistoryStatus.SKIPPED, record.status)
        assertEquals(SkipReason.APP_DISABLED, record.reason)
        assertEquals("com.whatsapp|t|1", record.snapshot.key)
    }

    @Test
    fun onNotification_announceable_recordsQueued() {
        pipeline().onNotification(snapshot(), enabled, state())

        assertEquals(listOf(HistoryStatus.QUEUED), statuses())
        assertNull(recorded.single().reason)
    }

    @Test
    fun recordedRecordsCarryTheAppPriority() {
        pipeline().onNotification(snapshot(), enabled, state(), priority = AppPriority.HIGH)

        assertEquals(AppPriority.HIGH, recorded.single().priority)
    }

    @Test
    fun speechCompleted_recordsAnnouncedAfterQueued() {
        val pipeline = pipeline()
        pipeline.onNotification(snapshot(), enabled, state())
        pipeline.tick(state()) { enabled }

        pipeline.onSpeechFinished()

        assertEquals(listOf(HistoryStatus.QUEUED, HistoryStatus.ANNOUNCED), statuses())
    }

    @Test
    fun speechStopped_recordsInterruptedInsteadOfAnnounced() {
        val pipeline = pipeline()
        pipeline.onNotification(snapshot(), enabled, state())
        pipeline.tick(state()) { enabled }

        pipeline.onSpeechFinished(spoken = false)

        assertTrue(HistoryStatus.ANNOUNCED !in statuses())
        assertEquals(HistoryStatus.INTERRUPTED, recorded.last().status)
    }

    @Test
    fun tick_everySectionDisabled_recordsSkippedNotAnnounced() {
        val pipeline = pipeline(
            format = AnnouncementFormat(
                includeAppName = false,
                includeSender = false,
                includeMessage = false,
            ),
        )
        pipeline.onNotification(snapshot(), enabled, state())

        assertNull(pipeline.tick(state()) { enabled })
        assertNull(pipeline.tick(state()) { enabled })

        assertEquals(listOf(HistoryStatus.QUEUED, HistoryStatus.SKIPPED), statuses())
    }

    @Test
    fun onNotification_screenOn_recordsSkipReasonFromTheRules() {
        pipeline().onNotification(snapshot(), enabled, state(screenOn = true))

        assertEquals(HistoryStatus.SKIPPED, recorded.single().status)
        assertEquals(SkipReason.SCREEN_ON, recorded.single().reason)
    }
}
