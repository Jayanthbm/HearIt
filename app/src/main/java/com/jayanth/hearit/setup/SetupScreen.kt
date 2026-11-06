package com.jayanth.hearit.setup

import android.Manifest
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Cable
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.jayanth.hearit.R
import com.jayanth.hearit.core.common.model.AudioOutputSettings
import com.jayanth.hearit.core.setup.SetupBlocker
import com.jayanth.hearit.core.setup.SetupStep

/**
 * Modern Material 3 Expressive Setup Wizard.
 *
 * Guides the user through first-launch prerequisites with high-contrast accessibility,
 * fluid step transitions, clear status badges, and comfortable touch targets.
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
    modifier: Modifier = Modifier,
) {
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { onPermissionsChanged() },
    )

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { onPermissionsChanged() },
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            ModernBottomBar(
                state = state,
                onBack = onBack,
                onNext = onNext,
                onSkip = onSkip,
                onStart = onStart,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Modern Stepper Header with animated progress
            SetupHeader(state = state)

            // Step Content area with smooth fade/scale transition
            AnimatedContent(
                targetState = state.step,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                },
                label = "step_content",
                modifier = Modifier.weight(1f),
            ) { currentStep ->
                when (currentStep) {
                    SetupStep.WELCOME -> WelcomeStep()
                    SetupStep.NOTIFICATION_ACCESS -> NotificationAccessStep(
                        granted = state.prerequisites.notificationAccessGranted,
                        onOpenSettings = onOpenNotificationAccessSettings,
                    )
                    SetupStep.BLUETOOTH_PERMISSION -> BluetoothStep(
                        granted = state.prerequisites.bluetoothPermissionGranted,
                        onRequest = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                            }
                        },
                    )
                    SetupStep.TEXT_TO_SPEECH -> TextToSpeechStep(
                        engineAvailable = state.prerequisites.ttsEngineAvailable,
                        languageAvailable = state.prerequisites.ttsLanguageAvailable,
                        onRecheck = onPermissionsChanged,
                    )
                    SetupStep.BATTERY_GUIDANCE -> BatteryGuidanceStep(
                        acknowledged = state.batteryGuidanceAcknowledged,
                        onOpenBatterySettings = onOpenBatterySettings,
                        onAcknowledge = onBatteryGuidanceAcknowledged,
                    )
                    SetupStep.APP_SELECTION -> AppSelectionStep(
                        apps = state.apps,
                        loading = state.appsLoading,
                        enabledApps = state.enabledApps,
                        onToggleApp = onToggleApp,
                    )
                    SetupStep.AUDIO_DEVICES -> AudioDevicesStep(
                        settings = state.audioOutputSettings,
                        outputs = state.outputs,
                        onSettingsChanged = onAudioOutputSettingsChanged,
                    )
                    SetupStep.TEST_ANNOUNCEMENT -> TestAnnouncementStep(
                        testState = state.testAnnouncement,
                        onPlay = {
                            onTestAnnouncement("This is a test announcement from Hear It.")
                        },
                    )
                    SetupStep.REVIEW -> ReviewStep(
                        state = state,
                        onRequestNotificationPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                    )
                }
            }
        }
    }
}

// ============================================================================
// Header & Progress Indicator
// ============================================================================

@Composable
private fun SetupHeader(state: SetupUiState) {
    val progress = (state.stepIndex.toFloat() / state.stepCount.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        // Step chip + Progress percentage
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.setup_progress, state.stepIndex, state.stepCount).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp,
                    )
                }
            }

            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Material 3 Expressive wavy progress indicator
        ExpressiveWavyProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Step Title (Sleek one-liner)
        Text(
            text = stringResource(titleFor(state.step)),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(3.dp))

        // Step Subtitle (Gracefully wrapped)
        Text(
            text = stringResource(descriptionFor(state.step)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            lineHeight = 20.sp,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ============================================================================
// Material 3 Expressive Wavy Progress Indicator
// ============================================================================

@Composable
private fun ExpressiveWavyProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    strokeWidth: androidx.compose.ui.unit.Dp = 5.dp,
    waveAmplitude: androidx.compose.ui.unit.Dp = 2.5.dp,
    wavelength: androidx.compose.ui.unit.Dp = 20.dp,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "wavyProgressAnim",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "waveInfinite")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wavePhase",
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp),
    ) {
        val width = size.width
        val height = size.height
        val midY = height / 2f
        val strokePx = strokeWidth.toPx()
        val ampPx = waveAmplitude.toPx()
        val waveLenPx = wavelength.toPx().coerceAtLeast(10f)

        val activeWidth = width * animatedProgress
        val gapPx = 6.dp.toPx()

        // 1. Inactive track: straight line with rounded cap
        val trackStart = if (animatedProgress > 0f) (activeWidth + gapPx).coerceAtMost(width) else 0f
        if (trackStart < width) {
            drawLine(
                color = trackColor,
                start = Offset(trackStart, midY),
                end = Offset(width, midY),
                strokeWidth = strokePx * 0.75f,
                cap = StrokeCap.Round,
            )
        }

        // 2. Active wave path: sinusoidal curve
        if (activeWidth > 2f) {
            val wavePath = Path().apply {
                moveTo(0f, midY)
                var x = 0f
                val step = 2f
                while (x <= activeWidth) {
                    val progressRatio = (x / activeWidth).coerceIn(0f, 1f)
                    val envelope = Math.sin(progressRatio * Math.PI).toFloat()
                    val currentAmp = ampPx * (0.35f + 0.65f * envelope)
                    val angle = (2f * Math.PI.toFloat() * (x / waveLenPx)) - phase
                    val y = midY + currentAmp * Math.sin(angle.toDouble()).toFloat()
                    lineTo(x, y)
                    x += step
                }
            }
            drawPath(
                path = wavePath,
                color = color,
                style = Stroke(
                    width = strokePx,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }
    }
}

// ============================================================================
// Step 1: Welcome Step (Material 3 Expressive Morphed & Compact)
// ============================================================================

@Composable
private fun WelcomeStep() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Compact expressive brand emblem
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.size(62.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                ),
                shadowElevation = 4.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_logo_audio_lines),
                        contentDescription = stringResource(R.string.app_name),
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        FeatureCard(
            icon = Icons.Rounded.Headphones,
            iconTint = MaterialTheme.colorScheme.primary,
            iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
            title = "Earphone-First Audio",
            description = stringResource(R.string.setup_welcome_point_1),
            shape = RoundedCornerShape(20.dp),
            iconShape = RoundedCornerShape(12.dp),
        )

        FeatureCard(
            icon = Icons.Rounded.Tune,
            iconTint = MaterialTheme.colorScheme.secondary,
            iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            title = "Smart Eligibility Rules",
            description = stringResource(R.string.setup_welcome_point_2),
            shape = RoundedCornerShape(20.dp),
            iconShape = RoundedCornerShape(12.dp),
        )

        FeatureCard(
            icon = Icons.Rounded.Security,
            iconTint = MaterialTheme.colorScheme.tertiary,
            iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
            title = "100% Private & Local",
            description = stringResource(R.string.setup_welcome_point_3),
            shape = RoundedCornerShape(20.dp),
            iconShape = RoundedCornerShape(12.dp),
        )
    }
}

@Composable
private fun FeatureCard(
    icon: ImageVector,
    iconTint: Color,
    iconContainerColor: Color,
    title: String,
    description: String,
    shape: Shape = RoundedCornerShape(18.dp),
    iconShape: Shape = RoundedCornerShape(12.dp),
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = iconShape,
                color = iconContainerColor,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ============================================================================
// Step 2: Notification Access Step
// ============================================================================

@Composable
private fun NotificationAccessStep(
    granted: Boolean,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PermissionCard(
            title = "Notification Listener",
            description = "Allows Hear It to detect incoming notifications from apps you choose.",
            granted = granted,
            icon = Icons.Rounded.NotificationsActive,
            actionLabel = stringResource(R.string.setup_notification_access_open),
            onAction = onOpenSettings,
        )

        if (granted) {
            StatusBanner(
                isSuccess = true,
                message = "Access is granted! Hear It is ready to listen to notifications.",
            )
        } else {
            StatusBanner(
                isSuccess = false,
                message = "Tap the button above to enable 'Hear It' in system notification access.",
            )
        }
    }
}

// ============================================================================
// Step 3: Bluetooth Step
// ============================================================================

@Composable
private fun BluetoothStep(
    granted: Boolean,
    onRequest: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PermissionCard(
            title = "Nearby Bluetooth Devices",
            description = stringResource(R.string.setup_bluetooth_body),
            granted = granted,
            icon = Icons.Rounded.Bluetooth,
            actionLabel = stringResource(R.string.setup_bluetooth_request),
            onAction = onRequest,
        )

        if (granted) {
            StatusBanner(
                isSuccess = true,
                message = "Bluetooth permission is active. Headphone routing is ready.",
            )
        } else {
            StatusBanner(
                isSuccess = false,
                message = "Optional: If you only use wired earphones, you can safely skip this step.",
            )
        }
    }
}

// ============================================================================
// Step 4: Text-To-Speech Step
// ============================================================================

@Composable
private fun TextToSpeechStep(
    engineAvailable: Boolean,
    languageAvailable: Boolean,
    onRecheck: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TtsProbeCard(
            icon = Icons.Rounded.RecordVoiceOver,
            title = "Speech Synthesis Engine",
            ready = engineAvailable,
            description = if (engineAvailable) "TTS engine is active and ready." else "No text-to-speech engine found on this device.",
        )

        TtsProbeCard(
            icon = Icons.Rounded.Language,
            title = "Device Language Voice",
            ready = languageAvailable,
            description = if (languageAvailable) "Voice data is installed for your language." else "Missing voice files. Install language pack in system TTS settings.",
        )

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedButton(
            onClick = onRecheck,
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.setup_recheck), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun TtsProbeCard(
    icon: ImageVector,
    title: String,
    ready: Boolean,
    description: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (ready) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                    )
                    StatusChip(isSuccess = ready, label = if (ready) "Ready" else "Unavailable")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ============================================================================
// Step 5: Battery Guidance Step
// ============================================================================

@Composable
private fun BatteryGuidanceStep(
    acknowledged: Boolean,
    onOpenBatterySettings: () -> Unit,
    onAcknowledge: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
            ),
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.BatteryChargingFull,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Background Optimization",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.setup_battery_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onOpenBatterySettings,
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    ),
                ) {
                    Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.setup_battery_open), fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Acknowledgement Checkbox Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onAcknowledge() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (acknowledged) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceContainer,
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (acknowledged) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(24.dp),
                    shape = CircleShape,
                    color = if (acknowledged) MaterialTheme.colorScheme.primary else Color.Transparent,
                    border = if (acknowledged) null else androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                ) {
                    if (acknowledged) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = stringResource(R.string.setup_battery_acknowledge),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (acknowledged) FontWeight.Medium else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

// ============================================================================
// Step 6: App Selection Step
// ============================================================================

@Composable
private fun AppSelectionStep(
    apps: List<InstalledApp>,
    loading: Boolean,
    enabledApps: Set<String>,
    onToggleApp: (String) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) apps
        else apps.filter { it.label.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search installed apps…") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Text(
                    text = stringResource(R.string.setup_apps_enabled_count, enabledApps.size),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }

            Text(
                text = "${filteredApps.size} apps found",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(36.dp))
            }
        } else if (filteredApps.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.setup_apps_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    val isEnabled = app.packageName in enabledApps
                    AppRow(
                        app = app,
                        enabled = isEnabled,
                        onToggle = { onToggleApp(app.packageName) },
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun SetupAppIcon(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var iconBitmap by remember(packageName) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(packageName) {
        withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager.getApplicationIcon(packageName)
                    .toBitmap(96, 96, Bitmap.Config.ARGB_8888)
                    .asImageBitmap()
            }.getOrNull()
        }.also {
            iconBitmap = it
        }
    }

    val bitmap = iconBitmap
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp)),
        )
    } else {
        Surface(
            modifier = modifier.size(38.dp),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AppRow(
    app: InstalledApp,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainer,
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SetupAppIcon(packageName = app.packageName)

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (enabled) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp,
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Switch(
                checked = enabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}

// ============================================================================
// Step 7: Audio Devices Step
// ============================================================================

@Composable
private fun AudioDevicesStep(
    settings: AudioOutputSettings,
    outputs: List<DetectedOutput>,
    onSettingsChanged: (AudioOutputSettings) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Bluetooth
        DeviceToggleCard(
            icon = Icons.Rounded.Headphones,
            title = stringResource(R.string.setup_devices_allow_bluetooth),
            subtitle = if (outputs.any { it.isBluetooth }) "Connected: ${outputs.first { it.isBluetooth }.label}" else "Active when Bluetooth headphones connect",
            checked = settings.allowBluetooth,
            isRecommended = true,
            onCheckedChange = { onSettingsChanged(settings.copy(allowBluetooth = it)) },
        )

        // Wired
        DeviceToggleCard(
            icon = Icons.Rounded.Cable,
            title = stringResource(R.string.setup_devices_allow_wired),
            subtitle = "Active when standard 3.5mm or USB-C earphones plug in",
            checked = settings.allowWired,
            isRecommended = true,
            onCheckedChange = { onSettingsChanged(settings.copy(allowWired = it)) },
        )

        // Phone Speaker
        DeviceToggleCard(
            icon = Icons.AutoMirrored.Rounded.VolumeUp,
            title = stringResource(R.string.setup_devices_allow_speaker),
            subtitle = "Announces notifications aloud through the phone's loudspeaker",
            checked = settings.allowPhoneSpeaker,
            isRecommended = false,
            onCheckedChange = { onSettingsChanged(settings.copy(allowPhoneSpeaker = it)) },
        )
    }
}

@Composable
private fun DeviceToggleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    isRecommended: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (checked) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (isRecommended) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                        ) {
                            Text(
                                text = "Default",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                maxLines = 1,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}

// ============================================================================
// Step 8: Test Announcement Step
// ============================================================================

@Composable
private fun TestAnnouncementStep(
    testState: TestAnnouncementState,
    onPlay: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
            ),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "“This is a test announcement from Hear It.”",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.setup_test_card_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onPlay,
                    enabled = testState != TestAnnouncementState.SPEAKING,
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    if (testState == TestAnnouncementState.SPEAKING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(stringResource(R.string.setup_test_speaking), fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.setup_test_play), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        when (testState) {
            TestAnnouncementState.COMPLETED -> StatusBanner(
                isSuccess = true,
                message = stringResource(R.string.setup_test_completed),
            )
            TestAnnouncementState.FAILED -> StatusBanner(
                isSuccess = false,
                message = stringResource(R.string.setup_test_failed),
            )
            else -> Unit
        }
    }
}

// ============================================================================
// Step 9: Review & Start Step
// ============================================================================

@Composable
private fun ReviewStep(
    state: SetupUiState,
    onRequestNotificationPermission: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
            ),
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Configuration Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(14.dp))

                ReviewRow(
                    icon = Icons.Rounded.Notifications,
                    label = "Enabled Applications",
                    value = if (state.enabledApps.isEmpty()) "None (Set in Apps)" else "${state.enabledApps.size} apps selected",
                    isWarning = state.enabledApps.isEmpty(),
                )

                Spacer(modifier = Modifier.height(10.dp))

                val outputSummary = buildList {
                    if (state.audioOutputSettings.allowBluetooth) add("Bluetooth")
                    if (state.audioOutputSettings.allowWired) add("Wired")
                    if (state.audioOutputSettings.allowPhoneSpeaker) add("Speaker")
                }.joinToString(", ").ifEmpty { "None" }

                ReviewRow(
                    icon = Icons.Rounded.Headphones,
                    label = "Allowed Outputs",
                    value = outputSummary,
                )

                Spacer(modifier = Modifier.height(10.dp))

                ReviewRow(
                    icon = Icons.Rounded.Lock,
                    label = "Privacy Guard",
                    value = "100% On-Device",
                )
            }
        }

        // Notification permission for API 33+ if required
        if (state.notificationPermissionRequired && !state.notificationPermissionGranted) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Service Status Notification",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.setup_review_notification_permission_body),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    FilledTonalButton(
                        onClick = onRequestNotificationPermission,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 42.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.setup_review_notification_permission_button),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }

        if (state.startBlockers.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.startBlockers.forEach { blocker ->
                    StatusBanner(isSuccess = false, message = stringResource(blockerMessage(blocker)))
                }
            }
        } else {
            val successMessage = if (state.enabledApps.isEmpty()) {
                "Hear It is ready! You can enable apps anytime from the Apps tab."
            } else {
                "All prerequisites satisfied! Press Start below to begin reading notifications."
            }
            StatusBanner(
                isSuccess = true,
                message = successMessage,
            )
        }
    }
}

@Composable
private fun ReviewRow(
    icon: ImageVector,
    label: String,
    value: String,
    isWarning: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(34.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isWarning) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            },
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isWarning) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ============================================================================
// Shared Reusable UI Components (Cards, Status Chips, Banners)
// ============================================================================

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    granted: Boolean,
    icon: ImageVector,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (granted) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = if (granted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                StatusChip(isSuccess = granted, label = if (granted) "Granted" else "Action Required")
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp,
            )

            if (!granted) {
                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = actionLabel,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(isSuccess: Boolean, label: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSuccess) Color(0xFF064E3B) else Color(0xFF78350F),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSuccess) Color(0xFF10B981) else Color(0xFFF59E0B),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isSuccess) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                contentDescription = null,
                tint = if (isSuccess) Color(0xFF34D399) else Color(0xFFFBBF24),
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSuccess) Color(0xFF34D399) else Color(0xFFFBBF24),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun StatusBanner(isSuccess: Boolean, message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (isSuccess) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFF78350F).copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSuccess) Color(0xFF10B981).copy(alpha = 0.4f) else Color(0xFFF59E0B).copy(alpha = 0.4f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isSuccess) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                contentDescription = null,
                tint = if (isSuccess) Color(0xFF34D399) else Color(0xFFFBBF24),
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = if (isSuccess) Color(0xFFD1FAE5) else Color(0xFFFEF3C7),
                lineHeight = 18.sp,
            )
        }
    }
}

// ============================================================================
// Modern Expressive Bottom Navigation Bar
// ============================================================================

@Composable
private fun ModernBottomBar(
    state: SetupUiState,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onStart: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Back Button
            if (state.canGoBack) {
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.setup_back),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.setup_back),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                val isAppSelection = state.step == SetupStep.APP_SELECTION
                val noAppsEnabled = isAppSelection && state.enabledApps.isEmpty()

                // Skip Button for skippable steps (when not handled by primary action)
                if ((state.canOnlySkip || (state.step.isSkippable && !state.canAdvance)) && !noAppsEnabled) {
                    TextButton(
                        onClick = onSkip,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.setup_skip),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                val morphedCtaShape = RoundedCornerShape(16.dp)

                // CTA Button (Continue, Start, or Choose Later)
                if (state.step == SetupStep.REVIEW) {
                    Button(
                        onClick = onStart,
                        enabled = state.canStart,
                        shape = morphedCtaShape,
                        modifier = Modifier.defaultMinSize(minHeight = 52.dp),
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RocketLaunch,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.setup_start),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                    }
                } else if (noAppsEnabled) {
                    // On App Selection with 0 apps, the primary CTA is "Choose Later"
                    Button(
                        onClick = onSkip,
                        shape = morphedCtaShape,
                        modifier = Modifier.defaultMinSize(minHeight = 52.dp),
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(
                            text = "Choose Later",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                } else {
                    Button(
                        onClick = onNext,
                        enabled = state.canAdvance,
                        shape = morphedCtaShape,
                        modifier = Modifier.defaultMinSize(minHeight = 52.dp),
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(
                            text = if (state.step == SetupStep.WELCOME) "Get Started" else stringResource(R.string.setup_next),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// String Resource Helpers
// ============================================================================

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
private fun descriptionFor(step: SetupStep): Int = when (step) {
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
