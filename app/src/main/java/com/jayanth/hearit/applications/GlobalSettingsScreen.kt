package com.jayanth.hearit.applications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = stringResource(R.string.global_settings_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.global_settings_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))
        ChoiceRow(
            labelRes = R.string.global_screen_condition,
            options = listOf(
                R.string.global_screen_on to (state.screenCondition == ScreenCondition.ON),
                R.string.global_screen_off to (state.screenCondition == ScreenCondition.OFF),
                R.string.global_screen_both to (state.screenCondition == ScreenCondition.BOTH),
            ),
            onSelect = { index ->
                viewModel.onScreenConditionChanged(
                    when (index) {
                        0 -> ScreenCondition.ON
                        1 -> ScreenCondition.OFF
                        else -> ScreenCondition.BOTH
                    },
                )
            },
        )

        ChoiceRow(
            labelRes = R.string.global_lock_condition,
            options = listOf(
                R.string.global_lock_locked to (state.lockCondition == LockCondition.LOCKED),
                R.string.global_lock_unlocked to (state.lockCondition == LockCondition.UNLOCKED),
                R.string.global_lock_both to (state.lockCondition == LockCondition.BOTH),
            ),
            onSelect = { index ->
                viewModel.onLockConditionChanged(
                    when (index) {
                        0 -> LockCondition.LOCKED
                        1 -> LockCondition.UNLOCKED
                        else -> LockCondition.BOTH
                    },
                )
            },
        )

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = state.textLength.maxLengthChars?.toString() ?: "",
            onValueChange = { raw ->
                viewModel.onTextLengthChanged(
                    state.textLength.copy(maxLengthChars = raw.toIntOrNull()?.takeIf { it > 0 }),
                )
            },
            label = { Text(text = stringResource(R.string.global_max_length)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        ChoiceRow(
            labelRes = R.string.global_scope_message,
            options = listOf(
                R.string.global_scope_message to (state.textLength.scope == LengthScope.MESSAGE),
                R.string.global_scope_whole_notification to
                    (state.textLength.scope == LengthScope.WHOLE_NOTIFICATION),
            ),
            onSelect = { index ->
                viewModel.onTextLengthChanged(
                    state.textLength.copy(
                        scope = if (index == 0) LengthScope.MESSAGE else LengthScope.WHOLE_NOTIFICATION,
                    ),
                )
            },
        )

        Spacer(Modifier.height(16.dp))
        ToggleRow(
            labelRes = R.string.global_keywords_enabled,
            checked = state.keywordFilter.enabled,
            onCheckedChange = { enabled ->
                viewModel.onKeywordFilterChanged(state.keywordFilter.copy(enabled = enabled))
            },
        )
        ChoiceRow(
            labelRes = R.string.global_keywords_mode,
            options = listOf(
                R.string.global_keyword_mode_whole_word to
                    (state.keywordFilter.mode == KeywordMatchMode.WHOLE_WORD),
                R.string.global_keyword_mode_phrase to
                    (state.keywordFilter.mode == KeywordMatchMode.PHRASE),
            ),
            onSelect = { index ->
                viewModel.onKeywordFilterChanged(
                    state.keywordFilter.copy(
                        mode = if (index == 0) KeywordMatchMode.WHOLE_WORD else KeywordMatchMode.PHRASE,
                    ),
                )
            },
        )
        OutlinedTextField(
            value = state.keywordFilter.keywords.joinToString(", "),
            onValueChange = { raw ->
                viewModel.onKeywordFilterChanged(
                    state.keywordFilter.copy(keywords = splitList(raw)),
                )
            },
            label = { Text(text = stringResource(R.string.global_keywords_keywords)) },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))
        ToggleRow(
            labelRes = R.string.global_privacy_exclude_app,
            checked = state.privacy.excludeApp,
            onCheckedChange = { viewModel.onPrivacyChanged(state.privacy.copy(excludeApp = it)) },
        )
        ToggleRow(
            labelRes = R.string.global_privacy_skip_otp,
            checked = state.privacy.skipOtp,
            onCheckedChange = { viewModel.onPrivacyChanged(state.privacy.copy(skipOtp = it)) },
        )
        ToggleRow(
            labelRes = R.string.global_privacy_skip_password,
            checked = state.privacy.skipPassword,
            onCheckedChange = { viewModel.onPrivacyChanged(state.privacy.copy(skipPassword = it)) },
        )
        ToggleRow(
            labelRes = R.string.global_privacy_skip_banking,
            checked = state.privacy.skipBanking,
            onCheckedChange = { viewModel.onPrivacyChanged(state.privacy.copy(skipBanking = it)) },
        )
        ToggleRow(
            labelRes = R.string.global_privacy_skip_payment,
            checked = state.privacy.skipPayment,
            onCheckedChange = { viewModel.onPrivacyChanged(state.privacy.copy(skipPayment = it)) },
        )
        ToggleRow(
            labelRes = R.string.global_privacy_hide_content,
            checked = state.privacy.hideContent,
            onCheckedChange = { viewModel.onPrivacyChanged(state.privacy.copy(hideContent = it)) },
        )
        OutlinedTextField(
            value = state.privacy.sensitiveKeywords.joinToString(", "),
            onValueChange = { raw ->
                viewModel.onPrivacyChanged(state.privacy.copy(sensitiveKeywords = splitList(raw)))
            },
            label = { Text(text = stringResource(R.string.global_privacy_sensitive_keywords)) },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        ToggleRow(
            labelRes = R.string.global_audio_allow_wired,
            checked = state.audioOutput.allowWired,
            onCheckedChange = { viewModel.onAudioOutputChanged(allowWired = it) },
        )
        ToggleRow(
            labelRes = R.string.global_audio_allow_bluetooth,
            checked = state.audioOutput.allowBluetooth,
            onCheckedChange = { viewModel.onAudioOutputChanged(allowBluetooth = it) },
        )
        ToggleRow(
            labelRes = R.string.global_audio_allow_speaker,
            checked = state.audioOutput.allowPhoneSpeaker,
            onCheckedChange = { viewModel.onAudioOutputChanged(allowPhoneSpeaker = it) },
        )

        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.global_settings_apply_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
    }
}

/** One labelled radio group; [options] pairs a label with its selected flag. */
@Composable
private fun ChoiceRow(
    labelRes: Int,
    options: List<Pair<Int, Boolean>>,
    onSelect: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyLarge,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEachIndexed { index, (optionLabelRes, selected) ->
                Row(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = { onSelect(index) },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = selected, onClick = null)
                    Text(
                        text = stringResource(optionLabelRes),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }
        }
    }
}

/** One labelled checkbox row at the 48 dp touch-target minimum. */
@Composable
private fun ToggleRow(
    labelRes: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f).padding(end = 12.dp),
        )
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** Splits the comma-separated text fields the same way the codec stores lists. */
private fun splitList(raw: String): List<String> =
    raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
