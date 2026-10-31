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
 * ADR-006 step 4: privacy rules. Detection is heuristic and imperfect by design; these
 * tests pin the sub-order (excluded → OTP → password → payment → banking → custom keyword)
 * and prove that Priority Announce mode can never bypass a privacy rule.
 */
class PrivacyEligibilityTest {

    private val rules = EligibilityRules()

    private fun snapshot(messageText: String) = NotificationSnapshot(
        key = "com.example.app|tag|1",
        packageName = "com.example.app",
        appName = "Example",
        messageText = messageText,
        postTimeMillis = 1_000L,
    )

    private fun settings(privacy: PrivacySettings = PrivacySettings()) = ResolvedSettings(
        appEnabled = true,
        eligibility = EligibilitySettings(
            screenCondition = ScreenCondition.BOTH,
            lockCondition = LockCondition.BOTH,
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
    fun evaluate_allPrivacyTogglesOff_financialTextStillAnnounces() {
        assertEquals(
            EligibilityDecision.Announce,
            rules.evaluate(
                snapshot("Your payment of Rs. 500 was successful"),
                settings(),
                allowedState,
            ),
        )
    }

    @Test
    fun evaluate_excludeApp_skipsEvenWhenEnabled() {
        assertEquals(
            EligibilityDecision.Skip(SkipReason.PRIVACY_EXCLUDED_APP),
            rules.evaluate(
                snapshot("hello"),
                settings(PrivacySettings(excludeApp = true)),
                allowedState,
            ),
        )
    }

    @Test
    fun evaluate_otpTermPresent_skipsWithPrivacyOtp() {
        assertEquals(
            EligibilityDecision.Skip(SkipReason.PRIVACY_OTP),
            rules.evaluate(
                snapshot("Your OTP for login is 482193"),
                settings(PrivacySettings(skipOtp = true)),
                allowedState,
            ),
        )
    }

    @Test
    fun evaluate_messageIsOnlyNumericCode_skipsWithPrivacyOtp() {
        assertEquals(
            EligibilityDecision.Skip(SkipReason.PRIVACY_OTP),
            rules.evaluate(
                snapshot("123456"),
                settings(PrivacySettings(skipOtp = true)),
                allowedState,
            ),
        )
    }

    @Test
    fun evaluate_otpDetectionIsCaseInsensitive_skips() {
        assertEquals(
            EligibilityDecision.Skip(SkipReason.PRIVACY_OTP),
            rules.evaluate(
                snapshot("use this otp now"),
                settings(PrivacySettings(skipOtp = true)),
                allowedState,
            ),
        )
    }

    @Test
    fun evaluate_passwordTermPresent_skipsWithPrivacyPassword() {
        assertEquals(
            EligibilityDecision.Skip(SkipReason.PRIVACY_PASSWORD),
            rules.evaluate(
                snapshot("Your password was changed"),
                settings(PrivacySettings(skipPassword = true)),
                allowedState,
            ),
        )
    }

    @Test
    fun evaluate_paymentTermPresent_skipsWithPrivacyPayment() {
        assertEquals(
            EligibilityDecision.Skip(SkipReason.PRIVACY_PAYMENT),
            rules.evaluate(
                snapshot("Payment of Rs. 200 received"),
                settings(PrivacySettings(skipPayment = true)),
                allowedState,
            ),
        )
    }

    @Test
    fun evaluate_bankingTermPresent_skipsWithPrivacyBanking() {
        assertEquals(
            EligibilityDecision.Skip(SkipReason.PRIVACY_BANKING),
            rules.evaluate(
                snapshot("Account balance is Rs. 5,000"),
                settings(PrivacySettings(skipBanking = true)),
                allowedState,
            ),
        )
    }

    @Test
    fun evaluate_customSensitiveKeywordMatches_skipsWithPrivacySensitiveKeyword() {
        assertEquals(
            EligibilityDecision.Skip(SkipReason.PRIVACY_SENSITIVE_KEYWORD),
            rules.evaluate(
                snapshot("Project GREY is cancelled"),
                settings(PrivacySettings(sensitiveKeywords = listOf("grey"))),
                allowedState,
            ),
        )
    }

    @Test
    fun evaluate_otpAndPasswordBothPresent_reportsOtpFirst() {
        val privacy = PrivacySettings(skipOtp = true, skipPassword = true)
        assertEquals(
            EligibilityDecision.Skip(SkipReason.PRIVACY_OTP),
            rules.evaluate(
                snapshot("Your OTP: 4821 and your password is unchanged"),
                settings(privacy),
                allowedState,
            ),
        )
    }

    @Test
    fun evaluate_paymentAndBankingBothPresent_reportsPaymentFirst() {
        val privacy = PrivacySettings(skipPayment = true, skipBanking = true)
        assertEquals(
            EligibilityDecision.Skip(SkipReason.PRIVACY_PAYMENT),
            rules.evaluate(
                snapshot("Bank payment of Rs. 100 completed"),
                settings(privacy),
                allowedState,
            ),
        )
    }

    @Test
    fun evaluate_priorityAnnounceActiveAndListed_cannotBypassPrivacy() {
        val state = allowedState.copy(priorityAnnounceActive = true)
        assertEquals(
            EligibilityDecision.Skip(SkipReason.PRIVACY_OTP),
            rules.evaluate(
                snapshot("Your OTP is 991122"),
                settings(
                    PrivacySettings(skipOtp = true),
                ).copy(priorityAnnounceListed = true),
                state,
            ),
        )
    }

    @Test
    fun evaluate_mutedAndPrivacyMatch_reportsMutedFirst() {
        val state = allowedState.copy(temporaryMuteActive = true)
        assertEquals(
            EligibilityDecision.Skip(SkipReason.MUTED),
            rules.evaluate(
                snapshot("Your password was changed"),
                settings(PrivacySettings(skipPassword = true)),
                state,
            ),
        )
    }

    @Test
    fun evaluate_appDisabledAndPrivacyMatch_reportsAppDisabledFirst() {
        val disabledSettings = settings(PrivacySettings(excludeApp = true)).copy(appEnabled = false)
        assertEquals(
            EligibilityDecision.Skip(SkipReason.APP_DISABLED),
            rules.evaluate(snapshot("hello"), disabledSettings, allowedState),
        )
    }

    @Test
    fun evaluate_hideContentOnly_isNotASkipRule_announces() {
        assertEquals(
            EligibilityDecision.Announce,
            rules.evaluate(
                snapshot("any message body"),
                settings(PrivacySettings(hideContent = true)),
                allowedState,
            ),
        )
    }
}
