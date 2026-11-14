package com.jayanth.hearit.wear

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.jayanth.hearit.core.database.HistoryRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Listens for requests from the Wear OS companion app.
 */
@AndroidEntryPoint
class PhoneWearableListenerService : WearableListenerService() {

    @Inject
    lateinit var historyRepository: HistoryRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            "/request/sync" -> {
                WearDataLayerSync.syncAllNow(this) { _, _ -> }
            }
            "/history/delete" -> {
                runCatching {
                    val json = JSONObject(String(messageEvent.data, Charsets.UTF_8))
                    val historyId = json.optLong("historyId", 0L)
                    val key = json.optString("id", "")
                    scope.launch {
                        if (historyId != 0L) {
                            historyRepository.delete(historyId)
                        } else if (key.isNotBlank()) {
                            val entry = historyRepository.latestFor(key)
                            if (entry != null) {
                                historyRepository.delete(entry.id)
                            }
                        }
                    }
                }
            }
            else -> super.onMessageReceived(messageEvent)
        }
    }
}
