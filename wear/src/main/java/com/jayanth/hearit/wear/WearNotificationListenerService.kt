package com.jayanth.hearit.wear

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import org.json.JSONObject

class WearNotificationListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            "/config/sync" -> {
                try {
                    val jsonString = String(messageEvent.data, Charsets.UTF_8)
                    val json = JSONObject(jsonString)
                    val jsonArray = json.optJSONArray("enabledApps")
                    val enabled = mutableSetOf<String>()
                    if (jsonArray != null) {
                        for (i in 0 until jsonArray.length()) {
                            enabled.add(jsonArray.getString(i))
                        }
                    }
                    WearNotificationStore.setEnabledApps(enabled)
                } catch (_: Exception) {
                }
            }
            "/notifications/new" -> {
                try {
                    val jsonString = String(messageEvent.data, Charsets.UTF_8)
                    val json = JSONObject(jsonString)
                    val notification = WearNotification(
                        id = json.optString("id", System.currentTimeMillis().toString()),
                        packageName = json.optString("packageName", ""),
                        appName = json.optString("appName", "Unknown App"),
                        sender = json.optString("sender", ""),
                        message = json.optString("message", ""),
                        timestamp = json.optLong("timestamp", System.currentTimeMillis())
                    )
                    WearNotificationStore.addNotification(notification)
                } catch (_: Exception) {
                }
            }
            else -> {
                super.onMessageReceived(messageEvent)
            }
        }
    }
}
