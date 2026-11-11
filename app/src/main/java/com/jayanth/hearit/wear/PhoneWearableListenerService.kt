package com.jayanth.hearit.wear

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

/**
 * Listens for requests from the Wear OS companion app.
 */
class PhoneWearableListenerService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/request/sync") {
            WearDataLayerSync.syncAllNow(this) { _, _ -> }
        } else {
            super.onMessageReceived(messageEvent)
        }
    }
}
