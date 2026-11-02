package com.jayanth.hearit.setup

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.jayanth.hearit.R
import com.jayanth.hearit.core.common.model.AudioOutputSettings
import com.jayanth.hearit.core.setup.SetupBlocker
import com.jayanth.hearit.core.setup.SetupStep

/**
 * Entry point of the setup feature: observes the ViewModel, refreshes prerequisites whenever
 * the screen resumes (so returning from the system notification-access or battery screens is
 * reflected immediately) and routes to setup, loading or the post-setup placeholder.
 *
 * The dashboard placeholder is deliberately minimal — P12 replaces it.
 */
@Composable
fun SetupRoute(viewModel: SetupViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = context as? LifecycleOwner

    DisposableEffect(lifecycleOwner) {
        if (lifecycleOwner == null) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) viewModel.onRefresh()
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                !state.loaded -> Text(
                    text = stringResource(R.string.setup_loading),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(24.dp),
                )
                state.setupCompleted -> SetupCompleteContent()
                else -> SetupScreen(
                    state = state,
                    onBack = viewModel::onBack,
                    onNext = viewModel::onNext,
                    onSkip = viewModel::onSkip,
                    onToggleApp = viewModel::onToggleApp,
                    onAudioOutputSettingsChanged = viewModel::onAudioOutputSettingsChanged,
                    onBatteryGuidanceAcknowledged = viewModel::onBatteryGuidanceAcknowledged,
                    onTestAnnouncement = viewModel::onTestAnnouncement,
                    onStart = viewModel::onStart,
                    onOpenNotificationAccessSettings = viewModel::onOpenNotificationAccessSettings,
                    onOpenBatterySettings = viewModel::onOpenBatterySettings,
                    onPermissionsChanged = viewModel::onPermissionsChanged,
                )
            }
        }
    }
}

/**
 * The nine-step setup screen (master prompt §4).
 *
 * Stateless: it renders [SetupUiState] and emits events, so no gating or navigation policy
 * lives here (global standards). Every string comes from resources, the app list is lazy,
 * rows carry a single accessible toggle, and the forward action is enabled only when
 * `:core:setup` says so.
 */
@Composable
fun SetupScreen(
    state: SetupUiState,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onToggleApp: (String) -> Unit,
    onAudioOutputSettingsChanged: (AudioOutputSettings) -> Unit,
    onBatteryGuidanceAcknowledged: () -> Unit,
    onTestAnnouncement: (String) -> Unit,
    onStart: () -> Unit,
    onOpenNotificationAccessSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onPermissionsChanged: () -> Unit,
) {
    val testAnnouncementText = stringResource(R.string.setup_test_announcement_spoken)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { onPermissionsChanged() }
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { onPermissionsChanged() }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
        Text(
            text = stringResource(R.string.setup_progress, state.stepIndex, state.stepCount),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(titleFor(state.step)),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 4.dp),
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (state.step == SetupStep.APP_SELECTION) {
                AppSelectionContent(
                    state = state,
                    onToggleApp = onToggleApp,
                )
            } else {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    StepBody(
                        state = state,
                        testAnnouncementText = testAnnouncementText,
                        onBatteryGuidanceAcknowledged = onBatteryGuidanceAcknowledged,
                        onAudioOutputSettingsChanged = onAudioOutputSettingsChanged,
                        onTestAnnouncement = onTestAnnouncement,
                        onOpenNotificationAccessSettings = onOpenNotificationAccessSettings,
                        onOpenBatterySettings = onOpenBatterySettings,
                        onRequestNotificationPermission = {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        },
                        onRequestBluetoothPermission = {
                            bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                        },
                        onPermissionsChanged = onPermissionsChanged,
                    )
                }
            }
        }

        if (state.blockers.isNotEmpty()) {
            BlockersList(blockers = state.blockers)
        }

        NavigationRow(
            state = state,
            onBack = onBack,
            onNext = onNext,
            onSkip = onSkip,
            onStart = onStart,
        )
    }
}

