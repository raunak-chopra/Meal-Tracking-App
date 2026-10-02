package com.kalotracker.app.core.data.repository

import com.kalotracker.app.core.database.dao.MealDao
import com.kalotracker.app.core.database.dao.MealWithItems
import com.kalotracker.app.core.database.entity.FoodItemEntity
import com.kalotracker.app.core.database.entity.MealEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class MealRepository(
    private val mealDao: MealDao,
    private val dispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO
) {

    fun getMealsForDay(startOfDay: Long, endOfDay: Long): Flow<List<MealWithItems>> {
        return mealDao.getMealsForDay(startOfDay, endOfDay)
    }

    suspend fun getMealsBetween(start: Long, end: Long): List<MealWithItems> =
        withContext(dispatcher) { mealDao.getMealsBetween(start, end) }

    suspend fun getMeal(id: String): MealWithItems? =
        withContext(dispatcher) { mealDao.getMealById(id) }

    suspend fun saveMeal(meal: MealEntity, items: List<FoodItemEntity>) = withContext(dispatcher) {
        mealDao.insertMealWithItems(meal, items)
    }

    /** Overwrites an existing meal and all of its items atomically. */
    suspend fun updateMeal(meal: MealEntity, items: List<FoodItemEntity>) = withContext(dispatcher) {
        mealDao.replaceMealWithItems(meal, items)
    }

    suspend fun deleteMeal(meal: MealEntity) = withContext(dispatcher) {
        mealDao.deleteFoodItemsByMealId(meal.id)
        mealDao.deleteMeal(meal)
    }

    /** Puts back a meal that was just deleted (undo). */
    suspend fun restoreMeal(mealWithItems: MealWithItems) = withContext(dispatcher) {
        mealDao.insertMealWithItems(mealWithItems.meal, mealWithItems.items)
    }

    /**
     * Most recent distinct meals (by title), newest first, so frequent meals are one tap to log again.
     */
    suspend fun getRecentDistinctMeals(limit: Int = 12): List<MealWithItems> = withContext(dispatcher) {
        mealDao.getRecentMeals(200)
            .distinctBy { it.meal.title.trim().lowercase() }
            .take(limit)
    }

    /** Logs a copy of an existing meal at [timestamp] (default: now). Photo is not copied. */
    suspend fun duplicateMeal(source: MealWithItems, timestamp: Long = System.currentTimeMillis()): String =
        withContext(dispatcher) {
            val newId = UUID.randomUUID().toString()
            val meal = source.meal.copy(id = newId, timestamp = timestamp, imageLocalUri = null)
            val items = source.items.map { it.copy(id = UUID.randomUUID().toString(), mealId = newId) }
            mealDao.insertMealWithItems(meal, items)
            newId
        }
}
