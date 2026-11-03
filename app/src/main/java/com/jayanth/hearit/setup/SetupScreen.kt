package com.jayanth.hearit.setup

import android.Manifest
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.jayanth.hearit.R
import com.jayanth.hearit.core.common.model.AudioOutputSettings
import com.jayanth.hearit.core.setup.SetupBlocker
import com.jayanth.hearit.core.setup.SetupStep
import com.jayanth.hearit.ui.theme.Primary
import com.jayanth.hearit.ui.theme.PrimaryContainer
import com.jayanth.hearit.ui.theme.Surface
import com.jayanth.hearit.ui.theme.SurfaceVariant
import com.jayanth.hearit.ui.theme.OnSurface
import com.jayanth.hearit.ui.theme.Outline
import com.jayanth.hearit.ui.theme.Tertiary
import com.jayanth.hearit.ui.theme.TertiaryContainer
import com.jayanth.hearit.ui.theme.Error
import com.jayanth.hearit.ui.theme.ErrorContainer



// ---------- animated step page transitions ----------

@Composable
private fun AnimatedStepPage(
    currentStep: SetupStep,
    content: @Composable (SetupStep) -> Unit,
) {
    AnimatedContent(
        targetState = currentStep,
        transitionSpec = {
            val enter = fadeIn(animationSpec = tween(260, easing = LinearEasing))
                .plus(scaleIn(initialScale = 0.97f, animationSpec = tween(260, easing = LinearEasing)))
            val exit = fadeOut(animationSpec = tween(120, easing = LinearEasing))
                .plus(scaleOut(targetScale = 0.97f, animationSpec = tween(120, easing = LinearEasing)))
            ContentTransform(enter, exit)
        },
        label = "setup_step_page",
    ) { target ->
        content(target)
    }
}

// ---------- stepper ----------

@Composable
private fun StepChip(
    step: SetupStep,
    selected: Boolean,
    position: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    val isLast = position == total - 1
    val shape = if (isLast) androidx.compose.foundation.shape.CircleShape
    else androidx.compose.foundation.shape.RoundedCornerShape(12.dp)

    Card(
        modifier = modifier
            .padding(end = if (isLast) 0.dp else 8.dp)
            .size(width = 36.dp, height = 36.dp),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) PrimaryContainer else SurfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) 4.dp else 0.dp,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(if (selected) Primary else Color.Transparent),
            contentAlignment = Alignment.Center,
            ) {
            if (selected) {
                Canvas(
                    modifier = Modifier.size(18.dp),
                ) {
                    drawCircle(brush = Brush.verticalGradient(
                        colors = listOf(Color.White, Color(0xFF1A2A4A))
                    ), radius = 9f)
                }
            }
        }
    }
}

@Composable
private fun CurrentStepIndicator(
    step: SetupStep,
    visibleSteps: List<SetupStep>,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            visibleSteps.forEachIndexed { index, s ->
                StepChip(step = s, selected = (s == step), position = index, total = visibleSteps.size)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(step.friendlyName()),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

// ---------- header ----------

@Composable
private fun StepHeader(step: SetupStep, state: SetupUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.setup_progress, state.stepIndex, state.stepCount),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(step.friendlyName()),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(step.description()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
    }
}

// ---------- body ----------

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
            onAcknowledged = { _ -> onBatteryGuidanceAcknowledged() }
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(
            text = stringResource(R.string.setup_welcome_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp,
        )
        Spacer(modifier = Modifier.height(18.dp))
        WelcomePoint(
            title = stringResource(R.string.setup_welcome_point_1),
            icon = R.drawable.ic_notification,
        )
        WelcomePoint(
            title = stringResource(R.string.setup_welcome_point_2),
            icon = R.drawable.ic_notification,
        )
        WelcomePoint(
            title = stringResource(R.string.setup_welcome_point_3),
            icon = R.drawable.ic_notification,
        )
    }
}

@Composable
private fun WelcomePoint(title: String, icon: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceVariant)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icVector(icon),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NotificationAccessContent(granted: Boolean, onOpenSettings: () -> Unit) {
    Column(modifier = Modifier.padding(24.dp)) {
        StatusCard(
            title = stringResource(R.string.setup_notification_access_body),
            status = stringResource(R.string.setup_notification_access_status, if (granted) stringResource(R.string.setup_status_granted) else stringResource(R.string.setup_status_not_granted)).toString(),
            actions = {
                Button(onClick = onOpenSettings) {
                    Text(text = stringResource(R.string.setup_notification_access_open))
                }
            },
        )
    }
}

