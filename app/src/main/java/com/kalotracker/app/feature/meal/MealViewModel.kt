package com.kalotracker.app.feature.meal

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
import java.util.UUID

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
    val localImageUri: String? = null,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
) {
    val totalCalories: Int
        get() = items.sumOf { it.currentCalories } + (if (hasAddedOil) 120 else 0)

    val totalProtein: Float
        get() = items.map { it.currentProtein }.sum()

    val totalCarbs: Float
        get() = items.map { it.currentCarbs }.sum()

    val totalFat: Float
        get() = items.map { it.currentFat }.sum() + (if (hasAddedOil) 14f else 0f)
}

class MealViewModel(
    private val mealRepository: MealRepository,
    private val analysisService: MealAnalysisService = MealAnalysisService()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MealScanUiState())
    val uiState: StateFlow<MealScanUiState> = _uiState.asStateFlow()

    fun toggleAddedOil() {
        _uiState.update { it.copy(hasAddedOil = !it.hasAddedOil) }
    }

    fun analyzeCapturedImage(imageBytes: ByteArray, localImageUri: String? = null) {
        _uiState.update {
            it.copy(
                isAnalyzing = true,
                errorMessage = null,
                localImageUri = localImageUri
            )
        }

        viewModelScope.launch {
            val result = analysisService.analyzeMealImage(
                imageBytes = imageBytes,
                hasAddedOil = _uiState.value.hasAddedOil
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
                        mealTitle = response.mealTitle,
                        items = editableItems,
                        confidence = response.confidence,
                        notes = response.estimationNotes
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
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
        _uiState.update { state ->
            val updated = state.items.map { item ->
                if (item.id == itemId) {
                    val newWeight = (item.portionGrams * multiplier).coerceIn(10f, 2000f)
                    item.copy(portionGrams = newWeight)
                } else {
                    item
                }
            }
            state.copy(items = updated)
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
                notes = currentState.notes
            )

            val foodEntities = currentState.items.map { item ->
                FoodItemEntity(
                    mealId = mealId,
                    name = item.name,
                    portionGrams = item.portionGrams,
                    calories = item.currentCalories,
                    protein = item.currentProtein,
                    carbs = item.currentCarbs,
                    fat = item.currentFat,
                    confidence = item.confidence
                )
            }

            mealRepository.saveMeal(mealEntity, foodEntities)
            _uiState.update {
                it.copy(
                    isSaving = false,
                    isSaved = true,
                    items = emptyList(),
                    mealTitle = ""
                )
            }
            onSuccess()
        }
    }

    fun resetScan() {
        _uiState.value = MealScanUiState()
    }

    fun setError(message: String) {
        _uiState.update { it.copy(isAnalyzing = false, isSaving = false, errorMessage = message) }
    }
}

class MealViewModelFactory(
    private val mealRepository: MealRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MealViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MealViewModel(mealRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
