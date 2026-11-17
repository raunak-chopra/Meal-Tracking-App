package com.kalotracker.app.core.ai

data class NutritionInsight(
    val title: String,
    val description: String,
    val type: InsightType
)

enum class InsightType {
    PROTEIN,
    HYDRATION
}

/**
 * At most one short observation for the day, or null when there is nothing useful to add.
 *
 * Calories already show their own logged / target / delta, so they never get a card.
 * Exercise is never suggested as a way to "balance" food.
 */
object NutritionInsightEngine {

    fun generateDailyInsight(
        targetCalories: Int,
        consumedCalories: Int,
        targetProtein: Int,
        consumedProtein: Int,
        targetWaterMl: Int,
        consumedWaterMl: Int,
        isToday: Boolean = true
    ): NutritionInsight? {
        // Too early in the day (or nothing logged) to say anything fair.
        if (targetCalories <= 0 || consumedCalories < targetCalories * 0.5f) return null
        val soFar = if (isToday) " so far" else ""

        if (targetProtein > 0 && consumedProtein < targetProtein * 0.6f) {
            return NutritionInsight(
                title = "Protein is behind",
                description = "$consumedProtein g of your $targetProtein g target$soFar, with ${percent(consumedCalories, targetCalories)}% of your energy logged.",
                type = InsightType.PROTEIN
            )
        }

        if (targetWaterMl > 0 && consumedWaterMl < targetWaterMl * 0.4f) {
            return NutritionInsight(
                title = "Water is low",
                description = "$consumedWaterMl ml of your $targetWaterMl ml target$soFar.",
                type = InsightType.HYDRATION
            )
        }
        return null
    }

    private fun percent(part: Int, whole: Int) = (part * 100f / whole).toInt()
}
