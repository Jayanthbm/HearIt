package com.jayanth.hearit.core.common.model

/**
 * How app selection / history storing behaves for an app (master prompt §12: "Global/per-app
 * storage modes: Full content (default), Metadata only, Disable history; per-app can inherit
 * global").
 */
enum class HistoryStorageMode {

    /** Store sender and full message text. Default (master prompt §12). */
    FULL_CONTENT,

    /** Store app, sender, timestamps and status only — never the message body. */
    METADATA_ONLY,

    /** Do not store this app's notifications at all (speech is unaffected). */
    DISABLED,
}

/** Update handling for history entries (master prompt §12). */
enum class HistoryUpdateHandling {

    /** A notification update rewrites one existing history entry. Default. */
    UPDATE_ENTRY,

    /** Every update is stored as its own history entry. */
    NEW_ENTRY,
}

/**
 * History behavior (master prompt §12, phase P10 builds storage on top of it).
 *
 * Defaults: full content, updates replace their entry, unlimited retention (the user
 * chooses a retention period in settings), no biometric lock (master prompt §12:
 * optional authentication, "disabled by default").
 *
 * @property storageMode Content captured for history; [HistoryStorageMode.FULL_CONTENT]
 *   by default, matching the master prompt.
 * @property updateHandling Whether an update replaces its entry or adds a new one.
 * @property retentionDays Days to keep history, or null for no automatic deletion.
 *   Global only — per-app overrides never change retention (kept in one place).
 * @property requireAuth Require device authentication to open history. Default off,
 *   following the master prompt's "disabled by default".
 */
data class HistorySettings(
    val storageMode: HistoryStorageMode = HistoryStorageMode.FULL_CONTENT,
    val updateHandling: HistoryUpdateHandling = HistoryUpdateHandling.UPDATE_ENTRY,
    val retentionDays: Int? = null,
    val requireAuth: Boolean = false,
) {

    init {
        if (retentionDays != null) {
            require(retentionDays >= 0) {
                "HistorySettings.retentionDays must not be negative, was $retentionDays"
            }
        }
    }

    /** Merges this app's override; null fields inherit the global value. */
    fun resolveWith(override: AppHistoryOverride?): HistorySettings =
        if (override == null) {
            this
        } else {
            copy(
                storageMode = override.storageMode ?: storageMode,
                updateHandling = override.updateHandling ?: updateHandling,
                requireAuth = override.requireAuth ?: requireAuth,
            )
        }
}

/**
 * Per-app history override (master prompt §12: per-app storage mode with inheritance).
 * A null field inherits the global value; [HistorySettings.retentionDays] is not
 * overridable per app.
 */
data class AppHistoryOverride(
    val storageMode: HistoryStorageMode? = null,
    val updateHandling: HistoryUpdateHandling? = null,
    val requireAuth: Boolean? = null,
) {

    /** True when this app inherits every history value (UI "Inherits global"). */
    val inheritsEveryValue: Boolean
        get() = storageMode == null && updateHandling == null && requireAuth == null
}
