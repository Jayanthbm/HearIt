package com.jayanth.hearit.applications

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayanth.hearit.R
import com.jayanth.hearit.core.common.model.AppPriority
import com.jayanth.hearit.core.common.model.AudioFocusPolicy
import com.jayanth.hearit.core.common.model.HistoryStorageMode
import com.jayanth.hearit.core.common.model.HistoryUpdateHandling

/**
 * Entry point of the Applications feature (master prompt §5, phase P13).
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
 * The app list: search, filter/batch selection, and modern app cards.
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        // Modern Search Bar
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search installed applications…") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingIcon = {
                if (state.query.isNotBlank()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Status & Batch Controls Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Text(
                    text = stringResource(
                        R.string.applications_enabled_count,
                        state.enabledApps.size,
                        state.apps.size,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable(
                        enabled = state.visibleApps.any { it.packageName !in state.enabledApps },
                        onClick = onSelectAll,
                    ),
                ) {
                    Text(
                        text = "Select all",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable(
                        enabled = state.visibleApps.any { it.packageName in state.enabledApps },
                        onClick = onDeselectAll,
                    ),
                ) {
                    Text(
                        text = "Deselect all",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                }

                state.catalogUnavailable -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.applications_catalog_unavailable),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                state.visibleApps.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (state.query.isBlank()) {
                            stringResource(R.string.applications_empty)
                        } else {
                            stringResource(R.string.applications_empty_search, state.query.trim())
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items = state.visibleApps, key = { app -> app.packageName }) { app ->
                        ApplicationRow(
                            app = app,
                            enabled = app.packageName in state.enabledApps,
                            icons = icons,
                            onToggle = { onToggleApp(app.packageName) },
                            onOpenSettings = { onOpenApp(app.packageName) },
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // App Icon
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AppIcon(icons = icons, packageName = app.packageName)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // App Label & Package (clickable to toggle)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onToggle() },
            ) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (enabled) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Switch
            Switch(
                checked = enabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                ),
            )

            // Settings button
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = stringResource(R.string.applications_open_settings),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

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
        Icon(
            imageVector = Icons.Rounded.Notifications,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

// ============================================================================
// App Settings Screen (Per-App Rules with Inheritance)
// ============================================================================

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
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.dashboard_back),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = state.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // General Card: Enabled Switch
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.app_settings_enabled),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.app_settings_enabled_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.enabled,
                    onCheckedChange = { onToggleEnabled() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
        }

        // Section: Announcement Priority
        SettingsGroupCard(title = stringResource(R.string.app_settings_priority_section)) {
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
        }

        // Section: History Storage
        SettingsGroupCard(title = stringResource(R.string.app_settings_history_section)) {
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

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Update Handling",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp),
            )

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
        }

        // Section: DND & Call Rules
        SettingsGroupCard(title = stringResource(R.string.app_settings_when_section)) {
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

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.app_settings_call_queue),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp),
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
        }

        // Section: Audio Focus Policy
        SettingsGroupCard(title = stringResource(R.string.app_settings_audio_focus)) {
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
        }

        // Reset Overrides Action Button
        OutlinedButton(
            onClick = onResetToGlobal,
            enabled = state.hasOverrides,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.app_settings_reset), fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SettingsGroupCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun OptionRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
            ),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
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

private val BOOLEAN_OPTIONS = listOf(true, false)
