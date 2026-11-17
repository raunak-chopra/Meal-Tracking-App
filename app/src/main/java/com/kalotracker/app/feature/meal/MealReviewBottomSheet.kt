package com.kalotracker.app.feature.meal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kalotracker.app.core.designsystem.*
import com.kalotracker.app.core.designsystem.components.DateTimeChip
import com.kalotracker.app.core.designsystem.components.KaloButton
import java.io.File

/** Everything the user can change about a photo result before it is saved. */
data class MealReviewActions(
    val onTitleChange: (String) -> Unit,
    val onScaleMeal: (Float) -> Unit,
    val onCookingFatChange: (CookingFat, Float) -> Unit,
    val onNutritionChange: (String, Int, Float, Float, Float) -> Unit,
    val onNameChange: (String, String) -> Unit,
    val onKeepNutrition: (String) -> Unit,
    /** Re-asks Gemini for one food using its typed name and amount. */
    val onReestimateItem: (String) -> Unit,
    val onAmountInputChange: (String, String) -> Unit,
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
    val idle = !uiState.isAnalyzing && !uiState.isSaving && uiState.estimatingItemId == null

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(KaloBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Review meal", style = KaloTypography.titleLarge, color = KaloTextPrimary, modifier = Modifier.weight(1f))
                IconButton(onClick = actions.onDiscard, enabled = !uiState.isSaving) {
                    Icon(Icons.Default.Close, contentDescription = "Discard meal", tint = KaloTextSecondary)
                }
            }

            uiState.draftMessage?.let { Text(it, style = KaloTypography.bodyMedium, color = KaloTextSecondary) }
            uiState.errorMessage?.let { Text(it, style = KaloTypography.bodyMedium, color = KaloError) }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp)
            ) {
                item {
                    val photoFile = uiState.localImageUri?.let(::File)?.takeIf { it.exists() }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(KaloSurface, RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        if (photoFile != null) {
                            AsyncImage(
                                model = photoFile,
                                contentDescription = "Captured meal photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(KaloSurfaceElevated)
                            )
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(if (uiState.items.any { !it.hasValidPortion }) "Check food amounts" else if (uiState.items.any { !it.isReadyToSave }) "Review food nutrition" else "About ${uiState.totalCalories} kcal", style = KaloTypography.displayMedium, color = KaloTextPrimary)
                            Text("Estimated from your photo. Check foods and amounts.", style = KaloTypography.bodyMedium, color = KaloTextSecondary)
                            uiState.notes?.takeIf { it.isNotBlank() }?.let { note ->
                                Text("Assumed: $note", style = KaloTypography.bodyMedium, color = KaloTextSecondary)
                            }
                            if (uiState.items.all { it.isReadyToSave }) Text(
                                "Protein ${uiState.totalProtein.toInt()} g  ·  Carbs ${uiState.totalCarbs.toInt()} g  ·  Fat ${uiState.totalFat.toInt()} g",
                                style = KaloTypography.bodyMedium.copy(fontFeatureSettings = TabularFontFeature),
                                color = KaloTextSecondary
                            )
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        DateTimeChip(millis = uiState.timestamp, onChange = actions.onTimeChange)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { actions.onScaleMeal(0.75f) }, enabled = idle) { Text("Smaller") }
                        TextButton(onClick = { actions.onScaleMeal(1.25f) }, enabled = idle) { Text("Larger") }
                    }
                }

                item { HorizontalDivider(color = KaloDivider) }

                items(uiState.items, key = { it.id }) { item ->
                    EditableItemRow(
                        item = item,
                        enabled = idle,
                        onNameChange = { actions.onNameChange(item.id, it) },
                        onAmountInputChange = { actions.onAmountInputChange(item.id, it) },
                        onRemove = { actions.onRemoveItem(item.id) },
                        onNutritionChange = { k, p, c, f -> actions.onNutritionChange(item.id, k, p, c, f) },
                        onKeepNutrition = { actions.onKeepNutrition(item.id) },
                        onReestimate = { actions.onReestimateItem(item.id) },
                        estimating = uiState.estimatingItemId == item.id
                    )
                }

                item {
                    OutlinedButton(onClick = actions.onAddFood, enabled = idle, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text("Add a food")
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = uiState.hasAddedOil,
                            onClick = actions.onToggleOil,
                            enabled = idle,
                            label = { Text("Extra oil or butter") }
                        )
                        if (uiState.hasAddedOil) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CookingFat.entries.forEach { fat ->
                                    FilterChip(
                                        selected = fat == uiState.cookingFat,
                                        onClick = { actions.onCookingFatChange(fat, uiState.cookingFatGrams) },
                                        enabled = idle,
                                        label = { Text(fat.label) }
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(5f to "1 tsp", 14f to "1 tbsp", 28f to "2 tbsp").forEach { (grams, label) ->
                                    FilterChip(
                                        selected = grams == uiState.cookingFatGrams,
                                        onClick = { actions.onCookingFatChange(uiState.cookingFat, grams) },
                                        enabled = idle,
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = uiState.userNote,
                        onValueChange = actions.onNoteChange,
                        label = { Text("Something missing or wrong?") },
                        placeholder = { Text("Chicken bharta, not paneer · two rotis") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (uiState.userNote.isNotBlank() || uiState.isAnalyzing) {
                        TextButton(onClick = actions.onReanalyze, enabled = idle) {
                            Text(if (uiState.isAnalyzing) "Reading photo…" else "Read the photo again")
                        }
                    }
                }
            }

            KaloButton(
                text = if (uiState.items.any { !it.hasValidPortion }) "Check food amounts" else if (uiState.items.any { !it.isReadyToSave }) "Review food nutrition" else "Save meal",
                onClick = actions.onSave,
                loading = uiState.isSaving,
                enabled = idle && uiState.items.isNotEmpty() && uiState.items.all { it.isReadyToSave }
            )
        }
    }
}

/** One food: name, then an editable amount in g or ml, with its energy beside it. */
@Composable
fun EditableItemRow(
    item: EditableFoodItem,
    onNameChange: (String) -> Unit,
    onAmountInputChange: (String) -> Unit,
    onRemove: () -> Unit,
    onNutritionChange: (Int, Float, Float, Float) -> Unit,
    onKeepNutrition: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    /** When set, offers a fresh Gemini estimate for the name as typed (e.g. after fixing a wrong dish). */
    onReestimate: (() -> Unit)? = null,
    estimating: Boolean = false
) {
    var editNutrition by remember { mutableStateOf(false) }
    if (editNutrition) NutritionDialog(item, { editNutrition = false }, onNutritionChange)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(KaloSurface, RoundedCornerShape(20.dp))
            .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = item.name,
                onValueChange = onNameChange,
                singleLine = true,
                enabled = enabled,
                label = { Text("Food") },
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRemove, enabled = enabled) {
                Icon(Icons.Default.Close, contentDescription = "Remove ${item.name}", tint = KaloTextSecondary)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
            OutlinedTextField(
                value = item.amountText,
                onValueChange = onAmountInputChange,
                singleLine = true,
                enabled = enabled,
                isError = !item.hasValidPortion,
                supportingText = if (!item.hasValidPortion) {
                    { Text("1–5000") }
                } else null,
                label = { Text("Amount") },
                suffix = { Text(item.unit) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                modifier = Modifier.width(140.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(
                    if (item.hasValidPortion) "${item.currentCalories} kcal" else "Check amount",
                    style = KaloTypography.titleMedium.copy(fontFeatureSettings = TabularFontFeature),
                    color = KaloTextPrimary
                )
                Text(
                    if (item.hasValidPortion) "P ${item.currentProtein.toInt()}  C ${item.currentCarbs.toInt()}  F ${item.currentFat.toInt()}" else "Nutrition waits for a valid amount",
                    style = KaloTypography.bodyMedium.copy(fontFeatureSettings = TabularFontFeature),
                    color = KaloTextSecondary
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { editNutrition = true }, enabled = enabled && item.hasValidPortion) { Text("Edit nutrition") }
            if (onReestimate != null) {
                TextButton(onClick = onReestimate, enabled = enabled && item.name.isNotBlank() && item.hasValidPortion) {
                    Text(if (estimating) "Estimating…" else "Update nutrition")
                }
            }
        }
        if (item.needsNutritionReview) {
            Text("Name changed. These values still belong to “${item.nutritionBasisName}”. Update or edit the nutrition, or confirm that the existing values still apply.",
                style = KaloTypography.bodyMedium, color = KaloTextSecondary, modifier = Modifier.padding(end = 12.dp))
            TextButton(onClick = onKeepNutrition, enabled = enabled && item.hasValidPortion && item.name.isNotBlank()) {
                Text("Keep these values")
            }
        } else if (onReestimate != null) {
            Text("Correct the food name above, then tap Update nutrition to estimate it again.",
                style = KaloTypography.bodySmall, color = KaloTextSecondary)
        }
    }
}
