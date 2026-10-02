package com.kalotracker.app.feature.trends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kalotracker.app.core.ai.GoalAdvisor
import com.kalotracker.app.core.ai.GoalSuggestion
import com.kalotracker.app.core.ai.NutritionTargets
import com.kalotracker.app.core.ai.TrendAnalyzer
import com.kalotracker.app.core.ai.TrendStats
import com.kalotracker.app.core.ai.WeightPoint
import com.kalotracker.app.core.data.repository.MealRepository
import com.kalotracker.app.core.data.repository.UserProfile
import com.kalotracker.app.core.data.repository.UserProfileRepository
import com.kalotracker.app.core.data.repository.WaterRepository
import com.kalotracker.app.core.data.repository.WeightRepository
import com.kalotracker.app.core.data.repository.WorkoutRepository
import com.kalotracker.app.core.database.entity.WeightLogEntity
import com.kalotracker.app.core.network.MealAnalysisService
import com.kalotracker.app.core.settings.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class TrendsUiState(
    val rangeDays: Int = 7,
    val isLoading: Boolean = true,
    val stats: TrendStats? = null,
    val targets: NutritionTargets = NutritionTargets(2200, 160, 2500),
    val insights: List<String> = emptyList(),
    val profile: UserProfile = UserProfile(),
    val weights: List<WeightLogEntity> = emptyList(), // newest first
    val weightInput: String = "",
    val weightError: String? = null,
    val goalSuggestion: GoalSuggestion? = null,
    val aiConfigured: Boolean = false,
    val isSummarizing: Boolean = false,
    val aiSummary: String? = null,
    val aiError: String? = null
)

class TrendsViewModel(
    private val mealRepository: MealRepository,
    private val workoutRepository: WorkoutRepository,
    private val waterRepository: WaterRepository,
    private val weightRepository: WeightRepository,
    private val userProfileRepository: UserProfileRepository,
    private val appSettings: AppSettings,
    private val analysisService: MealAnalysisService
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrendsUiState(aiConfigured = appSettings.ai.value.isConfigured))
    val uiState: StateFlow<TrendsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userProfileRepository.profile.collect { profile ->
                _uiState.update {
                    it.copy(
                        profile = profile,
                        targets = NutritionTargets(profile.targetCalories, profile.targetProtein, profile.targetWaterMl)
                    )
                }
                recomputeGoalSuggestion()
                loadRange()
            }
        }
        viewModelScope.launch {
            weightRepository.observeAll().collect { list ->
                _uiState.update { it.copy(weights = list) }
                recomputeGoalSuggestion()
            }
        }
    }

    fun setRange(days: Int) {
        if (days == _uiState.value.rangeDays) return
        _uiState.update { it.copy(rangeDays = days, aiSummary = null, aiError = null) }
        viewModelScope.launch { loadRange() }
    }

    private suspend fun loadRange() {
        val state = _uiState.value
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val start = today.minusDays((state.rangeDays - 1).toLong())
        val startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

        val meals = mealRepository.getMealsBetween(startMillis, endMillis)
        val water = waterRepository.getLogsBetween(startMillis, endMillis)
        val workouts = workoutRepository.getWorkoutsBetween(startMillis, endMillis)

        val days = TrendAnalyzer.buildDays(start, today, meals, water, workouts, zone)
        val stats = TrendAnalyzer.stats(days, state.targets, today)
        _uiState.update {
            it.copy(
                isLoading = false,
                stats = stats,
                insights = TrendAnalyzer.insights(stats, state.targets)
            )
        }
    }

    private fun recomputeGoalSuggestion() {
        val s = _uiState.value
        // Use the last 6 weeks so the trend reflects current behaviour, not old history.
        val cutoff = System.currentTimeMillis() - 42L * 24 * 60 * 60 * 1000
        val points = s.weights.filter { it.timestamp >= cutoff }.map { WeightPoint(it.timestamp, it.weightKg) }
        val suggestion = GoalAdvisor.suggest(s.profile.goal, GoalAdvisor.weeklyChangeKg(points), s.profile.targetCalories)
        _uiState.update { it.copy(goalSuggestion = suggestion) }
    }

    fun setWeightInput(text: String) {
        _uiState.update { it.copy(weightInput = text.filter { c -> c.isDigit() || c == '.' }.take(6), weightError = null) }
    }

    fun logWeight() {
        val kg = _uiState.value.weightInput.toFloatOrNull()
        if (kg == null || kg < 20f || kg > 350f) {
            _uiState.update { it.copy(weightError = "Enter a weight between 20 and 350 kg.") }
            return
        }
        viewModelScope.launch {
            weightRepository.log(kg)
            _uiState.update { it.copy(weightInput = "", weightError = null) }
        }
    }

    fun deleteWeight(log: WeightLogEntity) {
        viewModelScope.launch { weightRepository.delete(log) }
    }

    fun applySuggestedTarget() {
        val target = _uiState.value.goalSuggestion?.newCalorieTarget ?: return
        userProfileRepository.setCalorieTargetKeepingSplit(target)
    }

    fun generateAiSummary() {
        val s = _uiState.value
        val stats = s.stats ?: return
        if (stats.loggedDays < 3 || s.isSummarizing) return
        _uiState.update { it.copy(isSummarizing = true, aiError = null, aiSummary = null, aiConfigured = appSettings.ai.value.isConfigured) }

        val prompt = buildString {
            appendLine("You are a supportive, practical nutrition coach. This is a user's food log for the last ${s.rangeDays} days.")
            appendLine("Targets per day: ${s.targets.calories} kcal, ${s.targets.protein} g protein, ${s.targets.waterMl} ml water. Goal: ${s.profile.goal.label}.")
            appendLine("Daily totals (date: kcal / protein g / water ml / meals / workouts):")
            stats.days.forEach { d ->
                appendLine("${d.date}: ${if (d.isLogged) "${d.calories} / ${d.protein.toInt()} / ${d.waterMl} / ${d.mealCount} / ${d.workoutCount}" else "nothing logged"}")
            }
            appendLine("Write 3 short observations about patterns and 2 specific, realistic suggestions. Be kind, not preachy, under 140 words. Do not give medical advice or diagnose. Mention that unlogged days limit accuracy if there are many.")
        }

        viewModelScope.launch {
            val result = analysisService.generateText(prompt)
            _uiState.update {
                it.copy(
                    isSummarizing = false,
                    aiSummary = result.getOrNull(),
                    aiError = result.exceptionOrNull()?.localizedMessage
                )
            }
        }
    }
}

class TrendsViewModelFactory(
    private val mealRepository: MealRepository,
    private val workoutRepository: WorkoutRepository,
    private val waterRepository: WaterRepository,
    private val weightRepository: WeightRepository,
    private val userProfileRepository: UserProfileRepository,
    private val appSettings: AppSettings,
    private val analysisService: MealAnalysisService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TrendsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TrendsViewModel(
                mealRepository, workoutRepository, waterRepository, weightRepository,
                userProfileRepository, appSettings, analysisService
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
