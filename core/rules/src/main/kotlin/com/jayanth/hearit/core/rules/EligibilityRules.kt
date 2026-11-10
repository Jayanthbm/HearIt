package com.jayanth.hearit.core.rules

import com.jayanth.hearit.core.common.model.AudioOutput
import com.jayanth.hearit.core.common.model.AudioOutputKind
import com.jayanth.hearit.core.common.model.AudioOutputSettings
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
 *  3. active call — never speak; `Queue(CALL_ACTIVE)` when the per-app flag allows it
 *  4. DND (respected by default; per-app override may lift it)
 *  5. privacy rules (ADR-006 sub-order: excluded → OTP → password → payment → banking → keyword)
 *  7. keyword filter (disabled by default)
 *  8. text length (skip whole item, never truncate)
 *  9. screen/lock (the only family Priority Announce may bypass)
 *  10. allowed audio output
 *
 * Step 6 (hide content) is not a skip rule and is applied by the announcement template (P04).
 * DND reads only `state.isDndActive`: the snapshot's channel metadata is never used as a
 * DND proxy, so a silent channel and an actual DND policy stay distinct (ADR-017).
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

        // Critical Breakthrough Check (Idea 21): Emergency words break through temporary mute & DND
        val isCriticalBreakthrough = isEmergencyBreakthrough(snapshot.messageText)

        // Low Battery Auto-Mute (Idea 19)
        if (state.isBatteryLowOrSaver && !state.priorityAnnounceActive && !isCriticalBreakthrough) {
            return EligibilityDecision.Skip(SkipReason.MUTED)
        }

        // 2. Temporary mute is a global override and also applies in Priority Announce mode
        //    (master prompt §11, ADR-006).
        if (state.temporaryMuteActive && !isCriticalBreakthrough) {
            return EligibilityDecision.Skip(SkipReason.MUTED)
        }

        // 3. Never speak while a phone call is active (master prompt §6). A per-app opt-in can
        //    hold the item instead; the queue re-evaluates it through the whole pipeline.
        if (state.isCallActive) {
            return if (settings.eligibility.queueDuringCall) {
                EligibilityDecision.Queue(SkipReason.CALL_ACTIVE)
            } else {
                EligibilityDecision.Skip(SkipReason.CALL_ACTIVE)
            }
        }

        // 4. DND — respected by default, per-app override may lift it (master prompt §6).
        //    Reads only the device state; channel silence never reaches this rule (ADR-017).
        if (state.isDndActive && settings.eligibility.respectDnd && !isCriticalBreakthrough) {
            return EligibilityDecision.Skip(SkipReason.DND)
        }

        // 5. Privacy rules — excluded app, OTP, password, payment, banking, custom sensitive
        //    keywords (master prompt §6). Not overridable by Priority Announce mode.
        SensitiveContentDetector.detect(snapshot, settings.eligibility.privacy)?.let { return skip(it) }

        // 7. Keyword filter (disabled by default). Evaluated before the length limit so that
        //    a user-authored keyword — not the generic length rule — is the reason history
        //    shows when both would fail (ADR-006 refinement).
        val keywordFilter = settings.eligibility.keywordFilter
        if (keywordFilter.enabled &&
            KeywordMatcher.matchesNotification(
                messageText = snapshot.messageText,
                sender = snapshot.sender,
                keywords = keywordFilter.keywords,
                mode = keywordFilter.mode,
            )
        ) {
            return EligibilityDecision.Skip(SkipReason.KEYWORD_FILTERED)
        }

        // 8. Text length — the whole item is skipped when the limit is exceeded; the
        //    snapshot keeps its full text, so truncation is impossible by construction.
        evaluateTextLength(snapshot, settings.eligibility.textLength)?.let { return skip(it) }

        // 9. Screen/lock — normally required, but Priority Announce mode overrides exactly
        //    these restrictions and only for apps in its separate list (master prompt §10,
        //    ADR-006). Priority Announce does not bypass other rules (privacy, keywords,
        //    length, output). Dedup: if app is in both lists, Priority takes precedence.
        val inPriorityList = settings.priorityAnnounceListed
        val priorityOverrideApplies = state.priorityAnnounceActive && inPriorityList
        if (priorityOverrideApplies) {
            // Priority Announce bypasses screen/lock for listed apps only.
            // Other rules (privacy, keywords, length, output) still apply.
        } else {
            evaluateScreenAndLock(settings.eligibility, state)?.let { return it }
        }

        // 10. Allowed output — evaluated last, right before speech would start, and re-checked
        //     while speaking so a route change stops playback (ADR-011). Priority Announce
        //     does not bypass it (ADR-006, ADR-017).
        evaluateOutput(settings.eligibility.audioOutput, state.activeOutput)?.let { return skip(it) }

        // Every ADR-006 family is now implemented; only step 6 (hide content) remains, and it
        // is not a skip rule.
        return EligibilityDecision.Announce
    }

    /** ADR-006 step 10: null when speech may start on [activeOutput], or when it is unobserved. */
    private fun evaluateOutput(
        settings: AudioOutputSettings,
        activeOutput: AudioOutput?,
    ): SkipReason? {
        val output = activeOutput ?: return null // not observed yet → never block speech

        val allowed = when (output.kind) {
            AudioOutputKind.WIRED -> settings.allowWired
            AudioOutputKind.PHONE_SPEAKER -> settings.allowPhoneSpeaker
            AudioOutputKind.BLUETOOTH -> {
                val allowedIds = settings.allowedBluetoothIds
                settings.allowBluetooth &&
                    (allowedIds == null || (output.id != null && output.id in allowedIds))
            }
            AudioOutputKind.OTHER -> settings.allowUnidentifiedOutput
        }
        return if (allowed) null else SkipReason.OUTPUT_NOT_ALLOWED
    }

    private fun skip(reason: SkipReason): EligibilityDecision = EligibilityDecision.Skip(reason)

    /** ADR-006 step 8: returns [SkipReason.TOO_LONG] when the configured limit is exceeded. */
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
            // The reason names the actual device state that failed the condition, mirroring
            // the screen branch above (lock required + unlocked → UNLOCKED, and vice versa).
            return EligibilityDecision.Skip(
                if (state.isLocked) SkipReason.LOCKED else SkipReason.UNLOCKED,
            )
        }

        return null
    }

    private fun isEmergencyBreakthrough(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        val lower = text.lowercase()
        return lower.contains("emergency") || lower.contains("urgent") || lower.contains("icu") || lower.contains("hospital")
    }
}
