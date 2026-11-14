package com.jayanth.hearit.core.queue

import com.jayanth.hearit.core.common.model.AppPriority
import com.jayanth.hearit.core.common.model.AppQueueOverride
import com.jayanth.hearit.core.common.model.NotificationSnapshot
import com.jayanth.hearit.core.common.model.OverflowPolicy
import com.jayanth.hearit.core.common.model.SkipReason
import com.jayanth.hearit.core.common.model.QueueSettings
import com.jayanth.hearit.core.common.time.Clock

/** One announcement ready to be spoken: a same-app group selected by [AnnouncementQueue.pollReady]. */
data class ReadyAnnouncement(

    /** Opaque id the caller passes back to `markStarted`/`markCompleted`; stable across repeated polls of the same selection. */
    val id: Long,

    /** Package every item in [items] belongs to (grouping is same-app only). */
    val packageName: String,

    /** Display name announced by the template (from the first item). */
    val appName: String,

    /** Highest priority among [items]; the selection itself is highest-first. */
    val priority: AppPriority,

    /** Grouped notifications in arrival order. */
    val items: List<NotificationSnapshot>,
) {
    init {
        require(items.isNotEmpty()) { "ReadyAnnouncement.items must not be empty" }
        require(items.all { it.packageName == packageName }) {
            "ReadyAnnouncement items must all belong to $packageName"
        }
    }
}

/** Result of [AnnouncementQueue.pollReady]: sweep events plus, when due, the next announcement. */
data class PollResult(
    val events: List<QueueEvent>,
    val announcement: ReadyAnnouncement?,
)

/** Cross-package or single-package batch selected for speaking when Multi-App Digest may run. */
data class ReadyBatch(
    val id: Long,
    val primaryPackageName: String,
    val appName: String,
    val priority: AppPriority,
    val items: List<NotificationSnapshot>,
    val isMultiApp: Boolean,
)

data class BatchPollResult(
    val events: List<QueueEvent>,
    val batch: ReadyBatch?,
)

/** One item released from the call-hold list for full-pipeline re-evaluation (ADR-006/ADR-004). */
data class BlockedNotification(
    val snapshot: NotificationSnapshot,
    val priority: AppPriority,
)

/** Result of [AnnouncementQueue.releaseBlocked]: sweep events plus the released items in hold order. */
data class ReleaseResult(
    val events: List<QueueEvent>,
    val blocked: List<BlockedNotification>,
)

/**
 * One queue instance per Android process; drive it from a single coroutine scope (architecture §3).
 *
 * Device-silent gate (ADR-017): a phone in ringer mode Silent must not be spoken to.
 * [canAnnounce] reports false for the whole queue while [DeviceAudioState.isSilent] says
 * the device is silent, so every poll and readiness report is suppressed until silence
 * ends. Vibrate-only is *not* silent — the standard notification sound still plays — and is
 * not a suppression trigger. Silence overrides everything: it cannot be overridden by
 * Priority Announce, per-app settings or app-state rules (ADR-017), and speech stops
 * immediately if the mode becomes silent while a previous announcement was speaking
 * (ADR-011). The Android layer supplies [DeviceAudioState], so this module stays pure
 * Kotlin with no Android dependency.
 *
 * Responsibilities:
 *  - device silence (ADR-017): [canAnnounce] reports whether the queue is
 *    announceable at all — Silent suppresses everything until the mode leaves Silent;
 *  - priority ordering: highest [AppPriority.rank] first, equal priority FIFO by arrival;
 *  - grouping: same-app items within the resolved grouping window form one announcement,
 *    capped at `maxMessagesPerAnnouncement` — leftovers wait for the next one;
 *  - scheduling: per-item announcement delay and a minimum gap from the previous
 *    announcement's completion to the next start;
 *  - never interrupting: while an announcement is being spoken, [pollReady] returns none;
 *  - dedup: an update of an already-announced key is not repeated unless configured
 *    (`repeatNotificationUpdates`), and an update of a pending key replaces it in place;
 *  - size limits (items, not groups) with the configured overflow policy, and expiry for
 *    pending and call-held items;
 *  - call-hold storage for `EligibilityDecision.Queue(reason)` items: held items are never
 *    polled here, only handed back by [releaseBlocked] for full-pipeline re-evaluation.
 *
 * All time comes from [clock]; expiry ages from queue entry (held items from hold time,
 * restarting on release — ADR-019). Every removal produces a [QueueEvent] carrying history
 * status and reason so history never loses discarded or expired items (§8/§12).
 */
