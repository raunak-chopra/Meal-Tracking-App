package com.kalotracker.app.core.database.dao

import androidx.room.*
import com.kalotracker.app.core.database.entity.ExerciseSetEntity
import com.kalotracker.app.core.database.entity.WorkoutEntity
import kotlinx.coroutines.flow.Flow

data class WorkoutWithSets(
    @Embedded val workout: WorkoutEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "workoutId"
    )
    val sets: List<ExerciseSetEntity>
)

@Dao
interface WorkoutDao {
    @Transaction
    @Query("SELECT * FROM workouts WHERE timestamp >= :startOfDay AND timestamp <= :endOfDay ORDER BY timestamp DESC")
    fun getWorkoutsForDay(startOfDay: Long, endOfDay: Long): Flow<List<WorkoutWithSets>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseSets(sets: List<ExerciseSetEntity>)

    @Transaction
    suspend fun insertWorkoutWithSets(workout: WorkoutEntity, sets: List<ExerciseSetEntity>) {
        insertWorkout(workout)
        insertExerciseSets(sets)
    }

    @Query("SELECT * FROM exercise_sets WHERE exerciseName = :exerciseName ORDER BY id DESC LIMIT :limit")
    suspend fun getLastSessionSetsForExercise(exerciseName: String, limit: Int = 5): List<ExerciseSetEntity>

    @Query("SELECT * FROM workouts WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSyncWorkouts(): List<WorkoutEntity>

    @Transaction
    @Query("SELECT * FROM workouts WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSyncWorkoutsWithSets(): List<WorkoutWithSets>

    @Delete
    suspend fun deleteWorkout(workout: WorkoutEntity)

    @Query("DELETE FROM exercise_sets WHERE workoutId = :workoutId")
    suspend fun deleteExerciseSetsByWorkoutId(workoutId: String)

    @Update
    suspend fun updateWorkout(workout: WorkoutEntity)
}
