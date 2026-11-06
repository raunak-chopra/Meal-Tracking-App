package com.kalotracker.app.feature.meal

import com.kalotracker.app.core.database.entity.FoodItemEntity
import com.kalotracker.app.core.network.DetectedFoodItem
import com.kalotracker.app.core.network.MealAnalysisResponse
import com.kalotracker.app.core.network.portionUnit
import java.util.UUID

/** A Gemini item as an editable row; nutrition is kept per gram (or ml) so amounts can change. */
internal fun DetectedFoodItem.toEditable(): EditableFoodItem {
    val grams = portionGrams.coerceAtLeast(1f)
    return EditableFoodItem(
        name = name,
        portionGrams = grams,
        baseCaloriesPerGram = calories / grams,
        baseProteinPerGram = protein / grams,
        baseCarbsPerGram = carbs / grams,
        baseFatPerGram = fat / grams,
        confidence = confidence,
        unit = portionUnit(unit)
    )
}

/** A Gemini item ready for the manual meal draft. */
internal fun DetectedFoodItem.toFoodItem(): FoodItemEntity = FoodItemEntity(
    id = UUID.randomUUID().toString(),
    mealId = "",
    name = name,
    portionGrams = portionGrams.coerceAtLeast(1f),
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat,
    confidence = confidence,
    unit = portionUnit(unit)
)

/** What is sent to Gemini when one food is re-estimated from its (corrected) name. */
internal fun EditableFoodItem.estimateDescription(): String =
    "${name.trim()}, ${formatAmount(portionGrams, unit).trim()}"

/**
 * Keeps the user's name and amount and takes nutrition density from the estimate. Several
 * returned parts (e.g. "rice and dal") are combined; their total amount sets the density.
 */
internal fun EditableFoodItem.withEstimate(response: MealAnalysisResponse): EditableFoodItem {
    val parts = response.items
    require(parts.isNotEmpty())
    require(parts.all { portionUnit(it.unit) == unit }) {
        "The estimate changed portion units. Edit nutrition using your label or keep the current values; grams and millilitres cannot be combined automatically."
    }
    val amount = parts.sumOf { it.portionGrams.toDouble() }.toFloat().coerceAtLeast(1f)
    return copy(
        baseCaloriesPerGram = parts.sumOf { it.calories } / amount,
        baseProteinPerGram = parts.sumOf { it.protein.toDouble() }.toFloat() / amount,
        baseCarbsPerGram = parts.sumOf { it.carbs.toDouble() }.toFloat() / amount,
        baseFatPerGram = parts.sumOf { it.fat.toDouble() }.toFloat() / amount,
        confidence = response.confidence,
        nutritionBasisName = name
    )
}
