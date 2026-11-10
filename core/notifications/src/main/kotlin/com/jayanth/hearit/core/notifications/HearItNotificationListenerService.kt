package com.jayanth.hearit.core.notifications

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.jayanth.hearit.core.common.logger.HearItLogger
import com.jayanth.hearit.core.common.logger.LogCategory

/**
 * The single notification source (ADR-003): receives every posted/updated/removed
 * notification, extracts a [com.jayanth.hearit.core.common.model.NotificationSnapshot]
 * and hands it to a [NotificationSink].
 *
 * Contains **no** speech, rules, queue or history logic (P07 acceptance criterion); the
 * pipeline is wired through [sink] (tests) or the process-wide [NotificationRelay]
 * (production, registered by the P08 announcement service). Callbacks arrive on the main
 * thread and the extraction is cheap (architecture §5), so work is forwarded synchronously
 * and the sink is responsible for hopping to its own coroutine scope.
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
        activeInstance = this
        handler.removeCallbacks(rebindAttempt)
        HearItLogger.info(LogCategory.NOTIFICATION, "NotificationListener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (activeInstance === this) activeInstance = null
        handler.removeCallbacks(rebindAttempt)
        handler.postDelayed(rebindAttempt, REBIND_DELAY_MILLIS)
        HearItLogger.warn(LogCategory.NOTIFICATION, "NotificationListener disconnected; rebind scheduled")
    }

    override fun onDestroy() {
        if (activeInstance === this) activeInstance = null
        handler.removeCallbacks(rebindAttempt)
        super.onDestroy()
        HearItLogger.info(LogCategory.NOTIFICATION, "NotificationListener destroyed")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        // Our own notifications (the P08 persistent notification) are never candidates.
        if (sbn.packageName == packageName) return

        HearItLogger.debug(LogCategory.NOTIFICATION, "Notification received: pkg=${sbn.packageName}, id=${sbn.id}")
        val raw = StatusBarNotificationMapper.toRaw(sbn, appLabelFor(sbn.packageName))
        val snapshot = extractor.extract(raw, System.currentTimeMillis())
        // Per-instance sink (tests) wins; otherwise the process-wide relay feeds the
        // announcement service registered in P08.
        (sink ?: NotificationRelay).onPosted(snapshot)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn == null) return
        if (sbn.packageName == packageName) return

        HearItLogger.debug(LogCategory.NOTIFICATION, "Notification removed: pkg=${sbn.packageName}, id=${sbn.id}")
        val key = NotificationExtractor.keyOf(sbn.packageName, sbn.tag, sbn.id)
        extractor.forget(key)
        (sink ?: NotificationRelay).onRemoved(key)
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

    companion object {
        private var activeInstance: HearItNotificationListenerService? = null

        val isConnected: Boolean get() = activeInstance != null

        fun dismissNotification(key: String, packageName: String? = null, tag: String? = null, id: Int? = null) {
            val service = activeInstance ?: return
            try {
                service.cancelNotification(key)
            } catch (_: Throwable) {
                if (packageName != null && id != null) {
                    try {
                        service.cancelNotification(packageName, tag, id)
                    } catch (_: Throwable) {}
                }
            }
        }

        fun requestRebindIfDisconnected(context: Context) {
            if (activeInstance != null) return
            val componentName = ComponentName(context, HearItNotificationListenerService::class.java)
            try {
                requestRebind(componentName)
            } catch (_: SecurityException) {
            } catch (_: Throwable) {
                try {
                    val pm = context.packageManager
                    pm.setComponentEnabledSetting(
                        componentName,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP,
                    )
                    pm.setComponentEnabledSetting(
                        componentName,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP,
                    )
                } catch (_: Throwable) {}
            }
        }

        /** Single delayed retry after a disconnect: enough for transient unbinds, no storm. */
        const val REBIND_DELAY_MILLIS = 5_000L
    }
}
