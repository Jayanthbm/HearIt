package com.jayanth.hearit.devices

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.Cable
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DevicesOther
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jayanth.hearit.wear.WearCompanionState
import com.jayanth.hearit.wear.WearDataLayerSync
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.jayanth.hearit.R
import com.jayanth.hearit.core.common.model.AudioOutputKind

/**
 * Entry point of the Devices feature (master prompt §6, phase P14).
 *
 * Re-reads connected outputs whenever the screen resumes.
 */
@Composable
fun DevicesRoute(viewModel: DevicesViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = context as? LifecycleOwner

    DisposableEffect(lifecycleOwner, context) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(c: android.content.Context?, intent: android.content.Intent?) {
                viewModel.onRefresh()
            }
        }
        val filter = android.content.IntentFilter().apply {
            addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            addAction(android.media.AudioManager.ACTION_HEADSET_PLUG)
        }
        context.registerReceiver(receiver, filter)

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onRefresh()
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
            lifecycleOwner?.lifecycle?.removeObserver(observer)
        }
    }

    DevicesScreen(
        state = state,
        onRefresh = viewModel::onRefresh,
        onCategoryAllowed = viewModel::onCategoryAllowed,
        onDeviceAllowed = viewModel::onDeviceAllowed,
        onForgetDevice = viewModel::onForgetDevice,
        modifier = modifier,
    )
}

/**
 * Modern Material 3 Expressive Devices Screen.
 */
@Composable
fun DevicesScreen(
    state: DevicesUiState,
    onRefresh: () -> Unit,
    onCategoryAllowed: (AudioOutputKind, Boolean) -> Unit,
    onDeviceAllowed: (String, Boolean) -> Unit,
    onForgetDevice: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.unavailable) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.devices_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }

        // Section: Wear OS Smartwatch Companion
        WearOsCompanionCard()

        // Section: Allowed Output Types
        Text(
            text = "Allowed Output Types",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        OutputCategoryCard(
            icon = Icons.Rounded.Headphones,
            title = stringResource(R.string.devices_allow_bluetooth),
            subtitle = "Wireless Bluetooth headsets & earbuds",
            checked = state.audioOutput.allowBluetooth,
            onCheckedChange = { onCategoryAllowed(AudioOutputKind.BLUETOOTH, it) },
        )

        OutputCategoryCard(
            icon = Icons.Rounded.Cable,
            title = stringResource(R.string.devices_allow_wired),
            subtitle = "3.5mm headphone jack or USB-C earphones",
            checked = state.audioOutput.allowWired,
            onCheckedChange = { onCategoryAllowed(AudioOutputKind.WIRED, it) },
        )

        OutputCategoryCard(
            icon = Icons.AutoMirrored.Rounded.VolumeUp,
            title = stringResource(R.string.devices_allow_speaker),
            subtitle = "Phone loudspeaker (Disabled by default)",
            checked = state.audioOutput.allowPhoneSpeaker,
            onCheckedChange = { onCategoryAllowed(AudioOutputKind.PHONE_SPEAKER, it) },
        )

        OutputCategoryCard(
            icon = Icons.Rounded.DevicesOther,
            title = stringResource(R.string.devices_allow_unidentified),
            subtitle = "Fallback for unclassified or OEM audio accessories",
            checked = state.audioOutput.allowUnidentifiedOutput,
            onCheckedChange = { onCategoryAllowed(AudioOutputKind.OTHER, it) },
        )

        if (state.noBluetoothDeviceAllowed) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
            ) {
                Text(
                    text = stringResource(R.string.devices_no_bt_allowed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }

        // Section: Connected Now
        Text(
            text = "Connected Now",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        when {
            !state.loaded -> {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
            }

            state.connected.isEmpty() -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Headphones,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Audio Output Connected",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.devices_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                        )
                    }
                }
            }

            else -> {
                state.connected.forEach { row ->
                    ConnectedDeviceCard(
                        row = row,
                        bluetoothEnabled = state.audioOutput.allowBluetooth,
                        onAllowedChanged = { onDeviceAllowed(row.id, it) },
                    )
                }
            }
        }

        // Section: Remembered Devices
        Text(
            text = "Remembered Devices",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        if (state.remembered.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Bluetooth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No Remembered Devices",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.devices_remembered_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                    )
                }
            }
        } else {
            state.remembered.forEach { row ->
                RememberedDeviceCard(
                    row = row,
                    bluetoothEnabled = state.audioOutput.allowBluetooth,
                    onAllowedChanged = { onDeviceAllowed(row.id, it) },
                    onForget = { onForgetDevice(row.id) },
                )
            }
        }

        // Refresh Action Button
        OutlinedButton(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.devices_refresh), fontWeight = FontWeight.SemiBold)
        }

        Text(
            text = "Android routes audio to the active output. Hear It decides whether to speak.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun OutputCategoryCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        shape = RoundedCornerShape(20.dp),
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
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
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

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    lineHeight = 18.sp,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
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

private fun getAudioDeviceIcon(label: String, isBluetooth: Boolean): ImageVector {
    if (!isBluetooth) return Icons.Rounded.Cable
    val lower = label.lowercase()
    return when {
        lower.contains("speaker") || lower.contains("soundbar") || lower.contains("audio") || lower.contains("echo") || lower.contains("nest") -> Icons.AutoMirrored.Rounded.VolumeUp
        else -> Icons.Rounded.Headphones
    }
}

@Composable
private fun ConnectedDeviceCard(
    row: DeviceRow,
    bluetoothEnabled: Boolean,
    onAllowedChanged: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF064E3B),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = getAudioDeviceIcon(row.label, row.isBluetooth),
                        contentDescription = null,
                        tint = Color(0xFF34D399),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = row.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        lineHeight = 18.sp,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981)),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Active",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (row.allowed) "Allowed for announcements" else "Muted for announcements",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (row.isBluetooth && row.canToggle) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = {
                        try {
                            android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 70)
                                .startTone(android.media.ToneGenerator.TONE_PROP_BEEP2, 250)
                        } catch (_: Throwable) {}
                    },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = "Test Audio on this device",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Switch(
                    checked = row.allowed,
                    enabled = bluetoothEnabled,
                    onCheckedChange = onAllowedChanged,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
        }
    }
}

