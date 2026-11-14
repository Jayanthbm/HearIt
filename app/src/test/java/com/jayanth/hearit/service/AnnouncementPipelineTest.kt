package com.jayanth.hearit.service

import com.jayanth.hearit.core.common.model.EligibilitySettings
import com.jayanth.hearit.core.common.model.NotificationSnapshot
import com.jayanth.hearit.core.common.model.ResolvedSettings
import com.jayanth.hearit.core.common.model.RuntimeState
import com.jayanth.hearit.core.common.model.SkipReason
import com.jayanth.hearit.core.common.time.Clock
import com.jayanth.hearit.core.queue.AnnouncementQueue
import com.jayanth.hearit.core.queue.DeviceAudioState
import com.jayanth.hearit.core.queue.QueueEvent
import com.jayanth.hearit.core.rules.EligibilityDecision
import com.jayanth.hearit.core.tts.AnnouncementFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pipeline orchestration rules (written for P08, not executed in the cloud). */
class AnnouncementPipelineTest {

    private class TestClock(var now: Long = 0L) : Clock {
        override fun nowMillis(): Long = now
    }

    private val enabled = ResolvedSettings(appEnabled = true)

    private fun snapshot(
        key: String = "com.whatsapp|t|1",
        packageName: String = "com.whatsapp",
        sender: String? = "Rahul",
        messageText: String = "hi",
        isUpdate: Boolean = false,
    ) = NotificationSnapshot(
        key = key,
        packageName = packageName,
        appName = "WhatsApp",
        sender = sender,
        messageText = messageText,
        postTimeMillis = 0L,
        isUpdate = isUpdate,
    )

    private fun state(
        screenOn: Boolean = false,
        locked: Boolean = true,
        mute: Boolean = false,
        call: Boolean = false,
        dnd: Boolean = false,
    ) = RuntimeState(
        screenOn = screenOn,
        isLocked = locked,
        temporaryMuteActive = mute,
        priorityAnnounceActive = false,
        isCallActive = call,
        isDndActive = dnd,
        activeOutput = null,
    )

    private fun newQueue(deviceAudio: DeviceAudioState = DeviceAudioState { false }) =
        AnnouncementQueue(clock = TestClock(), deviceAudio = deviceAudio)

    @Test
    fun onNotification_announceable_enqueuesAndReportsAnnounce() {
        val pipeline = AnnouncementPipeline(newQueue())

        val processed = pipeline.onNotification(snapshot(), enabled, state())

        assertEquals(EligibilityDecision.Announce, processed.decision)
        assertEquals(1, processed.queueEvents.size)
    }

    @Test
    fun onNotification_appDisabled_skipsWithoutQueueing() {
        val pipeline = AnnouncementPipeline(newQueue())

        val processed = pipeline.onNotification(snapshot(), ResolvedSettings(), state())

        assertTrue(processed.decision is EligibilityDecision.Skip)
        assertTrue(processed.queueEvents.isEmpty())
    }

    @Test
    fun onNotification_muted_skips() {
        val pipeline = AnnouncementPipeline(newQueue())

        val processed = pipeline.onNotification(snapshot(), enabled, state(mute = true))

        assertTrue(processed.decision is EligibilityDecision.Skip)
    }

    @Test
    fun onNotification_callActiveWithQueueing_holdsInsteadOfQueueing() {
        val pipeline = AnnouncementPipeline(newQueue())
        val settings = ResolvedSettings(
            appEnabled = true,
            eligibility = EligibilitySettings(queueDuringCall = true),
        )

        val processed = pipeline.onNotification(snapshot(), settings, state(call = true))

        assertTrue(processed.decision is EligibilityDecision.Queue)
        assertTrue(processed.queueEvents.single() is QueueEvent.Queued)
    }

    @Test
    fun onNotification_updateOfPendingItem_replacesInPlace() {
        val pipeline = AnnouncementPipeline(newQueue())
        pipeline.onNotification(snapshot(), enabled, state())

        val processed = pipeline.onNotification(snapshot(isUpdate = true), enabled, state())

        assertTrue(processed.queueEvents.single() is QueueEvent.Replaced)
    }

