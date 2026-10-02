package com.kalotracker.app.core.data.backup

import com.kalotracker.app.core.database.entity.ExerciseSetEntity
import com.kalotracker.app.core.database.entity.FoodItemEntity
import com.kalotracker.app.core.database.entity.MealEntity
import com.kalotracker.app.core.database.entity.WaterLogEntity
import com.kalotracker.app.core.database.entity.WeightLogEntity
import com.kalotracker.app.core.database.entity.WorkoutEntity
import com.kalotracker.app.core.database.entity.*
import com.kalotracker.app.core.data.food.PersonalFood
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Everything the app stores, in a plain, versioned, human-readable JSON file. Photos are not included. */
@Serializable
data class BackupFile(
    val app: String = APP_ID,
    val version: Int = CURRENT_VERSION,
    val exportedAt: Long = System.currentTimeMillis(),
    val profile: BackupProfile? = null,
    val meals: List<BackupMeal> = emptyList(),
    val workouts: List<BackupWorkout> = emptyList(),
    val water: List<BackupWater> = emptyList(),
    val weights: List<BackupWeight> = emptyList(),
    val dayStatus: List<DayStatusEntity> = emptyList(),
    val goalHistory: List<GoalHistoryEntity> = emptyList(),
    val savedFoods: List<SavedFoodEntity> = emptyList(),
    val barcodes: List<BarcodeCacheEntity> = emptyList(),
    val routines: List<WorkoutRoutineEntity> = emptyList(),
    val preferences: BackupPreferences? = null
) {
    companion object {
        const val APP_ID = "kalo"
        const val CURRENT_VERSION = 2
    }
}

@Serializable
data class BackupPreferences(val model: String, val reminderEnabled: Boolean, val reminderHour: Int, val reminderMinute: Int, val appearance: String = "SYSTEM")

@Serializable
data class BackupProfile(
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val steps: Long,
    val waterMl: Int,
    val goal: String
)

@Serializable
data class BackupMeal(
    val id: String,
    val title: String,
    val totalCalories: Int,
    val totalProteinGrams: Float,
    val totalCarbsGrams: Float,
    val totalFatGrams: Float,
    val notes: String? = null,
    val timestamp: Long,
    val items: List<BackupFoodItem> = emptyList(),
    val photoEntry: String? = null
)

@Serializable
data class BackupFoodItem(
    val id: String,
    val name: String,
    val portionGrams: Float,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val confidence: Float = 1f
)

@Serializable
data class BackupWorkout(
    val id: String,
    val title: String,
    val type: String,
    val durationMinutes: Int,
    val estimatedCaloriesBurned: Int,
    val timestamp: Long,
    val sets: List<BackupSet> = emptyList(),
    val exercisesJson: String = "[]"
)

@Serializable
data class BackupSet(
    val id: String,
    val exerciseName: String,
    val setNumber: Int,
    val weightKg: Float,
    val reps: Int,
    val isCompleted: Boolean = true
)

@Serializable
data class BackupWater(val id: String, val milliliters: Int, val timestamp: Long)

@Serializable
data class BackupWeight(val id: String, val weightKg: Float, val timestamp: Long)

class BackupFormatException(message: String) : Exception(message)

