package com.jayanth.hearit.applications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Cable
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayanth.hearit.R
import com.jayanth.hearit.core.common.model.EligibilitySettings
import com.jayanth.hearit.core.common.model.KeywordFilterSettings
import com.jayanth.hearit.core.common.model.KeywordMatchMode
import com.jayanth.hearit.core.common.model.LengthScope
import com.jayanth.hearit.core.common.model.LockCondition
import com.jayanth.hearit.core.common.model.PrivacySettings
import com.jayanth.hearit.core.common.model.ScreenCondition
import com.jayanth.hearit.core.common.model.TextLengthSettings
import com.jayanth.hearit.core.datastore.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Global Rules settings ViewModel (phase P15).
 *
 * Single-writer over `SettingsRepository`: every control writes its group immediately, and
 * the form state is rebuilt from the persisted settings flow, so the screen cannot drift
 * from what the pipeline actually reads (ADR-021).
 */
@HiltViewModel
class GlobalSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(GlobalSettingsUiState())
    val state: StateFlow<GlobalSettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.value = GlobalSettingsUiState.from(settings)
            }
        }
    }

    fun onScreenConditionChanged(condition: ScreenCondition) =
        updateEligibility { it.copy(screenCondition = condition) }

    fun onLockConditionChanged(condition: LockCondition) =
        updateEligibility { it.copy(lockCondition = condition) }

    fun onTextLengthChanged(textLength: TextLengthSettings) =
        updateEligibility { it.copy(textLength = textLength) }

    fun onKeywordFilterChanged(keywordFilter: KeywordFilterSettings) =
        updateEligibility { it.copy(keywordFilter = keywordFilter) }

    fun onPrivacyChanged(privacy: PrivacySettings) =
        updateEligibility { it.copy(privacy = privacy) }

    fun onAudioOutputChanged(allowWired: Boolean? = null, allowBluetooth: Boolean? = null, allowPhoneSpeaker: Boolean? = null) =
        updateEligibility {
            it.copy(
                audioOutput = it.audioOutput.copy(
                    allowWired = allowWired ?: it.audioOutput.allowWired,
                    allowBluetooth = allowBluetooth ?: it.audioOutput.allowBluetooth,
                    allowPhoneSpeaker = allowPhoneSpeaker ?: it.audioOutput.allowPhoneSpeaker,
                ),
            )
        }

    fun onResetToDefaults() {
        viewModelScope.launch {
            settingsRepository.update { settings ->
                settings.copy(eligibility = EligibilitySettings())
            }
        }
    }

    fun onEligibilityChanged(updated: EligibilitySettings) =
        updateEligibility { updated }

    private fun updateEligibility(transform: (EligibilitySettings) -> EligibilitySettings) {
        viewModelScope.launch {
            settingsRepository.update { settings ->
                settings.copy(eligibility = transform(settings.eligibility))
            }
        }
    }
}

/** Entry point of Global Rules (phase P15). */
@Composable
fun GlobalSettingsRoute(
    viewModel: GlobalSettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()

    GlobalSettingsScreen(
        state = state,
        onScreenConditionChanged = viewModel::onScreenConditionChanged,
        onLockConditionChanged = viewModel::onLockConditionChanged,
        onTextLengthChanged = viewModel::onTextLengthChanged,
        onKeywordFilterChanged = viewModel::onKeywordFilterChanged,
        onPrivacyChanged = viewModel::onPrivacyChanged,
        onAudioOutputChanged = viewModel::onAudioOutputChanged,
        onEligibilityChanged = viewModel::onEligibilityChanged,
        onResetToDefaults = viewModel::onResetToDefaults,
        modifier = modifier,
    )
}

/**
 * Modern Material 3 Expressive Global Rules Screen.
 */
