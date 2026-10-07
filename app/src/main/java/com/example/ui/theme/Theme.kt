package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DipuColorScheme = darkColorScheme(
    primary = DipuCyan,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF97F0FF),

    secondary = DipuBlue,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF004494),
    onSecondaryContainer = Color(0xFFD9E2FF),

    tertiary = DipuPurple,
    onTertiary = Color(0xFFFFFFFF),

    background = DipuDarkBg,
    onBackground = TextPrimary,

    surface = DipuSurface,
    onSurface = TextPrimary,
    surfaceVariant = DipuSurfaceElevated,
    onSurfaceVariant = TextSecondary,

    outline = DipuBorder,
    error = StatusError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to dark modern proxy UI
    dynamicColor: Boolean = false, // Keep the custom cyber-finance aesthetic
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DipuColorScheme,
        typography = Typography,
        content = content
    )
}
