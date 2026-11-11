package com.jayanth.hearit.core.tts

/**
 * User-configurable announcement format (master prompt §7), pure Kotlin with no Android
 * types.
 *
 * The default matches the master prompt's default format — app name + sender (if
 * available) + message — with no introduction and no artificial pauses between sections.
 * Section text itself (e.g. the `says,` connector) is fixed by [AnnouncementTemplate] so
 * the spoken wording stays in one place; the knobs here are exactly §7's configurable
 * items: app name, sender, message, custom introduction and pauses between sections.
 * Speaking speed, pitch and voice live in [SpeechSettings] because they are applied by the
 * engine rather than by the template (ADR-018).
 *
 * @property includeAppName Speak the posting app's name. Default on.
 * @property includeSender Speak the conversation sender when the notification exposes one.
 *   Default on; the section disappears entirely when no usable sender exists.
 * @property includeMessage Speak the message text. Default on.
 * @property introduction Optional custom text spoken before every other section, e.g.
 *   "New message". Blank or null adds nothing (master prompt §7: no default introduction).
 * @property pauseBetweenSectionsMillis Silence the speaker inserts between consecutive
 *   sections. Default 0 (no artificial pause; sentence punctuation still paces speech).
 */
data class AnnouncementFormat(
    val includeAppName: Boolean = true,
    val includeSender: Boolean = true,
    val includeMessage: Boolean = true,
    val introduction: String? = null,
    val pauseBetweenSectionsMillis: Long = 0L,
) {
    init {
        require(pauseBetweenSectionsMillis >= 0L) {
            "AnnouncementFormat.pauseBetweenSectionsMillis must not be negative"
        }
    }
}
