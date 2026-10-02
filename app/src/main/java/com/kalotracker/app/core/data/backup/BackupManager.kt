package com.kalotracker.app.core.data.backup

import androidx.room.withTransaction
import com.kalotracker.app.core.ai.GoalType
import com.kalotracker.app.core.data.repository.UserProfileRepository
import com.kalotracker.app.core.database.KaloDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class ImportSummary(val meals: Int, val workouts: Int, val water: Int, val weights: Int) {
    val total: Int get() = meals + workouts + water + weights
}

class BackupManager(
    private val database: KaloDatabase,
    private val profileRepository: UserProfileRepository,
    private val photosDir: File
) {

    suspend fun buildBackup(): BackupFile = withContext(Dispatchers.IO) {
        val p = profileRepository.profile.value
        BackupFile(
            profile = BackupProfile(
                calories = p.targetCalories, protein = p.targetProtein, carbs = p.targetCarbs,
                fat = p.targetFat, steps = p.targetSteps, waterMl = p.targetWaterMl, goal = p.goal.name
            ),
            meals = database.mealDao().getAllMeals().map { BackupCodec.toBackupMeal(it.meal, it.items) },
            workouts = database.workoutDao().getAllWorkouts().map { BackupCodec.toBackupWorkout(it.workout, it.sets) },
            water = database.waterDao().getAll().map { BackupWater(it.id, it.milliliters, it.timestamp) },
            weights = database.weightDao().getAll().map { BackupWeight(it.id, it.weightKg, it.timestamp) }
        )
    }

    /**
     * Adds or updates records by id inside one transaction: either everything imports or nothing does.
     * Records that exist on the phone but not in the file are kept.
     */
    suspend fun import(file: BackupFile): ImportSummary = withContext(Dispatchers.IO) {
        database.withTransaction {
            val mealDao = database.mealDao()
            mealDao.insertMeals(file.meals.map(BackupCodec::toMealEntity))
            mealDao.insertFoodItems(file.meals.flatMap(BackupCodec::toFoodItemEntities))

            val workoutDao = database.workoutDao()
            workoutDao.insertWorkouts(file.workouts.map(BackupCodec::toWorkoutEntity))
            workoutDao.insertExerciseSets(file.workouts.flatMap(BackupCodec::toSetEntities))

            database.waterDao().insertAll(file.water.map(BackupCodec::toWaterEntity))
            database.weightDao().insertAll(file.weights.map(BackupCodec::toWeightEntity))
        }

        file.profile?.let {
            profileRepository.updateTargets(it.calories, it.protein, it.carbs, it.fat, it.steps, it.waterMl)
            runCatching { GoalType.valueOf(it.goal) }.getOrNull()?.let(profileRepository::setGoal)
        }
        ImportSummary(file.meals.size, file.workouts.size, file.water.size, file.weights.size)
    }

    /** Removes every log and meal photo. Goals and the API key are kept. */
    suspend fun deleteAllData() = withContext(Dispatchers.IO) {
        database.withTransaction {
            database.mealDao().deleteAllFoodItems()
            database.mealDao().deleteAllMeals()
            database.workoutDao().deleteAllExerciseSets()
            database.workoutDao().deleteAllWorkouts()
            database.waterDao().deleteAll()
            database.weightDao().deleteAll()
        }
        photosDir.listFiles()?.forEach { runCatching { it.delete() } }
    }
}
