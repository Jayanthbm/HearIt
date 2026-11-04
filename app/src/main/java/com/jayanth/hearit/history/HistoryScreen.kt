package com.jayanth.hearit.history

import android.text.format.DateUtils
import androidx.annotation.StringRes
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.jayanth.hearit.R
import com.jayanth.hearit.core.common.model.HistoryStatus
import com.jayanth.hearit.core.common.model.HistoryStorageMode
import com.jayanth.hearit.core.common.model.HistoryUpdateHandling

/**
 * Entry point of the History feature (master prompt §12, phase P18).
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
 * Modern Material 3 Browsable History Screen.
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "header_spacing") {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Search Bar & Filter Chips
        item(key = "search_and_filters") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = state.filter.query,
                    onValueChange = onQueryChanged,
                    placeholder = {
                        Text(
                            text = "Search notification history…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    trailingIcon = {
                        if (state.filter.query.isNotBlank()) {
                            IconButton(onClick = { onQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                // Filter & Sort Pills Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(
                        selected = state.filter.status != null,
                        onClick = { openDialog = DIALOG_STATUS },
                        label = {
                            Text(
                                text = state.filter.status?.let { historyStatusLabel(it) } ?: "All Statuses",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 6.dp, bottomEnd = 12.dp, bottomStart = 6.dp),
                    )

                    FilterChip(
                        selected = state.filter.sort != HistorySortOrder.RecentFirst,
                        onClick = { openDialog = DIALOG_SORT },
                        label = {
                            Text(
                                text = sortLabel(state.filter.sort),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 6.dp, bottomEnd = 12.dp, bottomStart = 6.dp),
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    if (state.items.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 4.dp, bottomEnd = 10.dp, bottomStart = 4.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        ) {
                            IconButton(
                                onClick = { confirmingClearAll = true },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = stringResource(R.string.history_clear_all_button),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Global Storage Settings Collapsible Card
        item(key = "settings_card") {
            HistorySettingsCard(
                settings = state.settings,
                onStorageClick = { openDialog = DIALOG_STORAGE },
                onUpdatesClick = { openDialog = DIALOG_UPDATES },
                onRetentionClick = { openDialog = DIALOG_RETENTION },
            )
        }

        // Section Title: History Timeline
        item(key = "timeline_header") {
            Text(
                text = "Notification Feed",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        when {
            state.unavailable -> item(key = "unavailable") {
                HistoryNoticeBanner(message = stringResource(R.string.history_unavailable), isError = true)
            }

            !state.loaded -> item(key = "loading") {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            }

            state.items.isEmpty() -> item(key = "empty") {
                HistoryNoticeBanner(
                    message = if (state.isFiltered) stringResource(R.string.history_empty_filtered) else stringResource(R.string.history_empty),
                    isError = false,
                )
            }

            else -> items(items = state.items, key = { item -> item.id }) { item ->
                ModernHistoryCard(
                    item = item,
                    onDelete = { pendingDelete = item },
                )
            }
        }

        item(key = "footer") {
            if (state.items.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (state.loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else if (state.endReached) {
                        Text(
                            text = "End of notification history",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        OutlinedButton(
                            onClick = onLoadMore,
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(stringResource(R.string.history_load_more), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Dialogs
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
            title = { Text(stringResource(R.string.history_delete_confirm_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.history_delete_confirm_body, deleting.appName)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        onDeleteEntry(deleting.id)
                    },
                ) {
                    Text(stringResource(R.string.history_delete), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (confirmingClearAll) {
        AlertDialog(
            onDismissRequest = { confirmingClearAll = false },
            title = { Text(stringResource(R.string.history_clear_all_confirm_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.history_clear_all_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingClearAll = false
                        onClearAll()
                    },
                ) {
                    Text(stringResource(R.string.history_clear_all_button), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingClearAll = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun ModernHistoryCard(
    item: HistoryItem,
    onDelete: () -> Unit,
) {
    val relativeTime = remember(item.recordedAtMillis) {
        DateUtils.getRelativeTimeSpanString(
            item.recordedAtMillis,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
        ).toString()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 8.dp, bottomEnd = 18.dp, bottomStart = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // App Name + Status + Delete Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.size(30.dp),
                        shape = RoundedCornerShape(topStart = 10.dp, topEnd = 5.dp, bottomEnd = 10.dp, bottomStart = 5.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = item.appName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    HistoryStatusPill(status = item.status)
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = stringResource(R.string.history_delete_entry, item.appName),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            // Reason / Skip Cause Tag
            if (item.reason != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        text = "Reason: ${skipReasonLabel(item.reason)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }

            // Sender
            if (item.sender != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.sender,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            // Message text
            if (item.messageText != null) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = item.messageText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 18.sp,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Timestamp + Updated indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = relativeTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                )

                if (item.isUpdate) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Text(
                            text = "Updated",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            fontSize = 10.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryStatusPill(status: HistoryStatus) {
    val (bgColor, textColor, label) = when (status) {
        HistoryStatus.ANNOUNCED -> Triple(Color(0xFF064E3B), Color(0xFF34D399), "Spoken")
        HistoryStatus.SKIPPED -> Triple(Color(0xFF78350F), Color(0xFFFBBF24), "Skipped")
        HistoryStatus.QUEUED -> Triple(Color(0xFF1E3A8A), Color(0xFF93C5FD), "Queued")
        HistoryStatus.PENDING -> Triple(Color(0xFF1E3A8A), Color(0xFF93C5FD), "Pending")
        HistoryStatus.INTERRUPTED -> Triple(Color(0xFF7F1D1D), Color(0xFFFCA5A5), "Interrupted")
    }

    Surface(
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 4.dp, bottomEnd = 8.dp, bottomStart = 4.dp),
        color = bgColor,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun HistoryNoticeBanner(message: String, isError: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 8.dp, bottomEnd = 18.dp, bottomStart = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceContainer,
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isError) Icons.Rounded.Close else Icons.Rounded.History,
                contentDescription = null,
                tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HistorySettingsCard(
    settings: HistorySettingsState,
    onStorageClick: () -> Unit,
    onUpdatesClick: () -> Unit,
    onRetentionClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 10.dp, bottomEnd = 22.dp, bottomStart = 10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "History & Privacy Settings",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))

            HistorySettingRow(
                label = stringResource(R.string.history_storage_label),
                value = stringResource(storageLabelRes(settings.storageMode)),
                onClick = onStorageClick,
            )

            HistorySettingRow(
                label = stringResource(R.string.history_updates_label),
                value = stringResource(updateHandlingLabelRes(settings.updateHandling)),
                onClick = onUpdatesClick,
            )

            HistorySettingRow(
                label = stringResource(R.string.history_retention_label),
                value = settings.retentionDays?.let { stringResource(R.string.history_retention_days, it) }
                    ?: stringResource(R.string.history_retention_never),
                onClick = onRetentionClick,
            )
        }
    }
}

@Composable
private fun HistorySettingRow(
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

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
        title = { Text(text = title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                for (option in options) {
                    val isSelected = option == selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .selectable(
                                selected = isSelected,
                                role = Role.RadioButton,
                                onClick = { onSelect(option) },
                            )
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                        Text(
                            text = label(option),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
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

private const val DIALOG_NONE = 0
private const val DIALOG_STATUS = 1
private const val DIALOG_SORT = 2
private const val DIALOG_STORAGE = 3
private const val DIALOG_UPDATES = 4
private const val DIALOG_RETENTION = 5

private val RETENTION_OPTIONS: List<Int?> = listOf(null, 7, 30, 90)
