package com.kalotracker.app.feature.meal

import androidx.lifecycle.ViewModel
import com.kalotracker.app.core.util.SaveOperation
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kalotracker.app.core.data.food.FoodCatalogItem
import com.kalotracker.app.core.data.repository.MealRepository
import com.kalotracker.app.core.database.entity.FoodItemEntity
import com.kalotracker.app.core.database.entity.MealEntity
import com.kalotracker.app.core.network.MealAnalysisService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditMealUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val title: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val items: List<EditableFoodItem> = emptyList(),
    val notes: String = "",
    val imageLocalUri: String? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    /** Food being re-estimated from its typed name; saving waits for it. */
    val estimatingItemId: String? = null
) {
    val totalCalories: Int get() = items.sumOf { it.currentCalories }
    val totalProtein: Float get() = items.map { it.currentProtein }.sum()
    val totalCarbs: Float get() = items.map { it.currentCarbs }.sum()
    val totalFat: Float get() = items.map { it.currentFat }.sum()
}

class EditMealViewModel(
    private val mealId: String,
    private val mealRepository: MealRepository,
    private val analysisService: MealAnalysisService? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditMealUiState())
    val uiState: StateFlow<EditMealUiState> = _uiState.asStateFlow()
    val saveOperation = SaveOperation { status ->
        _uiState.update { it.copy(isSaving = status.busy, errorMessage = status.error) }
    }

    private var original: MealEntity? = null

    init {
        viewModelScope.launch {
            val loaded = try { mealRepository.getMeal(mealId) }
                catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
                catch (e: Exception) { _uiState.update { it.copy(isLoading = false, notFound = true, errorMessage = "Could not load meal. Close and try again.") }; return@launch }
            if (loaded == null) {
                _uiState.update { it.copy(isLoading = false, notFound = true) }
                return@launch
            }
            original = loaded.meal
            _uiState.update {
                EditMealUiState(
                    isLoading = false,
                    title = loaded.meal.title,
                    timestamp = loaded.meal.timestamp,
                    notes = loaded.meal.notes.orEmpty(),
                    imageLocalUri = loaded.meal.imageLocalUri,
                    items = loaded.items.map { item ->
                        val grams = item.portionGrams.coerceAtLeast(1f)
                        EditableFoodItem(
                            id = item.id,
                            name = item.name,
                            portionGrams = grams,
                            baseCaloriesPerGram = item.calories / grams,
                            baseProteinPerGram = item.protein / grams,
                            baseCarbsPerGram = item.carbs / grams,
                            baseFatPerGram = item.fat / grams,
                            confidence = item.confidence,
                            unit = com.kalotracker.app.core.network.portionUnit(item.unit)
                        )
                    }
                )
            }
        }
    }

    fun setTitle(value: String) = _uiState.update { it.copy(title = value) }
    fun setTimestamp(value: Long) = _uiState.update { it.copy(timestamp = value) }
    fun setNotes(value: String) = _uiState.update { it.copy(notes = value) }

    fun setItemName(id: String, name: String) = updateItem(id) { it.renamed(name) }
    fun keepItemNutrition(id: String) = updateItem(id) { if (it.name.isNotBlank()) it.keepNutrition() else it }
    fun setItemGrams(id: String, grams: Float) { if (grams.isFinite()) updateItem(id) { it.copy(portionGrams = grams.coerceIn(1f, 5000f), amountInput = null) } }
    fun setItemAmountInput(id: String, text: String) = updateItem(id) { it.withPortionInput(text) }
    /** Asks Gemini again for one food using the name as typed; amount and name stay as entered. */
    fun reestimateItem(id: String) {
        val service = analysisService ?: return
        val s = _uiState.value
        val item = s.items.firstOrNull { it.id == id } ?: return
        if (item.name.isBlank() || !item.hasValidPortion || s.isSaving || s.estimatingItemId != null) return
        _uiState.update { it.copy(estimatingItemId = id, errorMessage = null) }
        viewModelScope.launch {
            val result = service.analyzeMealText(item.estimateDescription()).mapCatching { item.withEstimate(it) }
            _uiState.update { current ->
                if (current.items.firstOrNull { it.id == id } != item) {
                    return@update current.copy(estimatingItemId = null)
                }
                result.fold(
                    onSuccess = { estimated ->
                        current.copy(estimatingItemId = null,
                            items = current.items.map { if (it.id == id) estimated else it })
                    },
                    onFailure = { error ->
                        current.copy(estimatingItemId = null, errorMessage = error.localizedMessage ?: "Couldn't re-estimate ${item.name}")
                    }
                )
            }
        }
    }

    fun correctNutrition(id: String, kcal: Int, p: Float, c: Float, f: Float) {
        updateItem(id) { it.correctedNutrition(kcal, p, c, f) }
    }
    fun removeItem(id: String) = _uiState.update { s -> s.copy(items = s.items.filterNot { it.id == id }) }

    fun addCatalogItem(food: FoodCatalogItem) {
        val item = EditableFoodItem(
            name = food.name,
            portionGrams = food.defaultServingGrams,
            baseCaloriesPerGram = food.caloriesPer100g / 100f,
            baseProteinPerGram = food.proteinPer100g / 100f,
            baseCarbsPerGram = food.carbsPer100g / 100f,
            baseFatPerGram = food.fatPer100g / 100f,
            confidence = 1f
        )
        _uiState.update { it.copy(items = it.items + item) }
    }

    private fun updateItem(id: String, transform: (EditableFoodItem) -> EditableFoodItem) {
        _uiState.update { s -> s.copy(items = s.items.map { if (it.id == id) transform(it) else it }) }
    }

    fun save(onDone: () -> Unit) {
        val s = _uiState.value
        val base = original ?: return
        if (s.items.isEmpty() || s.isSaving || s.estimatingItemId != null) return
        if (s.items.any { !it.hasValidPortion }) {
            _uiState.update { it.copy(errorMessage = "Check each food amount before saving (1–5000 g or ml).") }
            return
        }
        if (s.items.any { !it.isReadyToSave }) {
            _uiState.update { it.copy(errorMessage = "Review renamed foods: update, edit, or explicitly keep their nutrition values before saving.") }
            return
        }
        saveOperation.launch(viewModelScope, onDone) {
            val meal = base.copy(
                title = s.title.ifBlank { "Logged Meal" },
                totalCalories = s.totalCalories,
                totalProteinGrams = s.totalProtein,
                totalCarbsGrams = s.totalCarbs,
                totalFatGrams = s.totalFat,
                notes = s.notes.ifBlank { null },
                timestamp = s.timestamp
            )
            val items = s.items.map {
                FoodItemEntity(
                    id = it.id,
                    mealId = base.id,
                    name = it.name.ifBlank { "Food" },
                    portionGrams = it.portionGrams,
                    calories = it.currentCalories,
                    protein = it.currentProtein,
                    carbs = it.currentCarbs,
                    fat = it.currentFat,
                    confidence = it.confidence,
                    unit = it.unit
                )
            }
            mealRepository.updateMeal(meal, items)

        }
    }
}

class EditMealViewModelFactory(
    private val mealId: String,
    private val mealRepository: MealRepository,
    private val analysisService: MealAnalysisService? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditMealViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditMealViewModel(mealId, mealRepository, analysisService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
