package com.kalotracker.app.feature.meal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kalotracker.app.core.data.food.FoodCatalogItem
import com.kalotracker.app.core.data.repository.MealRepository
import com.kalotracker.app.core.database.entity.FoodItemEntity
import com.kalotracker.app.core.database.entity.MealEntity
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
    val isSaving: Boolean = false
) {
    val totalCalories: Int get() = items.sumOf { it.currentCalories }
    val totalProtein: Float get() = items.map { it.currentProtein }.sum()
    val totalCarbs: Float get() = items.map { it.currentCarbs }.sum()
    val totalFat: Float get() = items.map { it.currentFat }.sum()
}

class EditMealViewModel(
    private val mealId: String,
    private val mealRepository: MealRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditMealUiState())
    val uiState: StateFlow<EditMealUiState> = _uiState.asStateFlow()

    private var original: MealEntity? = null

    init {
        viewModelScope.launch {
            val loaded = mealRepository.getMeal(mealId)
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
                            confidence = item.confidence
                        )
                    }
                )
            }
        }
    }

    fun setTitle(value: String) = _uiState.update { it.copy(title = value) }
    fun setTimestamp(value: Long) = _uiState.update { it.copy(timestamp = value) }
    fun setNotes(value: String) = _uiState.update { it.copy(notes = value) }

    fun setItemName(id: String, name: String) = updateItem(id) { it.copy(name = name) }
    fun setItemGrams(id: String, grams: Float) = updateItem(id) { it.copy(portionGrams = grams.coerceIn(1f, 5000f)) }
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
        if (s.items.isEmpty() || s.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
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
                    confidence = it.confidence
                )
            }
            mealRepository.updateMeal(meal, items)
            _uiState.update { it.copy(isSaving = false) }
            onDone()
        }
    }
}

class EditMealViewModelFactory(
    private val mealId: String,
    private val mealRepository: MealRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditMealViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditMealViewModel(mealId, mealRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
