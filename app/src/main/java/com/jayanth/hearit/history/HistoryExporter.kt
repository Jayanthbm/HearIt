package com.jayanth.hearit.history

import com.jayanth.hearit.core.database.HistoryEntry
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Pure JVM helpers for History date range filtering (#9) and CSV/TXT export (#10).
 */
object HistoryExporter {

    private val isoTimestampFormat: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

    /**
     * Resolves inclusive `(fromMillis, toMillis)` bounds for [preset] in [timeZone].
     */
    fun dateBoundsForPreset(
        preset: HistoryDatePreset,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault(),
    ): Pair<Long?, Long?> {
        if (preset == HistoryDatePreset.ALL) return null to null
        if (preset == HistoryDatePreset.CUSTOM) return null to null

        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = cal.timeInMillis
        val dayMillis = 86_400_000L
        return when (preset) {
            HistoryDatePreset.TODAY -> todayStart to (todayStart + dayMillis - 1L)
            HistoryDatePreset.YESTERDAY -> (todayStart - dayMillis) to (todayStart - 1L)
            HistoryDatePreset.LAST_7_DAYS -> (todayStart - 6L * dayMillis) to (todayStart + dayMillis - 1L)
            HistoryDatePreset.ALL, HistoryDatePreset.CUSTOM -> null to null
        }
    }

    /**
     * Converts UTC picker midnight millis from Material 3 `DateRangePicker` into local inclusive day bounds.
     */
    fun normalizePickerRange(
        startUtcMillis: Long,
        endUtcMillis: Long,
        localTimeZone: TimeZone = TimeZone.getDefault(),
    ): Pair<Long, Long> {
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = minOf(startUtcMillis, endUtcMillis)
        }
        val startLocal = Calendar.getInstance(localTimeZone).apply {
            set(
                utcCal.get(Calendar.YEAR),
                utcCal.get(Calendar.MONTH),
                utcCal.get(Calendar.DAY_OF_MONTH),
                0,
                0,
                0,
            )
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endUtcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = maxOf(startUtcMillis, endUtcMillis)
        }
        val endLocal = Calendar.getInstance(localTimeZone).apply {
            set(
                endUtcCal.get(Calendar.YEAR),
                endUtcCal.get(Calendar.MONTH),
                endUtcCal.get(Calendar.DAY_OF_MONTH),
                23,
                59,
                59,
            )
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        return startLocal to endLocal
    }

    /**
     * Formats [entries] as RFC-4180 CSV with stable column headers.
     */
    fun formatCsv(entries: List<HistoryEntry>): String {
        val formatter = isoTimestampFormat
        val sb = StringBuilder()
        sb.appendLine("id,timestamp_utc,app_name,package_name,status,skip_reason,sender,message_text")
        for (entry in entries) {
            sb.append(entry.id).append(',')
            sb.append(csvEscape(formatter.format(Date(entry.recordedAtMillis)))).append(',')
            sb.append(csvEscape(entry.appName)).append(',')
            sb.append(csvEscape(entry.packageName)).append(',')
            sb.append(csvEscape(entry.status.name)).append(',')
            sb.append(csvEscape(entry.reason?.name ?: "")).append(',')
            sb.append(csvEscape(entry.sender ?: "")).append(',')
            sb.append(csvEscape(entry.messageText ?: "(Redacted)")).append('\n')
        }
        return sb.toString()
    }

    /**
     * Formats [entries] as readable plain text logs.
     */
    fun formatTxt(entries: List<HistoryEntry>): String {
        val formatter = isoTimestampFormat
        val sb = StringBuilder()
        sb.appendLine("HearIt Notification History Export (${entries.size} entries)")
        sb.appendLine("=".repeat(56))
        for (entry in entries) {
            val ts = formatter.format(Date(entry.recordedAtMillis))
            sb.append('[').append(ts).append("] ")
            sb.append(entry.appName).append(" (").append(entry.packageName).append(") — ")
            sb.append(entry.status.name)
            entry.reason?.let { sb.append(" [").append(it.name).append(']') }
            sb.append('\n')
            entry.sender?.let { sb.append("  Sender: ").append(it).append('\n') }
            sb.append("  Message: ").append(entry.messageText ?: "(Redacted)").append("\n\n")
        }
        return sb.toString()
    }

    private fun csvEscape(raw: String): String {
        val needsQuotes = raw.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        return if (!needsQuotes) raw else "\"${raw.replace("\"", "\"\"")}\""
    }
}
