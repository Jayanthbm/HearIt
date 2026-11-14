package com.jayanth.hearit.service

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import com.jayanth.hearit.core.common.model.AudioOutput
import com.jayanth.hearit.core.common.model.AudioOutputKind
import com.jayanth.hearit.core.common.model.RuntimeState

/**
 * Reads the device state the rules pipeline needs, without requesting any runtime permissions.
 */
class RuntimeStateReader(
    context: Context,
    private val audioManager: AudioManager,
    private val sessionStore: SessionStore,
) {

    private val appContext = context.applicationContext
    private val powerManager = context.getSystemService(PowerManager::class.java)
    private val keyguardManager = context.getSystemService(KeyguardManager::class.java)
    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private val batteryManager = context.getSystemService(android.os.BatteryManager::class.java)

    private var lastScreenWakeTime: Long = 0L

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_ON) {
                lastScreenWakeTime = SystemClock.elapsedRealtime()
            }
        }
    }

    init {
        try {
            val filter = IntentFilter(Intent.ACTION_SCREEN_ON)
            appContext.registerReceiver(screenReceiver, filter)
        } catch (_: Exception) {}
    }

    fun unregister(context: Context? = null) {
        try {
            appContext.unregisterReceiver(screenReceiver)
        } catch (_: Exception) {}
    }

    /** Current battery percentage (1..100, fallback 100). */
    fun readBatteryPercent(): Int =
        batteryManager?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
            ?.takeIf { it in 1..100 }
            ?: 100

    /** Snapshot of the current device state for one rules evaluation. */
    fun read(
        priorityAnnounceActive: Boolean = false,
        isProximityCovered: Boolean = true,
        lowBatteryThresholdPercent: Int = 15,
        pauseOnPowerSaver: Boolean = true,
    ): RuntimeState {
        val capacity = readBatteryPercent()
        val isPowerSave = powerManager?.isPowerSaveMode ?: false
        val isLowBattery = (capacity in 1..lowBatteryThresholdPercent) || (isPowerSave && pauseOnPowerSaver)

        val isLocked = keyguardManager?.isKeyguardLocked ?: false
        val rawScreenInteractive = powerManager?.isInteractive ?: true
        val now = SystemClock.elapsedRealtime()
        // If the screen turned on within 1.5 seconds while the device is locked, it was woken by
        // ambient notification display. Treat it as screen-off so ambient notifications aren't dropped.
        val isAmbientWake = isLocked && rawScreenInteractive && (now - lastScreenWakeTime in 0..AMBIENT_WAKE_BUFFER_MILLIS)
        val screenOn = rawScreenInteractive && !isAmbientWake

        return RuntimeState(
            screenOn = screenOn,
            isLocked = isLocked,
            temporaryMuteActive = sessionStore.isTemporaryMuteActive,
            priorityAnnounceActive = priorityAnnounceActive,
            isCallActive = audioManager.mode == AudioManager.MODE_IN_CALL ||
                audioManager.mode == AudioManager.MODE_IN_COMMUNICATION,
            isDndActive = (notificationManager?.currentInterruptionFilter ?: DND_OFF) != DND_OFF,
            activeOutput = readActiveOutput(),
            isBatteryLowOrSaver = isLowBattery,
            isProximityCovered = isProximityCovered,
            batteryPercent = capacity,
            isPowerSave = isPowerSave,
        )
    }

    /** Best connected output, in preference order; null when nothing is observed. */
    private fun readActiveOutput(): AudioOutput? {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)

        fun firstOf(vararg types: Int): AudioDeviceInfo? =
            devices.firstOrNull { device -> types.any { it == device.type } }

        val bluetoothOutput = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            firstOf(
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                AudioDeviceInfo.TYPE_BLE_HEADSET,
                AudioDeviceInfo.TYPE_BLE_SPEAKER,
                AudioDeviceInfo.TYPE_HEARING_AID,
            )
        } else {
            firstOf(
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            )
        }
        bluetoothOutput?.let { return it.toOutput(AudioOutputKind.BLUETOOTH) }

        firstOf(
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_USB_HEADSET,
        )?.let { return it.toOutput(AudioOutputKind.WIRED) }

        firstOf(
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE,
        )?.let { return it.toOutput(AudioOutputKind.PHONE_SPEAKER) }

        // Anything else the platform exposes as a sink (OEM routes, remote submix, cast
        // endpoints) so the "Unrecognised outputs" switch is actually enforced.
        devices.firstOrNull { it.isSink }?.let { return it.toOutput(AudioOutputKind.OTHER) }

        return null
    }

    private fun AudioDeviceInfo.toOutput(kind: AudioOutputKind): AudioOutput = AudioOutput(
        kind = kind,
        id = address?.takeIf { it.isNotBlank() },
        name = productName?.toString(),
    )

    private companion object {
        const val DND_OFF = NotificationManager.INTERRUPTION_FILTER_ALL
        const val AMBIENT_WAKE_BUFFER_MILLIS = 1500L
    }
}
