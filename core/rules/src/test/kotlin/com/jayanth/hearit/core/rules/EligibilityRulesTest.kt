package com.jayanth.hearit.core.rules

import com.jayanth.hearit.core.common.model.EligibilitySettings
import com.jayanth.hearit.core.common.model.LockCondition
import com.jayanth.hearit.core.common.model.NotificationSnapshot
import com.jayanth.hearit.core.common.model.ResolvedSettings
import com.jayanth.hearit.core.common.model.RuntimeState
import com.jayanth.hearit.core.common.model.ScreenCondition
import com.jayanth.hearit.core.common.model.SkipReason
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Rule-by-rule coverage of `EligibilityRules.evaluate` following the precedence order of
 * ADR-006. Fixed inputs and no clock keep every case deterministic.
 */
class EligibilityRulesTest {

    private val rules = EligibilityRules()

    private fun snapshot(messageText: String = "Are you coming today?") = NotificationSnapshot(
        key = "com.example.app|tag|1",
        packageName = "com.example.app",
        appName = "Example",
        sender = "Rahul",
        messageText = messageText,
        postTimeMillis = 1_000L,
    )

    private fun settings(
        appEnabled: Boolean = true,
        priorityAnnounceListed: Boolean = false,
        screen: ScreenCondition = ScreenCondition.OFF,
        lock: LockCondition = LockCondition.LOCKED,
    ) = ResolvedSettings(
        appEnabled = appEnabled,
        priorityAnnounceListed = priorityAnnounceListed,
        eligibility = EligibilitySettings(screenCondition = screen, lockCondition = lock),
    )

    private fun state(
        screenOn: Boolean = false,
        isLocked: Boolean = true,
        temporaryMuteActive: Boolean = false,
        priorityAnnounceActive: Boolean = false,
    ) = RuntimeState(
        screenOn = screenOn,
        isLocked = isLocked,
        temporaryMuteActive = temporaryMuteActive,
        priorityAnnounceActive = priorityAnnounceActive,
    )

    // --- Default screen/lock behaviour (screen OFF AND locked) --------------------------

    @Test
    fun evaluate_defaultConditions_screenOffAndLocked_announces() {
        assertEquals(
            EligibilityDecision.Announce,
            rules.evaluate(snapshot(), settings(), state()),
        )
    }

    @Test
    fun evaluate_screenOn_skipsWithScreenOnReason() {
        assertEquals(
            EligibilityDecision.Skip(SkipReason.SCREEN_ON),
            rules.evaluate(snapshot(), settings(), state(screenOn = true)),
        )
    }

    @Test
    fun evaluate_unlocked_skipsWithUnlockedReason() {
        assertEquals(
            EligibilityDecision.Skip(SkipReason.UNLOCKED),
            rules.evaluate(snapshot(), settings(), state(isLocked = false)),
        )
    }

    @Test
    fun evaluate_screenOnAndUnlocked_reportsScreenOnBeforeUnlocked() {
        assertEquals(
            EligibilityDecision.Skip(SkipReason.SCREEN_ON),
            rules.evaluate(snapshot(), settings(), state(screenOn = true, isLocked = false)),
        )
    }

    // --- Independent screen and lock conditions -----------------------------------------

    @Test
    fun evaluate_screenConditionBoth_announcesWhileScreenIsOn() {
        assertEquals(
            EligibilityDecision.Announce,
            rules.evaluate(snapshot(), settings(screen = ScreenCondition.BOTH), state(screenOn = true)),
        )
    }

    @Test
    fun evaluate_lockConditionBoth_announcesWhileUnlocked() {
        assertEquals(
            EligibilityDecision.Announce,
            rules.evaluate(snapshot(), settings(lock = LockCondition.BOTH), state(isLocked = false)),
        )
    }

    @Test
    fun evaluate_screenConditionOnButScreenOff_skipsWithScreenOffReason() {
        assertEquals(
            EligibilityDecision.Skip(SkipReason.SCREEN_OFF),
            rules.evaluate(snapshot(), settings(screen = ScreenCondition.ON), state(screenOn = false)),
        )
    }

