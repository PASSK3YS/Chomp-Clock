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
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Blue400,
    onPrimary = Color.Black,
    secondary = Emerald400,
    onSecondary = Color.Black,
    tertiary = Orange500,
    onTertiary = Color.Black,
    background = Black,
    onBackground = Color.White,
    surface = Zinc900,
    onSurface = Color.White,
    surfaceVariant = Zinc800,
    onSurfaceVariant = Zinc400,
    outline = Zinc800
)

private val LightColorScheme = lightColorScheme(
    primary = Blue500,
    onPrimary = Color.White,
    secondary = Emerald400,
    onSecondary = Color.White,
    tertiary = Orange500,
    onTertiary = Color.White,
    background = Color(0xFF0F172A), // Keep high contrast dark-leaning background or clean slate
    onBackground = Color.White,
    surface = Color(0xFF1E293B),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF475569)
)

@Composable
fun MyApplicationTheme(
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
        else -> DarkColorScheme // Maintain crisp AMOLED dark styling across the app
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                // When darkTheme is true (dark/black backgrounds), status bar icons (clock, battery, wifi, notifications) must be WHITE/LIGHT -> isAppearanceLightStatusBars = false
                // When darkTheme is false (light backgrounds), status bar icons must be DARK -> isAppearanceLightStatusBars = true
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
