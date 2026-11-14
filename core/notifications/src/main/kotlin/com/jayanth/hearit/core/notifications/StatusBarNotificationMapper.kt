package com.jayanth.hearit.core.notifications

import android.app.Notification
import android.service.notification.StatusBarNotification

/**
 * The only Android-aware step of extraction: reads a platform [StatusBarNotification]
 * into [RawNotification] without interpreting it.
 *
 * Deliberately dumb and side-effect free. Missing extras (title/text/bigText/subText),
 * a missing category or a missing channel are all normal and map to `null`; blank text is
 * normalized to `null` so the extractor's fallbacks decide what to use.
 *
 * **Known limitation — `channelIsSilent`:** a channel's sound/vibration configuration is
 * only readable for the app's *own* channels on public API 29+ (`NotificationManager`
 * has no cross-package channel query), so this mapper cannot set it. It stays `false`
 * (assumed audible) and the field remains part of [RawNotification] for sources that can
 * provide it (tests, future platform APIs). Tracked in the P07 progress record.
 */
internal object StatusBarNotificationMapper {

    /**
     * @param appLabel label resolved by the service (null when it could not be resolved;
     *   the extractor then falls back to the package name).
     */
    fun toRaw(sbn: StatusBarNotification, appLabel: String?): RawNotification {
        val notification: Notification? = sbn.notification
        val extras = notification?.extras

        var isGroup = false
        var conversationTitle: String? = null
        var messagingSender: String? = null
        var messagingText: String? = null

        if (extras != null) {
            isGroup = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false)
            conversationTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE).asTextOrNull()

            // 1. Check Notification.EXTRA_MESSAGES (MessagingStyle)
            val messagesArray = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            if (!messagesArray.isNullOrEmpty()) {
                val lastMsg = messagesArray.lastOrNull()
                if (lastMsg is android.os.Bundle) {
                    messagingText = lastMsg.getCharSequence("text").asTextOrNull()
                    val personBundle = lastMsg.getBundle("sender_person")
                    messagingSender = personBundle?.getCharSequence("name").asTextOrNull()
                        ?: lastMsg.getCharSequence("sender").asTextOrNull()
                }
            }

            // 2. Fallback: Check Notification.EXTRA_TEXT_LINES (InboxStyle)
            if (messagingText == null) {
                val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                if (!lines.isNullOrEmpty()) {
                    messagingText = lines.lastOrNull { !it.isNullOrBlank() }.asTextOrNull()
                }
            }
        }

        return RawNotification(
            packageName = sbn.packageName,
            appLabel = appLabel,
            tag = sbn.tag,
            id = sbn.id,
            title = extras?.getCharSequence(Notification.EXTRA_TITLE).asTextOrNull(),
            text = extras?.getCharSequence(Notification.EXTRA_TEXT).asTextOrNull(),
            bigText = extras?.getCharSequence(Notification.EXTRA_BIG_TEXT).asTextOrNull(),
            subText = extras?.getCharSequence(Notification.EXTRA_SUB_TEXT).asTextOrNull(),
            channelId = notification?.channelId,
            channelIsSilent = false, // see class KDoc: not readable cross-package on API 29+
            category = notification?.category,
            postTimeMillis = sbn.postTime,
            isGroupConversation = isGroup,
            conversationTitle = conversationTitle,
            messagingSender = messagingSender,
            messagingText = messagingText,
            isGroupSummary = (notification?.flags ?: 0) and Notification.FLAG_GROUP_SUMMARY != 0,
        )
    }

    private fun CharSequence?.asTextOrNull(): String? =
        this?.toString()?.takeIf { it.isNotBlank() }
}
