package com.jayanth.hearit.core.database

import com.jayanth.hearit.core.common.model.AppPriority
import com.jayanth.hearit.core.common.model.HistorySettings
import com.jayanth.hearit.core.common.model.HistoryStatus
import com.jayanth.hearit.core.common.model.HistoryStorageMode
import com.jayanth.hearit.core.common.model.HistoryUpdateHandling
import com.jayanth.hearit.core.common.model.NotificationSnapshot
import com.jayanth.hearit.core.common.model.SkipReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Recording, retention and query policy (written for P10, not executed in the cloud). */
class HistoryPolicyTest {

    private val recordedAt = 1_700_000_000_000L

    private fun snapshot(
        key: String = "com.whatsapp|t|1",
        packageName: String = "com.whatsapp",
        appName: String = "WhatsApp",
        sender: String? = "Rahul",
        messageText: String = "hi there",
        isUpdate: Boolean = false,
    ) = NotificationSnapshot(
        key = key,
        packageName = packageName,
        appName = appName,
        sender = sender,
        messageText = messageText,
        postTimeMillis = 100L,
        isUpdate = isUpdate,
    )

    private fun fullContent() = HistorySettings(storageMode = HistoryStorageMode.FULL_CONTENT)

    @Test
    fun decide_fullContent_keepsMessageText() {
        val action = HistoryRecorderPolicy.decide(snapshot(), fullContent(), HistoryStatus.ANNOUNCED, null, recordedAt)

        val entry = (action as HistoryRecordAction.Insert).entry
        assertEquals("hi there", entry.messageText)
        assertEquals("Rahul", entry.sender)
        assertEquals("com.whatsapp", entry.packageName)
        assertEquals("WhatsApp", entry.appName)
    }

    @Test
    fun decide_metadataOnly_dropsMessageText_butKeepsSender() {
        val settings = HistorySettings(storageMode = HistoryStorageMode.METADATA_ONLY)

        val action = HistoryRecorderPolicy.decide(snapshot(), settings, HistoryStatus.ANNOUNCED, null, recordedAt)

        val entry = (action as HistoryRecordAction.Insert).entry
        assertNull(entry.messageText)
        assertEquals("Rahul", entry.sender)
        assertEquals("com.whatsapp", entry.packageName)
    }

    @Test
    fun decide_disabled_discardsEverything() {
        val settings = HistorySettings(storageMode = HistoryStorageMode.DISABLED)

        val action = HistoryRecorderPolicy.decide(snapshot(), settings, HistoryStatus.ANNOUNCED, null, recordedAt)

        assertEquals(HistoryRecordAction.Discard, action)
    }

    @Test
    fun decide_blankMessageText_storesNull() {
        val action = HistoryRecorderPolicy.decide(
            snapshot(messageText = "   "),
            fullContent(),
            HistoryStatus.SKIPPED,
            SkipReason.TOO_LONG,
            recordedAt,
        )

        assertNull((action as HistoryRecordAction.Insert).entry.messageText)
    }

    @Test
    fun decide_existingRowWithSameKey_insertsNewEntry() {
        val settings = HistorySettings()

        val action = HistoryRecorderPolicy.decide(
            snapshot(isUpdate = true),
            settings,
            HistoryStatus.ANNOUNCED,
            null,
            recordedAt,
            existingEntryForSameKey = true,
        )

        assertTrue(action is HistoryRecordAction.Insert)
    }

    @Test
    fun decide_withoutExistingRow_inserts() {
        val settings = HistorySettings()

        val action = HistoryRecorderPolicy.decide(
            snapshot(),
            settings,
            HistoryStatus.ANNOUNCED,
            null,
            recordedAt,
            existingEntryForSameKey = false,
        )

        assertTrue(action is HistoryRecordAction.Insert)
    }

    @Test
    fun decide_newEntryHandling_alwaysInserts() {
        val settings = HistorySettings(updateHandling = HistoryUpdateHandling.NEW_ENTRY)

        val action = HistoryRecorderPolicy.decide(
            snapshot(isUpdate = true),
            settings,
            HistoryStatus.ANNOUNCED,
            null,
            recordedAt,
            existingEntryForSameKey = true,
        )

        assertTrue(action is HistoryRecordAction.Insert)
    }

    @Test
    fun decide_queuedTransitionToAnnounced_replacesExistingEvenIfNewEntryMode() {
        val settings = HistorySettings(updateHandling = HistoryUpdateHandling.NEW_ENTRY)

        val action = HistoryRecorderPolicy.decide(
            snapshot(),
            settings,
            HistoryStatus.ANNOUNCED,
            null,
            recordedAt,
            existingEntryForSameKey = true,
            existingEntryStatus = HistoryStatus.QUEUED,
        )

        assertTrue(action is HistoryRecordAction.ReplaceExisting)
        assertEquals(HistoryStatus.ANNOUNCED, (action as HistoryRecordAction.ReplaceExisting).entry.status)
    }

