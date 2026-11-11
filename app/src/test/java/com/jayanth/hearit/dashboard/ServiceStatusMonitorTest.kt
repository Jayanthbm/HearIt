package com.jayanth.hearit.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Service-observation tests (P12). Written, **not executed** — cloud-agent restriction.
 *
 * `ServiceStatusMonitor` has no Android dependency, so the dashboard's view of the service
 * can be reasoned about without a device; reading `state.value` needs no coroutine test
 * machinery.
 */
class ServiceStatusMonitorTest {

    @Test
    fun initialState_isStoppedAndSilent() {
        val monitor = ServiceStatusMonitor()

        assertEquals(false, monitor.state.value.isRunning)
        assertEquals(false, monitor.state.value.isSpeaking)
        assertEquals(0, monitor.state.value.pendingCount)
    }

    @Test
    fun markRunning_setsRunningOnly() {
        val monitor = ServiceStatusMonitor()

        monitor.markRunning()

        assertTrue(monitor.state.value.isRunning)
        assertFalse(monitor.state.value.isSpeaking)
        assertEquals(0, monitor.state.value.pendingCount)
    }

    @Test
    fun updateSpeech_publishesSpeakingAndQueueDepth() {
        val monitor = ServiceStatusMonitor()
        monitor.markRunning()

        monitor.updateSpeech(isSpeaking = true, pendingCount = 3)

        assertTrue(monitor.state.value.isRunning)
        assertTrue(monitor.state.value.isSpeaking)
        assertEquals(3, monitor.state.value.pendingCount)
    }

    @Test
    fun updateSpeech_negativePendingCount_isClampedToZero() {
        val monitor = ServiceStatusMonitor()
        monitor.markRunning()

        monitor.updateSpeech(isSpeaking = false, pendingCount = -5)

        assertEquals(0, monitor.state.value.pendingCount)
    }

    @Test
    fun updateSpeech_beforeRunning_doesNotFakeAStart() {
        val monitor = ServiceStatusMonitor()

        monitor.updateSpeech(isSpeaking = true, pendingCount = 1)

        assertFalse(monitor.state.value.isRunning)
    }

    @Test
    fun markStopped_clearsRunningSpeechAndQueue() {
        val monitor = ServiceStatusMonitor()
        monitor.markRunning()
        monitor.updateSpeech(isSpeaking = true, pendingCount = 2)

        monitor.markStopped()

        assertEquals(ServiceSnapshot(), monitor.state.value)
    }

    @Test
    fun markStopped_isIdempotent() {
        val monitor = ServiceStatusMonitor()
        monitor.markRunning()

        monitor.markStopped()
        monitor.markStopped()

        assertEquals(ServiceSnapshot(), monitor.state.value)
    }

    @Test
    fun updateSpeech_afterStop_canReportAgainOnceRunning() {
        val monitor = ServiceStatusMonitor()
        monitor.markRunning()
        monitor.markStopped()

        monitor.markRunning()
        monitor.updateSpeech(isSpeaking = false, pendingCount = 1)

        assertTrue(monitor.state.value.isRunning)
        assertEquals(1, monitor.state.value.pendingCount)
    }
}
