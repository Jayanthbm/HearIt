package com.jayanth.hearit.settings

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * App Security: app lock and screenshot protection. Split out of Playback & Gestures, which
 * had no business owning device security.
 */
@Composable
fun SecurityRoute(
    viewModel: GeneralSettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val activity = LocalContext.current as? Activity

    SecurityScreen(
        biometricLockEnabled = state.security.biometricLockEnabled,
        secureScreenEnabled = state.security.secureScreenEnabled,
        isBiometricAvailable = state.isBiometricAvailable,
        onToggleBiometric = {
            activity?.let {
                viewModel.onToggleBiometricLock(it, !state.security.biometricLockEnabled)
            }
        },
        onToggleSecureScreen = viewModel::onToggleSecureScreen,
        modifier = modifier,
    )
}

@Composable
fun SecurityScreen(
    biometricLockEnabled: Boolean,
    secureScreenEnabled: Boolean,
    isBiometricAvailable: Boolean,
    onToggleBiometric: () -> Unit,
    onToggleSecureScreen: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsCard(
            title = "App Lock",
            subtitle = "Require authentication to open HearIt",
            icon = Icons.Rounded.Lock,
        ) {
            SecuritySwitchRow(
                title = "Biometric or device PIN",
                subtitle = if (isBiometricAvailable) {
                    "Uses your device lock to unlock the app"
                } else {
                    "Set up a PIN, pattern or biometrics in Android settings first"
                },
                checked = biometricLockEnabled,
                enabled = isBiometricAvailable || biometricLockEnabled,
                isError = !isBiometricAvailable,
                onCheckedChange = { onToggleBiometric() },
            )
        }

        SettingsCard(
            title = "Screen Privacy",
            subtitle = "Hide app content from screenshots and recents",
            icon = Icons.Rounded.Security,
        ) {
            SecuritySwitchRow(
                title = "Secure screen",
                subtitle = "Blocks screenshots and app switcher previews",
                checked = secureScreenEnabled,
                enabled = true,
                isError = false,
                onCheckedChange = onToggleSecureScreen,
            )
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun SecuritySwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    isError: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontSize = 11.sp,
                lineHeight = 15.sp,
                maxLines = 2,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}
