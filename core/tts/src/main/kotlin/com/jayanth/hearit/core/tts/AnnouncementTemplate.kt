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
    val isCritical: Boolean = false,
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
    fun build(
        snapshot: NotificationSnapshot,
        format: AnnouncementFormat,
        isCritical: Boolean = false,
        delayedMinutesByKey: Map<String, Long> = emptyMap(),
    ): BuiltAnnouncement = build(listOf(snapshot), format, isCritical, delayedMinutesByKey)

    /**
     * Builds the announcement for a grouped set of notifications from the same app.
     */
    fun build(
        snapshots: List<NotificationSnapshot>,
        format: AnnouncementFormat,
        isCritical: Boolean = false,
        delayedMinutesByKey: Map<String, Long> = emptyMap(),
        mergeSenderBursts: Boolean = false,
        senderBurstMinCount: Int = 2,
        senderBurstWindowMs: Long = 5_000L,
    ): BuiltAnnouncement {
        require(snapshots.isNotEmpty()) { "AnnouncementTemplate.build requires at least one snapshot" }
        val packageName = snapshots.first().packageName
        require(snapshots.all { it.packageName == packageName }) {
            "AnnouncementTemplate.build groups only notifications from one package ($packageName)"
        }

        val activeSnapshots = snapshots.filterNot { snapshot ->
            SmartContentCleaner.shouldSuppress(snapshot.messageText, packageName)
        }
        if (activeSnapshots.isEmpty()) {
            return BuiltAnnouncement(emptyList(), format.pauseBetweenSectionsMillis, isCritical = isCritical)
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
        if (activeSnapshots.size > 1 && !mergeSenderBursts) {
            sections += AnnouncementSection(SectionKind.MESSAGE_COUNT, "${activeSnapshots.size} messages.")
        }

        val category = SmartContentCleaner.resolveCategory(packageName)

        data class PreparedItem(
            val snapshot: NotificationSnapshot,
            val sender: String,
            val groupTitle: String?,
            val message: String,
            val delayedMins: Long?,
        ) {
            val senderSubject: String
                get() = if (!groupTitle.isNullOrBlank()) "$sender in $groupTitle" else sender
        }

        val prepared = activeSnapshots.map { snapshot ->
            val resolved = resolveGroupAndContent(snapshot, category)
            val sender = SmartContentCleaner.cleanSender(resolved.sender) ?: resolved.sender
            val message = SmartContentCleaner.cleanMessage(resolved.messageText, packageName).text
            val delayedMins = delayedMinutesByKey[snapshot.key]?.coerceAtLeast(1L)
            PreparedItem(snapshot, sender, resolved.groupTitle, message, delayedMins)
        }

        // Group consecutive same-sender bursts if enabled (#20), or emit flat if disabled
        var idx = 0
        while (idx < prepared.size) {
            val first = prepared[idx]
            if (mergeSenderBursts && first.sender.isNotEmpty()) {
                val run = mutableListOf(first)
                var nextIdx = idx + 1
                while (nextIdx < prepared.size) {
                    val candidate = prepared[nextIdx]
                    val sameSubject = candidate.senderSubject.equals(first.senderSubject, ignoreCase = true)
                    val spanOk = if (first.snapshot.postTimeMillis > 0L && candidate.snapshot.postTimeMillis > 0L) {
                        kotlin.math.abs(candidate.snapshot.postTimeMillis - first.snapshot.postTimeMillis) <= senderBurstWindowMs
                    } else {
                        true
                    }
                    if (sameSubject && spanOk) {
                        run += candidate
                        nextIdx++
                    } else {
                        break
                    }
                }
                if (run.size >= senderBurstMinCount) {
                    val joinedMessages = run.mapNotNull { it.message.takeIf(String::isNotBlank) }.joinToString(", ")
                    if (format.includeSender) {
                        sections += AnnouncementSection(
                            SectionKind.SENDER,
                            if (format.includeMessage && joinedMessages.isNotEmpty()) {
                                "${first.senderSubject} sent ${run.size} quick messages:"
                            } else {
                                "${first.senderSubject} sent ${run.size} quick messages."
                            },
                        )
                    } else if (activeSnapshots.size > 1) {
                        sections += AnnouncementSection(SectionKind.MESSAGE_COUNT, "${run.size} messages.")
                    }
                    if (format.includeMessage && joinedMessages.isNotEmpty()) {
                        sections += AnnouncementSection(SectionKind.MESSAGE, joinedMessages)
                    }
                    idx = nextIdx
                    continue
                }
            }

            if (activeSnapshots.size > 1 && mergeSenderBursts && idx == 0) {
                // Multi-sender grouped list still announces total count when not collapsed to one sender burst
                sections += AnnouncementSection(SectionKind.MESSAGE_COUNT, "${activeSnapshots.size} messages.")
            }

            if (format.includeSender && first.sender.isNotEmpty()) {
                val text = when {
                    first.delayedMins != null && format.includeMessage && first.message.isNotEmpty() ->
                        "${first.senderSubject} sent ${first.delayedMins} minutes ago:"
                    first.delayedMins != null ->
                        "${first.senderSubject} sent ${first.delayedMins} minutes ago."
                    format.includeMessage && first.message.isNotEmpty() ->
                        "${first.senderSubject} says,"
                    else ->
                        "${first.senderSubject}."
                }
                sections += AnnouncementSection(SectionKind.SENDER, text)
            } else if (first.delayedMins != null && format.includeMessage && first.message.isNotEmpty()) {
                sections += AnnouncementSection(SectionKind.SENDER, "Sent ${first.delayedMins} minutes ago:")
            }

            if (format.includeMessage && first.message.isNotEmpty()) {
                sections += AnnouncementSection(SectionKind.MESSAGE, first.message)
            }
            idx++
        }

        return BuiltAnnouncement(sections, format.pauseBetweenSectionsMillis, isCritical = isCritical)
    }

    /**
     * Builds a combined multi-app digest for items arriving within the digest burst window (#33).
     *
     * Example output sections:
     * `"You have 2 notifications:"`, `"Rahul on WhatsApp says, Hey"`, `"Mom on Messages says, Call me back"`.
     */
    fun buildMultiAppDigest(
        snapshots: List<NotificationSnapshot>,
        format: AnnouncementFormat,
        isCritical: Boolean = false,
        delayedMinutesByKey: Map<String, Long> = emptyMap(),
    ): BuiltAnnouncement {
        val active = snapshots.filterNot { SmartContentCleaner.shouldSuppress(it.messageText, it.packageName) }
        if (active.isEmpty()) {
            return BuiltAnnouncement(emptyList(), format.pauseBetweenSectionsMillis, isCritical = isCritical)
        }
        val distinctPackages = active.map { it.packageName }.distinct()
        if (distinctPackages.size <= 1) {
            return build(active, format, isCritical, delayedMinutesByKey)
        }

        val sections = mutableListOf<AnnouncementSection>()
        format.introduction?.trim()?.takeIf { it.isNotEmpty() }?.let {
            sections += AnnouncementSection(SectionKind.INTRODUCTION, it)
        }
        sections += AnnouncementSection(SectionKind.MESSAGE_COUNT, "You have ${active.size} notifications:")

        active.forEach { snapshot ->
            val category = SmartContentCleaner.resolveCategory(snapshot.packageName)
            val resolved = resolveGroupAndContent(snapshot, category)
            val appName = SmartContentCleaner.resolveAppName(snapshot.packageName, snapshot.appName.trim())
            val sender = SmartContentCleaner.cleanSender(resolved.sender) ?: resolved.sender
            val message = SmartContentCleaner.cleanMessage(resolved.messageText, snapshot.packageName).text
            val line = buildString {
                if (format.includeSender && sender.isNotEmpty()) {
                    append(sender)
                    if (format.includeAppName && appName.isNotEmpty()) append(" on $appName")
                    if (format.includeMessage && message.isNotEmpty()) append(" says, $message") else append(".")
                } else {
                    if (format.includeAppName && appName.isNotEmpty()) append("$appName: ")
                    if (format.includeMessage && message.isNotEmpty()) append(message)
                }
            }.trim()
            if (line.isNotEmpty()) {
                sections += AnnouncementSection(SectionKind.MESSAGE, line)
            }
        }
        return BuiltAnnouncement(sections, format.pauseBetweenSectionsMillis, isCritical = isCritical)
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

        val messagingPolishEnabled = SmartContentCleaner.isMessagingPolishEnabled(snapshot.packageName, category)
        if (!messagingPolishEnabled) {
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
