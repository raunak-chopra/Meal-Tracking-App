package com.kalotracker.app.core.designsystem.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.kalotracker.app.core.designsystem.*

private const val BOWLFUL_WORDMARK_PATH =
    "M129.792 74.0H147.328C163.84 74.0 164.48 51.28 151.936 50.192C162.432 49.296 162.56 29.2 148.608 29.2H129.792ZM138.752 46.16V38.16H146.432C152.64 38.16 152.0 46.16 146.432 46.16ZM138.752 53.84H146.496C153.536 53.84 153.472 65.04 146.496 65.04H138.752ZM179.328 41.552C159.232 41.552 159.168 74.448 179.328 74.448C199.168 74.448 199.232 41.552 179.328 41.552ZM179.264 65.808C170.88 65.808 170.88 50.0 179.264 50.0C187.392 50.0 187.264 65.808 179.264 65.808ZM214.464 62.864 207.424 42.0H197.824L208.64 74.0H217.472L223.872 55.248L230.336 74.0H239.232L249.92 42.0H240.256L233.28 62.864L226.112 42.0H221.632ZM262.464 29.2H253.504V74.0H262.464ZM277.248 74.0V50.128H283.968V41.936H277.248V40.784C277.248 38.48 279.296 37.584 281.728 37.584C283.2 37.584 284.8 37.968 286.208 38.48L287.872 30.544C286.528 30.032 283.328 29.264 279.936 29.264C274.432 29.264 268.288 31.248 268.288 39.76V42.0H265.984V50.192H268.288V74.0ZM291.456 64.976C291.456 71.376 296.128 74.384 301.312 74.384C305.536 74.384 310.016 72.4 312.704 68.56V74.0H321.664V42.0H312.704V61.264C311.232 64.08 307.648 66.32 304.704 66.32C302.336 66.32 300.416 64.784 300.416 60.816V42.0H291.456ZM334.208 29.2H325.248V74.0H334.208Z"

@Composable
fun BrandMark(modifier: Modifier = Modifier.size(32.dp)) {
    val bowl = KaloTextPrimary
    Canvas(modifier) {
        scale(size.width / 100f, size.height / 100f, pivot = Offset.Zero) {
            drawArc(bowl, startAngle = 0f, sweepAngle = 180f, useCenter = true, topLeft = Offset(12f, 12f), size = Size(76f, 76f))
            drawCircle(BowlfulTurmeric, radius = 15f, center = Offset(50f, 29f))
        }
    }
}

/**
 * Renders the Bowlful brand lockup: the bowl-and-turmeric-dot mark alongside the
 * outlined Elvara Sans Bold wordmark from `docs/brand/bowlful-lockup.svg`, tinted
 * dynamically to match the current light/dark theme.
 */
@Composable
fun BrandLockup(modifier: Modifier = Modifier) {
    val textColor = KaloTextPrimary
    val wordmarkPath = remember { PathParser().parsePathString(BOWLFUL_WORDMARK_PATH).toPath() }
    Row(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = "Bowlful" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BrandMark(Modifier.size(30.dp))
        Canvas(Modifier.width(96.dp).height(24.dp).clearAndSetSemantics {}) {
            // Wordmark bounding box in bowlful-lockup.svg is x=[129.8..334.2] (204.4w), y=[29.2..74.4] (45.2h)
            val scaleX = size.width / 205f
            val scaleY = size.height / 46f
            val s = minOf(scaleX, scaleY)
            scale(s, s, pivot = Offset.Zero) {
                translate(left = -129.5f, top = -28.5f) {
                    drawPath(wordmarkPath, color = textColor)
                }
            }
        }
    }
}

@Composable
fun MacroSummaryCard(
    currentCalories: Int,
    targetCalories: Int,
    proteinGrams: Int,
    targetProtein: Int,
    carbsGrams: Int,
    targetCarbs: Int,
    fatGrams: Int,
    targetFat: Int,
    modifier: Modifier = Modifier
) {
    val energy = EnergyDisplay(currentCalories, targetCalories)
    val animatedCalories by animateIntAsState(
        targetValue = currentCalories,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "calories"
    )
    val animatedProgress by animateFloatAsState(
        targetValue = energy.progress,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "calorieArc"
    )
    val large = LocalDensity.current.fontScale >= 1.3f
    Column(
        modifier
            .fillMaxWidth()
            .background(KaloSurface, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                Text("Nutrition · calories logged", style = KaloTypography.bodyMedium, color = KaloTextSecondary)
                Text("$animatedCalories kcal", style = KaloTypography.displayLarge, color = KaloTextPrimary)
                Text(
                    if (targetCalories > 0) "$targetCalories kcal target" else "Historical target unavailable",
                    color = KaloTextSecondary
                )
                Spacer(Modifier.height(12.dp))
                Text(energy.deltaText, style = KaloTypography.bodyLarge, color = KaloAccent)
            }
            if (!large && targetCalories > 0) Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
                DailyArc(animatedProgress, Modifier.fillMaxSize())
                Text(
                    "${(currentCalories.toLong() * 100 / targetCalories)}%",
                    style = KaloTypography.titleMedium.copy(fontFeatureSettings = TabularFontFeature),
                    color = KaloTextPrimary
                )
            }
        }
        BoxWithConstraints {
            if (large || maxWidth < 340.dp) Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                MacroPill("Protein", proteinGrams, targetProtein, KaloProtein)
                MacroPill("Carbs", carbsGrams, targetCarbs, KaloCarbs)
                MacroPill("Fat", fatGrams, targetFat, KaloFat)
            } else Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MacroPill("Protein", proteinGrams, targetProtein, KaloProtein, Modifier.weight(1f))
                MacroPill("Carbs", carbsGrams, targetCarbs, KaloCarbs, Modifier.weight(1f))
                MacroPill("Fat", fatGrams, targetFat, KaloFat, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DailyArc(progress: Float, modifier: Modifier) {
    val accent = KaloAccent
    val track = KaloDivider
    Canvas(modifier.clearAndSetSemantics {}) {
        val inset = 5.dp.toPx()
        val area = Size(size.width - inset * 2, size.height - inset * 2)
        val stroke = Stroke(7.dp.toPx(), cap = StrokeCap.Round)
        drawArc(track, 150f, 240f, false, Offset(inset, inset), area, style = stroke)
        if (progress > 0f) drawArc(accent, 150f, 240f * progress, false, Offset(inset, inset), area, style = stroke)
    }
}

@Composable
fun MacroPill(label: String, current: Int, target: Int, color: Color, modifier: Modifier = Modifier) {
    val targetFraction = if (target > 0) (current.toFloat() / target).coerceIn(0f, 1f) else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "macro_$label"
    )
    Column(modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = KaloTypography.bodyMedium, color = KaloTextSecondary)
        Text("${current} g", style = KaloTypography.titleMedium.copy(fontFeatureSettings = TabularFontFeature), color = KaloTextPrimary)
        LinearProgressIndicator(
            progress = { animatedFraction },
            modifier = Modifier.fillMaxWidth().height(5.dp),
            color = color,
            trackColor = KaloDivider,
            strokeCap = StrokeCap.Round
        )
        Text(
            if (target > 0) "of $target g" else "Target unavailable",
            style = KaloTypography.labelSmall.copy(fontFeatureSettings = TabularFontFeature),
            color = KaloTextSecondary
        )
    }
}
