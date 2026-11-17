package com.kalotracker.app.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Bowlful "Turmeric & Chai" tokens. Source of truth: brands/Bowlful/design-system/tokens.json in the Brand Manager workspace. */
data class BrandPalette(val background: Color, val surface: Color, val raised: Color, val text: Color,
    val secondaryText: Color, val accent: Color, val onAccent: Color, val outline: Color, val divider: Color,
    val protein: Color, val carbs: Color, val fat: Color, val water: Color, val error: Color, val onError: Color)
val WarmDark = BrandPalette(Color(0xFF1A1512),Color(0xFF251E19),Color(0xFF322922),Color(0xFFFBF5E8),
    Color(0xFFC9BBA8),Color(0xFFF2B454),Color(0xFF2B1A00),Color(0xFF9A8A78),Color(0xFF43382F),
    Color(0xFF8FD0A8),Color(0xFFF0A58A),Color(0xFFD9A6D6),Color(0xFF8EC5E2),Color(0xFFFFB4AB),Color(0xFF690005))
val WarmLight = BrandPalette(Color(0xFFFBF5E8),Color.White,Color(0xFFF3E9D3),Color(0xFF2B201A),
    Color(0xFF6B5B4E),Color(0xFF955500),Color.White,Color(0xFF8A7B6C),Color(0xFFE4D7BE),
    Color(0xFF2F6B4F),Color(0xFFA8401F),Color(0xFF7B3F7A),Color(0xFF2E6A8A),Color(0xFFA52A24),Color.White)
/** Logo dot colour; identical in both themes. */
val BowlfulTurmeric = Color(0xFFE9A23B)
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
val KaloSteps: Color @Composable get() = LocalBrandPalette.current.protein
val KaloWater: Color @Composable get() = LocalBrandPalette.current.water
val KaloWarning: Color @Composable get() = LocalBrandPalette.current.accent
val KaloError: Color @Composable get() = LocalBrandPalette.current.error
