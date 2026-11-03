package com.jayanth.hearit.service

import com.jayanth.hearit.core.common.model.AppPriority
import com.jayanth.hearit.core.common.model.NotificationSnapshot
import com.jayanth.hearit.core.common.model.ResolvedSettings
import com.jayanth.hearit.core.common.model.RuntimeState
import com.jayanth.hearit.core.queue.AnnouncementQueue
import com.jayanth.hearit.core.queue.QueueEvent
import com.jayanth.hearit.core.queue.ReadyAnnouncement
import com.jayanth.hearit.core.rules.EligibilityDecision
import com.jayanth.hearit.core.rules.EligibilityRules
import com.jayanth.hearit.core.tts.AnnouncementFormat
import com.jayanth.hearit.core.tts.AnnouncementTemplate
import com.jayanth.hearit.core.tts.BuiltAnnouncement

/**
 * The speech pipeline between collection and the TTS engine (architecture §3, P08).
 *
 * Pure Kotlin with no Android types, so every decision is JVM-unit-testable: the service
 * supplies the clock-backed [AnnouncementQueue], the settings snapshot and the device
 * state; the engine is called by the service, not from here.
 *
 * Flow:
 *  1. [onNotification] runs the eligibility rules once per snapshot and routes it —
 *     `Announce` enqueues, `Queue(reason)` holds for full re-evaluation, `Skip` only returns
 *     the decision (history records it from P10 on).
 *  2. [tick] polls the queue, builds the announcement and returns it for speaking. Groups
 *     whose every section is empty are retired silently instead of being spoken.
 *  3. [onSpeechFinished] marks the group completed so the minimum gap starts.
 *  4. [onStateChanged] re-evaluates the **speaking** group against fresh device state and
 *     returns `StopSpeech` the moment eligibility is lost or the device becomes silent
 *     (ADR-011, ADR-017).
 *  5. [releaseHeld] re-runs call-held items through the full pipeline once a call ends
 *     (ADR-004/ADR-006 — nothing is ever announced on the strength of its old decision).
 *
 * Not thread-safe: driven from the service's single main-thread loop (architecture §5).
 */
class AnnouncementPipeline(
    private val queue: AnnouncementQueue,
    private val rules: EligibilityRules = EligibilityRules(),
    private val format: AnnouncementFormat = AnnouncementFormat(),
) {

    /** What the service should do next; the service owns the engine. */
    sealed interface Action {

        /** Speak [announcement] now; [selection] identifies the queue group for completion. */
        data class Speak(
            val selection: ReadyAnnouncement,
            val announcement: BuiltAnnouncement,
        ) : Action

        /** Stop the in-progress announcement immediately. */
        data class StopSpeech(val reason: StopReason) : Action
    }

    /** Why speech must stop (recorded in history by P10; never contains content). */
    enum class StopReason {
        /** The re-evaluated rules no longer return `Announce` (screen/lock/mute/call/DND/output). */
        ELIGIBILITY_LOST,

        /** The device became silent (ADR-017). */
        DEVICE_SILENT,

        /** The active audio output disappeared (unplug/disconnect, P08 scope). */
        OUTPUT_LOST,
    }

    /** One snapshot's journey through the rules, plus the queue events worth recording. */
    data class Processed(
        val decision: EligibilityDecision,
        val queueEvents: List<QueueEvent>,
    )

    private data class ActiveSpeech(
        val selection: ReadyAnnouncement,
        val settings: ResolvedSettings,
    )

    private var active: ActiveSpeech? = null

    /** True while a group is being spoken (the service owns the actual engine). */
    val isSpeaking: Boolean get() = active != null

    /** Items waiting to be spoken; the dashboard shows this as queue status (P12). */
    val pendingCount: Int get() = queue.pendingCount

    /**
     * Routes one snapshot through the rules and into the queue or the call-hold list.
     *
     * @param priority per-app priority; defaults to [AppPriority.NORMAL] (ADR-019) until
     *   per-app priority configuration lands with the Applications feature (P13).
     */
    fun onNotification(
        snapshot: NotificationSnapshot,
        settings: ResolvedSettings,
        state: RuntimeState,
        priority: AppPriority = AppPriority.NORMAL,
    ): Processed {
        val decision = rules.evaluate(snapshot, settings, state)
        val events = when (decision) {
            EligibilityDecision.Announce -> queue.enqueue(snapshot, priority)
            is EligibilityDecision.Queue -> queue.hold(snapshot, priority, decision.reason)
            is EligibilityDecision.Skip -> emptyList()
        }
        return Processed(decision, events)
    }

    /**
     * Releases call-held items after a call ends and re-evaluates each through the full
     * pipeline with fresh state — never on the strength of its held decision
     * (ADR-004/ADR-006).
     */
    fun releaseHeld(
        state: RuntimeState,
        settingsOf: (String) -> ResolvedSettings,
    ): List<Processed> {
        val released = queue.releaseBlocked()
        return released.blocked.map { blocked ->
            onNotification(
                snapshot = blocked.snapshot,
                settings = settingsOf(blocked.snapshot.packageName),
                state = state,
                priority = blocked.priority,
            )
        }
    }

    /**
     * Returns the next announcement to speak, or null when nothing is due, a group is
     * already speaking, or the device is silent.
     *
     * The selected group is marked started here (so a poll can never hand the same group
     * twice); the service must call [onSpeechFinished] exactly once per returned action —
     * including when the engine rejects it.
     */
    fun tick(
        state: RuntimeState,
        announcementFormat: AnnouncementFormat = format,
        settingsOf: (String) -> ResolvedSettings,
    ): Action.Speak? {
        if (active != null) return null

        repeat(MAX_RETIRE_STEPS) {
            val selection = queue.pollReady().announcement ?: return null
            val built = AnnouncementTemplate.build(selection.items, announcementFormat)
            queue.markStarted(selection.id)
            if (built.isEmpty) {
                // Nothing speakable (every section disabled/empty): retire without speaking.
                queue.markCompleted(selection.id)
                return@repeat
            }
            active = ActiveSpeech(selection, settingsOf(selection.packageName))
            return Action.Speak(selection, built)
        }
        return null
    }

    /** The engine finished, failed or rejected the announcement: start the minimum gap. */
    fun onSpeechFinished(): List<QueueEvent> {
        val finished = active ?: return emptyList()
        active = null
        return queue.markCompleted(finished.selection.id)
    }

    /**
     * Re-evaluates the speaking group against fresh state (called on every service tick
     * and on audio-device changes). Returns at most one [Action.StopSpeech]; an empty list
     * means speech may continue.
     *
     * State-dependent rules are identical for every item of a group (screen/lock, mute,
     * call, DND, silence and output), so the first item is representative.
     */
    fun onStateChanged(
        state: RuntimeState,
        settingsOf: (String) -> ResolvedSettings,
    ): List<Action> {
        val speaking = active ?: return emptyList()
        if (!queue.canAnnounce) {
            return listOf(Action.StopSpeech(StopReason.DEVICE_SILENT))
        }
        val decision = rules.evaluate(
            snapshot = speaking.selection.items.first(),
            settings = settingsOf(speaking.selection.packageName),
            state = state,
        )
        return if (decision == EligibilityDecision.Announce) {
            emptyList()
        } else {
            listOf(Action.StopSpeech(StopReason.ELIGIBILITY_LOST))
        }
    }

    private companion object {
        /** Guards against an unbounded retire loop when every pending group is empty. */
        const val MAX_RETIRE_STEPS = 32
    }
}
