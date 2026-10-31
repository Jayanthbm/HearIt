package com.jayanth.hearit.core.rules

import com.jayanth.hearit.core.common.model.EligibilitySettings
import com.jayanth.hearit.core.common.model.KeywordFilterSettings
import com.jayanth.hearit.core.common.model.KeywordMatchMode
import com.jayanth.hearit.core.common.model.LockCondition
import com.jayanth.hearit.core.common.model.NotificationSnapshot
import com.jayanth.hearit.core.common.model.PrivacySettings
import com.jayanth.hearit.core.common.model.ResolvedSettings
import com.jayanth.hearit.core.common.model.RuntimeState
import com.jayanth.hearit.core.common.model.ScreenCondition
import com.jayanth.hearit.core.common.model.SkipReason
import org.junit.Assert.assertEquals
import org.junit.Test

/** ADR-006 step 7: custom keyword filtering, disabled by default (master prompt §6). */
class KeywordFilterEligibilityTest {

    private val rules = EligibilityRules()

    private fun snapshot(messageText: String) = NotificationSnapshot(
        key = "com.example.app|tag|1",
        packageName = "com.example.app",
        appName = "Example",
        messageText = messageText,
        postTimeMillis = 1_000L,
    )

    private fun settings(
        keywordFilter: KeywordFilterSettings = KeywordFilterSettings(),
        privacy: PrivacySettings = PrivacySettings(),
    ) = ResolvedSettings(
        appEnabled = true,
        eligibility = EligibilitySettings(
            screenCondition = ScreenCondition.BOTH,
            lockCondition = LockCondition.BOTH,
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
    fun evaluate_filterDisabled_keywordPresent_announces() {
        val filter = KeywordFilterSettings(enabled = false, keywords = listOf("meeting"))
        assertEquals(
            EligibilityDecision.Announce,
            rules.evaluate(snapshot("Meeting at 5"), settings(filter), allowedState),
        )
    }

    @Test
    fun evaluate_filterEnabled_noKeywords_announces() {
        val filter = KeywordFilterSettings(enabled = true, keywords = emptyList())
        assertEquals(
            EligibilityDecision.Announce,
            rules.evaluate(snapshot("Anything at all"), settings(filter), allowedState),
        )
    }

    @Test
    fun evaluate_filterEnabledMatchingKeywordIsCaseInsensitive_skips() {
        val filter = KeywordFilterSettings(enabled = true, keywords = listOf("confidential"))
        assertEquals(
            EligibilityDecision.Skip(SkipReason.KEYWORD_FILTERED),
            rules.evaluate(
                snapshot("This is Confidential information"),
                settings(filter),
                allowedState,
            ),
        )
    }

    @Test
    fun evaluate_wholeWordMode_substringInsideWordDoesNotMatch_announces() {
        val filter = KeywordFilterSettings(
            enabled = true,
            keywords = listOf("ash"),
            mode = KeywordMatchMode.WHOLE_WORD,
        )
        assertEquals(
            EligibilityDecision.Announce,
            rules.evaluate(snapshot("I will pay cash"), settings(filter), allowedState),
        )
    }

    @Test
    fun evaluate_wholeWordMode_delimitedWordMatches_skips() {
        val filter = KeywordFilterSettings(
            enabled = true,
            keywords = listOf("ash"),
            mode = KeywordMatchMode.WHOLE_WORD,
        )
        assertEquals(
            EligibilityDecision.Skip(SkipReason.KEYWORD_FILTERED),
            rules.evaluate(snapshot("Hand over the ash"), settings(filter), allowedState),
        )
    }

    @Test
    fun evaluate_phraseMode_substringInsideWordMatches_skips() {
        val filter = KeywordFilterSettings(
            enabled = true,
            keywords = listOf("ash"),
            mode = KeywordMatchMode.PHRASE,
        )
        assertEquals(
            EligibilityDecision.Skip(SkipReason.KEYWORD_FILTERED),
            rules.evaluate(snapshot("I will pay cash"), settings(filter), allowedState),
        )
    }

    @Test
    fun evaluate_multipleKeywords_secondOneMatches_skips() {
        val filter = KeywordFilterSettings(enabled = true, keywords = listOf("alpha", "beta"))
        assertEquals(
            EligibilityDecision.Skip(SkipReason.KEYWORD_FILTERED),
            rules.evaluate(snapshot("the beta build is ready"), settings(filter), allowedState),
        )
    }

    @Test
    fun evaluate_blankKeywordEntries_areIgnored_announces() {
        val filter = KeywordFilterSettings(enabled = true, keywords = listOf("", "   "))
        assertEquals(
            EligibilityDecision.Announce,
            rules.evaluate(snapshot("anything"), settings(filter), allowedState),
        )
    }

    @Test
    fun evaluate_devanagariKeywordWholeWord_matchesBoundaries() {
        val filter = KeywordFilterSettings(enabled = true, keywords = listOf("घोषणा"))
        assertEquals(
            EligibilityDecision.Announce,
            rules.evaluate(
                snapshot("यह घोषणापत्र जारी है"), // "ash"-style prefix inside a longer word
                settings(filter),
                allowedState,
            ),
        )
    }

    @Test
    fun evaluate_mutedAndKeywordMatch_reportsMutedFirst() {
        val filter = KeywordFilterSettings(enabled = true, keywords = listOf("secret"))
        val state = allowedState.copy(temporaryMuteActive = true)
        assertEquals(
            EligibilityDecision.Skip(SkipReason.MUTED),
            rules.evaluate(snapshot("the secret plan"), settings(filter), state),
        )
    }

    @Test
    fun evaluate_sensitiveKeywordAndFilterMatch_reportsPrivacyFirst() {
        val privacy = PrivacySettings(sensitiveKeywords = listOf("secret"))
        val filter = KeywordFilterSettings(enabled = true, keywords = listOf("secret"))
        assertEquals(
            EligibilityDecision.Skip(SkipReason.PRIVACY_SENSITIVE_KEYWORD),
            rules.evaluate(snapshot("the secret plan"), settings(filter, privacy), allowedState),
        )
    }
}
