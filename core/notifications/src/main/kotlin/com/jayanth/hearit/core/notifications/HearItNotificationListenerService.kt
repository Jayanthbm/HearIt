package com.jayanth.hearit.core.notifications

import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * The single notification source (ADR-003): receives every posted/updated/removed
 * notification, extracts a [com.jayanth.hearit.core.common.model.NotificationSnapshot]
 * and hands it to a [NotificationSink].
 *
 * Contains **no** speech, rules, queue or history logic (P07 acceptance criterion); the
 * pipeline is wired in P08 by assigning [sink]. Callbacks arrive on the main thread and the
 * extraction is cheap (architecture §5), so work is forwarded synchronously and the sink is
 * responsible for hopping to its own coroutine scope.
 *
 * Privacy: nothing here logs notification content — in fact nothing is logged at all.
 *
 * Binding lifecycle:
 *  - [onListenerConnected] cancels a pending rebind;
 *  - [onListenerDisconnected] (notification access revoked, or the system unbound us)
 *    schedules **one** rebind attempt after [REBIND_DELAY_MILLIS]. The rebind call throws
 *    once access is truly revoked, which is caught and ignored: the setup flow (P11)
 *    surfaces the missing permission to the user. No retry storm, no background loop.
 */
class HearItNotificationListenerService : NotificationListenerService() {

    private val extractor = NotificationExtractor()
    private val handler = Handler(Looper.getMainLooper())
    private val rebindAttempt = Runnable { attemptRebind() }

    /**
     * Optional downstream consumer. Null until P08 wires the pipeline: extraction still
     * runs (and update detection still advances) so behaviour is identical before/after
     * wiring, but snapshots are dropped.
     */
    var sink: NotificationSink? = null

    override fun onListenerConnected() {
        super.onListenerConnected()
        handler.removeCallbacks(rebindAttempt)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        handler.removeCallbacks(rebindAttempt)
        handler.postDelayed(rebindAttempt, REBIND_DELAY_MILLIS)
    }

    override fun onDestroy() {
        handler.removeCallbacks(rebindAttempt)
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        // Our own notifications (the P08 persistent notification) are never candidates.
        if (sbn.packageName == packageName) return

        val raw = StatusBarNotificationMapper.toRaw(sbn, appLabelFor(sbn.packageName))
        sink?.onPosted(extractor.extract(raw, System.currentTimeMillis()))
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn == null) return
        if (sbn.packageName == packageName) return

        val key = NotificationExtractor.keyOf(sbn.packageName, sbn.tag, sbn.id)
        extractor.forget(key)
        sink?.onRemoved(key)
    }

    /** One rebind attempt; harmless no-op when access has been revoked. */
    private fun attemptRebind() {
        try {
            requestRebind(ComponentName(this, HearItNotificationListenerService::class.java))
        } catch (_: SecurityException) {
            // Access genuinely revoked: nothing to rebind. The setup flow reports it.
        }
    }

    /** App label for [packageName], or null when the app vanished (extraction falls back). */
    private fun appLabelFor(packageName: String): String? =
        try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }

    private companion object {
        /** Single delayed retry after a disconnect: enough for transient unbinds, no storm. */
        const val REBIND_DELAY_MILLIS = 5_000L
    }
}
