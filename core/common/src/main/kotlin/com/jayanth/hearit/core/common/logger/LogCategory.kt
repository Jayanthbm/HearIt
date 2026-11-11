package com.jayanth.hearit.core.common.logger

/**
 * Structured diagnostic log categories (master prompt §17).
 *
 * Privacy Invariant:
 * Regardless of category, notification content, OTPs, passwords, and message text
 * must never be included in diagnostic logs.
 */
enum class LogCategory {
    NOTIFICATION,
    RULES,
    QUEUE,
    AUDIO,
    TTS,
    SERVICE,
}
