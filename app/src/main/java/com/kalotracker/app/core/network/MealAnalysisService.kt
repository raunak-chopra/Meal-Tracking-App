package com.kalotracker.app.core.network

import android.util.Base64
import io.github.jan.supabase.functions.functions
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class MealAnalysisService {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun analyzeMealImage(
        imageBytes: ByteArray,
        hasAddedOil: Boolean = false,
        userNote: String? = null
    ): Result<MealAnalysisResponse> = withContext(Dispatchers.IO) {
        try {
            // Check if Supabase credentials are still default placeholders
            if (!SupabaseModule.isConfigured) {
                // Return a realistic mock response for rapid offline testing
                return@withContext Result.success(getMockAnalysisResult(hasAddedOil))
            }

            val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            val requestBody = MealAnalysisRequest(
                imageBase64 = base64Image,
                hasAddedOil = hasAddedOil,
                userNote = userNote
            )

            val responseString = SupabaseModule.client.functions.invoke(
                function = "analyze-meal",
                body = requestBody
            ).bodyAsText()

            val parsed = json.decodeFromString<MealAnalysisResponse>(responseString)
            Result.success(parsed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getMockAnalysisResult(hasAddedOil: Boolean): MealAnalysisResponse {
        val oilCal = if (hasAddedOil) 120 else 0
        val oilFat = if (hasAddedOil) 14f else 0f

        val items = listOf(
            DetectedFoodItem(
                name = "Grilled Chicken Breast",
                portionGrams = 180f,
                calories = 297,
                protein = 55.8f,
                carbs = 0.0f,
                fat = 6.5f,
                confidence = 0.94f
            ),
            DetectedFoodItem(
                name = "Steamed Jasmine Rice",
                portionGrams = 160f,
                calories = 208,
                protein = 4.2f,
                carbs = 45.0f,
                fat = 0.4f,
                confidence = 0.91f
            ),
            DetectedFoodItem(
                name = "Steamed Broccoli Florets",
                portionGrams = 90f,
                calories = 31,
                protein = 2.5f,
                carbs = 6.0f,
                fat = 0.3f,
                confidence = 0.88f
            )
        )

        val totalCalories = items.sumOf { it.calories } + oilCal
        val totalProtein = items.map { it.protein }.sum()
        val totalCarbs = items.map { it.carbs }.sum()
        val totalFat = items.map { it.fat }.sum() + oilFat

        return MealAnalysisResponse(
            mealTitle = "Chicken Breast with Rice & Broccoli",
            items = items,
            totalCalories = totalCalories,
            totalProtein = totalProtein,
            totalCarbs = totalCarbs,
            totalFat = totalFat,
            confidence = 0.91f,
            estimationNotes = if (hasAddedOil) "Added ~1 tbsp cooking oil (+120 kcal)." else "Dry-grilled and steamed portion estimates."
        )
    }
}
