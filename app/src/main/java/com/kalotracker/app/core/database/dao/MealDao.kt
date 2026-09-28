package com.kalotracker.app.core.database.dao

import androidx.room.*
import com.kalotracker.app.core.database.entity.FoodItemEntity
import com.kalotracker.app.core.database.entity.MealEntity
import kotlinx.coroutines.flow.Flow

data class MealWithItems(
    @Embedded val meal: MealEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "mealId"
    )
    val items: List<FoodItemEntity>
)

@Dao
interface MealDao {
    @Transaction
    @Query("SELECT * FROM meals WHERE timestamp >= :startOfDay AND timestamp <= :endOfDay ORDER BY timestamp DESC")
    fun getMealsForDay(startOfDay: Long, endOfDay: Long): Flow<List<MealWithItems>>

    @Transaction
    @Query("SELECT * FROM meals WHERE timestamp >= :startOfDay AND timestamp <= :endOfDay ORDER BY timestamp DESC")
    suspend fun getMealsForDayOnce(startOfDay: Long, endOfDay: Long): List<MealWithItems>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodItems(items: List<FoodItemEntity>)

    @Transaction
    suspend fun insertMealWithItems(meal: MealEntity, items: List<FoodItemEntity>) {
        insertMeal(meal)
        insertFoodItems(items)
    }

    @Delete
    suspend fun deleteMeal(meal: MealEntity)

    @Query("DELETE FROM food_items WHERE mealId = :mealId")
    suspend fun deleteFoodItemsByMealId(mealId: String)

    @Query("SELECT * FROM meals WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSyncMeals(): List<MealEntity>

    @Transaction
    @Query("SELECT * FROM meals WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSyncMealsWithItems(): List<MealWithItems>
}
