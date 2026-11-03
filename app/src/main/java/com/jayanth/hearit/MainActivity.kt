package com.jayanth.hearit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.jayanth.hearit.applications.ApplicationsViewModel
import com.jayanth.hearit.applications.GlobalSettingsViewModel
import com.jayanth.hearit.dashboard.DashboardRoute
import com.jayanth.hearit.dashboard.DashboardViewModel
import com.jayanth.hearit.devices.DevicesViewModel
import com.jayanth.hearit.history.HistoryViewModel
import com.jayanth.hearit.setup.SetupScreen
import com.jayanth.hearit.setup.SetupViewModel
import com.jayanth.hearit.ui.theme.HearItTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * The single activity (master prompt: single activity, MVVM, Hilt DI).
 *
 * Routes between the first-launch setup flow and the dashboard shell.
 * Automatically refreshes permissions on ON_RESUME so when the user grants a permission
 * in system settings, returning to HearIt instantly updates without requiring manual rechecks.
 * Also handles back gestures seamlessly step-by-step.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val setupViewModel: SetupViewModel by viewModels()
    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val applicationsViewModel: ApplicationsViewModel by viewModels()
    private val globalSettingsViewModel: GlobalSettingsViewModel by viewModels()
    private val historyViewModel: HistoryViewModel by viewModels()
    private val devicesViewModel: DevicesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HearItTheme {
                val setupState by setupViewModel.state.collectAsState()
                val context = LocalContext.current
                val lifecycleOwner = context as? LifecycleOwner

                // Lifecycle observer to auto-refresh probes whenever the app comes to foreground
                DisposableEffect(lifecycleOwner) {
                    if (lifecycleOwner == null) {
                        onDispose { }
                    } else {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME) {
                                if (!setupState.setupCompleted) {
                                    setupViewModel.onRefresh()
                                }
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                    }
                }

                if (setupState.setupCompleted) {
                    DashboardRoute(
                        viewModel = dashboardViewModel,
                        applicationsViewModel = applicationsViewModel,
                        globalSettingsViewModel = globalSettingsViewModel,
                        historyViewModel = historyViewModel,
                        devicesViewModel = devicesViewModel,
                    )
                } else {
                    // Modern back gesture handling: navigate back step-by-step
                    BackHandler(enabled = setupState.canGoBack) {
                        setupViewModel.onBack()
                    }

                    SetupScreen(
                        state = setupState,
                        onBack = setupViewModel::onBack,
                        onNext = setupViewModel::onNext,
                        onSkip = setupViewModel::onSkip,
                        onToggleApp = setupViewModel::onToggleApp,
                        onAudioOutputSettingsChanged = setupViewModel::onAudioOutputSettingsChanged,
                        onBatteryGuidanceAcknowledged = setupViewModel::onBatteryGuidanceAcknowledged,
                        onTestAnnouncement = setupViewModel::onTestAnnouncement,
                        onStart = setupViewModel::onStart,
                        onOpenNotificationAccessSettings = setupViewModel::onOpenNotificationAccessSettings,
                        onOpenBatterySettings = setupViewModel::onOpenBatterySettings,
                        onPermissionsChanged = setupViewModel::onRefresh,
                    )
                }
            }
        }
    }
}
