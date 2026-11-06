package com.kalotracker.app.feature.dashboard

import androidx.lifecycle.ViewModel
import com.kalotracker.app.core.util.SaveOperation
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kalotracker.app.core.ai.NutritionInsight
import com.kalotracker.app.core.ai.NutritionInsightEngine
import com.kalotracker.app.core.data.repository.MealRepository
import com.kalotracker.app.core.data.repository.UserProfileRepository
import com.kalotracker.app.core.data.repository.WaterRepository
import com.kalotracker.app.core.data.repository.WorkoutRepository
import com.kalotracker.app.core.database.dao.MealWithItems
import com.kalotracker.app.core.database.dao.WorkoutWithSets
import com.kalotracker.app.core.health.HealthConnectManager
import com.kalotracker.app.core.health.HealthDataSummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

sealed class UndoAction(val message: String) {
    class Meal(val item: MealWithItems) : UndoAction("Deleted \"${item.meal.title}\"")
    class Workout(val item: WorkoutWithSets) : UndoAction("Deleted \"${item.workout.title}\"")
}

data class DashboardUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val isToday: Boolean = true,
    val dayComplete: Boolean = false,
    /** True until height, weight, age and activity are filled in Settings. */
    val needsBodyDetails: Boolean = false,
    val bodyWeightKg: Double = 70.0,
    val targetCalories: Int = 2200,
    val currentCalories: Int = 0,
    val targetProtein: Int = 160,
    val proteinGrams: Int = 0,
    val targetCarbs: Int = 220,
    val carbsGrams: Int = 0,
    val targetFat: Int = 70,
    val fatGrams: Int = 0,
    val targetSteps: Long = 10000L,
    val targetWaterMl: Int = 2500,
    val currentWaterMl: Int = 0,
    val healthData: HealthDataSummary = HealthDataSummary(),
    val todayMeals: List<MealWithItems> = emptyList(),
    val todayWorkouts: List<WorkoutWithSets> = emptyList(),
    val lastWorkout: WorkoutWithSets? = null,
    val routines: List<com.kalotracker.app.core.database.entity.WorkoutRoutineEntity> = emptyList(),
    val dailyInsight: NutritionInsight? = null,
    val pendingUndo: UndoAction? = null,
    val isLoading: Boolean = false,
    val isRepeatingMeal: Boolean = false,
    val isLoggingWorkout: Boolean = false,
    val repeatMessage: String? = null
)

