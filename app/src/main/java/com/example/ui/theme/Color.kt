package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val Black = Color(0xFF000000)
val Zinc950 = Color(0xFF09090B)
val Zinc900 = Color(0xFF18181B)
val Zinc800 = Color(0xFF27272A)
val Zinc700 = Color(0xFF3F3F46)
val Zinc500 = Color(0xFF71717A)
val Zinc400 = Color(0xFFA1A1AA)
val Zinc300 = Color(0xFFD4D4D8)
val Zinc200 = Color(0xFFE4E4E7)
val Zinc100 = Color(0xFFF4F4F5)
val Zinc50 = Color(0xFFFAFAFA)

val Blue600 = Color(0xFF2563EB)
val Blue500 = Color(0xFF3B82F6)
val Blue400 = Color(0xFF60A5FA)
val Blue300 = Color(0xFF93C5FD)

val Emerald600 = Color(0xFF059669)
val Emerald500 = Color(0xFF10B981)
val Emerald400 = Color(0xFF34D399)

val Orange600 = Color(0xFFEA580C)
val Orange500 = Color(0xFFF97316)
val Orange400 = Color(0xFFFB923C)

val Amber600 = Color(0xFFD97706)
val Amber500 = Color(0xFFF59E0B)
val Amber400 = Color(0xFFFBBF24)

val Red600 = Color(0xFFDC2626)
val Red500 = Color(0xFFEF4444)
val Red400 = Color(0xFFF87171)

// Light Theme Specifics
val LightBackground = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceElevated = Color(0xFFF1F5F9)
val LightSurfaceBorder = Color(0xFFE2E8F0)
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF475569)
val LightTextMuted = Color(0xFF64748B)

data class AppColorScheme(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceHighlight: Color,
    val border: Color,
    val borderLight: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val primary: Color,
    val primaryVariant: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val inputBackground: Color,
    val inputBorder: Color
)

val DarkAppColorScheme = AppColorScheme(
    isDark = true,
    background = Black,
    surface = Zinc900,
    surfaceElevated = Zinc800,
    surfaceHighlight = Color(0xFF2E2E33),
    border = Zinc800,
    borderLight = Zinc700,
    textPrimary = Color.White,
    textSecondary = Zinc400,
    textMuted = Zinc500,
    primary = Blue500,
    primaryVariant = Blue400,
    success = Emerald400,
    warning = Amber400,
    danger = Red500,
    inputBackground = Zinc800,
    inputBorder = Zinc700
)

val LightAppColorScheme = AppColorScheme(
    isDark = false,
    background = LightBackground,
    surface = LightSurface,
    surfaceElevated = LightSurfaceElevated,
    surfaceHighlight = Color(0xFFE2E8F0),
    border = LightSurfaceBorder,
    borderLight = Color(0xFFCBD5E1),
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textMuted = LightTextMuted,
    primary = Blue600,
    primaryVariant = Blue500,
    success = Emerald600,
    warning = Amber600,
    danger = Red600,
    inputBackground = LightSurfaceElevated,
    inputBorder = Color(0xFFCBD5E1)
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColorScheme }

object AppTheme {
    val colors: AppColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
}
