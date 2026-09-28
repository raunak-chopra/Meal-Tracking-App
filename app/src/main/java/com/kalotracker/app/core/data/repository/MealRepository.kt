package com.kalotracker.app.core.data.repository

import com.kalotracker.app.core.database.dao.MealDao
import com.kalotracker.app.core.database.dao.MealWithItems
import com.kalotracker.app.core.database.entity.FoodItemEntity
import com.kalotracker.app.core.database.entity.MealEntity
import com.kalotracker.app.core.network.SupabaseModule
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class RemoteMealInsert(
    val id: String,
    val user_id: String,
    val title: String,
    val total_calories: Int,
    val total_protein: Float,
    val total_carbs: Float,
    val total_fat: Float,
    val image_remote_url: String? = null,
    val notes: String? = null
)

@Serializable
data class RemoteFoodItemInsert(
    val id: String,
    val meal_id: String,
    val name: String,
    val portion_grams: Float,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val confidence: Float = 1.0f
)

class MealRepository(
    private val mealDao: MealDao
) {

    fun getMealsForDay(startOfDay: Long, endOfDay: Long): Flow<List<MealWithItems>> {
        return mealDao.getMealsForDay(startOfDay, endOfDay)
    }

    suspend fun saveMeal(meal: MealEntity, items: List<FoodItemEntity>) = withContext(Dispatchers.IO) {
        mealDao.insertMealWithItems(meal, items)
        // Fire-and-forget cloud sync — doesn't block the save operation
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { syncPendingMeals() }
    }

    suspend fun deleteMeal(meal: MealEntity) = withContext(Dispatchers.IO) {
        mealDao.deleteFoodItemsByMealId(meal.id)
        mealDao.deleteMeal(meal)
        if (SupabaseModule.isConfigured) {
            try {
                val user = SupabaseModule.client.auth.currentUserOrNull()
                if (user != null) {
                    SupabaseModule.client.from("meals").delete {
                        filter {
                            eq("id", meal.id)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun syncPendingMeals(): Result<Int> = withContext(Dispatchers.IO) {
        if (!SupabaseModule.isConfigured) return@withContext Result.success(0)

        try {
            val user = SupabaseModule.client.auth.currentUserOrNull() ?: return@withContext Result.success(0)
            val pendingMeals = mealDao.getPendingSyncMealsWithItems()
            if (pendingMeals.isEmpty()) return@withContext Result.success(0)

            var syncedCount = 0
            for (mealWithItems in pendingMeals) {
                val meal = mealWithItems.meal
                val remoteMeal = RemoteMealInsert(
                    id = meal.id,
                    user_id = user.id,
                    title = meal.title,
                    total_calories = meal.totalCalories,
                    total_protein = meal.totalProteinGrams,
                    total_carbs = meal.totalCarbsGrams,
                    total_fat = meal.totalFatGrams,
                    image_remote_url = meal.imageRemoteUrl,
                    notes = meal.notes
                )
                SupabaseModule.client.from("meals").upsert(remoteMeal)

                for (item in mealWithItems.items) {
                    val remoteItem = RemoteFoodItemInsert(
                        id = item.id,
                        meal_id = meal.id,
                        name = item.name,
                        portion_grams = item.portionGrams,
                        calories = item.calories,
                        protein = item.protein,
                        carbs = item.carbs,
                        fat = item.fat,
                        confidence = item.confidence
                    )
                    SupabaseModule.client.from("food_items").upsert(remoteItem)
                }

                mealDao.insertMeal(meal.copy(syncStatus = "SYNCED"))
                syncedCount++
            }
            Result.success(syncedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
