package com.jayanth.hearit.history

import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.jayanth.hearit.R
import com.jayanth.hearit.applications.FilterTypeInfoRow
import com.jayanth.hearit.core.common.model.HistoryStatus
import com.jayanth.hearit.core.common.model.HistoryStorageMode
import com.jayanth.hearit.core.common.model.HistoryUpdateHandling
import com.jayanth.hearit.core.common.model.KeywordMatchMode
import com.jayanth.hearit.core.common.model.SkipReason
import com.jayanth.hearit.ui.components.StatusPill
import com.jayanth.hearit.ui.components.SwipeToDeleteContainer
import com.jayanth.hearit.ui.components.verticalScrollbar

/**
 * Entry point of the History feature (master prompt §12, phase P18).
 */
@Composable
fun HistoryRoute(
    viewModel: HistoryViewModel,
    onOpenAppSettings: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = context as? LifecycleOwner

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.userMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

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

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) {
            viewModel.exportToUri(context.contentResolver, uri, asCsv = true)
        }
    }
    val txtExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri != null) {
            viewModel.exportToUri(context.contentResolver, uri, asCsv = false)
        }
    }

    HistoryScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onQueryChanged = viewModel::onQueryChanged,
        onStatusChanged = viewModel::onStatusChanged,
        onSortChanged = viewModel::onSortChanged,
        onDatePresetSelected = viewModel::onDatePresetSelected,
        onCustomDateRangeSelected = viewModel::onCustomDateRangeSelected,
        onRequestExportCsv = {
            val stamp = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            csvExportLauncher.launch("hearit_history_$stamp.csv")
        },
        onRequestExportTxt = {
            val stamp = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            txtExportLauncher.launch("hearit_history_$stamp.txt")
        },
        onAppFilterChanged = viewModel::onAppFilterChanged,
        onOpenAppSettings = onOpenAppSettings,
        onPlayEntry = viewModel::onPlayEntry,
        onConfirmPlayBypass = viewModel::onConfirmPlayBypass,
        onDismissPlayBlocked = viewModel::onDismissPlayBlocked,
        onStopPlaying = viewModel::onStopPlaying,
        onAddFilter = viewModel::onAddFilter,
        onLoadMore = viewModel::onLoadMore,
        onDeleteEntry = viewModel::onDeleteEntry,
        onDeleteMultiple = viewModel::onDeleteMultiple,
        onClearAll = viewModel::onClearAll,
        onStorageModeChanged = viewModel::onStorageModeChanged,
        onUpdateHandlingChanged = viewModel::onUpdateHandlingChanged,
        onRetentionChanged = viewModel::onRetentionChanged,
        modifier = modifier,
    )
}

