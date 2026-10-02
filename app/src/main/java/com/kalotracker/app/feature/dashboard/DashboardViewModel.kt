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
    val dailyInsight: NutritionInsight? = null,
    val pendingUndo: UndoAction? = null,
    val isLoading: Boolean = false,
    val isRepeatingMeal: Boolean = false,
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

        viewModelScope.launch { userProfileRepository.history.collect { applyTargets(); recomputeInsight() } }
        loadDataForDate(LocalDate.now())
    }

    fun goToPreviousDay() {
        val prev = _uiState.value.selectedDate.minusDays(1)
        loadDataForDate(prev)
    }

    fun goToNextDay() {
        val next = _uiState.value.selectedDate.plusDays(1)
        loadDataForDate(next)
    }

    fun goToToday() {
        loadDataForDate(LocalDate.now())
    }

    fun loadDataForDate(date: LocalDate) {
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
                val totalProtein = meals.sumOf { it.meal.totalProteinGrams.toDouble() }.toInt()
                val totalCarbs = meals.sumOf { it.meal.totalCarbsGrams.toDouble() }.toInt()
                val totalFat = meals.sumOf { it.meal.totalFatGrams.toDouble() }.toInt()

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
                _uiState.update { it.copy(todayWorkouts = workouts) }
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
        healthJob = viewModelScope.launch {
            val healthSummary = healthConnectManager.readHealthDataForDate(date)
            if (request != healthRequest || date != _uiState.value.selectedDate) return@launch
            _uiState.update { it.copy(healthData = healthSummary) }
            recomputeInsight()
        }
    }

    fun logWater(milliliters: Int) {
        viewModelScope.launch {
            val zoneId = ZoneId.systemDefault()
            val timestamp = if (_uiState.value.isToday) {
                System.currentTimeMillis()
            } else {
                _uiState.value.selectedDate.atTime(12, 0).atZone(zoneId).toInstant().toEpochMilli()
            }
            waterRepository.logWater(milliliters, timestamp)
        }
    }

    fun undoLastWaterLog() {
        viewModelScope.launch {
            val zoneId = ZoneId.systemDefault()
            val startOfDay = _uiState.value.selectedDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val endOfDay = _uiState.value.selectedDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1
            waterRepository.undoLastWaterLog(startOfDay, endOfDay)
        }
    }

    fun deleteMeal(mealWithItems: MealWithItems) {
        viewModelScope.launch {
            mealRepository.deleteMeal(mealWithItems.meal)
            _uiState.update { it.copy(pendingUndo = UndoAction.Meal(mealWithItems)) }
        }
    }

    fun deleteWorkout(workoutWithSets: WorkoutWithSets) {
        viewModelScope.launch {
            workoutRepository.deleteWorkout(workoutWithSets.workout)
            _uiState.update { it.copy(pendingUndo = UndoAction.Workout(workoutWithSets)) }
        }
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
        _uiState.update { it.copy(pendingUndo = null) }
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
            targetCarbs = s.targetCarbs,
            consumedCarbs = s.carbsGrams,
            targetFat = s.targetFat,
            consumedFat = s.fatGrams,
            workoutCount = s.todayWorkouts.size,
            activeBurn = s.healthData.activeCaloriesBurned,
            targetWaterMl = s.targetWaterMl,
            consumedWaterMl = s.currentWaterMl
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
