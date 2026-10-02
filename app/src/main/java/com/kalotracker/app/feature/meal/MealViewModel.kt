package com.kalotracker.app.feature.meal

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.kalotracker.app.core.util.SaveOperation
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
import kotlinx.coroutines.isActive
import java.io.File
import java.util.UUID

const val ADDED_OIL_GRAMS = 14f
const val ADDED_OIL_CALORIES = 120
const val ADDED_OIL_NAME = "Cooking oil / butter (1 tbsp)"

@kotlinx.serialization.Serializable
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

@kotlinx.serialization.Serializable
data class MealScanUiState(
    val draftMealId: String = UUID.randomUUID().toString(),
    val draftLoading: Boolean = false,
    val resumePending: Boolean = false,
    val draftMessage: String? = null,
    val isAnalyzing: Boolean = false,
    val isSaving: Boolean = false,
    val hasAddedOil: Boolean = false,
    val cookingFat: CookingFat = CookingFat.OIL,
    val cookingFatGrams: Float = 14f,
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
        get() = items.sumOf { it.currentCalories } + (if (hasAddedOil) kotlin.math.round(cookingFatGrams * cookingFat.kcalPerGram).toInt() else 0)

    val totalProtein: Float
        get() = items.map { it.currentProtein }.sum()

    val totalCarbs: Float
        get() = items.map { it.currentCarbs }.sum()

    val totalFat: Float
        get() = items.map { it.currentFat }.sum() + (if (hasAddedOil) cookingFatGrams * cookingFat.fatPerGram else 0f)
}

