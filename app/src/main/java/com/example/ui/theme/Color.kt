package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// ==========================================
// 1. Dark (Cyber Black) Theme Color Palette
// ==========================================
val CyberBackground = Color(0xFF090D16)
val CyberSurface = Color(0xFF111726)
val CyberSurfaceElevated = Color(0xFF172033)
val CyberSurfaceCard = Color(0xFF151D2C)
val CyberSurfaceVariant = Color(0xFF1E293B)
val CyberBorder = Color(0xFF222F46)
val CyberBorderBright = Color(0xFF334664)

val DarkTextPrimary = Color(0xFFF1F5F9)
val DarkTextSecondary = Color(0xFF94A3B8)
val DarkTextMuted = Color(0xFF64748B)

// For backwards compatibility
val TextPrimary = DarkTextPrimary
val TextSecondary = DarkTextSecondary
val TextMuted = DarkTextMuted
val TextAccent = Color(0xFF00E5FF)

// ==========================================
// 2. Light (High-Tech White) Theme Color Palette
// ==========================================
val LightBackground = Color(0xFFF6F8FC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceElevated = Color(0xFFF0F4F8)
val LightSurfaceCard = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE2E8F0)
val LightBorder = Color(0xFFDDE3EA)
val LightBorderBright = Color(0xFFCBD5E1)

val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF334155)
val LightTextMuted = Color(0xFF64748B)
val LightPrimaryAccent = Color(0xFF0284C7)
val LightSecondaryAccent = Color(0xFF059669)
val LightTertiaryAccent = Color(0xFFE11D48)

// ==========================================
// 3. Shared Electric Neon Accents
// ==========================================
val NeonCyan = Color(0xFF00E5FF)
val NeonGreen = Color(0xFF00E676)
val NeonCoral = Color(0xFFFF3366)
val NeonAmber = Color(0xFFFFAB00)
val NeonPurple = Color(0xFF7C4DFF)
val NeonBlue = Color(0xFF2979FF)
val NeonPink = Color(0xFFFF4081)

// Heart Rate Zone Colors
val Zone1WarmUp = Color(0xFF00E5FF)    // Cyan (50-60%)
val Zone2FatBurn = Color(0xFF00E676)   // Neon Green (60-70%)
val Zone3Aerobic = Color(0xFFFFD600)   // Bright Yellow (70-80%)
val Zone4Anaerobic = Color(0xFFFF9100) // Vibrant Orange (80-90%)
val Zone5Peak = Color(0xFFFF1744)      // Crimson Red (90-100%)

// Status & Metric Colors
val SpeedColor = Color(0xFF00E5FF)
val CadenceColor = Color(0xFF7C4DFF)
val CalorieColor = Color(0xFFFF3366)
val ElevationColor = Color(0xFFFFAB00)
val RecoveryGreen = Color(0xFF10B981)

// ==========================================
// 4. Unified Pulse Design System Color Bundle
// ==========================================
@Immutable
data class PulseColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceCard: Color,
    val surfaceVariant: Color,
    val border: Color,
    val borderBright: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val accent: Color
)

val DarkPulseColors = PulseColors(
    isDark = true,
    background = CyberBackground,
    surface = CyberSurface,
    surfaceElevated = CyberSurfaceElevated,
    surfaceCard = CyberSurfaceCard,
    surfaceVariant = CyberSurfaceVariant,
    border = CyberBorder,
    borderBright = CyberBorderBright,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textMuted = DarkTextMuted,
    primary = NeonCyan,
    secondary = NeonGreen,
    tertiary = NeonCoral,
    accent = NeonCyan
)

val LightPulseColors = PulseColors(
    isDark = false,
    background = LightBackground,
    surface = LightSurface,
    surfaceElevated = LightSurfaceElevated,
    surfaceCard = LightSurfaceCard,
    surfaceVariant = LightSurfaceVariant,
    border = LightBorder,
    borderBright = LightBorderBright,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textMuted = LightTextMuted,
    primary = LightPrimaryAccent,
    secondary = LightSecondaryAccent,
    tertiary = LightTertiaryAccent,
    accent = LightPrimaryAccent
)
