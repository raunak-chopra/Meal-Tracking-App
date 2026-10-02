package com.kalotracker.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.kalotracker.app.core.designsystem.*

@Composable
fun WaterIntakeCard(
    currentWaterMl: Int,
    targetWaterMl: Int = 2500,
    onAddWater: (Int) -> Unit,
    onUndoWater: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (targetWaterMl > 0) {
        (currentWaterMl.toFloat() / targetWaterMl).coerceIn(0f, 1f)
    } else 0f

    val percent = (progress * 100).toInt()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(KaloSurface, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column {
            // Header: Category Tag & Target Percent
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(KaloWater)
                    )
                    Text(
                        text = "HYDRATION",
                        style = KaloTypography.labelSmall,
                        color = KaloWater
                    )
                }

                Text(
                    text = if(targetWaterMl > 0) "$percent% of goal" else "Target unavailable",
                    style = KaloTypography.bodyMedium,
                    color = if (percent >= 100) KaloWater else KaloTextMuted
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Current Volume / Goal
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "%,d ml".format(currentWaterMl),
                        style = KaloTypography.displayMedium,
                        color = KaloTextPrimary
                    )
                    Text(
                        text = if(targetWaterMl > 0) "of %,d ml target".format(targetWaterMl) else "No goal recorded for this day",
                        style = KaloTypography.bodyMedium,
                        color = KaloTextSecondary
                    )
                }

                if (currentWaterMl > 0) {
                    // Quick Undo Button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(KaloSurfaceElevated)
                            .clickable(onClick = onUndoWater)
                            .heightIn(min = 48.dp)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Undo last water entry",
                            tint = KaloTextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Undo",
                            style = KaloTypography.labelSmall,
                            color = KaloTextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = KaloWater,
                trackColor = KaloSurfaceElevated,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Quick-add Increment Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WaterPill(
                    label = "+250 ml",
                    subLabel = "Glass",
                    onClick = { onAddWater(250) },
                    modifier = Modifier.weight(1f)
                )
                WaterPill(
                    label = "+500 ml",
                    subLabel = "Bottle",
                    onClick = { onAddWater(500) },
                    modifier = Modifier.weight(1f)
                )
                WaterPill(
                    label = "+750 ml",
                    subLabel = "Large",
                    onClick = { onAddWater(750) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun WaterPill(
    label: String,
    subLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(KaloSurfaceElevated)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = KaloTypography.titleMedium,
                color = KaloTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subLabel,
                style = KaloTypography.labelSmall,
                color = KaloWater
            )
        }
    }
}
