package com.jayanth.hearit.history

import com.jayanth.hearit.core.common.model.HistoryStatus
import com.jayanth.hearit.core.common.model.SkipReason
import com.jayanth.hearit.core.database.HistoryEntry
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryExporterTest {

    private fun sampleEntry(
        id: Long = 1L,
        sender: String? = "Rahul, \"Team\"",
        message: String? = "Line 1\nLine 2",
        status: HistoryStatus = HistoryStatus.ANNOUNCED,
        reason: SkipReason? = null,
    ) = HistoryEntry(
        id = id,
        key = "key-$id",
        packageName = "com.whatsapp",
        appName = "WhatsApp",
        sender = sender,
        messageText = message,
        postTimeMillis = 1_700_000_000_000L,
        recordedAtMillis = 1_700_000_000_000L,
        status = status,
        reason = reason,
    )

    @Test
    fun formatCsv_escapesQuotesCommasAndNewlines_andPreservesRedactedNulls() {
        val csv = HistoryExporter.formatCsv(
            listOf(
                sampleEntry(id = 1L),
                sampleEntry(id = 2L, sender = null, message = null, status = HistoryStatus.SKIPPED, reason = SkipReason.LOW_BATTERY),
            ),
        )
        assertTrue(csv.startsWith("id,timestamp_utc,app_name,package_name,status,skip_reason,sender,message_text"))
        assertTrue(csv.contains("\"Rahul, \"\"Team\"\"\""))
        assertTrue(csv.contains("\"Line 1\nLine 2\""))
        assertTrue(csv.contains("SKIPPED,LOW_BATTERY,,(Redacted)"))
    }

    @Test
    fun formatTxt_containsReadableLines() {
        val txt = HistoryExporter.formatTxt(listOf(sampleEntry(message = "Hello there")))
        assertTrue(txt.contains("HearIt Notification History Export (1 entries)"))
        assertTrue(txt.contains("WhatsApp (com.whatsapp) — ANNOUNCED"))
        assertTrue(txt.contains("Message: Hello there"))
    }

    @Test
    fun dateBoundsForPreset_allReturnsNullBounds_andTodaySpansExactly24Hours() {
        val utc = TimeZone.getTimeZone("UTC")
        val all = HistoryExporter.dateBoundsForPreset(HistoryDatePreset.ALL, nowMillis = 1_700_050_000_000L, timeZone = utc)
        assertNull(all.first)
        assertNull(all.second)

        val today = HistoryExporter.dateBoundsForPreset(HistoryDatePreset.TODAY, nowMillis = 1_700_050_000_000L, timeZone = utc)
        assertEquals(86_399_999L, today.second!! - today.first!!)
    }
}
