package com.jayanth.hearit.core.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Extraction rules from `NotificationExtractor` (written for P07, not executed in the cloud). */
class NotificationExtractorTest {

    private val nowMillis = 1_700_000_000_000L

    private fun extractor() = NotificationExtractor()

    private fun raw(
        packageName: String = "com.whatsapp",
        appLabel: String? = "WhatsApp",
        tag: String? = "tag-1",
        id: Int = 42,
        title: String? = "Rahul",
        text: String? = "see you at 8",
        bigText: String? = null,
        subText: String? = null,
        channelId: String? = "messages",
        channelIsSilent: Boolean = false,
        category: String? = "msg",
        postTimeMillis: Long = 1_000L,
    ) = RawNotification(
        packageName = packageName,
        appLabel = appLabel,
        tag = tag,
        id = id,
        title = title,
        text = text,
        bigText = bigText,
        subText = subText,
        channelId = channelId,
        channelIsSilent = channelIsSilent,
        category = category,
        postTimeMillis = postTimeMillis,
    )

    @Test
    fun extract_fullNotification_mapsEveryField() {
        val snapshot = extractor().extract(raw(), nowMillis)

        assertEquals("com.whatsapp|tag-1|42", snapshot.key)
        assertEquals("com.whatsapp", snapshot.packageName)
        assertEquals("WhatsApp", snapshot.appName)
        assertEquals("Rahul", snapshot.sender)
        assertEquals("see you at 8", snapshot.messageText)
        assertEquals("messages", snapshot.channelId)
        assertFalse(snapshot.isSilentChannel)
        assertEquals("msg", snapshot.category)
        assertEquals(1_000L, snapshot.postTimeMillis)
        assertFalse(snapshot.isUpdate)
    }

    @Test
    fun extract_missingTitle_senderIsNull() {
        val snapshot = extractor().extract(raw(title = null), nowMillis)

        assertNull(snapshot.sender)
        assertEquals("see you at 8", snapshot.messageText)
    }

    @Test
    fun extract_blankTitle_senderIsNull() {
        val snapshot = extractor().extract(raw(title = "   "), nowMillis)

        assertNull(snapshot.sender)
    }

    @Test
    fun extract_missingText_messageTextIsEmpty() {
        val snapshot = extractor().extract(raw(text = null), nowMillis)

        assertEquals("", snapshot.messageText)
        assertEquals("Rahul", snapshot.sender)
    }

    @Test
    fun extract_missingEverything_producesValidSnapshot() {
        val snapshot = extractor().extract(
            raw(title = null, text = null, bigText = null, subText = null, channelId = null, category = null),
            nowMillis,
        )

        assertNull(snapshot.sender)
        assertEquals("", snapshot.messageText)
        assertNull(snapshot.channelId)
        assertNull(snapshot.category)
    }

    @Test
    fun extract_bigTextIsPreferredOverText() {
        val snapshot = extractor().extract(
            raw(text = "short", bigText = "the full expanded message"),
            nowMillis,
        )

        assertEquals("the full expanded message", snapshot.messageText)
    }

    @Test
    fun extract_blankBigText_fallsBackToText() {
        val snapshot = extractor().extract(raw(text = "short", bigText = "   "), nowMillis)

        assertEquals("short", snapshot.messageText)
    }

    @Test
    fun extract_titleEqualToAppName_suppressesSender() {
        val snapshot = extractor().extract(raw(title = "WhatsApp"), nowMillis)

        assertNull(snapshot.sender)
    }

    @Test
    fun extract_titleDifferingOnlyInCaseFromAppName_suppressesSender() {
        val snapshot = extractor().extract(raw(title = "WHATSAPP"), nowMillis)

        assertNull(snapshot.sender)
    }

    @Test
    fun extract_titleAndTextAreTrimmed() {
        val snapshot = extractor().extract(raw(title = "  Rahul  ", text = "  hello  "), nowMillis)

        assertEquals("Rahul", snapshot.sender)
        assertEquals("hello", snapshot.messageText)
    }

