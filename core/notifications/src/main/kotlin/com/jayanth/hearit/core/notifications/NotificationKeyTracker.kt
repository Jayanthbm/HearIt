package com.jayanth.hearit.core.notifications

/**
 * Bounded memory of notification keys already seen, for update-vs-new detection
 * (master prompt §8/§10 dedup depends on it; §12 "update existing vs new entry").
 *
 * Pure Kotlin/JVM-testable (ADR-002: identity/update detection lives in
 * `:core/notifications`). Semantics:
 *  - the **first** [markSeen] of a key returns `false` (a new post);
 *  - any later [markSeen] of the same key returns `true` (an update of that notification);
 *  - [forget] (called when the platform reports removal) makes the next post new again;
 *  - memory is bounded at [maxEntries]: the least recently marked key is evicted first,
 *    trading accuracy for a hard memory ceiling (a re-post of an evicted key is reported
 *    as new). Re-marking a key refreshes its recency.
 *
 * Not thread-safe: the listener drives it from its main thread only (architecture §5).
 */
class NotificationKeyTracker(private val maxEntries: Int = DEFAULT_MAX_ENTRIES) {

    init {
        require(maxEntries > 0) { "NotificationKeyTracker.maxEntries must be positive" }
    }

    private val seen = LinkedHashSet<String>()

    /** Number of remembered keys (never more than [maxEntries]). */
    val size: Int get() = seen.size

    /**
     * Records [key] as seen.
     *
     * @return `true` when the key was already remembered (this post is an update),
     *   `false` for a genuinely new notification.
     */
    fun markSeen(key: String): Boolean {
        val wasSeen = seen.remove(key)
        seen.add(key) // re-insert so eviction is least-recently-marked first
        if (seen.size > maxEntries) {
            seen.remove(seen.first())
        }
        return wasSeen
    }

    /** Forgets [key], e.g. after the platform reports the notification removed. */
    fun forget(key: String) {
        seen.remove(key)
    }

    /** Drops every remembered key (listener connect/reset). */
    fun clear() {
        seen.clear()
    }

    companion object {
        /** Generous ceiling: covers far more concurrent notifications than Android keeps. */
        const val DEFAULT_MAX_ENTRIES: Int = 500
    }
}
