package com.jayanth.hearit.core.common.model

/**
 * Immutable, platform-free representation of a posted notification.
 *
 * Produced by the notification listener (phase P07) from an `android.app.Notification`
 * and consumed by the eligibility rules, the announcement queue and history storage.
 * It deliberately contains no Android types so that every consumer can be unit-tested
 * on the JVM.
 *
 * Snapshot identity: [key] must be stable across an update of the same notification
 * (package + tag + id), because deduplication of announcement updates (master prompt §8/§10)
 * and "update existing vs new entry" history handling (§12) both depend on it.
 *
 * @property key Stable notification identity, e.g. `com.example.app|tag|42`.
 * @property packageName Posting application package name.
 * @property appName User-visible name of the posting application.
 * @property sender Conversation/sender name when the notification exposes one, e.g. the
 *   summary/title line of a messaging notification. Null when not available.
 * @property messageText Full concatenated message text exactly as posted. Never truncated:
 *   text-length limits are applied by the rules pipeline, which skips whole items instead
 *   of reading partial text (master prompt §6).
 * @property channelId Notification channel id, kept as metadata for the extraction layer.
 * @property isSilentChannel True when the posting channel produces no sound. This is a
 *   property of the notification, **not** of the device: Do Not Disturb is read only from
 *   `RuntimeState.isDndActive`, and channel silence never triggers or suppresses the DND
 *   rule (master prompt §6, ADR-017). Whether silent-channel items should be announced is
 *   not specified by the master prompt and is currently a no-op in eligibility.
 * @property category `android.app.Notification.CATEGORY_*` value when the app provides one.
 *   Android does not guarantee a standardized semantic category, so this is a hint only.
 * @property postTimeMillis Wall-clock time the notification was posted, epoch milliseconds.
 *   The extraction layer falls back to the current time when the platform value is missing.
 * @property isUpdate True when this snapshot updates a previously seen [key] rather than
 *   being a genuinely new notification.
 */
data class NotificationSnapshot(
    val key: String,
    val packageName: String,
    val appName: String,
    val sender: String? = null,
    val messageText: String = "",
    val channelId: String? = null,
    val isSilentChannel: Boolean = false,
    val category: String? = null,
    val postTimeMillis: Long,
    val isUpdate: Boolean = false,
    val isGroup: Boolean = false,
    val groupTitle: String? = null,
    val isGroupSummary: Boolean = false,
) {
    init {
        require(key.isNotBlank()) { "NotificationSnapshot.key must not be blank" }
        require(packageName.isNotBlank()) { "NotificationSnapshot.packageName must not be blank" }
        require(appName.isNotBlank()) { "NotificationSnapshot.appName must not be blank" }
        require(postTimeMillis >= 0) { "NotificationSnapshot.postTimeMillis must not be negative" }
    }
}
