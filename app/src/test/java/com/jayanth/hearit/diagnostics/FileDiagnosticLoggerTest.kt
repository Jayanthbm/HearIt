package com.jayanth.hearit.diagnostics

import com.jayanth.hearit.core.common.logger.LogCategory
import com.jayanth.hearit.core.common.logger.LogLevel
import java.io.ByteArrayOutputStream
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FileDiagnosticLoggerTest {

    private lateinit var tempDir: File
    private lateinit var logger: FileDiagnosticLogger

    @Before
    fun setUp() {
        tempDir = File.createTempFile("diagnostics_test", "").apply {
            delete()
            mkdirs()
        }
        logger = FileDiagnosticLogger(tempDir)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun logAndExport_createsFileAndWritesHeader() {
        logger.log(LogLevel.INFO, LogCategory.SERVICE, "Service started successfully")

        // Wait brief moment for background executor to flush
        Thread.sleep(150L)

        val files = logger.getLogFiles()
        assertEquals(1, files.size)

        val output = ByteArrayOutputStream()
        logger.exportLogs(output)
        val exportedString = output.toString("UTF-8")

        assertTrue(exportedString.contains("Hear It System Diagnostics Log Export"))
        assertTrue(exportedString.contains("Privacy Invariant: 100% On-Device · Metadata Only · Zero Notification Text"))
        assertTrue(exportedString.contains("[INFO] [SERVICE] Service started successfully"))
    }

    @Test
    fun clearAllLogs_removesAllFiles() {
        logger.log(LogLevel.INFO, LogCategory.RULES, "Rules evaluated")
        Thread.sleep(150L)

        assertTrue(logger.getLogFiles().isNotEmpty())

        val cleared = logger.clearAllLogs()
        assertTrue(cleared)
        assertEquals(0, logger.getLogFiles().size)
        assertEquals(0L, logger.getTotalSizeBytes())
    }

    @Test
    fun pruneOldLogs_removesFilesOlderThan7Days() {
        val oldFile = File(tempDir, "log_2020-01-01.log").apply {
            writeText("old event")
            setLastModified(System.currentTimeMillis() - 10L * 24 * 60 * 60 * 1000L)
        }
        val recentFile = File(tempDir, "log_2026-11-03.log").apply {
            writeText("recent event")
            setLastModified(System.currentTimeMillis())
        }

        assertEquals(2, logger.getLogFiles().size)

        logger.pruneOldLogs()

        val remainingFiles = logger.getLogFiles()
        assertEquals(1, remainingFiles.size)
        assertEquals(recentFile.name, remainingFiles[0].name)
    }
}
