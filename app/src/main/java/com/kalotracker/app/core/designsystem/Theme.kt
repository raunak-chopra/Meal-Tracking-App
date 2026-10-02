package com.kalotracker.app.core.designsystem

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.kalotracker.app.core.settings.Appearance

val KaloShapes = Shapes(extraSmall=RoundedCornerShape(8.dp),small=RoundedCornerShape(12.dp),
    medium=RoundedCornerShape(16.dp),large=RoundedCornerShape(20.dp),extraLarge=RoundedCornerShape(28.dp))
@Composable
fun KaloTheme(appearance: Appearance = Appearance.SYSTEM, content: @Composable () -> Unit) {
    val dark = when(appearance) { Appearance.SYSTEM -> isSystemInDarkTheme(); Appearance.DARK -> true; Appearance.LIGHT -> false }
    val p = if(dark) WarmDark else WarmLight
    val base = if(dark) darkColorScheme() else lightColorScheme()
    val colors = base.copy(primary=p.accent,onPrimary=p.onAccent,primaryContainer=p.raised,onPrimaryContainer=p.text,
        secondary=p.carbs,onSecondary=p.onAccent,secondaryContainer=p.raised,onSecondaryContainer=p.text,
        tertiary=p.protein,onTertiary=p.onAccent,tertiaryContainer=p.raised,onTertiaryContainer=p.text,
        errorContainer=p.error,onErrorContainer=p.onError,background=p.background,onBackground=p.text,
        surface=p.surface,onSurface=p.text,surfaceVariant=p.raised,onSurfaceVariant=p.secondaryText,
        surfaceContainerLowest=p.background,surfaceContainerLow=p.surface,surfaceContainer=p.surface,
        surfaceContainerHigh=p.raised,surfaceContainerHighest=p.raised,
        outline=p.outline,outlineVariant=p.divider,error=p.error,onError=p.onError,
        inverseSurface=if(dark) WarmLight.surface else WarmDark.surface,
        inverseOnSurface=if(dark) WarmLight.text else WarmDark.text,surfaceTint=p.accent)
    val view = LocalView.current
    if(!view.isInEditMode) SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            window.statusBarColor = p.background.toArgb(); window.navigationBarColor = p.background.toArgb()
            WindowCompat.getInsetsController(window,view).apply {
                isAppearanceLightStatusBars = !dark; isAppearanceLightNavigationBars = !dark
            }
        }
    }
    CompositionLocalProvider(LocalBrandPalette provides p) {
        MaterialTheme(colorScheme=colors,typography=KaloTypography,shapes=KaloShapes,content=content)
    }
}