/**
 * Modern Material 3 Browsable History Screen with high density and quick filtering.
 */
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onQueryChanged: (String) -> Unit,
    onStatusChanged: (HistoryStatus?) -> Unit,
    onSortChanged: (HistorySortOrder) -> Unit,
    onDatePresetSelected: (HistoryDatePreset) -> Unit = {},
    onCustomDateRangeSelected: (Long, Long) -> Unit = { _, _ -> },
    onRequestExportCsv: () -> Unit = {},
    onRequestExportTxt: () -> Unit = {},
    onAppFilterChanged: (String?, String?) -> Unit,
    onOpenAppSettings: (String) -> Unit = {},
    onPlayEntry: (HistoryItem) -> Unit,
    onConfirmPlayBypass: () -> Unit = {},
    onDismissPlayBlocked: () -> Unit = {},
    onStopPlaying: () -> Unit,
    onAddFilter: (String, KeywordMatchMode, Boolean, String?) -> Unit,
    onLoadMore: () -> Unit,
    onDeleteEntry: (Long) -> Unit,
    onDeleteMultiple: (Set<Long>) -> Unit = {},
    onClearAll: () -> Unit,
    onStorageModeChanged: (HistoryStorageMode) -> Unit,
    onUpdateHandlingChanged: (HistoryUpdateHandling) -> Unit,
    onRetentionChanged: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var openDialog by rememberSaveable { mutableStateOf(DIALOG_NONE) }
    var pendingDelete by remember { mutableStateOf<HistoryItem?>(null) }
    var confirmingClearAll by rememberSaveable { mutableStateOf(false) }
    var confirmingDeleteSelected by rememberSaveable { mutableStateOf(false) }
    var detailItem by remember { mutableStateOf<HistoryItem?>(null) }
    var filterItem by remember { mutableStateOf<HistoryItem?>(null) }
    var selectedIds by rememberSaveable { mutableStateOf(emptySet<Long>()) }
    var isSelectionMode by rememberSaveable { mutableStateOf(false) }

    state.pendingPlayBlocked?.let { pending ->
        val reasonText = when (pending.reason) {
            com.jayanth.hearit.core.common.model.SkipReason.APP_DISABLED ->
                "${pending.item.appName} is disabled for announcements."
            com.jayanth.hearit.core.common.model.SkipReason.SCREEN_ON -> "Screen is on."
            com.jayanth.hearit.core.common.model.SkipReason.SCREEN_OFF -> "Screen is off."
            com.jayanth.hearit.core.common.model.SkipReason.UNLOCKED -> "Device is unlocked."
            com.jayanth.hearit.core.common.model.SkipReason.LOCKED -> "Device is locked."
            com.jayanth.hearit.core.common.model.SkipReason.MUTED -> "Announcements are temporarily muted."
            com.jayanth.hearit.core.common.model.SkipReason.CALL_ACTIVE -> "A call is in progress."
            com.jayanth.hearit.core.common.model.SkipReason.DND -> "Do Not Disturb is on."
            com.jayanth.hearit.core.common.model.SkipReason.TOO_LONG -> "Message is too long."
            com.jayanth.hearit.core.common.model.SkipReason.KEYWORD_FILTERED -> "Filtered by keyword."
            com.jayanth.hearit.core.common.model.SkipReason.OUTPUT_NOT_ALLOWED -> "Audio output is not allowed."
            com.jayanth.hearit.core.common.model.SkipReason.PRIVACY_EXCLUDED_APP -> "Excluded by privacy settings."
            com.jayanth.hearit.core.common.model.SkipReason.PRIVACY_OTP -> "OTP/code content is hidden."
            com.jayanth.hearit.core.common.model.SkipReason.PRIVACY_PASSWORD -> "Password content is hidden."
            com.jayanth.hearit.core.common.model.SkipReason.PRIVACY_PAYMENT -> "Payment content is hidden."
            com.jayanth.hearit.core.common.model.SkipReason.PRIVACY_BANKING -> "Banking content is hidden."
            com.jayanth.hearit.core.common.model.SkipReason.PRIVACY_SENSITIVE_KEYWORD -> "Sensitive keyword matched."
            com.jayanth.hearit.core.common.model.SkipReason.LOW_BATTERY -> "Low battery or battery saver is blocking playback."
            com.jayanth.hearit.core.common.model.SkipReason.PROXIMITY_UNCOVERED -> "Pocket/proximity check blocked playback."
            com.jayanth.hearit.core.common.model.SkipReason.SUPPRESSED_INTELLIGENCE -> "Filtered by active intelligence."
            else -> "Can't play: ${pending.reason.name.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() }}."
        }
        val note = if (pending.reason == com.jayanth.hearit.core.common.model.SkipReason.APP_DISABLED) {
            " Still want to play this notification? Other rules and filters will still be checked."
        } else {
            " Play anyway? Other rules and filters will still be checked."
        }
        AlertDialog(
            onDismissRequest = onDismissPlayBlocked,
            title = { Text(if (pending.reason == com.jayanth.hearit.core.common.model.SkipReason.APP_DISABLED) "App is disabled" else "Can't play") },
            text = { Text(reasonText + note) },
            confirmButton = {
                TextButton(onClick = onConfirmPlayBypass) { Text("Play anyway") }
            },
            dismissButton = {
                TextButton(onClick = onDismissPlayBlocked) { Text("Cancel") }
            },
        )
    }

    val groupedItems = remember(state.items) {
        state.items.groupBy { getDateGroup(it.recordedAtMillis) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
        item(key = "header_spacing") {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Compact Search Bar & Horizontal Filter Chips
        item(key = "search_and_filters") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.filter.query,
                    onValueChange = onQueryChanged,
                    placeholder = {
                        Text(
                            text = "Search by app, sender, or message…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    trailingIcon = {
                        if (state.filter.query.isNotBlank()) {
                            IconButton(onClick = { onQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                )

                // Filter & Sort Horizontal Scroll Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // App filter pill
                    FilterChip(
                        selected = state.filter.selectedPackageName != null,
                        onClick = { openDialog = DIALOG_APP },
                        label = {
                            Text(
                                text = state.filter.selectedAppName ?: "Apps",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.Apps, contentDescription = null, modifier = Modifier.size(15.dp))
                        },
                        shape = RoundedCornerShape(12.dp),
                    )

                    // Status pill
                    FilterChip(
                        selected = state.filter.status != null,
                        onClick = { openDialog = DIALOG_STATUS },
                        label = {
                            Text(
                                text = state.filter.status?.let { historyStatusLabel(it) } ?: "Status",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.FilterList, contentDescription = null, modifier = Modifier.size(15.dp))
                        },
                        shape = RoundedCornerShape(12.dp),
                    )

                    // Date Range pill (#9)
                    FilterChip(
                        selected = state.filter.datePreset != HistoryDatePreset.ALL,
                        onClick = { openDialog = DIALOG_DATE },
                        label = {
                            Text(
                                text = historyDateChipLabel(state.filter),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.DateRange, contentDescription = null, modifier = Modifier.size(15.dp))
                        },
                        shape = RoundedCornerShape(12.dp),
                    )

                    // Sort pill
                    FilterChip(
                        selected = state.filter.sort != HistorySortOrder.RecentFirst,
                        onClick = { openDialog = DIALOG_SORT },
                        label = {
                            Text(
                                text = when (state.filter.sort) {
                                    HistorySortOrder.RecentFirst -> "Newest"
                                    HistorySortOrder.OldestFirst -> "Oldest"
                                    HistorySortOrder.ByApp -> "By App"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = null, modifier = Modifier.size(15.dp))
                        },
                        shape = RoundedCornerShape(12.dp),
                    )

                    if (state.items.isNotEmpty()) {
                        // Export CSV/TXT pill (#10)
                        FilterChip(
                            selected = false,
                            onClick = { openDialog = DIALOG_EXPORT },
                            label = {
                                Text(
                                    text = "Export",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(15.dp))
                            },
                            shape = RoundedCornerShape(12.dp),
                        )

                        // Multi-select toggle chip
                        FilterChip(
                            selected = isSelectionMode,
                            onClick = {
                                isSelectionMode = !isSelectionMode
                                if (!isSelectionMode) selectedIds = emptySet()
                            },
                            label = {
                                Text(
                                    text = if (isSelectionMode) "Done" else "Select",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isSelectionMode) Icons.Rounded.Check else Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                        ) {
                            IconButton(
                                onClick = { confirmingClearAll = true },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = stringResource(R.string.history_clear_all_button),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))
                }

                // Selection Mode Contextual Banner
                if (isSelectionMode) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "${selectedIds.size} selected",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    onClick = {
                                        selectedIds = if (selectedIds.size == state.items.size) {
                                            emptySet()
                                        } else {
                                            state.items.map { it.id }.toSet()
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = if (selectedIds.size == state.items.size) "Deselect All" else "Select All",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                if (selectedIds.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { confirmingDeleteSelected = true },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Delete selected",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
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

        // Section Title: Notification History
        item(key = "timeline_header") {
            Text(
                text = "Notification History",
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
                ModernHistoryEmptyCard(
                    isFiltered = state.isFiltered,
                    query = state.filter.query,
                    onResetFilters = {
                        onQueryChanged("")
                        onAppFilterChanged(null, null)
                        onStatusChanged(null)
                    },
                )
            }

            else -> {
                groupedItems.forEach { (dateGroup, itemsInGroup) ->
                    item(key = "header_$dateGroup") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = dateGroup,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                            ) {
                                Text(
                                    text = "${itemsInGroup.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                )
                            }
                        }
                    }
                    items(items = itemsInGroup, key = { item -> item.id }) { item ->
                        val isSelected = item.id in selectedIds
                        SwipeToDeleteContainer(
                            onDelete = { onDeleteEntry(item.id) },
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            ModernHistoryCard(
                                item = item,
                                isPlaying = state.playingItemId == item.id,
                                isSelectionMode = isSelectionMode,
                                isSelected = isSelected,
                                onToggleSelect = {
                                    selectedIds = if (isSelected) selectedIds - item.id else selectedIds + item.id
                                },
                                onPlay = { onPlayEntry(item) },
                                onAppClick = { onOpenAppSettings(item.packageName) },
                                onCardClick = {
                                    if (isSelectionMode) {
                                        selectedIds = if (isSelected) selectedIds - item.id else selectedIds + item.id
                                    } else {
                                        detailItem = item
                                    }
                                },
                                onDelete = { pendingDelete = item },
                            )
                        }
                    }
                }
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
            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // App Picker Dialog
    if (openDialog == DIALOG_APP) {
        val appOptions: List<Pair<String?, String>> = listOf<Pair<String?, String>>(null to "All Apps") + state.availableApps
        val currentApp: Pair<String?, String> = state.filter.selectedPackageName to (state.filter.selectedAppName ?: "All Apps")
        ChoiceDialog<Pair<String?, String>>(
            title = "Filter by App",
            options = appOptions,
            selected = currentApp,
            label = { pair -> pair.second },
            onSelect = { selected ->
                openDialog = DIALOG_NONE
                onAppFilterChanged(selected.first, if (selected.first == null) null else selected.second)
            },
            onDismiss = { openDialog = DIALOG_NONE },
        )
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
            title = "Update Handling",
            options = HistoryUpdateHandling.entries,
            selected = state.settings.updateHandling,
            label = { handling ->
                when (handling) {
                    HistoryUpdateHandling.UPDATE_ENTRY -> "Update"
                    HistoryUpdateHandling.NEW_ENTRY -> "Separate"
                }
            },
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

        DIALOG_DATE -> HistoryDatePresetDialog(
            selected = state.filter.datePreset,
            onSelectPreset = { preset -> openDialog = DIALOG_NONE; onDatePresetSelected(preset) },
            onOpenCustomPicker = { openDialog = DIALOG_DATE_CUSTOM },
            onDismiss = { openDialog = DIALOG_NONE },
        )

        DIALOG_DATE_CUSTOM -> HistoryCustomDateRangePickerDialog(
            initialStartMillis = state.filter.dateFromMillis,
            initialEndMillis = state.filter.dateToMillis,
            onConfirm = { start, end ->
                openDialog = DIALOG_NONE
                onCustomDateRangeSelected(start, end)
            },
            onDismiss = { openDialog = DIALOG_NONE },
        )

        DIALOG_EXPORT -> HistoryExportFormatDialog(
            entryCountShown = state.items.size,
            onExportCsv = { openDialog = DIALOG_NONE; onRequestExportCsv() },
            onExportTxt = { openDialog = DIALOG_NONE; onRequestExportTxt() },
            onDismiss = { openDialog = DIALOG_NONE },
        )
    }

    // Details Dialog Popup
    detailItem?.let { item ->
        HistoryDetailDialog(
            item = item,
            isPlaying = state.playingItemId == item.id,
            onPlay = { onPlayEntry(item) },
            onFilterThis = {
                detailItem = null
                filterItem = item
            },
            onDelete = {
                detailItem = null
                pendingDelete = item
            },
            onDismiss = { detailItem = null },
        )
    }

    // Filter This Dialog
    filterItem?.let { item ->
        FilterThisDialog(
            item = item,
            onAddFilter = { keyword, mode, isSender, targetPkg ->
                onAddFilter(keyword, mode, isSender, targetPkg)
                filterItem = null
            },
            onDismiss = { filterItem = null },
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

    if (confirmingDeleteSelected) {
        AlertDialog(
            onDismissRequest = { confirmingDeleteSelected = false },
            title = { Text("Delete ${selectedIds.size} announcements?", fontWeight = FontWeight.Bold) },
            text = { Text("The selected notification history entries will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingDeleteSelected = false
                        onDeleteMultiple(selectedIds)
                        selectedIds = emptySet()
                        isSelectionMode = false
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDeleteSelected = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                .zIndex(99f),
        )
    }
}

private fun getDateGroup(recordedAtMillis: Long): String {
    val now = java.util.Calendar.getInstance()
    val itemCal = java.util.Calendar.getInstance().apply { timeInMillis = recordedAtMillis }

    val nowDay = now.get(java.util.Calendar.DAY_OF_YEAR)
    val nowYear = now.get(java.util.Calendar.YEAR)
    val itemDay = itemCal.get(java.util.Calendar.DAY_OF_YEAR)
    val itemYear = itemCal.get(java.util.Calendar.YEAR)

    return when {
        nowYear == itemYear && nowDay == itemDay -> "Today"
        nowYear == itemYear && nowDay - itemDay == 1 -> "Yesterday"
        nowYear == itemYear && nowDay - itemDay in 2..6 -> "Earlier this week"
        else -> java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.getDefault()).format(java.util.Date(recordedAtMillis))
    }
}

@Composable
private fun ModernHistoryCard(
    item: HistoryItem,
    isPlaying: Boolean,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onPlay: () -> Unit,
    onAppClick: () -> Unit,
    onCardClick: () -> Unit,
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceContainer,
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Selection Indicator + App Icon + App Name (full width!) + Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            enabled = !isSelectionMode,
                            role = Role.Button,
                            onClickLabel = "Open ${item.appName} settings",
                            onClick = onAppClick,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isSelectionMode) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            border = BorderStroke(1.5.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .size(22.dp)
                                .clickable(onClick = onToggleSelect),
                        ) {
                            if (isSelected) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.appName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = relativeTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                StatusPill(status = item.status)
            }

            // Reason / Skip Cause Tag
            if (item.reason != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                ) {
                    val labelPrefix = if (item.status == HistoryStatus.INTERRUPTED) "Interrupted: " else "Reason: "
                    Text(
                        text = "$labelPrefix${skipReasonLabel(item.reason)}",
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

            // Message text (Truncated to 2 lines with ...)
            if (item.messageText != null) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = item.messageText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer Action Row: Update badge (left) + Action Buttons (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (item.isUpdate) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Text(
                            text = "Update",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(
                        onClick = onPlay,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Stop" else "Listen",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Full Notification Detail Popup on Card Click.
 */
@Composable
private fun HistoryDetailDialog(
    item: HistoryItem,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onFilterThis: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val relativeTime = remember(item.recordedAtMillis) {
        DateUtils.getRelativeTimeSpanString(
            item.recordedAtMillis,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
        ).toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = relativeTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusPill(status = item.status)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val detailScrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(detailScrollState)
                        .verticalScrollbar(detailScrollState)
                        .padding(end = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (item.sender != null) {
                        Column {
                            Text(
                                text = "Sender",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = item.sender,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Notification Content",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        SelectionContainer {
                            Text(
                                text = item.messageText ?: "(No content stored)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    if (item.reason != null) {
                        Text(
                            text = "Skip Reason: ${skipReasonLabel(item.reason)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Actions: Play and Filter This
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onPlay,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isPlaying) "Stop" else "Listen", maxLines = 1)
                    }

                    val isAlreadyFiltered = item.reason == com.jayanth.hearit.core.common.model.SkipReason.KEYWORD_FILTERED ||
                        item.reason == com.jayanth.hearit.core.common.model.SkipReason.PRIVACY_SENSITIVE_KEYWORD ||
                        item.reason == com.jayanth.hearit.core.common.model.SkipReason.SUPPRESSED_INTELLIGENCE ||
                        item.reason == com.jayanth.hearit.core.common.model.SkipReason.PRIVACY_EXCLUDED_APP

                    if (!isAlreadyFiltered) {
                        Button(
                            onClick = onFilterThis,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Filter this", maxLines = 1)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDelete) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

/**
 * Interactive "Filter This" Dialog to generate custom filter rules from a notification.
 */
@Composable
private fun FilterThisDialog(
    item: HistoryItem,
    onAddFilter: (keyword: String, mode: KeywordMatchMode, isSender: Boolean, targetPackage: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var keywordText by remember { mutableStateOf(item.messageText ?: item.sender ?: "") }
    var selectedScope by remember { mutableStateOf(0) } // 0 = This App, 1 = All Apps
    var selectedFilterKind by remember { mutableStateOf("Phrase") } // "Word", "Phrase", "Exact", "Sender"

    val selectedMode = when (selectedFilterKind) {
        "Word" -> KeywordMatchMode.WHOLE_WORD
        "Exact" -> KeywordMatchMode.EXACT
        else -> KeywordMatchMode.PHRASE
    }
    val isSender = selectedFilterKind == "Sender"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Filter Notification",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Block notifications matching this keyword, pattern, or sender from being spoken.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Scope Picker
                Text(
                    text = "App Scope",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedScope == 0,
                        onClick = { selectedScope = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    ) {
                        Text(item.appName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    SegmentedButton(
                        selected = selectedScope == 1,
                        onClick = { selectedScope = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    ) {
                        Text("All Apps", maxLines = 1)
                    }
                }

                // Quick presets
                Text(
                    text = "Quick Presets",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val sender = item.sender
                    if (!sender.isNullOrBlank()) {
                        FilterChip(
                            selected = keywordText == sender && isSender,
                            onClick = {
                                keywordText = sender
                                selectedFilterKind = "Sender"
                            },
                            label = { Text("Sender: $sender", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            shape = RoundedCornerShape(8.dp),
                        )
                    }

                    val message = item.messageText
                    if (!message.isNullOrBlank()) {
                        FilterChip(
                            selected = keywordText == message && selectedFilterKind == "Exact",
                            onClick = {
                                keywordText = message
                                selectedFilterKind = "Exact"
                            },
                            label = { Text("Exact Message", style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                            shape = RoundedCornerShape(8.dp),
                        )

                        val firstWord = message.split(Regex("\\s+")).firstOrNull { it.length > 3 && it.all { c -> c.isLetterOrDigit() } }
                        if (firstWord != null) {
                            FilterChip(
                                selected = keywordText == firstWord && selectedFilterKind == "Word",
                                onClick = {
                                    keywordText = firstWord
                                    selectedFilterKind = "Word"
                                },
                                label = { Text("Word: $firstWord", style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                                shape = RoundedCornerShape(8.dp),
                            )
                        }

                        val pattern = message.replace(Regex("\\d+")) { "$1" }
                        if (pattern.contains("$1")) {
                            FilterChip(
                                selected = keywordText == pattern && selectedFilterKind == "Phrase",
                                onClick = {
                                    keywordText = pattern
                                    selectedFilterKind = "Phrase"
                                },
                                label = { Text("Pattern ($1)", style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                                shape = RoundedCornerShape(8.dp),
                            )
                        }
                    }
                }

                // Editable keyword text
                OutlinedTextField(
                    value = keywordText,
                    onValueChange = { keywordText = it },
                    label = { Text(if (isSender) "Sender Name to Block" else "Keyword or Template (use $1 or * for wildcards)") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )

                var showFilterTypeInfo by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Filter Type",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        IconButton(
                            onClick = { showFilterTypeInfo = !showFilterTypeInfo },
                            modifier = Modifier.size(20.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = "Filter types info",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }

                    Text(
                        text = when (selectedFilterKind) {
                            "Word" -> "Whole word match"
                            "Phrase" -> "Pattern or phrase"
                            "Exact" -> "Exact full text"
                            "Sender" -> "Block by sender"
                            else -> ""
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (showFilterTypeInfo) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            FilterTypeInfoRow("Word", "Blocks if message contains this standalone whole word.")
                            FilterTypeInfoRow("Phrase", "Blocks phrases or patterns (supports wildcards * and numbers $1).")
                            FilterTypeInfoRow("Exact", "Blocks only when the notification text matches this entire message.")
                            FilterTypeInfoRow("Sender", "Blocks all notifications from this sender or contact name.")
                        }
                    }
                }

                // Match Mode Segmented
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        val kinds = listOf("Word", "Phrase", "Exact", "Sender")
                        kinds.forEach { kind ->
                            val isSelected = selectedFilterKind == kind
                            val pillBg by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                label = "filterKindPillBg",
                            )
                            val pillText by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                label = "filterKindPillText",
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(pillBg)
                                    .clickable {
                                        selectedFilterKind = kind
                                        if (kind == "Sender" && !item.sender.isNullOrBlank()) {
                                            keywordText = item.sender ?: ""
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = kind,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = pillText,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (keywordText.isNotBlank()) {
                        val targetPkg = if (selectedScope == 0) item.packageName else null
                        onAddFilter(keywordText.trim(), selectedMode, isSender, targetPkg)
                    }
                },
                enabled = keywordText.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("Add Filter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun ModernHistoryEmptyCard(
    isFiltered: Boolean,
    query: String,
    onResetFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isFiltered) Icons.Rounded.SearchOff else Icons.Rounded.NotificationsNone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (isFiltered) "No matching notifications" else "No notification history yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isFiltered) {
                    if (query.isNotBlank()) "No recorded notifications match \"${query.trim()}\". Try another term or clear the filter."
                    else "No notifications match the active app or status filter. Try resetting filters."
                } else {
                    "Notifications from enabled apps will be recorded here in real-time as they arrive, allowing you to listen, search, and manage rules."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
            )

            if (isFiltered) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onResetFilters,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset Filters", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun HistoryNoticeBanner(message: String, isError: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceContainer,
        ),
        border = BorderStroke(
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "History & Storage Rules",
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
                label = "Update Handling",
                value = when (settings.updateHandling) {
                    HistoryUpdateHandling.UPDATE_ENTRY -> "Update"
                    HistoryUpdateHandling.NEW_ENTRY -> "Separate"
                },
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
            .heightIn(min = 40.dp)
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                items(options) { option ->
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
                            .padding(vertical = 4.dp),
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
                item {
                    Spacer(modifier = Modifier.height(16.dp))
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

private const val DIALOG_NONE = 0
private const val DIALOG_STATUS = 1
private const val DIALOG_SORT = 2
private const val DIALOG_STORAGE = 3
private const val DIALOG_UPDATES = 4
private const val DIALOG_RETENTION = 5
private const val DIALOG_APP = 6
private const val DIALOG_DATE = 7
private const val DIALOG_DATE_CUSTOM = 8
private const val DIALOG_EXPORT = 9

private val RETENTION_OPTIONS: List<Int?> = listOf(null, 7, 30, 90)
