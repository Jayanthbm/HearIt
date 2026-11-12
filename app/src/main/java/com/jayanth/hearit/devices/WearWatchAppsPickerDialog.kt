package com.jayanth.hearit.devices

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jayanth.hearit.wear.WearConfigHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class WatchPickerAppItem(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?,
)

/**
 * Searchable app picker capped at 4 apps for Wear OS notification mirroring (#35).
 */
@Composable
fun WearWatchAppsPickerDialog(
    initialSelected: List<String>,
    watchAppInstalled: Boolean,
    onSave: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var selected by remember(initialSelected) {
        mutableStateOf(WearConfigHelper.normalizeWatchApps(initialSelected))
    }
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var catalog by remember { mutableStateOf<List<WatchPickerAppItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        catalog = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolved = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            resolved
                .mapNotNull { info ->
                    val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                    if (pkg == context.packageName) return@mapNotNull null
                    val label = info.loadLabel(pm).toString().ifBlank { pkg }
                    val bmp = runCatching {
                        val d = info.loadIcon(pm)
                        if (d is BitmapDrawable) {
                            d.bitmap.asImageBitmap()
                        } else {
                            val b = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
                            val c = Canvas(b)
                            d.setBounds(0, 0, c.width, c.height)
                            d.draw(c)
                            b.asImageBitmap()
                        }
                    }.getOrNull()
                    WatchPickerAppItem(pkg, label, bmp)
                }
                .distinctBy { it.packageName }
                .sortedWith(
                    compareByDescending<WatchPickerAppItem> { it.packageName in initialSelected }
                        .thenBy { it.label.lowercase() },
                )
        }
        loading = false
    }

    val filtered = remember(catalog, query, selected) {
        val q = query.trim().lowercase()
        val matches = if (q.isEmpty()) {
            catalog
        } else {
            catalog.filter { it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
        }
        // Selected apps are always listed first.
        matches.sortedWith(
            compareByDescending<WatchPickerAppItem> { it.packageName in selected }
                .thenBy { it.label.lowercase() },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Apps (${selected.size}/${WearConfigHelper.MAX_WATCH_APPS})",
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Pick up to 4 apps to mirror to your watch.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search apps…") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                if (loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(filtered, key = { it.packageName }) { item ->
                            val isChecked = item.packageName in selected
                            val canEnableMore = isChecked || selected.size < WearConfigHelper.MAX_WATCH_APPS
                            Surface(
                                onClick = {
                                    selected = WearConfigHelper.toggleWatchAppSelection(selected, item.packageName)
                                },
                                enabled = canEnableMore,
                                shape = RoundedCornerShape(12.dp),
                                color = if (isChecked) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainer
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (item.icon != null) {
                                        Image(
                                            bitmap = item.icon,
                                            contentDescription = item.label,
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                        )
                                    } else {
                                        Icon(
                                            Icons.Rounded.Apps,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = item.packageName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    Checkbox(
                                        checked = isChecked,
                                        enabled = canEnableMore,
                                        onCheckedChange = {
                                            selected = WearConfigHelper.toggleWatchAppSelection(selected, item.packageName)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(selected) }) {
                Text(if (watchAppInstalled) "Save & Sync" else "Save")
            }
        },
    )
}
