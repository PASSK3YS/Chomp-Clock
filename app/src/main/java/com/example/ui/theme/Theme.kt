package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkM3ColorScheme = darkColorScheme(
    primary = Color(0xFF96CCFF),
    onPrimary = Color(0xFF003353),
    primaryContainer = Color(0xFF004B76),
    onPrimaryContainer = Color(0xFFCEE5FF),
    secondary = Color(0xFFB8C8DA),
    onSecondary = Color(0xFF233240),
    secondaryContainer = Color(0xFF3A4857),
    onSecondaryContainer = Color(0xFFD5E4F6),
    tertiary = Color(0xFFD2BFE7),
    onTertiary = Color(0xFF382A4A),
    tertiaryContainer = Color(0xFF4F4061),
    onTertiaryContainer = Color(0xFFEDDCFF),
    background = Color(0xFF0F1419),
    onBackground = Color(0xFFE1E2E8),
    surface = Color(0xFF12171D),
    onSurface = Color(0xFFE1E2E8),
    surfaceVariant = Color(0xFF42474E),
    onSurfaceVariant = Color(0xFFC2C7CF),
    surfaceContainerLowest = Color(0xFF0A0E13),
    surfaceContainerLow = Color(0xFF171C22),
    surfaceContainer = Color(0xFF1B2026),
    surfaceContainerHigh = Color(0xFF252A31),
    surfaceContainerHighest = Color(0xFF30353C),
    outline = Color(0xFF8C9199),
    outlineVariant = Color(0xFF42474E),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightM3ColorScheme = lightColorScheme(
    primary = Color(0xFF00639B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCEE5FF),
    onPrimaryContainer = Color(0xFF001D33),
    secondary = Color(0xFF51606F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD5E4F6),
    onSecondaryContainer = Color(0xFF0E1D2A),
    tertiary = Color(0xFF67587A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEDDCFF),
    onTertiaryContainer = Color(0xFF221533),
    background = Color(0xFFF7F9FF),
    onBackground = Color(0xFF181C20),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF181C20),
    surfaceVariant = Color(0xFFDEE3EB),
    onSurfaceVariant = Color(0xFF42474E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F4FA),
    surfaceContainer = Color(0xFFEBEEF4),
    surfaceContainerHigh = Color(0xFFE5E8EE),
    surfaceContainerHighest = Color(0xFFDFE3E9),
    outline = Color(0xFF72777F),
    outlineVariant = Color(0xFFC2C7CF),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val m3ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkM3ColorScheme
        else -> LightM3ColorScheme
    }

    val customAppColors = createMaterialYouAppColorScheme(m3ColorScheme, darkTheme)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                // When darkTheme is true (dark background), status bar & nav bar icons must be light/white (isAppearanceLightStatusBars = false)
                // When darkTheme is false (light background), status bar & nav bar icons must be dark (isAppearanceLightStatusBars = true)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalAppColors provides customAppColors) {
        MaterialTheme(
            colorScheme = m3ColorScheme,
            typography = Typography,
            content = content
        )
    }
}
