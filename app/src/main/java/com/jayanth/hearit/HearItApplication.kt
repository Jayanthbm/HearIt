package com.jayanth.hearit

import android.app.Application
import android.os.Build
import com.jayanth.hearit.core.common.logger.HearItLogger
import com.jayanth.hearit.core.common.logger.LogCategory
import com.jayanth.hearit.diagnostics.FileDiagnosticLogger
import com.jayanth.hearit.intelligence.IntelligentSettingsSync
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class and Hilt dependency-injection root (architecture §3, ADR-001).
 *
 * Every Hilt-backed component in the app is rooted here (activities via
 * `@AndroidEntryPoint`; services the same when they arrive in P07/P08). Constructor
 * injection and Hilt modules only — no service locator, no global mutable state
 * (global standards).
 */
@HiltAndroidApp
class HearItApplication : Application() {

    @Inject
    lateinit var fileDiagnosticLogger: FileDiagnosticLogger

    @Inject
    lateinit var intelligentSettingsSync: IntelligentSettingsSync

    override fun onCreate() {
        super.onCreate()
        HearItLogger.registerSink(fileDiagnosticLogger)
        intelligentSettingsSync.start()
        com.jayanth.hearit.core.notifications.NotificationRelay.onAnyNotificationPosted = { snapshot ->
            com.jayanth.hearit.wear.WearDataLayerSync.syncNotification(this, snapshot)
        }
        val appVersion = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "2.0.0"
        } catch (e: Exception) {
            "2.0.0"
        }
        HearItLogger.info(
            LogCategory.SERVICE,
            "HearIt application initialized (version $appVersion, SDK ${Build.VERSION.SDK_INT}, OEM ${Build.MANUFACTURER})",
        )
    }
}
