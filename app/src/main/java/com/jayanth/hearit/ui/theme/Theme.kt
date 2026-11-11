package com.jayanth.hearit.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Material 3 Expressive theme for Hear It.
 *
 * Ensures WCAG AAA/AA accessible contrast ratios across both Dark and Light themes.
 * Uses HearIt's signature brand palette (Electric Blue, Cyan, Violet, Slate) by default.
 */
@Composable
fun HearItTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = HearItTypography,
        content = content,
    )
}

/**
 * Curated high-contrast dark theme with glowing neon accents and deep OLED-ready surfaces.
 */
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF030712),
    primaryContainer = Color(0xFF0C4A6E),
    onPrimaryContainer = Color(0xFFBAE6FD),

    secondary = Violet400,
    onSecondary = Color(0xFF030712),
    secondaryContainer = Violet950,
    onSecondaryContainer = Violet300,

    tertiary = Cyan400,
    onTertiary = Color(0xFF030712),
    tertiaryContainer = Cyan950,
    onTertiaryContainer = Cyan300,

    background = MorpheBackground,
    onBackground = Slate100,

    surface = MorpheSurface,
    onSurface = Slate100,
    surfaceVariant = MorpheSurfaceHigh,
    onSurfaceVariant = Slate400,

    surfaceContainerLowest = Color(0xFF0D0E14),
    surfaceContainerLow = Color(0xFF151720),
    surfaceContainer = MorpheSurface,
    surfaceContainerHigh = MorpheSurfaceHigh,
    surfaceContainerHighest = MorpheSurfaceHighest,

    outline = Slate600,
    outlineVariant = MorpheOutline,

    error = ErrorRed,
    onError = Color(0xFF450A0A),
    errorContainer = ErrorRedContainer,
    onErrorContainer = Color(0xFFFECACA),
)

/**
 * Clean high-contrast light theme.
 */
private val LightColorScheme = lightColorScheme(
    primary = Blue600,
    onPrimary = Color.White,
    primaryContainer = Blue100,
    onPrimaryContainer = Blue900,

    secondary = Violet600,
    onSecondary = Color.White,
    secondaryContainer = Violet300,
    onSecondaryContainer = Violet950,

    tertiary = Cyan600,
    onTertiary = Color.White,
    tertiaryContainer = Cyan300,
    onTertiaryContainer = Cyan950,

    background = Slate50,
    onBackground = Slate900,

    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,

    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Slate50,
    surfaceContainer = Color(0xFFF1F5F9),
    surfaceContainerHigh = Slate200,
    surfaceContainerHighest = Slate300,

    outline = Slate300,
    outlineVariant = Slate200,

    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
)
