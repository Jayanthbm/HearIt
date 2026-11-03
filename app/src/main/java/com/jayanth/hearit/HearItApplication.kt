package com.jayanth.hearit

import android.app.Application
import android.os.Build
import com.jayanth.hearit.core.common.logger.HearItLogger
import com.jayanth.hearit.core.common.logger.LogCategory
import com.jayanth.hearit.diagnostics.FileDiagnosticLogger
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

    override fun onCreate() {
        super.onCreate()
        HearItLogger.registerSink(fileDiagnosticLogger)
        HearItLogger.info(
            LogCategory.SERVICE,
            "HearIt application initialized (version 1.0, SDK ${Build.VERSION.SDK_INT}, OEM ${Build.MANUFACTURER})",
        )
    }
}
