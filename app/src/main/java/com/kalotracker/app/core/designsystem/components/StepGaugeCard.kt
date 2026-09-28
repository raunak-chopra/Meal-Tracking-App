package com.kalotracker.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun StepGaugeCard(
    currentSteps: Long,
    stepGoal: Long = 10000L,
    activeCaloriesBurned: Double = 0.0,
    syncSource: String = "Samsung Health / Google Fit",
    onConnectHealthClick: (() -> Unit)? = null,
    isHealthConnected: Boolean = true,
    modifier: Modifier = Modifier
) {
    val progress = (currentSteps.toFloat() / stepGoal).coerceIn(0f, 1f)

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isHealthConnected) KaloSteps else KaloWarning)
                    )
                    Text(
                        text = if (isHealthConnected) "HEALTH CONNECT" else "SYNC DISCONNECTED",
                        style = KaloTypography.labelSmall,
                        color = if (isHealthConnected) KaloSteps else KaloWarning
                    )
                }

                Text(
                    text = syncSource,
                    style = KaloTypography.bodyMedium,
                    color = KaloTextMuted
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "%,d".format(currentSteps),
                        style = KaloTypography.displayMedium,
                        color = KaloTextPrimary
                    )
                    Text(
                        text = "of %,d goal".format(stepGoal),
                        style = KaloTypography.bodyMedium,
                        color = KaloTextSecondary
                    )
                }

                if (activeCaloriesBurned > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${activeCaloriesBurned.toInt()} kcal",
                            style = KaloTypography.titleLarge,
                            color = KaloCalories
                        )
                        Text(
                            text = "Active Burn",
                            style = KaloTypography.bodyMedium,
                            color = KaloTextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = KaloSteps,
                trackColor = KaloSurfaceElevated,
                strokeCap = StrokeCap.Round
            )
        }
    }
}