object BackupCodec {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true // newer files with extra fields still import
        encodeDefaults = true
    }

    fun encode(file: BackupFile): String = json.encodeToString(BackupFile.serializer(), file)

    /** Parses and validates; throws [BackupFormatException] with a user-readable reason. */
    fun decode(text: String): BackupFile {
        val file = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: SerializationException) {
            throw BackupFormatException("This isn't a valid Kalo backup file.")
        } catch (e: IllegalArgumentException) {
            throw BackupFormatException("This isn't a valid Kalo backup file.")
        }
        if (file.app != BackupFile.APP_ID) throw BackupFormatException("This file wasn't created by Kalo.")
        if (file.version > BackupFile.CURRENT_VERSION) {
            throw BackupFormatException("This backup is from a newer version of Kalo. Update the app to import it.")
        }
        if (file.version < 1) throw BackupFormatException("This backup version is not supported.")
        if (file.meals.any { it.id.isBlank() || it.items.any { i -> i.id.isBlank() } } ||
            file.workouts.any { it.id.isBlank() || it.sets.any { s -> s.id.isBlank() } } ||
            file.water.any { it.id.isBlank() } || file.weights.any { it.id.isBlank() }
        ) {
            throw BackupFormatException("The backup contains records without ids and can't be imported safely.")
        }
        validate(file)
        return file
    }

    private fun validate(file: BackupFile) {
        fun unique(ids: List<String>) = ids.size == ids.toSet().size
        if (!unique(file.meals.map { it.id }) || !unique(file.meals.flatMap { it.items }.map { it.id }) ||
            !unique(file.workouts.map { it.id }) || !unique(file.workouts.flatMap { it.sets }.map { it.id }) ||
            !unique(file.water.map { it.id }) || !unique(file.weights.map { it.id })) {
            throw BackupFormatException("The backup contains duplicate record ids.")
        }
        fun nonnegative(value: Float) = value.isFinite() && value >= 0f
        val invalidMeal = file.meals.any { m ->
            m.title.isBlank() || m.totalCalories < 0 || !nonnegative(m.totalProteinGrams) ||
                !nonnegative(m.totalCarbsGrams) || !nonnegative(m.totalFatGrams) || m.items.any { i ->
                    i.name.isBlank() || !i.portionGrams.isFinite() || i.portionGrams <= 0f ||
                        i.calories < 0 || !nonnegative(i.protein) || !nonnegative(i.carbs) ||
                        !nonnegative(i.fat) || !i.confidence.isFinite() || i.confidence !in 0f..1f
                }
        }
        val invalidWorkout = file.workouts.any { w ->
            w.title.isBlank() || w.type !in setOf("STRENGTH", "CARDIO") || w.durationMinutes <= 0 ||
                w.estimatedCaloriesBurned < 0 || w.sets.any { s ->
                    s.exerciseName.isBlank() || s.setNumber <= 0 || !nonnegative(s.weightKg) || s.reps < 0
                }
        }
        if (invalidMeal || invalidWorkout || file.water.any { it.milliliters <= 0 } ||
            file.weights.any { !it.weightKg.isFinite() || it.weightKg !in 20f..350f }) {
            throw BackupFormatException("The backup contains invalid log values. No data was imported.")
        }
        if (!unique(file.dayStatus.map { it.date }) || !unique(file.goalHistory.map { it.date }) ||
            !unique(file.savedFoods.map { it.id }) || !unique(file.barcodes.map { it.barcode }) || !unique(file.routines.map { it.id }))
            throw BackupFormatException("Duplicate library/history ids.")
        try {
            file.dayStatus.forEach { java.time.LocalDate.parse(it.date) }
            file.goalHistory.forEach { g ->
                java.time.LocalDate.parse(g.date)
                require(com.kalotracker.app.core.util.validateGoalInputs(g.calories.toString(),g.protein.toString(),g.carbs.toString(),g.fat.toString(),"0",g.waterMl.toString()) == null)
                require(g.goal in setOf("LOSE","MAINTAIN","GAIN"))
            }
            file.savedFoods.forEach(PersonalFood::validate)
            file.barcodes.forEach { require(it.barcode.isNotBlank() && it.barcode.all(Char::isDigit));
                val p = PersonalFood.json.decodeFromString<com.kalotracker.app.core.network.ScannedFoodProduct>(it.payload)
                require(p.barcode == it.barcode && p.caloriesPer100g >= 0 && p.servingSizeGrams.isFinite() && p.servingSizeGrams > 0f)
                require(listOf(p.proteinPer100g,p.carbsPer100g,p.fatPer100g).all { v -> v.isFinite() && v >= 0f })
            }
            file.routines.forEach { require(it.id.isNotBlank() && it.name.isNotBlank());
                com.kalotracker.app.feature.workout.validateRoutinePayload(it.payload)
            }
            file.preferences?.let { require(it.reminderHour in 0..23 && it.reminderMinute in 0..59 && it.model.isNotBlank() && it.appearance in setOf("SYSTEM", "LIGHT", "DARK")) }
            file.meals.forEach { require(it.photoEntry == null || it.photoEntry == ArchiveCodec.photoName(it.id)) }
        } catch (_: Exception) { throw BackupFormatException("Invalid library, history, settings or photo data.") }
        file.workouts.filter { it.exercisesJson != "[]" }.forEach { try { com.kalotracker.app.feature.workout.validateRoutinePayload(it.exercisesJson) } catch (_: Exception) { throw BackupFormatException("Invalid workout session.") } }
        file.profile?.let { p ->
            val error = com.kalotracker.app.core.util.validateGoalInputs(p.calories.toString(),
                p.protein.toString(), p.carbs.toString(), p.fat.toString(), p.steps.toString(), p.waterMl.toString())
            if (error != null || p.goal !in setOf("LOSE", "MAINTAIN", "GAIN"))
                throw BackupFormatException("The backup contains invalid goals. No data was imported.")
        }
    }

    // ---- entity mapping ----

    fun toBackupMeal(meal: MealEntity, items: List<FoodItemEntity>) = BackupMeal(
        id = meal.id, title = meal.title, totalCalories = meal.totalCalories,
        totalProteinGrams = meal.totalProteinGrams, totalCarbsGrams = meal.totalCarbsGrams,
        totalFatGrams = meal.totalFatGrams, notes = meal.notes, timestamp = meal.timestamp,
        items = items.map {
            BackupFoodItem(it.id, it.name, it.portionGrams, it.calories, it.protein, it.carbs, it.fat, it.confidence)
        }
    )

    fun toMealEntity(m: BackupMeal) = MealEntity(
        id = m.id, title = m.title, totalCalories = m.totalCalories,
        totalProteinGrams = m.totalProteinGrams, totalCarbsGrams = m.totalCarbsGrams,
        totalFatGrams = m.totalFatGrams, imageLocalUri = null, notes = m.notes, timestamp = m.timestamp
    )

    fun toFoodItemEntities(m: BackupMeal) = m.items.map {
        FoodItemEntity(it.id, m.id, it.name, it.portionGrams, it.calories, it.protein, it.carbs, it.fat, it.confidence)
    }

    fun toBackupWorkout(w: WorkoutEntity, sets: List<ExerciseSetEntity>) = BackupWorkout(
        id = w.id, title = w.title, type = w.type, durationMinutes = w.durationMinutes,
        estimatedCaloriesBurned = w.estimatedCaloriesBurned, timestamp = w.timestamp,
        sets = sets.map { BackupSet(it.id, it.exerciseName, it.setNumber, it.weightKg, it.reps, it.isCompleted) },
        exercisesJson = w.exercisesJson
    )

    fun toWorkoutEntity(w: BackupWorkout) = WorkoutEntity(
        id = w.id, title = w.title, type = w.type, durationMinutes = w.durationMinutes,
        estimatedCaloriesBurned = w.estimatedCaloriesBurned, timestamp = w.timestamp, exercisesJson = w.exercisesJson
    )

    fun toSetEntities(w: BackupWorkout) = w.sets.map {
        ExerciseSetEntity(it.id, w.id, it.exerciseName, it.setNumber, it.weightKg, it.reps, it.isCompleted)
    }

    fun toWaterEntity(w: BackupWater) = WaterLogEntity(w.id, w.milliliters, w.timestamp)
    fun toWeightEntity(w: BackupWeight) = WeightLogEntity(w.id, w.weightKg, w.timestamp)

    /** Meals as CSV for spreadsheets: one row per food item. */
    fun mealsToCsv(meals: List<BackupMeal>): String {
        fun esc(v: String): String =
            if (v.any { it == ',' || it == '"' || it == '\n' }) "\"" + v.replace("\"", "\"\"") + "\"" else v
        val zone = java.time.ZoneId.systemDefault()
        val fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        return buildString {
            appendLine("date_time,meal,food,grams,calories,protein_g,carbs_g,fat_g")
            meals.forEach { m ->
                val whenText = java.time.Instant.ofEpochMilli(m.timestamp).atZone(zone).format(fmt)
                if (m.items.isEmpty()) {
                    appendLine("$whenText,${esc(m.title)},,,${m.totalCalories},${m.totalProteinGrams},${m.totalCarbsGrams},${m.totalFatGrams}")
                } else {
                    m.items.forEach { i ->
                        appendLine("$whenText,${esc(m.title)},${esc(i.name)},${i.portionGrams},${i.calories},${i.protein},${i.carbs},${i.fat}")
                    }
                }
            }
        }
    }
}
