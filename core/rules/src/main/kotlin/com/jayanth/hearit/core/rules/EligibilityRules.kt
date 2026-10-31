package com.jayanth.hearit.core.rules

import com.jayanth.hearit.core.common.model.EligibilitySettings
import com.jayanth.hearit.core.common.model.LengthScope
import com.jayanth.hearit.core.common.model.LockCondition
import com.jayanth.hearit.core.common.model.NotificationSnapshot
import com.jayanth.hearit.core.common.model.ResolvedSettings
import com.jayanth.hearit.core.common.model.RuntimeState
import com.jayanth.hearit.core.common.model.ScreenCondition
import com.jayanth.hearit.core.common.model.SkipReason
import com.jayanth.hearit.core.common.model.TextLengthSettings

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
 *  4. privacy rules (ADR-006 sub-order: excluded → OTP → password → payment → banking → keyword)
 *  6. text length (skip whole item, never truncate)
 *  7. keyword filter
 *  9. screen/lock (the only family Priority Announce may bypass)
 *
 * Rule families added by later phases, in their ADR-006 position: active call and DND (P03),
 * allowed audio output (P03). Step 5 (hide content) is not a skip rule and is applied by the
 * announcement template (P04).
 *
 * The same instance is used to re-evaluate conditions **while** speech is in progress;
 * any transition to [EligibilityDecision.Skip] must stop playback immediately (ADR-011).
 */
class EligibilityRules {

    /**
     * Evaluates whether [snapshot] may be announced under [settings] in the current [state].
     *
     * @param snapshot the notification being evaluated. Privacy, length and keyword rules
     *   read its text; screen/lock, mute and the enabled list decide from [settings] and
     *   [state] alone.
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

        // 4. Privacy rules — excluded app, OTP, password, payment, banking, custom sensitive
        //    keywords (master prompt §6). Not overridable by Priority Announce mode.
        SensitiveContentDetector.detect(snapshot, settings.eligibility.privacy)?.let { return skip(it) }

        // 6. Text length — the whole item is skipped when the limit is exceeded; the
        //    snapshot keeps its full text, so truncation is impossible by construction.
        evaluateTextLength(snapshot, settings.eligibility.textLength)?.let { return skip(it) }

        // 7. Keyword filter (disabled by default).
        val keywordFilter = settings.eligibility.keywordFilter
        if (keywordFilter.enabled &&
            KeywordMatcher.matchesAny(snapshot.messageText, keywordFilter.keywords, keywordFilter.mode)
        ) {
            return EligibilityDecision.Skip(SkipReason.KEYWORD_FILTERED)
        }

        // 9. Screen/lock — normally required, but Priority Announce mode overrides exactly
        //    these restrictions and only for apps in its separate list (master prompt §10,
        //    ADR-006).
        val priorityOverrideApplies = state.priorityAnnounceActive && settings.priorityAnnounceListed
        if (!priorityOverrideApplies) {
            evaluateScreenAndLock(settings.eligibility, state)?.let { return it }
        }

        // ADR-006 steps 3 (call) and 8 (DND) are inserted above this screen/lock check by
        // phase P03, and step 10 (allowed audio output) below it, so the precedence order
        // always matches the ADR as each family lands.
        return EligibilityDecision.Announce
    }

    private fun skip(reason: SkipReason): EligibilityDecision = EligibilityDecision.Skip(reason)

    /** ADR-006 step 6: returns [SkipReason.TOO_LONG] when the configured limit is exceeded. */
    private fun evaluateTextLength(
        snapshot: NotificationSnapshot,
        settings: TextLengthSettings,
    ): SkipReason? {
        val max = settings.maxLengthChars ?: return null
        if (max <= 0) return null // non-positive limit means "no limit"

        val measured = when (settings.scope) {
            LengthScope.MESSAGE -> snapshot.messageText
            // Whole notification = sender + body; the app name is metadata, not notification
            // text, and is always announced separately.
            LengthScope.WHOLE_NOTIFICATION -> listOfNotNull(snapshot.sender, snapshot.messageText)
                .joinToString(" ")
        }
        return if (measured.length > max) SkipReason.TOO_LONG else null
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
