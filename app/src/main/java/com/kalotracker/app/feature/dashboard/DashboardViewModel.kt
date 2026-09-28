package com.kalotracker.app.feature.dashboard

import androidx.lifecycle.ViewModel
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
import com.kalotracker.app.core.database.entity.MealEntity
import com.kalotracker.app.core.database.entity.WorkoutEntity
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

data class DashboardUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val isToday: Boolean = true,
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
    val isLoading: Boolean = false
)

class DashboardViewModel(
    private val mealRepository: MealRepository,
    private val workoutRepository: WorkoutRepository,
    private val userProfileRepository: UserProfileRepository,
    private val waterRepository: WaterRepository,
    private val healthConnectManager: HealthConnectManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var mealsJob: Job? = null
    private var workoutsJob: Job? = null
    private var waterJob: Job? = null

    init {
        // Observe profile changes (targets)
        viewModelScope.launch {
            userProfileRepository.profile.collect { profile ->
                _uiState.update {
                    it.copy(
                        targetCalories = profile.targetCalories,
                        targetProtein = profile.targetProtein,
                        targetCarbs = profile.targetCarbs,
                        targetFat = profile.targetFat,
                        targetSteps = profile.targetSteps,
                        targetWaterMl = profile.targetWaterMl
                    )
                }
                recomputeInsight()
            }
        }

        loadDataForDate(LocalDate.now())
        refreshHealthData()
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
                isToday = isToday
            )
        }

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

    fun refreshHealthData(date: LocalDate = _uiState.value.selectedDate) {
        viewModelScope.launch {
            val healthSummary = healthConnectManager.readHealthDataForDate(date)
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

    fun deleteMeal(meal: MealEntity) {
        viewModelScope.launch {
            mealRepository.deleteMeal(meal)
        }
    }

    fun deleteWorkout(workout: WorkoutEntity) {
        viewModelScope.launch {
            workoutRepository.deleteWorkout(workout)
        }
    }

    private fun recomputeInsight() {
        val s = _uiState.value
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
