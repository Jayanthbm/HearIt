package com.jayanth.hearit.core.notifications

/**
 * Platform values read off one posted notification, before any HearIt interpretation.
 *
 * Produced by the Android mapping layer from a `StatusBarNotification`; deliberately
 * plain Kotlin so [NotificationExtractor] and its rules are unit-testable on the JVM
 * (ADR-002/ADR-003: extraction lives in `:core:notifications`, the domain model in
 * `:core:common`).
 *
 * Every field mirrors what Android actually exposed: nullable where the app may omit a
 * value. Missing values are *normal*, not errors — extraction decides the fallbacks
 * (master prompt: extraction handles missing title/text/extras).
 *
 * @property packageName Posting application package name (never blank).
 * @property appLabel Resolved application label; null when the label could not be resolved,
 *   in which case extraction falls back to [packageName].
 * @property tag Notification tag from the platform identity, null when the app used none.
 * @property id Notification id from the platform identity.
 * @property title `EXTRA_TITLE`, null when absent or blank.
 * @property text `EXTRA_TEXT`, null when absent or blank.
 * @property bigText `EXTRA_BIG_TEXT` (expanded text), null when absent or blank.
 * @property subText `EXTRA_SUB_TEXT`, reserved for extraction refinements.
 * @property channelId Notification channel id, null before channels existed (never on 26+).
 * @property channelIsSilent True when the posting channel produces neither sound nor
 *   vibration. A property of the notification, never of the device (ADR-017).
 * @property category `Notification.CATEGORY_*` hint, null when the app provides none.
 * @property postTimeMillis Platform post time, 0 when unknown.
 */
data class RawNotification(
    val packageName: String,
    val appLabel: String? = null,
    val tag: String? = null,
    val id: Int = 0,
    val title: String? = null,
    val text: String? = null,
    val bigText: String? = null,
    val subText: String? = null,
    val channelId: String? = null,
    val channelIsSilent: Boolean = false,
    val category: String? = null,
    val postTimeMillis: Long = 0L,
    val isGroupConversation: Boolean = false,
    val conversationTitle: String? = null,
    val messagingSender: String? = null,
    val messagingText: String? = null,
) {
    init {
        require(packageName.isNotBlank()) { "RawNotification.packageName must not be blank" }
    }
}
