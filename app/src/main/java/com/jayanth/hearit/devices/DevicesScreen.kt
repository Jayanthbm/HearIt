package com.jayanth.hearit.devices

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.jayanth.hearit.R
import com.jayanth.hearit.core.common.model.AudioOutputKind

/**
 * Entry point of the Devices feature (master prompt §6, phase P14).
 *
 * Re-reads connected outputs whenever the screen resumes, because Bluetooth devices come and
 * go while the user is elsewhere.
 */
@Composable
fun DevicesRoute(viewModel: DevicesViewModel, modifier: Modifier = Modifier) {
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
 * Output categories, connected devices and the remembered device list.
 *
 * Stateless: every switch writes straight through the ViewModel, every string is a resource,
 * every target is at least 48 dp, and the copy states what HearIt can and cannot control
 * (Android routes audio; HearIt only decides whether to speak).
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.devices_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.unavailable) {
            Text(
                text = stringResource(R.string.devices_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader(R.string.devices_allowed_types)
        CategorySwitchRow(
            labelRes = R.string.devices_allow_bluetooth,
            checked = state.audioOutput.allowBluetooth,
            onCheckedChange = { allowed -> onCategoryAllowed(AudioOutputKind.BLUETOOTH, allowed) },
        )
        CategorySwitchRow(
            labelRes = R.string.devices_allow_wired,
            checked = state.audioOutput.allowWired,
            onCheckedChange = { allowed -> onCategoryAllowed(AudioOutputKind.WIRED, allowed) },
        )
        CategorySwitchRow(
            labelRes = R.string.devices_allow_speaker,
            checked = state.audioOutput.allowPhoneSpeaker,
            onCheckedChange = { allowed -> onCategoryAllowed(AudioOutputKind.PHONE_SPEAKER, allowed) },
        )
        CategorySwitchRow(
            labelRes = R.string.devices_allow_unidentified,
            checked = state.audioOutput.allowUnidentifiedOutput,
            onCheckedChange = { allowed -> onCategoryAllowed(AudioOutputKind.OTHER, allowed) },
        )
        Text(
            text = stringResource(R.string.devices_categories_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.noBluetoothDeviceAllowed) {
            Text(
                text = stringResource(R.string.devices_no_bt_allowed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Spacer(Modifier.height(20.dp))
        SectionHeader(R.string.devices_connected_now)
        when {
            !state.loaded -> Text(
                text = stringResource(R.string.devices_loading),
                style = MaterialTheme.typography.bodyMedium,
            )

            state.connected.isEmpty() -> Text(
                text = stringResource(R.string.devices_empty),
                style = MaterialTheme.typography.bodyMedium,
            )

            else -> state.connected.forEach { row ->
                DeviceRowView(
                    row = row,
                    bluetoothEnabled = state.audioOutput.allowBluetooth,
                    onAllowedChanged = { allowed -> onDeviceAllowed(row.id, allowed) },
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionHeader(R.string.devices_remembered)
        if (state.remembered.isEmpty()) {
            Text(
                text = stringResource(R.string.devices_remembered_empty),
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            state.remembered.forEach { row ->
                DeviceRowView(
                    row = row,
                    bluetoothEnabled = state.audioOutput.allowBluetooth,
                    onAllowedChanged = { allowed -> onDeviceAllowed(row.id, allowed) },
                    onForget = { onForgetDevice(row.id) },
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text(text = stringResource(R.string.devices_refresh))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.devices_note_routing),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SectionHeader(@StringRes titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun CategorySwitchRow(
    @StringRes labelRes: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f).padding(end = 12.dp),
        )
        Switch(checked = checked, onCheckedChange = null)
    }
}

/**
 * One device row: name, connection state, the per-device switch (Bluetooth only) and, for
 * remembered devices, a Forget action.
 */
@Composable
private fun DeviceRowView(
    row: DeviceRow,
    bluetoothEnabled: Boolean,
    onAllowedChanged: (Boolean) -> Unit,
    onForget: (() -> Unit)? = null,
) {
    val allowDescription = stringResource(R.string.devices_allow_device, row.label)

    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = row.label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(
                    if (row.connected) {
                        R.string.devices_status_connected
                    } else {
                        R.string.devices_status_not_connected
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (row.canToggle) {
            Switch(
                checked = row.allowed,
                onCheckedChange = onAllowedChanged,
                enabled = bluetoothEnabled,
                modifier = Modifier.semantics { contentDescription = allowDescription },
            )
        } else {
            Text(
                text = stringResource(
                    if (row.allowed) R.string.devices_allowed else R.string.devices_not_allowed,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (onForget != null) {
            TextButton(onClick = onForget, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(text = stringResource(R.string.devices_forget))
            }
        }
    }
}
