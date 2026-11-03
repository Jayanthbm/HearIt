package com.jayanth.hearit.applications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Cable
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Global Rules, Voice & Speech settings (phase P15).
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

    private fun updateEligibility(transform: (EligibilitySettings) -> EligibilitySettings) {
        viewModelScope.launch {
            settingsRepository.update { settings ->
                settings.copy(eligibility = transform(settings.eligibility))
            }
        }
    }
}

/** Entry point of the Global Rules, Voice & Speech settings (phase P15). */
@Composable
fun GlobalSettingsRoute(
    viewModel: GlobalSettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Description Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.global_settings_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.global_settings_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                )
            }
        }

        // Section: Screen & Lock Condition
        SectionCard(
            title = "Device State Conditions",
            icon = Icons.Rounded.PhoneAndroid,
        ) {
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
                ChipOption(
                    label = stringResource(R.string.global_screen_off),
                    selected = state.screenCondition == ScreenCondition.OFF,
                    onClick = { viewModel.onScreenConditionChanged(ScreenCondition.OFF) },
                    modifier = Modifier.weight(1f),
                )
                ChipOption(
                    label = stringResource(R.string.global_screen_on),
                    selected = state.screenCondition == ScreenCondition.ON,
                    onClick = { viewModel.onScreenConditionChanged(ScreenCondition.ON) },
                    modifier = Modifier.weight(1f),
                )
                ChipOption(
                    label = stringResource(R.string.global_screen_both),
                    selected = state.screenCondition == ScreenCondition.BOTH,
                    onClick = { viewModel.onScreenConditionChanged(ScreenCondition.BOTH) },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                ChipOption(
                    label = stringResource(R.string.global_lock_locked),
                    selected = state.lockCondition == LockCondition.LOCKED,
                    onClick = { viewModel.onLockConditionChanged(LockCondition.LOCKED) },
                    modifier = Modifier.weight(1f),
                )
                ChipOption(
                    label = stringResource(R.string.global_lock_unlocked),
                    selected = state.lockCondition == LockCondition.UNLOCKED,
                    onClick = { viewModel.onLockConditionChanged(LockCondition.UNLOCKED) },
                    modifier = Modifier.weight(1f),
                )
                ChipOption(
                    label = stringResource(R.string.global_lock_both),
                    selected = state.lockCondition == LockCondition.BOTH,
                    onClick = { viewModel.onLockConditionChanged(LockCondition.BOTH) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Section: Text Length Limits
        SectionCard(
            title = "Text Length Limits",
            icon = Icons.Rounded.Straighten,
        ) {
            OutlinedTextField(
                value = state.textLength.maxLengthChars?.toString() ?: "",
                onValueChange = { raw ->
                    viewModel.onTextLengthChanged(
                        state.textLength.copy(maxLengthChars = raw.toIntOrNull()?.takeIf { it > 0 }),
                    )
                },
                label = { Text(text = stringResource(R.string.global_max_length)) },
                placeholder = { Text("No limit") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Apply limit to",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ChipOption(
                    label = stringResource(R.string.global_scope_message),
                    selected = state.textLength.scope == LengthScope.MESSAGE,
                    onClick = { viewModel.onTextLengthChanged(state.textLength.copy(scope = LengthScope.MESSAGE)) },
                    modifier = Modifier.weight(1f),
                )
                ChipOption(
                    label = stringResource(R.string.global_scope_whole_notification),
                    selected = state.textLength.scope == LengthScope.WHOLE_NOTIFICATION,
                    onClick = { viewModel.onTextLengthChanged(state.textLength.copy(scope = LengthScope.WHOLE_NOTIFICATION)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Section: Keyword Filters
        SectionCard(
            title = "Keyword Filtering",
            icon = Icons.Rounded.FilterList,
        ) {
            ModernSwitchRow(
                label = stringResource(R.string.global_keywords_enabled),
                checked = state.keywordFilter.enabled,
                onCheckedChange = { enabled ->
                    viewModel.onKeywordFilterChanged(state.keywordFilter.copy(enabled = enabled))
                },
            )

            if (state.keywordFilter.enabled) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ChipOption(
                        label = stringResource(R.string.global_keyword_mode_whole_word),
                        selected = state.keywordFilter.mode == KeywordMatchMode.WHOLE_WORD,
                        onClick = { viewModel.onKeywordFilterChanged(state.keywordFilter.copy(mode = KeywordMatchMode.WHOLE_WORD)) },
                        modifier = Modifier.weight(1f),
                    )
                    ChipOption(
                        label = stringResource(R.string.global_keyword_mode_phrase),
                        selected = state.keywordFilter.mode == KeywordMatchMode.PHRASE,
                        onClick = { viewModel.onKeywordFilterChanged(state.keywordFilter.copy(mode = KeywordMatchMode.PHRASE)) },
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.keywordFilter.keywords.joinToString(", "),
                    onValueChange = { raw ->
                        viewModel.onKeywordFilterChanged(
                            state.keywordFilter.copy(keywords = splitList(raw)),
                        )
                    },
                    label = { Text(text = stringResource(R.string.global_keywords_keywords)) },
                    placeholder = { Text("Comma-separated words…") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Section: Privacy Protection
        SectionCard(
            title = "Privacy & Content Shields",
            icon = Icons.Rounded.Security,
        ) {
            ModernSwitchRow(
                label = stringResource(R.string.global_privacy_exclude_app),
                checked = state.privacy.excludeApp,
                onCheckedChange = { viewModel.onPrivacyChanged(state.privacy.copy(excludeApp = it)) },
            )
            ModernSwitchRow(
                label = stringResource(R.string.global_privacy_skip_otp),
                checked = state.privacy.skipOtp,
                onCheckedChange = { viewModel.onPrivacyChanged(state.privacy.copy(skipOtp = it)) },
            )
            ModernSwitchRow(
                label = stringResource(R.string.global_privacy_skip_password),
                checked = state.privacy.skipPassword,
                onCheckedChange = { viewModel.onPrivacyChanged(state.privacy.copy(skipPassword = it)) },
            )
            ModernSwitchRow(
                label = stringResource(R.string.global_privacy_skip_banking),
                checked = state.privacy.skipBanking,
                onCheckedChange = { viewModel.onPrivacyChanged(state.privacy.copy(skipBanking = it)) },
            )
            ModernSwitchRow(
                label = stringResource(R.string.global_privacy_skip_payment),
                checked = state.privacy.skipPayment,
                onCheckedChange = { viewModel.onPrivacyChanged(state.privacy.copy(skipPayment = it)) },
            )
            ModernSwitchRow(
                label = stringResource(R.string.global_privacy_hide_content),
                checked = state.privacy.hideContent,
                onCheckedChange = { viewModel.onPrivacyChanged(state.privacy.copy(hideContent = it)) },
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.privacy.sensitiveKeywords.joinToString(", "),
                onValueChange = { raw ->
                    viewModel.onPrivacyChanged(state.privacy.copy(sensitiveKeywords = splitList(raw)))
                },
                label = { Text(text = stringResource(R.string.global_privacy_sensitive_keywords)) },
                placeholder = { Text("otp, pin, passcode…") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Section: Audio Output Fallback Policies
        SectionCard(
            title = "Allowed Output Channels",
            icon = Icons.Rounded.Cable,
        ) {
            ModernSwitchRow(
                label = stringResource(R.string.global_audio_allow_bluetooth),
                checked = state.audioOutput.allowBluetooth,
                onCheckedChange = { viewModel.onAudioOutputChanged(allowBluetooth = it) },
            )
            ModernSwitchRow(
                label = stringResource(R.string.global_audio_allow_wired),
                checked = state.audioOutput.allowWired,
                onCheckedChange = { viewModel.onAudioOutputChanged(allowWired = it) },
            )
            ModernSwitchRow(
                label = stringResource(R.string.global_audio_allow_speaker),
                checked = state.audioOutput.allowPhoneSpeaker,
                onCheckedChange = { viewModel.onAudioOutputChanged(allowPhoneSpeaker = it) },
            )
        }

        Text(
            text = stringResource(R.string.global_settings_apply_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 14.dp),
            ) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            content()
        }
    }
}

@Composable
private fun ChipOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
            )
        },
        shape = RoundedCornerShape(10.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.primary,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        modifier = modifier,
    )
}

@Composable
private fun ModernSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(end = 12.dp),
        )
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

/** Splits the comma-separated text fields the same way the codec stores lists. */
private fun splitList(raw: String): List<String> =
    raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