    @Test
    fun tick_emptyQueue_returnsNull() {
        val pipeline = AnnouncementPipeline(newQueue())

        assertNull(pipeline.tick(state()) { enabled })
    }

    @Test
    fun tick_enqueuedItem_returnsSpeakWithBuiltAnnouncement() {
        val pipeline = AnnouncementPipeline(newQueue())
        pipeline.onNotification(snapshot(), enabled, state())

        val action = pipeline.tick(state()) { enabled }

        assertTrue(action != null)
        assertEquals("com.whatsapp|t|1", action!!.selection.items.single().key)
        assertFalse(action.announcement.isEmpty)
        assertTrue(action.announcement.sections.any { it.text.contains("WhatsApp") })
        assertTrue(pipeline.isSpeaking)
    }

    @Test
    fun tick_whileSpeaking_returnsNull() {
        val pipeline = AnnouncementPipeline(newQueue())
        pipeline.onNotification(snapshot(), enabled, state())
        pipeline.tick(state()) { enabled }

        assertNull(pipeline.tick(state()) { enabled })
    }

    @Test
    fun onSpeechFinished_clearsSpeaking_andStartsTheGap() {
        val pipeline = AnnouncementPipeline(newQueue())
        pipeline.onNotification(snapshot(), enabled, state())
        pipeline.tick(state()) { enabled }

        val events = pipeline.onSpeechFinished()

        assertTrue(events.isNotEmpty()) // markCompleted reports the announced item
        assertFalse(pipeline.isSpeaking)
        assertNull(pipeline.tick(state()) { enabled }) // queue is empty now
    }

    @Test
    fun onSpeechFinished_withoutSpeech_isNoOp() {
        val pipeline = AnnouncementPipeline(newQueue())

        assertTrue(pipeline.onSpeechFinished().isEmpty())
    }

    @Test
    fun onStateChanged_withoutSpeech_returnsNoActions() {
        val pipeline = AnnouncementPipeline(newQueue())

        assertTrue(pipeline.onStateChanged(state()) { enabled }.isEmpty())
    }

    @Test
    fun onStateChanged_screenTurnsOn_stopsSpeech() {
        val pipeline = AnnouncementPipeline(newQueue())
        pipeline.onNotification(snapshot(), enabled, state())
        pipeline.tick(state()) { enabled }

        val actions = pipeline.onStateChanged(state(screenOn = true)) { enabled }

        assertEquals(
            AnnouncementPipeline.Action.StopSpeech(AnnouncementPipeline.StopReason.ELIGIBILITY_LOST),
            actions.single(),
        )
    }

    @Test
    fun onStateChanged_deviceBecomesSilent_stopsSpeech() {
        var silent = false
        val pipeline = AnnouncementPipeline(
            newQueue(deviceAudio = DeviceAudioState { silent }),
        )
        pipeline.onNotification(snapshot(), enabled, state())
        pipeline.tick(state()) { enabled }

        silent = true

        assertEquals(
            AnnouncementPipeline.Action.StopSpeech(AnnouncementPipeline.StopReason.DEVICE_SILENT),
            pipeline.onStateChanged(state()) { enabled }.single(),
        )
    }

    @Test
    fun onStateChanged_eligibilityStillMatching_keepsSpeaking() {
        val pipeline = AnnouncementPipeline(newQueue())
        pipeline.onNotification(snapshot(), enabled, state())
        pipeline.tick(state()) { enabled }

        assertTrue(pipeline.onStateChanged(state()) { enabled }.isEmpty())
    }

    @Test
    fun tick_everySectionDisabled_retiresGroupWithoutSpeaking() {
        val pipeline = AnnouncementPipeline(
            newQueue(),
            format = AnnouncementFormat(
                includeAppName = false,
                includeSender = false,
                includeMessage = false,
            ),
        )
        pipeline.onNotification(snapshot(), enabled, state())

        assertNull(pipeline.tick(state()) { enabled })
        assertFalse(pipeline.isSpeaking)
        assertNull(pipeline.tick(state()) { enabled }) // group was retired, not left pending
    }