@Composable
fun GlobalSettingsScreen(
    state: GlobalSettingsUiState,
    onScreenConditionChanged: (ScreenCondition) -> Unit,
    onLockConditionChanged: (LockCondition) -> Unit,
    onTextLengthChanged: (TextLengthSettings) -> Unit = {},
    onKeywordFilterChanged: (KeywordFilterSettings) -> Unit,
    onPrivacyChanged: (PrivacySettings) -> Unit,
    onResetToDefaults: () -> Unit,
    modifier: Modifier = Modifier,
    onAudioOutputChanged: (Boolean?, Boolean?, Boolean?) -> Unit = { _, _, _ -> },
    onEligibilityChanged: (EligibilitySettings) -> Unit = {},
) {
    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Hero Card with un-truncated FlowRow status pills
        GlobalRulesHeroCard(state = state)

        val isDeviceStateModified = state.screenCondition != ScreenCondition.OFF || state.lockCondition != LockCondition.LOCKED
        val activeShieldsCount = listOf(
            state.privacy.skipOtp,
            state.privacy.skipPassword,
            state.privacy.skipBanking,
            state.privacy.skipPayment,
            state.privacy.hideContent,
        ).count { it }

        // Section 1: Device State Conditions
        DeviceStateCard(
            screenCondition = state.screenCondition,
            lockCondition = state.lockCondition,
            isModified = isDeviceStateModified,
            onScreenConditionChanged = onScreenConditionChanged,
            onLockConditionChanged = onLockConditionChanged,
        )

        // Section 2: Privacy Protection & Content Shields (with keyboard bringIntoView)
        PrivacyShieldCard(
            privacy = state.privacy,
            activeCount = activeShieldsCount,
            onPrivacyChanged = onPrivacyChanged,
        )

        // Section 3: Global Keyword Filtering UI
        GlobalKeywordFilterCard(
            keywordFilter = state.keywordFilter,
            onKeywordFilterChanged = onKeywordFilterChanged,
        )

        // Section 4: Critical Breakthrough, Low Battery Safeguard & Pocket Detection
        CriticalBreakthroughCard(
            eligibility = state.eligibility,
            onEligibilityChanged = onEligibilityChanged,
        )

        LowBatterySafeguardCard(
            eligibility = state.eligibility,
            onEligibilityChanged = onEligibilityChanged,
        )

        PocketDetectionCard(
            eligibility = state.eligibility,
            onEligibilityChanged = onEligibilityChanged,
        )

        // Reset to Defaults Button
        OutlinedButton(
            onClick = { showResetDialog = true },
            enabled = state.hasOverrides,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Icon(
                imageVector = if (state.hasOverrides) Icons.Rounded.RestartAlt else Icons.Rounded.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (state.hasOverrides) {
                    stringResource(R.string.global_rules_reset)
                } else {
                    "All Global Rules at Default"
                },
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(modifier = Modifier.height(120.dp))
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.global_rules_reset_confirm_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.global_rules_reset_confirm_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        onResetToDefaults()
                    },
                ) {
                    Text(text = stringResource(R.string.settings_reset), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

// ============================================================================
// Hero Header Card
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GlobalRulesHeroCard(state: GlobalSettingsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.global_settings_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (state.hasOverrides) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Text(
                                    text = "Modified",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.global_settings_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // Active Policy Summary Chips in FlowRow - No Truncation!
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Screen Status Pill
                val (screenIcon, screenText, screenPositive) = when (state.screenCondition) {
                    ScreenCondition.OFF -> Triple(Icons.Rounded.Nightlight, "Screen Off", true)
                    ScreenCondition.ON -> Triple(Icons.Rounded.WbSunny, "Screen On", false)
                    ScreenCondition.BOTH -> Triple(Icons.Rounded.Sync, "Screen: Any", false)
                }
                StatusPill(icon = screenIcon, text = screenText, isPositive = screenPositive)

                // Lock Status Pill
                val (lockIcon, lockText, lockPositive) = when (state.lockCondition) {
                    LockCondition.LOCKED -> Triple(Icons.Rounded.Lock, "Device Locked", true)
                    LockCondition.UNLOCKED -> Triple(Icons.Rounded.LockOpen, "Unlocked", false)
                    LockCondition.BOTH -> Triple(Icons.Rounded.Sync, "Lock: Any", false)
                }
                StatusPill(icon = lockIcon, text = lockText, isPositive = lockPositive)

                // Privacy Shields Status Pill
                val activeShields = listOf(
                    state.privacy.skipOtp,
                    state.privacy.skipPassword,
                    state.privacy.skipBanking,
                    state.privacy.skipPayment,
                    state.privacy.hideContent,
                ).count { it }
                val (shieldIcon, shieldText, shieldPositive) = when {
                    activeShields == 5 -> Triple(Icons.Rounded.Shield, "All 5 Shields On", true)
                    activeShields > 0 -> Triple(Icons.Rounded.Shield, "$activeShields Shields Active", true)
                    else -> Triple(Icons.Rounded.Shield, "Shields Off", false)
                }
                StatusPill(icon = shieldIcon, text = shieldText, isPositive = shieldPositive)
            }
        }
    }
}

@Composable
private fun StatusPill(
    icon: ImageVector,
    text: String,
    isPositive: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (isPositive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, if (isPositive) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isPositive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

// ============================================================================
// Section 1: Device State Conditions
// ============================================================================

@Composable
private fun DeviceStateCard(
    screenCondition: ScreenCondition,
    lockCondition: LockCondition,
    isModified: Boolean,
    onScreenConditionChanged: (ScreenCondition) -> Unit,
    onLockConditionChanged: (LockCondition) -> Unit,
) {
    SectionCard(
        title = "Device State",
        subtitle = "Prerequisites for screen and lock states",
        icon = Icons.Rounded.PhoneAndroid,
        badgeText = if (isModified) "Modified" else "Default",
        isBadgeHighlighted = isModified,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Screen Condition
            Column {
                Text(
                    text = stringResource(R.string.global_screen_condition),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SegmentOption(
                        label = stringResource(R.string.global_screen_off),
                        subtitle = "Recommended",
                        icon = Icons.Rounded.Nightlight,
                        selected = screenCondition == ScreenCondition.OFF,
                        onClick = { onScreenConditionChanged(ScreenCondition.OFF) },
                        modifier = Modifier.weight(1f),
                    )
                    SegmentOption(
                        label = stringResource(R.string.global_screen_on),
                        subtitle = "Awake only",
                        icon = Icons.Rounded.WbSunny,
                        selected = screenCondition == ScreenCondition.ON,
                        onClick = { onScreenConditionChanged(ScreenCondition.ON) },
                        modifier = Modifier.weight(1f),
                    )
                    SegmentOption(
                        label = stringResource(R.string.global_screen_both),
                        subtitle = "Either state",
                        icon = Icons.Rounded.Sync,
                        selected = screenCondition == ScreenCondition.BOTH,
                        onClick = { onScreenConditionChanged(ScreenCondition.BOTH) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Lock Condition
            Column {
                Text(
                    text = stringResource(R.string.global_lock_condition),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SegmentOption(
                        label = stringResource(R.string.global_lock_locked),
                        subtitle = "Recommended",
                        icon = Icons.Rounded.Lock,
                        selected = lockCondition == LockCondition.LOCKED,
                        onClick = { onLockConditionChanged(LockCondition.LOCKED) },
                        modifier = Modifier.weight(1f),
                    )
                    SegmentOption(
                        label = stringResource(R.string.global_lock_unlocked),
                        subtitle = "Unlocked",
                        icon = Icons.Rounded.LockOpen,
                        selected = lockCondition == LockCondition.UNLOCKED,
                        onClick = { onLockConditionChanged(LockCondition.UNLOCKED) },
                        modifier = Modifier.weight(1f),
                    )
                    SegmentOption(
                        label = stringResource(R.string.global_lock_both),
                        subtitle = "Either state",
                        icon = Icons.Rounded.Sync,
                        selected = lockCondition == LockCondition.BOTH,
                        onClick = { onLockConditionChanged(LockCondition.BOTH) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

// ============================================================================
// Section 2: Privacy Protection & Content Shields (with keyboard bringIntoView)
// ============================================================================

@Composable
private fun PrivacyShieldCard(
    privacy: PrivacySettings,
    activeCount: Int,
    onPrivacyChanged: (PrivacySettings) -> Unit,
) {
    var activeShieldInfo by remember { mutableStateOf<Pair<String, String>?>(null) }

    SectionCard(
        title = "Privacy Shields",
        subtitle = "Redact sensitive content like OTPs and balances",
        icon = Icons.Rounded.Security,
        badgeText = "$activeCount Active",
        isBadgeHighlighted = activeCount > 0,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // OTP Skip
            ModernSwitchRow(
                icon = Icons.Rounded.Shield,
                label = stringResource(R.string.global_privacy_skip_otp),
                subtitle = stringResource(R.string.global_privacy_skip_otp_desc),
                checked = privacy.skipOtp,
                badgeText = if (privacy.skipOtp) "ACTIVE" else null,
                onCheckedChange = { onPrivacyChanged(privacy.copy(skipOtp = it)) },
                onInfoClick = {
                    activeShieldInfo = "OTP & Verification Codes Shield" to
                        "Automatically detects 4-8 digit numeric codes, one-time passwords, and 2-step verification notifications (e.g. Google, WhatsApp, SMS, bank logins).\n\nWhen detected, Hear It silences the announcement completely to prevent anyone nearby from overhearing your private verification codes."
                },
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Password Skip
            ModernSwitchRow(
                icon = Icons.Rounded.Key,
                label = stringResource(R.string.global_privacy_skip_password),
                subtitle = stringResource(R.string.global_privacy_skip_password_desc),
                checked = privacy.skipPassword,
                badgeText = if (privacy.skipPassword) "ACTIVE" else null,
                onCheckedChange = { onPrivacyChanged(privacy.copy(skipPassword = it)) },
                onInfoClick = {
                    activeShieldInfo = "Password & Credentials Shield" to
                        "Detects notifications referencing passwords, PINs, temporary access tokens, or account credentials.\n\nMutes the notification so sensitive access keys are never announced in public."
                },
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Banking Skip
            ModernSwitchRow(
                icon = Icons.Rounded.AccountBalance,
                label = stringResource(R.string.global_privacy_skip_banking),
                subtitle = stringResource(R.string.global_privacy_skip_banking_desc),
                checked = privacy.skipBanking,
                badgeText = if (privacy.skipBanking) "ACTIVE" else null,
                onCheckedChange = { onPrivacyChanged(privacy.copy(skipBanking = it)) },
                onInfoClick = {
                    activeShieldInfo = "Banking & Account Alerts Shield" to
                        "Scans for financial statements, bank account numbers, credited/debited alerts, and ATM withdrawals.\n\nKeeps your bank balances and account numbers strictly confidential."
                },
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Payment Skip
            ModernSwitchRow(
                icon = Icons.Rounded.CreditCard,
                label = stringResource(R.string.global_privacy_skip_payment),
                subtitle = stringResource(R.string.global_privacy_skip_payment_desc),
                checked = privacy.skipPayment,
                badgeText = if (privacy.skipPayment) "ACTIVE" else null,
                onCheckedChange = { onPrivacyChanged(privacy.copy(skipPayment = it)) },
                onInfoClick = {
                    activeShieldInfo = "Payment Receipts Shield" to
                        "Silences purchase confirmations, digital wallet transactions, UPI payments (Google Pay, PhonePe, Paytm), and credit/debit receipts.\n\nMutes transaction amounts and merchant confirmations."
                },
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Hide Content
            ModernSwitchRow(
                icon = Icons.Rounded.VisibilityOff,
                label = stringResource(R.string.global_privacy_hide_content),
                subtitle = stringResource(R.string.global_privacy_hide_content_desc),
                checked = privacy.hideContent,
                badgeText = if (privacy.hideContent) "ACTIVE" else null,
                onCheckedChange = { onPrivacyChanged(privacy.copy(hideContent = it)) },
                onInfoClick = {
                    activeShieldInfo = "Hide Notification Message Body" to
                        "Only speaks the App Name and Sender (e.g. 'WhatsApp message from Alice').\n\nThe message content is never read aloud, ensuring complete privacy in public or shared spaces."
                },
            )
        }
    }

    activeShieldInfo?.let { (infoTitle, infoBody) ->
        AlertDialog(
            onDismissRequest = { activeShieldInfo = null },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            },
            title = {
                Text(
                    text = infoTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            text = {
                Text(
                    text = infoBody,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = { activeShieldInfo = null }) {
                    Text("Got It", fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}

// ============================================================================
// Shared Material 3 Expressive UI Helpers
// ============================================================================

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    isBadgeHighlighted: Boolean = false,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                    )
                }
                if (badgeText != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isBadgeHighlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isBadgeHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            content()
        }
    }
}

@Composable
private fun SegmentOption(
    label: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Surface(
        modifier = modifier
            .clip(shape)
            .clickable(role = Role.RadioButton, onClick = onClick),
        shape = shape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ModernSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null,
    subtitle: String? = null,
    badgeText: String? = null,
    onInfoClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Switch) { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (icon != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (checked) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null,
                    modifier = Modifier.size(32.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (badgeText != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(10.dp),
                                )
                                Text(
                                    text = badgeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                    if (onInfoClick != null) {
                        IconButton(
                            onClick = onInfoClick,
                            modifier = Modifier.size(20.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = "Explain $label",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                }
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                    )
                }
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

/** Splits comma-separated strings cleanly. */
private fun splitList(raw: String): List<String> =
    raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GlobalKeywordFilterCard(
    keywordFilter: KeywordFilterSettings,
    onKeywordFilterChanged: (KeywordFilterSettings) -> Unit,
) {
    var newKeywordText by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header with Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.FilterList,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Global Keyword Filters",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Silence alerts containing these words",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Switch(
                    checked = keywordFilter.enabled,
                    onCheckedChange = { isChecked ->
                        onKeywordFilterChanged(keywordFilter.copy(enabled = isChecked))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }

            if (keywordFilter.enabled) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Mode Selector
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        KeywordMatchMode.WHOLE_WORD to "Word",
                        KeywordMatchMode.EXACT to "Exact",
                        KeywordMatchMode.PHRASE to "Phrase",
                    ).forEachIndexed { index, (mode, label) ->
                        SegmentedButton(
                            selected = keywordFilter.mode == mode,
                            onClick = { onKeywordFilterChanged(keywordFilter.copy(mode = mode)) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = 3),
                        ) {
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Add Keyword Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = newKeywordText,
                        onValueChange = { newKeywordText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("e.g. promo, sale, otp", fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                    )
                    Button(
                        onClick = {
                            val trimmed = newKeywordText.trim()
                            if (trimmed.isNotEmpty() && trimmed !in keywordFilter.keywords) {
                                onKeywordFilterChanged(
                                    keywordFilter.copy(keywords = keywordFilter.keywords + trimmed),
                                )
                                newKeywordText = ""
                            }
                        },
                        enabled = newKeywordText.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Add")
                    }
                }

                // Keyword chips
                if (keywordFilter.keywords.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        keywordFilter.keywords.forEach { kw ->
                            InputChip(
                                selected = true,
                                onClick = {},
                                label = { Text(kw) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable {
                                                onKeywordFilterChanged(
                                                    keywordFilter.copy(keywords = keywordFilter.keywords - kw),
                                                )
                                            },
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
