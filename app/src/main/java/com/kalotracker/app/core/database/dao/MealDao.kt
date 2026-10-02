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

    @Transaction
    @Query("SELECT * FROM meals WHERE timestamp >= :start AND timestamp <= :end ORDER BY timestamp ASC")
    suspend fun getMealsBetween(start: Long, end: Long): List<MealWithItems>

    @Transaction
    @Query("SELECT * FROM meals ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMeals(limit: Int): List<MealWithItems>

    @Transaction
    @Query("SELECT * FROM meals ORDER BY timestamp ASC")
    suspend fun getAllMeals(): List<MealWithItems>

    @Transaction
    @Query("SELECT * FROM meals WHERE id = :id")
    suspend fun getMealById(id: String): MealWithItems?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodItems(items: List<FoodItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeals(meals: List<MealEntity>)

    @Transaction
    suspend fun insertMealWithItems(meal: MealEntity, items: List<FoodItemEntity>) {
        insertMeal(meal)
        insertFoodItems(items)
    }

    /** Replaces a meal together with its items in one transaction (used by edit). */
    @Transaction
    suspend fun replaceMealWithItems(meal: MealEntity, items: List<FoodItemEntity>) {
        deleteFoodItemsByMealId(meal.id)
        insertMeal(meal)
        insertFoodItems(items)
    }

    @Delete
    suspend fun deleteMeal(meal: MealEntity)

    @Query("DELETE FROM food_items WHERE mealId = :mealId")
    suspend fun deleteFoodItemsByMealId(mealId: String)

    @Query("DELETE FROM food_items")
    suspend fun deleteAllFoodItems()

    @Query("DELETE FROM meals")
    suspend fun deleteAllMeals()
}
