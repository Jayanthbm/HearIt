package com.jayanth.hearit.wear

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Headphones
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
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.AppCard
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WearNotificationStore.init(applicationContext)
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
    val isMultipointBusy by WearNotificationStore.isMultipointBusy.collectAsState()
    val wearVersionName = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0.2"
    }

    var isPhoneConnected by remember { mutableStateOf(false) }
    var selectedPackageFilter by remember { mutableStateOf<String?>(null) } // null == ALL
    var selectedNotification by remember { mutableStateOf<WearNotification?>(null) }

    suspend fun refreshPhoneAndConfig() {
        withContext(Dispatchers.IO) {
            try {
                val nodeClient = Wearable.getNodeClient(context)
                val nodes = Tasks.await(nodeClient.connectedNodes)
                isPhoneConnected = !nodes.isNullOrEmpty()
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

    if (selectedNotification != null) {
        val currentNotification = selectedNotification!!
        val appItem = activeApps.firstOrNull { it.packageName == currentNotification.packageName }
        val iconBitmap = remember(currentNotification.packageName, appItem?.iconPngBytes) {
            decodeAppIcon(appItem?.iconPngBytes)
        }
        NotificationDetailScreen(
            notification = currentNotification,
            iconBitmap = iconBitmap,
            onDismiss = {
                dismissWatchNotification(context, currentNotification)
                selectedNotification = null
            },
            onClose = {
                selectedNotification = null
            },
        )
        return
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
            contentPadding = PaddingValues(top = 54.dp, bottom = 36.dp, start = 8.dp, end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            autoCentering = null,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top Header: Compact Phone Connection status indicator and subtitle
            item(key = "header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 6.dp)
                        .clickable { syncTrigger++ },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                if (isPhoneConnected) Color(0xFF4CAF50)
                                else Color(0xFFEF5350),
                            ),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPhoneConnected) "Phone connected" else "Phone offline · Tap to refresh",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isPhoneConnected) Color(0xFF81C784) else Color(0xFFE57373),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (isMultipointBusy) {
                item(key = "multipoint_busy_chip") {
                    CompactChip(
                        onClick = { toggleMultipointBusy(context) },
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.Headphones,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = Color(0xFFFFB74D),
                            )
                        },
                        label = {
                            Text(
                                text = "Busy hold · Tap to resume",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFFB74D),
                                maxLines = 1,
                            )
                        },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = Color(0xFF332011),
                        ),
                        modifier = Modifier.padding(bottom = 2.dp),
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
                        onClick = { selectedNotification = notif },
                    )
                }

                item(key = "clear_all") {
                    CompactChip(
                        onClick = { clearAllWatchNotifications(context) },
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Clear All",
                                modifier = Modifier.size(15.dp),
                                tint = Color(0xFFFF8A80),
                            )
                        },
                        label = {
                            Text(
                                text = "Clear All",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFF8A80),
                            )
                        },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = Color(0xFF3D1F1F),
                            contentColor = Color(0xFFFF8A80),
                        ),
                        modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
                    )
                }
            }

            item(key = "app_version") {
                Text(
                    text = "HearIt v$wearVersionName",
                    fontSize = 10.sp,
                    color = Color(0xFF757575),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp),
                )
            }
        }
    }
}

private fun dismissWatchNotification(context: android.content.Context, notification: WearNotification) {
    WearNotificationStore.removeNotification(notification.id)
    CoroutineScope(Dispatchers.IO).launch {
        runCatching {
            val nodeClient = Wearable.getNodeClient(context)
            val messageClient = Wearable.getMessageClient(context)
            val nodes = Tasks.await(nodeClient.connectedNodes)
            if (!nodes.isNullOrEmpty()) {
                val payload = JSONObject().apply {
                    put("id", notification.id)
                    put("historyId", notification.historyId)
                }.toString().toByteArray(Charsets.UTF_8)
                for (node in nodes) {
                    messageClient.sendMessage(node.id, "/history/delete", payload)
                }
            }
        }
    }
}

