package com.kalotracker.app.core.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.kalotracker.app.core.network.SupabaseModule
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    @SerialName("daily_calorie_target") val targetCalories: Int = 2200,
    @SerialName("daily_protein_target") val targetProtein: Int = 160,
    @SerialName("daily_carbs_target") val targetCarbs: Int = 220,
    @SerialName("daily_fat_target") val targetFat: Int = 70,
    @SerialName("daily_step_goal") val targetSteps: Long = 10000L,
    @SerialName("daily_water_target") val targetWaterMl: Int = 2500
)

@Serializable
data class RemoteProfileUpsert(
    val id: String,
    val daily_calorie_target: Int,
    val daily_protein_target: Int,
    val daily_carbs_target: Int,
    val daily_fat_target: Int,
    val daily_step_goal: Long
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
            targetWaterMl = prefs.getInt("target_water_ml", 2500)
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
            targetWaterMl = waterMl
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

        // Sync to Supabase in background if user is authenticated
        syncProfileToRemote(updated)
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

    private fun syncProfileToRemote(profile: UserProfile) {
        if (!SupabaseModule.isConfigured) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val user = SupabaseModule.client.auth.currentUserOrNull() ?: return@launch
                val remoteProfile = RemoteProfileUpsert(
                    id = user.id,
                    daily_calorie_target = profile.targetCalories,
                    daily_protein_target = profile.targetProtein,
                    daily_carbs_target = profile.targetCarbs,
                    daily_fat_target = profile.targetFat,
                    daily_step_goal = profile.targetSteps
                )
                SupabaseModule.client.from("profiles").upsert(remoteProfile)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
