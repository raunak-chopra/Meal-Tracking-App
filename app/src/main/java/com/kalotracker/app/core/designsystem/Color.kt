package com.kalotracker.app.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Warm Precision tokens supplied by the owner's Open Plate handoff. */
data class BrandPalette(val background: Color, val surface: Color, val raised: Color, val text: Color,
    val secondaryText: Color, val accent: Color, val onAccent: Color, val outline: Color, val divider: Color,
    val protein: Color, val carbs: Color, val fat: Color, val error: Color, val onError: Color)
val WarmDark = BrandPalette(Color(0xFF151916),Color(0xFF202621),Color(0xFF2B332D),Color(0xFFF4F1E8),
    Color(0xFFBBC3B7),Color(0xFFF1A17C),Color(0xFF202720),Color(0xFF899583),Color(0xFF414B42),
    Color(0xFFA1C3D9),Color(0xFFC7D39B),Color(0xFFE6ADBA),Color(0xFFFFB4AB),Color(0xFF690005))
val WarmLight = BrandPalette(Color(0xFFF5F2E9),Color.White,Color(0xFFE8ECE2),Color(0xFF202720),
    Color(0xFF586252),Color(0xFF98462D),Color.White,Color(0xFF6E7868),Color(0xFFD5DACE),
    Color(0xFF375E77),Color(0xFF566532),Color(0xFF874F61),Color(0xFFA52A24),Color.White)
val LocalBrandPalette = staticCompositionLocalOf { WarmDark }
val KaloBackground: Color @Composable get() = LocalBrandPalette.current.background
val KaloSurface: Color @Composable get() = LocalBrandPalette.current.surface
val KaloSurfaceElevated: Color @Composable get() = LocalBrandPalette.current.raised
val KaloBorder: Color @Composable get() = LocalBrandPalette.current.outline
val KaloDivider: Color @Composable get() = LocalBrandPalette.current.divider
val KaloTextPrimary: Color @Composable get() = LocalBrandPalette.current.text
val KaloTextSecondary: Color @Composable get() = LocalBrandPalette.current.secondaryText
val KaloTextMuted: Color @Composable get() = LocalBrandPalette.current.secondaryText
val KaloAccent: Color @Composable get() = LocalBrandPalette.current.accent
val KaloOnAccent: Color @Composable get() = LocalBrandPalette.current.onAccent
val KaloCalories: Color @Composable get() = LocalBrandPalette.current.accent
val KaloProtein: Color @Composable get() = LocalBrandPalette.current.protein
val KaloCarbs: Color @Composable get() = LocalBrandPalette.current.carbs
val KaloFat: Color @Composable get() = LocalBrandPalette.current.fat
val KaloSteps: Color @Composable get() = LocalBrandPalette.current.carbs
val KaloWater: Color @Composable get() = LocalBrandPalette.current.protein
val KaloWarning: Color @Composable get() = LocalBrandPalette.current.accent
val KaloError: Color @Composable get() = LocalBrandPalette.current.error
