package com.jayanth.hearit.core.tts

import com.jayanth.hearit.core.common.model.NotificationSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Announcement-template coverage (master prompt §7, ADR-018): the default format must
 * reproduce "WhatsApp. Rahul says, Hey, are you coming today?" exactly, missing
 * sender/message must never produce dangling grammar, and disabled sections or an empty
 * result must be handled gracefully by construction.
 */
class AnnouncementTemplateTest {

    private fun snapshot(
        sender: String? = "Rahul",
        messageText: String = "Hey, are you coming today?",
        appName: String = "WhatsApp",
    ) = NotificationSnapshot(
        key = "com.whatsapp|tag|1",
        packageName = "com.whatsapp",
        appName = appName,
        sender = sender,
        messageText = messageText,
        postTimeMillis = 1_000L,
    )

    private fun kinds(announcement: BuiltAnnouncement) = announcement.sections.map { it.kind }

    private fun texts(announcement: BuiltAnnouncement) = announcement.sections.map { it.text }

    // --- Default format (master prompt §7 example) ------------------------------------------

    @Test
    fun build_defaultFormat_producesMasterPromptExample() {
        val announcement = AnnouncementTemplate.build(snapshot(), AnnouncementFormat())
        assertEquals(
            listOf("WhatsApp.", "Rahul says,", "Hey, are you coming today?"),
            texts(announcement),
        )
        assertEquals(
            listOf(SectionKind.APP_NAME, SectionKind.SENDER, SectionKind.MESSAGE),
            kinds(announcement),
        )
        assertEquals(0L, announcement.pauseBetweenSectionsMillis)
        assertFalse(announcement.isEmpty)
    }

    @Test
    fun build_pauseBetweenSections_propagates() {
        val format = AnnouncementFormat(pauseBetweenSectionsMillis = 750L)
        assertEquals(750L, AnnouncementTemplate.build(snapshot(), format).pauseBetweenSectionsMillis)
    }

