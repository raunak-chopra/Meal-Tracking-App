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
    @SerialName("confidence") val confidence: Float = 0.9f
)

@Serializable
data class MealAnalysisResponse(
    @SerialName("meal_title") val mealTitle: String,
    @SerialName("items") val items: List<DetectedFoodItem>,
    @SerialName("total_calories") val totalCalories: Int,
    @SerialName("total_protein") val totalProtein: Float,
    @SerialName("total_carbs") val totalCarbs: Float,
    @SerialName("total_fat") val totalFat: Float,
    @SerialName("confidence") val confidence: Float = 0.85f,
    @SerialName("estimation_notes") val estimationNotes: String? = null
)

@Serializable
data class MealAnalysisRequest(
    @SerialName("image_base64") val imageBase64: String,
    @SerialName("has_added_oil") val hasAddedOil: Boolean = false,
    @SerialName("user_note") val userNote: String? = null
)
