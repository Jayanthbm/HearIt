package com.jayanth.hearit.service

import com.jayanth.hearit.core.common.logger.HearItLogger
import com.jayanth.hearit.core.common.logger.LogCategory
import com.jayanth.hearit.core.common.model.AppPriority
import com.jayanth.hearit.core.common.model.HistoryStatus
import com.jayanth.hearit.core.common.model.NavigationPriorityDetector
import com.jayanth.hearit.core.common.model.NotificationSnapshot
import com.jayanth.hearit.core.common.model.ResolvedSettings
import com.jayanth.hearit.core.common.model.RuntimeState
import com.jayanth.hearit.core.common.model.SkipReason
import com.jayanth.hearit.core.queue.AnnouncementQueue
import com.jayanth.hearit.core.queue.QueueEvent
import com.jayanth.hearit.core.queue.ReadyAnnouncement
import com.jayanth.hearit.core.rules.EligibilityDecision
import com.jayanth.hearit.core.rules.EligibilityRules
import com.jayanth.hearit.core.tts.AnnouncementFormat
import com.jayanth.hearit.core.tts.AnnouncementTemplate
import com.jayanth.hearit.core.tts.BuiltAnnouncement
import com.jayanth.hearit.core.tts.SmartContentCleaner

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
 *     (ADR-011, ADR-017). * 5. [releaseHeld] re-runs call-held items through the full pipeline once a call ends
 *    (ADR-004/ADR-006 — nothing is ever announced on the strength of its old decision).
 * 6. Every outcome is reported to [onRecord] as a [HistoryRecord], including skips, so
 *    history collection stays independent of eligibility (ADR-007).
 *
 * Not thread-safe: driven from the service's single main-thread loop (architecture §5).
 */
