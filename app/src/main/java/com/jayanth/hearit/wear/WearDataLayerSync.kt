package com.jayanth.hearit.wear

import android.content.Context
import com.google.android.gms.wearable.Wearable
import com.jayanth.hearit.core.common.model.NotificationSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

object WearDataLayerSync {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun syncNotification(context: Context, snapshot: NotificationSnapshot) {
        scope.launch {
            try {
                val nodeClient = Wearable.getNodeClient(context)
                val messageClient = Wearable.getMessageClient(context)
                val nodesTask = nodeClient.connectedNodes
                nodesTask.addOnSuccessListener { nodes ->
                    if (nodes != null && nodes.isNotEmpty()) {
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
                }
            } catch (_: Exception) {
                // Ignore gracefully if Google Play Services or Wearable is unavailable
            }
        }
    }
}