@Composable
private fun StepBody(
    state: SetupUiState,
    testAnnouncementText: String,
    onBatteryGuidanceAcknowledged: () -> Unit,
    onAudioOutputSettingsChanged: (AudioOutputSettings) -> Unit,
    onTestAnnouncement: (String) -> Unit,
    onOpenNotificationAccessSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestBluetoothPermission: () -> Unit,
    onPermissionsChanged: () -> Unit,
) {
    when (state.step) {
        SetupStep.WELCOME -> WelcomeContent()
        SetupStep.NOTIFICATION_ACCESS -> NotificationAccessContent(
            granted = state.prerequisites.notificationAccessGranted,
            onOpenSettings = onOpenNotificationAccessSettings,
        )
        SetupStep.BLUETOOTH_PERMISSION -> BluetoothContent(
            granted = state.prerequisites.bluetoothPermissionGranted,
            onRequest = onRequestBluetoothPermission,
        )
        SetupStep.TEXT_TO_SPEECH -> TextToSpeechContent(
            engineAvailable = state.prerequisites.ttsEngineAvailable,
            languageAvailable = state.prerequisites.ttsLanguageAvailable,
            onRecheck = onPermissionsChanged,
        )
        SetupStep.BATTERY_GUIDANCE -> BatteryGuidanceContent(
            acknowledged = state.batteryGuidanceAcknowledged,
            onOpenBatterySettings = onOpenBatterySettings,
            onAcknowledged = onBatteryGuidanceAcknowledged,
        )
        SetupStep.APP_SELECTION -> Unit
        SetupStep.AUDIO_DEVICES -> AudioDevicesContent(
            state = state,
            onAudioOutputSettingsChanged = onAudioOutputSettingsChanged,
        )
        SetupStep.TEST_ANNOUNCEMENT -> TestAnnouncementContent(
            testState = state.testAnnouncement,
            onPlay = { onTestAnnouncement(testAnnouncementText) },
        )
        SetupStep.REVIEW -> ReviewContent(
            state = state,
            onRequestNotificationPermission = onRequestNotificationPermission,
        )
    }
}

@Composable
private fun WelcomeContent() {
    Text(text = stringResource(R.string.setup_welcome_body), style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(12.dp))
    Text(text = "• ${stringResource(R.string.setup_welcome_point_1)}", style = MaterialTheme.typography.bodyMedium)
    Text(text = "• ${stringResource(R.string.setup_welcome_point_2)}", style = MaterialTheme.typography.bodyMedium)
    Text(text = "• ${stringResource(R.string.setup_welcome_point_3)}", style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun NotificationAccessContent(granted: Boolean, onOpenSettings: () -> Unit) {
    Text(text = stringResource(R.string.setup_notification_access_body), style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.setup_notification_access_status, statusLabel(granted)),
        style = MaterialTheme.typography.bodyMedium,
    )
    Spacer(Modifier.height(12.dp))
    OutlinedButton(onClick = onOpenSettings) {
        Text(text = stringResource(R.string.setup_notification_access_open))
    }
}

@Composable
private fun BluetoothContent(granted: Boolean, onRequest: () -> Unit) {
    Text(text = stringResource(R.string.setup_bluetooth_body), style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.setup_bluetooth_status, statusLabel(granted)),
        style = MaterialTheme.typography.bodyMedium,
    )
    Spacer(Modifier.height(12.dp))
    if (!granted) {
        Button(onClick = onRequest) {
            Text(text = stringResource(R.string.setup_bluetooth_request))
        }
    }
}

@Composable
private fun TextToSpeechContent(
    engineAvailable: Boolean,
    languageAvailable: Boolean,
    onRecheck: () -> Unit,
) {
    Text(text = stringResource(R.string.setup_tts_body), style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(
            R.string.setup_tts_engine_status,
            stringResource(if (engineAvailable) R.string.setup_status_ready else R.string.setup_status_unavailable),
        ),
        style = MaterialTheme.typography.bodyMedium,
    )
    Text(
        text = stringResource(
            R.string.setup_tts_language_status,
            stringResource(if (languageAvailable) R.string.setup_status_ready else R.string.setup_status_unavailable),
        ),
        style = MaterialTheme.typography.bodyMedium,
    )
    Spacer(Modifier.height(12.dp))
    OutlinedButton(onClick = onRecheck) {
        Text(text = stringResource(R.string.setup_recheck))
    }
}

