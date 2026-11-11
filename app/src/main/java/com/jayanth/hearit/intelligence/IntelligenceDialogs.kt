package com.jayanth.hearit.intelligence

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jayanth.hearit.applications.AppEntry
import com.jayanth.hearit.core.common.model.AppCategory
import com.jayanth.hearit.core.common.model.PronunciationRule
import com.jayanth.hearit.core.common.model.UrlSummaryRule

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Pronunciation Dictionary",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Also apply to sender names",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = applyOnSender, onCheckedChange = onToggleApplyOnSender)
                }

                // Rule entry / edit form
                OutlinedTextField(
                    value = matchText,
                    onValueChange = { matchText = it },
                    label = { Text("Word or Acronym (e.g. ETA)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = spokenText,
                    onValueChange = { spokenText = it },
                    label = { Text("Speak As (e.g. E T A)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
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
                    Spacer(modifier = Modifier.weight(1f))
                    OutlinedButton(
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Rules (${rules.size})",
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
                        .heightIn(max = 170.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (rules.isEmpty()) {
                        item {
                            Text(
                                text = "No custom rules yet. Tap 'Load Presets' or add your own.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        items(rules, key = { it.match.lowercase() }) { rule ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${rule.match} → ${rule.spokenAs}",
                                            style = MaterialTheme.typography.bodySmall,
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
                                    )
                                    IconButton(
                                        onClick = {
                                            editingOriginalMatch = rule.match
                                            matchText = rule.match
                                            spokenText = rule.spokenAs
                                            matchCase = rule.matchCase
                                            wholeWord = rule.wholeWord
                                        },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(Icons.Rounded.Edit, contentDescription = "Edit rule", modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { onDeleteRule(rule.match) },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete rule", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()
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
                    OutlinedButton(onClick = { onTestPronunciation(sampleTestText) }) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Test")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
    )
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Smart URL Summary Rules",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text("Domain / Path (e.g. notion.so)") },
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
                    OutlinedButton(
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

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(rules, key = { it.pattern.lowercase() }) { rule ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = rule.pattern,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${rule.spokenPhrase} · ${if (rule.isPredefined) "Preset" else "Custom"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                    )
                                }
                                Switch(
                                    checked = rule.enabled,
                                    onCheckedChange = { onToggleRule(rule.pattern, it) },
                                )
                                IconButton(
                                    onClick = {
                                        editingOriginalPattern = rule.pattern
                                        pattern = rule.pattern
                                        spokenPhrase = rule.spokenPhrase
                                    },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Edit URL rule", modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { onDeleteRule(rule.pattern) },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete URL rule", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
    )
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Carrier & Promo SMS Phrases",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = newPhrase,
                        onValueChange = { newPhrase = it },
                        label = { Text("Promo phrase (e.g. recharge now)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(
                        onClick = {
                            if (newPhrase.isNotBlank()) {
                                onAddPhrase(newPhrase.trim())
                                newPhrase = ""
                            }
                        },
                        enabled = newPhrase.isNotBlank(),
                    ) {
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
                        style = MaterialTheme.typography.labelMedium,
                    )
                    TextButton(onClick = onRestoreDefaults) {
                        Text("Load Defaults")
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(phrases, key = { it.lowercase() }) { phrase ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = phrase,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(
                                    onClick = { onRemovePhrase(phrase) },
                                    modifier = Modifier.size(30.dp),
                                ) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove phrase", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
    )
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "App Categories Grouping",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Assign apps to Email, Messaging, or SMS groups so category rules apply accurately.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AppCategory.entries.forEach { category ->
                        val count = appCategories.count { it.value == category }
                        val title = when (category) {
                            AppCategory.EMAIL -> "Email ($count)"
                            AppCategory.MESSAGING -> "Messaging ($count)"
                            AppCategory.SMS -> "SMS ($count)"
                        }
                        FilterChip(
                            selected = selectedTab == category,
                            onClick = { selectedTab = category },
                            label = { Text(title, maxLines = 1) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    label = { Text("Search apps...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(visibleApps, key = { it.packageName }) { entry ->
                        val currentCategory = appCategories[entry.packageName]
                        val inCurrentTab = currentCategory == selectedTab
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (inCurrentTab) {
                                        onSetAppCategory(entry.packageName, null)
                                    } else {
                                        onSetAppCategory(entry.packageName, selectedTab)
                                    }
                                }
                                .padding(horizontal = 4.dp, vertical = 4.dp),
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
                                    text = currentCategory?.let { "Assigned: ${it.name.lowercase().replaceFirstChar(Char::uppercase)}" }
                                        ?: entry.packageName,
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
                Spacer(modifier = Modifier.height(2.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
    )
}
