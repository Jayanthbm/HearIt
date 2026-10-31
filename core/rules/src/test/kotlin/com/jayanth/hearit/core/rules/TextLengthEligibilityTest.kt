package com.jayanth.hearit.core.rules

import com.jayanth.hearit.core.common.model.EligibilitySettings
import com.jayanth.hearit.core.common.model.KeywordFilterSettings
import com.jayanth.hearit.core.common.model.KeywordMatchMode
import com.jayanth.hearit.core.common.model.LengthScope
import com.jayanth.hearit.core.common.model.LockCondition
import com.jayanth.hearit.core.common.model.NotificationSnapshot
import com.jayanth.hearit.core.common.model.PrivacySettings
import com.jayanth.hearit.core.common.model.ResolvedSettings
import com.jayanth.hearit.core.common.model.RuntimeState
import com.jayanth.hearit.core.common.model.ScreenCondition
import com.jayanth.hearit.core.common.model.SkipReason
import com.jayanth.hearit.core.common.model.TextLengthSettings
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ADR-006 step 6: text-length limit. The item is skipped whole — the pipeline can never
 * return truncated text, which these tests pin down by checking the untouched snapshot.
 */
class TextLengthEligibilityTest {

    private val rules = EligibilityRules()

    private fun snapshot(messageText: String, sender: String? = null) = NotificationSnapshot(
        key = "com.example.app|tag|1",
        packageName = "com.example.app",
        appName = "Example",
        sender = sender,
        messageText = messageText,
        postTimeMillis = 1_000L,
    )

    private fun settings(
        textLength: TextLengthSettings = TextLengthSettings(),
        keywordFilter: KeywordFilterSettings = KeywordFilterSettings(),
        privacy: PrivacySettings = PrivacySettings(),
    ) = ResolvedSettings(
        appEnabled = true,
        eligibility = EligibilitySettings(
            screenCondition = ScreenCondition.BOTH,
            lockCondition = LockCondition.BOTH,
            textLength = textLength,
            keywordFilter = keywordFilter,
            privacy = privacy,
        ),
    )

    private val allowedState = RuntimeState(
        screenOn = true,
        isLocked = false,
        temporaryMuteActive = false,
        priorityAnnounceActive = false,
    )

    @Test
    fun evaluate_noLimitConfigured_announcesLongMessage() {
        val text = "x".repeat(5_000)
        assertEquals(
            EligibilityDecision.Announce,
            rules.evaluate(snapshot(text), settings(), allowedState),
        )
    }

    @Test
    fun evaluate_nonPositiveLimitTreatedAsUnlimited_announces() {
        val text = "x".repeat(500)
        assertEquals(
            EligibilityDecision.Announce,
            rules.evaluate(snapshot(text), settings(TextLengthSettings(maxLengthChars = 0)), allowedState),
        )
    }

    @Test
    fun evaluate_messageOverLimit_skipsWithTooLongReason() {
        assertEquals(
            EligibilityDecision.Skip(SkipReason.TOO_LONG),
            rules.evaluate(
                snapshot("x".repeat(51)),
                settings(TextLengthSettings(maxLengthChars = 50)),
                allowedState,
            ),
        )
    }

    @Test
    fun evaluate_messageExactlyAtLimit_announces() {
        assertEquals(
            EligibilityDecision.Announce,
            rules.evaluate(
                snapshot("x".repeat(50)),
                settings(TextLengthSettings(maxLengthChars = 50)),
                allowedState,
            ),
        )
    }

    @Test
    fun evaluate_wholeNotificationScope_countsSenderAndBodyTogether() {
        val bodyUnderLimit = "x".repeat(40)
        val senderOverLimit = "y".repeat(20)
        assertEquals(
            EligibilityDecision.Skip(SkipReason.TOO_LONG),
            rules.evaluate(
                snapshot(bodyUnderLimit, sender = senderOverLimit),
                settings(TextLengthSettings(maxLengthChars = 50, scope = LengthScope.WHOLE_NOTIFICATION)),
                allowedState,
            ),
        )
    }

    @Test
    fun evaluate_messageScope_ignoresSenderLength() {
        val bodyUnderLimit = "x".repeat(40)
        val senderOverLimit = "y".repeat(20)
        assertEquals(
            EligibilityDecision.Announce,
            rules.evaluate(
                snapshot(bodyUnderLimit, sender = senderOverLimit),
                settings(TextLengthSettings(maxLengthChars = 50, scope = LengthScope.MESSAGE)),
                allowedState,
            ),
        )
    }

    @Test
    fun evaluate_tooLongItem_isSkippedWithoutTruncatingTheSnapshot() {
        val fullText = "x".repeat(500)
        val snapshot = snapshot(fullText)

        val decision = rules.evaluate(
            snapshot,
            settings(TextLengthSettings(maxLengthChars = 50)),
            allowedState,
        )

        assertEquals(EligibilityDecision.Skip(SkipReason.TOO_LONG), decision)
        // The snapshot is immutable and untouched: nothing downstream can read a shortened body.
        assertEquals(fullText, snapshot.messageText)
        assertEquals(500, snapshot.messageText.length)
    }

    @Test
    fun evaluate_privacyAndTooLong_bothApply_reportsPrivacyFirst() {
        val privacy = PrivacySettings(skipBanking = true)
        assertEquals(
            EligibilityDecision.Skip(SkipReason.PRIVACY_BANKING),
            rules.evaluate(
                snapshot("x".repeat(500)),
                settings(
                    textLength = TextLengthSettings(maxLengthChars = 50),
                    privacy = privacy,
                ),
                allowedState,
            ),
        )
    }

    @Test
    fun evaluate_keywordFilterAndTooLong_bothApply_reportsTooLongFirst() {
        val keywordFilter = KeywordFilterSettings(
            enabled = true,
            keywords = listOf("x"),
            mode = KeywordMatchMode.PHRASE,
        )
        assertEquals(
            EligibilityDecision.Skip(SkipReason.TOO_LONG),
            rules.evaluate(
                snapshot("x".repeat(500)),
                settings(
                    textLength = TextLengthSettings(maxLengthChars = 50),
                    keywordFilter = keywordFilter,
                ),
                allowedState,
            ),
        )
    }
}