@Composable
private fun BluetoothContent(granted: Boolean, onRequest: () -> Unit) {
    Column(modifier = Modifier.padding(24.dp)) {
        StatusCard(
            title = stringResource(R.string.setup_bluetooth_body),
            status = stringResource(R.string.setup_bluetooth_status, if (granted) stringResource(R.string.setup_status_granted) else stringResource(R.string.setup_status_not_granted)).toString(),
            actions = {
                if (!granted) {
                    Button(onClick = onRequest) {
                        Text(text = stringResource(R.string.setup_bluetooth_request))
                    }
                }
            },
        )
    }
}

@Composable
private fun TextToSpeechContent(engineAvailable: Boolean, languageAvailable: Boolean, onRecheck: () -> Unit) {
    Column(modifier = Modifier.padding(24.dp)) {
        StatusCard(
            title = stringResource(R.string.setup_tts_body),
            status = buildAnnotatedString {
                append(stringResource(R.string.setup_tts_engine_status))
                append(" ")
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)) {
                    append(stringResource(if (engineAvailable) R.string.setup_status_ready else R.string.setup_status_unavailable))
                }
                append("  ·  ")
                append(stringResource(R.string.setup_tts_language_status))
                append(" ")
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)) {
                    append(stringResource(if (languageAvailable) R.string.setup_status_ready else R.string.setup_status_unavailable))
                }
            }.toString(),
            actions = {
                OutlinedButton(onClick = onRecheck) {
                    Text(text = stringResource(R.string.setup_recheck))
                }
            },
        )
    }
}

