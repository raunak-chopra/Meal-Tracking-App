package com.kalotracker.app.feature.meal

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kalotracker.app.core.data.food.FoodCatalogItem
import com.kalotracker.app.core.data.repository.MealRepository
import com.kalotracker.app.core.database.entity.FoodItemEntity
import com.kalotracker.app.core.database.entity.MealEntity
import com.kalotracker.app.core.network.MealAnalysisService
import com.kalotracker.app.core.util.ImageUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

const val ADDED_OIL_GRAMS = 14f
const val ADDED_OIL_CALORIES = 120
const val ADDED_OIL_NAME = "Cooking oil / butter (1 tbsp)"

data class EditableFoodItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val portionGrams: Float,
    val baseCaloriesPerGram: Float,
    val baseProteinPerGram: Float,
    val baseCarbsPerGram: Float,
    val baseFatPerGram: Float,
    val confidence: Float
) {
    val currentCalories: Int get() = (portionGrams * baseCaloriesPerGram).toInt()
    val currentProtein: Float get() = portionGrams * baseProteinPerGram
    val currentCarbs: Float get() = portionGrams * baseCarbsPerGram
    val currentFat: Float get() = portionGrams * baseFatPerGram
}

data class MealScanUiState(
    val isAnalyzing: Boolean = false,
    val isSaving: Boolean = false,
    val hasAddedOil: Boolean = false,
    val mealTitle: String = "",
    val items: List<EditableFoodItem> = emptyList(),
    val confidence: Float = 0.9f,
    val notes: String? = null,
    val userNote: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val localImageUri: String? = null,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val canRetry: Boolean = false
) {
    val totalCalories: Int
        get() = items.sumOf { it.currentCalories } + (if (hasAddedOil) ADDED_OIL_CALORIES else 0)

    val totalProtein: Float
        get() = items.map { it.currentProtein }.sum()

    val totalCarbs: Float
        get() = items.map { it.currentCarbs }.sum()

    val totalFat: Float
        get() = items.map { it.currentFat }.sum() + (if (hasAddedOil) ADDED_OIL_GRAMS else 0f)
}

