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

    /** Id of the single ongoing notification; the service uses it for `startForeground`. */
    const val NOTIFICATION_ID = 1

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
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                setBlockable(true)
            }
        }
        manager.createNotificationChannel(channel)
    }

    /**
     * Returns true if the service status notification is visible in the notification panel/shade.
     * Returns false if notifications are globally disabled for Hear It or if this channel is blocked.
     */
    fun isNotificationVisible(context: Context): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return true
        if (!manager.areNotificationsEnabled()) {
            return false
        }
        val channel = manager.getNotificationChannel(CHANNEL_ID) ?: return true
        return channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    /**
     * Opens system notification settings specifically for the announcement service channel.
     * Falls back to app notification settings if channel settings cannot be launched.
     */
    fun openNotificationSettings(context: Context) {
        ensureChannel(context)
        val channelIntent = Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
            putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, CHANNEL_ID)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(channelIntent)
        } catch (_: Throwable) {
            val appIntent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(appIntent)
            } catch (_: Throwable) {}
        }
    }

    /** Builds the ongoing notification; [speaking] and state changes dynamically configure actions. */
    fun build(
        context: Context,
        speaking: Boolean,
        isPriorityAnnounce: Boolean = false,
        isMuted: Boolean = false,
    ): Notification {
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            context,
            1,
            Intent(context, AnnouncementService::class.java).setAction(AnnouncementService.ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
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
            .setContentText(
                if (isMuted) {
                    "Announcements temporarily muted"
                } else if (isPriorityAnnounce) {
                    "Priority Announce active"
                } else {
                    context.getString(R.string.notification_text_service)
                },
            )
            .setContentIntent(openApp)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)

        if (speaking) {
            val pause = PendingIntent.getService(
                context,
                2,
                Intent(context, AnnouncementService::class.java).setAction(AnnouncementService.ACTION_PAUSE),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val skip = PendingIntent.getService(
                context,
                3,
                Intent(context, AnnouncementService::class.java).setAction(AnnouncementService.ACTION_SKIP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            builder.addAction(0, context.getString(R.string.notification_action_pause), pause)
            builder.addAction(0, context.getString(R.string.notification_action_skip), skip)
        } else {
            val priorityToggle = PendingIntent.getService(
                context,
                4,
                Intent(context, AnnouncementService::class.java).setAction(AnnouncementService.ACTION_PRIORITY_ANNOUNCE_TOGGLE),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val muteToggle = PendingIntent.getService(
                context,
                5,
                Intent(context, AnnouncementService::class.java).setAction(AnnouncementService.ACTION_TEMPORARY_MUTE_TOGGLE),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val priorityLabel = if (isPriorityAnnounce) {
                context.getString(R.string.notification_action_priority_off)
            } else {
                context.getString(R.string.notification_action_priority_on)
            }
            val muteLabel = if (isMuted) {
                context.getString(R.string.notification_action_unmute)
            } else {
                context.getString(R.string.notification_action_mute)
            }
            builder.addAction(0, priorityLabel, priorityToggle)
            builder.addAction(0, muteLabel, muteToggle)
        }

        builder.addAction(0, context.getString(R.string.notification_action_stop), stop)

        return builder.build()
    }
}
