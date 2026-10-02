package com.kalotracker.app.feature.meal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kalotracker.app.core.designsystem.*
import com.kalotracker.app.core.designsystem.components.DateTimeChip
import com.kalotracker.app.core.designsystem.components.KaloButton

/** Everything the user can change about an AI result before it is saved. */
data class MealReviewActions(
    val onTitleChange: (String) -> Unit,
    val onNameChange: (String, String) -> Unit,
    val onGramsChange: (String, Float) -> Unit,
    val onRemoveItem: (String) -> Unit,
    val onAddFood: () -> Unit,
    val onNoteChange: (String) -> Unit,
    val onReanalyze: () -> Unit,
    val onToggleOil: () -> Unit,
    val onTimeChange: (Long) -> Unit,
    val onSave: () -> Unit,
    val onDiscard: () -> Unit
)

@Composable
fun MealReviewBottomSheet(
    uiState: MealScanUiState,
    actions: MealReviewActions,
    modifier: Modifier = Modifier
) {
    val lowConfidence = uiState.confidence < 0.6f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(KaloBackground.copy(alpha = 0.97f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("AI ESTIMATE - CHECK BEFORE SAVING", style = KaloTypography.labelSmall, color = KaloProtein)
                    Text(
                        text = "${(uiState.confidence * 100).toInt()}% confidence" +
                            if (lowConfidence) " - low, please review carefully" else "",
                        style = KaloTypography.bodyMedium,
                        color = if (lowConfidence) KaloFat else KaloTextMuted
                    )
                }
                IconButton(
                    onClick = actions.onDiscard,
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(KaloSurfaceElevated)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Discard scan", tint = KaloTextPrimary)
                }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = uiState.mealTitle,
                onValueChange = actions.onTitleChange,
                label = { Text("Meal name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${uiState.totalCalories} kcal", style = KaloTypography.displayMedium, color = KaloCalories)
                Text(
                    "${uiState.totalProtein.toInt()}g P  •  ${uiState.totalCarbs.toInt()}g C  •  ${uiState.totalFat.toInt()}g F",
                    style = KaloTypography.titleMedium,
                    color = KaloTextSecondary
                )
            }

            Spacer(Modifier.height(8.dp))
            DateTimeChip(millis = uiState.timestamp, onChange = actions.onTimeChange)
            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.items, key = { it.id }) { item ->
                    EditableItemRow(
                        item = item,
                        onNameChange = { actions.onNameChange(item.id, it) },
                        onGramsChange = { actions.onGramsChange(item.id, it) },
                        onRemove = { actions.onRemoveItem(item.id) }
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = actions.onAddFood) { Text("+ Add food") }
                        FilterChip(
                            selected = uiState.hasAddedOil,
                            onClick = actions.onToggleOil,
                            label = { Text(if (uiState.hasAddedOil) "Oil/butter +120 kcal" else "Add oil/butter (1 tbsp)") }
                        )
                    }
                }

                if (!uiState.notes.isNullOrBlank()) {
                    item {
                        Text("AI note: ${uiState.notes}", style = KaloTypography.bodyMedium, color = KaloTextMuted)
                    }
                }

                item {
                    OutlinedTextField(
                        value = uiState.userNote,
                        onValueChange = actions.onNoteChange,
                        label = { Text("Tell the AI more (optional)") },
                        placeholder = { Text("e.g. half portion, 2 tbsp oil, skim milk") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    TextButton(
                        onClick = actions.onReanalyze,
                        enabled = !uiState.isAnalyzing
                    ) { Text(if (uiState.isAnalyzing) "Re-analyzing..." else "Re-analyze photo with this note") }
                    Text(
                        "Estimates from a photo can be off by 20-30%. Edit any name or weight above.",
                        style = KaloTypography.bodyMedium,
                        color = KaloTextMuted
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            KaloButton(
                text = "Log Meal (${uiState.totalCalories} kcal)",
                onClick = actions.onSave,
                loading = uiState.isSaving,
                enabled = !uiState.isSaving && !uiState.isAnalyzing && uiState.items.isNotEmpty()
            )
        }
    }
}

@Composable
fun EditableItemRow(
    item: EditableFoodItem,
    onNameChange: (String) -> Unit,
    onGramsChange: (Float) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Local text so partially typed numbers (e.g. "1", "12.") are not fought by the model value.
    var gramsText by remember(item.id) { mutableStateOf(item.portionGrams.toInt().toString()) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(KaloSurfaceElevated, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = item.name,
                onValueChange = onNameChange,
                singleLine = true,
                label = { Text("Food") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = gramsText,
                onValueChange = { text ->
                    gramsText = text.filter { it.isDigit() || it == '.' }.take(6)
                    gramsText.toFloatOrNull()?.takeIf { it > 0f }?.let(onGramsChange)
                },
                singleLine = true,
                label = { Text("Grams") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.width(96.dp)
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Remove ${item.name}", tint = KaloTextMuted)
            }
        }
        Text(
            text = "${item.currentCalories} kcal • ${item.currentProtein.toInt()}g P • ${item.currentCarbs.toInt()}g C • ${item.currentFat.toInt()}g F",
            style = KaloTypography.bodyMedium,
            color = KaloTextSecondary
        )
    }
}
