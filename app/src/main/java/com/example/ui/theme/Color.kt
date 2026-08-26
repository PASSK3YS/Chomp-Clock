package com.example.ui.theme

import androidx.compose.material3.ColorScheme
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
    val secondary: Color = primaryVariant,
    val tertiary: Color = primary,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val inputBackground: Color,
    val inputBorder: Color
)

fun createMaterialYouAppColorScheme(m3: ColorScheme, isDark: Boolean): AppColorScheme {
    return if (isDark) {
        AppColorScheme(
            isDark = true,
            background = m3.background,
            surface = m3.surfaceContainerLow,
            surfaceElevated = m3.surfaceContainer,
            surfaceHighlight = m3.surfaceContainerHigh,
            border = m3.outlineVariant.copy(alpha = 0.40f),
            borderLight = m3.outlineVariant.copy(alpha = 0.70f),
            textPrimary = m3.onSurface,
            textSecondary = m3.onSurfaceVariant,
            textMuted = m3.outline,
            primary = m3.primary,
            primaryVariant = m3.primaryContainer,
            secondary = m3.secondary,
            tertiary = m3.tertiary,
            success = Emerald400,
            warning = Amber400,
            danger = m3.error,
            inputBackground = m3.surfaceContainerHighest.copy(alpha = 0.45f),
            inputBorder = m3.outlineVariant.copy(alpha = 0.60f)
        )
    } else {
        AppColorScheme(
            isDark = false,
            background = m3.background,
            surface = m3.surface,
            surfaceElevated = m3.surfaceContainerLow,
            surfaceHighlight = m3.surfaceContainer,
            border = m3.outlineVariant.copy(alpha = 0.50f),
            borderLight = m3.outlineVariant.copy(alpha = 0.85f),
            textPrimary = m3.onSurface,
            textSecondary = m3.onSurfaceVariant,
            textMuted = m3.outline,
            primary = m3.primary,
            primaryVariant = m3.primaryContainer,
            secondary = m3.secondary,
            tertiary = m3.tertiary,
            success = Emerald600,
            warning = Amber600,
            danger = m3.error,
            inputBackground = m3.surfaceContainerLowest,
            inputBorder = m3.outlineVariant.copy(alpha = 0.65f)
        )
    }
}

val DarkAppColorScheme = AppColorScheme(
    isDark = true,
    background = Color(0xFF0F1419),
    surface = Color(0xFF171C22),
    surfaceElevated = Color(0xFF1B2026),
    surfaceHighlight = Color(0xFF252A31),
    border = Color(0xFF42474E).copy(alpha = 0.5f),
    borderLight = Color(0xFF42474E),
    textPrimary = Color(0xFFE1E2E8),
    textSecondary = Color(0xFFC2C7CF),
    textMuted = Color(0xFF8C9199),
    primary = Color(0xFF96CCFF),
    primaryVariant = Color(0xFF004B76),
    secondary = Color(0xFFB8C8DA),
    tertiary = Color(0xFFD2BFE7),
    success = Emerald400,
    warning = Amber400,
    danger = Color(0xFFFFB4AB),
    inputBackground = Color(0xFF30353C).copy(alpha = 0.45f),
    inputBorder = Color(0xFF42474E)
)

val LightAppColorScheme = AppColorScheme(
    isDark = false,
    background = Color(0xFFF7F9FF),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFF1F4FA),
    surfaceHighlight = Color(0xFFEBEEF4),
    border = Color(0xFFC2C7CF).copy(alpha = 0.6f),
    borderLight = Color(0xFFC2C7CF),
    textPrimary = Color(0xFF181C20),
    textSecondary = Color(0xFF42474E),
    textMuted = Color(0xFF72777F),
    primary = Color(0xFF00639B),
    primaryVariant = Color(0xFFCEE5FF),
    secondary = Color(0xFF51606F),
    tertiary = Color(0xFF67587A),
    success = Emerald600,
    warning = Amber600,
    danger = Color(0xFFBA1A1A),
    inputBackground = Color(0xFFFFFFFF),
    inputBorder = Color(0xFFC2C7CF)
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColorScheme }

object AppTheme {
    val colors: AppColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
}
