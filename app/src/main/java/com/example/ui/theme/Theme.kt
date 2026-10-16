package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ElectricViolet,
    onPrimary = Color.White,
    primaryContainer = ElectricVioletContainer,
    onPrimaryContainer = NeonCyanLight,
    secondary = NeonCyan,
    onSecondary = MidnightNavy,
    secondaryContainer = NeonCyanContainer,
    onSecondaryContainer = Color.White,
    tertiary = SoftGreen,
    onTertiary = MidnightNavy,
    tertiaryContainer = SoftGreenContainer,
    onTertiaryContainer = Color.White,
    background = MidnightNavy,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceCard,
    outline = DarkBorder,
    outlineVariant = Color(0xFF334155),
    error = CrimsonError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricViolet,
    onPrimary = Color.White,
    primaryContainer = LightSurfaceVariant,
    onPrimaryContainer = ElectricViolet,
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0369A1),
    tertiary = Color(0xFF059669),
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = LightTextSecondary,
    outline = Color(0xFFCBD5E1),
    error = CrimsonError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to Dark Cinematic Studio theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
