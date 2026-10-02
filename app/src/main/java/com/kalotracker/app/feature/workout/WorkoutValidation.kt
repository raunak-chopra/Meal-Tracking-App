package com.kalotracker.app.feature.workout

internal fun validateWorkout(state: WorkoutUiState): String? {
    if (state.otherExercises.isEmpty()) return validateActiveWorkout(state)
    for (row in state.otherExercises) {
        val error = validateActiveWorkout(state.copy(exerciseName = row.name, isCardio = row.cardio, sets = row.sets,
            durationMinutes = row.durationMinutes.toString(), estimatedCalories = row.calories.toString(), otherExercises = emptyList()))
        if (error != null) return error
    }
    return if (state.exerciseName.isBlank()) null else validateActiveWorkout(state)
}

internal fun validateActiveWorkout(state: WorkoutUiState): String? {
    if (state.exerciseName.isBlank()) return "Enter an exercise name."
    if (state.durationMinutes.toIntOrNull()?.let { it in 1..1440 } != true)
        return "Duration must be a whole number between 1 and 1440 minutes."
    if (state.estimatedCalories.toIntOrNull()?.let { it in 0..100_000 } != true)
        return "Burn estimate must be a whole number between 0 and 100000 kcal."
    if (!state.isCardio) {
        if (state.sets.isEmpty()) return "Add at least one set."
        state.sets.forEachIndexed { index, set ->
            val weight = set.weightKg.toFloatOrNull()
            if (weight == null || !weight.isFinite() || weight !in 0f..10_000f)
                return "Set ${index + 1}: enter a weight from 0 to 10000 kg (0 for bodyweight)."
            if (set.reps.toIntOrNull()?.let { it in 1..10_000 } != true)
                return "Set ${index + 1}: enter a whole number of reps from 1 to 10000."
        }
    }
    return null
}
