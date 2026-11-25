package com.jayanth.hearit.wear

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class WatchAppFilterItem(
    val packageName: String,
    val appName: String,
    val iconPngBytes: ByteArray? = null,
)

object WearNotificationStore {
    private const val PREFS_NAME = "wear_notifications_prefs"
    private const val KEY_NOTIFICATIONS = "cached_notifications_json"

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

    private val _isMultipointBusy = MutableStateFlow(false)
    val isMultipointBusy: StateFlow<Boolean> = _isMultipointBusy.asStateFlow()

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
            loadFromPrefs()
        }
    }

    private fun loadFromPrefs() {
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_NOTIFICATIONS, null) ?: return
        runCatching {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<WearNotification>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    WearNotification(
                        id = obj.getString("id"),
                        packageName = obj.getString("packageName"),
                        appName = obj.optString("appName", "Unknown App"),
                        sender = obj.optString("sender", ""),
                        message = obj.optString("message", ""),
                        timestamp = obj.optLong("timestamp", 0L),
                        historyId = obj.optLong("historyId", 0L),
                    ),
                )
            }
            _notifications.value = list
        }
    }

    private fun persist() {
        val ctx = appContext ?: return
        val list = _notifications.value
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        list.forEach { notif ->
            arr.put(
                JSONObject().apply {
                    put("id", notif.id)
                    put("packageName", notif.packageName)
                    put("appName", notif.appName)
                    put("sender", notif.sender)
                    put("message", notif.message)
                    put("timestamp", notif.timestamp)
                    put("historyId", notif.historyId)
                },
            )
        }
        prefs.edit().putString(KEY_NOTIFICATIONS, arr.toString()).apply()
    }

    fun setPremiumStatus(active: Boolean) {
        _isPremium.value = active
    }

    fun setMultipointBusy(busy: Boolean) {
        _isMultipointBusy.value = busy
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
        persist()
    }

    fun addNotification(notification: WearNotification) {
        if (!_forwardingEnabled.value) return
        val synced = _watchApps.value
        if (synced != null && synced.none { it.packageName == notification.packageName }) {
            return
        }
        val updated = listOf(notification) + _notifications.value.filter { it.id != notification.id }
        _notifications.value = updated.take(50)
        persist()
    }

    fun setBatchNotifications(incoming: List<WearNotification>) {
        if (!_forwardingEnabled.value) return
        val synced = _watchApps.value
        val allowedPackages = synced?.map { it.packageName }?.toSet()
        val filtered = if (allowedPackages != null) {
            incoming.filter { it.packageName in allowedPackages }
        } else {
            incoming
        }
        _notifications.value = filtered.sortedByDescending { it.timestamp }.take(50)
        persist()
    }

    fun removeNotification(id: String) {
        _notifications.value = _notifications.value.filter { it.id != id }
        persist()
    }

    fun removeByHistoryId(historyId: Long) {
        if (historyId == 0L) return
        _notifications.value = _notifications.value.filter { it.historyId != historyId }
        persist()
    }

    fun removeByPackage(packageName: String) {
        if (packageName.isBlank()) return
        _notifications.value = _notifications.value.filter { it.packageName != packageName }
        persist()
    }

    fun clearAll() {
        _notifications.value = emptyList()
        persist()
    }
}