@Composable
private fun RememberedDeviceCard(
    row: DeviceRow,
    bluetoothEnabled: Boolean,
    onAllowedChanged: (Boolean) -> Unit,
    onForget: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = if (row.allowed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = getAudioDeviceIcon(row.label, row.isBluetooth),
                            contentDescription = null,
                            tint = if (row.allowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = row.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        lineHeight = 18.sp,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (row.allowed) "Allowed for announcements" else "Muted for announcements",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                    )
                }

                if (row.isBluetooth && row.canToggle) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = row.allowed,
                        enabled = bluetoothEnabled,
                        onCheckedChange = onAllowedChanged,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Previously paired",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                )

                TextButton(
                    onClick = onForget,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Forget",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
fun WearOsCompanionCard(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val sessionStore = remember(context) { com.jayanth.hearit.service.SessionStore(context) }
    var companionState by remember {
        mutableStateOf(
            WearCompanionState(
                watchFilterApps = sessionStore.watchFilterApps,
                isForwardingEnabled = sessionStore.isWatchForwardingEnabled,
                isHapticsEnabled = sessionStore.isWatchHapticsEnabled,
            ),
        )
    }
    var isSyncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(true) }
    var showWatchAppsPicker by remember { mutableStateOf(false) }

    val refreshStatus: () -> Unit = {
        WearDataLayerSync.checkCompanionStatus(context) { state ->
            companionState = state
        }
    }

    val triggerConfigSync: () -> Unit = {
        isSyncing = true
        syncMessage = null
        WearDataLayerSync.syncAllNow(context) { success, msg ->
            isSyncing = false
            isSuccess = success
            syncMessage = msg
            refreshStatus()
        }
    }

    LaunchedEffect(Unit) {
        refreshStatus()
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (companionState.isConnected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (companionState.isConnected)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.DevicesOther,
                                contentDescription = null,
                                tint = if (companionState.isConnected)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Wear OS Companion",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (companionState.isConnected) Color(0xFF4CAF50)
                                        else Color(0xFF9E9E9E)
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (companionState.isConnected)
                                    "Connected"
                                else "No watch connected",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = if (companionState.isConnected)
                                    Color(0xFF2E7D32)
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { refreshStatus() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Check connection",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 0.5.dp
            )

            // Status details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Last Synced",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = if (companionState.lastSyncTimestamp > 0L) {
                            val diff = System.currentTimeMillis() - companionState.lastSyncTimestamp
                            when {
                                diff < 60_000 -> "Just now"
                                diff < 3600_000 -> "${diff / 60_000}m ago"
                                else -> "${diff / 3600_000}h ago"
                            }
                        } else "Never",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Watch Apps Selected",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${companionState.watchFilterApps.size} / 4 apps",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                thickness = 0.5.dp,
            )

            // Master Forwarding Switch (#35 Option A)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                    Text(
                        text = "Watch Notification Forwarding",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Forward up to 4 selected apps independently of phone speech",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = companionState.isForwardingEnabled,
                    onCheckedChange = { checked ->
                        sessionStore.isWatchForwardingEnabled = checked
                        companionState = companionState.copy(isForwardingEnabled = checked)
                        triggerConfigSync()
                    },
                )
            }

            // Watch Haptic Pulse Switch (#35)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                    Text(
                        text = "Watch Haptic Pulse",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Gently vibrate watch when a mirrored alert arrives",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = companionState.isHapticsEnabled,
                    enabled = companionState.isForwardingEnabled,
                    onCheckedChange = { checked ->
                        sessionStore.isWatchHapticsEnabled = checked
                        companionState = companionState.copy(isHapticsEnabled = checked)
                        triggerConfigSync()
                    },
                )
            }

            // Max-4 Watch Apps Config Button (#35)
            OutlinedButton(
                onClick = { showWatchAppsPicker = true },
                enabled = companionState.isForwardingEnabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Configure Watch Apps (${companionState.watchFilterApps.size} / 4)")
            }

            if (syncMessage != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSuccess) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = syncMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSuccess) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }

            // Sync button
            OutlinedButton(
                onClick = triggerConfigSync,
                enabled = !isSyncing,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Syncing to Watch...")
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sync Watch Config & Icons")
                }
            }
        }
    }

    if (showWatchAppsPicker) {
        WearWatchAppsPickerDialog(
            initialSelected = companionState.watchFilterApps,
            onSave = { picked ->
                sessionStore.watchFilterApps = picked
                companionState = companionState.copy(watchFilterApps = picked)
                showWatchAppsPicker = false
                triggerConfigSync()
            },
            onDismiss = { showWatchAppsPicker = false },
        )
    }
}