    @Test
    fun evaluate_lockConditionUnlockedButDeviceLocked_skipsWithLockedReason() {
        assertEquals(
            EligibilityDecision.Skip(SkipReason.LOCKED),
            rules.evaluate(
                snapshot(),
                settings(lock = LockCondition.UNLOCKED),
                state(isLocked = true),
            ),
        )
    }

    @Test
    fun evaluate_customConditions_screenOnAndUnlocked_announces() {
        assertEquals(
            EligibilityDecision.Announce,
            rules.evaluate(
                snapshot(),
                settings(screen = ScreenCondition.ON, lock = LockCondition.UNLOCKED),
                state(screenOn = true, isLocked = false),
            ),
        )
    }

    // --- Precedence: enabled list and mute ----------------------------------------------

    @Test
    fun evaluate_appDisabled_skipsEvenWhenStateAllows() {
        assertEquals(
            EligibilityDecision.Skip(SkipReason.APP_DISABLED),
            rules.evaluate(snapshot(), settings(appEnabled = false), state()),
        )
    }

    @Test
    fun evaluate_appDisabledAndMuted_reportsAppDisabledFirst() {
        assertEquals(
            EligibilityDecision.Skip(SkipReason.APP_DISABLED),
            rules.evaluate(
                snapshot(),
                settings(appEnabled = false),
                state(temporaryMuteActive = true),
            ),
        )
    }

    @Test
    fun evaluate_muted_skipsEvenWhenStateAllows() {
        assertEquals(
            EligibilityDecision.Skip(SkipReason.MUTED),
            rules.evaluate(snapshot(), settings(), state(temporaryMuteActive = true)),
        )
    }

    @Test
    fun evaluate_mutedWhileScreenOn_reportsMutedBeforeScreen() {
        assertEquals(
            EligibilityDecision.Skip(SkipReason.MUTED),
            rules.evaluate(
                snapshot(),
                settings(),
                state(screenOn = true, temporaryMuteActive = true),
            ),
        )
    }

    // --- Priority Announce overrides only screen/lock (master prompt §10) ----------------

    @Test
    fun evaluate_priorityModeActiveAndListed_announcesWhileScreenOnAndUnlocked() {
        assertEquals(
            EligibilityDecision.Announce,
            rules.evaluate(
                snapshot(),
                settings(priorityAnnounceListed = true),
                state(screenOn = true, isLocked = false, priorityAnnounceActive = true),
            ),
        )
    }

    @Test
    fun evaluate_priorityModeActiveButAppNotListed_stillSkipsWithScreenOnReason() {
        assertEquals(
            EligibilityDecision.Skip(SkipReason.SCREEN_ON),
            rules.evaluate(
                snapshot(),
                settings(priorityAnnounceListed = false),
                state(screenOn = true, priorityAnnounceActive = true),
            ),
        )
    }

    @Test
    fun evaluate_appListedButPriorityModeInactive_stillSkipsWithScreenOnReason() {
        assertEquals(
            EligibilityDecision.Skip(SkipReason.SCREEN_ON),
            rules.evaluate(
                snapshot(),
                settings(priorityAnnounceListed = true),
                state(screenOn = true, priorityAnnounceActive = false),
            ),
        )
    }

    @Test
    fun evaluate_priorityModeActiveAndListedButMuted_stillSkipsWithMutedReason() {
        assertEquals(
            EligibilityDecision.Skip(SkipReason.MUTED),
            rules.evaluate(
                snapshot(),
                settings(priorityAnnounceListed = true),
                state(temporaryMuteActive = true, priorityAnnounceActive = true),
            ),
        )
    }

    @Test
    fun evaluate_priorityModeActiveAndListedButAppDisabled_stillSkipsWithAppDisabledReason() {
        assertEquals(
            EligibilityDecision.Skip(SkipReason.APP_DISABLED),
            rules.evaluate(
                snapshot(),
                settings(appEnabled = false, priorityAnnounceListed = true),
                state(screenOn = true, priorityAnnounceActive = true),
            ),
        )
    }
}
