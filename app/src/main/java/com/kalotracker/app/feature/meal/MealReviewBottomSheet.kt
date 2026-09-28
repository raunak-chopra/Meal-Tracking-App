package com.kalotracker.app.feature.meal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.kalotracker.app.core.designsystem.*
import com.kalotracker.app.core.designsystem.components.KaloButton

@Composable
fun MealReviewBottomSheet(
    uiState: MealScanUiState,
    onAdjustWeight: (String, Float) -> Unit,
    onSaveMeal: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(KaloBackground.copy(alpha = 0.95f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Drag handle / Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI ESTIMATION",
                    style = KaloTypography.labelSmall,
                    color = KaloProtein
                )

                Text(
                    text = "${(uiState.confidence * 100).toInt()}% Confidence",
                    style = KaloTypography.bodyMedium,
                    color = KaloTextMuted
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Meal Title & Total Calories
            Text(
                text = uiState.mealTitle.ifBlank { "Detected Meal" },
                style = KaloTypography.headlineMedium,
                color = KaloTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${uiState.totalCalories} kcal",
                    style = KaloTypography.displayMedium,
                    color = KaloCalories
                )
                Text(
                    text = "${uiState.totalProtein.toInt()}g P  •  ${uiState.totalCarbs.toInt()}g C  •  ${uiState.totalFat.toInt()}g F",
                    style = KaloTypography.titleMedium,
                    color = KaloTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Food items breakdown
            Text(
                text = "PORTION CALIBRATION (TAP TO TWEAK)",
                style = KaloTypography.labelSmall,
                color = KaloTextMuted
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.items) { item ->
                    ItemWeightRow(
                        item = item,
                        onDecrement = { onAdjustWeight(item.id, 0.9f) },
                        onIncrement = { onAdjustWeight(item.id, 1.1f) }
                    )
                }

                if (!uiState.notes.isNullOrBlank()) {
                    item {
                        Text(
                            text = "Note: ${uiState.notes}",
                            style = KaloTypography.bodyMedium,
                            color = KaloTextMuted,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Action
            KaloButton(
                text = "Log Meal (${uiState.totalCalories} kcal)",
                onClick = onSaveMeal,
                loading = uiState.isSaving,
                enabled = !uiState.isSaving && !uiState.isAnalyzing && uiState.items.isNotEmpty()
            )
        }
    }
}

@Composable
fun ItemWeightRow(
    item: EditableFoodItem,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(KaloSurfaceElevated, RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = KaloTypography.titleMedium,
                color = KaloTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${item.currentCalories} kcal • ${item.currentProtein.toInt()}g P",
                style = KaloTypography.bodyMedium,
                color = KaloTextSecondary
            )
        }

        // Stepper buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onDecrement,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(KaloSurface)
            ) {
                Text(text = "−", style = KaloTypography.titleLarge, color = KaloTextPrimary)
            }

            Text(
                text = "${item.portionGrams.toInt()}g",
                style = KaloTypography.titleMedium,
                color = KaloTextPrimary,
                modifier = Modifier.widthIn(min = 45.dp)
            )

            IconButton(
                onClick = onIncrement,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(KaloSurface)
            ) {
                Text(text = "+", style = KaloTypography.titleLarge, color = KaloTextPrimary)
            }
        }
    }
}
