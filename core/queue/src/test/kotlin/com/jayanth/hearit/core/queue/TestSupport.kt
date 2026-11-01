package com.jayanth.hearit.core.queue

import com.jayanth.hearit.core.common.model.NotificationSnapshot
import com.jayanth.hearit.core.common.time.Clock

/**
 * Deterministic clock for queue tests: starts at zero and only moves when told to, so
 * grouping windows, delay, min gap and expiry are all exact (ADR-019). Not locally
 * verified — written under the cloud-agent restriction (no build/test run).
 */
internal class FakeClock(var current: Long = 0L) : Clock {

    override fun nowMillis(): Long = current

    fun advance(millis: Long) {
        current += millis
    }
}

/** Builds a minimal valid snapshot; only the fields the queue reads are varied. */
internal fun testSnapshot(
    key: String,
    packageName: String = "com.whatsapp",
    appName: String = "WhatsApp",
    sender: String? = null,
    messageText: String = "hi",
    postTimeMillis: Long = 0L,
    isUpdate: Boolean = false,
): NotificationSnapshot = NotificationSnapshot(
    key = key,
    packageName = packageName,
    appName = appName,
    sender = sender,
    messageText = messageText,
    postTimeMillis = postTimeMillis,
    isUpdate = isUpdate,
)
