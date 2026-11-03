package com.jayanth.hearit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.jayanth.hearit.applications.ApplicationsViewModel
import com.jayanth.hearit.applications.GlobalSettingsViewModel
import com.jayanth.hearit.dashboard.DashboardRoute
import com.jayanth.hearit.dashboard.DashboardViewModel
import com.jayanth.hearit.devices.DevicesViewModel
import com.jayanth.hearit.history.HistoryViewModel
import com.jayanth.hearit.setup.SetupScreen
import com.jayanth.hearit.setup.SetupViewModel
import com.jayanth.hearit.ui.theme.HearItTheme
import com.jayanth.hearit.ui.theme.HearItTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * The single activity (master prompt: single activity, MVVM, Hilt DI).
 *
 * Routes between the two top-level surfaces of the app: the first-launch setup flow while
 * `setup.completed` is false, and the dashboard shell afterwards (master prompt §4:
 * "Returning users open Dashboard"). Both ViewModels are activity-scoped and survive
 * configuration changes; the setup flow's step and the dashboard's selected destination
 * are both restore-safe, so process death resumes where the user was.
 *
 * Nothing here starts the announcement service: setup's Start button and the dashboard's
 * Start button are the only paths (master prompt §4, ADR-024).
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
                if (setupState.setupCompleted) {
                    DashboardRoute(
                        viewModel = dashboardViewModel,
                        applicationsViewModel = applicationsViewModel,
                        globalSettingsViewModel = globalSettingsViewModel,
                        historyViewModel = historyViewModel,
                        devicesViewModel = devicesViewModel,
                    )
                } else {
                    SetupScreen(
    state = setupViewModel.state.collectAsState().value,
    onBack = setupViewModel::onBack,
    onNext = setupViewModel::onNext,
    onSkip = setupViewModel::onSkip,
    onToggleApp = setupViewModel::onToggleApp,
    onAudioOutputSettingsChanged = { setupViewModel.onAudioOutputSettingsChanged(it) },
    onBatteryGuidanceAcknowledged = setupViewModel::onBatteryGuidanceAcknowledged,
    onTestAnnouncement = setupViewModel::onTestAnnouncement,
    onStart = setupViewModel::onStart,
    onOpenNotificationAccessSettings = setupViewModel::onOpenNotificationAccessSettings,
    onOpenBatterySettings = setupViewModel::onOpenBatterySettings,
    onPermissionsChanged = {},
)
                }
            }
        }
    }
}
