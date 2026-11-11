package com.jayanth.hearit.history

import com.jayanth.hearit.core.common.model.HistoryStatus
import com.jayanth.hearit.core.common.model.SkipReason
import com.jayanth.hearit.core.database.HistoryEntry
import com.jayanth.hearit.core.database.HistoryQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * P18: the History screen must page and filter through `HistoryQuery` without building SQL.
 *
 * These tests pin the mapping only — the SQL itself is covered by the DAO contract and the
 * recording policies in `:core:database`.
 */
class HistoryQueryMapperTest {

    private fun entry(
        id: Long = 1L,
        status: HistoryStatus = HistoryStatus.ANNOUNCED,
        reason: SkipReason? = null,
        sender: String? = "Rahul",
        messageText: String? = "hi there",
        isUpdate: Boolean = false,
    ) = HistoryEntry(
        id = id,
        key = "com.whatsapp|t|$id",
        packageName = "com.whatsapp",
        appName = "WhatsApp",
        sender = sender,
        messageText = messageText,
        postTimeMillis = 100L,
        recordedAtMillis = 200L,
        status = status,
        reason = reason,
        isUpdate = isUpdate,
    )

    @Test
    fun toQuery_blankSearch_sendsNoTextFilterAndStartsAtZero() {
        val query = HistoryQueryMapper.toQuery(HistoryFilter(query = "   "))

        assertNull(query.searchText)
        assertEquals(0, query.offset)
    }

    @Test
    fun toQuery_trimsTheSearchText() {
        val query = HistoryQueryMapper.toQuery(HistoryFilter(query = "  Rahul  "))

        assertEquals("Rahul", query.searchText)
    }

    @Test
    fun toQuery_passesStatusAndOffsetThrough() {
        val query = HistoryQueryMapper.toQuery(
            HistoryFilter(status = HistoryStatus.SKIPPED),
            offset = 100,
        )

        assertEquals(HistoryStatus.SKIPPED, query.status)
        assertEquals(100, query.offset)
    }

    @Test
    fun toQuery_usesTheFeaturePageSize() {
        val query = HistoryQueryMapper.toQuery(HistoryFilter())

        assertEquals(HistoryQueryMapper.PAGE_SIZE, query.limit)
    }

    @Test
    fun toQuery_mapsEverySortOrder() {
        assertEquals(
            HistoryQuery.SortBy.RecentFirst,
            HistoryQueryMapper.toQuery(HistoryFilter(sort = HistorySortOrder.RecentFirst)).sortBy,
        )
        assertEquals(
            HistoryQuery.SortBy.OldestFirst,
            HistoryQueryMapper.toQuery(HistoryFilter(sort = HistorySortOrder.OldestFirst)).sortBy,
        )
        assertEquals(
            HistoryQuery.SortBy.ByApp,
            HistoryQueryMapper.toQuery(HistoryFilter(sort = HistorySortOrder.ByApp)).sortBy,
        )
    }

    @Test
    fun toItems_mapsEveryDisplayedField() {
        val item = HistoryQueryMapper.toItems(
            listOf(entry(status = HistoryStatus.SKIPPED, reason = SkipReason.SCREEN_ON, isUpdate = true)),
        ).single()

        assertEquals(1L, item.id)
        assertEquals("WhatsApp", item.appName)
        assertEquals("com.whatsapp", item.packageName)
        assertEquals(HistoryStatus.SKIPPED, item.status)
        assertEquals(SkipReason.SCREEN_ON, item.reason)
        assertEquals("Rahul", item.sender)
        assertEquals("hi there", item.messageText)
        assertEquals(200L, item.recordedAtMillis)
        assertEquals(true, item.isUpdate)
    }

    @Test
    fun toItems_keepsMetadataOnlyRowsWithoutContent() {
        val item = HistoryQueryMapper.toItems(listOf(entry(messageText = null))).single()

        assertNull(item.messageText)
        assertEquals("Rahul", item.sender)
    }

    @Test
    fun toItems_preservesRowOrder() {
        val items = HistoryQueryMapper.toItems(listOf(entry(id = 3), entry(id = 1), entry(id = 2)))

        assertEquals(listOf(3L, 1L, 2L), items.map { it.id })
    }
}
