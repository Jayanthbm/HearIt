package com.jayanth.hearit.wear

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable
import com.jayanth.hearit.core.common.model.NotificationSnapshot
import com.jayanth.hearit.core.datastore.HearItSettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class WearCompanionState(
    val isConnected: Boolean = false,
    val deviceName: String = "No watch connected",
    val lastSyncTimestamp: Long = 0L,
    val enabledAppsCount: Int = 0,
)

object WearDataLayerSync {
    private val scope = CoroutineScope(Dispatchers.IO)
    private const val PREFS_NAME = "wear_companion_prefs"
    private const val KEY_LAST_SYNC = "last_sync_timestamp"

    fun checkCompanionStatus(context: Context, callback: (WearCompanionState) -> Unit) {
        scope.launch {
            try {
                val nodeClient = Wearable.getNodeClient(context)
                val nodes = Tasks.await(nodeClient.connectedNodes)
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val lastSync = prefs.getLong(KEY_LAST_SYNC, 0L)

                val repository = HearItSettingsDataStore.repository(context)
                val settings = repository.settings.first()
                val enabledCount = settings.enabledApps.size

                if (!nodes.isNullOrEmpty()) {
                    val node = nodes.first()
                    callback(
                        WearCompanionState(
                            isConnected = true,
                            deviceName = node.displayName.ifBlank { "Wear OS Watch" },
                            lastSyncTimestamp = lastSync,
                            enabledAppsCount = enabledCount
                        )
                    )
                } else {
                    callback(
                        WearCompanionState(
                            isConnected = false,
                            deviceName = "No watch connected",
                            lastSyncTimestamp = lastSync,
                            enabledAppsCount = enabledCount
                        )
                    )
                }
            } catch (_: Exception) {
                callback(WearCompanionState(isConnected = false, deviceName = "Wear OS unavailable"))
            }
        }
    }

    fun syncAllNow(context: Context, callback: (Boolean, String) -> Unit) {
        scope.launch {
            try {
                val nodeClient = Wearable.getNodeClient(context)
                val messageClient = Wearable.getMessageClient(context)
                val nodes = Tasks.await(nodeClient.connectedNodes)

                if (nodes.isNullOrEmpty()) {
                    callback(false, "No Wear OS device connected. Please pair or connect your watch.")
                    return@launch
                }

                val repository = HearItSettingsDataStore.repository(context)
                val settings = repository.settings.first()
                val enabledApps = settings.enabledApps

                val jsonArray = JSONArray()
                enabledApps.forEach { jsonArray.put(it) }

                val payload = JSONObject().apply {
                    put("type", "CONFIG_SYNC")
                    put("timestamp", System.currentTimeMillis())
                    put("enabledApps", jsonArray)
                }.toString().toByteArray(Charsets.UTF_8)

                for (node in nodes) {
                    Tasks.await(messageClient.sendMessage(node.id, "/config/sync", payload))
                }

                val now = System.currentTimeMillis()
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putLong(KEY_LAST_SYNC, now).apply()

                val names = nodes.joinToString { it.displayName }
                callback(true, "Synced ${enabledApps.size} enabled apps to $names")
            } catch (e: Exception) {
                callback(false, "Sync failed: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun syncNotification(context: Context, snapshot: NotificationSnapshot) {
        scope.launch {
            try {
                val nodeClient = Wearable.getNodeClient(context)
                val messageClient = Wearable.getMessageClient(context)
                val nodes = Tasks.await(nodeClient.connectedNodes)

                if (!nodes.isNullOrEmpty()) {
                    val repository = HearItSettingsDataStore.repository(context)
                    val settings = repository.settings.first()
                    // If the notification's app is not enabled on phone, do not send to watch
                    if (!settings.enabledApps.contains(snapshot.packageName)) {
                        return@launch
                    }

                    val payload = JSONObject().apply {
                        put("id", snapshot.key)
                        put("packageName", snapshot.packageName)
                        put("appName", snapshot.appName)
                        put("sender", snapshot.sender ?: "")
                        put("message", snapshot.messageText)
                        put("timestamp", snapshot.postTimeMillis)
                    }.toString().toByteArray(Charsets.UTF_8)

                    for (node in nodes) {
                        messageClient.sendMessage(node.id, "/notifications/new", payload)
                    }
                }
            } catch (_: Exception) {
                // Ignore gracefully if Google Play Services or Wearable is unavailable
            }
        }
    }
}
