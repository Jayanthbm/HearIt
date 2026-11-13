package com.jayanth.hearit

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.jayanth.hearit.applications.ApplicationsViewModel
import com.jayanth.hearit.applications.GlobalSettingsViewModel
import com.jayanth.hearit.backup.BackupRestoreViewModel
import com.jayanth.hearit.dashboard.DashboardRoute
import com.jayanth.hearit.dashboard.DashboardViewModel
import com.jayanth.hearit.devices.DevicesViewModel
import com.jayanth.hearit.diagnostics.DiagnosticsViewModel
import com.jayanth.hearit.history.HistoryViewModel
import com.jayanth.hearit.security.AppLockScreen
import com.jayanth.hearit.security.BiometricAuthManager
import com.jayanth.hearit.security.BiometricAuthResult
import com.jayanth.hearit.settings.GeneralSettingsViewModel
import com.jayanth.hearit.setup.SetupScreen
import com.jayanth.hearit.setup.SetupViewModel
import com.jayanth.hearit.ui.theme.HearItTheme
import com.jayanth.hearit.voicespeech.VoiceSpeechViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The single activity (master prompt: single activity, MVVM, Hilt DI).
 *
 * Routes between the first-launch setup flow and the dashboard shell.
 * Automatically refreshes permissions on ON_RESUME so when the user grants a permission
 * in system settings, returning to HearIt instantly updates without requiring manual rechecks.
 * Also handles back gestures seamlessly step-by-step.
 * Enforces dynamic FLAG_SECURE and Biometric Gate protection (Master Prompt §15).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var biometricAuthManager: BiometricAuthManager

    private val setupViewModel: SetupViewModel by viewModels()
    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val applicationsViewModel: ApplicationsViewModel by viewModels()
    private val globalSettingsViewModel: GlobalSettingsViewModel by viewModels()
    private val historyViewModel: HistoryViewModel by viewModels()
    private val devicesViewModel: DevicesViewModel by viewModels()
    private val voiceSpeechViewModel: VoiceSpeechViewModel by viewModels()
    private val generalSettingsViewModel: GeneralSettingsViewModel by viewModels()
    private val backupRestoreViewModel: BackupRestoreViewModel by viewModels()
    private val diagnosticsViewModel: DiagnosticsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HearItTheme {
                val setupState by setupViewModel.state.collectAsState()
                val generalSettingsState by generalSettingsViewModel.state.collectAsState()
                val context = LocalContext.current
                val lifecycleOwner = context as? LifecycleOwner

                // Dynamic FLAG_SECURE: hide screen in recents and block screenshots (Master Prompt §15)
                LaunchedEffect(generalSettingsState.security.secureScreenEnabled) {
                    if (generalSettingsState.security.secureScreenEnabled) {
                        window.setFlags(
                            WindowManager.LayoutParams.FLAG_SECURE,
                            WindowManager.LayoutParams.FLAG_SECURE,
                        )
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }

                // Biometric Gate lock state
                var isAppUnlocked by rememberSaveable {
                    mutableStateOf(!generalSettingsState.security.biometricLockEnabled)
                }

                // If biometric lock is disabled in settings, immediately unlock
                LaunchedEffect(generalSettingsState.security.biometricLockEnabled) {
                    if (!generalSettingsState.security.biometricLockEnabled) {
                        isAppUnlocked = true
                    }
                }

                val launchBiometricPrompt = remember(generalSettingsState.security.biometricLockEnabled) {
                    {
                        if (generalSettingsState.security.biometricLockEnabled) {
                            biometricAuthManager.authenticate(
                                activity = this@MainActivity,
                                title = getString(R.string.settings_security_unlock_title),
                                subtitle = getString(R.string.settings_security_unlock_desc),
                            ) { result ->
                                if (result is BiometricAuthResult.Success) {
                                    isAppUnlocked = true
                                }
                            }
                        }
                    }
                }

                // Auto-lock on background (ON_STOP) and auto-prompt on resume (ON_RESUME)
                DisposableEffect(lifecycleOwner, generalSettingsState.security.biometricLockEnabled) {
                    if (lifecycleOwner == null) {
                        onDispose { }
                    } else {
                        val observer = LifecycleEventObserver { _, event ->
                            when (event) {
                                Lifecycle.Event.ON_STOP -> {
                                    if (generalSettingsState.security.biometricLockEnabled) {
                                        isAppUnlocked = false
                                    }
                                }
                                Lifecycle.Event.ON_RESUME -> {
                                    if (!setupState.setupCompleted) {
                                        setupViewModel.onRefresh()
                                    }
                                    if (generalSettingsState.security.biometricLockEnabled && !isAppUnlocked) {
                                        launchBiometricPrompt()
                                    }
                                }
                                else -> Unit
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                    }
                }

                if (generalSettingsState.security.biometricLockEnabled && !isAppUnlocked) {
                    AppLockScreen(
                        onUnlockClicked = { launchBiometricPrompt() },
                    )
                } else if (!setupState.loaded) {
                    // Wait for persisted setup state to load before deciding
                    // which screen to show — avoids a brief flash of the setup
                    // wizard on cold starts when setup is already completed.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else if (setupState.setupCompleted) {
                    DashboardRoute(
                        viewModel = dashboardViewModel,
                        applicationsViewModel = applicationsViewModel,
                        globalSettingsViewModel = globalSettingsViewModel,
                        historyViewModel = historyViewModel,
                        devicesViewModel = devicesViewModel,
                        voiceSpeechViewModel = voiceSpeechViewModel,
                        generalSettingsViewModel = generalSettingsViewModel,
                        backupRestoreViewModel = backupRestoreViewModel,
                        diagnosticsViewModel = diagnosticsViewModel,
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
