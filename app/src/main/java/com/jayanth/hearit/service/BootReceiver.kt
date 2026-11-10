package com.jayanth.hearit.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.jayanth.hearit.core.common.logger.HearItLogger
import com.jayanth.hearit.core.common.logger.LogCategory

/**
 * Boot receiver to auto-recover the announcement service after a device reboot
 * or app update if it was previously running (SessionStore.isStarted == true).
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val sessionStore = SessionStore(context)
            HearItLogger.info(
                LogCategory.SERVICE,
                "BootReceiver triggered: action=$action, isStarted=${sessionStore.isStarted}",
            )
            if (sessionStore.isStarted) {
                try {
                    ContextCompat.startForegroundService(
                        context,
                        AnnouncementService.createStartIntent(context),
                    )
                } catch (e: Exception) {
                    HearItLogger.error(
                        LogCategory.SERVICE,
                        "Failed to auto-restart AnnouncementService on boot",
                        e,
                    )
                }
            }
        }
    }
}