class MealViewModel(
    private val mealRepository: MealRepository,
    private val analysisService: MealAnalysisService
) : ViewModel() {

    private val _uiState = MutableStateFlow(MealScanUiState())
    val uiState: StateFlow<MealScanUiState> = _uiState.asStateFlow()

    /** Last captured image, kept so a failed or refined analysis can be retried without re-shooting. */
    private var lastImageBytes: ByteArray? = null

    fun toggleAddedOil() {
        _uiState.update { it.copy(hasAddedOil = !it.hasAddedOil) }
    }

    fun setUserNote(note: String) {
        _uiState.update { it.copy(userNote = note.take(300)) }
    }

    fun setMealTitle(title: String) {
        _uiState.update { it.copy(mealTitle = title) }
    }

    fun setTimestamp(millis: Long) {
        _uiState.update { it.copy(timestamp = millis) }
    }

    fun analyzeCapturedImage(imageBytes: ByteArray, localImageUri: String? = null) {
        lastImageBytes = imageBytes
        val previous = _uiState.value.localImageUri
        if (localImageUri != null && previous != null && previous != localImageUri) {
            runCatching { File(previous).delete() } // replaced by a newer photo, never saved
        }
        _uiState.update { it.copy(localImageUri = localImageUri ?: it.localImageUri) }
        runAnalysis()
    }

    /** Re-runs analysis on the same photo, e.g. after failure or after adding a clarifying note. */
    fun retryAnalysis() {
        if (lastImageBytes != null) runAnalysis()
    }

    private fun runAnalysis() {
        val bytes = lastImageBytes ?: return
        _uiState.update { it.copy(isAnalyzing = true, errorMessage = null, canRetry = false) }

        viewModelScope.launch {
            val result = analysisService.analyzeMealImage(
                imageBytes = bytes,
                userNote = _uiState.value.userNote.ifBlank { null }
            )

            result.onSuccess { response ->
                val editableItems = response.items.map { item ->
                    val grams = item.portionGrams.coerceAtLeast(1f)
                    EditableFoodItem(
                        name = item.name,
                        portionGrams = grams,
                        baseCaloriesPerGram = item.calories / grams,
                        baseProteinPerGram = item.protein / grams,
                        baseCarbsPerGram = item.carbs / grams,
                        baseFatPerGram = item.fat / grams,
                        confidence = item.confidence
                    )
                }

                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        mealTitle = response.mealTitle.ifBlank { editableItems.first().name },
                        items = editableItems,
                        confidence = response.confidence,
                        notes = response.estimationNotes
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        canRetry = true,
                        errorMessage = error.localizedMessage ?: "Failed to analyze photo"
                    )
                }
            }
        }
    }

    fun analyzeImageFromGallery(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isAnalyzing = true, errorMessage = null) }

                // Read EXIF orientation to prevent sideways/upside-down photos
                val rotationDegrees = try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val exif = android.media.ExifInterface(stream)
                        when (exif.getAttributeInt(
                            android.media.ExifInterface.TAG_ORIENTATION,
                            android.media.ExifInterface.ORIENTATION_NORMAL
                        )) {
                            android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90
                            android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
                            android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
                            else -> 0
                        }
                    } ?: 0
                } catch (_: Exception) {
                    0
                }

                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("Could not read image stream")

                val processed = ImageUtils.processBytes(context, bytes, rotationDegrees)
                analyzeCapturedImage(processed.compressedBytes, processed.localUri)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        errorMessage = "Could not process selected image: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun adjustItemWeight(itemId: String, multiplier: Float) {
        updateItem(itemId) { item ->
            item.copy(portionGrams = (item.portionGrams * multiplier).coerceIn(1f, 5000f))
        }
    }

    fun setItemGrams(itemId: String, grams: Float) {
        updateItem(itemId) { it.copy(portionGrams = grams.coerceIn(1f, 5000f)) }
    }

    fun setItemName(itemId: String, name: String) {
        updateItem(itemId) { it.copy(name = name) }
    }

    fun removeItem(itemId: String) {
        _uiState.update { state -> state.copy(items = state.items.filterNot { it.id == itemId }) }
    }

    fun addCatalogItem(food: FoodCatalogItem) {
        val grams = food.defaultServingGrams
        val item = EditableFoodItem(
            name = food.name,
            portionGrams = grams,
            baseCaloriesPerGram = food.caloriesPer100g / 100f,
            baseProteinPerGram = food.proteinPer100g / 100f,
            baseCarbsPerGram = food.carbsPer100g / 100f,
            baseFatPerGram = food.fatPer100g / 100f,
            confidence = 1f
        )
        _uiState.update { it.copy(items = it.items + item) }
    }

    private fun updateItem(itemId: String, transform: (EditableFoodItem) -> EditableFoodItem) {
        _uiState.update { state ->
            state.copy(items = state.items.map { if (it.id == itemId) transform(it) else it })
        }
    }

    fun saveMeal(onSuccess: () -> Unit) {
        val currentState = _uiState.value
        if (currentState.items.isEmpty() || currentState.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val mealId = UUID.randomUUID().toString()
            val mealEntity = MealEntity(
                id = mealId,
                title = currentState.mealTitle.ifBlank { "Logged Meal" },
                totalCalories = currentState.totalCalories,
                totalProteinGrams = currentState.totalProtein,
                totalCarbsGrams = currentState.totalCarbs,
                totalFatGrams = currentState.totalFat,
                imageLocalUri = currentState.localImageUri,
                notes = listOfNotNull(
                    currentState.notes?.takeIf { it.isNotBlank() }?.let { "AI estimate: $it" },
                    currentState.userNote.takeIf { it.isNotBlank() }?.let { "Note: $it" }
                ).joinToString("\n").ifBlank { null },
                timestamp = currentState.timestamp
            )

            val foodEntities = currentState.items.map { item ->
                FoodItemEntity(
                    mealId = mealId,
                    name = item.name.ifBlank { "Food" },
                    portionGrams = item.portionGrams,
                    calories = item.currentCalories,
                    protein = item.currentProtein,
                    carbs = item.currentCarbs,
                    fat = item.currentFat,
                    confidence = item.confidence
                )
            } + if (currentState.hasAddedOil) {
                // Stored as a real item so the meal total always equals the sum of its items.
                listOf(
                    FoodItemEntity(
                        mealId = mealId,
                        name = ADDED_OIL_NAME,
                        portionGrams = ADDED_OIL_GRAMS,
                        calories = ADDED_OIL_CALORIES,
                        protein = 0f,
                        carbs = 0f,
                        fat = ADDED_OIL_GRAMS,
                        confidence = 1f
                    )
                )
            } else emptyList()

            mealRepository.saveMeal(mealEntity, foodEntities)
            // Photo now belongs to the saved meal; never delete it in onCleared.
            _uiState.update {
                it.copy(isSaving = false, isSaved = true, items = emptyList(), mealTitle = "")
            }
            onSuccess()
        }
    }

    fun resetScan() {
        discardUnsavedPhoto()
        lastImageBytes = null
        _uiState.value = MealScanUiState()
    }

    fun setError(message: String) {
        _uiState.update { it.copy(isAnalyzing = false, isSaving = false, errorMessage = message) }
    }

    private fun discardUnsavedPhoto() {
        val state = _uiState.value
        if (!state.isSaved) state.localImageUri?.let { runCatching { File(it).delete() } }
    }

    override fun onCleared() {
        discardUnsavedPhoto()
        super.onCleared()
    }
}

class MealViewModelFactory(
    private val mealRepository: MealRepository,
    private val analysisService: MealAnalysisService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MealViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MealViewModel(mealRepository, analysisService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