    @Test
    fun decide_queuedTransitionToInterrupted_replacesExisting() {
        val settings = HistorySettings(updateHandling = HistoryUpdateHandling.NEW_ENTRY)

        val action = HistoryRecorderPolicy.decide(
            snapshot(),
            settings,
            HistoryStatus.INTERRUPTED,
            SkipReason.INTERRUPTED_USER,
            recordedAt,
            existingEntryForSameKey = true,
            existingEntryStatus = HistoryStatus.QUEUED,
        )

        assertTrue(action is HistoryRecordAction.ReplaceExisting)
        assertEquals(HistoryStatus.INTERRUPTED, (action as HistoryRecordAction.ReplaceExisting).entry.status)
        assertEquals(SkipReason.INTERRUPTED_USER, (action as HistoryRecordAction.ReplaceExisting).entry.reason)
    }

    @Test
    fun decide_queuedTransitionToSkipped_replacesExisting() {
        val settings = HistorySettings(updateHandling = HistoryUpdateHandling.NEW_ENTRY)

        val action = HistoryRecorderPolicy.decide(
            snapshot(),
            settings,
            HistoryStatus.SKIPPED,
            SkipReason.TOO_LONG,
            recordedAt,
            existingEntryForSameKey = true,
            existingEntryStatus = HistoryStatus.QUEUED,
        )

        assertTrue(action is HistoryRecordAction.ReplaceExisting)
        assertEquals(HistoryStatus.SKIPPED, (action as HistoryRecordAction.ReplaceExisting).entry.status)
    }

    @Test
    fun decide_skippedItem_isRecordedWithItsReason() {
        val action = HistoryRecorderPolicy.decide(
            snapshot(),
            fullContent(),
            HistoryStatus.SKIPPED,
            SkipReason.SCREEN_ON,
            recordedAt,
        )

        val entry = (action as HistoryRecordAction.Insert).entry
        assertEquals(HistoryStatus.SKIPPED, entry.status)
        assertEquals(SkipReason.SCREEN_ON, entry.reason)
    }

    @Test
    fun decide_queuedAndInterruptedItems_areRecorded() {
        val queued = HistoryRecorderPolicy.decide(
            snapshot(),
            fullContent(),
            HistoryStatus.QUEUED,
            null,
            recordedAt,
        )
        val interrupted = HistoryRecorderPolicy.decide(
            snapshot(),
            fullContent(),
            HistoryStatus.INTERRUPTED,
            null,
            recordedAt,
        )

        assertEquals(HistoryStatus.QUEUED, (queued as HistoryRecordAction.Insert).entry.status)
        assertEquals(HistoryStatus.INTERRUPTED, (interrupted as HistoryRecordAction.Insert).entry.status)
    }

    @Test
    fun decide_preservesUpdateFlagAndPriority() {
        val action = HistoryRecorderPolicy.decide(
            snapshot(isUpdate = true),
            fullContent(),
            HistoryStatus.ANNOUNCED,
            null,
            recordedAt,
            priority = AppPriority.HIGH,
        )

        val entry = (action as HistoryRecordAction.Insert).entry
        assertTrue(entry.isUpdate)
        assertEquals(AppPriority.HIGH, entry.priority)
    }

    @Test
    fun decide_usesProvidedRecordTime() {
        val action = HistoryRecorderPolicy.decide(snapshot(), fullContent(), HistoryStatus.ANNOUNCED, null, recordedAt)

        val entry = (action as HistoryRecordAction.Insert).entry
        assertEquals(recordedAt, entry.recordedAtMillis)
        assertEquals(100L, entry.postTimeMillis)
    }

    @Test
    fun retention_never_returnsNull() {
        assertNull(HistoryRetentionPolicy.cutoffMillis(recordedAt, null))
    }

    @Test
    fun retention_zeroDays_cutsOffAtNow() {
        assertEquals(recordedAt, HistoryRetentionPolicy.cutoffMillis(recordedAt, 0))
    }

    @Test
    fun retention_thirtyDays_subtractsThirtyDays() {
        val expected = recordedAt - 30L * HistoryRetentionPolicy.MILLIS_PER_DAY

        assertEquals(expected, HistoryRetentionPolicy.cutoffMillis(recordedAt, 30))
    }

    @Test
    fun retention_everyConfiguredOption() {
        listOf(1, 3, 7, 10, 15, 30, 60, 90).forEach { days ->
            val cutoff = HistoryRetentionPolicy.cutoffMillis(recordedAt, days)
            assertEquals(recordedAt - days.toLong() * HistoryRetentionPolicy.MILLIS_PER_DAY, cutoff)
        }
    }

    @Test
    fun retention_negativeDays_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            HistoryRetentionPolicy.cutoffMillis(recordedAt, -1)
        }
    }

    @Test
    fun query_defaults_areValid() {
        val query = HistoryQuery()

        assertNull(query.packageName)
        assertNull(query.status)
        assertEquals(50, query.limit)
        assertEquals(0, query.offset)
    }

    @Test
    fun query_limitAboveMaximum_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            HistoryQuery(limit = HistoryQuery.MAX_PAGE_SIZE + 1)
        }
    }

    @Test
    fun query_negativeOffset_isRejected() {
        assertThrows(IllegalArgumentException::class.java) { HistoryQuery(offset = -1) }
    }

    @Test
    fun query_blankPackageName_isRejected() {
        assertThrows(IllegalArgumentException::class.java) { HistoryQuery(packageName = " ") }
    }

    @Test
    fun query_blankSearchText_isRejected() {
        assertThrows(IllegalArgumentException::class.java) { HistoryQuery(searchText = "  ") }
    }
}
