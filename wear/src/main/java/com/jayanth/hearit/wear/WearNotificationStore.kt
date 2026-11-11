package com.jayanth.hearit.wear

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object WearNotificationStore {
    private val _notifications = MutableStateFlow<List<WearNotification>>(emptyList())
    val notifications: StateFlow<List<WearNotification>> = _notifications.asStateFlow()

    // Set of enabled package names synced from phone.
    // null = no sync received yet; emptySet = synced, 0 apps enabled.
    private val _enabledApps = MutableStateFlow<Set<String>?>(null)
    val enabledApps: StateFlow<Set<String>?> = _enabledApps.asStateFlow()

    // Premium status synced from Phone
    private val _isPremium = MutableStateFlow(true)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    fun setPremiumStatus(active: Boolean) {
        _isPremium.value = active
    }

    fun setEnabledApps(apps: Set<String>) {
        _enabledApps.value = apps
        // Filter existing notifications to only those in the enabled set
        _notifications.value = _notifications.value.filter { notif ->
            apps.contains(notif.packageName)
        }
    }

    fun addNotification(notification: WearNotification) {
        val current = _enabledApps.value
        // If config has been received and this app is not enabled, do not display it
        if (current != null && !current.contains(notification.packageName)) {
            return
        }
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
