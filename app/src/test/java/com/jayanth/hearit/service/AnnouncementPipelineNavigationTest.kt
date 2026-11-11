package com.jayanth.hearit.service

import com.jayanth.hearit.core.common.model.AppPriority
import com.jayanth.hearit.core.common.model.AudioOutput
import com.jayanth.hearit.core.common.model.AudioOutputKind
import com.jayanth.hearit.core.common.model.HistoryStatus
import com.jayanth.hearit.core.common.model.NavigationPriorityDetector
import com.jayanth.hearit.core.common.model.NotificationSnapshot
import com.jayanth.hearit.core.common.model.QueueSettings
import com.jayanth.hearit.core.common.model.ResolvedSettings
import com.jayanth.hearit.core.common.model.RuntimeState
import com.jayanth.hearit.core.common.time.Clock
import com.jayanth.hearit.core.queue.AnnouncementQueue
import com.jayanth.hearit.core.tts.AnnouncementFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnnouncementPipelineNavigationTest {

    @Test
    fun `deferActiveSpeechForNavigation does not mark interrupted and re-announces with app name`() {
        var now = 1_000L
        val clock = Clock { now }
        val queue = AnnouncementQueue(
            clock = clock,
            settings = QueueSettings(
                delayBeforeAnnouncementMillis = 0L,
                minGapBetweenAnnouncementsMillis = 500L,
            ),
        )
        val historyRecords = mutableListOf<AnnouncementPipeline.HistoryRecord>()
        val pipeline = AnnouncementPipeline(
            queue = queue,
            clock = { now },
            onRecord = { historyRecords += it },
        )

        val state = RuntimeState(
            screenOn = false,
            isLocked = true,
            temporaryMuteActive = false,
            priorityAnnounceActive = false,
            isCallActive = false,
            isDndActive = false,
            activeOutput = AudioOutput(AudioOutputKind.BLUETOOTH, "buds-1"),
        )
        val enabledSettings = ResolvedSettings(appEnabled = true)

        val wa = NotificationSnapshot(
            key = "wa|msg|1",
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            sender = "Rahul",
            messageText = "Reach home safely",
            postTimeMillis = now,
        )
        pipeline.onNotification(wa, enabledSettings, state, priority = AppPriority.NORMAL)

        val format = AnnouncementFormat(includeAppName = true, includeSender = true, includeMessage = true)
        val startedWa = pipeline.tick(
            state = state,
            announcementFormat = format,
            isAppNameDedupEnabled = true,
            settingsOf = { enabledSettings },
        )
        assertNotNull(startedWa)
        assertTrue(pipeline.isSpeaking)
        assertFalse(pipeline.isSpeakingNavigation())

        // Defer WhatsApp while Maps speaks turn-by-turn guidance
        val requeuedCount = pipeline.deferActiveSpeechForNavigation()
        assertEquals(1, requeuedCount)
        assertFalse(pipeline.isSpeaking)
        assertEquals(1, pipeline.pendingCount)
        assertTrue(historyRecords.none { it.status == HistoryStatus.INTERRUPTED })

        // Maps notification arrives and pre-empts cleanly
        val maps = NotificationSnapshot(
            key = "maps|nav|9",
            packageName = "com.google.android.apps.maps",
            appName = "Maps",
            sender = null,
            messageText = "Turn right onto Inner Ring Road",
            category = NavigationPriorityDetector.CATEGORY_NAVIGATION,
            postTimeMillis = now,
        )
        pipeline.onNotification(maps, enabledSettings, state, priority = AppPriority.CRITICAL)

        val startedMaps = pipeline.tick(
            state = state,
            announcementFormat = format,
            isAppNameDedupEnabled = true,
            settingsOf = { enabledSettings },
        )
        assertNotNull(startedMaps)
        assertEquals("com.google.android.apps.maps", startedMaps!!.selection.packageName)
        assertTrue(pipeline.isSpeakingNavigation())
        pipeline.onSpeechFinished(spoken = true)

        // Advance past minGap and resume deferred WhatsApp notification WITH full app name intro
        now += 1_000L
        val resumedWa = pipeline.tick(
            state = state,
            announcementFormat = format,
            isAppNameDedupEnabled = true,
            settingsOf = { enabledSettings },
        )
        assertNotNull(resumedWa)
        assertEquals("com.whatsapp", resumedWa!!.selection.packageName)
        assertTrue(
            resumedWa.announcement.sections.any { it.text.contains("WhatsApp") },
        )
    }
}
