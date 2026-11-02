package com.jayanth.hearit.core.notifications

import com.jayanth.hearit.core.common.model.NotificationSnapshot

/**
 * Downstream consumer of extracted notifications (architecture §3, step 1).
 *
 * The listener forwards **every** snapshot here regardless of eligibility: history
 * collection is independent of announcement eligibility (ADR-007). The pipeline that
 * resolves settings, runs the rules and feeds the queue is wired in P08; the listener
 * itself contains no speech or rules logic (P07 acceptance criterion).
 *
 * Callbacks arrive on the listener's main thread; implementations that do real work must
 * hop to their own coroutine scope instead of blocking (architecture §5).
 */
interface NotificationSink {

    /** A notification was posted or updated; [snapshot] is already extracted and deduped. */
    fun onPosted(snapshot: NotificationSnapshot)

    /** A notification was removed by the system or the app; forget it for update detection. */
    fun onRemoved(key: String)
}
