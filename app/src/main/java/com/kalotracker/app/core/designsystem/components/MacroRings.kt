package com.kalotracker.app.core.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kalotracker.app.core.designsystem.*

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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(KaloSurface, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "ENERGY REMAINING",
                        style = KaloTypography.labelSmall,
                        color = KaloTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val remaining = (targetCalories - currentCalories).coerceAtLeast(0)
                    Text(
                        text = "$remaining",
                        style = KaloTypography.displayLarge,
                        color = KaloTextPrimary
                    )
                    Text(
                        text = "kcal of $targetCalories target",
                        style = KaloTypography.bodyMedium,
                        color = KaloTextSecondary
                    )
                }

                // Concentric Macro Progress Indicator
                ConcentricRings(
                    calorieProgress = (currentCalories.toFloat() / targetCalories).coerceIn(0f, 1f),
                    proteinProgress = (proteinGrams.toFloat() / targetProtein).coerceIn(0f, 1f),
                    carbsProgress = (carbsGrams.toFloat() / targetCarbs).coerceIn(0f, 1f),
                    fatProgress = (fatGrams.toFloat() / targetFat).coerceIn(0f, 1f),
                    modifier = Modifier.size(110.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Linear Macro Breakdown Bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MacroPill(
                    label = "PROTEIN",
                    current = proteinGrams,
                    target = targetProtein,
                    color = KaloProtein,
                    modifier = Modifier.weight(1f)
                )
                MacroPill(
                    label = "CARBS",
                    current = carbsGrams,
                    target = targetCarbs,
                    color = KaloCarbs,
                    modifier = Modifier.weight(1f)
                )
                MacroPill(
                    label = "FAT",
                    current = fatGrams,
                    target = targetFat,
                    color = KaloFat,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ConcentricRings(
    calorieProgress: Float,
    proteinProgress: Float,
    carbsProgress: Float,
    fatProgress: Float,
    modifier: Modifier = Modifier
) {
    val animatedCalories = remember { Animatable(0f) }
    val animatedProtein = remember { Animatable(0f) }
    val animatedCarbs = remember { Animatable(0f) }
    val animatedFat = remember { Animatable(0f) }

    LaunchedEffect(calorieProgress, proteinProgress, carbsProgress, fatProgress) {
        val animSpec = tween<Float>(durationMillis = 800, easing = FastOutSlowInEasing)
        animatedCalories.animateTo(calorieProgress, animSpec)
        animatedProtein.animateTo(proteinProgress, animSpec)
        animatedCarbs.animateTo(carbsProgress, animSpec)
        animatedFat.animateTo(fatProgress, animSpec)
    }

    Canvas(modifier = modifier) {
        val strokeWidth = 7.dp.toPx()
        val spacing = 3.dp.toPx()

        val radii = listOf(
            Triple(KaloCalories, animatedCalories.value, (size.minDimension / 2) - strokeWidth),
            Triple(KaloProtein, animatedProtein.value, (size.minDimension / 2) - (strokeWidth * 2) - spacing),
            Triple(KaloCarbs, animatedCarbs.value, (size.minDimension / 2) - (strokeWidth * 3) - (spacing * 2)),
            Triple(KaloFat, animatedFat.value, (size.minDimension / 2) - (strokeWidth * 4) - (spacing * 3))
        )

        val center = Offset(size.width / 2, size.height / 2)

        radii.forEach { (color, progress, radius) ->
            // Background Track
            drawCircle(
                color = color.copy(alpha = 0.15f),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // Progress Arc
            if (progress > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }
    }
}

@Composable
fun MacroPill(
    label: String,
    current: Int,
    target: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(KaloSurfaceElevated, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = KaloTypography.labelSmall,
            color = color
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "${current}g",
            style = KaloTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = KaloTextPrimary
        )
        Text(
            text = "/ ${target}g",
            style = KaloTypography.bodyMedium,
            color = KaloTextMuted
        )
    }
}
