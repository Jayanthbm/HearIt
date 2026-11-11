package com.jayanth.hearit.wear

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Base64
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import java.util.concurrent.TimeUnit
import org.json.JSONObject

class WearNotificationListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        try {
            for (event in dataEvents) {
                if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/config/sync") {
                    val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                    val forwardingEnabled = dataMap.getBoolean("forwardingEnabled", true)
                    val hapticsEnabled = dataMap.getBoolean("hapticsEnabled", false)
                    val isPremium = dataMap.getBoolean("isPremium", true)
                    val tempExpiry = dataMap.getLong("temporaryPremiumExpiryTs", 0L)
                    WearNotificationStore.setPremiumStatus(isPremium || tempExpiry > System.currentTimeMillis())

                    val packages = dataMap.getStringArrayList("watchApps").orEmpty()
                    val labels = dataMap.getStringArrayList("watchAppLabels").orEmpty()
                    val items = packages.take(4).mapIndexed { idx, pkg ->
                        val label = labels.getOrNull(idx)?.takeIf { it.isNotBlank() } ?: pkg.substringAfterLast('.')
                        val iconAsset: Asset? = dataMap.getAsset("icon_$idx")
                        val iconBytes = iconAsset?.let { loadAssetBytes(it) }
                        WatchAppFilterItem(
                            packageName = pkg,
                            appName = label,
                            iconPngBytes = iconBytes,
                        )
                    }
                    WearNotificationStore.applyConfigSync(
                        apps = items,
                        forwardingEnabled = forwardingEnabled,
                        hapticsEnabled = hapticsEnabled,
                    )
                }
            }
        } catch (_: Exception) {
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            "/config/sync" -> {
                try {
                    val jsonString = String(messageEvent.data, Charsets.UTF_8)
                    val json = JSONObject(jsonString)
                    val forwardingEnabled = json.optBoolean("forwardingEnabled", true)
                    val hapticsEnabled = json.optBoolean("hapticsEnabled", false)
                    val appsArray = json.optJSONArray("watchApps")
                    val items = mutableListOf<WatchAppFilterItem>()
                    if (appsArray != null) {
                        for (i in 0 until minOf(4, appsArray.length())) {
                            val obj = appsArray.optJSONObject(i) ?: continue
                            val pkg = obj.optString("packageName", "")
                            if (pkg.isBlank()) continue
                            val label = obj.optString("appName", pkg.substringAfterLast('.'))
                            val iconB64 = obj.optString("iconBase64", "")
                            val iconBytes = if (iconB64.isNotEmpty()) {
                                runCatching { Base64.decode(iconB64, Base64.DEFAULT) }.getOrNull()
                            } else {
                                null
                            }
                            items += WatchAppFilterItem(
                                packageName = pkg,
                                appName = label,
                                iconPngBytes = iconBytes,
                            )
                        }
                    }
                    WearNotificationStore.applyConfigSync(
                        apps = items,
                        forwardingEnabled = forwardingEnabled,
                        hapticsEnabled = hapticsEnabled,
                    )

                    val isPremium = json.optBoolean("isPremium", true)
                    val tempExpiry = json.optLong("temporaryPremiumExpiryTs", 0L)
                    WearNotificationStore.setPremiumStatus(isPremium || tempExpiry > System.currentTimeMillis())
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
                        timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                    )
                    val beforeCount = WearNotificationStore.notifications.value.size
                    WearNotificationStore.addNotification(notification)
                    val added = WearNotificationStore.notifications.value.any { it.id == notification.id }
                    if (added && WearNotificationStore.hapticsEnabled.value &&
                        (beforeCount == 0 || WearNotificationStore.notifications.value.firstOrNull()?.id == notification.id)
                    ) {
                        triggerGentleHaptic()
                    }
                } catch (_: Exception) {
                }
            }
            else -> super.onMessageReceived(messageEvent)
        }
    }

    private fun loadAssetBytes(asset: Asset): ByteArray? = runCatching {
        val response = Tasks.await(
            Wearable.getDataClient(applicationContext).getFdForAsset(asset),
            2_000L,
            TimeUnit.MILLISECONDS,
        )
        response.inputStream.use { it.readBytes() }
    }.getOrNull()

    private fun triggerGentleHaptic() {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.vibrate(VibrationEffect.createOneShot(45L, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
}