class MealViewModel(
    private val mealRepository: MealRepository,
    private val analysisService: MealAnalysisService,
    private val initialTimestamp: Long = System.currentTimeMillis(),
    private val draftStore: MealDraftStore? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(MealScanUiState(timestamp = initialTimestamp, draftLoading = draftStore != null))
    val uiState: StateFlow<MealScanUiState> = _uiState.asStateFlow()
    val saveOperation = SaveOperation { status ->
        _uiState.update { it.copy(isSaving = status.busy, errorMessage = status.error) }
    }

    /** Last captured image, kept so a failed or refined analysis can be retried without re-shooting. */
    private var lastImageBytes: ByteArray? = null
    private var analysisJob: kotlinx.coroutines.Job? = null
    private var analysisGeneration = 0L
    private var galleryJob: kotlinx.coroutines.Job? = null
    private var galleryGeneration = 0L
    private val draftMutex = kotlinx.coroutines.sync.Mutex()

    init {
        if (draftStore != null) viewModelScope.launch {
            try {
                val saved = draftStore.load()
                if (saved != null && mealRepository.getMeal(saved.draftMealId) == null) {
                    _uiState.value = saved.copy(draftLoading = false, resumePending = true,
                        isAnalyzing = false, isSaving = false, isSaved = false, errorMessage = null)
                } else {
                    draftStore.clear()
                    _uiState.update { it.copy(draftLoading = false) }
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.update { it.copy(draftLoading = false, draftMessage = "Could not recover the last draft. You can start a new meal.") } }
            _uiState.collect { snapshot ->
                if (!snapshot.resumePending && !snapshot.draftLoading) {
                    draftMutex.lock()
                    try {
                        if (snapshot == _uiState.value) {
                            if (snapshot.isSaved || (snapshot.items.isEmpty() && snapshot.localImageUri == null)) draftStore.clear()
                            else draftStore.save(snapshot)
                        }
                    } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                    catch (_: Exception) { _uiState.update { it.copy(draftMessage = "Draft recovery is unavailable. Keep this screen open until you save.") } }
                    finally { draftMutex.unlock() }
                }
            }
        }
    }

    fun resumeDraft() {
        val generation = ++galleryGeneration
        _uiState.update { it.copy(resumePending = false) }
        val path = _uiState.value.localImageUri ?: return
        viewModelScope.launch {
            val bytes = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching { File(path).takeIf { it.length() <= 25 * 1024 * 1024 }?.readBytes() }.getOrNull()
            }
            if (generation != galleryGeneration) return@launch
            lastImageBytes = bytes
            if (bytes == null) _uiState.update { it.copy(draftMessage = "The draft photo is unavailable. Your estimated items can still be saved, or choose another photo.") }
            if (_uiState.value.items.isEmpty() && lastImageBytes != null) runAnalysis()
        }
    }

    fun toggleAddedOil() {
        _uiState.update { it.copy(hasAddedOil = !it.hasAddedOil) }
    }

    fun setCookingFat(fat: CookingFat, grams: Float) {
        if (!grams.isFinite() || grams !in 1f..100f) return
        _uiState.update { it.copy(cookingFat = fat, cookingFatGrams = grams, hasAddedOil = true) }
    }

    fun scaleMeal(multiplier: Float) {
        if (_uiState.value.isAnalyzing || _uiState.value.isSaving) return
        _uiState.update { it.copy(items = it.items.map { item -> item.scaledPortion(multiplier) },
            cookingFatGrams = if (it.hasAddedOil) (it.cookingFatGrams * multiplier).coerceIn(1f, 100f) else it.cookingFatGrams) }
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
        if (_uiState.value.isSaving || _uiState.value.draftLoading || _uiState.value.resumePending) return
        galleryGeneration++
        galleryJob?.cancel()
        acceptImage(imageBytes, localImageUri)
    }

    private fun acceptImage(imageBytes: ByteArray, localImageUri: String?) {
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
        if (_uiState.value.isSaving || _uiState.value.resumePending || _uiState.value.draftLoading) return
        if (lastImageBytes != null) runAnalysis()
    }

    private fun runAnalysis() {
        val bytes = lastImageBytes ?: return
        analysisJob?.cancel()
        val generation = ++analysisGeneration
        val note = _uiState.value.userNote.ifBlank { null }
        _uiState.update { it.copy(isAnalyzing = true, errorMessage = null, canRetry = false) }

        analysisJob = viewModelScope.launch {
            val result = analysisService.analyzeMealImage(
                imageBytes = bytes,
                userNote = note
            )

            if (generation != analysisGeneration) return@launch
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
        if (_uiState.value.isSaving || _uiState.value.draftLoading || _uiState.value.resumePending) return
        galleryJob?.cancel()
        analysisJob?.cancel()
        analysisGeneration++
        val generation = ++galleryGeneration
        galleryJob = viewModelScope.launch {
            try {
                _uiState.update { it.copy(isAnalyzing = true, errorMessage = null) }

                // Read EXIF orientation to prevent sideways/upside-down photos
                val rotationDegrees = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val exif = androidx.exifinterface.media.ExifInterface(stream)
                        when (exif.getAttributeInt(
                            androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                            androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
                        )) {
                            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90
                            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
                            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
                            else -> 0
                        }
                    } ?: 0
                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (_: Exception) { 0 } }

                val bytes = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        var count = stream.read(buffer)
                        while (count != -1) {
                            require(output.size() + count <= 25 * 1024 * 1024) { "Choose a photo smaller than 25 MB." }
                            output.write(buffer, 0, count)
                            count = stream.read(buffer)
                        }
                        val bytes = output.toByteArray()
                        require(bytes.size <= 25 * 1024 * 1024) { "Choose a photo smaller than 25 MB." }
                        bytes
                    }
                }
                    ?: throw IllegalStateException("Could not read image stream")

                if (generation != galleryGeneration) return@launch
                var processed: com.kalotracker.app.core.util.ProcessedImage? = null
                // Finish file creation even on cancellation, then clean any obsolete photo.
                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                    processed = ImageUtils.processBytes(context, bytes, rotationDegrees)
                }
                val image = processed ?: return@launch
                if (generation != galleryGeneration || !kotlinx.coroutines.currentCoroutineContext().isActive) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable + kotlinx.coroutines.Dispatchers.IO) { File(image.localUri).delete() }
                    return@launch
                }
                acceptImage(image.compressedBytes, image.localUri)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
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

    fun correctNutrition(itemId: String, kcal: Int, p: Float, c: Float, f: Float) {
        updateItem(itemId) { it.correctedNutrition(kcal, p, c, f) }
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
        if (currentState.items.isEmpty() || currentState.isSaving || currentState.isAnalyzing || currentState.isSaved || currentState.resumePending || currentState.draftLoading) return

        saveOperation.launch(viewModelScope, onSuccess) {
            // Persist the stable ID before the DB write so recovery cannot duplicate a committed meal.
            if (draftStore != null) {
                draftMutex.lock()
                try { draftStore.save(currentState) }
                catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (_: Exception) { /* A draft-storage failure must not prevent logging a meal. */ }
                finally { draftMutex.unlock() }
            }
            val mealId = currentState.draftMealId
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
                    id = item.id,
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
                        name = "Extra ${currentState.cookingFat.label.lowercase()}",
                        portionGrams = currentState.cookingFatGrams,
                        calories = kotlin.math.round(currentState.cookingFatGrams * currentState.cookingFat.kcalPerGram).toInt(),
                        protein = 0f,
                        carbs = 0f,
                        fat = currentState.cookingFatGrams * currentState.cookingFat.fatPerGram,
                        confidence = 1f
                    )
                )
            } else emptyList()

            mealRepository.saveMeal(mealEntity, foodEntities)
            // Photo now belongs to the saved meal; never delete it in onCleared.
            _uiState.update {
                it.copy(isSaving = false, isSaved = true, items = emptyList(), mealTitle = "")
            }
        }
    }

    fun resetScan() {
        if (_uiState.value.isSaving) return
        analysisGeneration++
        galleryGeneration++
        galleryJob?.cancel()
        analysisJob?.cancel()
        discardUnsavedPhoto()
        lastImageBytes = null
        _uiState.value = MealScanUiState(timestamp = initialTimestamp)
    }

    fun setError(message: String) {
        _uiState.update { it.copy(isAnalyzing = false, isSaving = false, errorMessage = message) }
    }

    private fun discardUnsavedPhoto() {
        val state = _uiState.value
        if (!state.isSaved) state.localImageUri?.let { runCatching { File(it).delete() } }
    }

    override fun onCleared() {
        if (draftStore == null) discardUnsavedPhoto()
        super.onCleared()
    }
}

class MealViewModelFactory(
    private val mealRepository: MealRepository,
    private val analysisService: MealAnalysisService,
    private val initialTimestamp: Long = System.currentTimeMillis(),
    private val draftStore: MealDraftStore? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MealViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MealViewModel(mealRepository, analysisService, initialTimestamp, draftStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