private fun clearAllWatchNotifications(context: android.content.Context) {
    WearNotificationStore.clearAll()
    CoroutineScope(Dispatchers.IO).launch {
        runCatching {
            val nodeClient = Wearable.getNodeClient(context)
            val messageClient = Wearable.getMessageClient(context)
            val nodes = Tasks.await(nodeClient.connectedNodes)
            if (!nodes.isNullOrEmpty()) {
                val payload = ByteArray(0)
                for (node in nodes) {
                    messageClient.sendMessage(node.id, "/history/clear_all", payload)
                }
            }
        }
    }
}

private fun toggleMultipointBusy(context: android.content.Context) {
    CoroutineScope(Dispatchers.IO).launch {
        runCatching {
            val nodeClient = Wearable.getNodeClient(context)
            val messageClient = Wearable.getMessageClient(context)
            val nodes = Tasks.await(nodeClient.connectedNodes)
            if (!nodes.isNullOrEmpty()) {
                val payload = ByteArray(0)
                for (node in nodes) {
                    messageClient.sendMessage(node.id, "/multipoint/toggle", payload)
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
    onClick: () -> Unit,
) {
    val formattedTime = remember(notification.timestamp) {
        val calNow = Calendar.getInstance()
        val calItem = Calendar.getInstance().apply { timeInMillis = notification.timestamp }
        val isSameYear = calNow.get(Calendar.YEAR) == calItem.get(Calendar.YEAR)
        val pattern = if (isSameYear) "d MMM, h:mm a" else "d MMM yyyy, h:mm a"
        SimpleDateFormat(pattern, Locale.getDefault()).format(Date(notification.timestamp))
    }

    AppCard(
        onClick = onClick,
        appName = {
            Text(
                text = notification.appName,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF81C784),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        time = {
            Text(
                text = formattedTime,
                fontSize = 9.5.sp,
                color = Color.LightGray,
                maxLines = 1,
            )
        },
        title = {
            if (notification.sender.isNotBlank()) {
                Text(
                    text = notification.sender,
                    fontSize = 12.5.sp,
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
            fontSize = 11.5.sp,
            lineHeight = 15.sp,
            color = Color(0xFFE0E0E0),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun NotificationDetailScreen(
    notification: WearNotification,
    iconBitmap: ImageBitmap?,
    onDismiss: () -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)

    val detailListState = rememberScalingLazyListState()
    val fullDateTime = remember(notification.timestamp) {
        SimpleDateFormat("d MMM yyyy · h:mm a", Locale.getDefault()).format(Date(notification.timestamp))
    }

    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = detailListState) },
    ) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            state = detailListState,
            contentPadding = PaddingValues(top = 54.dp, bottom = 44.dp, start = 12.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            autoCentering = null,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // App Header Pill (Icon + App Name)
            item(key = "detail_app_header") {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF222222))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (iconBitmap != null) {
                        Image(
                            bitmap = iconBitmap,
                            contentDescription = notification.appName,
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape),
                        )
                    }
                    Text(
                        text = notification.appName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF81C784),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Date & Time subtitle
            item(key = "detail_time") {
                Text(
                    text = fullDateTime,
                    fontSize = 10.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                )
            }

            // Sender (if present)
            if (notification.sender.isNotBlank()) {
                item(key = "detail_sender") {
                    Text(
                        text = notification.sender,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp),
                    )
                }
            }

            // Message Container (Full text, unconstrained, comfortable reading)
            item(key = "detail_message") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1C1C1C))
                        .border(1.dp, Color(0xFF2E2E2E), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                ) {
                    Text(
                        text = notification.message,
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp,
                        color = Color(0xFFEEEEEE),
                    )
                }
            }

            // Action Buttons (Back & Clear)
            item(key = "detail_actions") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CompactChip(
                        onClick = onClose,
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                modifier = Modifier.size(15.dp),
                                tint = Color.White,
                            )
                        },
                        label = {
                            Text(
                                text = "Back",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                            )
                        },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = Color(0xFF333333),
                            contentColor = Color.White,
                        ),
                    )

                    CompactChip(
                        onClick = onDismiss,
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Clear",
                                modifier = Modifier.size(15.dp),
                                tint = Color(0xFFFF8A80),
                            )
                        },
                        label = {
                            Text(
                                text = "Clear",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFF8A80),
                            )
                        },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = Color(0xFF3D1F1F),
                            contentColor = Color(0xFFFF8A80),
                        ),
                    )
                }
            }
        }
    }
}
