package com.jayanth.hearit.service

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import com.jayanth.hearit.core.tts.AnnouncementSection
import com.jayanth.hearit.core.tts.BuiltAnnouncement
import com.jayanth.hearit.core.tts.SectionKind
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pure formatting and platform helpers for Periodic Spoken Check-in (#32) and
 * Catch-Up Summary on Headphone Connect (#22).
 */
object CheckinAndCatchUpHelper {

    fun formatCatchUpText(
        count: Int,
        appNames: List<String>,
        includeAppBreakdown: Boolean,
    ): String {
        val noun = if (count == 1) "unread notification" else "unread notifications"
        if (!includeAppBreakdown || appNames.isEmpty()) {
            return "You have $count $noun."
        }
        val cleanApps = appNames.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val appSummary = when {
            cleanApps.isEmpty() -> ""
            cleanApps.size == 1 -> " from ${cleanApps[0]}"
            cleanApps.size == 2 -> " from ${cleanApps[0]} and ${cleanApps[1]}"
            else -> {
                val head = cleanApps.take(3).joinToString(", ")
                val remaining = cleanApps.size - 3
                if (remaining > 0) " from $head, and $remaining more" else " from ${cleanApps.dropLast(1).joinToString(", ")}, and ${cleanApps.last()}"
            }
        }
        return "You have $count $noun$appSummary."
    }

    fun buildCatchUpAnnouncement(
        count: Int,
        appNames: List<String>,
        includeAppBreakdown: Boolean,
    ): BuiltAnnouncement = BuiltAnnouncement(
        sections = listOf(
            AnnouncementSection(SectionKind.INTRODUCTION, "Connected."),
            AnnouncementSection(SectionKind.MESSAGE, formatCatchUpText(count, appNames, includeAppBreakdown)),
        ),
        pauseBetweenSectionsMillis = 200L,
    )

    fun formatCheckinMessage(
        includeTime: Boolean,
        includePhoneBattery: Boolean,
        includeEarphonesBattery: Boolean,
        includeSummary: Boolean,
        formattedTime: String,
        phoneBatteryPercent: Int,
        earphonesBatteryPercent: Int?,
        announcedTodayCount: Int,
        pendingCount: Int,
    ): String {
        val parts = mutableListOf<String>()
        if (includeTime) {
            parts += "It's $formattedTime"
        }
        if (includePhoneBattery) {
            parts += "Phone battery is $phoneBatteryPercent percent"
        }
        if (includeEarphonesBattery && earphonesBatteryPercent != null) {
            parts += "Earphones battery is $earphonesBatteryPercent percent"
        }
        if (includeSummary) {
            val todayLabel = if (announcedTodayCount == 1) "1 announcement today" else "$announcedTodayCount announcements today"
            val pendingLabel = if (pendingCount > 0) ", $pendingCount pending" else ""
            parts += "$todayLabel$pendingLabel"
        }
        return if (parts.isEmpty()) "" else parts.joinToString(". ") + "."
    }

    fun currentFormattedTime(nowMillis: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(nowMillis))

    /** Best-effort Bluetooth headset battery query via standard Android BluetoothDevice.getBatteryLevel reflection. */
    @SuppressLint("MissingPermission")
    fun readConnectedEarphonesBatteryPercent(context: Context): Int? {
        return runCatching {
            val btManager = context.getSystemService(BluetoothManager::class.java) ?: return null
            val connectedDevices = buildList {
                runCatching { addAll(btManager.getConnectedDevices(BluetoothProfile.HEADSET)) }
                runCatching { addAll(btManager.getConnectedDevices(BluetoothProfile.A2DP)) }
            }.distinctBy { it.address }

            val getBatteryMethod = android.bluetooth.BluetoothDevice::class.java.getMethod("getBatteryLevel")
            connectedDevices.firstNotNullOfOrNull { device ->
                val level = (getBatteryMethod.invoke(device) as? Int) ?: -1
                level.takeIf { it in 0..100 }
            }
        }.getOrNull()
    }
}
