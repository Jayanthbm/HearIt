package com.jayanth.hearit.history

import android.text.format.DateUtils
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.jayanth.hearit.R
import com.jayanth.hearit.core.common.model.HistoryStatus
import com.jayanth.hearit.core.common.model.HistoryStorageMode
import com.jayanth.hearit.core.common.model.HistoryUpdateHandling

/**
 * Entry point of the History feature (master prompt §12, phase P18).
 *
 * Reloads the first page whenever the screen resumes, because the announcement service keeps
 * writing history while the user is elsewhere.
 */
@Composable
fun HistoryRoute(viewModel: HistoryViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = context as? LifecycleOwner

    DisposableEffect(lifecycleOwner) {
        if (lifecycleOwner == null) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) viewModel.onRefresh()
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
    }

    HistoryScreen(
        state = state,
        onQueryChanged = viewModel::onQueryChanged,
        onStatusChanged = viewModel::onStatusChanged,
        onSortChanged = viewModel::onSortChanged,
        onLoadMore = viewModel::onLoadMore,
        onDeleteEntry = viewModel::onDeleteEntry,
        onClearAll = viewModel::onClearAll,
        onStorageModeChanged = viewModel::onStorageModeChanged,
        onUpdateHandlingChanged = viewModel::onUpdateHandlingChanged,
        onRetentionChanged = viewModel::onRetentionChanged,
        modifier = modifier,
    )
}

