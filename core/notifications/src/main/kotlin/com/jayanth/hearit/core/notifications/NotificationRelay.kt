package com.jayanth.hearit.core.notifications

import com.jayanth.hearit.core.common.model.NotificationSnapshot

/**
 * Process-wide bridge between the system-created listener and the app-created pipeline
 * (P08 wiring for the P07 listener).
 *
 * The listener instance is constructed by the platform, so it cannot receive constructor
 * injection; the foreground service registers itself here when it starts and clears the
 * registration when it stops. Exactly one sink exists per process (ADR-003/ADR-005:
 * one collection path, one pipeline), and every snapshot is forwarded exactly once —
 * eligibility is decided downstream, never here (ADR-007).
 *
 * `@Volatile` because registration happens on the service's main thread while callbacks
 * arrive on the listener's main thread; strict ordering between the two is not required
 * (a snapshot posted during a service restart is simply dropped).
 */
object NotificationRelay : NotificationSink {

    /** The active pipeline, or null when no announcement service is running. */
    @Volatile
    var sink: NotificationSink? = null

    override fun onPosted(snapshot: NotificationSnapshot) {
        sink?.onPosted(snapshot)
    }

    override fun onRemoved(key: String) {
        sink?.onRemoved(key)
    }
}
