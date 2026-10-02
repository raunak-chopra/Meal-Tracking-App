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
    private val photosDir: File,
    private val settings: com.kalotracker.app.core.settings.AppSettings,
    private val draftFile: File? = null
) {

    suspend fun buildBackup(): BackupFile = withContext(Dispatchers.IO) {
        val p = profileRepository.profile.value
        database.withTransaction {
            BackupFile(
                profile = BackupProfile(
                    calories = p.targetCalories, protein = p.targetProtein, carbs = p.targetCarbs,
                    fat = p.targetFat, steps = p.targetSteps, waterMl = p.targetWaterMl, goal = p.goal.name
                ),
                meals = database.mealDao().getAllMeals().map { BackupCodec.toBackupMeal(it.meal, it.items) },
                workouts = database.workoutDao().getAllWorkouts().map { BackupCodec.toBackupWorkout(it.workout, it.sets) },
                water = database.waterDao().getAll().map { BackupWater(it.id, it.milliliters, it.timestamp) },
                weights = database.weightDao().getAll().map { BackupWeight(it.id, it.weightKg, it.timestamp) },
                dayStatus = database.personalDao().days(), goalHistory = profileRepository.history.value,
                savedFoods = database.personalDao().foods(), barcodes = database.personalDao().barcodes(),
                routines = database.personalDao().routines(),
                preferences = BackupPreferences(settings.ai.value.model, settings.reminder.value.enabled,
                    settings.reminder.value.hour, settings.reminder.value.minute, settings.appearance.value.name)
            )
        }
    }

    /**
     * Adds or updates records by id inside one transaction: either everything imports or nothing does.
     * Records that exist on the phone but not in the file are kept.
     */
    suspend fun import(file: BackupFile, photoPaths: Map<String, String> = emptyMap()): ImportSummary = withContext(Dispatchers.IO) {
        database.withTransaction {
            val mealDao = database.mealDao()
            mealDao.insertMeals(file.meals.map { m -> BackupCodec.toMealEntity(m).copy(
                imageLocalUri = photoPaths[m.id] ?: mealDao.getMealById(m.id)?.meal?.imageLocalUri) })
            mealDao.insertFoodItems(file.meals.flatMap(BackupCodec::toFoodItemEntities))

            val workoutDao = database.workoutDao()
            workoutDao.insertWorkouts(file.workouts.map(BackupCodec::toWorkoutEntity))
            workoutDao.insertExerciseSets(file.workouts.flatMap(BackupCodec::toSetEntities))

            database.waterDao().insertAll(file.water.map(BackupCodec::toWaterEntity))
            database.weightDao().insertAll(file.weights.map(BackupCodec::toWeightEntity))
            database.personalDao().apply {
                putDays(file.dayStatus); putGoals(file.goalHistory); putFoods(file.savedFoods)
                putBarcodes(file.barcodes); putRoutines(file.routines)
            }
        }

        file.profile?.let {
            profileRepository.updateTargets(it.calories, it.protein, it.carbs, it.fat, it.steps, it.waterMl)
            runCatching { GoalType.valueOf(it.goal) }.getOrNull()?.let(profileRepository::setGoal)
        }
        profileRepository.restoreHistory(file.goalHistory)
        file.preferences?.let { p ->
            settings.saveAppearance(com.kalotracker.app.core.settings.Appearance.valueOf(p.appearance))
            settings.saveAi(settings.ai.value.apiKey, p.model)
            settings.saveReminder(com.kalotracker.app.core.settings.ReminderSettings(p.reminderEnabled, p.reminderHour, p.reminderMinute))
        }
        ImportSummary(file.meals.size, file.workouts.size, file.water.size, file.weights.size)
    }

    suspend fun exportArchive(output: java.io.OutputStream) = withContext(Dispatchers.IO) {
        val (backup, photos) = database.withTransaction {
            buildBackup() to database.mealDao().getAllMeals().associate { it.meal.id to it.meal.imageLocalUri }
        }
        val archive = backup.copy(meals = backup.meals.map { m ->
            m.copy(photoEntry = photos[m.id]?.let { ArchiveCodec.photoName(m.id) })
        })
        ArchiveCodec.write(output, archive) { id -> photos[id]?.let { File(it).readBytes() } }
    }

    suspend fun restoreArchive(archive: ArchiveContents): ImportSummary = withContext(Dispatchers.IO) {
        val created = mutableMapOf<String, String>()
        try {
            photosDir.mkdirs()
            archive.backup.meals.forEach { m -> m.photoEntry?.let { entry ->
                val photo = File.createTempFile("restored_", ".jpg", photosDir)
                created[m.id] = photo.absolutePath
                photo.writeBytes(archive.photos.getValue(entry))
            } }
            import(archive.backup, created)
        } catch (e: Exception) {
            // DB commit may have succeeded before preferences failed; never remove a referenced photo.
            val referenced = database.mealDao().getAllMeals().mapNotNull { it.meal.imageLocalUri }.toSet()
            created.values.filterNot { it in referenced }.forEach { File(it).delete() }
            throw e
        }
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
            database.personalDao().apply { clearDays(); clearGoals(); clearFoods(); clearBarcodes(); clearRoutines() }
        }
        draftFile?.let { android.util.AtomicFile(it).delete() }
        profileRepository.resetHistory()
        photosDir.listFiles()?.forEach { runCatching { it.delete() } }
    }
}
