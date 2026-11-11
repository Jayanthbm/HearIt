package com.jayanth.hearit.core.database

import com.jayanth.hearit.core.common.model.AppPriority
import com.jayanth.hearit.core.common.model.HistoryStatus
import com.jayanth.hearit.core.common.model.SkipReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Enum-name mapping and entity round-trips (written for P10, not executed in the cloud). */
class HistoryEntryMapperTest {

    private fun entry(
        messageText: String? = "hi there",
        status: HistoryStatus = HistoryStatus.ANNOUNCED,
    ) = HistoryEntry(
        id = 7L,
        key = "com.whatsapp|t|1",
        packageName = "com.whatsapp",
        appName = "WhatsApp",
        sender = "Rahul",
        messageText = messageText,
        channelId = "messages",
        category = "msg",
        postTimeMillis = 100L,
        recordedAtMillis = 200L,
        status = status,
        reason = SkipReason.DND,
        isUpdate = true,
        priority = AppPriority.HIGH,
    )

    @Test
    fun statusRoundTrips_forEveryValue() {
        HistoryStatus.entries.forEach { status ->
            assertEquals(status, HistoryEntryMapper.statusFromName(HistoryEntryMapper.statusToName(status)))
        }
    }

    @Test
    fun unknownStatus_fallsBackToSkipped() {
        assertEquals(HistoryStatus.SKIPPED, HistoryEntryMapper.statusFromName("FUTURE_STATUS"))
    }

    @Test
    fun reasonRoundTrips_forEveryValue() {
        SkipReason.entries.forEach { reason ->
            assertEquals(reason, HistoryEntryMapper.reasonFromName(HistoryEntryMapper.reasonToName(reason)))
        }
    }

    @Test
    fun unknownReason_isNull() {
        assertNull(HistoryEntryMapper.reasonFromName("FUTURE_REASON"))
        assertNull(HistoryEntryMapper.reasonFromName(null))
    }

    @Test
    fun priorityRoundTrips_forEveryValue() {
        AppPriority.entries.forEach { priority ->
            assertEquals(priority, HistoryEntryMapper.priorityFromName(HistoryEntryMapper.priorityToName(priority)))
        }
        assertNull(HistoryEntryMapper.priorityFromName(null))
    }

    @Test
    fun entityRoundTrip_preservesEveryField() {
        val original = entry()

        val roundTripped = original.toEntity().toDomain()

        assertEquals(original, roundTripped)
    }

    @Test
    fun metadataOnlyEntry_roundTripsWithNullMessage() {
        val original = entry(messageText = null)

        val roundTripped = original.toEntity().toDomain()

        assertNull(roundTripped.messageText)
        assertEquals(original, roundTripped)
    }

    @Test
    fun entityWithUnknownStoredStatus_decodesTolerantly() {
        val legacy = entry().toEntity().copy(status = "STATUS_FROM_A_NEWER_BUILD", reason = "MYSTERY")

        val domain = legacy.toDomain()

        assertEquals(HistoryStatus.SKIPPED, domain.status)
        assertNull(domain.reason)
    }
}
