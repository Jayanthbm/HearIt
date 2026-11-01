package com.jayanth.hearit.core.common.model

/**
 * What to evict when a queue size limit is exceeded (master prompt §8, ADR-005/ADR-019).
 */
enum class OverflowPolicy {

    /** Evict the item that has waited longest. Default (master-suggested). */
    DISCARD_OLDEST,

    /** Evict the most recently enqueued item — a new arrival can be refused. */
    DISCARD_NEWEST,

    /**
     * Evict the lowest-priority item; among equal priorities the newest is evicted first,
     * protecting items that have already waited (ADR-019).
     */
    KEEP_HIGHEST_PRIORITY,
}

/**
 * Queue, grouping and scheduling settings (master prompt §8): a pure model with no
 * Android types, mirroring [EligibilitySettings] — global defaults here, per-app grouping
 * window via [AppQueueOverride], resolution by [resolveWith].
 *
 * Defaults are the master-suggested values where §8 gives them (global 50, per-app 10,
 * expiry 30 minutes, discard oldest) and short neutral values elsewhere; all are
 * product-confirmable settings-surface values (ADR-019).
 */
data class QueueSettings(
    /** Same-app grouping window; companions are captured from the first member's queue entry. */
    val groupingWindowMillis: Long = 5_000L,
    /** Per-item wait before an item becomes speakable. */
    val delayBeforeAnnouncementMillis: Long = 0L,
    /** Hard cap on notifications inside one announcement. */
    val maxMessagesPerAnnouncement: Int = 3,
    /** Minimum time from the previous announcement's completion to the next start. */
    val minGapBetweenAnnouncementsMillis: Long = 0L,
    /** Maximum time an item may wait in the queue before it expires. */
    val expiryMillis: Long = 30L * 60_000L,
    /** Global pending-item cap (counts items, not groups). */
    val globalMaxSize: Int = 50,
    /** Per-app pending-item cap (counts items, not groups). */
    val perAppMaxSize: Int = 10,
    /** Which item a size limit evicts (ADR-019). */
    val overflowPolicy: OverflowPolicy = OverflowPolicy.DISCARD_OLDEST,
    /** Master prompt §10: announcements repeat when a notification is updated — default off. */
    val repeatNotificationUpdates: Boolean = false,
) {
    init {
        require(groupingWindowMillis >= 0L) {
            "QueueSettings.groupingWindowMillis must not be negative, was $groupingWindowMillis"
        }
        require(delayBeforeAnnouncementMillis >= 0L) {
            "QueueSettings.delayBeforeAnnouncementMillis must not be negative, was $delayBeforeAnnouncementMillis"
        }
        require(maxMessagesPerAnnouncement >= 1) {
            "QueueSettings.maxMessagesPerAnnouncement must be at least 1, was $maxMessagesPerAnnouncement"
        }
        require(minGapBetweenAnnouncementsMillis >= 0L) {
            "QueueSettings.minGapBetweenAnnouncementsMillis must not be negative, was $minGapBetweenAnnouncementsMillis"
        }
        require(expiryMillis > 0L) {
            "QueueSettings.expiryMillis must be positive, was $expiryMillis"
        }
        require(globalMaxSize >= 1) {
            "QueueSettings.globalMaxSize must be at least 1, was $globalMaxSize"
        }
        require(perAppMaxSize >= 1) {
            "QueueSettings.perAppMaxSize must be at least 1, was $perAppMaxSize"
        }
    }

    /**
     * Merges this app's override onto the global defaults; a null override inherits
     * everything (ADR-016-style whole-group inheritance).
     */
    fun resolveWith(override: AppQueueOverride?): QueueSettings =
        if (override == null) {
            this
        } else {
            copy(groupingWindowMillis = override.groupingWindowMillis ?: groupingWindowMillis)
        }
}

/**
 * Per-app queue override (master prompt §8: "grouping window per app"). A null field
 * inherits the global value.
 *
 * Only the grouping window is per-app configurable: size limits, delay, min gap, expiry,
 * overflow policy and update repetition are global controls (ADR-019).
 */
data class AppQueueOverride(
    val groupingWindowMillis: Long? = null,
) {
    init {
        if (groupingWindowMillis != null) {
            require(groupingWindowMillis >= 0L) {
                "AppQueueOverride.groupingWindowMillis must not be negative, was $groupingWindowMillis"
            }
        }
    }

    /** True when the app inherits the global grouping window (UI "Inherits global"). */
    val inheritsEveryValue: Boolean
        get() = groupingWindowMillis == null
}
