package com.kalotracker.app.core.ai

data class NutritionInsight(
    val title: String,
    val description: String,
    val type: InsightType
)

enum class InsightType {
    PROTEIN,
    ENERGY,
    WORKOUT_RECOVERY,
    HYDRATION_OR_BALANCE
}

object NutritionInsightEngine {

    fun generateDailyInsight(
        targetCalories: Int,
        consumedCalories: Int,
        targetProtein: Int,
        consumedProtein: Int,
        targetCarbs: Int,
        consumedCarbs: Int,
        targetFat: Int,
        consumedFat: Int,
        workoutCount: Int,
        activeBurn: Double,
        targetWaterMl: Int = 2500,
        consumedWaterMl: Int = 0
    ): NutritionInsight {
        val remainingCal = targetCalories - consumedCalories
        val remainingProtein = targetProtein - consumedProtein

        // Priority 1: High deficit in protein
        if (consumedCalories > (targetCalories * 0.4f) && remainingProtein > (targetProtein * 0.4f)) {
            return NutritionInsight(
                title = "Protein Deficit Detected",
                description = "You have ${remainingProtein}g of protein left today. Consider adding a high-protein source like 170g nonfat Greek yogurt (~17g P) or a whey shake (~25g P).",
                type = InsightType.PROTEIN
            )
        }

        // Priority 2: Workouts logged with active calorie burn
        if (workoutCount > 0) {
            return NutritionInsight(
                title = "Post-Workout Recovery",
                description = "$workoutCount workout logged (${activeBurn.toInt()} active kcal). Prioritize complex carbs and lean protein to optimize muscle glycogen replenishment and tissue repair.",
                type = InsightType.WORKOUT_RECOVERY
            )
        }

        // Priority 3: Low hydration alert
        if (consumedCalories > (targetCalories * 0.4f) && targetWaterMl > 0 && consumedWaterMl < (targetWaterMl * 0.4f)) {
            val remainingWater = targetWaterMl - consumedWaterMl
            return NutritionInsight(
                title = "Hydration Lag",
                description = "You have logged $consumedWaterMl ml of water today ($remainingWater ml remaining). Boost fluid intake to sustain metabolic rate and workout performance.",
                type = InsightType.HYDRATION_OR_BALANCE
            )
        }

        // Priority 4: Calorie target status
        if (remainingCal in 1..400) {
            return NutritionInsight(
                title = "Optimal Calorie Pacing",
                description = "You have $remainingCal kcal remaining. A balanced, light meal will keep you directly on target for your daily horizon.",
                type = InsightType.ENERGY
            )
        } else if (remainingCal < 0) {
            return NutritionInsight(
                title = "Calorie Surplus",
                description = "You are ${-remainingCal} kcal over your baseline goal. Daily steps and active movement will help balance energy expenditure.",
                type = InsightType.ENERGY
            )
        }

        // Default: balanced
        return NutritionInsight(
            title = "Clean Macro Horizon",
            description = "${consumedCalories} kcal logged across ${consumedProtein}g P • ${consumedCarbs}g C • ${consumedFat}g F. Hydration: $consumedWaterMl ml.",
            type = InsightType.HYDRATION_OR_BALANCE
        )
    }
}
