package com.kalotracker.app.feature.barcode

import androidx.lifecycle.ViewModel
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
    val isLookingUp: Boolean = false,
    val scannedBarcode: String? = null,
    val product: ScannedFoodProduct? = null,
    val portionGrams: Float = 100f,
    val isTorchOn: Boolean = false,
    val errorMessage: String? = null,
    val isSaved: Boolean = false,
    val isManualInputVisible: Boolean = false,
    val manualBarcodeText: String = ""
) {
    val currentCalories: Int get() = product?.calculateCalories(portionGrams) ?: 0
    val currentProtein: Float get() = product?.calculateProtein(portionGrams) ?: 0f
    val currentCarbs: Float get() = product?.calculateCarbs(portionGrams) ?: 0f
    val currentFat: Float get() = product?.calculateFat(portionGrams) ?: 0f
}

class BarcodeScannerViewModel(
    private val mealRepository: MealRepository,
    private val foodFactsService: OpenFoodFactsService = OpenFoodFactsService()
) : ViewModel() {

    private val _uiState = MutableStateFlow(BarcodeUiState())
    val uiState: StateFlow<BarcodeUiState> = _uiState.asStateFlow()

    fun onBarcodeScanned(barcode: String) {
        if (_uiState.value.isLookingUp || _uiState.value.product != null) return

        _uiState.update {
            it.copy(
                isLookingUp = true,
                scannedBarcode = barcode,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            val result = foodFactsService.getProductByBarcode(barcode)
            result.onSuccess { product ->
                _uiState.update {
                    it.copy(
                        isLookingUp = false,
                        product = product,
                        portionGrams = product.servingSizeGrams
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLookingUp = false,
                        errorMessage = "Product not found: ${err.localizedMessage}"
                    )
                }
            }
        }
    }

    fun updatePortionGrams(grams: Float) {
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
        _uiState.update {
            it.copy(
                isLookingUp = false,
                scannedBarcode = null,
                product = null,
                errorMessage = null,
                isSaved = false
            )
        }
    }

    fun logAsMeal(onSuccess: () -> Unit) {
        val s = _uiState.value
        val prod = s.product ?: return

        viewModelScope.launch {
            val mealId = UUID.randomUUID().toString()
            val mealTitle = prod.name.ifBlank { "Scanned Food" }

            val mealEntity = MealEntity(
                id = mealId,
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
                confidence = 1.0f
            )

            mealRepository.saveMeal(mealEntity, listOf(foodEntity))
            _uiState.update { it.copy(isSaved = true) }
            onSuccess()
        }
    }
}

class BarcodeScannerViewModelFactory(
    private val mealRepository: MealRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BarcodeScannerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BarcodeScannerViewModel(mealRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
