package com.jayanth.hearit.applications

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.jayanth.hearit.R
import com.jayanth.hearit.core.common.model.AppPriority
import com.jayanth.hearit.core.common.model.AudioFocusPolicy
import com.jayanth.hearit.core.common.model.HistoryStatus
import com.jayanth.hearit.core.common.model.HistoryStorageMode
import com.jayanth.hearit.core.common.model.HistoryUpdateHandling
import com.jayanth.hearit.core.common.model.KeywordMatchMode
import com.jayanth.hearit.core.database.HistoryEntry
import com.jayanth.hearit.core.rules.KeywordMatcher
import com.jayanth.hearit.history.skipReasonLabel
import com.jayanth.hearit.ui.components.verticalScrollbar

/**
 * Entry point of the Applications feature (master prompt §5, phase P13).
 */
@Composable
fun ApplicationsRoute(
    viewModel: ApplicationsViewModel,
    onDetailBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val settings = state.settings
    val snackbarHostState = remember { SnackbarHostState() }
    var filterType by rememberSaveable { mutableIntStateOf(0) } // 0 = All, 1 = Selected, 2 = Unselected (persisted across details view & configuration changes)

    LaunchedEffect(Unit) {
        viewModel.userMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    BackHandler(enabled = settings != null) {
        if (onDetailBack != null) onDetailBack() else viewModel.onCloseApp()
    }
    BackHandler(enabled = settings == null && state.isSelectionMode) { viewModel.onClearSelection() }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = settings != null,
            transitionSpec = {
                if (targetState) {
                    slideInHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) { it } +
                        fadeIn(animationSpec = tween(280)) togetherWith
                        slideOutHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) { -it } +
                        fadeOut(animationSpec = tween(280))
                } else {
                    slideInHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) { -it } +
                        fadeIn(animationSpec = tween(280)) togetherWith
                        slideOutHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) { it } +
                        fadeOut(animationSpec = tween(280))
                }
            },
            label = "AppSettingsNavigationTransition",
            modifier = Modifier.fillMaxSize(),
        ) { isShowingSettings ->
            if (!isShowingSettings || settings == null) {
                ApplicationsListScreen(
                    state = state,
                    icons = viewModel.icons,
                    filterType = filterType,
                    onFilterTypeChange = { filterType = it },
                    onQueryChange = viewModel::onQueryChange,
                    onToggleApp = viewModel::onToggleApp,
                    onTogglePriorityAnnounce = viewModel::onTogglePriorityAnnounce,
                    onOpenApp = viewModel::onOpenApp,
                    onSelectAllVisible = viewModel::onSelectAllVisible,
                    onDeselectAllVisible = viewModel::onDeselectAllVisible,
                    onToggleSelectionMode = viewModel::onToggleSelectionMode,
                    onToggleSelectPackage = viewModel::onToggleSelectPackage,
                    onSelectAllForBatch = viewModel::onSelectAllForBatch,
                    onClearSelection = viewModel::onClearSelection,
                    onBatchSetEnabled = viewModel::onBatchSetEnabled,
                    onBatchSetPriorityAnnounce = viewModel::onBatchSetPriorityAnnounce,
                    onBatchResetToGlobal = viewModel::onBatchResetToGlobal,
                    onSetAppCategory = viewModel::onSetAppCategory,
                    onResetAppCategory = viewModel::onResetAppCategory,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                AppSettingsScreen(
                    state = settings,
                    icons = viewModel.icons,
                    onBack = viewModel::onCloseApp,
                    onToggleEnabled = { viewModel.onToggleApp(settings.packageName) },
                    onTogglePriorityAnnounce = { viewModel.onTogglePriorityAnnounce(settings.packageName) },
                    onSpeechRateChanged = viewModel::onSpeechRateChanged,
                    onSpeechPitchChanged = viewModel::onSpeechPitchChanged,
                    onSpeechVoiceChanged = viewModel::onSpeechVoiceChanged,
                    onAutoDismissChanged = viewModel::onAutoDismissChanged,
                    onPriorityChanged = viewModel::onPriorityChanged,
                    onHistoryStorageModeChanged = viewModel::onHistoryStorageModeChanged,
                    onHistoryUpdateHandlingChanged = viewModel::onHistoryUpdateHandlingChanged,
                    onHistoryRetentionChanged = viewModel::onHistoryRetentionChanged,
                    onRespectDndChanged = viewModel::onRespectDndChanged,
                    onQueueDuringCallChanged = viewModel::onQueueDuringCallChanged,
                    onAudioFocusChanged = viewModel::onAudioFocusChanged,
                    onAddFilter = viewModel::onAddFilter,
                    onRemoveFilter = viewModel::onRemoveFilter,
                    onFilterModeChanged = viewModel::onFilterModeChanged,
                    onToggleFilterEnabled = viewModel::onToggleFilterEnabled,
                    onResetToGlobal = viewModel::onResetToGlobal,
                    onPlayHistoryEntry = viewModel::onPlayHistoryEntry,
                    onStopPlaying = viewModel::onStopPlaying,
                    onDeleteHistoryEntry = viewModel::onDeleteHistoryEntry,
                    onClearAppHistory = viewModel::onClearAppHistory,
                    onSpokenNameChanged = viewModel::onSpokenNameChanged,
                    modifier = Modifier.fillMaxSize(),
                )
            }
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

/**
 * The app list: search, filter/batch selection, and modern app cards.
 */
@Composable
private fun ApplicationsListScreen(
    state: ApplicationsUiState,
    icons: AppIconLoader,
    filterType: Int,
    onFilterTypeChange: (Int) -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleApp: (String) -> Unit,
    onTogglePriorityAnnounce: (String) -> Unit,
    onOpenApp: (String) -> Unit,
    onSelectAllVisible: (Collection<String>) -> Unit,
    onDeselectAllVisible: (Collection<String>) -> Unit,
    onToggleSelectionMode: (String?) -> Unit,
    onToggleSelectPackage: (String) -> Unit,
    onSelectAllForBatch: (Collection<String>) -> Unit,
    onClearSelection: () -> Unit,
    onBatchSetEnabled: (Boolean) -> Unit,
    onBatchSetPriorityAnnounce: (Boolean) -> Unit,
    onBatchResetToGlobal: () -> Unit,
    onSetAppCategory: (String, com.jayanth.hearit.core.common.model.AppCategory?) -> Unit = { _, _ -> },
    onResetAppCategory: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showSystemApps by rememberSaveable { mutableStateOf(false) }
    var showCategoriesDialog by rememberSaveable { mutableStateOf(false) }
    val displayedApps = remember(
        state.visibleApps,
        state.enabledApps,
        state.priorityAnnounceApps,
        filterType,
        showSystemApps,
    ) {
        val base = if (showSystemApps) state.visibleApps else state.visibleApps.filter { !it.isSystemApp || it.packageName in state.enabledApps }
        when (filterType) {
            1 -> base.filter { it.packageName in state.enabledApps }
            2 -> base.filter { it.packageName !in state.enabledApps }
            3 -> base.filter { it.packageName in state.priorityAnnounceApps }
            else -> base
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        // Morphe-style Capsule Search Bar (~52dp height, vertically centered with decorationBox, zero text cut-off)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )

                BasicTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (state.query.isEmpty()) {
                                Text(
                                    text = "Search installed apps…",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        fontSize = 15.sp,
                                    ),
                                    maxLines = 1,
                                )
                            }
                            innerTextField()
                        }
                    },
                )

                if (state.query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Tabs: All / Selected / Unselected (Full Row Modern Capsule Segments)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val filters = listOf(
                    0 to "All",
                    1 to "Enabled (${state.enabledApps.size})",
                    2 to "Off",
                    3 to "Priority (${state.priorityAnnounceApps.size})",
                )
                filters.forEach { (type, label) ->
                    val isSelected = filterType == type
                    val pillBg by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        label = "pillBg",
                    )
                    val textTint by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        label = "textTint",
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(11.dp))
                            .background(pillBg)
                            .clickable { onFilterTypeChange(type) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = textTint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = showSystemApps,
                onClick = { showSystemApps = !showSystemApps },
                label = {
                    Text(
                        text = if (showSystemApps) "System Shown" else "System Hidden",
                        fontSize = 11.5.sp,
                        maxLines = 1,
                    )
                },
                shape = RoundedCornerShape(10.dp),
            )
            FilterChip(
                selected = state.isSelectionMode,
                onClick = { onToggleSelectionMode(null) },
                leadingIcon = {
                    Icon(
                        imageVector = if (state.isSelectionMode) Icons.Rounded.Check else Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                    )
                },
                label = {
                    Text(
                        text = if (state.isSelectionMode) "Selecting (${state.selectedPackages.size})" else "Select",
                        fontSize = 11.5.sp,
                        maxLines = 1,
                    )
                },
                shape = RoundedCornerShape(10.dp),
            )
            FilterChip(
                selected = false,
                onClick = { showCategoriesDialog = true },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                    )
                },
                label = {
                    Text(
                        text = "Categories",
                        fontSize = 11.5.sp,
                        maxLines = 1,
                    )
                },
                shape = RoundedCornerShape(10.dp),
            )
        }

        if (showCategoriesDialog) {
            com.jayanth.hearit.intelligence.AppCategoriesDialog(
                apps = state.apps,
                appCategories = state.appCategories,
                onSetAppCategory = onSetAppCategory,
                onResetAppCategory = onResetAppCategory,
                onDismiss = { showCategoriesDialog = false },
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (!state.loading && !state.catalogUnavailable && displayedApps.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${displayedApps.size} ${if (displayedApps.size == 1) "app" else "apps"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )

                val anyUnselected = displayedApps.any { it.packageName !in state.enabledApps }
                val displayedPackageNames = remember(displayedApps) { displayedApps.map { it.packageName } }
                Surface(
                    onClick = {
                        if (anyUnselected) onSelectAllVisible(displayedPackageNames)
                        else onDeselectAllVisible(displayedPackageNames)
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Transparent,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    ) {
                        Icon(
                            imageVector = if (anyUnselected) Icons.Rounded.CheckCircle else Icons.Rounded.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (anyUnselected) "Enable all visible" else "Disable all visible",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

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

                displayedApps.isEmpty() -> {
                    ApplicationsEmptyCard(
                        query = state.query,
                        filterType = filterType,
                        onClearQuery = { onQueryChange("") },
                        onResetFilter = { onFilterTypeChange(0) },
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items = displayedApps, key = { app -> app.packageName }) { app ->
                        ApplicationRow(
                            app = app,
                            enabled = app.packageName in state.enabledApps,
                            isPriorityListed = app.packageName in state.priorityAnnounceApps,
                            hasCustomRules = app.packageName in state.customizedApps,
                            isSelectionMode = state.isSelectionMode,
                            isSelected = app.packageName in state.selectedPackages,
                            icons = icons,
                            onToggle = { onToggleApp(app.packageName) },
                            onTogglePriority = { onTogglePriorityAnnounce(app.packageName) },
                            onToggleSelect = { onToggleSelectPackage(app.packageName) },
                            onEnterSelectionMode = { onToggleSelectionMode(app.packageName) },
                            onOpenSettings = { onOpenApp(app.packageName) },
                        )
                    }
                    item { Spacer(modifier = Modifier.height(if (state.isSelectionMode) 190.dp else 100.dp)) }
                }
            }

            val allDisplayedSelected = displayedApps.isNotEmpty() && displayedApps.all { it.packageName in state.selectedPackages }
            androidx.compose.animation.AnimatedVisibility(
                visible = state.isSelectionMode,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                ApplicationsBatchActionBar(
                    selectedCount = state.selectedPackages.size,
                    allVisibleSelected = allDisplayedSelected,
                    onToggleSelectAll = { onSelectAllForBatch(displayedApps.map { it.packageName }) },
                    onEnableSelected = { onBatchSetEnabled(true) },
                    onDisableSelected = { onBatchSetEnabled(false) },
                    onAddToPriority = { onBatchSetPriorityAnnounce(true) },
                    onResetSelected = onBatchResetToGlobal,
                    onExitSelectionMode = onClearSelection,
                )
            }
        }
    }
}

@Composable
private fun ApplicationsEmptyCard(
    query: String,
    filterType: Int,
    onClearQuery: () -> Unit,
    onResetFilter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 36.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val icon = when {
                        query.isNotBlank() -> Icons.Rounded.SearchOff
                        filterType == 1 -> Icons.Rounded.NotificationsOff
                        filterType == 2 -> Icons.Rounded.CheckCircle
                        filterType == 3 -> Icons.Rounded.Star
                        else -> Icons.Rounded.Apps
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val title = when {
                query.isNotBlank() -> "No matching apps found"
                filterType == 1 -> "No enabled apps"
                filterType == 2 -> "All apps enabled"
                filterType == 3 -> "No priority apps yet"
                else -> "No applications available"
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(6.dp))

            val description = when {
                query.isNotBlank() -> "No installed apps match \"${query.trim()}\". Try another term or clear the search query."
                filterType == 1 -> "You haven't enabled speech announcements for any apps yet. Switch to 'All' or 'Off' to turn them on."
                filterType == 2 -> "Every installed application currently has announcement permissions enabled."
                filterType == 3 -> "Tap the star button on any app to add it to your Priority Announce quick-access list."
                else -> "No installed apps were detected on this device."
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
            )

            if (query.isNotBlank()) {
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedButton(
                    onClick = onClearQuery,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear search", fontWeight = FontWeight.SemiBold)
                }
            } else if (filterType in 1..3) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onResetFilter,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text("Show All Apps", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ApplicationRow(
    app: AppEntry,
    enabled: Boolean,
    isPriorityListed: Boolean,
    hasCustomRules: Boolean = false,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    icons: AppIconLoader,
    onToggle: () -> Unit,
    onTogglePriority: () -> Unit,
    onToggleSelect: () -> Unit,
    onEnterSelectionMode: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = { if (isSelectionMode) onToggleSelect() else onOpenSettings() },
                onLongClick = { if (!isSelectionMode) onEnterSelectionMode() else onToggleSelect() },
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                enabled -> MaterialTheme.colorScheme.surfaceContainerHigh
                else -> MaterialTheme.colorScheme.surfaceContainer
            },
        ),
        border = BorderStroke(
            1.dp,
            when {
                isSelected -> MaterialTheme.colorScheme.primary
                enabled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            },
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
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AppIcon(icons = icons, packageName = app.packageName)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // App Label & Package (tapping opens settings)
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (enabled) FontWeight.SemiBold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (hasCustomRules) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        ) {
                            Text(
                                text = "Custom",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                )
            } else {
                // 1-tap Priority Star (#5)
                IconButton(
                    onClick = onTogglePriority,
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        imageVector = if (isPriorityListed) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = if (isPriorityListed) "Remove from Priority List" else "Add to Priority List",
                        tint = if (isPriorityListed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp),
                    )
                }

                // Switch (ONLY toggles enable / disable)
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
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = stringResource(R.string.applications_open_settings),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp),
                    )
                }
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
    icons: AppIconLoader,
    onBack: () -> Unit,
    onToggleEnabled: () -> Unit,
    onTogglePriorityAnnounce: () -> Unit,
    onSpeechRateChanged: (Float?) -> Unit,
    onSpeechPitchChanged: (Float?) -> Unit,
    onSpeechVoiceChanged: (String?) -> Unit,
    onAutoDismissChanged: (Boolean?) -> Unit,
    onPriorityChanged: (AppPriority?) -> Unit,
    onHistoryStorageModeChanged: (HistoryStorageMode?) -> Unit,
    onHistoryUpdateHandlingChanged: (HistoryUpdateHandling?) -> Unit,
    onHistoryRetentionChanged: (Int?) -> Unit,
    onRespectDndChanged: (Boolean?) -> Unit,
    onQueueDuringCallChanged: (Boolean?) -> Unit,
    onAudioFocusChanged: (AudioFocusPolicy?) -> Unit,
    onAddFilter: (keyword: String, mode: KeywordMatchMode, isSender: Boolean) -> Unit,
    onRemoveFilter: (String) -> Unit,
    onFilterModeChanged: (KeywordMatchMode) -> Unit,
    onToggleFilterEnabled: (Boolean) -> Unit,
    onResetToGlobal: () -> Unit,
    onPlayHistoryEntry: (HistoryEntry) -> Unit,
    onStopPlaying: () -> Unit,
    onDeleteHistoryEntry: (Long) -> Unit,
    onClearAppHistory: (Int?) -> Unit,
    onSpokenNameChanged: (String?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Rules & Filters, 1 = App History
    var showAddFilterDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var filterTargetHistoryItem by remember { mutableStateOf<HistoryEntry?>(null) }
    var detailHistoryItem by remember { mutableStateOf<HistoryEntry?>(null) }
    var pendingDeleteHistoryItem by remember { mutableStateOf<HistoryEntry?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        // Top Navigation Header
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
            Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                Text(
                    text = state.label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    lineHeight = 24.sp,
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
            if (state.hasOverrides) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = "Custom Rules",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Hero Card: App Icon, Status, and Enabled Switch
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            border = BorderStroke(
                1.dp,
                if (state.enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        AppIcon(icons = icons, packageName = state.packageName)
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (state.enabled) "Enabled" else "Silenced",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (state.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (state.enabled) "Announces eligible alerts" else "Notifications silenced",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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

        Spacer(modifier = Modifier.height(12.dp))

        // Two-Tab Capsule Segmented Selector
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val tabs = listOf(
                    0 to "Rules & Filters",
                    1 to "App History (${state.recentHistory.size})",
                )
                tabs.forEach { (index, title) ->
                    val isSelected = selectedTab == index
                    val tabBg by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        label = "tabBg",
                    )
                    val textTint by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        label = "textTint",
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(11.dp))
                            .background(tabBg)
                            .clickable { selectedTab = index },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = textTint,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Content Area
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) { it } +
                            fadeIn(animationSpec = tween(280)) togetherWith
                            slideOutHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) { -it } +
                            fadeOut(animationSpec = tween(280))
                    } else {
                        slideInHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) { -it } +
                            fadeIn(animationSpec = tween(280)) togetherWith
                            slideOutHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) { it } +
                            fadeOut(animationSpec = tween(280))
                    }
                },
                label = "AppSettingsTabContentTransition",
                modifier = Modifier.fillMaxSize(),
            ) { tab ->
                if (tab == 0) {
                // Tab 0: Rules & Filters
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (state.enabled) {
                        // Custom Spoken Name Card (Idea 33)
                        var customSpokenName by remember(state.spokenName) { mutableStateOf(state.spokenName ?: "") }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Tune,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Column {
                                        Text(
                                            text = "Custom Spoken App Name",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = "Spoken nickname (e.g. 'Teams' instead of 'Microsoft Teams')",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp,
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = customSpokenName,
                                    onValueChange = { customSpokenName = it },
                                    placeholder = { Text("Default: ${state.label}") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = {
                                        if (customSpokenName.isNotBlank()) {
                                            IconButton(onClick = {
                                                customSpokenName = ""
                                                onSpokenNameChanged(null)
                                            }) {
                                                Icon(Icons.Rounded.Close, contentDescription = "Clear")
                                            }
                                        }
                                    },
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                ) {
                                    FilledTonalButton(
                                        onClick = { onSpokenNameChanged(customSpokenName.ifBlank { null }) },
                                        shape = RoundedCornerShape(10.dp),
                                    ) {
                                        Text("Save Name")
                                    }
                                }
                            }
                        }

                        // Content & Notification Filters Card
                        val filterSettings = state.keywordFilter.effective
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.FilterList,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Text(
                                        text = "Notification Filters",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                                Switch(
                                    checked = filterSettings.enabled,
                                    onCheckedChange = onToggleFilterEnabled,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Mute alerts matching specific blocked words, phrases, wildcard patterns, or senders.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            if (filterSettings.enabled) {
                                Spacer(modifier = Modifier.height(12.dp))

                                // Active Filter Chips
                                Text(
                                    text = "Active Filtered Items (${filterSettings.keywords.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                if (filterSettings.keywords.isEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(
                                            text = "No custom filter rules yet. Tap 'Add Filter Word / Pattern' below or filter past alerts directly from the App History tab.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(12.dp),
                                        )
                                    }
                                } else {
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        filterSettings.keywords.forEach { keyword ->
                                            val parsed = AppSettingsEditor.parseFilterItem(keyword)
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(5.dp),
                                                        color = MaterialTheme.colorScheme.primary,
                                                    ) {
                                                        Text(
                                                            text = parsed.type.uppercase(),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onPrimary,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                        )
                                                    }
                                                    Text(
                                                        text = parsed.text,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Rounded.Close,
                                                        contentDescription = "Remove filter",
                                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier
                                                            .size(16.dp)
                                                            .clip(CircleShape)
                                                            .clickable { onRemoveFilter(keyword) },
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedButton(
                                    onClick = { showAddFilterDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add Filter Word / Pattern", fontWeight = FontWeight.SemiBold)
                                }

                                if (filterSettings.keywords.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    Spacer(modifier = Modifier.height(10.dp))

                                    var testInput by rememberSaveable { mutableStateOf("") }
                                    val isMatch = remember(testInput, filterSettings.keywords, filterSettings.mode) {
                                        if (testInput.isBlank()) null
                                        else KeywordMatcher.matchesNotification(
                                            messageText = testInput,
                                            sender = null,
                                            keywords = filterSettings.keywords,
                                            mode = filterSettings.mode,
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                text = "Test Your Filters",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                            if (isMatch != null) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (isMatch) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                                                ) {
                                                    Text(
                                                        text = if (isMatch) "Filtered (Will Skip)" else "Passes (Will Speak)",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isMatch) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    )
                                                }
                                            }
                                        }

                                        OutlinedTextField(
                                            value = testInput,
                                            onValueChange = { testInput = it },
                                            placeholder = {
                                                Text(
                                                    "Type sample message to test filter matching…",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            textStyle = MaterialTheme.typography.bodyMedium,
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent,
                                            ),
                                            trailingIcon = {
                                                if (testInput.isNotEmpty()) {
                                                    IconButton(onClick = { testInput = "" }) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Close,
                                                            contentDescription = "Clear",
                                                            modifier = Modifier.size(16.dp),
                                                        )
                                                    }
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Announcement Priority Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Speech Priority",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                if (!state.priority.isInherited) {
                                    TextButton(onClick = { onPriorityChanged(null) }) {
                                        Text("Revert to Global", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            Text(
                                text = "Higher priority alerts jump ahead in the speech queue.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp),
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
                                    AppPriority.entries.forEach { priority ->
                                        val isSelected = state.priority.effective == priority
                                        val pillBg by animateColorAsState(
                                            targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                            label = "priorityPillBg",
                                        )
                                        val pillText by animateColorAsState(
                                            targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            label = "priorityPillText",
                                        )
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(9.dp))
                                                .background(pillBg)
                                                .clickable { onPriorityChanged(priority) },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = stringResource(priorityLabelRes(priority)),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 12.sp,
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
                    }

                    // Conditions: DND & Call Queue Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Conditions & Phone Calls",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Respect DND
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Respect Do Not Disturb",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = if (state.respectDnd.isInherited) "Inherits global (silence during DND)" else "Custom override active",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp,
                                    )
                                }
                                Switch(
                                    checked = state.respectDnd.effective,
                                    onCheckedChange = { onRespectDndChanged(it) },
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                            // Queue During Calls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Announce After Phone Calls",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = if (state.queueDuringCall.isInherited) "Inherits global (queue during calls)" else "Custom override active",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp,
                                    )
                                }
                                Switch(
                                    checked = state.queueDuringCall.effective,
                                    onCheckedChange = { onQueueDuringCallChanged(it) },
                                )
                            }
                        }
                    }

                    // Audio Focus Policy Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Audio Focus Policy",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Text(
                                text = "How to handle background music playback during speech.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp),
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
                                    val focusOptions = listOf(
                                        AudioFocusPolicy.DUCK to "Duck",
                                        AudioFocusPolicy.PAUSE to "Pause",
                                        AudioFocusPolicy.NONE to "Ignore",
                                    )
                                    focusOptions.forEach { (policy, label) ->
                                        val isSelected = state.audioFocus.effective == policy
                                        val pillBg by animateColorAsState(
                                            targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                            label = "audioFocusPillBg",
                                        )
                                        val pillText by animateColorAsState(
                                            targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            label = "audioFocusPillText",
                                        )
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(9.dp))
                                                .background(pillBg)
                                                .clickable { onAudioFocusChanged(policy) },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 12.sp,
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
                    }

                    // Priority List (#5), Voice/Speed/Pitch Override (#6) & Auto-Dismiss Override (#31)
                    PerAppPriorityVoiceAndDismissSection(
                        state = state,
                        onTogglePriorityAnnounce = onTogglePriorityAnnounce,
                        onSpeechRateChanged = onSpeechRateChanged,
                        onSpeechPitchChanged = onSpeechPitchChanged,
                        onSpeechVoiceChanged = onSpeechVoiceChanged,
                        onAutoDismissChanged = onAutoDismissChanged,
                    )
                } else {
                        // Banner showing announcements are silenced
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    modifier = Modifier.size(44.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.NotificationsOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Announcements are silenced",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Turn on announcements above to configure notification filters, speech priority, conditions, and audio focus policies for ${state.label}.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    // History & Logs Storage + Update Handling + Retention Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "History & Logs Storage",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Text(
                                text = "Choose how notification content from this app is saved locally.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Storage Mode 3-pill segmented selector
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp),
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
                                    val storageOptions = listOf(
                                        HistoryStorageMode.FULL_CONTENT to "Full",
                                        HistoryStorageMode.METADATA_ONLY to "Metadata",
                                        HistoryStorageMode.DISABLED to "None",
                                    )
                                    storageOptions.forEach { (mode, label) ->
                                        val isSelected = state.historyStorageMode.effective == mode
                                        val pillBg by animateColorAsState(
                                            targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                            label = "storagePillBg",
                                        )
                                        val pillText by animateColorAsState(
                                            targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            label = "storagePillText",
                                        )
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(9.dp))
                                                .background(pillBg)
                                                .clickable { onHistoryStorageModeChanged(mode) },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = label,
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

                            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

                            // Update Handling Section
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Update Handling",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                if (!state.historyUpdateHandling.isInherited) {
                                    TextButton(onClick = { onHistoryUpdateHandlingChanged(null) }) {
                                        Text("Revert to Global", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            Text(
                                text = "Choose whether edited or progress notifications overwrite earlier records.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // 2-pill segmented selector
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp),
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
                                    val handlingOptions = listOf(
                                        HistoryUpdateHandling.UPDATE_ENTRY to "Update",
                                        HistoryUpdateHandling.NEW_ENTRY to "Separate",
                                    )
                                    handlingOptions.forEach { (handling, label) ->
                                        val isSelected = state.historyUpdateHandling.effective == handling
                                        val pillBg by animateColorAsState(
                                            targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                            label = "updatePillBg",
                                        )
                                        val pillText by animateColorAsState(
                                            targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            label = "updatePillText",
                                        )
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(9.dp))
                                                .background(pillBg)
                                                .clickable { onHistoryUpdateHandlingChanged(handling) },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = pillText,
                                                maxLines = 1,
                                                softWrap = false,
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

                            // History Retention Section
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "History Retention",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                if (!state.historyRetention.isInherited) {
                                    TextButton(onClick = { onHistoryRetentionChanged(null) }) {
                                        Text("Revert to Global", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            Text(
                                text = "How long notification logs for this app are kept before being deleted.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // 4-pill segmented selector: Never, 7 Days, 30 Days, 90 Days
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp),
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
                                    val retentionOptions = listOf(
                                        null to "Never",
                                        7 to "7 Days",
                                        30 to "30 Days",
                                        90 to "90 Days",
                                    )
                                    val currentEffective = state.historyRetention.effective.let { if (it == 0 || it == -1) null else it }
                                    retentionOptions.forEach { (days, label) ->
                                        val isSelected = (days == null && currentEffective == null) || (days == currentEffective)
                                        val pillBg by animateColorAsState(
                                            targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                            label = "retentionPillBg",
                                        )
                                        val pillText by animateColorAsState(
                                            targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            label = "retentionPillText",
                                        )
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(9.dp))
                                                .background(pillBg)
                                                .clickable { onHistoryRetentionChanged(days ?: 0) },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = label,
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
                    }

                    // Reset to Global Action
                    if (state.hasOverrides) {
                        OutlinedButton(
                            onClick = onResetToGlobal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        ) {
                            Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reset App to Global Defaults", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(130.dp))
                }
            } else {
                // Tab 1: App History
                Column(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "All Notifications (${state.recentHistory.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        if (state.recentHistory.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { showClearHistoryDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear History", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    when {
                        state.historyLoading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(36.dp))
                            }
                        }

                        state.recentHistory.isEmpty() -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Surface(
                                        modifier = Modifier.size(56.dp),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.History,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(28.dp),
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "No notification history yet",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Notifications processed from ${state.label} will appear here in real-time. You can inspect exact messages, listen to them, or filter unwanted alerts.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }

                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                items(items = state.recentHistory, key = { it.id }) { entry ->
                                    AppHistoryItemCard(
                                        entry = entry,
                                        isPlaying = state.playingHistoryId == entry.id,
                                        canFilter = state.enabled,
                                        onPlay = {
                                            if (state.playingHistoryId == entry.id) {
                                                onStopPlaying()
                                            } else {
                                                onPlayHistoryEntry(entry)
                                            }
                                        },
                                        onFilterThis = { filterTargetHistoryItem = entry },
                                        onCardClick = { detailHistoryItem = entry },
                                        onDelete = { pendingDeleteHistoryItem = entry },
                                    )
                                }
                                item { Spacer(modifier = Modifier.height(80.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }
    }

    // Dialog: Full History Notification Details
    detailHistoryItem?.let { entry ->
        HistoryDetailDialog(
            entry = entry,
            isPlaying = state.playingHistoryId == entry.id,
            canFilter = state.enabled,
            onPlay = {
                if (state.playingHistoryId == entry.id) {
                    onStopPlaying()
                } else {
                    onPlayHistoryEntry(entry)
                }
            },
            onFilterThis = {
                detailHistoryItem = null
                filterTargetHistoryItem = entry
            },
            onDelete = {
                detailHistoryItem = null
                pendingDeleteHistoryItem = entry
            },
            onDismiss = { detailHistoryItem = null },
        )
    }

    // Dialog: Delete confirmation
    val deleting = pendingDeleteHistoryItem
    if (deleting != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteHistoryItem = null },
            title = { Text("Delete Notification Record", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this notification record from history?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeleteHistoryItem = null
                        onDeleteHistoryEntry(deleting.id)
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteHistoryItem = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Dialog: Add Custom Filter
    if (showAddFilterDialog) {
        AddCustomFilterDialog(
            appName = state.label,
            onDismiss = { showAddFilterDialog = false },
            onAdd = { keyword, mode, isSender ->
                onAddFilter(keyword, mode, isSender)
                showAddFilterDialog = false
            },
        )
    }

    // Dialog: Filter from History Item
    filterTargetHistoryItem?.let { entry ->
        FilterFromHistoryDialog(
            entry = entry,
            onDismiss = { filterTargetHistoryItem = null },
            onAdd = { keyword, mode, isSender ->
                onAddFilter(keyword, mode, isSender)
                filterTargetHistoryItem = null
            },
        )
    }

    // Dialog: Clear App History
    if (showClearHistoryDialog) {
        ClearAppHistoryDialog(
            appName = state.label,
            onDismiss = { showClearHistoryDialog = false },
            onClear = { olderThanDays ->
                showClearHistoryDialog = false
                onClearAppHistory(olderThanDays)
            },
        )
    }
}

@Composable
private fun ClearAppHistoryDialog(
    appName: String,
    onDismiss: () -> Unit,
    onClear: (olderThanDays: Int?) -> Unit,
) {
    var selectedDays by remember { mutableStateOf<Int?>(null) } // null = all, 1 = 1 day, 7 = 7 days, 30 = 30 days

    val options = listOf(
        null to "All history (everything)",
        1 to "Older than 24 hours (1 day)",
        7 to "Older than 7 days (1 week)",
        30 to "Older than 30 days (1 month)",
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Clear Notification History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Select what notification records to delete for $appName:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup(),
                ) {
                    options.forEach { (days, label) ->
                        val isSelected = selectedDays == days
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .selectable(
                                    selected = isSelected,
                                    role = Role.RadioButton,
                                    onClick = { selectedDays = days },
                                )
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.error,
                                ),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onClear(selectedDays) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("Clear History", fontWeight = FontWeight.Bold)
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
private fun AppHistoryItemCard(
    entry: HistoryEntry,
    isPlaying: Boolean,
    canFilter: Boolean = true,
    onPlay: () -> Unit,
    onFilterThis: () -> Unit,
    onCardClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val relativeTime = remember(entry.recordedAtMillis) {
        DateUtils.getRelativeTimeSpanString(
            entry.recordedAtMillis,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
        ).toString()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: App Name / Sender (full width) + Status Pill
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
                        shape = RoundedCornerShape(10.dp),
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.sender ?: entry.appName,
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
                HistoryStatusPill(status = entry.status)
            }

            // Skip Reason Tag
            val reason = entry.reason
            if (reason != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                ) {
                    Text(
                        text = "Reason: ${skipReasonLabel(reason)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }

            // Message text (Truncated to 2 lines with ...)
            val messageText = entry.messageText
            if (!messageText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = messageText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer Action Row: Filter button / Update badge (left) + Action Buttons (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (entry.isUpdate) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Text(
                                text = "Updated",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                fontSize = 10.sp,
                            )
                        }
                    }

                    if (canFilter) {
                        FilledTonalButton(
                            onClick = onFilterThis,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FilterAlt,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Filter This", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
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

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete entry",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
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
    entry: HistoryEntry,
    isPlaying: Boolean,
    canFilter: Boolean = true,
    onPlay: () -> Unit,
    onFilterThis: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val relativeTime = remember(entry.recordedAtMillis) {
        DateUtils.getRelativeTimeSpanString(
            entry.recordedAtMillis,
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
                        text = entry.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = relativeTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HistoryStatusPill(status = entry.status)
            }
        },
        text = {
            val detailScrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(detailScrollState)
                    .verticalScrollbar(detailScrollState)
                    .padding(end = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val sender = entry.sender
                if (sender != null) {
                    Column {
                        Text(
                            text = "Sender",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = sender,
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
                            text = entry.messageText ?: "(No content stored)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                val reason = entry.reason
                if (reason != null) {
                    Text(
                        text = "Skip Reason: ${skipReasonLabel(reason)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Actions: Play and Filter This
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onPlay,
                        modifier = if (canFilter) Modifier.weight(1f) else Modifier.fillMaxWidth(),
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

                    if (canFilter) {
                        Button(
                            onClick = onFilterThis,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FilterAlt,
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
        shape = RoundedCornerShape(8.dp),
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
private fun FilterFromHistoryDialog(
    entry: HistoryEntry,
    onDismiss: () -> Unit,
    onAdd: (keyword: String, mode: KeywordMatchMode, isSender: Boolean) -> Unit,
) {
    var keywordText by remember { mutableStateOf(entry.messageText ?: entry.sender ?: "") }
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
                text = "Filter Alert from ${entry.appName}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Block notifications matching this text or sender from being announced.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Presets
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
                    val sender = entry.sender
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

                    val message = entry.messageText
                    if (!message.isNullOrBlank()) {
                        FilterChip(
                            selected = keywordText == message && selectedFilterKind == "Exact",
                            onClick = {
                                keywordText = message
                                selectedFilterKind = "Exact"
                            },
                            label = { Text("Exact Sentence", style = MaterialTheme.typography.labelSmall, maxLines = 1) },
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

                OutlinedTextField(
                    value = keywordText,
                    onValueChange = { keywordText = it },
                    label = { Text(if (isSender) "Sender Name to Block" else "Filter Word or Pattern ($1 or *)") },
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

                // Perfectly aligned capsule segmented buttons
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
                                        if (kind == "Sender" && !entry.sender.isNullOrBlank()) {
                                            keywordText = entry.sender ?: ""
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
                        onAdd(keywordText.trim(), selectedMode, isSender)
                    }
                },
                enabled = keywordText.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("Add Filter Rule", fontWeight = FontWeight.Bold)
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
private fun AddCustomFilterDialog(
    appName: String,
    onDismiss: () -> Unit,
    onAdd: (keyword: String, mode: KeywordMatchMode, isSender: Boolean) -> Unit,
) {
    var keywordText by remember { mutableStateOf("") }
    var selectedFilterKind by remember { mutableStateOf("Word") } // "Word", "Phrase", "Exact", "Sender"

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
                text = "Add Filter for $appName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Enter a keyword, phrase, wildcard pattern, or sender to block alerts from $appName.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = keywordText,
                    onValueChange = { keywordText = it },
                    label = { Text(if (isSender) "Sender Name to Block" else "Word, Sentence, or Pattern") },
                    placeholder = {
                        Text(if (isSender) "e.g. John, Swiggy, Bank Alerts" else "e.g. OTP, order delivered, or Balance: Rs *")
                    },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )

                if (!isSender) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        FilterChip(
                            selected = false,
                            onClick = { keywordText = (keywordText.trim() + " *").trim() },
                            label = { Text("Add Wildcard (*)", style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(8.dp),
                        )
                        FilterChip(
                            selected = false,
                            onClick = { keywordText = (keywordText.trim() + " $1").trim() },
                            label = { Text("Add Code ($1)", style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(8.dp),
                        )
                    }
                }

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

                // Perfectly aligned capsule segmented buttons
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
                                label = "addFilterKindPillBg",
                            )
                            val pillText by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                label = "addFilterKindPillText",
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(pillBg)
                                    .clickable { selectedFilterKind = kind },
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
                        onAdd(keywordText.trim(), selectedMode, isSender)
                    }
                },
                enabled = keywordText.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("Add Filter Rule", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

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
private fun audioFocusLabelRes(policy: AudioFocusPolicy): Int = when (policy) {
    AudioFocusPolicy.DUCK -> R.string.app_settings_audio_focus_duck
    AudioFocusPolicy.PAUSE -> R.string.app_settings_audio_focus_pause
    AudioFocusPolicy.NONE -> R.string.app_settings_audio_focus_none
}

@Composable
fun FilterTypeInfoRow(type: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.primary,
        ) {
            Text(
                text = type.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}
