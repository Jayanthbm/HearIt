package com.jayanth.hearit.history

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jayanth.hearit.R
import com.jayanth.hearit.core.common.model.HistoryStatus
import com.jayanth.hearit.core.common.model.SkipReason

/** Visible label for a stored status; every status has its own string resource. */
@Composable
fun historyStatusLabel(status: HistoryStatus): String =
    stringResource(historyStatusLabelRes(status))

/** String resource for a stored status. */
@StringRes
fun historyStatusLabelRes(status: HistoryStatus): Int = when (status) {
    HistoryStatus.ANNOUNCED -> R.string.dashboard_history_announced
    HistoryStatus.SKIPPED -> R.string.dashboard_history_skipped
    HistoryStatus.QUEUED -> R.string.dashboard_history_queued
    HistoryStatus.PENDING -> R.string.dashboard_history_pending
    HistoryStatus.INTERRUPTED -> R.string.dashboard_history_interrupted
}

/** Visible label for a skip/dedup reason. */
@Composable
fun skipReasonLabel(reason: SkipReason): String = stringResource(skipReasonLabelRes(reason))

/**
 * String resource for a skip reason.
 *
 * Exhaustive on purpose: adding a reason to the domain must not silently fall back to a
 * generic "skipped", because the reason is the actionable part of a history row.
 */
@StringRes
fun skipReasonLabelRes(reason: SkipReason): Int = when (reason) {
    SkipReason.APP_DISABLED -> R.string.history_reason_app_disabled
    SkipReason.MUTED -> R.string.history_reason_muted
    SkipReason.SCREEN_ON -> R.string.history_reason_screen_on
    SkipReason.SCREEN_OFF -> R.string.history_reason_screen_off
    SkipReason.UNLOCKED -> R.string.history_reason_unlocked
    SkipReason.LOCKED -> R.string.history_reason_locked
    SkipReason.TOO_LONG -> R.string.history_reason_too_long
    SkipReason.KEYWORD_FILTERED -> R.string.history_reason_keyword
    SkipReason.PRIVACY_EXCLUDED_APP -> R.string.history_reason_privacy_app
    SkipReason.PRIVACY_OTP -> R.string.history_reason_privacy_otp
    SkipReason.PRIVACY_PASSWORD -> R.string.history_reason_privacy_password
    SkipReason.PRIVACY_PAYMENT -> R.string.history_reason_privacy_payment
    SkipReason.PRIVACY_BANKING -> R.string.history_reason_privacy_banking
    SkipReason.PRIVACY_SENSITIVE_KEYWORD -> R.string.history_reason_privacy_sensitive
    SkipReason.CALL_ACTIVE -> R.string.history_reason_call
    SkipReason.DND -> R.string.history_reason_dnd
    SkipReason.OUTPUT_NOT_ALLOWED -> R.string.history_reason_output
    SkipReason.EXPIRED -> R.string.history_reason_expired
    SkipReason.OVERFLOW -> R.string.history_reason_overflow
    SkipReason.DUPLICATE_UPDATE -> R.string.history_reason_duplicate
    SkipReason.SUPPRESSED_INTELLIGENCE -> R.string.history_reason_suppressed_intelligence
    SkipReason.LOW_BATTERY -> R.string.history_reason_low_battery
    SkipReason.PROXIMITY_UNCOVERED -> R.string.history_reason_proximity_uncovered
    SkipReason.SPAM_COOLDOWN -> R.string.history_reason_spam_cooldown
    SkipReason.MANUALLY_CLEARED -> R.string.history_reason_manually_cleared
    SkipReason.SERVICE_NOT_RUNNING -> R.string.history_reason_service_not_running
}
