package com.jayanth.hearit.service

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.jayanth.hearit.core.common.logger.HearItLogger
import com.jayanth.hearit.core.common.logger.LogCategory

/**
 * Handles Bluetooth device connection state changes to auto-start and auto-stop
 * the announcement service hands-free (Idea 15).
 */
class BluetoothDeviceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val sessionStore = SessionStore(context)
        if (!sessionStore.isBluetoothAutoStartEnabled) return

        when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                HearItLogger.info(LogCategory.SERVICE, "Bluetooth audio connected -> Auto-starting service")
                try {
                    ContextCompat.startForegroundService(
                        context,
                        AnnouncementService.createStartIntent(context),
                    )
                } catch (e: Exception) {
                    HearItLogger.error(LogCategory.SERVICE, "Failed to auto-start service on Bluetooth connect", e)
                }
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                HearItLogger.info(LogCategory.SERVICE, "Bluetooth audio disconnected -> Auto-stopping service")
                try {
                    context.startService(AnnouncementService.createStopIntent(context))
                } catch (e: Exception) {
                    HearItLogger.error(LogCategory.SERVICE, "Failed to auto-stop service on Bluetooth disconnect", e)
                }
            }
        }
    }
}
