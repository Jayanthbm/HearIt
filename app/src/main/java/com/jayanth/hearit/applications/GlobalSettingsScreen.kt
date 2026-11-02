package com.jayanth.hearit.applications

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jayanth.hearit.R
import com.jayanth.hearit.applications.ApplicationsRoute
import com.jayanth.hearit.core.common.model.KeywordMatchMode
import com.jayanth.hearit.core.common.model.LengthScope
import com.jayanth.hearit.core.common.model.NotificationSnapshot
import com.jayanth.hearit.core.common.model.SkipReason
import com.jayanth.hearit.core.common.model.ScreenCondition
import com.jayanth.hearit.core.common.model.LockCondition
import com.jayanth.hearit.core.common.model.TextLengthSettings
import com.jayanth.hearit.core.common.model.KeywordFilterSettings
import com.jayanth.hearit.core.common.model.PrivacySettings
import com.jayanth.hearit.core.common.model.AudioOutputSettings
import com.jayanth.hearit.core.common.model.AudioOutputKind
import com.jayanth.hearit.core.rules.KeywordMatcher

/** Entry point of the Global Rules, Voice & Speech settings (phase P15). */
@Composable
fun GlobalSettingsRoute(modifier: Modifier = Modifier) {
    val state by remember { mutableStateOf(GlobalSettingsUiState()) }
    var state by rememberSaveable { mutableStateOf(state) }

    BackHandler(enabled = true) {
        // Return to dashboard when back pressed
    }

    Column(
        modifier = Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        TextButton(onClick = { /* TODO: navigate back */ }) {
            Text(text = stringResource(R.string.dashboard_back))
        }
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
        SectionHeader(R.string.global_settings_screen_lock)
        ScreenLockSettings(state = state, onScreenConditionChanged = { onScreenConditionChanged(it) }, onLockConditionChanged = { onLockConditionChanged(it) })

        Spacer(Modifier.height(20.dp))
        SectionHeader(R.string.global_settings_text_length)
        TextLengthSettingsScreen(state = state, onTextLengthChanged = { onTextLengthChanged(it) })

        Spacer(Modifier.height(20.dp))
        SectionHeader(R.string.global_settings_keywords)
        KeywordFilterSettingsScreen(state = state, onKeywordFilterChanged = { onKeywordFilterChanged(it) })

        Spacer(Modifier.height(20.dp))
        SectionHeader(R.string.global_settings_privacy)
        PrivacySettingsScreen(state = state, onPrivacyChanged = { onPrivacyChanged(it) })

        Spacer(Modifier.height(20.dp))
        SectionHeader(R.string.global_settings_audio_output)
        AudioOutputSettingsScreen(state = state, onAudioOutputChanged = { onAudioOutputChanged(it) })

        Spacer(Modifier.height(24.dp))
        OutlinedButton(
            onClick = { /* TODO: save and apply */ },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text(text = stringResource(R.string.global_settings_apply))
        }
        Text(
            text = stringResource(R.string.global_settings_apply_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionHeader(@StringRes titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

// ---- Screen / Lock ----

@Composable
private fun ScreenLockSettings(
    state: GlobalSettingsUiState,
    onScreenConditionChanged: (ScreenCondition) -> Unit,
    onLockConditionChanged: (LockCondition) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.global_screen_condition),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).padding(end = 12.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = state.screenCondition == ScreenCondition.ON, onClick = { onScreenConditionChanged(ScreenCondition.ON) })
                Text(stringResource(R.string.global_screen_on))
                RadioButton(selected = state.screenCondition == ScreenCondition.OFF, onClick = { onScreenConditionChanged(ScreenCondition.OFF) })
                Text(stringResource(R.string.global_screen_off))
                RadioButton(selected = state.screenCondition == ScreenCondition.BOTH, onClick = { onScreenConditionChanged(ScreenCondition.BOTH) })
                Text(stringResource(R.string.global_screen_both))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.global_lock_condition),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).padding(end = 12.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = state.lockCondition == LockCondition.LOCKED, onClick = { onLockConditionChanged(LockCondition.LOCKED) })
                Text(stringResource(R.string.global_lock_locked))
                RadioButton(selected = state.lockCondition == LockCondition.UNLOCKED, onClick = { onLockConditionChanged(LockCondition.UNLOCKED) })
                RadioButton(selected = state.lockCondition == LockCondition.BOTH, onClick = { onLockConditionChanged(LockCondition.BOTH) })
                Text(stringResource(R.string.global_lock_both))
            }
        }
    }
}

// ---- Text Length ----

@Composable
private fun TextLengthSettingsScreen(
    state: GlobalSettingsUiState,
    onTextLengthChanged: (TextLengthSettings) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.global_max_length),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).padding(end = 12.dp),
            )
            OutlinedTextField(
                value = state.textLength.maxLengthChars?.toString() ?: "",
                onValueChange = { onTextLengthChanged(state.textLength.copy(maxLengthChars = it.toIntOrNull())) },
                label = { Text(stringResource(R.string.global_chars)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.6f),
            )
            Text(
                text = state.textLength.maxLengthChars?.toString() ?: "∞",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(when (state.textLength.scope) {
                    LengthScope.MESSAGE -> R.string.global_scope_message
                    LengthScope.WHOLE_NOTIFICATION -> R.string.global_scope_whole_notification
                }),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).padding(end = 12.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = state.textLength.scope == LengthScope.MESSAGE,
                    onClick = { onTextLengthChanged(state.textLength.copy(scope = LengthScope.MESSAGE)) }
                )
                Text(stringResource(R.string.global_scope_message))
                RadioButton(
                    selected = state.textLength.scope == LengthScope.WHOLE_NOTIFICATION,
                    onClick = { onTextLengthChanged(state.textLength.copy(scope = LengthScope.WHOLE_NOTIFICATION)) }
                )
                Text(stringResource(R.string.global_scope_whole_notification))
            }
        }
    }
}

