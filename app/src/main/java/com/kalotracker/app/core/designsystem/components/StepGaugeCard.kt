package com.kalotracker.app.core.designsystem.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.kalotracker.app.core.designsystem.*
import com.kalotracker.app.core.health.StepReadingState

@Composable
fun StepGaugeCard(
    currentSteps: Long,
    stepGoal: Long = 10000L,
    activeCaloriesBurned: Double = 0.0,
    syncSource: String = "Health Connect",
    onConnectHealthClick: (() -> Unit)? = null,
    isHealthConnected: Boolean = true,
    modifier: Modifier = Modifier,
    readingState: StepReadingState = if (isHealthConnected) StepReadingState.READY else StepReadingState.PERMISSION_REQUIRED,
    onRetry: (() -> Unit)? = null
) {
    val targetFraction = if (stepGoal > 0) (currentSteps.toFloat() / stepGoal).coerceIn(0f, 1f) else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "stepProgress"
    )
    Column(
        modifier
            .fillMaxWidth()
            .background(KaloSurface, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Steps", style = KaloTypography.titleMedium, color = KaloTextPrimary, modifier = Modifier.semantics { heading() })
        when (readingState) {
            StepReadingState.READY -> {
                Column(Modifier.semantics(mergeDescendants = true) {}) {
                    Text(
                        "%,d steps".format(currentSteps),
                        style = KaloTypography.headlineMedium.copy(fontFeatureSettings = TabularFontFeature),
                        color = KaloTextPrimary
                    )
                    Text(
                        if (stepGoal > 0) "%,d step target".format(stepGoal) else "No step target",
                        color = KaloTextSecondary
                    )
                }
                if (stepGoal > 0) LinearProgressIndicator(
                    progress = { animatedFraction },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = KaloSteps,
                    trackColor = KaloDivider,
                    strokeCap = StrokeCap.Round
                )
                if (activeCaloriesBurned > 0) Text("${activeCaloriesBurned.toInt()} kcal active energy", color = KaloTextSecondary)
                Text(syncSource, style = KaloTypography.bodyMedium, color = KaloTextSecondary)
            }
            StepReadingState.SYNCING -> Text("Reading Health Connect…", color = KaloTextSecondary)
            StepReadingState.EMPTY -> Text("Connected · no step readings for this day", color = KaloTextSecondary)
            StepReadingState.PERMISSION_REQUIRED -> Text("Connect Health Connect to read steps. No reading is available yet.", color = KaloTextSecondary)
            StepReadingState.UNAVAILABLE -> Text("Health Connect is unavailable on this device.", color = KaloTextSecondary)
            StepReadingState.ERROR -> Text("Couldn't read steps. Try again.", color = KaloTextSecondary)
        }
        if (readingState == StepReadingState.ERROR || readingState == StepReadingState.EMPTY) {
            onRetry?.let { TextButton(onClick = it, modifier = Modifier.heightIn(min = 48.dp)) { Text("Refresh steps") } }
        } else if (readingState == StepReadingState.PERMISSION_REQUIRED || readingState == StepReadingState.UNAVAILABLE) {
            onConnectHealthClick?.let { TextButton(onClick = it, modifier = Modifier.heightIn(min = 48.dp)) { Text("Connect steps") } }
        }
    }
}
