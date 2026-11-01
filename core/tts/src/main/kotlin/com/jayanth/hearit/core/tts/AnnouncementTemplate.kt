package com.jayanth.hearit.core.tts

import com.jayanth.hearit.core.common.model.NotificationSnapshot

/**
 * Which part of a notification an [AnnouncementSection] carries (master prompt §7).
 * P05 extends this with a grouped-message count for §8's grouped announcements.
 */
enum class SectionKind {
    INTRODUCTION,
    APP_NAME,
    SENDER,
    MESSAGE,
}

/** One spoken piece of an announcement, already punctuated for speech. */
data class AnnouncementSection(
    val kind: SectionKind,
    val text: String,
)

/**
 * Result of [AnnouncementTemplate.build]: an ordered list of sections plus the pause the
 * speaker inserts between consecutive sections.
 *
 * Structured rather than a flat string because §7 requires pauses *between sections*,
 * which a single string cannot express; the speaking layer synthesizes each section and
 * inserts the gap (ADR-018).
 *
 * @property sections Non-empty only when there is something to speak; an empty list means
 *   the speaker must skip the item entirely ([isEmpty]).
 * @property pauseBetweenSectionsMillis Non-negative silence between consecutive sections.
 */
data class BuiltAnnouncement(
    val sections: List<AnnouncementSection>,
    val pauseBetweenSectionsMillis: Long,
) {
    init {
        require(pauseBetweenSectionsMillis >= 0L) {
            "BuiltAnnouncement.pauseBetweenSectionsMillis must not be negative"
        }
    }

    /** True when every enabled section was empty — there is nothing to speak. */
    val isEmpty: Boolean
        get() = sections.isEmpty()
}

/**
 * Builds the spoken form of a notification from its snapshot and the configured format
 * (master prompt §7: "Default format: app name + sender (if available) + message",
 * e.g. "WhatsApp. Rahul says, Hey, are you coming today?").
 *
 * Pure logic: no Android TTS calls, no locale-sensitive formatting, no clock. Rules:
 *  - Sections are emitted in fixed order: introduction, app name, sender, message.
 *  - A missing or blank sender omits the whole section — never a dangling connector.
 *  - A sender without a usable message is spoken plainly ("Rahul."), not "Rahul says,".
 *  - The message text is spoken verbatim (after trimming surrounding whitespace) and is
 *    never truncated — content limits belong to the eligibility rules.
 *  - When every enabled section is empty the result [BuiltAnnouncement.isEmpty] is true.
 *
 * Missing sender/message are handled gracefully by construction; the default format
 * reproduces the master prompt's example exactly (ADR-018).
 */
object AnnouncementTemplate {

    /** Builds the announcement for [snapshot]; pure and deterministic. */
    fun build(snapshot: NotificationSnapshot, format: AnnouncementFormat): BuiltAnnouncement {
        val sections = mutableListOf<AnnouncementSection>()

        format.introduction?.trim()?.takeIf { it.isNotEmpty() }?.let { intro ->
            sections += AnnouncementSection(SectionKind.INTRODUCTION, intro)
        }

        val appName = snapshot.appName.trim()
        if (format.includeAppName && appName.isNotEmpty()) {
            sections += AnnouncementSection(SectionKind.APP_NAME, "$appName.")
        }

        val sender = snapshot.sender?.trim().orEmpty()
        val message = snapshot.messageText.trim()
        if (format.includeSender && sender.isNotEmpty()) {
            // With a message: "Rahul says," — without one: "Rahul." (no dangling grammar).
            val text = if (format.includeMessage && message.isNotEmpty()) {
                "$sender says,"
            } else {
                "$sender."
            }
            sections += AnnouncementSection(SectionKind.SENDER, text)
        }

        if (format.includeMessage && message.isNotEmpty()) {
            sections += AnnouncementSection(SectionKind.MESSAGE, message)
        }

        return BuiltAnnouncement(sections, format.pauseBetweenSectionsMillis)
    }
}
