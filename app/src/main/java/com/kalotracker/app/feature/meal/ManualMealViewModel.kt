package com.kalotracker.app.feature.meal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kalotracker.app.core.database.entity.FoodItemEntity
import com.kalotracker.app.core.database.dao.MealWithItems
import com.kalotracker.app.core.data.repository.MealRepository
import com.kalotracker.app.core.network.MealAnalysisService
import com.kalotracker.app.core.util.SaveOperation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Foods Gemini estimated from a typed description, waiting to join the draft. */
data class TextEstimate(val title: String, val items: List<FoodItemEntity>, val notes: String?)

data class RecentMealsUiState(
    val meals: List<MealWithItems> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/** Keeps an in-flight save and its completion alive across screen recreation. */
class ManualMealViewModel(
    private val analysisService: MealAnalysisService? = null,
    private val mealRepository: MealRepository? = null
) : ViewModel() {
    private val saved = MutableStateFlow(false)
    val isSaved = saved.asStateFlow()
    val saveOperation = SaveOperation()
    private val _recentMeals = MutableStateFlow(RecentMealsUiState())
    val recentMeals = _recentMeals.asStateFlow()

    init { loadRecentMeals() }

    fun loadRecentMeals() {
        val repository = mealRepository ?: return
        if (_recentMeals.value.isLoading) return
        _recentMeals.value = _recentMeals.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                _recentMeals.value = RecentMealsUiState(meals = repository.getRecentMealsForSearch())
            } catch (cancel: kotlinx.coroutines.CancellationException) {
                throw cancel
            } catch (_: Exception) {
                _recentMeals.value = _recentMeals.value.copy(isLoading = false, error = "Couldn't load recent meals. Try again.")
            }
        }
    }

    val canEstimate: Boolean get() = analysisService?.isConfigured == true
    private val _estimating = MutableStateFlow(false)
    val estimating = _estimating.asStateFlow()
    private val _estimateError = MutableStateFlow<String?>(null)
    val estimateError = _estimateError.asStateFlow()
    private val _estimate = MutableStateFlow<TextEstimate?>(null)
    /** Held until the screen adds it to the draft, so a rotation mid-request loses nothing. */
    val estimate = _estimate.asStateFlow()

    fun save(persist: suspend () -> Unit) {
        if (saved.value || _estimating.value || _estimate.value != null) return
        saveOperation.launch(viewModelScope, { saved.value = true }, persist)
    }

    /** Asks Gemini for foods and nutrition from words like "chicken bharta and 2 rotis". */
    fun estimate(description: String) {
        val service = analysisService ?: return
        if (!canEstimate) {
            _estimateError.value = com.kalotracker.app.core.network.MealAnalysisException.NotConfigured().message
            return
        }
        if (_estimating.value || _estimate.value != null || saved.value || saveOperation.status.value.busy || description.isBlank()) return
        _estimating.value = true
        _estimateError.value = null
        viewModelScope.launch {
            service.analyzeMealText(description)
                .onSuccess { response ->
                    _estimate.value = TextEstimate(response.mealTitle, response.items.map { it.toFoodItem() },
                        response.estimationNotes?.takeIf { it.isNotBlank() })
                }
                .onFailure { _estimateError.value = it.localizedMessage ?: "Couldn't estimate that meal." }
            _estimating.value = false
        }
    }

    fun consumeEstimate() {
        _estimate.value = null
    }
}

class ManualMealViewModelFactory(
    private val analysisService: MealAnalysisService,
    private val mealRepository: MealRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ManualMealViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ManualMealViewModel(analysisService, mealRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