class DashboardViewModel(
    private val personalDao: com.kalotracker.app.core.database.dao.PersonalDao,
    private val mealRepository: MealRepository,
    private val workoutRepository: WorkoutRepository,
    private val userProfileRepository: UserProfileRepository,
    private val waterRepository: WaterRepository,
    private val healthConnectManager: HealthConnectManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    private val repeatSave = SaveOperation { status ->
        _uiState.update { it.copy(isRepeatingMeal = status.busy, repeatMessage = status.error) }
    }
    private val workoutQuickSave = SaveOperation { status ->
        _uiState.update { it.copy(isLoggingWorkout = status.busy, repeatMessage = status.error) }
    }

    private var dayStatuses = emptyList<com.kalotracker.app.core.database.entity.DayStatusEntity>()
    private var healthJob: Job? = null
    private var healthRequest = 0L
    private var mealsJob: Job? = null
    private var workoutsJob: Job? = null
    private var waterJob: Job? = null

    init {
        viewModelScope.launch {
            personalDao.observeDays().collect { days ->
                dayStatuses = days
                _uiState.update { it.copy(dayComplete = days.any { d -> d.date == it.selectedDate.toString() && d.complete }) }
            }
        }
        // Observe profile changes (targets)
        viewModelScope.launch {
            userProfileRepository.profile.collect { profile ->
                applyTargets()
                recomputeInsight()
            }
        }

        viewModelScope.launch {
            userProfileRepository.body.collect { b ->
                val wt = b?.weightKg?.toDouble()?.takeIf { it in 20.0..300.0 } ?: 70.0
                _uiState.update { it.copy(needsBodyDetails = b == null, bodyWeightKg = wt) }
            }
        }
        viewModelScope.launch {
            personalDao.observeRoutines().collect { list ->
                _uiState.update { it.copy(routines = list) }
            }
        }
        viewModelScope.launch { userProfileRepository.history.collect { applyTargets(); recomputeInsight() } }
        loadDataForDate(LocalDate.now())
    }

    fun goToPreviousDay() {
        val prev = _uiState.value.selectedDate.minusDays(1)
        loadDataForDate(prev)
    }

    /** Days ahead of today hold no records yet, so the day picker stops at today. */
    fun goToNextDay() {
        val next = _uiState.value.selectedDate.plusDays(1)
        if (next.isAfter(LocalDate.now())) return
        loadDataForDate(next)
    }

    fun goToToday() {
        loadDataForDate(LocalDate.now())
    }

    fun loadDataForDate(requested: LocalDate) {
        val date = minOf(requested, LocalDate.now())
        val isToday = date.isEqual(LocalDate.now())
        _uiState.update {
            it.copy(
                selectedDate = date,
                dayComplete = dayStatuses.any { it.date == date.toString() && it.complete },
                isToday = isToday,
                healthData = HealthDataSummary()
            )
        }

        applyTargets()
        val zoneId = ZoneId.systemDefault()
        val startOfDay = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1

        mealsJob?.cancel()
        mealsJob = viewModelScope.launch {
            mealRepository.getMealsForDay(startOfDay, endOfDay).collect { meals ->
                val totalCal = meals.sumOf { it.meal.totalCalories }
                val totalProtein = kotlin.math.round(meals.sumOf { it.meal.totalProteinGrams.toDouble() }).toInt()
                val totalCarbs = kotlin.math.round(meals.sumOf { it.meal.totalCarbsGrams.toDouble() }).toInt()
                val totalFat = kotlin.math.round(meals.sumOf { it.meal.totalFatGrams.toDouble() }).toInt()

                _uiState.update {
                    it.copy(
                        todayMeals = meals,
                        currentCalories = totalCal,
                        proteinGrams = totalProtein,
                        carbsGrams = totalCarbs,
                        fatGrams = totalFat
                    )
                }
                recomputeInsight()
            }
        }

        workoutsJob?.cancel()
        workoutsJob = viewModelScope.launch {
            workoutRepository.getWorkoutsForDay(startOfDay, endOfDay).collect { workouts ->
                val recent = runCatching { workoutRepository.getRecentWorkouts(5) }.getOrDefault(emptyList())
                val latest = workouts.firstOrNull() ?: recent.firstOrNull()
                _uiState.update { it.copy(todayWorkouts = workouts, lastWorkout = latest) }
                recomputeInsight()
            }
        }

        waterJob?.cancel()
        waterJob = viewModelScope.launch {
            waterRepository.getWaterForDay(startOfDay, endOfDay).collect { waterTotal ->
                _uiState.update { it.copy(currentWaterMl = waterTotal) }
                recomputeInsight()
            }
        }

        refreshHealthData(date)
    }

    fun setDayComplete(complete: Boolean) {
        val date = _uiState.value.selectedDate.toString()
        repeatSave.launch(viewModelScope, {}) {
            personalDao.putDays(listOf(com.kalotracker.app.core.database.entity.DayStatusEntity(date, complete)))
        }
    }

    fun onResume() {
        if (_uiState.value.isToday && _uiState.value.selectedDate != LocalDate.now()) {
            loadDataForDate(LocalDate.now())
        } else {
            refreshHealthData()
        }
    }

    fun refreshHealthData(date: LocalDate = _uiState.value.selectedDate) {
        val request = ++healthRequest
        healthJob?.cancel()
        _uiState.update { it.copy(healthData = HealthDataSummary()) }
        healthJob = viewModelScope.launch {
            val healthSummary = healthConnectManager.readHealthDataForDate(date)
            if (request != healthRequest || date != _uiState.value.selectedDate) return@launch
            _uiState.update { it.copy(healthData = healthSummary) }
            recomputeInsight()
        }
    }

    fun logWater(milliliters: Int) {
        if (milliliters <= 0) return
        val day = _uiState.value
        viewModelScope.launch {
            val zoneId = ZoneId.systemDefault()
            val timestamp = if (day.isToday) {
                System.currentTimeMillis()
            } else {
                day.selectedDate.atTime(12, 0).atZone(zoneId).toInstant().toEpochMilli()
            }
            try {
                waterRepository.logWater(milliliters, timestamp)
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.update { it.copy(repeatMessage = "Could not save water. Check the amount and try again.") } }
        }
    }

    fun undoLastWaterLog() {
        val date = _uiState.value.selectedDate
        viewModelScope.launch {
            val zoneId = ZoneId.systemDefault()
            val startOfDay = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val endOfDay = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1
            try {
                waterRepository.undoLastWaterLog(startOfDay, endOfDay)
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.update { it.copy(repeatMessage = "Could not undo water. Try again.") } }
        }
    }

    fun deleteMeal(mealWithItems: MealWithItems) {
        viewModelScope.launch {
            _uiState.value.pendingUndo?.let { finishUndoWindow(it) }
            try {
                mealRepository.deleteMeal(mealWithItems.meal)
                _uiState.update { it.copy(pendingUndo = UndoAction.Meal(mealWithItems)) }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.update { it.copy(repeatMessage = "Could not delete that meal. Try again.") } }
        }
    }

    fun deleteWorkout(workoutWithSets: WorkoutWithSets) {
        viewModelScope.launch {
            _uiState.value.pendingUndo?.let { finishUndoWindow(it) }
            try {
                workoutRepository.deleteWorkout(workoutWithSets.workout)
                _uiState.update { it.copy(pendingUndo = UndoAction.Workout(workoutWithSets)) }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.update { it.copy(repeatMessage = "Could not delete that workout. Try again.") } }
        }
    }

    /** Once undo is no longer possible, a deleted meal's photo is removed from the phone too. */
    private suspend fun finishUndoWindow(action: UndoAction) {
        if (action !is UndoAction.Meal) return
        val path = action.item.meal.imageLocalUri ?: return
        if (mealRepository.getMeal(action.item.meal.id) != null) return
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { runCatching { java.io.File(path).delete() } }
    }

    fun undoDelete() {
        val action = _uiState.value.pendingUndo ?: return
        _uiState.update { it.copy(pendingUndo = null) }
        viewModelScope.launch {
            when (action) {
                is UndoAction.Meal -> mealRepository.restoreMeal(action.item)
                is UndoAction.Workout -> workoutRepository.restoreWorkout(action.item)
            }
        }
    }

    fun dismissUndo() {
        val action = _uiState.value.pendingUndo ?: return
        _uiState.update { it.copy(pendingUndo = null) }
        viewModelScope.launch { runCatching { finishUndoWindow(action) } }
    }

    /** Logs a copy of a meal on the day being viewed (now if today, otherwise noon of that day). */
    fun dismissRepeatMessage() { _uiState.update { it.copy(repeatMessage = null) } }

    fun logMealAgain(source: MealWithItems) {
        val s = _uiState.value
        repeatSave.launch(viewModelScope, { _uiState.update { it.copy(repeatMessage = "Meal logged.") } }) {
            val timestamp = if (s.isToday) {
                System.currentTimeMillis()
            } else {
                s.selectedDate.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            mealRepository.duplicateMeal(source, timestamp)
        }
    }

    fun logWorkoutAgain(source: WorkoutWithSets) {
        val s = _uiState.value
        workoutQuickSave.launch(viewModelScope, { _uiState.update { it.copy(repeatMessage = "Workout logged.") } }) {
            val timestamp = if (s.isToday) {
                System.currentTimeMillis()
            } else {
                s.selectedDate.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            workoutRepository.duplicateWorkout(source, timestamp)
        }
    }

    /**
     * Toggles a single bodyweight exercise ("Push-ups", "Sit-ups", "Crunches") inside the selected day's
     * "Daily fitness" session so tapping chips updates one clean workout entry rather than creating duplicates.
     */
    fun toggleDailyHabitExercise(exerciseName: String, defaultReps: Int = 20, totalMinutes: Int = 7) {
        val s = _uiState.value
        val safeReps = defaultReps.coerceIn(1, 10000)
        val safeMins = totalMinutes.coerceIn(3, 60)
        val weight = s.bodyWeightKg.coerceIn(20.0, 300.0)
        workoutQuickSave.launch(viewModelScope, {}) {
            val existing = s.todayWorkouts.firstOrNull { it.workout.title.equals("Daily fitness", ignoreCase = true) }
            val habitOrder = listOf("Push-ups", "Sit-ups", "Crunches")
            val currentReps = mutableMapOf<String, Int>()
            if (existing != null) {
                existing.sets.filter { it.isCompleted && it.exerciseName in habitOrder }.forEach { set ->
                    currentReps[set.exerciseName] = (currentReps[set.exerciseName] ?: 0) + set.reps
                }
            }
            val wasChecked = (currentReps[exerciseName] ?: 0) > 0
            if (wasChecked) {
                currentReps.remove(exerciseName)
            } else {
                currentReps[exerciseName] = safeReps
            }
            val repsList = habitOrder.map { currentReps[it] ?: 0 }
            if (repsList.all { it == 0 }) {
                if (existing != null) {
                    workoutRepository.deleteWorkout(existing.workout)
                }
                return@launch
            }
            val rows = com.kalotracker.app.feature.workout.dailyFitnessExercises(
                minutes = safeMins,
                weightKg = weight,
                effort = com.kalotracker.app.feature.workout.FitnessEffort.MODERATE,
                reps = repsList
            )
            val workoutId = existing?.workout?.id ?: java.util.UUID.randomUUID().toString()
            val timestamp = existing?.workout?.timestamp ?: if (s.isToday) {
                System.currentTimeMillis()
            } else {
                s.selectedDate.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            val entity = com.kalotracker.app.core.database.entity.WorkoutEntity(
                id = workoutId,
                title = "Daily fitness",
                type = "STRENGTH",
                durationMinutes = rows.sumOf { it.durationMinutes },
                estimatedCaloriesBurned = rows.sumOf { it.calories },
                timestamp = timestamp,
                exercisesJson = com.kalotracker.app.core.data.food.PersonalFood.json.encodeToString(
                    kotlinx.serialization.builtins.ListSerializer(com.kalotracker.app.feature.workout.WorkoutExercise.serializer()),
                    rows
                )
            )
            val setEntities = rows.flatMap { row ->
                row.sets.map { set ->
                    com.kalotracker.app.core.database.entity.ExerciseSetEntity(
                        workoutId = workoutId,
                        exerciseName = row.name,
                        setNumber = set.setNumber,
                        weightKg = set.weightKg.toFloatOrNull() ?: 0f,
                        reps = set.reps.toIntOrNull() ?: safeReps,
                        isCompleted = true
                    )
                }
            }
            if (existing == null) {
                workoutRepository.saveWorkout(entity, setEntities)
            } else {
                workoutRepository.updateWorkout(entity, setEntities)
            }
        }
    }

    fun quickLogDailyFitness(minutes: Int = 7, reps: List<Int> = listOf(20, 20, 20)) {
        val s = _uiState.value
        val safeMins = minutes.coerceIn(3, 60)
        val weight = s.bodyWeightKg.coerceIn(20.0, 300.0)
        val cleanReps = if (reps.size == 3 && reps.any { it > 0 }) reps else listOf(20, 20, 20)
        workoutQuickSave.launch(viewModelScope, { _uiState.update { it.copy(repeatMessage = "Daily fitness logged.") } }) {
            val existing = s.todayWorkouts.firstOrNull { it.workout.title.equals("Daily fitness", ignoreCase = true) }
            val rows = com.kalotracker.app.feature.workout.dailyFitnessExercises(
                minutes = safeMins,
                weightKg = weight,
                effort = com.kalotracker.app.feature.workout.FitnessEffort.MODERATE,
                reps = cleanReps
            )
            val workoutId = existing?.workout?.id ?: java.util.UUID.randomUUID().toString()
            val timestamp = existing?.workout?.timestamp ?: if (s.isToday) {
                System.currentTimeMillis()
            } else {
                s.selectedDate.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            val entity = com.kalotracker.app.core.database.entity.WorkoutEntity(
                id = workoutId,
                title = "Daily fitness",
                type = "STRENGTH",
                durationMinutes = rows.sumOf { it.durationMinutes },
                estimatedCaloriesBurned = rows.sumOf { it.calories },
                timestamp = timestamp,
                exercisesJson = com.kalotracker.app.core.data.food.PersonalFood.json.encodeToString(
                    kotlinx.serialization.builtins.ListSerializer(com.kalotracker.app.feature.workout.WorkoutExercise.serializer()),
                    rows
                )
            )
            val setEntities = rows.flatMap { row ->
                row.sets.map { set ->
                    com.kalotracker.app.core.database.entity.ExerciseSetEntity(
                        workoutId = workoutId,
                        exerciseName = row.name,
                        setNumber = set.setNumber,
                        weightKg = set.weightKg.toFloatOrNull() ?: 0f,
                        reps = set.reps.toIntOrNull() ?: 20,
                        isCompleted = true
                    )
                }
            }
            if (existing == null) {
                workoutRepository.saveWorkout(entity, setEntities)
            } else {
                workoutRepository.updateWorkout(entity, setEntities)
            }
        }
    }

    fun quickLogRoutine(routine: com.kalotracker.app.core.database.entity.WorkoutRoutineEntity) {
        val s = _uiState.value
        workoutQuickSave.launch(viewModelScope, { _uiState.update { it.copy(repeatMessage = "Logged \"${routine.name}\".") } }) {
            val rows = com.kalotracker.app.feature.workout.validateRoutinePayload(routine.payload)
            val workoutId = java.util.UUID.randomUUID().toString()
            val timestamp = if (s.isToday) {
                System.currentTimeMillis()
            } else {
                s.selectedDate.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            val entity = com.kalotracker.app.core.database.entity.WorkoutEntity(
                id = workoutId,
                title = routine.name,
                type = if (rows.all { it.cardio }) "CARDIO" else "STRENGTH",
                durationMinutes = rows.sumOf { it.durationMinutes },
                estimatedCaloriesBurned = rows.sumOf { it.calories },
                timestamp = timestamp,
                exercisesJson = routine.payload
            )
            val setEntities = rows.flatMap { row ->
                row.sets.map { set ->
                    com.kalotracker.app.core.database.entity.ExerciseSetEntity(
                        workoutId = workoutId,
                        exerciseName = row.name,
                        setNumber = set.setNumber,
                        weightKg = set.weightKg.toFloatOrNull() ?: 0f,
                        reps = set.reps.toIntOrNull() ?: 1,
                        isCompleted = true
                    )
                }
            }
            workoutRepository.saveWorkout(entity, setEntities)
        }
    }

    private fun applyTargets() {
        val state = _uiState.value
        val current = userProfileRepository.profile.value
        val goal = userProfileRepository.history.value.filter { it.date <= state.selectedDate.toString() }.maxByOrNull { it.date }
        _uiState.update { it.copy(targetCalories = goal?.calories ?: 0, targetProtein = goal?.protein ?: 0,
            targetCarbs = goal?.carbs ?: 0, targetFat = goal?.fat ?: 0, targetWaterMl = goal?.waterMl ?: 0,
            targetSteps = current.targetSteps) }
    }

    private fun recomputeInsight() {
        val s = _uiState.value
        if (s.targetCalories <= 0) { _uiState.update { it.copy(dailyInsight = null) }; return }
        val insight = NutritionInsightEngine.generateDailyInsight(
            targetCalories = s.targetCalories,
            consumedCalories = s.currentCalories,
            targetProtein = s.targetProtein,
            consumedProtein = s.proteinGrams,
            targetWaterMl = s.targetWaterMl,
            consumedWaterMl = s.currentWaterMl,
            isToday = s.isToday
        )
        _uiState.update { it.copy(dailyInsight = insight) }
    }
}

class DashboardViewModelFactory(
    private val personalDao: com.kalotracker.app.core.database.dao.PersonalDao,
    private val mealRepository: MealRepository,
    private val workoutRepository: WorkoutRepository,
    private val userProfileRepository: UserProfileRepository,
    private val waterRepository: WaterRepository,
    private val healthConnectManager: HealthConnectManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(
                personalDao,
                mealRepository,
                workoutRepository,
                userProfileRepository,
                waterRepository,
                healthConnectManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
