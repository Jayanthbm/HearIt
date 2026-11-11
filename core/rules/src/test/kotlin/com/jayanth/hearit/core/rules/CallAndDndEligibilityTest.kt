package com.jayanth.hearit.core.rules

import com.jayanth.hearit.core.common.model.EligibilitySettings
import com.jayanth.hearit.core.common.model.LockCondition
import com.jayanth.hearit.core.common.model.NotificationSnapshot
import com.jayanth.hearit.core.common.model.PrivacySettings
import com.jayanth.hearit.core.common.model.ResolvedSettings
import com.jayanth.hearit.core.common.model.RuntimeState
import com.jayanth.hearit.core.common.model.ScreenCondition
import com.jayanth.hearit.core.common.model.SkipReason
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ADR-006 steps 3 (call) and 4 (DND): the transient device-state family.
 *
 * Master-prompt rules pinned here: never speak during a call; call-time queueing is off by
 * default and never bypasses a rule; DND is respected by default with a per-app override;
 * DND reads only the device flag and never the notification's channel metadata (ADR-017);
 * no rule except screen/lock can be overridden by Priority Announce.
 */
class CallAndDndEligibilityTest {

    private val rules = EligibilityRules()

    private fun snapshot(messageText: String = "Are you coming today?", silentChannel: Boolean = false) =
        NotificationSnapshot(
            key = "com.example.app|tag|1",
            packageName = "com.example.app",
            appName = "Example",
            messageText = messageText,
            channelId = "messages",
            isSilentChannel = silentChannel,
            postTimeMillis = 1_000L,
        )

    private fun settings(
        appEnabled: Boolean = true,
        priorityAnnounceListed: Boolean = false,
        screen: ScreenCondition = ScreenCondition.BOTH,
        lock: LockCondition = LockCondition.BOTH,
        respectDnd: Boolean = true,
        queueDuringCall: Boolean = false,
        privacy: PrivacySettings = PrivacySettings(),
    ) = ResolvedSettings(
        appEnabled = appEnabled,
        priorityAnnounceListed = priorityAnnounceListed,
        eligibility = EligibilitySettings(
            screenCondition = screen,
            lockCondition = lock,
            respectDnd = respectDnd,
            queueDuringCall = queueDuringCall,
            privacy = privacy,
        ),
    )

    private fun state(
        callActive: Boolean = false,
        dndActive: Boolean = false,
        screenOn: Boolean = false,
        isLocked: Boolean = true,
        temporaryMuteActive: Boolean = false,
        priorityAnnounceActive: Boolean = false,
    ) = RuntimeState(
        screenOn = screenOn,
        isLocked = isLocked,
        temporaryMuteActive = temporaryMuteActive,
        priorityAnnounceActive = priorityAnnounceActive,
        isCallActive = callActive,
        isDndActive = dndActive,
        activeOutput = null,
    )

    // --- Step 3: active call ---------------------------------------------------------------

    @Test
    fun evaluate_callActive_skipsWithCallActiveReason() {
        assertEquals(
            EligibilityDecision.Skip(SkipReason.CALL_ACTIVE),
            rules.evaluate(snapshot(), settings(), state(callActive = true)),
        )
    }

    @Test
    fun evaluate_callActive_queueDuringCallEnabled_queuesInsteadOfSkipping() {
        assertEquals(
            EligibilityDecision.Queue(SkipReason.CALL_ACTIVE),
            rules.evaluate(snapshot(), settings(queueDuringCall = true), state(callActive = true)),
        )
    }

    @Test
    fun evaluate_queueDuringCallIsOffByDefault_andCallInactive_announces() {
        assertEquals(false, EligibilitySettings().queueDuringCall)
        assertEquals(
            EligibilityDecision.Announce,
            rules.evaluate(snapshot(), settings(), state()),
        )
    }

    @Test
    fun evaluate_callActive_appDisabled_reportsAppDisabledFirst() {
        assertEquals(
            EligibilityDecision.Skip(SkipReason.APP_DISABLED),
            rules.evaluate(snapshot(), settings(appEnabled = false), state(callActive = true)),
        )
    }

    @Test
    fun evaluate_callActive_muted_reportsMutedFirst() {
        assertEquals(
            EligibilityDecision.Skip(SkipReason.MUTED),
            rules.evaluate(snapshot(), settings(), state(callActive = true, temporaryMuteActive = true)),
        )
    }

