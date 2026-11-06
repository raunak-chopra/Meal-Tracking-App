package com.kalotracker.app.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DetectedFoodItem(
    @SerialName("name") val name: String,
    @SerialName("portion_grams") val portionGrams: Float,
    @SerialName("calories") val calories: Int,
    @SerialName("protein") val protein: Float,
    @SerialName("carbs") val carbs: Float,
    @SerialName("fat") val fat: Float,
    @SerialName("confidence") val confidence: Float = 0.7f,
    /** "g" for solids, "ml" for drinks and pourable liquids; portionGrams holds the amount in this unit. */
    @SerialName("unit") val unit: String = "g"
)

/** Grams, millilitres or "serving" (a quick entry with no weight); anything else falls back to grams. */
fun portionUnit(raw: String?): String = when (raw?.trim()?.lowercase()) {
    "ml", "millilitre", "milliliter", "millilitres", "milliliters" -> "ml"
    "serving" -> "serving"
    else -> "g"
}

/** Raw model output. Totals are intentionally not part of it; they are computed from items. */
@Serializable
data class MealAnalysisResponse(
    @SerialName("is_food") val isFood: Boolean = true,
    @SerialName("meal_title") val mealTitle: String = "",
    @SerialName("items") val items: List<DetectedFoodItem> = emptyList(),
    @SerialName("confidence") val confidence: Float = 0.7f,
    @SerialName("estimation_notes") val estimationNotes: String? = null
)

sealed class MealAnalysisException(message: String) : Exception(message) {
    class NotConfigured : MealAnalysisException("Add a Gemini key in Settings to estimate meals from photos or descriptions.")
    class Offline : MealAnalysisException("No internet connection. You can still log this meal manually.")
    class NotFood(message: String = "No food detected in this photo. Try a closer, well-lit shot.") : MealAnalysisException(message)
    class BadKey(detail: String) : MealAnalysisException("Gemini rejected the request: $detail")
    class RateLimited : MealAnalysisException("Gemini rate limit reached. Wait a minute and try again.")
    class BadResponse(detail: String) : MealAnalysisException("Couldn't read the meal result ($detail). Try again, or enter it manually.")
    class Other(detail: String) : MealAnalysisException(detail)
}
