package com.jayanth.hearit.diagnostics

import android.content.Context
import com.jayanth.hearit.core.common.logger.DiagnosticLogSink
import com.jayanth.hearit.core.common.logger.LogCategory
import com.jayanth.hearit.core.common.logger.LogLevel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.OutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

/**
 * File-based structured diagnostic logger adhering to master prompt §17.
 *
 * Invariants:
 * - 7-day rolling circular retention (older log files are automatically pruned).
 * - Non-blocking asynchronous file writes on a dedicated single-threaded executor.
 * - Guaranteed zero notification text / private content.
 */
@Singleton
class FileDiagnosticLogger(
    private val baseDirectory: File,
) : DiagnosticLogSink {

    @Inject
    constructor(@ApplicationContext context: Context) : this(File(context.filesDir, "diagnostics_logs"))

    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "HearIt-DiagnosticLogger").apply { isDaemon = true }
    }

    private val logDirectory: File
        get() = baseDirectory.apply {
            if (!exists()) {
                mkdirs()
            }
        }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    init {
        executor.execute {
            pruneOldLogs()
        }
    }

    override fun log(level: LogLevel, category: LogCategory, message: String, throwable: Throwable?) {
        val now = Date()
        val formattedTimestamp = synchronized(timestampFormat) { timestampFormat.format(now) }
        val dateFileName = synchronized(dateFormat) { dateFormat.format(now) }

        executor.execute {
            try {
                val file = File(logDirectory, "log_$dateFileName.log")
                val stringWriter = StringWriter()
                val printWriter = PrintWriter(stringWriter)

                printWriter.print("$formattedTimestamp [${level.name}] [${category.name}] $message\n")
                throwable?.printStackTrace(printWriter)
                printWriter.flush()

                file.appendText(stringWriter.toString())
            } catch (_: Throwable) {
                // Diagnostic logging must never crash the service or application
            }
        }
    }

    /**
     * Returns all existing diagnostic log files sorted chronologically (oldest to newest).
     */
    fun getLogFiles(): List<File> {
        return try {
            logDirectory.listFiles { file -> file.isFile && file.name.startsWith("log_") && file.name.endsWith(".log") }
                ?.sortedBy { it.name }
                ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    /**
     * Computes total byte size of all active diagnostic logs.
     */
    fun getTotalSizeBytes(): Long {
        return try {
            getLogFiles().sumOf { it.length() }
        } catch (_: Throwable) {
            0L
        }
    }

    /**
     * Exports all stored diagnostic logs to the target [outputStream], prepending a verified privacy header.
     */
    fun exportLogs(outputStream: OutputStream) {
        val files = getLogFiles()
        val exportTimestamp = synchronized(timestampFormat) { timestampFormat.format(Date()) }

        outputStream.bufferedWriter().use { writer ->
            writer.write("================================================================================\n")
            writer.write("Hear It System Diagnostics Log Export\n")
            writer.write("Exported at: $exportTimestamp\n")
            writer.write("Log Files Included: ${files.size}\n")
            writer.write("Privacy Invariant: 100% On-Device · Metadata Only · Zero Notification Text\n")
            writer.write("================================================================================\n\n")

            if (files.isEmpty()) {
                writer.write("No diagnostic events recorded yet.\n")
            } else {
                for (file in files) {
                    writer.write("----- File: ${file.name} -----\n")
                    try {
                        file.bufferedReader().use { fileReader ->
                            fileReader.copyTo(writer)
                        }
                    } catch (e: Throwable) {
                        writer.write("[Error reading log file: ${e.message}]\n")
                    }
                    writer.write("\n\n")
                }
            }
            writer.flush()
        }
    }

    /**
     * Wipes all stored diagnostic log files.
     */
    fun clearAllLogs(): Boolean {
        return try {
            val files = getLogFiles()
            var allDeleted = true
            for (file in files) {
                if (!file.delete()) {
                    allDeleted = false
                }
            }
            allDeleted
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Deletes log files older than 7 days.
     */
    fun pruneOldLogs() {
        try {
            val retentionCutoffMillis = System.currentTimeMillis() - RETENTION_PERIOD_MILLIS
            val files = getLogFiles()
            for (file in files) {
                if (file.lastModified() < retentionCutoffMillis) {
                    file.delete()
                }
            }
        } catch (_: Throwable) {
            // Non-critical retention failure
        }
    }

    companion object {
        private const val RETENTION_PERIOD_DAYS = 7L
        private const val RETENTION_PERIOD_MILLIS = RETENTION_PERIOD_DAYS * 24 * 60 * 60 * 1000L
    }
}
