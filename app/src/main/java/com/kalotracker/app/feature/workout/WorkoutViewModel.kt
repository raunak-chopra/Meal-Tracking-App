package com.kalotracker.app.feature.workout

import androidx.lifecycle.ViewModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import com.kalotracker.app.core.data.food.PersonalFood
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import com.kalotracker.app.core.util.SaveOperation
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

@Serializable
data class EditableSet(
    val setNumber: Int,
    val weightKg: String = "",
    val reps: String = "",
    val isCompleted: Boolean = true
)

data class WorkoutUiState(
    val exerciseName: String = "Bench Press",
    val sets: List<EditableSet> = listOf(EditableSet(1)),
    val isCardio: Boolean = false,
    val durationMinutes: String = "45",
    val estimatedCalories: String = "220",
    val previousSessionFound: Boolean = false,
    val overloadHint: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isSaved: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val otherExercises: List<WorkoutExercise> = emptyList(),
    val sessionTitle: String = "",
    val restSeconds: Int = 0,
    val isLoading: Boolean = false
)

class WorkoutViewModel(
    private val workoutRepository: WorkoutRepository,
    private val personalDao: com.kalotracker.app.core.database.dao.PersonalDao? = null,
    private val workoutId: String? = null,
    private val initialTimestamp: Long = System.currentTimeMillis()
) : ViewModel() {

    val commonExercises = listOf(
        "Push-ups",
        "Sit-ups",
        "Crunches",
        "Bodyweight Squats",
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

    private val _uiState = MutableStateFlow(WorkoutUiState(isLoading = workoutId != null, timestamp = initialTimestamp))
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()
    val saveOperation = SaveOperation { status ->
        _uiState.update { it.copy(isSaving = status.busy, errorMessage = status.error) }
    }

    val routines: StateFlow<List<com.kalotracker.app.core.database.entity.WorkoutRoutineEntity>> = personalDao?.observeRoutines()?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()) ?: MutableStateFlow(emptyList())
    private var timer: kotlinx.coroutines.Job? = null
    private var historyRequest = 0L
    private var fitnessBodyWeight = 70.0
    private var editLoaded = workoutId == null
    fun startRest(seconds: Int = 90) {
        timer?.cancel()
        timer = viewModelScope.launch {
            val end = (System.nanoTime() / 1_000_000L) + seconds * 1000L
            while (true) {
                val remaining = ((end - (System.nanoTime() / 1_000_000L) + 999) / 1000).toInt().coerceAtLeast(0)
                _uiState.update { it.copy(restSeconds = remaining) }
                if (remaining == 0) break
                kotlinx.coroutines.delay(250)
            }
        }
    }
    fun stopRest() { timer?.cancel(); _uiState.update { it.copy(restSeconds = 0) } }
    fun setSessionTitle(name: String) { _uiState.update { it.copy(sessionTitle = name) } }
    fun loadDailyFitness(minutes: String, weightKg: String) {
        if (_uiState.value.isSaving || _uiState.value.isLoading) return
        val duration = minutes.toIntOrNull()
        val weight = weightKg.toDoubleOrNull()
        if (duration == null || duration !in 3..60 || weight == null ||
            !weight.isFinite() || weight !in 20.0..300.0) {
            _uiState.update { it.copy(errorMessage = "Enter 3–60 minutes and a body weight of 20–300 kg.") }
            return
        }
        historyRequest++
        fitnessBodyWeight = weight
        _uiState.update { it.copy(otherExercises = dailyFitnessExercises(duration, weight),
            exerciseName = "", sets = listOf(EditableSet(1)), sessionTitle = "Daily fitness",
            previousSessionFound = false, overloadHint = null, errorMessage = null) }
    }
    fun saveDailyFitness(minutes: String, weightKg: String, effort: FitnessEffort,
        reps: List<String>, onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.isSaving || state.isSaved || state.isLoading || workoutId != null) return
        val rows = runCatching {
            dailyFitnessExercises(minutes.toInt(), weightKg.toDouble(), effort, reps.map { it.toInt() })
        }.getOrElse {
            _uiState.update { it.copy(errorMessage = "Enter 3–60 minutes, 20–300 kg and whole reps (0 skips an exercise). Include at least one exercise.") }
            return
        }
        historyRequest++
        fitnessBodyWeight = weightKg.toDouble()
        _uiState.update { it.copy(otherExercises = rows, exerciseName = "", sets = listOf(EditableSet(1)),
            sessionTitle = "Daily fitness", previousSessionFound = false, overloadHint = null, errorMessage = null) }
        saveWorkout(onSuccess)
    }

    fun toggleSetComplete(index: Int, complete: Boolean) { historyRequest++; _uiState.update { s -> s.copy(sets = s.sets.mapIndexed { i, set -> if (i == index) set.copy(isCompleted = complete) else set }) } }
    fun addExerciseToSession() {
        val s = _uiState.value
        val error = validateActiveWorkout(s)
        if (error != null) { _uiState.update { it.copy(errorMessage = error) }; return }
        historyRequest++
        _uiState.update { it.copy(otherExercises = it.otherExercises + s.activeExercise(), exerciseName = "", sets = listOf(EditableSet(1)), previousSessionFound = false, overloadHint = null) }
    }
    fun removeSessionExercise(index: Int) { _uiState.update { it.copy(otherExercises = it.otherExercises.filterIndexed { i, _ -> i != index }) } }
    fun editSessionExercise(index: Int) {
        val s = _uiState.value; val row = s.otherExercises.getOrNull(index) ?: return
        if (s.exerciseName.isNotBlank()) { _uiState.update { it.copy(errorMessage = "Add the current exercise to the session before editing another.") }; return }
        historyRequest++
        _uiState.update { it.copy(exerciseName = row.name, isCardio = row.cardio, sets = row.sets,
            durationMinutes = row.durationMinutes.toString(), estimatedCalories = row.calories.toString(),
            otherExercises = s.otherExercises.filterIndexed { i, _ -> i != index }) }
    }
    fun saveRoutine() {
        val s = _uiState.value; val error = validateWorkout(s)
        if (error != null || s.sessionTitle.isBlank()) { _uiState.update { it.copy(errorMessage = error ?: "Enter a session/routine name.") }; return }
        val dao = personalDao ?: return
        saveOperation.launch(viewModelScope, { _uiState.update { it.copy(errorMessage = "Routine saved.") } }) {
            dao.putRoutines(listOf(com.kalotracker.app.core.database.entity.WorkoutRoutineEntity(name = s.sessionTitle,
                payload = PersonalFood.json.encodeToString(s.sessionExercises()))))
        }
    }
    fun loadRoutine(row: com.kalotracker.app.core.database.entity.WorkoutRoutineEntity) {
        historyRequest++
        val exercises = runCatching { validateRoutinePayload(row.payload) }.getOrElse { _uiState.update { it.copy(errorMessage = "Could not read routine.") }; return }
        _uiState.update { it.copy(otherExercises = exercises, exerciseName = "", sets = listOf(EditableSet(1)), sessionTitle = row.name, errorMessage = null, previousSessionFound = false, overloadHint = null) }
    }
    fun deleteRoutine(row: com.kalotracker.app.core.database.entity.WorkoutRoutineEntity) {
        val dao = personalDao ?: return
        saveOperation.launch(viewModelScope, {}) { dao.deleteRoutine(row.id) }
    }
    private fun loadWorkout(id: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val loaded = workoutRepository.getWorkout(id) ?: error("Workout no longer exists.")
                val rows = if (loaded.workout.exercisesJson != "[]") validateRoutinePayload(loaded.workout.exercisesJson)
                    else if (loaded.sets.isEmpty()) listOf(WorkoutExercise(loaded.workout.title, true, durationMinutes = loaded.workout.durationMinutes, calories = loaded.workout.estimatedCaloriesBurned))
                    else loaded.sets.groupBy { it.exerciseName }.entries.mapIndexed { index, (name, sets) -> WorkoutExercise(name, sets = sets.map { EditableSet(it.setNumber, it.weightKg.toString(), it.reps.toString(), it.isCompleted) }, durationMinutes = (loaded.workout.durationMinutes / loaded.sets.map { it.exerciseName }.distinct().size).coerceAtLeast(1), calories = if (index == 0) loaded.workout.estimatedCaloriesBurned else 0) }
                editLoaded = true
                _uiState.update { it.copy(otherExercises = rows, exerciseName = "", sets = listOf(EditableSet(1)), timestamp = loaded.workout.timestamp,
                    sessionTitle = loaded.workout.title, isLoading = false) }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (e: Exception) { _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "Could not load workout.") } }
        }
    }

    init {
        if (workoutId == null) loadHistoryForExercise("Bench Press") else loadWorkout(workoutId)
    }

    fun selectExercise(name: String) {
        if (name == _uiState.value.exerciseName) return
        val isCardioType = name.contains("Run", ignoreCase = true) ||
                name.contains("Bike", ignoreCase = true) ||
                name.contains("Row", ignoreCase = true) && !name.contains("Barbell")

        _uiState.update {
            it.copy(
                exerciseName = name,
                isCardio = isCardioType,
                sets = listOf(EditableSet(1)),
                previousSessionFound = false,
                overloadHint = null,
                errorMessage = null
            )
        }
        loadHistoryForExercise(name)
    }

    fun updateExerciseName(name: String) {
        historyRequest++
        _uiState.update { it.copy(exerciseName = name, previousSessionFound = false, overloadHint = null) }
    }

    fun setTimestamp(millis: Long) {
        _uiState.update { it.copy(timestamp = millis) }
    }

    private fun loadHistoryForExercise(name: String) {
        val request = ++historyRequest
        viewModelScope.launch {
            val previousSets = try {
                workoutRepository.getLastSessionSetsForExercise(name)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (request == historyRequest) _uiState.update { it.copy(errorMessage = "Could not load previous sets. You can enter them manually.") }
                return@launch
            }
            if (request != historyRequest || _uiState.value.exerciseName != name) return@launch
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
        historyRequest++
        _uiState.update { state ->
            val nextNumber = state.sets.size + 1
            val lastSet = state.sets.lastOrNull()
            val newSet = EditableSet(
                setNumber = nextNumber,
                weightKg = lastSet?.weightKg ?: "",
                reps = lastSet?.reps ?: ""
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
        historyRequest++
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
        historyRequest++
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
        val name = _uiState.value.exerciseName
        if (name in listOf("Push-ups", "Sit-ups", "Crunches", "Bodyweight Squats")) {
            val met = if (name == "Crunches") 2.8 else 3.8
            return kotlin.math.round(met * 3.5 * fitnessBodyWeight / 200 * durationMins.coerceAtLeast(0)).toInt()
        }
        // Simple formula: ~5 kcal per heavy set + 3 kcal per min of training
        return (setCount * 6) + (durationMins * 3)
    }

    fun saveWorkout(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.isSaving || state.isSaved || state.isLoading || !editLoaded) return
        val error = validateWorkout(state)
        if (error != null) {
            _uiState.update { it.copy(errorMessage = error) }
            return
        }
        saveOperation.launch(viewModelScope, onSuccess) {
            val workoutId = this.workoutId ?: UUID.randomUUID().toString()
            val exercises = state.sessionExercises()
            val workoutEntity = WorkoutEntity(
                id = workoutId,
                title = state.sessionTitle.ifBlank { exercises.joinToString(" + ") { it.name } },
                type = if (exercises.all { it.cardio }) "CARDIO" else "STRENGTH",
                durationMinutes = exercises.sumOf { it.durationMinutes },
                estimatedCaloriesBurned = exercises.sumOf { it.calories },
                timestamp = state.timestamp,
                exercisesJson = PersonalFood.json.encodeToString(exercises)
            )

            val setEntities = exercises.flatMap { row -> row.sets.map { set ->
                ExerciseSetEntity(workoutId = workoutId, exerciseName = row.name, setNumber = set.setNumber,
                    weightKg = set.weightKg.toFloat(), reps = set.reps.toInt(), isCompleted = set.isCompleted)
            } }
            if (this.workoutId == null) workoutRepository.saveWorkout(workoutEntity, setEntities)
            else workoutRepository.updateWorkout(workoutEntity, setEntities)

            _uiState.update { it.copy(isSaved = true) }
        }
    }
}

class WorkoutViewModelFactory(
    private val workoutRepository: WorkoutRepository,
    private val personalDao: com.kalotracker.app.core.database.dao.PersonalDao,
    private val workoutId: String? = null,
    private val initialTimestamp: Long = System.currentTimeMillis()
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkoutViewModel(workoutRepository, personalDao, workoutId, initialTimestamp) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
