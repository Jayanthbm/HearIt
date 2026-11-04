package com.jayanth.hearit.setup

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.jayanth.hearit.R
import com.jayanth.hearit.service.AnnouncementService

/**
 * Platform seams for the setup flow (ADR-023).
 *
 * Each probe answers one factual question the pure `:core:setup` flow cannot answer without
 * Android. Keeping them as interfaces means the ViewModel has no direct framework calls and
 * the gating rules stay testable; the implementations below are deliberately tiny.
 */
interface NotificationAccessProbe {

    /** True when Hear It is in the system's enabled notification-listener list. */
    fun isGranted(): Boolean
}

/** `POST_NOTIFICATIONS` state, needed for the persistent notification on API 33+. */
interface PostNotificationProbe {

    /** True from API 33, where the permission exists. */
    val isRequired: Boolean

    /** True when the permission is granted (always true below API 33). */
    fun isGranted(): Boolean
}

/** `BLUETOOTH_CONNECT` state, required from API 31 for Bluetooth discovery/routing. */
interface BluetoothPermissionProbe {

    /** True from API 31, where the runtime permission exists. */
    val isRequired: Boolean

    /** True when the permission is granted (always true below API 31). */
    fun isGranted(): Boolean
}

/** Installed apps that can be offered in the setup app-selection step. */
interface AppCatalogProbe {

    /** Launcher apps other than Hear It, sorted by label. */
    fun installedApps(): List<InstalledApp>
}

/** Audio outputs connected right now, for orientation in the audio-device step. */
interface AudioOutputProbe {

    /** Bluetooth and wired outputs; the phone speaker is a setting, not a device row. */
    fun connectedOutputs(): List<DetectedOutput>
}

/** Side effects the setup flow triggers: system settings screens and the explicit Start. */
interface SetupLauncher {

    /** Opens the system notification-access list so the user can grant access. */
    fun openNotificationAccessSettings()

    /** Opens the battery-optimisation list (no direct-request permission is used). */
    fun openBatteryOptimizationSettings()

    /** Starts the foreground announcement service — only ever after the explicit Start. */
    fun startAnnouncements()
}

/** [NotificationAccessProbe] over [NotificationManagerCompat]. */
class AndroidNotificationAccessProbe(private val context: Context) : NotificationAccessProbe {

    override fun isGranted(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
}

/** [PostNotificationProbe] over the API-33 runtime permission. */
class AndroidPostNotificationProbe(private val context: Context) : PostNotificationProbe {

    override val isRequired: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    override fun isGranted(): Boolean = !isRequired || ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
}

/** [BluetoothPermissionProbe] over the API-31 `BLUETOOTH_CONNECT` runtime permission. */
class AndroidBluetoothPermissionProbe(private val context: Context) : BluetoothPermissionProbe {

    override val isRequired: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    override fun isGranted(): Boolean = !isRequired || ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.BLUETOOTH_CONNECT,
    ) == PackageManager.PERMISSION_GRANTED
}

/**
 * [AppCatalogProbe] over launcher activities.
 *
 * Deliberately narrower than the P13 catalog: setup only needs a manageable list to get the
 * user started, and launcher visibility is the least surprising subset. Requires the
 * `<queries>` element in the manifest (Android 11+ package visibility).
 */
class AndroidAppCatalogProbe(private val context: Context) : AppCatalogProbe {

    override fun installedApps(): List<InstalledApp> {
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(launcherIntent, 0)
            .asSequence()
            .mapNotNull { it.activityInfo?.applicationInfo }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .map { info -> InstalledApp(info.packageName, packageManager.getApplicationLabel(info).toString()) }
            .sortedBy { it.label.lowercase() }
            .toList()
    }
}

/** [AudioOutputProbe] over [AudioManager.getDevices]; never requests a permission. */
class AndroidAudioOutputProbe(private val context: Context) : AudioOutputProbe {

    override fun connectedOutputs(): List<DetectedOutput> {
        val audioManager = context.getSystemService(AudioManager::class.java) ?: return emptyList()
        return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .mapNotNull { device -> device.toDetectedOutput() }
            .distinctBy { it.id ?: it.label }
    }

    private fun AudioDeviceInfo.toDetectedOutput(): DetectedOutput? = when (type) {
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_BLE_HEADSET,
        AudioDeviceInfo.TYPE_HEARING_AID,
        -> DetectedOutput(id = address?.takeIf { it.isNotBlank() }, label = label(), isBluetooth = true)

        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_USB_HEADSET,
        -> DetectedOutput(id = address?.takeIf { it.isNotBlank() }, label = label(), isBluetooth = false)

        else -> null
    }

    private fun AudioDeviceInfo.label(): String =
        productName?.toString()?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.setup_device_unknown)
}

/** [SetupLauncher] over explicit intents; no silent service start anywhere. */
class AndroidSetupLauncher(private val context: Context) : SetupLauncher {

    override fun openNotificationAccessSettings() {
        context.startActivity(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    override fun openBatteryOptimizationSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            context.startActivity(
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    override fun startAnnouncements() {
        val intent = Intent(context, AnnouncementService::class.java)
            .setAction(AnnouncementService.ACTION_START)
        ContextCompat.startForegroundService(context, intent)
    }
}
