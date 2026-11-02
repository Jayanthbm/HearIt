package com.jayanth.hearit.applications

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jayanth.hearit.R
import com.jayanth.hearit.core.common.model.AppPriority
import com.jayanth.hearit.core.common.model.AudioFocusPolicy
import com.jayanth.hearit.core.common.model.HistoryStorageMode
import com.jayanth.hearit.core.common.model.HistoryUpdateHandling

/**
 * Entry point of the Applications feature (master prompt §5, phase P13).
 *
 * Shows the list, or the open app's settings. Both surfaces live inside the Apps tab, so
 * the shell stays unchanged; Back from the settings screen returns to the list.
 */
@Composable
fun ApplicationsRoute(viewModel: ApplicationsViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()
    val settings = state.settings

    BackHandler(enabled = settings != null) { viewModel.onCloseApp() }

    if (settings == null) {
        ApplicationsListScreen(
            state = state,
            icons = viewModel.icons,
            onQueryChange = viewModel::onQueryChange,
            onToggleApp = viewModel::onToggleApp,
            onSelectAll = viewModel::onSelectAllVisible,
            onDeselectAll = viewModel::onDeselectAllVisible,
            onOpenApp = viewModel::onOpenApp,
            modifier = modifier,
        )
    } else {
        AppSettingsScreen(
            state = settings,
            onBack = viewModel::onCloseApp,
            onToggleEnabled = { viewModel.onToggleApp(settings.packageName) },
            onPriorityChanged = viewModel::onPriorityChanged,
            onHistoryStorageModeChanged = viewModel::onHistoryStorageModeChanged,
            onHistoryUpdateHandlingChanged = viewModel::onHistoryUpdateHandlingChanged,
            onRespectDndChanged = viewModel::onRespectDndChanged,
            onQueueDuringCallChanged = viewModel::onQueueDuringCallChanged,
            onAudioFocusChanged = viewModel::onAudioFocusChanged,
            onResetToGlobal = viewModel::onResetToGlobal,
            modifier = modifier,
        )
    }
}

/**
 * The app list: search, select-all/deselect-all over the search result, and one row per app.
 *
 * Stateless apart from the search field's value coming from the ViewModel; the list is a
 * `LazyColumn`, so a large catalog composes only visible rows. Every row exposes exactly two
 * accessible targets — a `Role.Switch` toggle carrying the app name and a "Settings"
 * button — both at least 48 dp.
 */
