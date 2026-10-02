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
    val onScaleMeal: (Float) -> Unit,
    val onCookingFatChange: (CookingFat, Float) -> Unit,
    val onNutritionChange: (String, Int, Float, Float, Float) -> Unit,
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
    var showDetails by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }

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
                Column(Modifier.weight(1f)) {
                    Text("YOUR MEAL · APPROXIMATE", style = KaloTypography.labelSmall, color = KaloProtein)
                    Text(
                        text = "Save as it looks, or make a quick adjustment.",
                        style = KaloTypography.bodyMedium,
                        color = KaloTextMuted
                    )
                }
                IconButton(
                    onClick = actions.onDiscard,
                    enabled = !uiState.isSaving,
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(KaloSurfaceElevated)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Discard scan", tint = KaloTextPrimary)
                }
            }

            uiState.draftMessage?.let { Text(it, color = KaloTextSecondary) }
            uiState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = uiState.mealTitle,
                onValueChange = actions.onTitleChange,
                label = { Text("Meal name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Text("About ${uiState.totalCalories} kcal", style = KaloTypography.headlineLarge, color = KaloCalories)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { actions.onScaleMeal(0.75f) }, enabled = !uiState.isAnalyzing && !uiState.isSaving) { Text("Smaller") }
                OutlinedButton(onClick = { actions.onScaleMeal(1.25f) }, enabled = !uiState.isAnalyzing && !uiState.isSaving) { Text("Larger") }
            }
            Text("Each tap adjusts the current portion by about a quarter.", style = KaloTypography.bodySmall, color = KaloTextMuted)
            TextButton(onClick = { showDetails = !showDetails }) { Text(if (showDetails) "Hide grams & macros" else "Optional: grams & macros") }
            if (showDetails) Text("About ${uiState.totalProtein.toInt()} g protein · ${uiState.totalCarbs.toInt()} g carbs · ${uiState.totalFat.toInt()} g fat", color = KaloTextSecondary)

            Spacer(Modifier.height(8.dp))
            DateTimeChip(millis = uiState.timestamp, onChange = actions.onTimeChange)
            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.items, key = { it.id }) { item ->
                    if (showDetails) EditableItemRow(
                        item = item,
                        onNameChange = { actions.onNameChange(item.id, it) },
                        onGramsChange = { actions.onGramsChange(item.id, it) },
                        onRemove = { actions.onRemoveItem(item.id) },
                        onNutritionChange = { k, p, c, f -> actions.onNutritionChange(item.id, k, p, c, f) }
                    ) else Surface(shape = RoundedCornerShape(14.dp), color = KaloSurfaceElevated) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text(item.name, style = KaloTypography.titleMedium)
                            Text("About ${item.currentCalories} kcal", color = KaloTextSecondary)
                            Row {
                                TextButton(onClick = { actions.onGramsChange(item.id, (item.portionGrams * 0.75f).coerceAtLeast(1f)) }, enabled = !uiState.isAnalyzing && !uiState.isSaving) { Text("Smaller") }
                                TextButton(onClick = { actions.onGramsChange(item.id, (item.portionGrams * 1.25f).coerceAtMost(5000f)) }, enabled = !uiState.isAnalyzing && !uiState.isSaving) { Text("Larger") }
                                TextButton(onClick = { actions.onRemoveItem(item.id) }, enabled = !uiState.isAnalyzing && !uiState.isSaving) { Text("Remove") }
                            }
                        }
                    }
                }

                item {
                    Column {
                        OutlinedButton(onClick = actions.onAddFood, enabled = !uiState.isAnalyzing && !uiState.isSaving) { Text("+ Add food") }
                        Text("AI already estimates cooking fats. Add extra only if something was missed.", style = KaloTypography.bodySmall)
                        FilterChip(selected = uiState.hasAddedOil, onClick = actions.onToggleOil,
                            enabled = !uiState.isAnalyzing && !uiState.isSaving,
                            label = { Text(if (uiState.hasAddedOil) "Remove extra cooking fat" else "Optional: extra oil or butter") })
                        if (uiState.hasAddedOil) {
                            CookingFat.entries.forEach { fat ->
                                TextButton(onClick = { actions.onCookingFatChange(fat, uiState.cookingFatGrams) }, enabled = !uiState.isAnalyzing && !uiState.isSaving) {
                                    Text(if (fat == uiState.cookingFat) "✓ ${fat.label}" else fat.label)
                                }
                            }
                            listOf(5f to "About 1 teaspoon", 14f to "About 1 tablespoon", 28f to "About 2 tablespoons").forEach { (grams, label) ->
                                TextButton(onClick = { actions.onCookingFatChange(uiState.cookingFat, grams) }, enabled = !uiState.isAnalyzing && !uiState.isSaving) {
                                    Text(if (grams == uiState.cookingFatGrams) "✓ $label" else label)
                                }
                            }
                        }
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
                        placeholder = { Text("e.g. two rotis, half a bowl, paneer not chicken") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    TextButton(
                        onClick = actions.onReanalyze,
                        enabled = !uiState.isAnalyzing && !uiState.isSaving
                    ) { Text(if (uiState.isAnalyzing) "Re-analyzing..." else "Re-analyze photo with this note") }
                    Text(
                        "An everyday estimate for tracking habits. You can save without weighing or counting macros.",
                        style = KaloTypography.bodyMedium,
                        color = KaloTextMuted
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            KaloButton(
                text = "Save meal · about ${uiState.totalCalories} kcal",
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
    onNutritionChange: (Int, Float, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var editNutrition by remember { mutableStateOf(false) }
    if (editNutrition) NutritionDialog(item, { editNutrition = false }, onNutritionChange)
    // Local text so partially typed numbers (e.g. "1", "12.") are not fought by the model value.
    var gramsText by remember(item.id) { mutableStateOf(item.portionGrams.toString()) }
    LaunchedEffect(item.portionGrams) {
        // Preserve partial decimal typing, but reflect external Smaller/Larger changes.
        if (gramsText.toFloatOrNull() != item.portionGrams) gramsText = item.portionGrams.toString()
    }

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
        TextButton(onClick = { editNutrition = true }) { Text("Correct calories / macros") }
        Text(
            text = "${item.currentCalories} kcal • ${item.currentProtein.toInt()}g P • ${item.currentCarbs.toInt()}g C • ${item.currentFat.toInt()}g F",
            style = KaloTypography.bodyMedium,
            color = KaloTextSecondary
        )
    }
}