@Composable
private fun BatteryGuidanceContent(acknowledged: Boolean, onOpenBatterySettings: () -> Unit, onAcknowledged: (Boolean) -> Unit) {
    Column(modifier = Modifier.padding(24.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.setup_battery_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp,
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        SettingSwitchRow(
            label = stringResource(R.string.setup_battery_acknowledge),
            checked = acknowledged,
            onCheckedChange = onAcknowledged,
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(onClick = onOpenBatterySettings) {
            Text(text = stringResource(R.string.setup_battery_open))
        }
    }
}

@Composable
private fun AudioDevicesContent(state: SetupUiState, onAudioOutputSettingsChanged: (AudioOutputSettings) -> Unit) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(
            text = stringResource(R.string.setup_devices_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp,
        )
        Spacer(modifier = Modifier.height(16.dp))
        SettingSwitchRow(
            label = stringResource(R.string.setup_devices_allow_bluetooth),
            checked = state.audioOutputSettings.allowBluetooth,
            onCheckedChange = { onAudioOutputSettingsChanged(state.audioOutputSettings.copy(allowBluetooth = it)) },
        )
        SettingSwitchRow(
            label = stringResource(R.string.setup_devices_allow_wired),
            checked = state.audioOutputSettings.allowWired,
            onCheckedChange = { onAudioOutputSettingsChanged(state.audioOutputSettings.copy(allowWired = it)) },
        )
        SettingSwitchRow(
            label = stringResource(R.string.setup_devices_allow_speaker),
            checked = state.audioOutputSettings.allowPhoneSpeaker,
            onCheckedChange = { onAudioOutputSettingsChanged(state.audioOutputSettings.copy(allowPhoneSpeaker = it)) },
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.setup_devices_detected_header),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(6.dp))
        if (state.outputs.isEmpty()) {
            Text(
                text = stringResource(R.string.setup_devices_none_detected),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            state.outputs.forEach { output ->
                Text(
                    text = "• ${output.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TestAnnouncementContent(testState: TestAnnouncementState, onPlay: () -> Unit) {
    Column(modifier = Modifier.padding(24.dp)) {
        StatusCard(
            title = stringResource(R.string.setup_test_body),
            status = when (testState) {
                TestAnnouncementState.SPEAKING -> stringResource(R.string.setup_test_speaking).toString()
                TestAnnouncementState.COMPLETED -> stringResource(R.string.setup_test_completed).toString()
                TestAnnouncementState.FAILED -> stringResource(R.string.setup_test_failed).toString()
                TestAnnouncementState.IDLE -> ""
            },
            actions = {
                Button(
                    onClick = onPlay,
                    enabled = testState != TestAnnouncementState.SPEAKING,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (testState == TestAnnouncementState.IDLE) Primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (testState == TestAnnouncementState.IDLE) OnSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) {
                    Text(text = stringResource(R.string.setup_test_play))
                }
            },
        )
        if (testState != TestAnnouncementState.IDLE) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterStart),
                    color = Primary,
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(if (testState == TestAnnouncementState.SPEAKING) R.string.setup_test_speaking else R.string.setup_test_completed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ReviewContent(state: SetupUiState, onRequestNotificationPermission: () -> Unit) {
    Column(modifier = Modifier.padding(24.dp)) {
        ReviewRow(label = stringResource(R.string.setup_review_apps), value = stringResource(R.string.setup_apps_enabled_count, state.enabledApps.size))
        ReviewRow(
            label = stringResource(R.string.setup_review_outputs),
            value = buildAnnotatedString {
                val names = mutableListOf<String>()
                if (state.audioOutputSettings.allowBluetooth) names += stringResource(R.string.setup_devices_allow_bluetooth)
                if (state.audioOutputSettings.allowWired) names += stringResource(R.string.setup_devices_allow_wired)
                if (state.audioOutputSettings.allowPhoneSpeaker) names += stringResource(R.string.setup_devices_allow_speaker)
                append(stringResource(R.string.setup_review_outputs, names.joinToString(", ")))
            }.toString(),
        )
        val skippedTitles = mutableListOf<String>()
        for (step in state.visibleSteps) {
            if (step in state.skippedSteps) skippedTitles += stringResource(titleFor(step))
        }
        ReviewRow(
            label = stringResource(if (skippedTitles.isEmpty()) R.string.setup_review_nothing_skipped else R.string.setup_review_skipped),
            value = if (skippedTitles.isEmpty()) null else skippedTitles.joinToString(", "),
        )
        if (state.notificationPermissionRequired && !state.notificationPermissionGranted) {
            Spacer(modifier = Modifier.height(14.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = stringResource(R.string.setup_review_notification_permission_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(onClick = onRequestNotificationPermission) {
                        Text(text = stringResource(R.string.setup_review_notification_permission_button))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceVariant)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (value != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        androidx.compose.foundation.shape.CircleShape
    }
}

@Composable
private fun StatusCard(
    title: String,
    status: String,
    actions: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp,
            )
            if (status.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (status.contains("Granted", ignoreCase = true)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            actions()
        }
    }
}

@Composable
private fun SettingSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceVariant)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
            thumbContent = {
                if (checked) {
                    Icon(
                        imageVector = icVector(R.drawable.ic_notification),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                }
            },
        )
    }
}

@Composable
private fun BlockersBanner(blockers: List<SetupBlocker>) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(ErrorContainer)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icVector(R.drawable.ic_notification),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Error,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box {
                Text(
                    text = stringResource(blockerMessage(blockers.first())),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onError,
                )
                androidx.compose.animation.AnimatedVisibility(visible = blockers.size > 1) {
                    Text(
                        text = " +${blockers.size - 1}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onError,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

// ---------- navigation ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavigationBar(
    state: SetupUiState,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onStart: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.canGoBack) {
                TextButton(onClick = onBack) {
                    Text(text = stringResource(R.string.setup_back))
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            if (state.step == SetupStep.REVIEW) {
                Button(
                    onClick = onStart,
                    enabled = state.canStart,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                ) {
                    Text(text = stringResource(R.string.setup_start))
                }
            } else {
                if (state.canOnlySkip) {
                    TextButton(onClick = onSkip) {
                        Text(text = stringResource(R.string.setup_skip))
                    }
                }
                Button(
                    onClick = onNext,
                    enabled = state.canAdvance,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                ) {
                    Text(text = stringResource(R.string.setup_next))
                }
            }
        }
    }
}

// ---------- setup screen ----------

@OptIn(ExperimentalMaterial3Api::class)
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
    val context = LocalContext.current
    val lifecycleOwner = context as? LifecycleOwner

    // permission launchers created once, used across all steps
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { onPermissionsChanged() }
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { onPermissionsChanged() }

    DisposableEffect(lifecycleOwner) {
        if (lifecycleOwner == null) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) viewModelOnRefresh()
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Surface,
        bottomBar = {
            NavigationBar(
                state = state,
                onBack = onBack,
                onNext = onNext,
                onSkip = onSkip,
                onStart = onStart,
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            AnimatedStepPage(currentStep = state.step) { currentStep ->
                Column {
                    StepHeader(step = currentStep, state = state)
                    Box(modifier = Modifier.weight(1f)) {
                        if (currentStep == SetupStep.APP_SELECTION) {
                            AppSelectionContent(
                                state = state,
                                onToggleApp = onToggleApp,
                            )
                        } else {
                            StepBody(
                                state = state,
                                testAnnouncementText = stringResource(R.string.setup_test_announcement_spoken),
                                onBatteryGuidanceAcknowledged = { },
                                onAudioOutputSettingsChanged = onAudioOutputSettingsChanged,
                                onTestAnnouncement = onTestAnnouncement,
                                onOpenNotificationAccessSettings = onOpenNotificationAccessSettings,
                                onOpenBatterySettings = onOpenBatterySettings,
                                onRequestNotificationPermission = {
                                    if (notificationPermissionLauncher != null) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                                onRequestBluetoothPermission = {
                                    if (bluetoothPermissionLauncher != null) {
                                        bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                                    }
                                },
                                onPermissionsChanged = onPermissionsChanged,
                            )
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = state.blockers.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            ) {
                BlockersBanner(blockers = state.blockers)
            }
        }
    }
}

// ---------- app selection ----------


@Composable
private fun icVector(name: Int): androidx.compose.ui.graphics.vector.ImageVector = 
    androidx.compose.ui.graphics.vector.ImageVector.vectorResource(id = name)

@Composable
private fun AppSelectionContent(state: SetupUiState, onToggleApp: (String) -> Unit) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(
            text = stringResource(R.string.setup_apps_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.setup_apps_enabled_count, state.enabledApps.size),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        state.apps.forEach { app ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Switch(
                    checked = app.packageName in state.enabledApps,
                    onCheckedChange = null,
                    thumbContent = {
                        if (app.packageName in state.enabledApps) {
                            Icon(
                                imageVector = icVector(R.drawable.ic_notification),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    },
                )
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (state.appsLoading) {
            Text(
                text = stringResource(R.string.setup_apps_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (state.apps.isEmpty()) {
            Text(
                text = stringResource(R.string.setup_apps_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------- helpers ----------

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

private fun SetupStep.friendlyName(): Int = titleFor(this)

private fun SetupStep.description(): Int = when (this) {
    SetupStep.WELCOME -> R.string.setup_welcome_body
    SetupStep.NOTIFICATION_ACCESS -> R.string.setup_notification_access_body
    SetupStep.BLUETOOTH_PERMISSION -> R.string.setup_bluetooth_body
    SetupStep.TEXT_TO_SPEECH -> R.string.setup_tts_body
    SetupStep.BATTERY_GUIDANCE -> R.string.setup_battery_body
    SetupStep.APP_SELECTION -> R.string.setup_apps_body
    SetupStep.AUDIO_DEVICES -> R.string.setup_devices_body
    SetupStep.TEST_ANNOUNCEMENT -> R.string.setup_test_body
    SetupStep.REVIEW -> R.string.setup_review_body
}

private fun blockerMessage(blocker: SetupBlocker): Int = when (blocker) {
    is SetupBlocker.NotificationAccessMissing -> R.string.setup_blocker_notification_access
    is SetupBlocker.BluetoothPermissionMissing -> R.string.setup_blocker_bluetooth
    is SetupBlocker.TtsEngineUnavailable -> R.string.setup_blocker_tts_engine
    is SetupBlocker.TtsLanguageMissing -> R.string.setup_blocker_tts_language
    is SetupBlocker.NoEnabledApps -> R.string.setup_blocker_no_apps
    is SetupBlocker.NoAllowedOutput -> R.string.setup_blocker_no_output
}

// placeholder for the refresh action (defined by the ViewModel)
private fun viewModelOnRefresh() {
    // Refreshed by the SetupViewModel on ON_RESUME.
}