    @Test
    fun releaseHeld_callEnded_reEvaluatesAndEnqueues() {
        val pipeline = AnnouncementPipeline(newQueue())
        val settings = ResolvedSettings(
            appEnabled = true,
            eligibility = EligibilitySettings(queueDuringCall = true),
        )
        pipeline.onNotification(snapshot(), settings, state(call = true))

        val released = pipeline.releaseHeld(state(call = false)) { settings }

        assertEquals(EligibilityDecision.Announce, released.single().decision)
        assertTrue(released.single().queueEvents.isNotEmpty())
    }

    @Test
    fun releaseHeld_stillBlocked_holdsAgainThroughTheFullPipeline() {
        val pipeline = AnnouncementPipeline(newQueue())
        val settings = ResolvedSettings(
            appEnabled = true,
            eligibility = EligibilitySettings(queueDuringCall = true),
        )
        pipeline.onNotification(snapshot(), settings, state(call = true))

        val released = pipeline.releaseHeld(state(call = true)) { settings }

        assertTrue(released.single().decision is EligibilityDecision.Queue)
    }

    @Test
    fun heldCount_duringCall_reportsHeldItemAndEmptiesPending() {
        val pipeline = AnnouncementPipeline(newQueue())
        val settings = ResolvedSettings(
            appEnabled = true,
            eligibility = EligibilitySettings(queueDuringCall = true),
        )

        pipeline.onNotification(snapshot(), settings, state(call = true))

        assertEquals(1, pipeline.heldCount)
        assertEquals(0, pipeline.pendingCount)
    }

    @Test
    fun releaseHeld_callEnded_clearsHeldCountAndRefillsQueue() {
        val pipeline = AnnouncementPipeline(newQueue())
        val settings = ResolvedSettings(
            appEnabled = true,
            eligibility = EligibilitySettings(queueDuringCall = true),
        )
        pipeline.onNotification(snapshot(), settings, state(call = true))

        pipeline.releaseHeld(state(call = false)) { settings }

        assertEquals(0, pipeline.heldCount)
        assertEquals(1, pipeline.pendingCount)
    }

    @Test
    fun onNotification_suppressedByIntelligence_skipsWithoutSpeaking() {
        com.jayanth.hearit.core.tts.SmartContentCleaner.isReactionFilterEnabled = true
        com.jayanth.hearit.core.tts.SmartContentCleaner.reactionFilterMode =
            com.jayanth.hearit.core.common.model.ReactionFilterMode.SILENCE
        try {
            val recorded = mutableListOf<AnnouncementPipeline.HistoryRecord>()
            val pipeline = AnnouncementPipeline(newQueue(), onRecord = { recorded += it })

            val processed = pipeline.onNotification(
                snapshot(messageText = "Liked a photo"),
                enabled,
                state(),
            )

            assertEquals(
                EligibilityDecision.Skip(com.jayanth.hearit.core.common.model.SkipReason.SUPPRESSED_INTELLIGENCE),
                processed.decision,
            )
            assertEquals(0, pipeline.pendingCount)
            assertEquals(
                com.jayanth.hearit.core.common.model.SkipReason.SUPPRESSED_INTELLIGENCE,
                recorded.single().reason,
            )
        } finally {
            com.jayanth.hearit.core.tts.SmartContentCleaner.reactionFilterMode =
                com.jayanth.hearit.core.common.model.ReactionFilterMode.CONDENSE
        }
    }

    @Test
    fun tick_respectsAppNameDedupToggleAndDelayedTimestampConfig() {
        val clock = TestClock(now = 5 * 60_000L)
        val pipeline = AnnouncementPipeline(
            queue = AnnouncementQueue(clock = Clock { clock.now }),
            clock = { clock.now },
        )
        val s1 = snapshot(key = "k1", messageText = "One").copy(postTimeMillis = 60_000L) // 4 mins old
        val s2 = snapshot(key = "k2", messageText = "Two").copy(postTimeMillis = 60_000L)
        pipeline.onNotification(s1, enabled, state())

        val first = pipeline.tick(
            state = state(),
            isAppNameDedupEnabled = true,
            appNameDedupWindowMs = 30_000L,
            isDelayedTimestampEnabled = true,
            delayedTimestampThresholdMs = 120_000L,
        ) { enabled }
        assertTrue(first!!.announcement.sections.any { it.text.contains("sent 4 minutes ago:") })
        pipeline.onSpeechFinished(true)

        // Rapid second notification within dedup window omits app name when enabled
        pipeline.onNotification(s2, enabled, state())
        val second = pipeline.tick(
            state = state(),
            isAppNameDedupEnabled = true,
            appNameDedupWindowMs = 30_000L,
            isDelayedTimestampEnabled = false,
        ) { enabled }
        assertFalse(second!!.announcement.sections.any { it.kind == com.jayanth.hearit.core.tts.SectionKind.APP_NAME })
    }

