package com.kalotracker.app.core.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.kalotracker.app.core.ai.GoalType

data class UserProfile(
    val targetCalories: Int = 2200,
    val targetProtein: Int = 160,
    val targetCarbs: Int = 220,
    val targetFat: Int = 70,
    val targetSteps: Long = 10000L,
    val targetWaterMl: Int = 2500,
    val goal: GoalType = GoalType.MAINTAIN
)

enum class MacroPreset(val title: String, val proteinPct: Int, val carbsPct: Int, val fatPct: Int) {
    BALANCED("Balanced (30/45/25)", 30, 45, 25),
    HIGH_PROTEIN("High Protein (40/35/25)", 40, 35, 25),
    LOW_CARB("Low Carb (35/20/45)", 35, 20, 45),
    KETO("Keto (25/5/70)", 25, 5, 70),
    CUSTOM("Custom", 0, 0, 0)
}

class UserProfileRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("kalo_user_prefs", Context.MODE_PRIVATE)

    private val _profile = MutableStateFlow(loadProfileFromPrefs())
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    private fun loadProfileFromPrefs(): UserProfile {
        return UserProfile(
            targetCalories = prefs.getInt("target_calories", 2200),
            targetProtein = prefs.getInt("target_protein", 160),
            targetCarbs = prefs.getInt("target_carbs", 220),
            targetFat = prefs.getInt("target_fat", 70),
            targetSteps = prefs.getLong("target_steps", 10000L),
            targetWaterMl = prefs.getInt("target_water_ml", 2500),
            goal = runCatching { GoalType.valueOf(prefs.getString("goal", null) ?: "") }
                .getOrDefault(GoalType.MAINTAIN)
        )
    }

    fun updateTargets(
        calories: Int,
        protein: Int,
        carbs: Int,
        fat: Int,
        steps: Long,
        waterMl: Int = _profile.value.targetWaterMl
    ) {
        val updated = UserProfile(
            targetCalories = calories,
            targetProtein = protein,
            targetCarbs = carbs,
            targetFat = fat,
            targetSteps = steps,
            targetWaterMl = waterMl,
            goal = _profile.value.goal
        )
        prefs.edit()
            .putInt("target_calories", calories)
            .putInt("target_protein", protein)
            .putInt("target_carbs", carbs)
            .putInt("target_fat", fat)
            .putLong("target_steps", steps)
            .putInt("target_water_ml", waterMl)
            .apply()

        _profile.value = updated
    }

    fun setGoal(goal: GoalType) {
        prefs.edit().putString("goal", goal.name).apply()
        _profile.value = _profile.value.copy(goal = goal)
    }

    /** Changes only the calorie target, rescaling macros to keep the current split. */
    fun setCalorieTargetKeepingSplit(newCalories: Int) {
        val p = _profile.value
        if (p.targetCalories <= 0) return
        val ratio = newCalories.toFloat() / p.targetCalories
        updateTargets(
            calories = newCalories,
            protein = p.targetProtein,
            carbs = (p.targetCarbs * ratio).toInt(),
            fat = (p.targetFat * ratio).toInt(),
            steps = p.targetSteps,
            waterMl = p.targetWaterMl
        )
    }

    fun applyPreset(preset: MacroPreset, totalCalories: Int) {
        if (preset == MacroPreset.CUSTOM) return
        // Protein: 4 kcal/g, Carbs: 4 kcal/g, Fat: 9 kcal/g
        val proteinGrams = ((totalCalories * (preset.proteinPct / 100f)) / 4f).toInt()
        val carbsGrams = ((totalCalories * (preset.carbsPct / 100f)) / 4f).toInt()
        val fatGrams = ((totalCalories * (preset.fatPct / 100f)) / 9f).toInt()

        updateTargets(
            calories = totalCalories,
            protein = proteinGrams,
            carbs = carbsGrams,
            fat = fatGrams,
            steps = _profile.value.targetSteps
        )
    }
}