class AnnouncementPipeline(
    private val queue: AnnouncementQueue,
    private val rules: EligibilityRules = EligibilityRules(),
    private val format: AnnouncementFormat = AnnouncementFormat(),
    private val clock: () -> Long = { System.currentTimeMillis() },
    /** History sink; called inline, so implementations must not block (see ADR-007). */
    private val onRecord: (HistoryRecord) -> Unit = {},
) {

    private data class ContentSignature(
        val packageName: String,
        val sender: String,
        val messageText: String,
    )

    private val recentSignatures = mutableMapOf<ContentSignature, Long>()
    private val dedupWindowMillis = 20_000L

    private data class SenderCooldownBucket(
        val acceptedTimestamps: MutableList<Long> = mutableListOf(),
        var throttledSinceSummary: Int = 0,
    )

    private val senderCooldowns = mutableMapOf<Pair<String, String>, SenderCooldownBucket>()

    private fun isContentDuplicate(snapshot: NotificationSnapshot, now: Long): Boolean {
        val text = snapshot.messageText?.trim().orEmpty()
        val sender = snapshot.sender?.trim().orEmpty()
        if (text.isBlank() && sender.isBlank()) return false

        recentSignatures.entries.removeAll { now - it.value >= dedupWindowMillis }

        val signature = ContentSignature(snapshot.packageName, sender, text)
        val lastSeen = recentSignatures[signature]
        if (lastSeen != null && now - lastSeen < dedupWindowMillis) {
            return true
        }
        recentSignatures[signature] = now
        return false
    }

    /** What the service should do next; the service owns the engine. */
    sealed interface Action {

        /** Speak [announcement] now; [selection] identifies the queue group for completion. */
        data class Speak(
            val selection: ReadyAnnouncement,
            val announcement: BuiltAnnouncement,
            val spokenItems: List<NotificationSnapshot> = selection.items,
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

    /**
     * One history fact produced by the pipeline (master prompt §12, phase L3).
     *
     * Carries no history **settings**: storage mode, update handling and retention belong to
     * the app that receives it, so the service resolves them per package.
     */
    data class HistoryRecord(
        val snapshot: NotificationSnapshot,
        val status: HistoryStatus,
        val reason: SkipReason?,
        val priority: AppPriority,
    )

    private data class ActiveSpeech(
        val selection: ReadyAnnouncement,
        val settings: ResolvedSettings,
    )

    private var active: ActiveSpeech? = null
    private var lastAnnouncedPackage: String? = null
    private var lastAnnouncedAtMillis: Long = 0L

    /** True while a group is being spoken (the service owns the actual engine). */
    val isSpeaking: Boolean get() = active != null

    /** Items waiting to be spoken; the dashboard shows this as queue status (P12). */
    val pendingCount: Int get() = queue.pendingCount

    /**
     * Items parked because a call was in progress. The service releases them once the call
     * ends (L6); until then they age out under the queue's expiry window.
     */
    val heldCount: Int get() = queue.heldCount

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
        isSpamCooldownEnabled: Boolean = false,
        spamCooldownMaxMessages: Int = 3,
        spamCooldownWindowMs: Long = 60_000L,
    ): Processed {
        val decision = rules.evaluate(snapshot, settings, state)
        HearItLogger.info(
            LogCategory.RULES,
            "Eligibility evaluated for pkg=${snapshot.packageName}: decision=${decision.javaClass.simpleName}${if (decision is EligibilityDecision.Skip) " (reason=${decision.reason})" else ""}",
        )
        val now = clock()
        val isReplacingPending = queue.hasPending(snapshot.key)
        if (!isReplacingPending && decision is EligibilityDecision.Announce && isContentDuplicate(snapshot, now)) {
            HearItLogger.info(
                LogCategory.RULES,
                "Duplicate notification content suppressed within ${dedupWindowMillis / 1000}s for pkg=${snapshot.packageName}",
            )
            val skipDecision = EligibilityDecision.Skip(SkipReason.DUPLICATE_UPDATE)
            onRecord(HistoryRecord(snapshot, HistoryStatus.SKIPPED, SkipReason.DUPLICATE_UPDATE, priority))
            return Processed(skipDecision, emptyList())
        }
        if (decision is EligibilityDecision.Announce &&
            SmartContentCleaner.shouldSuppress(snapshot.messageText, snapshot.packageName)
        ) {
            HearItLogger.info(
                LogCategory.RULES,
                "Notification suppressed by Active Intelligence rules for pkg=${snapshot.packageName}",
            )
            val skipDecision = EligibilityDecision.Skip(SkipReason.SUPPRESSED_INTELLIGENCE)
            onRecord(HistoryRecord(snapshot, HistoryStatus.SKIPPED, SkipReason.SUPPRESSED_INTELLIGENCE, priority))
            return Processed(skipDecision, emptyList())
        }

        // Per-Sender Spam Cooldown Throttle (#28 / Idea 16)
        val isCritical = EligibilityRules.isCriticalBreakthrough(snapshot, settings.eligibility)
        val cleanSenderKey = (SmartContentCleaner.cleanSender(snapshot.sender) ?: snapshot.sender)?.trim().orEmpty()
        if (!isReplacingPending &&
            isSpamCooldownEnabled &&
            !isCritical &&
            decision is EligibilityDecision.Announce &&
            cleanSenderKey.isNotEmpty()
        ) {
            val key = snapshot.packageName to cleanSenderKey.lowercase()
            val bucket = senderCooldowns.getOrPut(key) { SenderCooldownBucket() }
            bucket.acceptedTimestamps.removeAll { now - it >= spamCooldownWindowMs }
            if (bucket.acceptedTimestamps.size >= spamCooldownMaxMessages) {
                bucket.throttledSinceSummary++
                val skipDecision = EligibilityDecision.Skip(SkipReason.SPAM_COOLDOWN)
                onRecord(HistoryRecord(snapshot, HistoryStatus.SKIPPED, SkipReason.SPAM_COOLDOWN, priority))
                if (bucket.throttledSinceSummary % 5 == 0) {
                    val summarySnap = snapshot.copy(
                        key = "${snapshot.packageName}|spam_summary|$cleanSenderKey|$now",
                        sender = null,
                        messageText = "$cleanSenderKey sent ${bucket.throttledSinceSummary} more messages.",
                    )
                    val summaryEvents = queue.enqueue(summarySnap, priority)
                    return Processed(skipDecision, summaryEvents)
                }
                return Processed(skipDecision, emptyList())
            } else {
                bucket.acceptedTimestamps += now
                bucket.throttledSinceSummary = 0
            }
        }
        val events = when (decision) {
            EligibilityDecision.Announce -> queue.enqueue(snapshot, priority)
            is EligibilityDecision.Queue -> queue.hold(snapshot, priority, decision.reason)
            is EligibilityDecision.Skip -> {
                // A skip is a history fact too, and keeps the reason it was skipped for.
                onRecord(HistoryRecord(snapshot, HistoryStatus.SKIPPED, decision.reason, priority))
                emptyList()
            }
        }
        events.forEach { event -> onRecord(event.toHistoryRecord(priority)) }
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

    /** Distinct app names waiting in pending queue. */
    fun pendingAppNames(): List<String> = queue.pendingAppNames()

    /** Manually clears all queued & held announcements, recording clear facts in history (#8). */
    fun clearQueue(): Int {
        val events = queue.clearPending()
        events.forEach { event -> onRecord(event.toHistoryRecord()) }
        return events.size
    }

    /**
     * Returns the next announcement to speak, or null when nothing is due, a group is
     * already speaking, or the device is silent.
     */
    fun tick(
        state: RuntimeState,
        announcementFormat: AnnouncementFormat = format,
        isAppNameDedupEnabled: Boolean = true,
        appNameDedupWindowMs: Long = 45_000L,
        isDelayedTimestampEnabled: Boolean = true,
        delayedTimestampThresholdMs: Long = 120_000L,
        isSenderBurstAggregatorEnabled: Boolean = false,
        senderBurstMinCount: Int = 2,
        senderBurstWindowMs: Long = 5_000L,
        isMultiAppDigestEnabled: Boolean = false,
        multiAppDigestWindowMs: Long = 2_500L,
        settingsOf: (String) -> ResolvedSettings,
    ): Action.Speak? {
        if (active != null) return null

        repeat(MAX_RETIRE_STEPS) {
            val poll = queue.pollReadyDigest(
                multiAppEnabled = isMultiAppDigestEnabled,
                digestWindowMillis = multiAppDigestWindowMs,
            )
            poll.events.forEach { event -> onRecord(event.toHistoryRecord()) }
            val batch = poll.batch ?: return null
            val now = clock()
            val resolvedSettings = settingsOf(batch.primaryPackageName)

            val delayedMinutesByKey = if (isDelayedTimestampEnabled) {
                batch.items.mapNotNull { item ->
                    val delayMillis = now - item.postTimeMillis
                    if (item.postTimeMillis > 0L && delayMillis > delayedTimestampThresholdMs) {
                        val minutes = (delayMillis / 60_000L).coerceAtLeast(1L)
                        item.key to minutes
                    } else {
                        null
                    }
                }.toMap()
            } else {
                emptyMap()
            }

            val isRapidSameApp = !batch.isMultiApp &&
                isAppNameDedupEnabled &&
                lastAnnouncedPackage == batch.primaryPackageName &&
                (now - lastAnnouncedAtMillis < appNameDedupWindowMs)
            lastAnnouncedPackage = if (batch.isMultiApp) null else batch.primaryPackageName
            lastAnnouncedAtMillis = now

            val effectiveFormat = if (isRapidSameApp) {
                announcementFormat.copy(includeAppName = false)
            } else {
                announcementFormat
            }

            val isCritical = batch.items.any { item ->
                EligibilityRules.isCriticalBreakthrough(
                    snapshot = item,
                    eligibility = settingsOf(item.packageName).eligibility,
                )
            }

            val built = if (batch.isMultiApp) {
                AnnouncementTemplate.buildMultiAppDigest(
                    snapshots = batch.items,
                    format = effectiveFormat,
                    isCritical = isCritical,
                    delayedMinutesByKey = delayedMinutesByKey,
                )
            } else {
                AnnouncementTemplate.build(
                    snapshots = batch.items,
                    format = effectiveFormat,
                    isCritical = isCritical,
                    delayedMinutesByKey = delayedMinutesByKey,
                    mergeSenderBursts = isSenderBurstAggregatorEnabled,
                    senderBurstMinCount = senderBurstMinCount,
                    senderBurstWindowMs = senderBurstWindowMs,
                )
            }

            val selection = ReadyAnnouncement(
                id = batch.id,
                packageName = batch.primaryPackageName,
                appName = batch.appName,
                priority = batch.priority,
                items = batch.items.filter { it.packageName == batch.primaryPackageName },
            )
            queue.markStarted(batch.id)
                .forEach { event -> onRecord(event.toHistoryRecord(batch.priority)) }
            if (built.isEmpty) {
                queue.markCompleted(batch.id).forEach { event ->
                    onRecord(event.toHistoryRecord(batch.priority, overrideStatus = HistoryStatus.SKIPPED))
                }
                return@repeat
            }
            active = ActiveSpeech(selection, resolvedSettings)
            return Action.Speak(selection, built, spokenItems = batch.items)
        }
        return null
    }

    /**
     * The utterance ended: start the minimum gap and record the outcome.
     *
     * @param spoken true only when every section was actually spoken. A stop, a skip or an
     *   engine error passes false, so history records `INTERRUPTED` instead of `ANNOUNCED`
     *   for a group the queue considers completed.
     */
    fun onSpeechFinished(spoken: Boolean = true): List<QueueEvent> {
        val finished = active ?: return emptyList()
        active = null
        val events = queue.markCompleted(finished.selection.id)
        val overrideStatus = if (spoken) null else HistoryStatus.INTERRUPTED
        events.forEach { event ->
            onRecord(event.toHistoryRecord(finished.selection.priority, overrideStatus))
        }
        return events
    }

    /**
     * Pauses and defers the active speech back to the pending queue so higher-priority
     * Maps/navigation audio or alerts can speak first, then resumes the deferred notification.
     *
     * Does NOT record an `INTERRUPTED` history row and clears rapid same-app dedup so the
     * deferred notification speaks with its full introduction when resumed.
     */
    fun deferActiveSpeechForNavigation(): Int {
        val deferred = active ?: return 0
        active = null
        lastAnnouncedPackage = null
        return queue.requeueSpeaking(deferred.selection.id)
    }

    /** True when the currently speaking announcement is itself a navigation notification. */
    fun isSpeakingNavigation(): Boolean =
        active?.selection?.items?.any { NavigationPriorityDetector.isNavigationNotification(it) } == true

    /**
     * Maps one queue event to the history fact to store.
     *
     * @param priority priority of the group; queue events carry the snapshot only.
     * @param overrideStatus Replaces the queue's `ANNOUNCED` when nothing was really spoken:
     *   `SKIPPED` for a group retired with every section empty, `INTERRUPTED` for an
     *   utterance that was stopped, skipped or failed.
     */
    private fun QueueEvent.toHistoryRecord(
        priority: AppPriority = AppPriority.NORMAL,
        overrideStatus: HistoryStatus? = null,
    ): HistoryRecord = if (overrideStatus != null && this is QueueEvent.Announced) {
        HistoryRecord(snapshot, overrideStatus, null, priority)
    } else {
        HistoryRecord(snapshot, historyStatus, reason, priority)
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