    @Test
    fun build_negativePause_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            AnnouncementFormat(pauseBetweenSectionsMillis = -1L)
        }
    }

    // --- Graceful handling of missing sender / message ---------------------------------------

    @Test
    fun build_missingSender_omitsSenderSection() {
        val announcement = AnnouncementTemplate.build(snapshot(sender = null), AnnouncementFormat())
        assertEquals(listOf(SectionKind.APP_NAME, SectionKind.MESSAGE), kinds(announcement))
        assertEquals("WhatsApp.", texts(announcement).first())
        assertFalse(texts(announcement).any { it.contains("says") })
    }

    @Test
    fun build_blankSender_treatedAsMissing() {
        val announcement = AnnouncementTemplate.build(snapshot(sender = "   "), AnnouncementFormat())
        assertEquals(listOf(SectionKind.APP_NAME, SectionKind.MESSAGE), kinds(announcement))
    }

    @Test
    fun build_missingMessage_omitsMessageSection() {
        val announcement = AnnouncementTemplate.build(snapshot(messageText = ""), AnnouncementFormat())
        assertEquals(listOf(SectionKind.APP_NAME, SectionKind.SENDER), kinds(announcement))
    }

    @Test
    fun build_senderWithoutMessage_isSpokenWithoutConnector() {
        val announcement = AnnouncementTemplate.build(snapshot(messageText = ""), AnnouncementFormat())
        assertEquals("WhatsApp.", texts(announcement)[0])
        assertEquals("Rahul.", texts(announcement)[1])
        assertFalse(texts(announcement).any { it.contains("says") })
    }

    @Test
    fun build_missingSenderAndMessage_announcesAppOnly() {
        val announcement = AnnouncementTemplate.build(
            snapshot(sender = null, messageText = ""),
            AnnouncementFormat(),
        )
        assertEquals(listOf(SectionKind.APP_NAME), kinds(announcement))
        assertEquals("WhatsApp.", texts(announcement).single())
        assertFalse(announcement.isEmpty)
    }

    // --- Configurable sections -----------------------------------------------------------------

    @Test
    fun build_appNameDisabled_skipsAppSection() {
        val format = AnnouncementFormat(includeAppName = false)
        assertEquals(listOf(SectionKind.SENDER, SectionKind.MESSAGE), kinds(AnnouncementTemplate.build(snapshot(), format)))
    }

    @Test
    fun build_senderDisabled_skipsSenderSection() {
        val format = AnnouncementFormat(includeSender = false)
        assertEquals(listOf(SectionKind.APP_NAME, SectionKind.MESSAGE), kinds(AnnouncementTemplate.build(snapshot(), format)))
    }

    @Test
    fun build_messageDisabled_usesPlainSenderWithoutConnector() {
        val format = AnnouncementFormat(includeMessage = false)
        val announcement = AnnouncementTemplate.build(snapshot(), format)
        assertEquals(listOf(SectionKind.APP_NAME, SectionKind.SENDER), kinds(announcement))
        assertEquals("Rahul.", texts(announcement).single { it.startsWith("Rahul") })
    }

    // --- Custom introduction -------------------------------------------------------------------

    @Test
    fun build_introduction_isFirstSection() {
        val format = AnnouncementFormat(introduction = "New message")
        val announcement = AnnouncementTemplate.build(snapshot(), format)
        assertEquals(SectionKind.INTRODUCTION, kinds(announcement).first())
        assertEquals("New message", texts(announcement).first())
        assertEquals(4, announcement.sections.size)
    }

    @Test
    fun build_blankIntroduction_ignored() {
        val format = AnnouncementFormat(introduction = "   ")
        assertEquals(listOf(SectionKind.APP_NAME, SectionKind.SENDER, SectionKind.MESSAGE),
            kinds(AnnouncementTemplate.build(snapshot(), format)))
    }

    @Test
    fun build_introductionOnly_whenAllSectionsDisabled_stillAnnounces() {
        val format = AnnouncementFormat(
            includeAppName = false,
            includeSender = false,
            includeMessage = false,
            introduction = "New message",
        )
        val announcement = AnnouncementTemplate.build(snapshot(), format)
        assertEquals(listOf(SectionKind.INTRODUCTION), kinds(announcement))
        assertFalse(announcement.isEmpty)
    }

    // --- Empty result ----------------------------------------------------------------------------

    @Test
    fun build_everythingDisabledWithoutIntroduction_isEmpty() {
        val format = AnnouncementFormat(
            includeAppName = false,
            includeSender = false,
            includeMessage = false,
        )
        val announcement = AnnouncementTemplate.build(snapshot(), format)
        assertTrue(announcement.isEmpty)
        assertTrue(announcement.sections.isEmpty())
    }

    @Test
    fun build_disabledSectionsWithMissingContent_isEmpty() {
        // All sections enabled but the snapshot has nothing except the app name, and the
        // app-name section is switched off: nothing to speak, no dangling connector.
        val format = AnnouncementFormat(includeAppName = false)
        val announcement = AnnouncementTemplate.build(
            snapshot(sender = null, messageText = ""),
            format,
        )
        assertTrue(announcement.isEmpty)
    }

    // --- Content is never rewritten -------------------------------------------------------------

    @Test
    fun build_messageText_isSpokenVerbatim() {
        val message = "Are you serious?!! Reply ASAP."
        val announcement = AnnouncementTemplate.build(snapshot(messageText = message), AnnouncementFormat())
        assertEquals(message, texts(announcement).last())
    }

    @Test
    fun build_inlineGroupChatFormat_readsSenderInGroupSays() {
        val groupSnap = snapshot(sender = "Family Group", messageText = "Rahul: Dinner at 8?")
        val announcement = AnnouncementTemplate.build(groupSnap, AnnouncementFormat())
        assertEquals(
            listOf("WhatsApp.", "Rahul in Family Group says,", "Dinner at 8?"),
            texts(announcement),
        )
    }

    @Test
    fun build_suppressedMessage_returnsEmptyAnnouncementWithoutAppName() {
        val syncSnap = snapshot(sender = null, messageText = "Checking for new messages...")
        val announcement = AnnouncementTemplate.build(syncSnap, AnnouncementFormat())
        assertTrue(announcement.isEmpty)
        assertTrue(announcement.sections.isEmpty())
    }

    @Test
    fun build_delayedMessage_formatsNaturalSentMinutesAgo() {
        val snap = snapshot(sender = "Rahul", messageText = "Where are you?")
        val announcement = AnnouncementTemplate.build(
            snapshot = snap,
            format = AnnouncementFormat(),
            isCritical = true,
            delayedMinutesByKey = mapOf(snap.key to 12L),
        )
        assertTrue(announcement.isCritical)
        assertEquals(
            listOf("WhatsApp.", "Rahul sent 12 minutes ago:", "Where are you?"),
            texts(announcement),
        )
    }
}
