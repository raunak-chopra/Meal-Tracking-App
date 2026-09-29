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

    @Transaction
    @Query("SELECT * FROM workouts WHERE timestamp >= :start AND timestamp <= :end ORDER BY timestamp ASC")
    suspend fun getWorkoutsBetween(start: Long, end: Long): List<WorkoutWithSets>

    @Transaction
    @Query("SELECT * FROM workouts ORDER BY timestamp ASC")
    suspend fun getAllWorkouts(): List<WorkoutWithSets>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkouts(workouts: List<WorkoutEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseSets(sets: List<ExerciseSetEntity>)

    @Transaction
    suspend fun insertWorkoutWithSets(workout: WorkoutEntity, sets: List<ExerciseSetEntity>) {
        insertWorkout(workout)
        insertExerciseSets(sets)
    }

    /** Sets from the most recent workout (by time) that contained this exercise. */
    @Query(
        """
        SELECT s.* FROM exercise_sets s
        WHERE s.exerciseName = :exerciseName COLLATE NOCASE
          AND s.workoutId = (
            SELECT w.id FROM workouts w
            JOIN exercise_sets s2 ON s2.workoutId = w.id
            WHERE s2.exerciseName = :exerciseName COLLATE NOCASE
            ORDER BY w.timestamp DESC LIMIT 1
          )
        ORDER BY s.setNumber ASC
        """
    )
    suspend fun getLastSessionSetsForExercise(exerciseName: String): List<ExerciseSetEntity>

    @Delete
    suspend fun deleteWorkout(workout: WorkoutEntity)

    @Query("DELETE FROM exercise_sets WHERE workoutId = :workoutId")
    suspend fun deleteExerciseSetsByWorkoutId(workoutId: String)

    @Query("DELETE FROM exercise_sets")
    suspend fun deleteAllExerciseSets()

    @Query("DELETE FROM workouts")
    suspend fun deleteAllWorkouts()
}
