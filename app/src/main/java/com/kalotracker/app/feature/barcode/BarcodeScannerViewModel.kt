package com.kalotracker.app.feature.barcode

import androidx.lifecycle.ViewModel
import com.kalotracker.app.core.util.SaveOperation
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kalotracker.app.core.data.repository.MealRepository
import com.kalotracker.app.core.database.entity.FoodItemEntity
import com.kalotracker.app.core.database.entity.MealEntity
import com.kalotracker.app.core.network.OpenFoodFactsService
import com.kalotracker.app.core.network.ScannedFoodProduct
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class BarcodeUiState(
    val timestamp: Long = System.currentTimeMillis(),
    val isLookingUp: Boolean = false,
    val scannedBarcode: String? = null,
    val product: ScannedFoodProduct? = null,
    val portionGrams: Float = 100f,
    val isTorchOn: Boolean = false,
    val errorMessage: String? = null,
    val isSaved: Boolean = false,
    val isSaving: Boolean = false,
    val isManualInputVisible: Boolean = false,
    val manualBarcodeText: String = "",
    val labelConfirmed: Boolean = false,
    val reviewLabelUrl: String? = null
) {
    val canLog: Boolean get() = product != null && !isSaving && !isSaved && !isLookingUp &&
        (product.requiresLabelConfirmation.not() || labelConfirmed)
    val currentCalories: Int get() = product?.calculateCalories(portionGrams) ?: 0
    val currentProtein: Float get() = product?.calculateProtein(portionGrams) ?: 0f
    val currentCarbs: Float get() = product?.calculateCarbs(portionGrams) ?: 0f
    val currentFat: Float get() = product?.calculateFat(portionGrams) ?: 0f
}

class BarcodeScannerViewModel(
    private val mealRepository: MealRepository,
    private val foodFactsService: OpenFoodFactsService = OpenFoodFactsService(),
    initialTimestamp: Long = System.currentTimeMillis()
) : ViewModel() {

    private var lookupGeneration = 0L
    private var lookupJob: kotlinx.coroutines.Job? = null
    private val _uiState = MutableStateFlow(BarcodeUiState(timestamp = initialTimestamp))
    val uiState: StateFlow<BarcodeUiState> = _uiState.asStateFlow()
    val saveOperation = SaveOperation { status ->
        _uiState.update { it.copy(isSaving = status.busy, errorMessage = status.error) }
    }

    fun onBarcodeScanned(barcode: String) {
        if (_uiState.value.isLookingUp || _uiState.value.product != null) return

        _uiState.update {
            it.copy(
                isLookingUp = true,
                scannedBarcode = barcode,
                errorMessage = null, labelConfirmed = false, reviewLabelUrl = null
            )
        }

        val request = ++lookupGeneration
        lookupJob?.cancel()
        lookupJob = viewModelScope.launch {
            val result = foodFactsService.getProductByBarcode(barcode)
            result.onSuccess { product ->
                if (request != lookupGeneration) return@onSuccess
                _uiState.update {
                    it.copy(
                        isLookingUp = false,
                        product = product,
                        portionGrams = product.servingSizeGrams
                    )
                }
            }.onFailure { err ->
                if (request != lookupGeneration) return@onFailure
                _uiState.update {
                    it.copy(
                        isLookingUp = false,
                        errorMessage = err.localizedMessage ?: "Lookup failed.",
                        reviewLabelUrl = (err as? com.kalotracker.app.core.network.BarcodeLookupException.LabelReviewRequired)?.labelUrl
                    )
                }
            }
        }
    }

    fun confirmLabel(value: Boolean) { _uiState.update { it.copy(labelConfirmed = value) } }

    fun setTimestamp(value: Long) { _uiState.update { it.copy(timestamp = value) } }

    fun refreshProduct() {
        val product = _uiState.value.product ?: return
        if (product.fromCatalog) return
        val barcode = product.barcode
        if (_uiState.value.isLookingUp || _uiState.value.isSaving) return
        _uiState.update { it.copy(isLookingUp = true, errorMessage = null) }
        val request = ++lookupGeneration
        lookupJob?.cancel()
        lookupJob = viewModelScope.launch {
            val result = foodFactsService.getProductByBarcode(barcode, refresh = true)
            if (request != lookupGeneration) return@launch
            _uiState.update { it.copy(isLookingUp = false, product = result.getOrNull() ?: it.product,
                labelConfirmed = false,
                errorMessage = result.exceptionOrNull()?.localizedMessage) }
        }
    }
    fun updatePortionGrams(grams: Float) {
        if (!grams.isFinite()) return
        _uiState.update { it.copy(portionGrams = grams.coerceIn(5f, 2500f)) }
    }

    fun adjustPortion(multiplier: Float) {
        _uiState.update {
            val updated = (it.portionGrams * multiplier).coerceIn(5f, 2500f)
            it.copy(portionGrams = updated)
        }
    }

    fun toggleTorch() {
        _uiState.update { it.copy(isTorchOn = !it.isTorchOn) }
    }

    fun setManualInputVisible(visible: Boolean) {
        _uiState.update { it.copy(isManualInputVisible = visible) }
    }

    fun updateManualBarcode(text: String) {
        _uiState.update { it.copy(manualBarcodeText = text) }
    }

    fun submitManualBarcode() {
        val barcode = _uiState.value.manualBarcodeText.trim()
        if (barcode.isNotBlank()) {
            setManualInputVisible(false)
            onBarcodeScanned(barcode)
        }
    }

    fun resumeScanning() {
        if (_uiState.value.isSaving) return
        lookupGeneration++
        lookupJob?.cancel()
        _uiState.update {
            it.copy(
                isLookingUp = false,
                scannedBarcode = null,
                product = null,
                errorMessage = null,
                isSaved = false, labelConfirmed = false, reviewLabelUrl = null
            )
        }
    }

    fun logAsMeal(onSuccess: () -> Unit) {
        val s = _uiState.value
        val prod = s.product ?: return
        if (!s.canLog) return

        saveOperation.launch(viewModelScope, onSuccess) {
            val mealId = UUID.randomUUID().toString()
            val mealTitle = prod.name.ifBlank { "Scanned Food" }

            val mealEntity = MealEntity(
                id = mealId,
                timestamp = s.timestamp,
                title = mealTitle,
                totalCalories = s.currentCalories,
                totalProteinGrams = s.currentProtein,
                totalCarbsGrams = s.currentCarbs,
                totalFatGrams = s.currentFat,
                notes = if (!prod.brand.isNullOrBlank()) "Brand: ${prod.brand} • Barcode: ${prod.barcode}" else "Barcode: ${prod.barcode}"
            )

            val foodEntity = FoodItemEntity(
                mealId = mealId,
                name = mealTitle,
                portionGrams = s.portionGrams,
                calories = s.currentCalories,
                protein = s.currentProtein,
                carbs = s.currentCarbs,
                fat = s.currentFat,
                confidence = if (prod.fromCatalog) 0.7f else 1.0f
            )

            mealRepository.saveMeal(mealEntity, listOf(foodEntity))
            _uiState.update { it.copy(isSaved = true) }

        }
    }
}

class BarcodeScannerViewModelFactory(
    private val mealRepository: MealRepository,
    private val personalDao: com.kalotracker.app.core.database.dao.PersonalDao,
    private val initialTimestamp: Long = System.currentTimeMillis(),
    private val catalogLoader: (() -> com.kalotracker.app.core.network.IndianSnackCatalog)? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BarcodeScannerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BarcodeScannerViewModel(mealRepository, OpenFoodFactsService(personalDao, catalogLoader), initialTimestamp) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
