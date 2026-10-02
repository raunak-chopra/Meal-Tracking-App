package com.kalotracker.app.feature.workout

import com.kalotracker.app.core.data.food.PersonalFood
import kotlinx.serialization.Serializable

@Serializable
data class WorkoutExercise(val name: String, val cardio: Boolean = false, val sets: List<EditableSet> = emptyList(),
    val durationMinutes: Int = 1, val calories: Int = 0)

fun validateRoutinePayload(payload: String): List<WorkoutExercise> {
    val rows = PersonalFood.json.decodeFromString<List<WorkoutExercise>>(payload)
    require(rows.isNotEmpty()) { "Add at least one exercise." }
    rows.forEach { row ->
        require(validateActiveWorkout(WorkoutUiState(exerciseName = row.name, isCardio = row.cardio,
            sets = row.sets, durationMinutes = row.durationMinutes.toString(), estimatedCalories = row.calories.toString())) == null) { "Invalid routine exercise." }
    }
    return rows
}

internal fun WorkoutUiState.activeExercise() = WorkoutExercise(exerciseName, isCardio,
    if (isCardio) emptyList() else sets, durationMinutes.toInt(), estimatedCalories.toInt())
internal fun WorkoutUiState.sessionExercises(): List<WorkoutExercise> =
    otherExercises + if (exerciseName.isBlank() && otherExercises.isNotEmpty()) emptyList() else listOf(activeExercise())
