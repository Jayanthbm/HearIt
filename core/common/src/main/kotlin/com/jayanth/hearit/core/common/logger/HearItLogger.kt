package com.jayanth.hearit.core.common.logger

import java.util.concurrent.atomic.AtomicReference

/**
 * Diagnostic log sink contract for structured local logging.
 */
interface DiagnosticLogSink {
    fun log(level: LogLevel, category: LogCategory, message: String, throwable: Throwable? = null)
}

/**
 * Global structured diagnostic logger accessible across all core modules and app components.
 *
 * Enforces zero-notification-content invariant.
 */
object HearItLogger {
    private val sink = AtomicReference<DiagnosticLogSink?>(null)

    fun registerSink(logSink: DiagnosticLogSink) {
        sink.set(logSink)
    }

    fun unregisterSink() {
        sink.set(null)
    }

    fun log(level: LogLevel, category: LogCategory, message: String, throwable: Throwable? = null) {
        sink.get()?.log(level, category, message, throwable)
    }

    fun debug(category: LogCategory, message: String) = log(LogLevel.DEBUG, category, message)
    fun info(category: LogCategory, message: String) = log(LogLevel.INFO, category, message)
    fun warn(category: LogCategory, message: String, throwable: Throwable? = null) = log(LogLevel.WARN, category, message, throwable)
    fun error(category: LogCategory, message: String, throwable: Throwable? = null) = log(LogLevel.ERROR, category, message, throwable)
}
