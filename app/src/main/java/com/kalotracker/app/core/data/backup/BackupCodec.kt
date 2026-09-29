package com.kalotracker.app.core.data.backup

import com.kalotracker.app.core.database.entity.ExerciseSetEntity
import com.kalotracker.app.core.database.entity.FoodItemEntity
import com.kalotracker.app.core.database.entity.MealEntity
import com.kalotracker.app.core.database.entity.WaterLogEntity
import com.kalotracker.app.core.database.entity.WeightLogEntity
import com.kalotracker.app.core.database.entity.WorkoutEntity
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
    val weights: List<BackupWeight> = emptyList()
) {
    companion object {
        const val APP_ID = "kalo"
        const val CURRENT_VERSION = 1
    }
}

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
    val items: List<BackupFoodItem> = emptyList()
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
    val sets: List<BackupSet> = emptyList()
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
        if (file.meals.any { it.id.isBlank() || it.items.any { i -> i.id.isBlank() } } ||
            file.workouts.any { it.id.isBlank() || it.sets.any { s -> s.id.isBlank() } } ||
            file.water.any { it.id.isBlank() } || file.weights.any { it.id.isBlank() }
        ) {
            throw BackupFormatException("The backup contains records without ids and can't be imported safely.")
        }
        return file
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
        sets = sets.map { BackupSet(it.id, it.exerciseName, it.setNumber, it.weightKg, it.reps, it.isCompleted) }
    )

    fun toWorkoutEntity(w: BackupWorkout) = WorkoutEntity(
        id = w.id, title = w.title, type = w.type, durationMinutes = w.durationMinutes,
        estimatedCaloriesBurned = w.estimatedCaloriesBurned, timestamp = w.timestamp
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
