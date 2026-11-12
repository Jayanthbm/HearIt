package com.jayanth.hearit.wear

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.util.Base64
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.jayanth.hearit.core.common.model.NotificationSnapshot
import com.jayanth.hearit.service.SessionStore
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class WearCompanionState(
    val isConnected: Boolean = false,
    /** True when a paired watch advertises the Hear It watch app capability. */
    val watchAppInstalled: Boolean = false,
    val deviceName: String = "No watch connected",
    val lastSyncTimestamp: Long = 0L,
    val watchFilterApps: List<String> = emptyList(),
    val isForwardingEnabled: Boolean = true,
    val isHapticsEnabled: Boolean = false,
) {
    val enabledAppsCount: Int get() = watchFilterApps.size
}

object WearDataLayerSync {
    private val scope = CoroutineScope(Dispatchers.IO)
    private const val PREFS_NAME = "wear_companion_prefs"
    private const val KEY_LAST_SYNC = "last_sync_timestamp"
    private const val ICON_SIZE_PX = 48
    private const val WATCH_APP_CAPABILITY = "hearit_watch_app"

    fun checkCompanionStatus(context: Context, callback: (WearCompanionState) -> Unit) {
        val appContext = context.applicationContext
        scope.launch {
            val sessionStore = SessionStore(appContext)
            val watchApps = sessionStore.watchFilterApps
            val forwarding = sessionStore.isWatchForwardingEnabled
            val haptics = sessionStore.isWatchHapticsEnabled
            val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastSync = prefs.getLong(KEY_LAST_SYNC, 0L)

            val nodes = runCatching {
                Tasks.await(Wearable.getNodeClient(appContext).connectedNodes)
            }.getOrNull().orEmpty()

            // FILTER_REACHABLE proves the watch app is installed and online right now;
            // FILTER_ALL also reports a paired watch that is simply out of range.
            val reachableWatchAppNodes = watchAppNodes(appContext, CapabilityClient.FILTER_REACHABLE)
            val knownWatchAppNodes = watchAppNodes(appContext, CapabilityClient.FILTER_ALL)
            val watchAppInstalled = reachableWatchAppNodes.isNotEmpty() || knownWatchAppNodes.isNotEmpty()

            val deviceName = (reachableWatchAppNodes.firstOrNull()
                ?: knownWatchAppNodes.firstOrNull()
                ?: nodes.firstOrNull())
                ?.displayName
                ?.takeIf { it.isNotBlank() }
                ?: "No watch connected"

            callback(
                WearCompanionState(
                    isConnected = nodes.isNotEmpty(),
                    watchAppInstalled = watchAppInstalled,
                    deviceName = deviceName,
                    lastSyncTimestamp = lastSync,
                    watchFilterApps = watchApps,
                    isForwardingEnabled = forwarding,
                    isHapticsEnabled = haptics,
                ),
            )
        }
    }

    private fun watchAppNodes(context: Context, filterType: Int): Set<Node> = runCatching {
        Tasks.await(
            Wearable.getCapabilityClient(context).getCapability(WATCH_APP_CAPABILITY, filterType),
        ).nodes
    }.getOrDefault(emptySet())

