package com.jayanth.hearit.core.common.model

/** What the text-length limit is measured against (master prompt §6, "Limit scope"). */
enum class LengthScope {
    /** Limit applies to each message separately — recommended default for messaging apps. */
    MESSAGE,

    /** Limit applies to the whole notification (sender + message) as one block. */
    WHOLE_NOTIFICATION,
}

/**
 * Text-length limit for announcements (master prompt §6).
 *
 * The rule **skips** the applicable message/notification entirely when the limit is
 * exceeded — partial text is never read, so no truncation can happen anywhere in the
 * pipeline (the snapshot always keeps the full text and `EligibilityRules` returns
 * `Skip(TOO_LONG)` instead of shortening it).
 *
 * @property maxLengthChars Maximum length in characters, or null for no limit. Shipped
 *   default is **no limit** because the rule skips rather than truncates: a numeric
 *   default would silently hide long notifications the user never asked to hide (ADR-015);
 *   the settings UI recommends ~500 characters instead.
 * @property scope Whether the limit counts one message or the whole notification. Shipped
 *   default [LengthScope.MESSAGE], §6's recommendation for messaging apps.
 */
data class TextLengthSettings(
    val maxLengthChars: Int? = null,
    val scope: LengthScope = LengthScope.MESSAGE,
)
