package com.jayanth.hearit.core.notifications

import com.jayanth.hearit.core.common.model.NotificationSnapshot

/**
 * Turns platform values ([RawNotification]) into the domain model [NotificationSnapshot].
 *
 * Pure Kotlin with no Android types, so every extraction rule is JVM-unit-testable
 * (ADR-002/ADR-003: the Android surface is only `StatusBarNotification` → [RawNotification]).
 *
 * Extraction rules (master prompt: missing title/text/extras are normal):
 *  - **identity:** `key = packageName|tag|id` — stable across updates of the same
 *    notification, and new again after [forget];
 *  - **app name:** trimmed app label, falling back to the package name when absent/blank;
 *  - **sender:** the trimmed title, except when it merely repeats the app name (then the
 *    app has no sender line and announcing "WhatsApp. WhatsApp says" would be wrong);
 *  - **message text:** expanded `bigText` preferred over `text`; blank values fall through;
 *    the result is trimmed but never truncated (text-length rules skip, they do not cut —
 *    master prompt §6);
 *  - **post time:** the platform value when present, otherwise the caller's current time;
 *  - **channel/category:** passed through untouched (silence is a channel property, never a
 *    device property — ADR-017);
 *  - **update flag:** [NotificationKeyTracker] decides new vs update.
 *
 * Not thread-safe: driven from the listener's main thread (architecture §5).
 */
class NotificationExtractor(
    private val tracker: NotificationKeyTracker = NotificationKeyTracker(),
) {

    /**
     * Extracts [raw] at [nowMillis] (used only when the platform post time is missing).
     *
     * @throws IllegalArgumentException from [NotificationSnapshot] invariants only
     *   ([RawNotification] already guarantees a non-blank package).
     */
    fun extract(raw: RawNotification, nowMillis: Long): NotificationSnapshot {
        val appName = raw.appLabel?.trim()?.takeIf { it.isNotEmpty() } ?: raw.packageName
        val rawTitle = raw.title.trimmedOrNull()?.takeUnless { it.equals(appName, ignoreCase = true) }
        val rawSubText = raw.subText.trimmedOrNull()?.takeUnless { it.equals(appName, ignoreCase = true) }
        val convTitle = raw.conversationTitle.trimmedOrNull()?.takeUnless { it.equals(appName, ignoreCase = true) }

        var resolvedSender: String? = raw.messagingSender.trimmedOrNull()
        var resolvedGroupTitle: String? = convTitle

        if (resolvedSender != null) {
            // MessagingStyle: messagingSender is the individual message author
            if (resolvedGroupTitle == null) {
                // If notification title differs from author, it holds the group title (e.g. WhatsApp groups)
                if (rawTitle != null && !rawTitle.equals(resolvedSender, ignoreCase = true)) {
                    resolvedGroupTitle = if (rawTitle.contains(" @ ")) {
                        rawTitle.substringAfter(" @ ").trim()
                    } else {
                        rawTitle
                    }
                } else if (rawSubText != null && !rawSubText.equals(resolvedSender, ignoreCase = true)) {
                    resolvedGroupTitle = rawSubText
                }
            }
        } else if (rawTitle != null) {
            if (rawTitle.contains(" @ ")) {
                val parts = rawTitle.split(" @ ", limit = 2)
                resolvedSender = parts[0].trim()
                if (resolvedGroupTitle == null) {
                    resolvedGroupTitle = parts[1].trim()
                }
            } else {
                resolvedSender = rawTitle
            }
        }

        val isGroup = raw.isGroupConversation ||
            (resolvedGroupTitle != null && resolvedSender != null && !resolvedGroupTitle.equals(resolvedSender, ignoreCase = true))

        val messageText = raw.messagingText.trimmedOrNull()
            ?: raw.bigText.trimmedOrNull()
            ?: raw.text.trimmedOrNull().orEmpty()
        val key = keyOf(raw)

        return NotificationSnapshot(
            key = key,
            packageName = raw.packageName,
            appName = appName,
            sender = resolvedSender?.takeUnless { it.equals(appName, ignoreCase = true) },
            messageText = messageText,
            channelId = raw.channelId,
            isSilentChannel = raw.channelIsSilent,
            category = raw.category,
            postTimeMillis = raw.postTimeMillis.takeIf { it > 0L } ?: nowMillis,
            isUpdate = tracker.markSeen(key),
            isGroup = isGroup,
            groupTitle = resolvedGroupTitle?.takeUnless { it.equals(appName, ignoreCase = true) },
            isGroupSummary = raw.isGroupSummary,
        )
    }

    /** Forgets the identity of a removed notification so a later post counts as new. */
    fun forget(key: String) {
        tracker.forget(key)
    }

    /** Current tracker size (bounded); exposed for diagnostics and tests. */
    val trackedKeyCount: Int get() = tracker.size

    companion object {
        /** Builds the stable snapshot identity from the platform parts, e.g. `com.app|tag|42`. */
        fun keyOf(packageName: String, tag: String?, id: Int): String =
            "$packageName|${tag.orEmpty()}|$id"

        /** [keyOf] for a [RawNotification]. */
        fun keyOf(raw: RawNotification): String = keyOf(raw.packageName, raw.tag, raw.id)
    }
}

/** Trim, then treat blank as absent: extraction's one text fallback rule. */
private fun String?.trimmedOrNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
