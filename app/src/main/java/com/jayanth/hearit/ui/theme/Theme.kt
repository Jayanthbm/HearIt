package com.jayanth.hearit.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Material 3 Expressive theme for Hear It.
 *
 * Builds each scheme from the library's [lightColorScheme]/[darkColorScheme] so every
 * role (primary, secondary, background, onSurface, surface, error, ...) is copied with its
 * correct on-* contrast pair and no-token is missing. The brand's deep-navy primary is
 * applied on top by overriding the `primary`, `onPrimary` and `surface` roles; the violet
 * and cyan accents are available as [Secondary] / [Tertiary] values for custom components.
 */
@Composable
fun HearItTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic colour is available on Android 12+; overridden per-scheme on older devices.
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }

        // Stylised dark scheme: deep navy surface, violet/cyan accents, high-contrast text.
        darkTheme -> DarkColorScheme

        // Stylised light scheme: deep navy surface, accent cream cards, cyan/violet text.
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = HearItTypography,
        content = content,
    )
}

private val DarkColorScheme = darkColorScheme().copy(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    background = Surface,
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    error = Error,
    errorContainer = ErrorContainer,
    onError = OnError,
)

private val LightColorScheme = lightColorScheme().copy(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    background = Surface,
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    error = Error,
    errorContainer = ErrorContainer,
    onError = OnError,
)
