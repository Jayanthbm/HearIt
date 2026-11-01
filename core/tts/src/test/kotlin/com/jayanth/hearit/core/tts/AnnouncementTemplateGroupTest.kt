package com.jayanth.hearit.core.tts

import com.jayanth.hearit.core.common.model.NotificationSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Grouped-announcement build (master prompt §8: app name + message count, followed by
 * available sender/message content; ADR-018/ADR-019). Not locally verified — written
 * under the cloud-agent restriction (no build/test run).
 */
class AnnouncementTemplateGroupTest {

    @Test
    fun build_threeMessages_announcesAppCountThenContentInOrder() {
        val announcement = AnnouncementTemplate.build(
            listOf(
                snapshot("a", sender = "Rahul", text = "Hey"),
                snapshot("b", sender = "Priya", text = "Hi"),
                snapshot("c", sender = "Amit", text = "On my way"),
            ),
            AnnouncementFormat(),
        )

        assertEquals(
            listOf(
                SectionKind.APP_NAME,
                SectionKind.MESSAGE_COUNT,
                SectionKind.SENDER,
                SectionKind.MESSAGE,
                SectionKind.SENDER,
                SectionKind.MESSAGE,
                SectionKind.SENDER,
                SectionKind.MESSAGE,
            ),
            announcement.sections.map { it.kind },
        )
        assertEquals(
            listOf(
                "WhatsApp.",
                "3 messages.",
                "Rahul says,",
                "Hey",
                "Priya says,",
                "Hi",
                "Amit says,",
                "On my way",
            ),
            announcement.sections.map { it.text },
        )
    }

    @Test
    fun build_singleSnapshotInList_producesExactlyTheSingleSections() {
        val single = snapshot("a", sender = "Rahul", text = "Hey")

        assertEquals(
            AnnouncementTemplate.build(single, AnnouncementFormat()),
            AnnouncementTemplate.build(listOf(single), AnnouncementFormat()),
        )
    }

    @Test
    fun build_groupWithMissingSenders_omitsSectionsCleanly() {
        val announcement = AnnouncementTemplate.build(
            listOf(
                snapshot("a", sender = "Rahul", text = "Hey"),
                snapshot("b", sender = null, text = "On my way"),
            ),
            AnnouncementFormat(),
        )

        assertEquals(
            listOf(
                SectionKind.APP_NAME,
                SectionKind.MESSAGE_COUNT,
                SectionKind.SENDER,
                SectionKind.MESSAGE,
                SectionKind.MESSAGE,
            ),
            announcement.sections.map { it.kind },
        )
        assertEquals("2 messages.", announcement.sections[1].text)
    }

    @Test
    fun build_groupWithMessagesDisabled_announcesCountAndPlainSenders() {
        val announcement = AnnouncementTemplate.build(
            listOf(
                snapshot("a", sender = "Rahul", text = "Hey"),
                snapshot("b", sender = "Priya", text = "Hi"),
            ),
            AnnouncementFormat(includeMessage = false),
        )

        assertEquals(
            listOf(
                SectionKind.APP_NAME,
                SectionKind.MESSAGE_COUNT,
                SectionKind.SENDER,
                SectionKind.SENDER,
            ),
            announcement.sections.map { it.kind },
        )
        // No message available: plain "Rahul." — never a dangling "says,".
        assertEquals(listOf("Rahul.", "Priya."), announcement.sections.drop(2).map { it.text })
    }

    @Test
    fun build_groupWithBlankContent_stillAnnouncesAppAndCount() {
        val announcement = AnnouncementTemplate.build(
            listOf(
                snapshot("a", sender = null, text = ""),
                snapshot("b", sender = null, text = ""),
            ),
            AnnouncementFormat(),
        )

        assertFalse(announcement.isEmpty)
        assertEquals(
            listOf(SectionKind.APP_NAME, SectionKind.MESSAGE_COUNT),
            announcement.sections.map { it.kind },
        )
        assertEquals("2 messages.", announcement.sections[1].text)
    }

    @Test
    fun build_emptyList_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            AnnouncementTemplate.build(emptyList(), AnnouncementFormat())
        }
    }

    @Test
    fun build_mixedPackages_areRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            AnnouncementTemplate.build(
                listOf(
                    snapshot("a", packageName = "com.whatsapp"),
                    snapshot("b", packageName = "com.telegram"),
                ),
                AnnouncementFormat(),
            )
        }
    }

    private fun snapshot(
        key: String,
        packageName: String = "com.whatsapp",
        sender: String? = null,
        text: String = "hi",
    ): NotificationSnapshot = NotificationSnapshot(
        key = key,
        packageName = packageName,
        appName = "WhatsApp",
        sender = sender,
        messageText = text,
        postTimeMillis = 0L,
        isUpdate = false,
    )
}
