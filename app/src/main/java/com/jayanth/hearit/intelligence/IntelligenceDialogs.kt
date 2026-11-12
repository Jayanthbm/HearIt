package com.jayanth.hearit.intelligence

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jayanth.hearit.applications.AppEntry
import com.jayanth.hearit.core.common.model.AppCategory
import com.jayanth.hearit.core.common.model.PronunciationRule
import com.jayanth.hearit.core.common.model.UrlSummaryRule

@Composable
private fun IntelligenceConfigModal(
    title: String,
    subtitle: String? = null,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 700.dp)
                .imePadding(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    content()
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PronunciationDictionaryDialog(
    rules: List<PronunciationRule>,
    applyOnSender: Boolean,
    onToggleApplyOnSender: (Boolean) -> Unit,
    onSaveRule: (oldMatch: String?, rule: PronunciationRule) -> Unit,
    onToggleRule: (match: String, enabled: Boolean) -> Unit,
    onDeleteRule: (match: String) -> Unit,
    onLoadPresets: () -> Unit,
    onTestPronunciation: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var editingOriginalMatch by remember { mutableStateOf<String?>(null) }
    var matchText by remember { mutableStateOf("") }
    var spokenText by remember { mutableStateOf("") }
    var matchCase by remember { mutableStateOf(false) }
    var wholeWord by remember { mutableStateOf(true) }
    var sampleTestText by remember { mutableStateOf("OTP 482910 ETA 15 mins from Dr. Sharma") }

    IntelligenceConfigModal(
        title = "Pronunciation Dictionary",
        subtitle = "Customize how acronyms, brands, and shorthand sound when spoken",
        onDismiss = onDismiss,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Also apply to sender names",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = applyOnSender, onCheckedChange = onToggleApplyOnSender)
            }
        }

        // Structured Add / Edit Rule Form
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = matchText,
                        onValueChange = { matchText = it },
                        label = { Text("Word") },
                        placeholder = { Text("e.g. ETA") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = spokenText,
                        onValueChange = { spokenText = it },
                        label = { Text("Speak as") },
                        placeholder = { Text("e.g. E T A") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(
                        selected = wholeWord,
                        onClick = { wholeWord = !wholeWord },
                        label = { Text("Whole word") },
                    )
                    FilterChip(
                        selected = matchCase,
                        onClick = { matchCase = !matchCase },
                        label = { Text("Match case") },
                    )
                    if (editingOriginalMatch != null) {
                        TextButton(
                            onClick = {
                                editingOriginalMatch = null
                                matchText = ""
                                spokenText = ""
                            },
                        ) {
                            Text("Cancel Edit")
                        }
                    }
                    FilledTonalButton(
                        onClick = {
                            if (matchText.isNotBlank() && spokenText.isNotBlank()) {
                                onSaveRule(
                                    editingOriginalMatch,
                                    PronunciationRule(
                                        match = matchText.trim(),
                                        spokenAs = spokenText.trim(),
                                        matchCase = matchCase,
                                        wholeWord = wholeWord,
                                    ),
                                )
                                editingOriginalMatch = null
                                matchText = ""
                                spokenText = ""
                            }
                        },
                        enabled = matchText.isNotBlank() && spokenText.isNotBlank(),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (editingOriginalMatch != null) "Update" else "Add")
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Active Rules (${rules.size})",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(onClick = onLoadPresets) {
                Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Load Presets")
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .heightIn(min = 100.dp, max = 180.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (rules.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Text(
                            text = "No custom rules yet. Tap 'Load Presets' or add your own above.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                }
            } else {
                items(rules, key = { it.match.lowercase() }) { rule ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${rule.match} → ${rule.spokenAs}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = buildString {
                                        append(if (rule.wholeWord) "Whole word" else "Partial")
                                        append(if (rule.matchCase) " · Case-sensitive" else " · Ignore case")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = rule.enabled,
                                onCheckedChange = { onToggleRule(rule.match, it) },
                                modifier = Modifier.scale(0.85f),
                            )
                            IconButton(
                                onClick = {
                                    editingOriginalMatch = rule.match
                                    matchText = rule.match
                                    spokenText = rule.spokenAs
                                    matchCase = rule.matchCase
                                    wholeWord = rule.wholeWord
                                },
                                modifier = Modifier.size(34.dp),
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Edit rule", modifier = Modifier.size(17.dp))
                            }
                            IconButton(
                                onClick = { onDeleteRule(rule.match) },
                                modifier = Modifier.size(34.dp),
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete rule", modifier = Modifier.size(17.dp))
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = sampleTestText,
                onValueChange = { sampleTestText = it },
                label = { Text("Quick Test Sample") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            FilledTonalButton(onClick = { onTestPronunciation(sampleTestText) }) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Test")
            }
        }
    }
}

@Composable
fun UrlSummaryRulesDialog(
    rules: List<UrlSummaryRule>,
    onSaveRule: (oldPattern: String?, rule: UrlSummaryRule) -> Unit,
    onToggleRule: (pattern: String, enabled: Boolean) -> Unit,
    onDeleteRule: (pattern: String) -> Unit,
    onLoadPredefined: () -> Unit,
    onDismiss: () -> Unit,
) {
    var editingOriginalPattern by remember { mutableStateOf<String?>(null) }
    var pattern by remember { mutableStateOf("") }
    var spokenPhrase by remember { mutableStateOf("") }

    IntelligenceConfigModal(
        title = "Smart URL Summary Rules",
        subtitle = "Replace verbose pasted web links with friendly spoken descriptions",
        onDismiss = onDismiss,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text("Domain or Path (e.g. notion.so)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = spokenPhrase,
                    onValueChange = { spokenPhrase = it },
                    label = { Text("Spoken Phrase (e.g. Notion page)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onLoadPredefined) {
                        Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Load Predefined")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (editingOriginalPattern != null) {
                            TextButton(
                                onClick = {
                                    editingOriginalPattern = null
                                    pattern = ""
                                    spokenPhrase = ""
                                },
                            ) {
                                Text("Cancel")
                            }
                        }
                        FilledTonalButton(
                            onClick = {
                                if (pattern.isNotBlank() && spokenPhrase.isNotBlank()) {
                                    onSaveRule(
                                        editingOriginalPattern,
                                        UrlSummaryRule(
                                            pattern = pattern.trim(),
                                            spokenPhrase = spokenPhrase.trim(),
                                            isPredefined = false,
                                            enabled = true,
                                        ),
                                    )
                                    editingOriginalPattern = null
                                    pattern = ""
                                    spokenPhrase = ""
                                }
                            },
                            enabled = pattern.isNotBlank() && spokenPhrase.isNotBlank(),
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (editingOriginalPattern != null) "Update" else "Add Rule")
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .heightIn(min = 120.dp, max = 260.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (rules.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Text(
                            text = "No URL summary rules configured. Tap 'Load Predefined' or add a mapping above.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                }
            } else {
                items(rules, key = { it.pattern.lowercase() }) { rule ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = rule.pattern,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${rule.spokenPhrase} · ${if (rule.isPredefined) "Preset" else "Custom"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Switch(
                                checked = rule.enabled,
                                onCheckedChange = { onToggleRule(rule.pattern, it) },
                                modifier = Modifier.scale(0.85f),
                            )
                            IconButton(
                                onClick = {
                                    editingOriginalPattern = rule.pattern
                                    pattern = rule.pattern
                                    spokenPhrase = rule.spokenPhrase
                                },
                                modifier = Modifier.size(34.dp),
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Edit URL rule", modifier = Modifier.size(17.dp))
                            }
                            IconButton(
                                onClick = { onDeleteRule(rule.pattern) },
                                modifier = Modifier.size(34.dp),
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete URL rule", modifier = Modifier.size(17.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CarrierSpamPhrasesDialog(
    phrases: List<String>,
    onAddPhrase: (String) -> Unit,
    onRemovePhrase: (String) -> Unit,
    onRestoreDefaults: () -> Unit,
    onDismiss: () -> Unit,
) {
    var newPhrase by remember { mutableStateOf("") }

    IntelligenceConfigModal(
        title = "Carrier & Promo SMS Phrases",
        subtitle = "Skip annoying carrier upsells, loan promos, and marketing SMS blasts",
        onDismiss = onDismiss,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = newPhrase,
                onValueChange = { newPhrase = it },
                label = { Text("Promo phrase") },
                placeholder = { Text("e.g. recharge now") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            FilledTonalButton(
                onClick = {
                    if (newPhrase.isNotBlank()) {
                        onAddPhrase(newPhrase.trim())
                        newPhrase = ""
                    }
                },
                enabled = newPhrase.isNotBlank(),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${phrases.size} blocked phrases",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(onClick = onRestoreDefaults) {
                Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Load Defaults")
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .heightIn(min = 120.dp, max = 260.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (phrases.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Text(
                            text = "No blocked promo phrases. Add phrases above or tap 'Load Defaults'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                }
            } else {
                items(phrases, key = { it.lowercase() }) { phrase ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = phrase,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = { onRemovePhrase(phrase) },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove phrase", modifier = Modifier.size(17.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppCategoriesDialog(
    apps: List<AppEntry>,
    appCategories: Map<String, AppCategory?>,
    onSetAppCategory: (packageName: String, category: AppCategory?) -> Unit,
    onResetAppCategory: (packageName: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(AppCategory.EMAIL) }
    var query by remember { mutableStateOf("") }

    val visibleApps = remember(apps, appCategories, selectedTab, query) {
        val q = query.trim().lowercase()
        apps
            .filter { !it.isSystemApp || appCategories[it.packageName] != null }
            .filter { entry ->
                q.isEmpty() || entry.label.lowercase().contains(q) || entry.packageName.lowercase().contains(q)
            }
            .sortedWith(
                compareByDescending<AppEntry> { appCategories[it.packageName] == selectedTab }
                    .thenBy { it.label.lowercase() },
            )
    }

    IntelligenceConfigModal(
        title = "App categories",
        subtitle = "Group enabled apps for Email, Chat, or SMS rules",
        onDismiss = onDismiss,
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val categories = AppCategory.entries
            categories.forEachIndexed { index, category ->
                val label = when (category) {
                    AppCategory.EMAIL -> "Email"
                    AppCategory.MESSAGING -> "Chat"
                    AppCategory.SMS -> "SMS"
                }
                val isSelected = selectedTab == category
                SegmentedButton(
                    selected = isSelected,
                    onClick = { selectedTab = category },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = categories.size),
                    icon = {},
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            label = { Text("Search installed apps...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp, max = 300.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(visibleApps, key = { it.packageName }) { entry ->
                val currentCategory = appCategories[entry.packageName]
                val inCurrentTab = currentCategory == selectedTab
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (inCurrentTab) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSetAppCategory(entry.packageName, if (inCurrentTab) null else selectedTab)
                        },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = inCurrentTab,
                            onCheckedChange = { checked ->
                                onSetAppCategory(entry.packageName, if (checked) selectedTab else null)
                            },
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (inCurrentTab) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = currentCategory?.let {
                                    "Assigned: ${it.name.lowercase().replaceFirstChar(Char::uppercase)}"
                                } ?: entry.packageName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (currentCategory != null && currentCategory != selectedTab) {
                            TextButton(onClick = { onResetAppCategory(entry.packageName) }) {
                                Text("Reset", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
