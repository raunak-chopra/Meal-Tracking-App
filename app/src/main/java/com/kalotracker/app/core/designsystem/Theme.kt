package com.kalotracker.app.core.designsystem

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val KaloColorScheme = darkColorScheme(
    primary = KaloTextPrimary,
    onPrimary = KaloBackground,
    background = KaloBackground,
    onBackground = KaloTextPrimary,
    surface = KaloSurface,
    onSurface = KaloTextPrimary,
    surfaceVariant = KaloSurfaceElevated,
    onSurfaceVariant = KaloTextSecondary,
    outline = KaloBorder
)

@Composable
fun KaloTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = KaloBackground.toArgb()
            window.navigationBarColor = KaloBackground.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = KaloColorScheme,
        typography = KaloTypography,
        content = content
    )
}