/**
 * Browsable history: search, status filter, sort, paged rows, per-entry delete, clear-all and
 * the global history settings.
 *
 * Stateless: every decision is the ViewModel's, every string is a resource, and every
 * interactive target is at least 48 dp. Destructive actions open a confirmation dialog first
 * (P18 acceptance).
 */
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onQueryChanged: (String) -> Unit,
    onStatusChanged: (HistoryStatus?) -> Unit,
    onSortChanged: (HistorySortOrder) -> Unit,
    onLoadMore: () -> Unit,
    onDeleteEntry: (Long) -> Unit,
    onClearAll: () -> Unit,
    onStorageModeChanged: (HistoryStorageMode) -> Unit,
    onUpdateHandlingChanged: (HistoryUpdateHandling) -> Unit,
    onRetentionChanged: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var openDialog by rememberSaveable { mutableStateOf(DIALOG_NONE) }
    var pendingDelete by remember { mutableStateOf<HistoryItem?>(null) }
    var confirmingClearAll by rememberSaveable { mutableStateOf(false) }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item(key = "search") {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = state.filter.query,
                    onValueChange = onQueryChanged,
                    label = { Text(text = stringResource(R.string.history_search_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.history_search_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { openDialog = DIALOG_STATUS },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(
                            text = stringResource(
                                R.string.history_status_filter,
                                state.filter.status?.let { historyStatusLabel(it) }
                                    ?: stringResource(R.string.history_status_all),
                            ),
                        )
                    }
                    TextButton(
                        onClick = { openDialog = DIALOG_SORT },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(
                            text = stringResource(
                                R.string.history_sort_label,
                                sortLabel(state.filter.sort),
                            ),
                        )
                    }
                }
                TextButton(
                    onClick = { confirmingClearAll = true },
                    enabled = state.items.isNotEmpty(),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(text = stringResource(R.string.history_clear_all_button))
                }
            }
        }

        item(key = "settings") {
            HistorySettingsSection(
                settings = state.settings,
                onStorageClick = { openDialog = DIALOG_STORAGE },
                onUpdatesClick = { openDialog = DIALOG_UPDATES },
                onRetentionClick = { openDialog = DIALOG_RETENTION },
            )
        }

        item(key = "divider") { HorizontalDivider() }

        when {
            state.unavailable -> item(key = "unavailable") {
                Message(R.string.history_unavailable, error = true)
            }

            !state.loaded -> item(key = "loading") { Message(R.string.history_loading) }

            state.items.isEmpty() -> item(key = "empty") {
                Message(
                    if (state.isFiltered) R.string.history_empty_filtered else R.string.history_empty,
                )
            }

            else -> items(items = state.items, key = { item -> item.id }) { item ->
                HistoryRow(item = item, onDelete = { pendingDelete = item })
            }
        }

        item(key = "footer") {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                when {
                    state.loading -> Text(
                        text = stringResource(R.string.history_loading),
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    state.endReached && state.items.isNotEmpty() -> Text(
                        text = stringResource(R.string.history_end),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    state.items.isNotEmpty() -> TextButton(
                        onClick = onLoadMore,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(text = stringResource(R.string.history_load_more))
                    }
                }
            }
        }
    }

    when (openDialog) {
        DIALOG_STATUS -> ChoiceDialog(
            title = stringResource(R.string.history_status_title),
            options = listOf(null) + HistoryStatus.entries,
            selected = state.filter.status,
            label = { status ->
                status?.let { historyStatusLabel(it) } ?: stringResource(R.string.history_status_all)
            },
            onSelect = { status -> openDialog = DIALOG_NONE; onStatusChanged(status) },
            onDismiss = { openDialog = DIALOG_NONE },
        )

        DIALOG_SORT -> ChoiceDialog(
            title = stringResource(R.string.history_sort_title),
            options = HistorySortOrder.entries,
            selected = state.filter.sort,
            label = { sort -> sortLabel(sort) },
            onSelect = { sort -> openDialog = DIALOG_NONE; onSortChanged(sort) },
            onDismiss = { openDialog = DIALOG_NONE },
        )

        DIALOG_STORAGE -> ChoiceDialog(
            title = stringResource(R.string.history_storage_label),
            options = HistoryStorageMode.entries,
            selected = state.settings.storageMode,
            label = { mode -> stringResource(storageLabelRes(mode)) },
            onSelect = { mode -> openDialog = DIALOG_NONE; onStorageModeChanged(mode) },
            onDismiss = { openDialog = DIALOG_NONE },
        )

        DIALOG_UPDATES -> ChoiceDialog(
            title = stringResource(R.string.history_updates_label),
            options = HistoryUpdateHandling.entries,
            selected = state.settings.updateHandling,
            label = { handling -> stringResource(updateHandlingLabelRes(handling)) },
            onSelect = { handling -> openDialog = DIALOG_NONE; onUpdateHandlingChanged(handling) },
            onDismiss = { openDialog = DIALOG_NONE },
        )

        DIALOG_RETENTION -> ChoiceDialog(
            title = stringResource(R.string.history_retention_label),
            options = RETENTION_OPTIONS,
            selected = state.settings.retentionDays,
            label = { days ->
                days?.let { stringResource(R.string.history_retention_days, it) }
                    ?: stringResource(R.string.history_retention_never)
            },
            onSelect = { days -> openDialog = DIALOG_NONE; onRetentionChanged(days) },
            onDismiss = { openDialog = DIALOG_NONE },
        )
    }

    val deleting = pendingDelete
    if (deleting != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(text = stringResource(R.string.history_delete_confirm_title)) },
            text = { Text(text = stringResource(R.string.history_delete_confirm_body, deleting.appName)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        onDeleteEntry(deleting.id)
                    },
                ) {
                    Text(text = stringResource(R.string.history_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (confirmingClearAll) {
        AlertDialog(
            onDismissRequest = { confirmingClearAll = false },
            title = { Text(text = stringResource(R.string.history_clear_all_confirm_title)) },
            text = { Text(text = stringResource(R.string.history_clear_all_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingClearAll = false
                        onClearAll()
                    },
                ) {
                    Text(text = stringResource(R.string.history_clear_all_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingClearAll = false }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

/** Global history settings, each row opening its choice dialog. */
@Composable
private fun HistorySettingsSection(
    settings: HistorySettingsState,
    onStorageClick: () -> Unit,
    onUpdatesClick: () -> Unit,
    onRetentionClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = stringResource(R.string.history_settings_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp),
        )
        SettingsValueRow(
            label = stringResource(R.string.history_storage_label),
            value = stringResource(storageLabelRes(settings.storageMode)),
            onClick = onStorageClick,
        )
        SettingsValueRow(
            label = stringResource(R.string.history_updates_label),
            value = stringResource(updateHandlingLabelRes(settings.updateHandling)),
            onClick = onUpdatesClick,
        )
        SettingsValueRow(
            label = stringResource(R.string.history_retention_label),
            value = settings.retentionDays?.let { stringResource(R.string.history_retention_days, it) }
                ?: stringResource(R.string.history_retention_never),
            onClick = onRetentionClick,
        )
        Text(
            text = stringResource(R.string.history_settings_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsValueRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f).padding(end = 12.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** One stored notification: app + outcome, reason, sender/content, time and delete. */
@Composable
private fun HistoryRow(item: HistoryItem, onDelete: () -> Unit) {
    val relativeTime = remember(item.recordedAtMillis) {
        DateUtils.getRelativeTimeSpanString(
            item.recordedAtMillis,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
        ).toString()
    }
    val deleteDescription = stringResource(R.string.history_delete_entry, item.appName)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Text(
            text = stringResource(
                R.string.history_row_title,
                item.appName,
                historyStatusLabel(item.status),
            ),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
        item.reason?.let { reason ->
            Text(
                text = stringResource(R.string.history_row_reason, skipReasonLabel(reason)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item.sender?.let { sender ->
            Text(
                text = stringResource(R.string.history_row_sender, sender),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item.messageText?.let { message ->
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = relativeTime,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (item.isUpdate) {
                Text(
                    text = stringResource(R.string.history_row_updated),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
            TextButton(
                onClick = onDelete,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .semantics { contentDescription = deleteDescription },
            ) {
                Text(text = stringResource(R.string.history_delete))
            }
        }
    }
    HorizontalDivider()
}

@Composable
private fun Message(@StringRes textRes: Int, error: Boolean = false) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.bodyMedium,
        color = if (error) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier.padding(24.dp),
    )
}

/**
 * One single-choice dialog. [label] is composable so every option can be resolved to a string
 * resource inside the dialog's own composition.
 */
@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column {
                for (option in options) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .selectable(
                                selected = option == selected,
                                role = Role.RadioButton,
                                onClick = { onSelect(option) },
                            )
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option == selected, onClick = null)
                        Text(
                            text = label(option),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun sortLabel(sort: HistorySortOrder): String = stringResource(
    when (sort) {
        HistorySortOrder.RecentFirst -> R.string.history_sort_recent
        HistorySortOrder.OldestFirst -> R.string.history_sort_oldest
        HistorySortOrder.ByApp -> R.string.history_sort_by_app
    },
)

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

private const val DIALOG_NONE = ""
private const val DIALOG_STATUS = "status"
private const val DIALOG_SORT = "sort"
private const val DIALOG_STORAGE = "storage"
private const val DIALOG_UPDATES = "updates"
private const val DIALOG_RETENTION = "retention"

/** Retention choices from the master prompt §12 list, plus "never". */
private val RETENTION_OPTIONS: List<Int?> = listOf(null, 1, 3, 7, 10, 15, 30, 60, 90)