    @Test
    fun extract_blankAppLabel_fallsBackToPackageName() {
        val snapshot = extractor().extract(raw(appLabel = "  "), nowMillis)

        assertEquals("com.whatsapp", snapshot.appName)
    }

    @Test
    fun extract_missingAppLabel_fallsBackToPackageName() {
        val snapshot = extractor().extract(raw(appLabel = null), nowMillis)

        assertEquals("com.whatsapp", snapshot.appName)
    }

    @Test
    fun extract_missingPostTime_fallsBackToNow() {
        val snapshot = extractor().extract(raw(postTimeMillis = 0L), nowMillis)

        assertEquals(nowMillis, snapshot.postTimeMillis)
    }

    @Test
    fun extract_taglessKey_hasEmptyTagSegment() {
        val snapshot = extractor().extract(raw(tag = null, id = 7), nowMillis)

        assertEquals("com.whatsapp||7", snapshot.key)
    }

    @Test
    fun extract_silentChannelFlag_passesThrough() {
        val snapshot = extractor().extract(raw(channelIsSilent = true), nowMillis)

        assertTrue(snapshot.isSilentChannel)
    }

    @Test
    fun extract_secondPostOfSameKey_isUpdate() {
        val extractor = extractor()
        val first = extractor.extract(raw(postTimeMillis = 1_000L), nowMillis)
        val second = extractor.extract(raw(text = "edited", postTimeMillis = 2_000L), nowMillis)

        assertFalse(first.isUpdate)
        assertTrue(second.isUpdate)
        assertEquals(first.key, second.key)
    }

    @Test
    fun extract_thirdPostOfSameKey_isStillUpdate() {
        val extractor = extractor()
        extractor.extract(raw(postTimeMillis = 1_000L), nowMillis)
        extractor.extract(raw(postTimeMillis = 2_000L), nowMillis)
        val third = extractor.extract(raw(postTimeMillis = 3_000L), nowMillis)

        assertTrue(third.isUpdate)
    }

    @Test
    fun extract_differentIds_areNotUpdates() {
        val extractor = extractor()
        extractor.extract(raw(id = 1), nowMillis)
        val other = extractor.extract(raw(id = 2), nowMillis)

        assertFalse(other.isUpdate)
    }

    @Test
    fun extract_sameIdDifferentTag_isNotAnUpdate() {
        val extractor = extractor()
        extractor.extract(raw(tag = "a"), nowMillis)
        val other = extractor.extract(raw(tag = "b"), nowMillis)

        assertFalse(other.isUpdate)
    }

    @Test
    fun extract_afterForget_isNewAgain() {
        val extractor = extractor()
        val first = extractor.extract(raw(), nowMillis)
        extractor.forget(first.key)
        val reposted = extractor.extract(raw(), nowMillis)

        assertFalse(reposted.isUpdate)
    }

    @Test
    fun extract_tracksAtMostTheTrackerCeiling() {
        val extractor = NotificationExtractor(
            NotificationKeyTracker(maxEntries = 2),
        )
        extractor.extract(raw(id = 1), nowMillis)
        extractor.extract(raw(id = 2), nowMillis)
        extractor.extract(raw(id = 3), nowMillis)

        assertEquals(2, extractor.trackedKeyCount)
    }

    @Test
    fun keyOf_partsBuildTheStableIdentity() {
        assertEquals("com.app|tag|42", NotificationExtractor.keyOf("com.app", "tag", 42))
        assertEquals("com.app||42", NotificationExtractor.keyOf("com.app", null, 42))
    }

    @Test
    fun extract_prefersMessagingStyleSenderAndText() {
        val raw = raw().copy(
            title = "Group Name",
            text = "2 new messages",
            messagingSender = "Deepak",
            messagingText = "I will reach in 5 minutes",
            isGroupConversation = true,
            conversationTitle = "College Friends",
        )
        val snapshot = extractor().extract(raw, nowMillis)

        assertEquals("Deepak", snapshot.sender)
        assertEquals("I will reach in 5 minutes", snapshot.messageText)
        assertTrue(snapshot.isGroup)
        assertEquals("College Friends", snapshot.groupTitle)
    }
}
