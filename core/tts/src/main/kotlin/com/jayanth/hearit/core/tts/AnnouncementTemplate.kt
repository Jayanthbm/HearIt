package com.jayanth.hearit.core.tts

import com.jayanth.hearit.core.common.model.AppCategory
import com.jayanth.hearit.core.common.model.NotificationSnapshot

/**
 * Which part of a notification an [AnnouncementSection] carries (master prompt §7).
 * [MESSAGE_COUNT] is added by §8's grouped announcements (phase P05).
 */
enum class SectionKind {
    INTRODUCTION,
    APP_NAME,

    /** "3 messages." — grouped announcements only, never emitted for a single item. */
    MESSAGE_COUNT,
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
 *  - Sections are emitted in fixed order: introduction, app name, message count (only
 *    when more than one notification is grouped), then sender/message per notification
 *    in arrival order (master prompt §8).
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
    fun build(snapshot: NotificationSnapshot, format: AnnouncementFormat): BuiltAnnouncement =
        build(listOf(snapshot), format)

    /**
     * Builds the announcement for a grouped set of notifications from the same app
     * (master prompt §8: "Announce app name and message count, followed by available
     * sender/message content"): introduction, app name, "N messages." when more than one,
     * then each notification's sender and message in arrival order.
     *
     * [snapshots] must be non-empty and all from one package — the queue guarantees both
     * (ADR-019); a single-element list produces exactly the single-notification sections.
     * Pure and deterministic like the single overload.
     */
    fun build(snapshots: List<NotificationSnapshot>, format: AnnouncementFormat): BuiltAnnouncement {
        require(snapshots.isNotEmpty()) { "AnnouncementTemplate.build requires at least one snapshot" }
        val packageName = snapshots.first().packageName
        require(snapshots.all { it.packageName == packageName }) {
            "AnnouncementTemplate.build groups only notifications from one package ($packageName)"
        }

        val activeSnapshots = snapshots.filterNot { snapshot ->
            SmartContentCleaner.shouldSuppress(snapshot.messageText, packageName)
        }
        if (activeSnapshots.isEmpty()) {
            return BuiltAnnouncement(emptyList(), format.pauseBetweenSectionsMillis)
        }

        val sections = mutableListOf<AnnouncementSection>()

        format.introduction?.trim()?.takeIf { it.isNotEmpty() }?.let { intro ->
            sections += AnnouncementSection(SectionKind.INTRODUCTION, intro)
        }

        val rawAppName = activeSnapshots.first().appName.trim()
        val appName = SmartContentCleaner.resolveAppName(packageName, rawAppName)
        if (format.includeAppName && appName.isNotEmpty()) {
            sections += AnnouncementSection(SectionKind.APP_NAME, "$appName.")
        }

        // §8: always plural — grouped lists have at least two members when this is emitted.
        if (activeSnapshots.size > 1) {
            sections += AnnouncementSection(SectionKind.MESSAGE_COUNT, "${activeSnapshots.size} messages.")
        }

        val category = SmartContentCleaner.resolveCategory(packageName)

        for (snapshot in activeSnapshots) {
            val resolved = resolveGroupAndContent(snapshot, category)
            val sender = SmartContentCleaner.cleanSender(resolved.sender) ?: resolved.sender
            val message = SmartContentCleaner.cleanMessage(resolved.messageText, packageName).text

            if (format.includeSender && sender.isNotEmpty()) {
                val text = if (!resolved.groupTitle.isNullOrBlank()) {
                    if (format.includeMessage && message.isNotEmpty()) {
                        "$sender in ${resolved.groupTitle} says,"
                    } else {
                        "$sender in ${resolved.groupTitle}."
                    }
                } else if (format.includeMessage && message.isNotEmpty()) {
                    "$sender says,"
                } else {
                    "$sender."
                }
                sections += AnnouncementSection(SectionKind.SENDER, text)
            }

            if (format.includeMessage && message.isNotEmpty()) {
                sections += AnnouncementSection(SectionKind.MESSAGE, message)
            }
        }

        return BuiltAnnouncement(sections, format.pauseBetweenSectionsMillis)
    }

    private data class ResolvedSnapshotContent(
        val sender: String,
        val groupTitle: String?,
        val messageText: String,
    )

    private fun resolveGroupAndContent(
        snapshot: NotificationSnapshot,
        category: AppCategory?,
    ): ResolvedSnapshotContent {
        val rawSender = snapshot.sender?.trim().orEmpty()
        val rawGroup = if (snapshot.isGroup) snapshot.groupTitle?.trim()?.takeIf { it.isNotEmpty() } else null
        val rawMessage = snapshot.messageText.trim()

        if (!SmartContentCleaner.isMessagingIntelligenceEnabled) {
            return ResolvedSnapshotContent(rawSender, rawGroup, rawMessage)
        }

        // Already structured group metadata from Android MessagingStyle
        if (rawGroup != null) {
            return ResolvedSnapshotContent(rawSender, rawGroup, rawMessage)
        }

        // Inline format 1: sender itself is "Group Name: Sender Name"
        val colonSplitSender = INLINE_GROUP_SENDER_REGEX.matchEntire(rawSender)
        if (colonSplitSender != null) {
            val group = colonSplitSender.groupValues[1].trim()
            val member = colonSplitSender.groupValues[2].trim()
            if (group.isNotEmpty() && member.isNotEmpty() && !NON_GROUP_PREFIXES.contains(group.lowercase())) {
                return ResolvedSnapshotContent(member, group, rawMessage)
            }
        }

        // Inline format 2 (Messaging apps): sender is group name, message starts with "Member: text"
        if (category == AppCategory.MESSAGING && rawSender.isNotEmpty()) {
            val inlineMemberMsg = INLINE_MEMBER_MESSAGE_REGEX.matchEntire(rawMessage)
            if (inlineMemberMsg != null) {
                val member = inlineMemberMsg.groupValues[1].trim()
                val body = inlineMemberMsg.groupValues[2].trim()
                if (!NON_GROUP_PREFIXES.contains(member.lowercase()) && !member.contains("http", ignoreCase = true)) {
                    return ResolvedSnapshotContent(member, rawSender, body)
                }
            }
        }

        // Inline format 3 (Messaging apps): missing sender, message starts with "Group: Member: text"
        if (category == AppCategory.MESSAGING && rawSender.isEmpty()) {
            val triple = TRIPLE_GROUP_REGEX.matchEntire(rawMessage)
            if (triple != null) {
                val group = triple.groupValues[1].trim()
                val member = triple.groupValues[2].trim()
                val body = triple.groupValues[3].trim()
                if (!NON_GROUP_PREFIXES.contains(group.lowercase()) && !NON_GROUP_PREFIXES.contains(member.lowercase())) {
                    return ResolvedSnapshotContent(member, group, body)
                }
            }
        }

        return ResolvedSnapshotContent(rawSender, null, rawMessage)
    }

    private val INLINE_GROUP_SENDER_REGEX = Regex("^([^:]{2,40}):\\s+([^:]{1,40})$")
    private val INLINE_MEMBER_MESSAGE_REGEX = Regex("^([A-Za-z][A-Za-z0-9 ._'-]{1,28}):\\s+(.+)$")
    private val TRIPLE_GROUP_REGEX = Regex("^([^:]{2,35}):\\s+([A-Za-z][^:]{1,28}):\\s+(.+)$")
    private val NON_GROUP_PREFIXES = setOf("re", "fwd", "fw", "reply", "forward", "http", "https", "note", "alert", "document", "contact")
}
