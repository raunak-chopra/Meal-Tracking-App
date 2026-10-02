package com.kalotracker.app.core.data.repository

import com.kalotracker.app.core.database.entity.GoalHistoryEntity
import com.kalotracker.app.core.data.food.PersonalFood
import kotlinx.serialization.encodeToString
import java.time.LocalDate
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
    private val _history = MutableStateFlow(runCatching {
        PersonalFood.json.decodeFromString<List<GoalHistoryEntity>>(prefs.getString("goal_history", "[]")!!)
    }.getOrDefault(emptyList()).ifEmpty { listOf(historyRow(_profile.value)) })
    val history: StateFlow<List<GoalHistoryEntity>> = _history.asStateFlow()

    init {
        check(prefs.edit().putString("goal_history", PersonalFood.json.encodeToString(_history.value)).commit()) { "Could not save goal history." }
    }

    fun resetHistory() {
        val rows = listOf(historyRow(_profile.value))
        check(prefs.edit().putString("goal_history", PersonalFood.json.encodeToString(rows)).commit())
        _history.value = rows
    }

    private fun historyRow(p: UserProfile) = GoalHistoryEntity(LocalDate.now().toString(),
        p.targetCalories, p.targetProtein, p.targetCarbs, p.targetFat, p.targetWaterMl, p.goal.name)

    private fun record(p: UserProfile) {
        val row = historyRow(p)
        val updated = (_history.value.filterNot { it.date == row.date } + row).sortedBy { it.date }
        check(prefs.edit().putString("goal_history", PersonalFood.json.encodeToString(updated)).commit()) { "Could not save goal history." }
        _history.value = updated
    }

    fun restoreHistory(rows: List<GoalHistoryEntity>) {
        if (rows.isEmpty()) return
        val updated = (_history.value.filterNot { old -> rows.any { it.date == old.date } } + rows).sortedBy { it.date }
        check(prefs.edit().putString("goal_history", PersonalFood.json.encodeToString(updated)).commit())
        _history.value = updated
    }

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
        if (updated == _profile.value) return
        prefs.edit()
            .putInt("target_calories", calories)
            .putInt("target_protein", protein)
            .putInt("target_carbs", carbs)
            .putInt("target_fat", fat)
            .putLong("target_steps", steps)
            .putInt("target_water_ml", waterMl)
            .apply()

        if (updated.copy(targetSteps = _profile.value.targetSteps) != _profile.value) record(updated)
        _profile.value = updated
    }

    fun setGoal(goal: GoalType) {
        if (goal == _profile.value.goal) return
        prefs.edit().putString("goal", goal.name).apply()
        val updated = _profile.value.copy(goal = goal)
        record(updated)
        _profile.value = updated
    }

    /** Changes only the calorie target, rescaling macros to keep the current split. */
    fun setCalorieTargetKeepingSplit(newCalories: Int) {
        val p = _profile.value
        if (p.targetCalories <= 0) return
        val ratio = newCalories.toFloat() / p.targetCalories
        updateTargets(
            calories = newCalories,
            protein = (p.targetProtein * ratio).toInt(),
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