    @Test
    fun evaluate_callActive_priorityModeActiveStillReportsCallActive() {
        // Priority Announce bypasses screen/lock only (master prompt §10); a call is absolute.
        assertEquals(
            EligibilityDecision.Skip(SkipReason.CALL_ACTIVE),
            rules.evaluate(
                snapshot(),
                settings(priorityAnnounceListed = true),
                state(callActive = true, screenOn = true, isLocked = false, priorityAnnounceActive = true),
            ),
        )
    }

    // --- Step 4: DND ------------------------------------------------------------------------

    @Test
    fun evaluate_dndActive_respectedByDefault_skipsWithDndReason() {
        assertEquals(true, EligibilitySettings().respectDnd)
        assertEquals(
            EligibilityDecision.Skip(SkipReason.DND),
            rules.evaluate(snapshot(), settings(), state(dndActive = true)),
        )
    }

    @Test
    fun evaluate_dndActive_perAppOverrideLiftsDnd_announces() {
        assertEquals(
            EligibilityDecision.Announce,
            rules.evaluate(snapshot(), settings(respectDnd = false), state(dndActive = true)),
        )
    }

    @Test
    fun evaluate_dndInactive_announces() {
        assertEquals(
            EligibilityDecision.Announce,
            rules.evaluate(snapshot(), settings(), state(dndActive = false)),
        )
    }

    @Test
    fun evaluate_dndActive_muted_reportsMutedFirst() {
        assertEquals(
            EligibilityDecision.Skip(SkipReason.MUTED),
            rules.evaluate(snapshot(), settings(), state(dndActive = true, temporaryMuteActive = true)),
        )
    }

    @Test
    fun evaluate_dndActive_callActive_reportsCallActiveFirst() {
        assertEquals(
            EligibilityDecision.Skip(SkipReason.CALL_ACTIVE),
            rules.evaluate(snapshot(), settings(), state(callActive = true, dndActive = true)),
        )
    }

    @Test
    fun evaluate_dndActive_privacyExcluded_reportsDndBeforePrivacy() {
        val privacy = PrivacySettings(excludeApp = true)
        assertEquals(
            EligibilityDecision.Skip(SkipReason.DND),
            rules.evaluate(snapshot(), settings(privacy = privacy), state(dndActive = true)),
        )
    }

    @Test
    fun evaluate_dndActive_priorityModeActiveStillReportsDnd() {
        assertEquals(
            EligibilityDecision.Skip(SkipReason.DND),
            rules.evaluate(
                snapshot(),
                settings(priorityAnnounceListed = true),
                state(dndActive = true, screenOn = true, isLocked = false, priorityAnnounceActive = true),
            ),
        )
    }

    // --- DND vs silent channel (ADR-017) ----------------------------------------------------

    @Test
    fun evaluate_silentChannel_dndInactive_announces() {
        assertEquals(
            EligibilityDecision.Announce,
            rules.evaluate(snapshot(silentChannel = true), settings(), state(dndActive = false)),
        )
    }

    @Test
    fun evaluate_silentChannel_dndActive_reportsDeviceDndReason() {
        // Channel metadata must neither suppress nor create the DND rule.
        assertEquals(
            EligibilityDecision.Skip(SkipReason.DND),
            rules.evaluate(snapshot(silentChannel = true), settings(), state(dndActive = true)),
        )
        assertEquals(
            EligibilityDecision.Skip(SkipReason.DND),
            rules.evaluate(snapshot(silentChannel = false), settings(), state(dndActive = true, screenOn = true)),
        )
    }

    // --- Queue re-evaluation contract (master prompt §6) -------------------------------------

    @Test
    fun evaluate_callEnds_queuedItemIsReEvaluatedThroughTheFullPipeline() {
        val queued = rules.evaluate(snapshot(), settings(queueDuringCall = true), state(callActive = true))
        assertEquals(EligibilityDecision.Queue(SkipReason.CALL_ACTIVE), queued)

        // After the call, the same snapshot re-enters the whole pipeline: all-clear state
        // announces, and a state that now fails screen/lock reports that reason instead —
        // queueing never carries a pre-approved decision forward.
        assertEquals(
            EligibilityDecision.Announce,
            rules.evaluate(snapshot(), settings(), state(callActive = false)),
        )
        assertEquals(
            EligibilityDecision.Skip(SkipReason.SCREEN_ON),
            rules.evaluate(
                snapshot(),
                settings(screen = ScreenCondition.OFF),
                state(callActive = false, screenOn = true),
            ),
        )
    }
}