@Composable
private fun ApplicationsListScreen(
    state: ApplicationsUiState,
    icons: AppIconLoader,
    onQueryChange: (String) -> Unit,
    onToggleApp: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onOpenApp: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            label = { Text(text = stringResource(R.string.applications_search_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Text(
            text = stringResource(
                R.string.applications_enabled_count,
                state.enabledApps.size,
                state.apps.size,
            ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onSelectAll,
                enabled = state.visibleApps.any { it.packageName !in state.enabledApps },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(text = stringResource(R.string.applications_select_all))
            }
            TextButton(
                onClick = onDeselectAll,
                enabled = state.visibleApps.any { it.packageName in state.enabledApps },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(text = stringResource(R.string.applications_deselect_all))
            }
        }
        Text(
            text = stringResource(R.string.applications_visibility_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.loading -> Text(
                    text = stringResource(R.string.applications_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(24.dp),
                )

                state.catalogUnavailable -> Text(
                    text = stringResource(R.string.applications_catalog_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(24.dp),
                )

                state.visibleApps.isEmpty() -> Text(
                    text = if (state.query.isBlank()) {
                        stringResource(R.string.applications_empty)
                    } else {
                        stringResource(R.string.applications_empty_search, state.query.trim())
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(24.dp),
                )

                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = state.visibleApps, key = { app -> app.packageName }) { app ->
                        ApplicationRow(
                            app = app,
                            enabled = app.packageName in state.enabledApps,
                            icons = icons,
                            onToggle = { onToggleApp(app.packageName) },
                            onOpenSettings = { onOpenApp(app.packageName) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ApplicationRow(
    app: AppEntry,
    enabled: Boolean,
    icons: AppIconLoader,
    onToggle: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp)
                .toggleable(value = enabled, role = Role.Switch, onValueChange = { onToggle() })
                .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(icons = icons, packageName = app.packageName)
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).padding(start = 12.dp),
            )
            Switch(checked = enabled, onCheckedChange = null)
        }
        TextButton(onClick = onOpenSettings, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(text = stringResource(R.string.applications_open_settings))
        }
    }
}

/** Loads one icon asynchronously; a missing icon leaves the row without one. */
@Composable
private fun AppIcon(icons: AppIconLoader, packageName: String) {
    var icon by remember(packageName) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(packageName) {
        icon = icons.load(packageName)
    }
    val loaded = icon
    if (loaded != null) {
        Image(bitmap = loaded, contentDescription = null, modifier = Modifier.size(32.dp))
    } else {
        Spacer(Modifier.size(32.dp))
    }
}

/**
 * Per-app settings with visible inheritance (master prompt §5, P13 acceptance).
 *
 * Every group starts with an "Inherit global (value)" option and then the explicit choices.
 * Edits apply immediately, so there is no Save button; "Reset to global values" clears only
 * this app's overrides and leaves the announcement switch alone.
 */
@Composable
private fun AppSettingsScreen(
    state: AppSettingsUiState,
    onBack: () -> Unit,
    onToggleEnabled: () -> Unit,
    onPriorityChanged: (AppPriority?) -> Unit,
    onHistoryStorageModeChanged: (HistoryStorageMode?) -> Unit,
    onHistoryUpdateHandlingChanged: (HistoryUpdateHandling?) -> Unit,
    onRespectDndChanged: (Boolean?) -> Unit,
    onQueueDuringCallChanged: (Boolean?) -> Unit,
    onAudioFocusChanged: (AudioFocusPolicy?) -> Unit,
    onResetToGlobal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(text = stringResource(R.string.dashboard_back))
        }
        Text(
            text = state.label,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = state.packageName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))
        SectionHeader(R.string.app_settings_general)
        SettingSwitchRow(
            label = stringResource(R.string.app_settings_enabled),
            checked = state.enabled,
            onCheckedChange = { onToggleEnabled() },
        )
        Text(
            text = stringResource(R.string.app_settings_enabled_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))
        SectionHeader(R.string.app_settings_priority_section)
        OptionRow(
            label = inheritLabel(priorityLabelRes(state.priority.global)),
            selected = state.priority.isInherited,
            onSelect = { onPriorityChanged(null) },
        )
        for (priority in AppPriority.entries) {
            OptionRow(
                label = stringResource(priorityLabelRes(priority)),
                selected = !state.priority.isInherited && state.priority.effective == priority,
                onSelect = { onPriorityChanged(priority) },
            )
        }

        Spacer(Modifier.height(20.dp))
        SectionHeader(R.string.app_settings_history_section)
        OptionRow(
            label = inheritLabel(storageLabelRes(state.historyStorageMode.global)),
            selected = state.historyStorageMode.isInherited,
            onSelect = { onHistoryStorageModeChanged(null) },
        )
        for (mode in HistoryStorageMode.entries) {
            OptionRow(
                label = stringResource(storageLabelRes(mode)),
                selected = !state.historyStorageMode.isInherited && state.historyStorageMode.effective == mode,
                onSelect = { onHistoryStorageModeChanged(mode) },
            )
        }
        OptionRow(
            label = inheritLabel(updateHandlingLabelRes(state.historyUpdateHandling.global)),
            selected = state.historyUpdateHandling.isInherited,
            onSelect = { onHistoryUpdateHandlingChanged(null) },
        )
        for (handling in HistoryUpdateHandling.entries) {
            OptionRow(
                label = stringResource(updateHandlingLabelRes(handling)),
                selected = !state.historyUpdateHandling.isInherited &&
                    state.historyUpdateHandling.effective == handling,
                onSelect = { onHistoryUpdateHandlingChanged(handling) },
            )
        }

        Spacer(Modifier.height(20.dp))
        SectionHeader(R.string.app_settings_when_section)
        OptionRow(
            label = inheritLabel(onOffLabelRes(state.respectDnd.global)),
            selected = state.respectDnd.isInherited,
            onSelect = { onRespectDndChanged(null) },
        )
        for (value in BOOLEAN_OPTIONS) {
            OptionRow(
                label = stringResource(onOffLabelRes(value)),
                selected = !state.respectDnd.isInherited && state.respectDnd.effective == value,
                onSelect = { onRespectDndChanged(value) },
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.app_settings_call_queue),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        OptionRow(
            label = inheritLabel(onOffLabelRes(state.queueDuringCall.global)),
            selected = state.queueDuringCall.isInherited,
            onSelect = { onQueueDuringCallChanged(null) },
        )
        for (value in BOOLEAN_OPTIONS) {
            OptionRow(
                label = stringResource(onOffLabelRes(value)),
                selected = !state.queueDuringCall.isInherited && state.queueDuringCall.effective == value,
                onSelect = { onQueueDuringCallChanged(value) },
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.app_settings_audio_focus),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        OptionRow(
            label = inheritLabel(audioFocusLabelRes(state.audioFocus.global)),
            selected = state.audioFocus.isInherited,
            onSelect = { onAudioFocusChanged(null) },
        )
        for (policy in AudioFocusPolicy.entries) {
            OptionRow(
                label = stringResource(audioFocusLabelRes(policy)),
                selected = !state.audioFocus.isInherited && state.audioFocus.effective == policy,
                onSelect = { onAudioFocusChanged(policy) },
            )
        }

        Spacer(Modifier.height(20.dp))
        OutlinedButton(
            onClick = onResetToGlobal,
            enabled = state.hasOverrides,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text(text = stringResource(R.string.app_settings_reset))
        }
        Text(
            text = stringResource(R.string.app_settings_reset_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.app_settings_note_later),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
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

@Composable
private fun SettingSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f).padding(end = 12.dp),
        )
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** One radio option; the whole row is the target (≥48 dp) and TalkBack announces the role. */
@Composable
private fun OptionRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun inheritLabel(valueLabelRes: Int): String = stringResource(
    R.string.app_settings_inherit_value,
    stringResource(valueLabelRes),
)

@StringRes
private fun priorityLabelRes(priority: AppPriority): Int = when (priority) {
    AppPriority.NORMAL -> R.string.app_settings_priority_normal
    AppPriority.HIGH -> R.string.app_settings_priority_high
    AppPriority.CRITICAL -> R.string.app_settings_priority_critical
    AppPriority.LOW -> R.string.app_settings_priority_low
}

@StringRes
private fun storageLabelRes(mode: HistoryStorageMode): Int = when (mode) {
    HistoryStorageMode.FULL_CONTENT -> R.string.app_settings_storage_full
    HistoryStorageMode.METADATA_ONLY -> R.string.app_settings_storage_metadata
    HistoryStorageMode.DISABLED -> R.string.app_settings_storage_off
}

@StringRes
private fun updateHandlingLabelRes(handling: HistoryUpdateHandling): Int = when (handling) {
    HistoryUpdateHandling.UPDATE_ENTRY -> R.string.app_settings_update_entry
    HistoryUpdateHandling.NEW_ENTRY -> R.string.app_settings_update_new
}

@StringRes
private fun audioFocusLabelRes(policy: AudioFocusPolicy): Int = when (policy) {
    AudioFocusPolicy.DUCK -> R.string.app_settings_audio_focus_duck
    AudioFocusPolicy.PAUSE -> R.string.app_settings_audio_focus_pause
    AudioFocusPolicy.NONE -> R.string.app_settings_audio_focus_none
}

@StringRes
private fun onOffLabelRes(value: Boolean): Int =
    if (value) R.string.app_settings_on else R.string.app_settings_off

/** Explicit On/Off order, so the two options never depend on enum declaration order. */
private val BOOLEAN_OPTIONS = listOf(true, false)
