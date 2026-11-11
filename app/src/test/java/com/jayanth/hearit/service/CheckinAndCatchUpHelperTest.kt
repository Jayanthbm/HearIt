package com.jayanth.hearit.service

import org.junit.Assert.assertEquals
import org.junit.Test

class CheckinAndCatchUpHelperTest {

    @Test
    fun formatCatchUpText_groupsSingleMultipleAndDisabledBreakdown() {
        assertEquals(
            "You have 1 unread notification from WhatsApp.",
            CheckinAndCatchUpHelper.formatCatchUpText(1, listOf("WhatsApp"), true),
        )
        assertEquals(
            "You have 3 unread notifications from WhatsApp and Gmail.",
            CheckinAndCatchUpHelper.formatCatchUpText(3, listOf("WhatsApp", "Gmail", "WhatsApp"), true),
        )
        assertEquals(
            "You have 4 unread notifications.",
            CheckinAndCatchUpHelper.formatCatchUpText(4, listOf("WhatsApp", "Gmail"), false),
        )
    }

    @Test
    fun formatCheckinMessage_includesConfiguredFieldsOnly() {
        val full = CheckinAndCatchUpHelper.formatCheckinMessage(
            includeTime = true,
            includePhoneBattery = true,
            includeEarphonesBattery = true,
            includeSummary = true,
            formattedTime = "5:30 PM",
            phoneBatteryPercent = 72,
            earphonesBatteryPercent = 85,
            announcedTodayCount = 12,
            pendingCount = 2,
        )
        assertEquals(
            "It's 5:30 PM. Phone battery is 72 percent. Earphones battery is 85 percent. 12 announcements today, 2 pending.",
            full,
        )

        val partial = CheckinAndCatchUpHelper.formatCheckinMessage(
            includeTime = true,
            includePhoneBattery = false,
            includeEarphonesBattery = false,
            includeSummary = false,
            formattedTime = "9:00 AM",
            phoneBatteryPercent = 90,
            earphonesBatteryPercent = null,
            announcedTodayCount = 0,
            pendingCount = 0,
        )
        assertEquals("It's 9:00 AM.", partial)
    }
}
