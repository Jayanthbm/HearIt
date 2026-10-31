package com.jayanth.hearit.core.rules

import com.jayanth.hearit.core.common.model.EligibilitySettings
import com.jayanth.hearit.core.common.model.LockCondition
import com.jayanth.hearit.core.common.model.NotificationSnapshot
import com.jayanth.hearit.core.common.model.ResolvedSettings
import com.jayanth.hearit.core.common.model.RuntimeState
import com.jayanth.hearit.core.common.model.ScreenCondition
import com.jayanth.hearit.core.common.model.SkipReason

/**
 * Deterministic announcement-eligibility pipeline (master prompt §6, ADR-004, ADR-006).
 *
 * The pipeline is a pure function of its three inputs: no Android types, no I/O, no clock
 * access, no mutation. Rule families are evaluated in a fixed precedence order and the
 * **first failing rule wins**, so history always shows exactly one actionable reason.
 *
 * Implemented rule families:
 *  1. enabled-app list
 *  2. temporary mute (global override)
 *  9. screen/lock (the only family Priority Announce may bypass)
 *
 * Rule families added by later phases, in their ADR-006 position: active call (P03),
 * privacy (P02), text length (P02), keywords (P02), DND (P03), allowed audio output (P03).
 *
 * The same instance is used to re-evaluate conditions **while** speech is in progress;
 * any transition to [EligibilityDecision.Skip] must stop playback immediately (ADR-011).
 */
class EligibilityRules {

    /**
     * Evaluates whether [snapshot] may be announced under [settings] in the current [state].
     *
     * @param snapshot the notification being evaluated. The families implemented in P01
     *   decide from [settings] and [state] alone; later families (text length, keywords,
     *   privacy) read the snapshot, which is why the parameter is already part of the
     *   contract.
     * @param settings global defaults already merged with this app's overrides.
     * @param state device/service state captured at evaluation time.
     */
    fun evaluate(
        snapshot: NotificationSnapshot,
        settings: ResolvedSettings,
        state: RuntimeState,
    ): EligibilityDecision {
        // 1. An app that the user never enabled is never announced (all disabled by default).
        if (!settings.appEnabled) {
            return EligibilityDecision.Skip(SkipReason.APP_DISABLED)
        }

        // 2. Temporary mute is a global override and also applies in Priority Announce mode
        //    (master prompt §11, ADR-006).
        if (state.temporaryMuteActive) {
            return EligibilityDecision.Skip(SkipReason.MUTED)
        }

        // 9. Screen/lock — normally required, but Priority Announce mode overrides exactly
        //    these restrictions and only for apps in its separate list (master prompt §10,
        //    ADR-006).
        val priorityOverrideApplies = state.priorityAnnounceActive && settings.priorityAnnounceListed
        if (!priorityOverrideApplies) {
            evaluateScreenAndLock(settings.eligibility, state)?.let { return it }
        }

        // ADR-006 steps 3-8 (call, privacy, length, keyword, DND) are inserted above this
        // screen/lock check by phases P02/P03, and step 10 (allowed audio output) below it,
        // so the precedence order always matches the ADR as each family lands.
        return EligibilityDecision.Announce
    }

    /**
     * Screen condition first, then lock condition; both must pass because the global
     * default combines them with AND logic (master prompt §6). Returns null when both
     * pass, or the skip reason describing the actual state that failed.
     */
    private fun evaluateScreenAndLock(
        settings: EligibilitySettings,
        state: RuntimeState,
    ): EligibilityDecision.Skip? {
        val screenMatches = when (settings.screenCondition) {
            ScreenCondition.ON -> state.screenOn
            ScreenCondition.OFF -> !state.screenOn
            ScreenCondition.BOTH -> true
        }
        if (!screenMatches) {
            return EligibilityDecision.Skip(
                if (state.screenOn) SkipReason.SCREEN_ON else SkipReason.SCREEN_OFF,
            )
        }

        val lockMatches = when (settings.lockCondition) {
            LockCondition.LOCKED -> state.isLocked
            LockCondition.UNLOCKED -> !state.isLocked
            LockCondition.BOTH -> true
        }
        if (!lockMatches) {
            return EligibilityDecision.Skip(
                if (state.isLocked) SkipReason.UNLOCKED else SkipReason.LOCKED,
            )
        }

        return null
    }
}
