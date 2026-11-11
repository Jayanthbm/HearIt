package com.jayanth.hearit.wear

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WatchAppFilterItem(
    val packageName: String,
    val appName: String,
    val iconPngBytes: ByteArray? = null,
)

object WearNotificationStore {
    private val _notifications = MutableStateFlow<List<WearNotification>>(emptyList())
    val notifications: StateFlow<List<WearNotification>> = _notifications.asStateFlow()

    /**
     * Ordered watch apps (max 4) synced from phone (#35-#37).
     * `null` = awaiting initial sync from phone; `emptyList()` = 0 watch apps enabled.
     */
    private val _watchApps = MutableStateFlow<List<WatchAppFilterItem>?>(null)
    val watchApps: StateFlow<List<WatchAppFilterItem>?> = _watchApps.asStateFlow()

    private val _forwardingEnabled = MutableStateFlow(true)
    val forwardingEnabled: StateFlow<Boolean> = _forwardingEnabled.asStateFlow()

    private val _hapticsEnabled = MutableStateFlow(false)
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled.asStateFlow()

    private val _isPremium = MutableStateFlow(true)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    fun setPremiumStatus(active: Boolean) {
        _isPremium.value = active
    }

    fun applyConfigSync(
        apps: List<WatchAppFilterItem>,
        forwardingEnabled: Boolean,
        hapticsEnabled: Boolean,
    ) {
        val capped = apps.take(4)
        _watchApps.value = capped
        _forwardingEnabled.value = forwardingEnabled
        _hapticsEnabled.value = hapticsEnabled
        val allowedPackages = capped.map { it.packageName }.toSet()
        _notifications.value = if (!forwardingEnabled || allowedPackages.isEmpty()) {
            emptyList()
        } else {
            _notifications.value.filter { it.packageName in allowedPackages }
        }
    }

    fun addNotification(notification: WearNotification) {
        if (!_forwardingEnabled.value) return
        val synced = _watchApps.value
        if (synced != null && synced.none { it.packageName == notification.packageName }) {
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