@Composable
private fun BatteryGuidanceContent(
    acknowledged: Boolean,
    onOpenBatterySettings: () -> Unit,
    onAcknowledged: () -> Unit,
) {
    Text(text = stringResource(R.string.setup_battery_body), style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(12.dp))
    OutlinedButton(onClick = onOpenBatterySettings) {
        Text(text = stringResource(R.string.setup_battery_open))
    }
    Spacer(Modifier.height(12.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = acknowledged, role = Role.Checkbox, onValueChange = { onAcknowledged() }),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = acknowledged, onCheckedChange = null)
        Text(
            text = stringResource(R.string.setup_battery_acknowledge),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun AppSelectionContent(state: SetupUiState, onToggleApp: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(text = stringResource(R.string.setup_apps_body), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(R.string.setup_apps_enabled_count, state.enabledApps.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        items(items = state.apps, key = { app -> app.packageName }) { app ->
            AppRow(
                app = app,
                checked = app.packageName in state.enabledApps,
                onToggle = { onToggleApp(app.packageName) },
            )
        }
        if (state.appsLoading) {
            item { Text(text = stringResource(R.string.setup_apps_loading), style = MaterialTheme.typography.bodyMedium) }
        } else if (state.apps.isEmpty()) {
            item { Text(text = stringResource(R.string.setup_apps_empty), style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun AppRow(app: InstalledApp, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun AudioDevicesContent(
    state: SetupUiState,
    onAudioOutputSettingsChanged: (AudioOutputSettings) -> Unit,
) {
    val audio = state.audioOutputSettings
    Text(text = stringResource(R.string.setup_devices_body), style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(8.dp))

    SettingSwitchRow(
        label = stringResource(R.string.setup_devices_allow_bluetooth),
        checked = audio.allowBluetooth,
        onCheckedChange = { onAudioOutputSettingsChanged(audio.copy(allowBluetooth = it)) },
    )
    SettingSwitchRow(
        label = stringResource(R.string.setup_devices_allow_wired),
        checked = audio.allowWired,
        onCheckedChange = { onAudioOutputSettingsChanged(audio.copy(allowWired = it)) },
    )
    SettingSwitchRow(
        label = stringResource(R.string.setup_devices_allow_speaker),
        checked = audio.allowPhoneSpeaker,
        onCheckedChange = { onAudioOutputSettingsChanged(audio.copy(allowPhoneSpeaker = it)) },
    )

    Spacer(Modifier.height(12.dp))
    Text(text = stringResource(R.string.setup_devices_detected_header), style = MaterialTheme.typography.titleSmall)
    if (state.outputs.isEmpty()) {
        Text(text = stringResource(R.string.setup_devices_none_detected), style = MaterialTheme.typography.bodyMedium)
    } else {
        for (output in state.outputs) {
            Text(text = "• ${output.label}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SettingSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun TestAnnouncementContent(
    testState: TestAnnouncementState,
    onPlay: () -> Unit,
) {
    Text(text = stringResource(R.string.setup_test_body), style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(12.dp))
    Button(onClick = onPlay, enabled = testState != TestAnnouncementState.SPEAKING) {
        Text(text = stringResource(R.string.setup_test_play))
    }
    Spacer(Modifier.height(8.dp))
    val status = when (testState) {
        TestAnnouncementState.IDLE -> null
        TestAnnouncementState.SPEAKING -> R.string.setup_test_speaking
        TestAnnouncementState.COMPLETED -> R.string.setup_test_completed
        TestAnnouncementState.FAILED -> R.string.setup_test_failed
    }
    if (status != null) {
        Text(text = stringResource(status), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ReviewContent(
    state: SetupUiState,
    onRequestNotificationPermission: () -> Unit,
) {
    Text(text = stringResource(R.string.setup_review_body), style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.setup_review_apps, state.enabledApps.size),
        style = MaterialTheme.typography.bodyMedium,
    )

    val outputNames = mutableListOf<String>()
    if (state.audioOutputSettings.allowBluetooth) outputNames += stringResource(R.string.setup_devices_allow_bluetooth)
    if (state.audioOutputSettings.allowWired) outputNames += stringResource(R.string.setup_devices_allow_wired)
    if (state.audioOutputSettings.allowPhoneSpeaker) outputNames += stringResource(R.string.setup_devices_allow_speaker)
    Text(
        text = stringResource(R.string.setup_review_outputs, outputNames.joinToString(", ")),
        style = MaterialTheme.typography.bodyMedium,
    )

    val skippedTitles = mutableListOf<String>()
    for (step in state.visibleSteps) {
        if (step in state.skippedSteps) skippedTitles += stringResource(titleFor(step))
    }
    Text(
        text = if (skippedTitles.isEmpty()) {
            stringResource(R.string.setup_review_nothing_skipped)
        } else {
            stringResource(R.string.setup_review_skipped, skippedTitles.joinToString(", "))
        },
        style = MaterialTheme.typography.bodyMedium,
    )

    if (state.notificationPermissionRequired && !state.notificationPermissionGranted) {
        Spacer(Modifier.height(12.dp))
        Card {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(R.string.setup_review_notification_permission_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onRequestNotificationPermission) {
                    Text(text = stringResource(R.string.setup_review_notification_permission_button))
                }
            }
        }
    }
}

@Composable
private fun BlockersList(blockers: List<SetupBlocker>) {
    Spacer(Modifier.height(8.dp))
    for (blocker in blockers) {
        Text(
            text = stringResource(blockerMessage(blocker)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun NavigationRow(
    state: SetupUiState,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onStart: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.canGoBack) {
            OutlinedButton(onClick = onBack) {
                Text(text = stringResource(R.string.setup_back))
            }
        }
        Spacer(Modifier.weight(1f))
        if (state.step == SetupStep.REVIEW) {
            Button(onClick = onStart, enabled = state.canStart) {
                Text(text = stringResource(R.string.setup_start))
            }
        } else {
            if (state.canOnlySkip) {
                TextButton(onClick = onSkip) {
                    Text(text = stringResource(R.string.setup_skip))
                }
            }
            Button(onClick = onNext, enabled = state.canAdvance) {
                Text(text = stringResource(R.string.setup_next))
            }
        }
    }
}

@Composable
private fun SetupCompleteContent() {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(
            text = stringResource(R.string.setup_complete_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.setup_complete_body),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun statusLabel(granted: Boolean): String =
    stringResource(if (granted) R.string.setup_status_granted else R.string.setup_status_not_granted)

@StringRes
private fun titleFor(step: SetupStep): Int = when (step) {
    SetupStep.WELCOME -> R.string.setup_title_welcome
    SetupStep.NOTIFICATION_ACCESS -> R.string.setup_title_notification_access
    SetupStep.BLUETOOTH_PERMISSION -> R.string.setup_title_bluetooth
    SetupStep.TEXT_TO_SPEECH -> R.string.setup_title_tts
    SetupStep.BATTERY_GUIDANCE -> R.string.setup_title_battery
    SetupStep.APP_SELECTION -> R.string.setup_title_apps
    SetupStep.AUDIO_DEVICES -> R.string.setup_title_devices
    SetupStep.TEST_ANNOUNCEMENT -> R.string.setup_title_test
    SetupStep.REVIEW -> R.string.setup_title_review
}

@StringRes
private fun blockerMessage(blocker: SetupBlocker): Int = when (blocker) {
    SetupBlocker.NotificationAccessMissing -> R.string.setup_blocker_notification_access
    SetupBlocker.BluetoothPermissionMissing -> R.string.setup_blocker_bluetooth
    SetupBlocker.TtsEngineUnavailable -> R.string.setup_blocker_tts_engine
    SetupBlocker.TtsLanguageMissing -> R.string.setup_blocker_tts_language
    SetupBlocker.NoEnabledApps -> R.string.setup_blocker_no_apps
    SetupBlocker.NoAllowedOutput -> R.string.setup_blocker_no_output
}
