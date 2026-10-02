package com.kalotracker.app.feature.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kalotracker.app.core.ai.OverloadCoach
import com.kalotracker.app.core.ai.PastSet
import com.kalotracker.app.core.data.repository.WorkoutRepository
import com.kalotracker.app.core.database.entity.ExerciseSetEntity
import com.kalotracker.app.core.database.entity.WorkoutEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class EditableSet(
    val setNumber: Int,
    val weightKg: String = "60.0",
    val reps: String = "10"
)

data class WorkoutUiState(
    val exerciseName: String = "Bench Press",
    val sets: List<EditableSet> = listOf(
        EditableSet(1, "60.0", "10"),
        EditableSet(2, "60.0", "10"),
        EditableSet(3, "60.0", "8")
    ),
    val isCardio: Boolean = false,
    val durationMinutes: String = "45",
    val estimatedCalories: String = "220",
    val previousSessionFound: Boolean = false,
    val overloadHint: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isSaved: Boolean = false
)

class WorkoutViewModel(
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    val commonExercises = listOf(
        "Bench Press",
        "Barbell Squat",
        "Deadlift",
        "Overhead Press",
        "Barbell Row",
        "Pull-Ups",
        "Incline DB Press",
        "Lateral Raises",
        "Bicep Curls",
        "Tricep Pushdowns",
        "Leg Press",
        "Outdoor Run",
        "Stationary Bike"
    )

    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    init {
        loadHistoryForExercise("Bench Press")
    }

    fun selectExercise(name: String) {
        val isCardioType = name.contains("Run", ignoreCase = true) ||
                name.contains("Bike", ignoreCase = true) ||
                name.contains("Row", ignoreCase = true) && !name.contains("Barbell")

        _uiState.update {
            it.copy(
                exerciseName = name,
                isCardio = isCardioType
            )
        }
        loadHistoryForExercise(name)
    }

    fun updateExerciseName(name: String) {
        _uiState.update { it.copy(exerciseName = name) }
    }

    fun setTimestamp(millis: Long) {
        _uiState.update { it.copy(timestamp = millis) }
    }

    private fun loadHistoryForExercise(name: String) {
        viewModelScope.launch {
            val previousSets = workoutRepository.getLastSessionSetsForExercise(name)
            if (previousSets.isNotEmpty()) {
                val loaded = previousSets.mapIndexed { idx, set ->
                    EditableSet(
                        setNumber = idx + 1,
                        weightKg = set.weightKg.toString(),
                        reps = set.reps.toString()
                    )
                }
                _uiState.update {
                    it.copy(
                        sets = loaded,
                        previousSessionFound = true,
                        overloadHint = OverloadCoach.suggest(previousSets.map { s -> PastSet(s.weightKg, s.reps) })
                    )
                }
            } else {
                _uiState.update { it.copy(previousSessionFound = false, overloadHint = null) }
            }
        }
    }

    fun addSet() {
        _uiState.update { state ->
            val nextNumber = state.sets.size + 1
            val lastSet = state.sets.lastOrNull()
            val newSet = EditableSet(
                setNumber = nextNumber,
                weightKg = lastSet?.weightKg ?: "60.0",
                reps = lastSet?.reps ?: "10"
            )
            val updatedSets = state.sets + newSet
            val autoCals = calculateAutoCalories(updatedSets.size, state.durationMinutes.toIntOrNull() ?: 45)
            state.copy(
                sets = updatedSets,
                estimatedCalories = autoCals.toString()
            )
        }
    }

    fun deleteSet(index: Int) {
        _uiState.update { state ->
            if (state.sets.size <= 1) return@update state
            val updated = state.sets.toMutableList()
            if (index in updated.indices) {
                updated.removeAt(index)
            }
            // Renumber sets
            val renumbered = updated.mapIndexed { idx, s -> s.copy(setNumber = idx + 1) }
            state.copy(sets = renumbered)
        }
    }

    fun updateSet(index: Int, weight: String, reps: String) {
        _uiState.update { state ->
            val updated = state.sets.toMutableList()
            if (index in updated.indices) {
                updated[index] = updated[index].copy(weightKg = weight, reps = reps)
            }
            state.copy(sets = updated)
        }
    }

    fun updateDuration(duration: String) {
        _uiState.update { state ->
            val d = duration.toIntOrNull() ?: 30
            val autoCals = calculateAutoCalories(state.sets.size, d)
            state.copy(
                durationMinutes = duration,
                estimatedCalories = autoCals.toString()
            )
        }
    }

    fun updateCalories(calories: String) {
        _uiState.update { it.copy(estimatedCalories = calories) }
    }

    fun toggleCardio(isCardio: Boolean) {
        _uiState.update { it.copy(isCardio = isCardio) }
    }

    private fun calculateAutoCalories(setCount: Int, durationMins: Int): Int {
        // Simple formula: ~5 kcal per heavy set + 3 kcal per min of training
        return (setCount * 6) + (durationMins * 3)
    }

    fun saveWorkout(onSuccess: () -> Unit) {
        val state = _uiState.value
        viewModelScope.launch {
            val workoutId = UUID.randomUUID().toString()
            val workoutEntity = WorkoutEntity(
                id = workoutId,
                title = state.exerciseName.ifBlank { "Workout Session" },
                type = if (state.isCardio) "CARDIO" else "STRENGTH",
                durationMinutes = state.durationMinutes.toIntOrNull() ?: 45,
                estimatedCaloriesBurned = state.estimatedCalories.toIntOrNull() ?: 200,
                timestamp = state.timestamp
            )

            val setEntities = state.sets.map { set ->
                ExerciseSetEntity(
                    workoutId = workoutId,
                    exerciseName = state.exerciseName,
                    setNumber = set.setNumber,
                    weightKg = set.weightKg.toFloatOrNull() ?: 0f,
                    reps = set.reps.toIntOrNull() ?: 0
                )
            }

            workoutRepository.saveWorkout(workoutEntity, setEntities)
            _uiState.update { it.copy(isSaved = true) }
            onSuccess()
        }
    }
}

class WorkoutViewModelFactory(
    private val workoutRepository: WorkoutRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkoutViewModel(workoutRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
