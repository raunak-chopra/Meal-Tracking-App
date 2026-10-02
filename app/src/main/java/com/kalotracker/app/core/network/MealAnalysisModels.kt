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
    @SerialName("confidence") val confidence: Float = 0.7f
)

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
    class NotConfigured : MealAnalysisException("Add your Gemini API key in Settings to scan meals.")
    class Offline : MealAnalysisException("No internet connection. You can still log this meal manually.")
    class NotFood : MealAnalysisException("No food detected in this photo. Try a closer, well-lit shot.")
    class BadKey(detail: String) : MealAnalysisException("Gemini rejected the request: $detail")
    class RateLimited : MealAnalysisException("Gemini rate limit reached. Wait a minute and try again.")
    class BadResponse(detail: String) : MealAnalysisException("Couldn't read the AI response ($detail). Try again.")
    class Other(detail: String) : MealAnalysisException(detail)
}