class AnnouncementQueue(
    private val clock: Clock,
    settings: QueueSettings = QueueSettings(),
    appOverrides: Map<String, AppQueueOverride> = emptyMap(),
    private val deviceAudio: DeviceAudioState = DeviceAudioState { false },
) {
    private var settings: QueueSettings = settings
    private var appOverrides: Map<String, AppQueueOverride> = appOverrides

    private data class PendingItem(
        val snapshot: NotificationSnapshot,
        val priority: AppPriority,
        val seq: Long,
        val enqueuedAt: Long,
    )

    private data class HeldItem(
        val snapshot: NotificationSnapshot,
        val priority: AppPriority,
        val reason: SkipReason,
        val heldAt: Long,
    )

    private val pending = mutableListOf<PendingItem>()
    private val held = mutableListOf<HeldItem>()

    /** Device audit (ADR-017): a Silent phone never gets spoken to. */
    /** Device audit (ADR-017): a Silent phone never gets spoken to. */
    val canAnnounce: Boolean get() = !deviceAudio.isSilent()

    /** Announced-key memory for dedup; entries are forgotten after the expiry window (ADR-019). */
    private val announcedAt = mutableMapOf<String, Long>()

    private var nextSeq = 0L
    private var nextAnnouncementId = 0L

    /** Latest poll selection, kept so repeated polls of the same group reuse one id. */
    private var lastSelection: Pair<Long, List<PendingItem>>? = null

    private var speakingId: Long? = null
    private var lastCompletedAtMillis: Long? = null

    /** Number of items waiting to be announced (call-held items are not counted here). */
    val pendingCount: Int
        get() = pending.size

    /** Checks whether an item with [key] is currently waiting in the pending queue. */
    fun hasPending(key: String): Boolean = pending.any { it.snapshot.key == key }

    /** Seeds recently announced notification keys with their announcement timestamps (e.g. from history DB). */
    fun seedAnnounced(keys: Map<String, Long>) {
        val now = clock.nowMillis()
        keys.forEach { (key, timestamp) ->
            if (now - timestamp < settings.expiryMillis) {
                announcedAt[key] = timestamp
            }
        }
    }

    /** Number of items parked by an active call. */
    val heldCount: Int
        get() = held.size

    /**
     * Applies new settings and per-app overrides (the service reloads them when the user
     * saves), then sweeps expiry and re-trims against any newly shrunk size limits.
     * Returns the resulting events.
     */
    fun updateSettings(
        settings: QueueSettings,
        appOverrides: Map<String, AppQueueOverride> = emptyMap(),
    ): List<QueueEvent> {
        this.settings = settings
        this.appOverrides = appOverrides
        val events = sweepExpired().toMutableList()
        events += enforceLimits()
        return events
    }

    /**
     * Offers one notification to the queue. Returns the events produced: expiry sweeps,
     * then exactly one of [QueueEvent.Deduplicated] (update of a recently announced key
     * while repeats are off), [QueueEvent.Replaced] (same key already pending — content
     * updated in place, seq and queue-entry time kept), or [QueueEvent.Queued] followed by
     * any overflow discards the new arrival triggered.
     */
    fun enqueue(snapshot: NotificationSnapshot, priority: AppPriority): List<QueueEvent> {
        val events = sweepExpired().toMutableList()
        val now = clock.nowMillis()

        if (snapshot.isUpdate && !settings.repeatNotificationUpdates) {
            val lastAnnounced = announcedAt[snapshot.key]
            if (lastAnnounced != null && now - lastAnnounced < settings.expiryMillis) {
                events += QueueEvent.Deduplicated(snapshot)
                return events
            }
        }

        val existingIndex = pending.indexOfFirst { it.snapshot.key == snapshot.key }
        if (existingIndex >= 0) {
            val existing = pending[existingIndex]
            pending[existingIndex] = existing.copy(snapshot = snapshot, priority = priority)
            events += QueueEvent.Replaced(snapshot)
            return events
        }

        pending += PendingItem(snapshot, priority, nextSeq++, now)
        events += QueueEvent.Queued(snapshot)
        events += enforceLimits()
        return events
    }

    /**
     * Parks an item because a phone call made it ineligible (`EligibilityDecision.Queue`).
     * Held items are excluded from polling and from size limits (they exist precisely to
     * be announced later — ADR-019) but do age out under the expiry window. A same-key
     * repeat hold replaces the stored content in place. Returns the resulting events.
     */
    fun hold(snapshot: NotificationSnapshot, priority: AppPriority, reason: SkipReason): List<QueueEvent> {
        val events = sweepExpired().toMutableList()
        val existingIndex = held.indexOfFirst { it.snapshot.key == snapshot.key }
        if (existingIndex >= 0) {
            held[existingIndex] = held[existingIndex].copy(snapshot = snapshot, priority = priority)
            events += QueueEvent.Replaced(snapshot)
            return events
        }
        held += HeldItem(snapshot, priority, reason, clock.nowMillis())
        events += QueueEvent.Queued(snapshot, reason)
        return events
    }

    /**
     * Hands every call-held item back to the caller in hold order so P08 can re-run the
     * full eligibility pipeline (ADR-006: the queue stores, never re-evaluates). Items that
     * expired while held are swept out instead and reported as events. Held items restart
     * their wait clock only if the caller re-enqueues them (ADR-019).
     */
    fun releaseBlocked(): ReleaseResult {
        val events = sweepExpired()
        val blocked = held.sortedBy { it.heldAt }.map { BlockedNotification(it.snapshot, it.priority) }
        held.clear()
        return ReleaseResult(events, blocked)
    }

    /**
     * Returns the next announcement when one is due, without removing anything — the
     * caller must confirm with [markStarted]. Selection: highest priority first, equal
     * priority FIFO, then same-package companions within the resolved grouping window
     * (from the first member's queue entry, inclusive) capped at
     * `maxMessagesPerAnnouncement`, in arrival order. Returns no announcement while
     * speaking (never interrupt), while inside the minimum gap after the previous
     * completion, or when nothing is ready yet.
     *
     * Expiry and other sweep events come back in [PollResult.events] on every call. The
     * returned announcement id stays stable while the selected group is unchanged, so the
     * latest poll's id must be started before polling again after any change.
     */
    fun pollReady(): PollResult {
        val events = sweepExpired()
        if (speakingId != null) return PollResult(events, null)
        if (!canAnnounce) return PollResult(events, null)  // device is Silent (ADR-017)

        val now = clock.nowMillis()
        val lastCompletedAt = lastCompletedAtMillis
        if (lastCompletedAt != null && now - lastCompletedAt < settings.minGapBetweenAnnouncementsMillis) {
            return PollResult(events, null)
        }

        val ready = pending.filter { now >= it.enqueuedAt + settings.delayBeforeAnnouncementMillis }
        val first = ready.minWithOrNull(
            compareByDescending<PendingItem> { it.priority.rank }.thenBy { it.seq },
        ) ?: return PollResult(events, null)

        val appSettings = settings.resolveWith(appOverrides[first.snapshot.packageName])
        val windowEnd = first.enqueuedAt + appSettings.groupingWindowMillis
        val companions = ready.filter {
            it.snapshot.packageName == first.snapshot.packageName &&
                it.seq != first.seq &&
                it.enqueuedAt <= windowEnd
        }.sortedBy { it.seq }.take(appSettings.maxMessagesPerAnnouncement - 1)
        val group = (listOf(first) + companions).sortedBy { it.seq }
        if (!canAnnounce) return PollResult(events, null)  // device went silent after selection

        val previous = lastSelection
        val id = if (previous != null && previous.second == group) previous.first else nextAnnouncementId++
        lastSelection = id to group

        return PollResult(
            events,
            ReadyAnnouncement(
                id = id,
                packageName = first.snapshot.packageName,
                appName = first.snapshot.appName,
                priority = first.priority,
                items = group.map { it.snapshot },
            ),
        )
    }

    /** Snapshots & entries of the group currently being spoken. */
    private var speakingItems: List<NotificationSnapshot> = emptyList()
    private var speakingEntries: List<PendingItem> = emptyList()

    /**
     * Confirms the latest [pollReady] selection as the one being spoken: removes the group
     * and records it as the speaking group so a later silent mode is caught (ADR-011).
     *
     * Doors on the selection id: a stale id (from an older poll, or from a mode change that
     * flipped [canAnnounce] off then on) is rejected here, and a second
     * [markStarted] without completion is rejected too.
     * from the pending queue and records its keys as announced (the dedup memory). Returns
     * the resulting sweep events. Rejects a stale id from an older poll and a second start
     * while another announcement is speaking.
     */
    fun markStarted(id: Long): List<QueueEvent> {
        val selection = lastSelection
        require(selection != null && selection.first == id) {
            "Announcement $id is not the latest pollReady() selection; poll again before starting"
        }
        require(speakingId == null) { "Another announcement is already being spoken" }

        val events = sweepExpired()
        speakingId = id
        speakingEntries = selection.second
        speakingItems = selection.second.map { it.snapshot }
        val now = clock.nowMillis()
        selection.second.forEach { item ->
            pending.remove(item)
            announcedAt[item.snapshot.key] = now
        }
        lastSelection = null
        return events
    }

    /**
     * Defers the currently speaking announcement back into [pending] so higher-priority
     * navigation audio or alerts can speak immediately without recording `INTERRUPTED` or
     * enforcing the inter-announcement cooldown gap.
     */
    fun requeueSpeaking(id: Long): Int {
        if (speakingId != id || speakingEntries.isEmpty()) return 0
        val entries = speakingEntries
        speakingId = null
        speakingEntries = emptyList()
        speakingItems = emptyList()
        entries.forEach { entry ->
            announcedAt.remove(entry.snapshot.key)
            if (pending.none { it.snapshot.key == entry.snapshot.key }) {
                pending += entry
            }
        }
        return entries.size
    }

    /**
     * Reports the spoken announcement finished: clears the speaking state and starts the
     * minimum-gap clock for the next selection. Returns the resulting sweep events.
     * Rejects an id that is not the one currently speaking.
     */
    fun markCompleted(id: Long): List<QueueEvent> {
        require(speakingId == id) { "Announcement $id is not currently being spoken" }
        speakingId = null
        lastCompletedAtMillis = clock.nowMillis()
        // The spoken group is reported as ANNOUNCED so history can record it; the sweep
        // events that follow keep reporting expiries that happened meanwhile.
        val announced = speakingItems.map { QueueEvent.Announced(it) }
        speakingEntries = emptyList()
        speakingItems = emptyList()
        return announced + sweepExpired()
    }

    /**
     * Removes every pending and held item that has waited at least `expiryMillis` and
     * reports each with [QueueEvent.Expired]; also forgets announced-key dedup memory past
     * the same window and invalidates a selection whose items expired. Runs at the start of
     * every operation so callers only see current items (ADR-019: `>=` at the boundary).
     */
    private fun sweepExpired(): List<QueueEvent> {
        val now = clock.nowMillis()
        val events = mutableListOf<QueueEvent>()

        val expiredPending = pending.filter { now - it.enqueuedAt >= settings.expiryMillis }
        if (expiredPending.isNotEmpty()) {
            pending.removeAll(expiredPending.toSet())
            expiredPending.forEach { events += QueueEvent.Expired(it.snapshot) }
            val selection = lastSelection
            if (selection != null && selection.second.any { it in expiredPending }) {
                lastSelection = null
            }
        }

        val expiredHeld = held.filter { now - it.heldAt >= settings.expiryMillis }
        if (expiredHeld.isNotEmpty()) {
            held.removeAll(expiredHeld.toSet())
            expiredHeld.forEach { events += QueueEvent.Expired(it.snapshot) }
        }

        announcedAt.entries.removeAll { now - it.value >= settings.expiryMillis }
        return events
    }

    /** Applies both size limits under the configured overflow policy, evicting until each fits. */
    private fun enforceLimits(): List<QueueEvent> {
        val events = mutableListOf<QueueEvent>()

        pending.map { it.snapshot.packageName }.distinct().forEach { packageName ->
            while (pending.count { it.snapshot.packageName == packageName } > settings.perAppMaxSize) {
                val victim = pickVictim(
                    pending.filter { it.snapshot.packageName == packageName },
                    settings.overflowPolicy,
                )
                pending.remove(victim)
                events += QueueEvent.OverflowDiscarded(victim.snapshot)
            }
        }

        while (pending.size > settings.globalMaxSize) {
            val victim = pickVictim(pending, settings.overflowPolicy)
            pending.remove(victim)
            events += QueueEvent.OverflowDiscarded(victim.snapshot)
        }

        return events
    }

    /**
     * Chooses the item a limit evicts (ADR-019): oldest or newest by arrival, or for
     * [OverflowPolicy.KEEP_HIGHEST_PRIORITY] the lowest priority with the newest evicted
     * first among equals so items that already waited are protected. Callers guarantee a
     * non-empty list (limits are always at least 1).
     */
    /** Distinct display app names currently waiting in the pending queue, in arrival order. */
    fun pendingAppNames(): List<String> =
        pending.mapNotNull { it.snapshot.appName.trim().takeIf(String::isNotEmpty) }.distinct()

    /** Manually flushes every pending and call-held item from the queue (#8). */
    fun clearPending(): List<QueueEvent> {
        val clearedEvents = mutableListOf<QueueEvent>()
        pending.forEach { clearedEvents += QueueEvent.ManuallyCleared(it.snapshot) }
        held.forEach { clearedEvents += QueueEvent.ManuallyCleared(it.snapshot) }
        pending.clear()
        held.clear()
        lastSelection = null
        return clearedEvents
    }

    /**
     * Polls either multi-app digest batches arriving within [digestWindowMillis] (#33)
     * or delegates to single-app [pollReady] when [multiAppEnabled] is false.
     */
    fun pollReadyDigest(
        multiAppEnabled: Boolean,
        digestWindowMillis: Long = 2_500L,
    ): BatchPollResult {
        if (!multiAppEnabled) {
            val poll = pollReady()
            val mapped = poll.announcement?.let { ann ->
                ReadyBatch(
                    id = ann.id,
                    primaryPackageName = ann.packageName,
                    appName = ann.appName,
                    priority = ann.priority,
                    items = ann.items,
                    isMultiApp = false,
                )
            }
            return BatchPollResult(poll.events, mapped)
        }

        val events = sweepExpired()
        if (speakingId != null || !canAnnounce) return BatchPollResult(events, null)

        val now = clock.nowMillis()
        val lastCompletedAt = lastCompletedAtMillis
        if (lastCompletedAt != null && now - lastCompletedAt < settings.minGapBetweenAnnouncementsMillis) {
            return BatchPollResult(events, null)
        }

        val ready = pending.filter { now >= it.enqueuedAt + settings.delayBeforeAnnouncementMillis }
        val first = ready.minWithOrNull(
            compareByDescending<PendingItem> { it.priority.rank }.thenBy { it.seq },
        ) ?: return BatchPollResult(events, null)

        // Critical announcements (e.g. active navigation turn alerts) bypass the multi-app digest
        // hold delay and announce immediately without being bundled with normal notifications.
        if (first.priority == AppPriority.CRITICAL) {
            val poll = pollReady()
            val mapped = poll.announcement?.let { ann ->
                ReadyBatch(
                    id = ann.id,
                    primaryPackageName = ann.packageName,
                    appName = ann.appName,
                    priority = ann.priority,
                    items = ann.items,
                    isMultiApp = false,
                )
            }
            return BatchPollResult(events + poll.events, mapped)
        }

        // Hold briefly inside digest window if fewer than maxMessages have arrived yet
        val digestEnd = first.enqueuedAt + digestWindowMillis.coerceAtLeast(0L)
        if (now < digestEnd && ready.size < settings.maxMessagesPerAnnouncement) {
            return BatchPollResult(events, null)
        }

        val group = ready
            .filter { it.enqueuedAt <= digestEnd }
            .sortedWith(compareByDescending<PendingItem> { it.priority.rank }.thenBy { it.seq })
            .take(settings.maxMessagesPerAnnouncement)
        if (group.isEmpty() || !canAnnounce) return BatchPollResult(events, null)

        val previous = lastSelection
        val id = if (previous != null && previous.second == group) previous.first else nextAnnouncementId++
        lastSelection = id to group

        val distinctPackages = group.map { it.snapshot.packageName }.distinct().size
        return BatchPollResult(
            events = events,
            batch = ReadyBatch(
                id = id,
                primaryPackageName = first.snapshot.packageName,
                appName = first.snapshot.appName,
                priority = first.priority,
                items = group.map { it.snapshot },
                isMultiApp = distinctPackages > 1,
            ),
        )
    }

    private fun pickVictim(items: List<PendingItem>, policy: OverflowPolicy): PendingItem =
        when (policy) {
            OverflowPolicy.DISCARD_OLDEST -> items.minBy { it.seq }
            OverflowPolicy.DISCARD_NEWEST -> items.maxBy { it.seq }
            OverflowPolicy.KEEP_HIGHEST_PRIORITY -> items.minWith(
                compareBy<PendingItem> { it.priority.rank }.thenByDescending { it.seq },
            )
        }

}
