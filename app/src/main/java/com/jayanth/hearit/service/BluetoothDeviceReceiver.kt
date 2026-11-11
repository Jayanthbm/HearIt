package com.jayanth.hearit.service

import android.annotation.SuppressLint
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.jayanth.hearit.core.common.logger.HearItLogger
import com.jayanth.hearit.core.common.logger.LogCategory

/**
 * Handles Bluetooth audio device connection changes with audio-profile filtering,
 * separate Auto-Start/Auto-Stop toggles, and optional allowed-devices restriction (#27).
 */
class BluetoothDeviceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val sessionStore = SessionStore(context)
        val device = extractBluetoothDevice(intent)

        if (!isAudioCapableOrActive(context, device)) {
            HearItLogger.info(LogCategory.SERVICE, "Ignoring non-audio Bluetooth event (${intent.action})")
            return
        }

        if (sessionStore.isBluetoothAutoOnlyAllowedEnabled) {
            val allowedIds = sessionStore.cachedAllowedBluetoothIds
            val deviceId = device?.address
            if (allowedIds.isNotEmpty() && (deviceId == null || deviceId !in allowedIds)) {
                HearItLogger.info(LogCategory.SERVICE, "Bluetooth device $deviceId not in allowed headset list; skipping")
                return
            }
        }

        when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                if (!sessionStore.isBluetoothAutoStartEnabled) return
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
                sessionStore.lastHeadphoneDisconnectedAtMillis = System.currentTimeMillis()
                if (!sessionStore.isBluetoothAutoStopEnabled) return
                HearItLogger.info(LogCategory.SERVICE, "Bluetooth audio disconnected -> Auto-stopping service")
                try {
                    context.startService(AnnouncementService.createStopIntent(context))
                } catch (e: Exception) {
                    HearItLogger.error(LogCategory.SERVICE, "Failed to auto-stop service on Bluetooth disconnect", e)
                }
            }
        }
    }

    private fun extractBluetoothDevice(intent: Intent): BluetoothDevice? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }

    @SuppressLint("MissingPermission")
    private fun isAudioCapableOrActive(context: Context, device: BluetoothDevice?): Boolean {
        if (device != null) {
            val btClass = runCatching { device.bluetoothClass }.getOrNull()
            if (btClass != null) {
                val isAudioMajor = btClass.majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO
                val hasAudioService = btClass.hasService(BluetoothClass.Service.AUDIO) ||
                    btClass.hasService(BluetoothClass.Service.RENDER)
                return isAudioMajor || hasAudioService
            }
        }
        val audioManager = context.getSystemService(AudioManager::class.java) ?: return true
        val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val hasBtAudioOutput = outputs.any {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
        }
        return hasBtAudioOutput || device == null
    }
}
