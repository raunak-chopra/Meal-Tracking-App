package com.kalotracker.app.core.data.repository

import com.kalotracker.app.core.database.dao.WorkoutDao
import com.kalotracker.app.core.database.dao.WorkoutWithSets
import com.kalotracker.app.core.database.entity.ExerciseSetEntity
import com.kalotracker.app.core.database.entity.WorkoutEntity
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
data class RemoteWorkoutInsert(
    val id: String,
    val user_id: String,
    val title: String,
    val type: String,
    val duration_minutes: Int,
    val estimated_calories_burned: Int
)

@Serializable
data class RemoteExerciseSetInsert(
    val id: String,
    val workout_id: String,
    val exercise_name: String,
    val set_number: Int,
    val weight_kg: Float,
    val reps: Int,
    val is_completed: Boolean = true
)

class WorkoutRepository(
    private val workoutDao: WorkoutDao
) {

    fun getWorkoutsForDay(startOfDay: Long, endOfDay: Long): Flow<List<WorkoutWithSets>> {
        return workoutDao.getWorkoutsForDay(startOfDay, endOfDay)
    }

    suspend fun getLastSessionSetsForExercise(exerciseName: String): List<ExerciseSetEntity> =
        withContext(Dispatchers.IO) {
            workoutDao.getLastSessionSetsForExercise(exerciseName)
        }

    suspend fun saveWorkout(workout: WorkoutEntity, sets: List<ExerciseSetEntity>) =
        withContext(Dispatchers.IO) {
            workoutDao.insertWorkoutWithSets(workout, sets)
            // Fire-and-forget background cloud sync
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { syncPendingWorkouts() }
        }

    suspend fun deleteWorkout(workout: WorkoutEntity) = withContext(Dispatchers.IO) {
        workoutDao.deleteExerciseSetsByWorkoutId(workout.id)
        workoutDao.deleteWorkout(workout)
        if (SupabaseModule.isConfigured) {
            try {
                val user = SupabaseModule.client.auth.currentUserOrNull()
                if (user != null) {
                    SupabaseModule.client.from("workouts").delete {
                        filter {
                            eq("id", workout.id)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun syncPendingWorkouts(): Result<Int> = withContext(Dispatchers.IO) {
        if (!SupabaseModule.isConfigured) return@withContext Result.success(0)
        try {
            val user = SupabaseModule.client.auth.currentUserOrNull()
                ?: return@withContext Result.success(0)
            val pending = workoutDao.getPendingSyncWorkoutsWithSets()
            var synced = 0
            for (workoutWithSets in pending) {
                val workout = workoutWithSets.workout
                try {
                    val remote = RemoteWorkoutInsert(
                        id = workout.id,
                        user_id = user.id,
                        title = workout.title,
                        type = workout.type,
                        duration_minutes = workout.durationMinutes,
                        estimated_calories_burned = workout.estimatedCaloriesBurned
                    )
                    SupabaseModule.client.from("workouts").upsert(remote)

                    for (set in workoutWithSets.sets) {
                        val remoteSet = RemoteExerciseSetInsert(
                            id = set.id,
                            workout_id = workout.id,
                            exercise_name = set.exerciseName,
                            set_number = set.setNumber,
                            weight_kg = set.weightKg,
                            reps = set.reps,
                            is_completed = set.isCompleted
                        )
                        SupabaseModule.client.from("exercise_sets").upsert(remoteSet)
                    }

                    workoutDao.updateWorkout(workout.copy(syncStatus = "SYNCED"))
                    synced++
                } catch (_: Exception) { }
            }
            Result.success(synced)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
