package com.jayanth.hearit.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.jayanth.hearit.MainActivity
import com.jayanth.hearit.R

/**
 * Persistent notification for the announcement service (master prompt §4: "a persistent
 * notification with controls"; ADR-010: the foreground service owns it).
 *
 * Content is static service state only — never notification text, so nothing private can
 * appear on the lock screen. The only control is Stop; V4.0-style actions
 * (pause/mute/priority) arrive with the features that define them (P12/P16/P17).
 *
 * A `POST_NOTIFICATIONS` permission is intentionally not declared yet (P06 decision):
 * from API 33 the channel exists but may stay hidden until the setup flow explains and
 * requests it (P11). The foreground service itself still runs.
 */
internal object AnnouncementNotification {

    const val CHANNEL_ID = "announcement_service"
    private const val CHANNEL_NAME_RES = R.string.notification_channel_service
    private const val NOTIFICATION_ID = 1

    /** Creates the low-importance channel; idempotent, safe to call on every start. */
    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(CHANNEL_NAME_RES),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notification_channel_service_description)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    /** Builds the ongoing notification; [speaking] only changes the title line. */
    fun build(context: Context, speaking: Boolean): Notification {
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            context,
            0,
            Intent(context, AnnouncementService::class.java).setAction(AnnouncementService.ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(
                context.getString(
                    if (speaking) {
                        R.string.notification_title_speaking
                    } else {
                        R.string.notification_title_running
                    },
                ),
            )
            .setContentText(context.getString(R.string.notification_text_service))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .addAction(0, context.getString(R.string.notification_action_stop), stop)
            .build()
    }
}
