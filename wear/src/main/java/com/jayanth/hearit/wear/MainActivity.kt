package com.jayanth.hearit.wear

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.wear.compose.material.AppCard
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.items
import androidx.wear.compose.material.rememberScalingLazyListState
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                WearApp()
            }
        }
    }
}

@Composable
fun WearApp() {
    val context = LocalContext.current
    val lifecycleOwner = context as? LifecycleOwner
    val listState = rememberScalingLazyListState()
    val allNotifications by WearNotificationStore.notifications.collectAsState()
    val watchApps by WearNotificationStore.watchApps.collectAsState()
    val forwardingEnabled by WearNotificationStore.forwardingEnabled.collectAsState()

    var isPhoneConnected by remember { mutableStateOf(false) }
    var connectedNodeName by remember { mutableStateOf<String?>(null) }
    var selectedPackageFilter by remember { mutableStateOf<String?>(null) } // null == ALL

    suspend fun refreshPhoneAndConfig() {
        withContext(Dispatchers.IO) {
            try {
                val nodeClient = Wearable.getNodeClient(context)
                val nodes = Tasks.await(nodeClient.connectedNodes)
                isPhoneConnected = !nodes.isNullOrEmpty()
                connectedNodeName = nodes.firstOrNull()?.displayName
                nodes?.forEach { node ->
                    Wearable.getMessageClient(context).sendMessage(node.id, "/request/sync", byteArrayOf())
                }
            } catch (_: Exception) {
                isPhoneConnected = false
            }

            // Also read any already-persisted DataItem config so icons show even when phone is asleep
            try {
                val dataItems = Tasks.await(Wearable.getDataClient(context).dataItems)
                dataItems.forEach { item ->
                    if (item.uri.path == "/config/sync") {
                        val map = DataMapItem.fromDataItem(item).dataMap
                        val enabled = map.getBoolean("forwardingEnabled", true)
                        val haptics = map.getBoolean("hapticsEnabled", false)
                        val pkgs = map.getStringArrayList("watchApps").orEmpty()
                        val labels = map.getStringArrayList("watchAppLabels").orEmpty()
                        val loaded = pkgs.take(4).mapIndexed { idx, pkg ->
                            val label = labels.getOrNull(idx)?.takeIf { it.isNotBlank() }
                                ?: pkg.substringAfterLast('.')
                            val asset: Asset? = map.getAsset("icon_$idx")
                            val bytes = asset?.let {
                                runCatching {
                                    val fd = Tasks.await(Wearable.getDataClient(context).getFdForAsset(it))
                                    fd.inputStream.use { stream -> stream.readBytes() }
                                }.getOrNull()
                            }
                            WatchAppFilterItem(packageName = pkg, appName = label, iconPngBytes = bytes)
                        }
                        WearNotificationStore.applyConfigSync(loaded, enabled, haptics)
                    }
                }
                dataItems.release()
            } catch (_: Exception) {
            }
        }
    }

    var syncTrigger by remember { mutableStateOf(0) }
    LaunchedEffect(syncTrigger) {
        refreshPhoneAndConfig()
    }

    DisposableEffect(lifecycleOwner) {
        if (lifecycleOwner == null) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    syncTrigger++
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
    }

    // Clear selected filter if that app was removed from synced max-4 list
    val activeApps = watchApps.orEmpty()
    LaunchedEffect(activeApps) {
        if (selectedPackageFilter != null && activeApps.none { it.packageName == selectedPackageFilter }) {
            selectedPackageFilter = null
        }
    }

    val filteredNotifications = remember(allNotifications, activeApps, selectedPackageFilter, forwardingEnabled) {
        if (!forwardingEnabled || activeApps.isEmpty()) {
            emptyList()
        } else {
            val allowed = activeApps.map { it.packageName }.toSet()
            val byAllowed = allNotifications.filter { it.packageName in allowed }
            val pkg = selectedPackageFilter
            if (pkg == null) byAllowed else byAllowed.filter { it.packageName == pkg }
        }
    }

    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top Header with Phone Connection Green/Offline Dot (#37)
            item(key = "header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp)
                        .clickable { syncTrigger++ },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isPhoneConnected) Color(0xFF4CAF50)
                                    else Color(0xFFEF5350),
                                ),
                        )
                        Text(
                            text = "HearIt",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64B5F6),
                        )
                    }
                    Text(
                        text = if (isPhoneConnected) {
                            connectedNodeName?.let { "Phone connected · $it" } ?: "Phone connected"
                        } else {
                            "Phone offline · Tap to refresh"
                        },
                        fontSize = 10.sp,
                        color = if (isPhoneConnected) Color(0xFF81C784) else Color(0xFFE57373),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Dynamic Real App Filter Bar: ALL + up to 4 real app icons (#37 — NO categories)
            if (forwardingEnabled && activeApps.isNotEmpty()) {
                item(key = "app_filter_bar") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // ALL Filter Button
                        val isAllSelected = selectedPackageFilter == null
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isAllSelected) Color(0xFF64B5F6) else Color(0xFF262626))
                                .border(
                                    width = if (isAllSelected) 1.5.dp else 1.dp,
                                    color = if (isAllSelected) Color.White else Color(0xFF3E3E3E),
                                    shape = CircleShape,
                                )
                                .clickable { selectedPackageFilter = null },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "ALL",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isAllSelected) Color.Black else Color.White,
                            )
                        }

                        // Up to 4 real app icons
                        activeApps.take(4).forEach { appItem ->
                            val isSelected = selectedPackageFilter == appItem.packageName
                            val decodedBitmap = remember(appItem.packageName, appItem.iconPngBytes) {
                                decodeAppIcon(appItem.iconPngBytes)
                            }
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color(0xFF64B5F6) else Color(0xFF222222))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF64B5F6) else Color(0xFF3E3E3E),
                                        shape = CircleShape,
                                    )
                                    .clickable { selectedPackageFilter = appItem.packageName },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (decodedBitmap != null) {
                                    Image(
                                        bitmap = decodedBitmap,
                                        contentDescription = appItem.appName,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape),
                                    )
                                } else {
                                    Text(
                                        text = appItem.appName.take(2).uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.Black else Color.White,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Empty State when no watch apps are enabled or forwarding disabled (#37)
            if (!forwardingEnabled || activeApps.isEmpty()) {
                item(key = "no_apps_enabled") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Apps,
                            contentDescription = null,
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            text = "No apps enabled",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = if (!forwardingEnabled) {
                                "Watch forwarding is paused in HearIt on your phone."
                            } else {
                                "Select up to 4 apps in HearIt on your phone to mirror notifications here."
                            },
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else if (filteredNotifications.isEmpty()) {
                item(key = "no_notifs") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsNone,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(22.dp),
                        )
                        val activeLabel = selectedPackageFilter?.let { pkg ->
                            activeApps.firstOrNull { it.packageName == pkg }?.appName
                        }
                        Text(
                            text = activeLabel?.let { "No alerts from $it" } ?: "All caught up",
                            color = Color.LightGray,
                            fontSize = 11.5.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                items(filteredNotifications, key = { it.id }) { notif ->
                    NotificationCard(
                        notification = notif,
                        onDismiss = { WearNotificationStore.removeNotification(notif.id) },
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                item(key = "clear_all") {
                    CompactChip(
                        onClick = { WearNotificationStore.clearAll() },
                        label = {
                            Text(
                                text = "Clear All",
                                fontSize = 11.sp,
                                color = Color(0xFFFF8A80),
                            )
                        },
                        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                    )
                }
            }
        }
    }
}

private fun decodeAppIcon(bytes: ByteArray?): ImageBitmap? {
    if (bytes == null || bytes.isEmpty()) return null
    return runCatching {
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }.getOrNull()
}

@Composable
fun NotificationCard(
    notification: WearNotification,
    onDismiss: () -> Unit,
) {
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val formattedTime = remember(notification.timestamp) {
        timeFormatter.format(Date(notification.timestamp))
    }

    AppCard(
        onClick = onDismiss,
        appName = {
            Text(
                text = notification.appName,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF81C784),
            )
        },
        time = {
            Text(
                text = formattedTime,
                fontSize = 10.sp,
                color = Color.LightGray,
            )
        },
        title = {
            if (notification.sender.isNotBlank()) {
                Text(
                    text = notification.sender,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
    ) {
        Text(
            text = notification.message,
            fontSize = 12.sp,
            color = Color(0xFFE0E0E0),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
