package com.jayanth.hearit.core.common.model

/**
 * Privacy controls (master prompt §6, "Privacy"), usable as the global value and as a
 * per-app group override inside [EligibilitySettings].
 *
 * Detection combines notification metadata and text heuristics and is explicitly
 * **imperfect** — the product never claims perfect semantic detection. Every skip reason
 * is specific so history explains what triggered the rule.
 *
 * Defaults follow ADR-015: the two high-precision rules ship **on** (OTP, password) so a
 * hands-free app never reads a code aloud out of the box, while the low-precision rules
 * (banking, payment) ship **off** because they routinely hit routine alerts users want to
 * hear. Keyword filtering remains off as mandated by §6.
 *
 * @property excludeApp Never announce this app at all, even when it is enabled. Meaningful
 *   as a per-app override; as a global value it would exclude every app. Default off.
 * @property skipOtp Skip OTP/verification-code content. Default **on** (ADR-015).
 * @property skipBanking Skip banking/financial notifications. Default off.
 * @property skipPassword Skip password-related notifications. Default **on** (ADR-015).
 * @property skipPayment Skip payment/transaction alerts. Default off.
 * @property hideContent Hide the message body and announce app/sender only. This is **not**
 *   a skip rule (ADR-006 step 6): eligibility still returns `Announce`, and the
 *   announcement template (P04) omits the body. Default off.
 * @property sensitiveKeywords Custom sensitive keywords/phrases; matched case-insensitively
 *   as whole words (the same script-aware rule as [KeywordFilterSettings]).
 */
data class PrivacySettings(
    val excludeApp: Boolean = false,
    val skipOtp: Boolean = true,
    val skipBanking: Boolean = false,
    val skipPassword: Boolean = true,
    val skipPayment: Boolean = false,
    val hideContent: Boolean = false,
    val sensitiveKeywords: List<String> = emptyList(),
)
