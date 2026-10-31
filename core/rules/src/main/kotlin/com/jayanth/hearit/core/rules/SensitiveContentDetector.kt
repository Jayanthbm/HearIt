package com.jayanth.hearit.core.rules

import com.jayanth.hearit.core.common.model.KeywordMatchMode
import com.jayanth.hearit.core.common.model.NotificationSnapshot
import com.jayanth.hearit.core.common.model.PrivacySettings
import com.jayanth.hearit.core.common.model.SkipReason

/**
 * Privacy-rule detection (master prompt §6, "Privacy"): combines text heuristics with the
 * user's own rules. Detection is deliberately **conservative and imperfect** — the product
 * never promises perfect semantic categorization, and the built-in term lists only apply
 * when the corresponding toggle is on (OTP and password ship on by default, banking and
 * payment off — ADR-015).
 *
 * Sub-order within ADR-006 step 5 (first hit wins, so history is unambiguous):
 * excluded app → OTP → password → payment → banking → custom sensitive keyword.
 *
 * `hideContent` is intentionally not handled here: it is not a skip rule (ADR-006 step 6),
 * it changes announcement content and belongs to the template builder in P04.
 */
object SensitiveContentDetector {

    /** Terms that indicate OTP/verification-code content (whole-word match). */
    private val OTP_TERMS = listOf(
        "otp",
        "one time password",
        "one time code",
        "verification code",
        "verify code",
        "confirmation code",
        "security code",
        "authentication code",
        "auth code",
        "passcode",
        "login code",
        "access code",
        "2fa",
        "2 step verification",
    )

    private val PASSWORD_TERMS = listOf(
        "password",
        "passwd",
        "passcode",
        "credential",
    )

    private val PAYMENT_TERMS = listOf(
        "payment",
        "transaction",
        "upi",
        "debited",
        "refund",
        "invoice",
        "purchase",
    )

    private val BANKING_TERMS = listOf(
        "bank",
        "banking",
        "account balance",
        "mini statement",
        "deposit",
        "withdrawal",
        "atm",
        "cheque",
        "ifsc",
        "kyc",
        "loan",
    )

    /** A message that consists of nothing but a 4-8 digit code is treated as an OTP. */
    private val CODE_ONLY = Regex("^\\d{4,8}$")

    /**
     * Returns the first privacy skip reason for [snapshot] under [privacy], or null when
     * the content may be announced (possibly with hidden body — decided by P04).
     */
    fun detect(snapshot: NotificationSnapshot, privacy: PrivacySettings): SkipReason? {
        if (privacy.excludeApp) {
            return SkipReason.PRIVACY_EXCLUDED_APP
        }

        val text = snapshot.messageText

        if (privacy.skipOtp && looksLikeOtp(text)) {
            return SkipReason.PRIVACY_OTP
        }
        if (privacy.skipPassword && KeywordMatcher.matchesAny(text, PASSWORD_TERMS, KeywordMatchMode.WHOLE_WORD)) {
            return SkipReason.PRIVACY_PASSWORD
        }
        if (privacy.skipPayment && KeywordMatcher.matchesAny(text, PAYMENT_TERMS, KeywordMatchMode.WHOLE_WORD)) {
            return SkipReason.PRIVACY_PAYMENT
        }
        if (privacy.skipBanking && KeywordMatcher.matchesAny(text, BANKING_TERMS, KeywordMatchMode.WHOLE_WORD)) {
            return SkipReason.PRIVACY_BANKING
        }
        if (privacy.sensitiveKeywords.any { it.isNotBlank() } &&
            KeywordMatcher.matchesAny(text, privacy.sensitiveKeywords, KeywordMatchMode.WHOLE_WORD)
        ) {
            return SkipReason.PRIVACY_SENSITIVE_KEYWORD
        }

        return null
    }

    /**
     * OTP heuristic: an explicit OTP term appears in the text, or the whole message is
     * just a numeric code (e.g. the sender posted only "482193"). False positives are
     * accepted in exchange for never reading a code aloud; false negatives are covered by
     * the user's custom sensitive keywords.
     */
    private fun looksLikeOtp(text: String): Boolean {
        if (KeywordMatcher.matchesAny(text, OTP_TERMS, KeywordMatchMode.WHOLE_WORD)) {
            return true
        }
        return CODE_ONLY.matches(text.trim())
    }
}
