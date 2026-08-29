package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalPulseColors = staticCompositionLocalOf { DarkPulseColors }

object PulseTheme {
    val colors: PulseColors
        @Composable
        @ReadOnlyComposable
        get() = LocalPulseColors.current
}

private val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF003840),
    onPrimaryContainer = Color(0xFF9CF4FF),
    secondary = NeonGreen,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF00391A),
    onSecondaryContainer = Color(0xFFA6FFC4),
    tertiary = NeonCoral,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF4C0016),
    onTertiaryContainer = Color(0xFFFFD9DF),
    background = CyberBackground,
    onBackground = DarkTextPrimary,
    surface = CyberSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = CyberBorder,
    outlineVariant = CyberBorderBright
)

private val LightColorScheme: ColorScheme = lightColorScheme(
    primary = LightPrimaryAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = LightSecondaryAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF065F46),
    tertiary = LightTertiaryAccent,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE4E6),
    onTertiaryContainer = Color(0xFF9F1239),
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    outlineVariant = LightBorderBright
)

@Composable
fun PranamTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val pulseColors = if (darkTheme) DarkPulseColors else LightPulseColors

    CompositionLocalProvider(LocalPulseColors provides pulseColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun PulseTrackTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = PranamTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = PranamTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
