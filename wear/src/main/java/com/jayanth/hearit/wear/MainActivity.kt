package com.jayanth.hearit.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.AppCard
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.items
import androidx.wear.compose.material.rememberScalingLazyListState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

enum class FilterCategory(val label: String) {
    ALL("All"),
    WHATSAPP("WhatsApp"),
    SMS("SMS"),
    BANKING("Banking")
}

@Composable
fun WearApp() {
    val listState = rememberScalingLazyListState()
    val allNotifications by WearNotificationStore.notifications.collectAsState()
    var selectedFilter by remember { mutableStateOf(FilterCategory.ALL) }

    val filteredNotifications = remember(allNotifications, selectedFilter) {
        when (selectedFilter) {
            FilterCategory.ALL -> allNotifications
            FilterCategory.WHATSAPP -> allNotifications.filter {
                it.packageName.contains("whatsapp", ignoreCase = true) ||
                    it.appName.contains("whatsapp", ignoreCase = true)
            }
            FilterCategory.SMS -> allNotifications.filter {
                it.packageName.contains("messaging", ignoreCase = true) ||
                    it.packageName.contains("sms", ignoreCase = true) ||
                    it.appName.contains("message", ignoreCase = true)
            }
            FilterCategory.BANKING -> allNotifications.filter {
                it.appName.contains("bank", ignoreCase = true) ||
                    it.sender.contains("bank", ignoreCase = true) ||
                    it.sender.contains("hdfc", ignoreCase = true) ||
                    it.message.contains("upi", ignoreCase = true) ||
                    it.message.contains("inr", ignoreCase = true) ||
                    it.message.contains("rs", ignoreCase = true) ||
                    it.message.contains("otp", ignoreCase = true)
            }
        }
    }

    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    text = "HearIt",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64B5F6),
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilterCategory.values().forEach { category ->
                        val isSelected = selectedFilter == category
                        CompactChip(
                            onClick = { selectedFilter = category },
                            label = {
                                Text(
                                    text = category.label,
                                    fontSize = 10.sp,
                                    color = if (isSelected) Color.Black else Color.White
                                )
                            },
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
            }

            if (filteredNotifications.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No notifications in ${selectedFilter.label}",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(filteredNotifications, key = { it.id }) { notif ->
                    NotificationCard(
                        notification = notif,
                        onDismiss = {
                            WearNotificationStore.removeNotification(notif.id)
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                item {
                    CompactChip(
                        onClick = { WearNotificationStore.clearAll() },
                        label = {
                            Text(
                                text = "Clear All",
                                fontSize = 11.sp,
                                color = Color(0xFFFF8A80)
                            )
                        },
                        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    notification: WearNotification,
    onDismiss: () -> Unit
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
                color = Color(0xFF81C784)
            )
        },
        time = {
            Text(
                text = formattedTime,
                fontSize = 10.sp,
                color = Color.LightGray
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
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        Text(
            text = notification.message,
            fontSize = 12.sp,
            color = Color(0xFFE0E0E0),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}
