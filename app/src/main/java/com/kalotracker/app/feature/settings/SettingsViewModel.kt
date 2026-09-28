package com.kalotracker.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kalotracker.app.core.data.repository.AuthRepository
import com.kalotracker.app.core.data.repository.MacroPreset
import com.kalotracker.app.core.data.repository.MealRepository
import com.kalotracker.app.core.data.repository.UserProfile
import com.kalotracker.app.core.data.repository.UserProfileRepository
import com.kalotracker.app.core.data.repository.WorkoutRepository
import com.kalotracker.app.core.network.SupabaseModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val profile: UserProfile = UserProfile(),
    val calorieInput: String = "2200",
    val proteinInput: String = "160",
    val carbsInput: String = "220",
    val fatInput: String = "70",
    val stepsInput: String = "10000",
    val waterInput: String = "2500",
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val isSupabaseConfigured: Boolean = false,
    val userEmail: String? = null,
    val isGuestMode: Boolean = true
)

class SettingsViewModel(
    private val userProfileRepository: UserProfileRepository,
    private val mealRepository: MealRepository,
    private val workoutRepository: WorkoutRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userProfileRepository.profile.collect { profile ->
                _uiState.update {
                    it.copy(
                        profile = profile,
                        calorieInput = profile.targetCalories.toString(),
                        proteinInput = profile.targetProtein.toString(),
                        carbsInput = profile.targetCarbs.toString(),
                        fatInput = profile.targetFat.toString(),
                        stepsInput = profile.targetSteps.toString(),
                        waterInput = profile.targetWaterMl.toString(),
                        isSupabaseConfigured = SupabaseModule.isConfigured
                    )
                }
            }
        }

        viewModelScope.launch {
            authRepository.authState.collect { auth ->
                _uiState.update {
                    it.copy(
                        userEmail = auth.email,
                        isGuestMode = auth.isGuestMode
                    )
                }
            }
        }
    }

    fun updateCalorieInput(value: String) {
        _uiState.update { it.copy(calorieInput = value) }
    }

    fun updateProteinInput(value: String) {
        _uiState.update { it.copy(proteinInput = value) }
    }

    fun updateCarbsInput(value: String) {
        _uiState.update { it.copy(carbsInput = value) }
    }

    fun updateFatInput(value: String) {
        _uiState.update { it.copy(fatInput = value) }
    }

    fun updateStepsInput(value: String) {
        _uiState.update { it.copy(stepsInput = value) }
    }

    fun updateWaterInput(value: String) {
        _uiState.update { it.copy(waterInput = value) }
    }

    fun applyPreset(preset: MacroPreset) {
        val calories = _uiState.value.calorieInput.toIntOrNull() ?: 2200
        userProfileRepository.applyPreset(preset, calories)
    }

    fun saveGoals() {
        val calories = _uiState.value.calorieInput.toIntOrNull() ?: 2200
        val protein = _uiState.value.proteinInput.toIntOrNull() ?: 160
        val carbs = _uiState.value.carbsInput.toIntOrNull() ?: 220
        val fat = _uiState.value.fatInput.toIntOrNull() ?: 70
        val steps = _uiState.value.stepsInput.toLongOrNull() ?: 10000L
        val water = _uiState.value.waterInput.toIntOrNull() ?: 2500

        userProfileRepository.updateTargets(
            calories = calories,
            protein = protein,
            carbs = carbs,
            fat = fat,
            steps = steps,
            waterMl = water
        )
    }

    fun syncCloudNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncMessage = null) }
            val mealResult = mealRepository.syncPendingMeals()
            val workoutResult = workoutRepository.syncPendingWorkouts()
            val totalSynced = (mealResult.getOrElse { 0 }) + (workoutResult.getOrElse { 0 })
            val hasError = mealResult.isFailure || workoutResult.isFailure
            _uiState.update {
                it.copy(
                    isSyncing = false,
                    syncMessage = if (hasError)
                        "Sync partially failed — check connection"
                    else if (totalSynced > 0) "Successfully synced $totalSynced items"
                    else "All items up to date"
                )
            }
        }
    }

    fun signOut(onSignedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            onSignedOut()
        }
    }
}

class SettingsViewModelFactory(
    private val userProfileRepository: UserProfileRepository,
    private val mealRepository: MealRepository,
    private val workoutRepository: WorkoutRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(userProfileRepository, mealRepository, workoutRepository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