    @Test
    fun onNotification_spamCooldownThrottlesAndClearQueueFlushesPending() {
        val clock = TestClock(now = 1_000L)
        val recorded = mutableListOf<AnnouncementPipeline.HistoryRecord>()
        val pipeline = AnnouncementPipeline(
            queue = AnnouncementQueue(clock = Clock { clock.now }),
            clock = { clock.now },
            onRecord = { recorded += it },
        )

        repeat(8) { index ->
            clock.now += 100L
            pipeline.onNotification(
                snapshot = snapshot(key = "k$index", sender = "Rahul", messageText = "Spam #$index"),
                settings = enabled,
                state = state(),
                isSpamCooldownEnabled = true,
                spamCooldownMaxMessages = 3,
                spamCooldownWindowMs = 60_000L,
            )
        }

        // First 3 queued directly + 1 summary emitted at 5th throttled item = 4 pending
        assertEquals(4, pipeline.pendingCount)
        assertTrue(recorded.any { it.reason == com.jayanth.hearit.core.common.model.SkipReason.SPAM_COOLDOWN })

        val cleared = pipeline.clearQueue()
        assertEquals(4, cleared)
        assertEquals(0, pipeline.pendingCount)
    }

    @Test
    fun onNotification_dedupWindowIs30SecondsAndSuppressesIdenticalContent() {
        val clock = TestClock(now = 10_000L)
        val pipeline = AnnouncementPipeline(
            queue = AnnouncementQueue(clock = Clock { clock.now }),
            clock = { clock.now },
        )

        assertEquals(30_000L, pipeline.dedupWindowMillis)

        val s1 = snapshot(key = "k1", sender = "Rahul", messageText = "Hello")
        val s2 = snapshot(key = "k2", sender = "Rahul", messageText = "Hello")

        val p1 = pipeline.onNotification(s1, enabled, state())
        assertTrue(p1.decision is EligibilityDecision.Announce)

        // Arrives 25 seconds later (within 30s window) -> suppressed as duplicate
        clock.now += 25_000L
        val p2 = pipeline.onNotification(s2, enabled, state())
        assertTrue(p2.decision is EligibilityDecision.Skip)
        assertEquals(SkipReason.DUPLICATE_UPDATE, (p2.decision as EligibilityDecision.Skip).reason)

        // Arrives 35 seconds later (outside 30s window) -> allowed to announce
        clock.now += 35_000L
        val s3 = snapshot(key = "k3", sender = "Rahul", messageText = "Hello")
        val p3 = pipeline.onNotification(s3, enabled, state())
        assertTrue(p3.decision is EligibilityDecision.Announce)
    }

    @Test
    fun seedSignatures_suppressesDuplicateFromReconnectOrRestart() {
        val clock = TestClock(now = 50_000L)
        val pipeline = AnnouncementPipeline(
            queue = AnnouncementQueue(clock = Clock { clock.now }),
            clock = { clock.now },
        )

        // Seed with a notification that was announced 10 seconds ago
        val signature = AnnouncementPipeline.ContentSignature("com.whatsapp", "Rahul", "Hello from shade")
        pipeline.seedSignatures(mapOf(signature to 40_000L))

        // Reconnect delivers the shade notification
        val s = snapshot(packageName = "com.whatsapp", key = "shade1", sender = "Rahul", messageText = "Hello from shade")
        val processed = pipeline.onNotification(s, enabled, state())

        assertTrue(processed.decision is EligibilityDecision.Skip)
        assertEquals(SkipReason.DUPLICATE_UPDATE, (processed.decision as EligibilityDecision.Skip).reason)
    }
}
