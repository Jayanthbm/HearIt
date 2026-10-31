package com.jayanth.hearit.core.common.model

/**
 * Privacy controls (master prompt §6, "Privacy"), usable as the global value and as a
 * per-app group override inside [EligibilitySettings].
 *
 * Detection combines notification metadata and text heuristics and is explicitly
 * **imperfect** — the product never claims perfect semantic detection. Every skip reason
 * is specific so history explains what triggered the rule.
 *
 * All toggles default to **off**: the master prompt lists the available controls but does
 * not state shipping defaults for them (only keyword filtering's default is specified),
 * so no privacy behaviour is invented here — see the open question in the cloud progress
 * file before enabling anything by default in P15.
 *
 * @property excludeApp Never announce this app at all, even when it is enabled. Meaningful
 *   as a per-app override; as a global value it would exclude every app.
 * @property skipOtp Skip OTP/verification-code content.
 * @property skipBanking Skip banking/financial notifications.
 * @property skipPassword Skip password-related notifications.
 * @property skipPayment Skip payment/transaction alerts.
 * @property hideContent Hide the message body and announce app/sender only. This is **not**
 *   a skip rule (ADR-006 step 5): eligibility still returns `Announce`, and the
 *   announcement template (P04) omits the body.
 * @property sensitiveKeywords Custom sensitive keywords/phrases; matched case-insensitively
 *   as whole words (the same script-aware rule as [KeywordFilterSettings]).
 */
data class PrivacySettings(
    val excludeApp: Boolean = false,
    val skipOtp: Boolean = false,
    val skipBanking: Boolean = false,
    val skipPassword: Boolean = false,
    val skipPayment: Boolean = false,
    val hideContent: Boolean = false,
    val sensitiveKeywords: List<String> = emptyList(),
)
