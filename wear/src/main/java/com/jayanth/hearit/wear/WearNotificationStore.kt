package com.jayanth.hearit.wear

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object WearNotificationStore {
    private val _notifications = MutableStateFlow<List<WearNotification>>(
        listOf(
            WearNotification(
                id = "demo-1",
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                sender = "Alex Miller",
                message = "Hey, are you free for a quick sync on the roadmap?",
                timestamp = System.currentTimeMillis() - 120_000
            ),
            WearNotification(
                id = "demo-2",
                packageName = "com.google.android.apps.messaging",
                appName = "Messages",
                sender = "Mom",
                message = "Let me know when you reach home safe!",
                timestamp = System.currentTimeMillis() - 600_000
            ),
            WearNotification(
                id = "demo-3",
                packageName = "com.hdfc.bank",
                appName = "HDFC Bank",
                sender = "HDFCBK",
                message = "Sent Rs 2,500 via UPI. Remaining Balance: Rs 48,210. Ref 402918.",
                timestamp = System.currentTimeMillis() - 1_800_000
            )
        )
    )
    val notifications: StateFlow<List<WearNotification>> = _notifications.asStateFlow()

    fun addNotification(notification: WearNotification) {
        val updated = listOf(notification) + _notifications.value.filter { it.id != notification.id }
        _notifications.value = updated.take(50)
    }

    fun removeNotification(id: String) {
        _notifications.value = _notifications.value.filter { it.id != id }
    }

    fun clearAll() {
        _notifications.value = emptyList()
    }
}
