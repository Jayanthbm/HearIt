package com.jayanth.hearit.core.rules

import com.jayanth.hearit.core.common.model.AudioOutput
import com.jayanth.hearit.core.common.model.AudioOutputKind
import com.jayanth.hearit.core.common.model.EligibilitySettings
import com.jayanth.hearit.core.common.model.LockCondition
import com.jayanth.hearit.core.common.model.NotificationSnapshot
import com.jayanth.hearit.core.common.model.ResolvedSettings
import com.jayanth.hearit.core.common.model.RuntimeState
import com.jayanth.hearit.core.common.model.ScreenCondition
import com.jayanth.hearit.core.common.model.SkipReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeguardAndBreakthroughEligibilityTest {

    private val rules = EligibilityRules()

    private fun snap(
        text: String = "Hello",
        sender: String? = "Alex",
    ) = NotificationSnapshot(
        key = "com.whatsapp|1",
        packageName = "com.whatsapp",
        appName = "WhatsApp",
        sender = sender,
        messageText = text,
        postTimeMillis = 1000L,
    )

    private fun baseState(
        batteryPercent: Int = 80,
        isPowerSave: Boolean = false,
        temporaryMute: Boolean = false,
        priorityAnnounce: Boolean = false,
        proximityCovered: Boolean = true,
        outputKind: AudioOutputKind? = AudioOutputKind.BLUETOOTH,
    ) = RuntimeState(
        screenOn = false,
        isLocked = true,
        temporaryMuteActive = temporaryMute,
        priorityAnnounceActive = priorityAnnounce,
        isCallActive = false,
        isDndActive = false,
        activeOutput = outputKind?.let { AudioOutput(it, "bt1", "Earbuds") },
        isBatteryLowOrSaver = false,
        isProximityCovered = proximityCovered,
        batteryPercent = batteryPercent,
        isPowerSave = isPowerSave,
    )

    @Test
    fun lowBatterySafeguard_skipsBelowConfiguredThreshold() {
        val settings = ResolvedSettings(
            appEnabled = true,
            eligibility = EligibilitySettings(lowBatterySafeguardEnabled = true, lowBatteryThresholdPercent = 20),
        )
        assertEquals(
            EligibilityDecision.Skip(SkipReason.LOW_BATTERY),
            rules.evaluate(snap(), settings, baseState(batteryPercent = 18)),
        )
        assertEquals(
            EligibilityDecision.Announce,
            rules.evaluate(snap(), settings, baseState(batteryPercent = 25)),
        )
    }

    @Test
    fun criticalKeywordBreakthrough_bypassesMuteAndDisabledAppWhenConfigured() {
        val settings = ResolvedSettings(
            appEnabled = false,
            eligibility = EligibilitySettings(
                criticalBreakthroughEnabled = true,
                criticalKeywords = listOf("Server Down", "PagerDuty"),
                criticalBreakthroughAllowDisabledApps = true,
            ),
        )
        assertEquals(
            EligibilityDecision.Announce,
            rules.evaluate(snap("Alert: Production Server Down!"), settings, baseState(temporaryMute = true)),
        )
        assertEquals(
            EligibilityDecision.Skip(SkipReason.APP_DISABLED),
            rules.evaluate(snap("Normal ping"), settings, baseState()),
        )
    }

    @Test
    fun pocketDetection_skipsWhenUncoveredUnlessPriorityOrHeadphoneBypass() {
        val pocketOn = ResolvedSettings(
            appEnabled = true,
            priorityAnnounceListed = true,
            eligibility = EligibilitySettings(
                screenCondition = ScreenCondition.BOTH,
                lockCondition = LockCondition.BOTH,
                pocketDetectionEnabled = true,
                pocketDetectionHeadphoneBypass = false,
            ),
        )
        assertEquals(
            EligibilityDecision.Skip(SkipReason.PROXIMITY_UNCOVERED),
            rules.evaluate(snap(), pocketOn, baseState(proximityCovered = false)),
        )
        // Priority Announce active bypasses pocket detection cleanly
        assertEquals(
            EligibilityDecision.Announce,
            rules.evaluate(snap(), pocketOn, baseState(proximityCovered = false, priorityAnnounce = true)),
        )
        // Headphone bypass option lets audio continue on Bluetooth when phone is in hand
        val withBypass = pocketOn.copy(
            eligibility = pocketOn.eligibility.copy(pocketDetectionHeadphoneBypass = true),
        )
        assertEquals(
            EligibilityDecision.Announce,
            rules.evaluate(snap(), withBypass, baseState(proximityCovered = false, outputKind = AudioOutputKind.BLUETOOTH)),
        )
    }
}