// ---- Keyword Filter ----

@Composable
private fun KeywordFilterSettingsScreen(
    state: GlobalSettingsUiState,
    onKeywordFilterChanged: (KeywordFilterSettings) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.global_keywords_enabled),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).padding(end = 12.dp),
            )
            Checkbox(checked = state.keywordFilter.enabled, onCheckedChange = { onKeywordFilterChanged(state.keywordFilter.copy(enabled = it)) })
        }

        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.global_keywords_mode),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).padding(end = 12.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = state.keywordFilter.mode == KeywordMatchMode.WHOLE_WORD,
                    onClick = { onKeywordFilterChanged(state.keywordFilter.copy(mode = KeywordMatchMode.WHOLE_WORD)) }
                )
                Text(stringResource(R.string.global_keyword_mode_whole_word))
                RadioButton(
                    selected = state.keywordFilter.mode == KeywordMatchMode.PHRASE,
                    onClick = { onKeywordFilterChanged(state.keywordFilter.copy(mode = KeywordMatchMode.PHRASE)) }
                )
                Text(stringResource(R.string.global_keyword_mode_phrase))
            }
        }

        OutlinedTextField(
            value = joinStrings(state.keywordFilter.keywords, ", "),
            onValueChange = { onKeywordFilterChanged(state.keywordFilter.copy(keywords = it.split(",").map { it.trim() }.filter { it.isNotEmpty() })) },
            label = { Text(stringResource(R.string.global_keywords_keywords)) },
            singleLine = false,
            modifier = Modifier.fillMaxWidth(0.8f).height(80.dp),
            keyboardOptions = androidx.compose.material3.KeyboardOptions(keyboardType = androidx.compose.material3.KeyboardType.TEXT),
        )
    }
}

// ---- Privacy ----

@Composable
private fun PrivacySettingsScreen(
    state: GlobalSettingsUiState,
    onPrivacyChanged: (PrivacySettings) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.global_privacy_exclude_app),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).padding(end = 12.dp),
            )
            Checkbox(checked = state.privacy.excludeApp, onCheckedChange = { onPrivacyChanged(state.privacy.copy(excludeApp = it)) })
        }

        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.global_privacy_skip_otp),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).padding(end = 12.dp),
            )
            Checkbox(checked = state.privacy.skipOtp, onCheckedChange = { onPrivacyChanged(state.privacy.copy(skipOtp = it)) })
        }

        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.global_privacy_skip_password),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).padding(end = 12.dp),
            )
            Checkbox(checked = state.privacy.skipPassword, onCheckedChange = { onPrivacyChanged(state.privacy.copy(skipPassword = it)) })
        }

        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.global_privacy_skip_banking),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).padding(end = 12.dp),
            )
            Checkbox(checked = state.privacy.skipBanking, onCheckedChange = { onPrivacyChanged(state.privacy.copy(skipBanking = it)) })
        }

        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.global_privacy_skip_payment),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).padding(end = 12.dp),
            )
            Checkbox(checked = state.privacy.skipPayment, onCheckedChange = { onPrivacyChanged(state.privacy.copy(skipPayment = it)) })
        }

        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.global_privacy_hide_content),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).padding(end = 12.dp),
            )
            Checkbox(checked = state.privacy.hideContent, onCheckedChange = { onPrivacyChanged(state.privacy.copy(hideContent = it)) })
        }

        OutlinedTextField(
            value = joinStrings(state.privacy.sensitiveKeywords, ", "),
            onValueChange = { onPrivacyChanged(state.privacy.copy(sensitiveKeywords = it.split(",").map { it.trim() }.filter { it.isNotBlank() })) },
            label = { Text(stringResource(R.string.global_privacy_sensitive_keywords)) },
            singleLine = false,
            modifier = Modifier.fillMaxWidth(0.8f).height(80.dp),
            keyboardOptions = androidx.compose.material3.KeyboardOptions(keyboardType = androidx.compose.material3.KeyboardType.TEXT),
        )
    }
}

// ---- Audio Output ----

@Composable
private fun AudioOutputSettingsScreen(
    state: GlobalSettingsUiState,
    onAudioOutputChanged: (AudioOutputSettings) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.global_audio_allow_wired),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).padding(end = 12.dp),
            )
            Checkbox(checked = state.audioOutput.allowWired, onCheckedChange = { onAudioOutputChanged(state.audioOutput.copy(allowWired = it)) })
        }

        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.global_audio_allow_bluetooth),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).padding(end = 12.dp),
            )
            Checkbox(checked = state.audioOutput.allowBluetooth, onCheckedChange = { onAudioOutputChanged(state.audioOutput.copy(allowBluetooth = it)) })
        }

        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.global_audio_allow_speaker),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).padding(end = 12.dp),
            )
            Checkbox(checked = state.audioOutput.allowPhoneSpeaker, onCheckedChange = { onAudioOutputChanged(state.audioOutput.copy(allowPhoneSpeaker = it)) })
        }
    }
}