package com.kalotracker.app.core.data.repository

import com.kalotracker.app.core.database.dao.WorkoutDao
import com.kalotracker.app.core.database.dao.WorkoutWithSets
import com.kalotracker.app.core.database.entity.ExerciseSetEntity
import com.kalotracker.app.core.database.entity.WorkoutEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class WorkoutRepository(
    private val workoutDao: WorkoutDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    fun getWorkoutsForDay(startOfDay: Long, endOfDay: Long): Flow<List<WorkoutWithSets>> {
        return workoutDao.getWorkoutsForDay(startOfDay, endOfDay)
    }

    suspend fun getWorkoutsBetween(start: Long, end: Long): List<WorkoutWithSets> =
        withContext(dispatcher) { workoutDao.getWorkoutsBetween(start, end) }

    suspend fun getRecentWorkouts(limit: Int = 10): List<WorkoutWithSets> =
        withContext(dispatcher) { workoutDao.getRecentWorkouts(limit) }

    /** Duplicates a past workout onto the requested timestamp with fresh workout and set IDs. */
    suspend fun duplicateWorkout(source: WorkoutWithSets, timestamp: Long = System.currentTimeMillis()) =
        withContext(dispatcher) {
            val newWorkoutId = java.util.UUID.randomUUID().toString()
            val newWorkout = source.workout.copy(id = newWorkoutId, timestamp = timestamp)
            val newSets = source.sets.map { set ->
                set.copy(id = java.util.UUID.randomUUID().toString(), workoutId = newWorkoutId, isCompleted = true)
            }
            workoutDao.insertWorkoutWithSets(newWorkout, newSets)
        }

    suspend fun getLastSessionSetsForExercise(exerciseName: String): List<ExerciseSetEntity> =
        withContext(dispatcher) {
            workoutDao.getLastSessionSetsForExercise(exerciseName.trim())
        }

    suspend fun getWorkout(id: String) = withContext(dispatcher) { workoutDao.getWorkoutById(id) }
    suspend fun updateWorkout(workout: WorkoutEntity, sets: List<ExerciseSetEntity>) = withContext(dispatcher) { workoutDao.replaceWorkoutWithSets(workout, sets) }

    suspend fun saveWorkout(workout: WorkoutEntity, sets: List<ExerciseSetEntity>) =
        withContext(dispatcher) {
            workoutDao.insertWorkoutWithSets(workout, sets)
        }

    suspend fun deleteWorkout(workout: WorkoutEntity) = withContext(dispatcher) {
        workoutDao.deleteExerciseSetsByWorkoutId(workout.id)
        workoutDao.deleteWorkout(workout)
    }

    /** Puts back a workout that was just deleted (undo). */
    suspend fun restoreWorkout(workoutWithSets: WorkoutWithSets) = withContext(dispatcher) {
        workoutDao.insertWorkoutWithSets(workoutWithSets.workout, workoutWithSets.sets)
    }
}
