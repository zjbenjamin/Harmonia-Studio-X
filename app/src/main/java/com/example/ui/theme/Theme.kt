package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val StudioDarkColorScheme = darkColorScheme(
    primary = StudioCyan,
    onPrimary = StudioDarkBg,
    primaryContainer = StudioSurfaceActive,
    onPrimaryContainer = StudioCyan,
    secondary = StudioViolet,
    onSecondary = StudioDarkBg,
    secondaryContainer = StudioSurfaceElevated,
    onSecondaryContainer = StudioViolet,
    tertiary = StudioAmber,
    onTertiary = StudioDarkBg,
    background = StudioDarkBg,
    onBackground = TextPrimary,
    surface = StudioSurface,
    onSurface = TextPrimary,
    surfaceVariant = StudioSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = StudioBorder,
    error = StudioRedRecord
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = StudioDarkBg.toArgb()
                window.navigationBarColor = StudioDarkBg.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = StudioDarkColorScheme,
        typography = Typography,
        content = content
    )
}
