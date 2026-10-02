package com.kalotracker.app.core.database.dao

import androidx.room.*
import com.kalotracker.app.core.database.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalDao {
    @Query("SELECT * FROM day_status ORDER BY date") fun observeDays(): Flow<List<DayStatusEntity>>
    @Query("SELECT * FROM day_status ORDER BY date") suspend fun days(): List<DayStatusEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putDays(rows: List<DayStatusEntity>)
    @Query("SELECT * FROM goal_history ORDER BY date") fun observeGoals(): Flow<List<GoalHistoryEntity>>
    @Query("SELECT * FROM goal_history ORDER BY date") suspend fun goals(): List<GoalHistoryEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putGoals(rows: List<GoalHistoryEntity>)
    @Query("SELECT * FROM saved_foods ORDER BY favorite DESC, name COLLATE NOCASE") fun observeFoods(): Flow<List<SavedFoodEntity>>
    @Query("SELECT * FROM saved_foods ORDER BY name") suspend fun foods(): List<SavedFoodEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putFoods(rows: List<SavedFoodEntity>)
    @Query("DELETE FROM saved_foods WHERE id = :id") suspend fun deleteFood(id: String)
    @Query("SELECT * FROM barcode_cache WHERE barcode = :barcode") suspend fun cachedBarcode(barcode: String): BarcodeCacheEntity?
    @Query("SELECT * FROM barcode_cache") suspend fun barcodes(): List<BarcodeCacheEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putBarcodes(rows: List<BarcodeCacheEntity>)
    @Query("SELECT * FROM workout_routines ORDER BY name") fun observeRoutines(): Flow<List<WorkoutRoutineEntity>>
    @Query("SELECT * FROM workout_routines ORDER BY name") suspend fun routines(): List<WorkoutRoutineEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putRoutines(rows: List<WorkoutRoutineEntity>)
    @Query("DELETE FROM workout_routines WHERE id = :id") suspend fun deleteRoutine(id: String)
    @Query("DELETE FROM day_status") suspend fun clearDays()
    @Query("DELETE FROM goal_history") suspend fun clearGoals()
    @Query("DELETE FROM saved_foods") suspend fun clearFoods()
    @Query("DELETE FROM barcode_cache") suspend fun clearBarcodes()
    @Query("DELETE FROM workout_routines") suspend fun clearRoutines()
}