    /**
     * Syncs persistent Watch configuration (`/config/sync` via DataClient + real 48x48 PNG app icons)
     * and pushes an immediate MessageClient update to connected nodes (#36).
     */
    fun syncAllNow(context: Context, callback: (Boolean, String) -> Unit) {
        val appContext = context.applicationContext
        scope.launch {
            try {
                val sessionStore = SessionStore(appContext)
                val watchApps = sessionStore.watchFilterApps
                val forwardingEnabled = sessionStore.isWatchForwardingEnabled
                val hapticsEnabled = sessionStore.isWatchHapticsEnabled
                val isPremium = sessionStore.isPremiumActive
                val tempExpiry = sessionStore.temporaryPremiumExpiryTimestamp
                val now = System.currentTimeMillis()

                val pm = appContext.packageManager
                val labels = ArrayList<String>(watchApps.size)
                val iconBytesList = ArrayList<ByteArray?>(watchApps.size)

                watchApps.forEach { pkg ->
                    val label = runCatching {
                        val info = pm.getApplicationInfo(pkg, 0)
                        pm.getApplicationLabel(info).toString()
                    }.getOrDefault(pkg.substringAfterLast('.'))
                    labels.add(label)
                    iconBytesList.add(loadCompressedAppIcon(appContext, pkg))
                }

                // 1. Persistent DataClient sync (auto-delivers on reconnect with Asset PNG icons)
                val putDataMapRequest = PutDataMapRequest.create("/config/sync").apply {
                    dataMap.putLong("timestamp", now)
                    dataMap.putBoolean("forwardingEnabled", forwardingEnabled)
                    dataMap.putBoolean("hapticsEnabled", hapticsEnabled)
                    dataMap.putBoolean("isPremium", isPremium)
                    dataMap.putLong("temporaryPremiumExpiryTs", tempExpiry)
                    dataMap.putStringArrayList("watchApps", ArrayList(watchApps))
                    dataMap.putStringArrayList("watchAppLabels", labels)
                    iconBytesList.forEachIndexed { idx, bytes ->
                        if (bytes != null) {
                            dataMap.putAsset("icon_$idx", Asset.createFromBytes(bytes))
                        }
                    }
                    setUrgent()
                }
                val dataSaved = runCatching {
                    Tasks.await(Wearable.getDataClient(appContext).putDataItem(putDataMapRequest.asPutDataRequest()))
                    true
                }.getOrDefault(false)

                // 2. Instant MessageClient sync when watch nodes are currently online
                val nodes = runCatching {
                    Tasks.await(Wearable.getNodeClient(appContext).connectedNodes)
                }.getOrNull().orEmpty()

                if (nodes.isNotEmpty()) {
                    val appsArray = JSONArray()
                    watchApps.forEachIndexed { idx, pkg ->
                        appsArray.put(
                            JSONObject().apply {
                                put("packageName", pkg)
                                put("appName", labels.getOrElse(idx) { pkg })
                                val bytes = iconBytesList.getOrNull(idx)
                                if (bytes != null) {
                                    put("iconBase64", Base64.encodeToString(bytes, Base64.NO_WRAP))
                                }
                            },
                        )
                    }
                    val payload = JSONObject().apply {
                        put("type", "CONFIG_SYNC")
                        put("timestamp", now)
                        put("forwardingEnabled", forwardingEnabled)
                        put("hapticsEnabled", hapticsEnabled)
                        put("watchApps", appsArray)
                        put("isPremium", isPremium)
                        put("temporaryPremiumExpiryTs", tempExpiry)
                    }.toString().toByteArray(Charsets.UTF_8)

                    val msgClient = Wearable.getMessageClient(appContext)
                    nodes.forEach { node ->
                        runCatching { Tasks.await(msgClient.sendMessage(node.id, "/config/sync", payload)) }
                    }
                }

                val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putLong(KEY_LAST_SYNC, now).apply()

                if (nodes.isNotEmpty()) {
                    callback(true, "Synced ${watchApps.size}/4 watch apps")
                } else if (dataSaved) {
                    callback(true, "Saved ${watchApps.size}/4 watch apps — auto-syncs when watch connects")
                } else {
                    callback(false, "Wear OS service unavailable on this device")
                }
            } catch (e: Exception) {
                callback(false, "Sync failed: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    /**
     * Option A independent notification forwarding (#36): mirrors notifications for the user's
     * max-4 Watch Apps whenever Watch Forwarding is ON, regardless of AnnouncementService state.
     */
    fun syncNotification(context: Context, snapshot: NotificationSnapshot) {
        val appContext = context.applicationContext
        val sessionStore = SessionStore(appContext)
        if (!sessionStore.isWatchForwardingEnabled) return
        if (snapshot.packageName !in sessionStore.watchFilterApps) return

        scope.launch {
            try {
                val nodeClient = Wearable.getNodeClient(appContext)
                val messageClient = Wearable.getMessageClient(appContext)
                val nodes = Tasks.await(nodeClient.connectedNodes)

                if (!nodes.isNullOrEmpty()) {
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
                // Ignore gracefully if Wearable API is unavailable
            }
        }
    }

    private fun loadCompressedAppIcon(context: Context, packageName: String): ByteArray? = runCatching {
        val drawable = context.packageManager.getApplicationIcon(packageName)
        val bitmap = if (drawable is BitmapDrawable &&
            drawable.bitmap.width == ICON_SIZE_PX &&
            drawable.bitmap.height == ICON_SIZE_PX
        ) {
            drawable.bitmap
        } else {
            val created = Bitmap.createBitmap(ICON_SIZE_PX, ICON_SIZE_PX, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(created)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            created
        }
        ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            out.toByteArray()
        }
    }.getOrNull()
}
