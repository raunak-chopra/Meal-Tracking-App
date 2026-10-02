package com.kalotracker.app.feature.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.kalotracker.app.core.data.backup.BackupCodec
import com.kalotracker.app.core.data.backup.BackupManager
import com.kalotracker.app.core.data.backup.ImportSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kalotracker.app.core.ai.GoalType
import com.kalotracker.app.core.data.repository.MacroPreset
import com.kalotracker.app.core.data.repository.UserProfile
import com.kalotracker.app.core.data.repository.UserProfileRepository
import com.kalotracker.app.core.network.MealAnalysisService
import com.kalotracker.app.core.settings.AiSettings
import com.kalotracker.app.core.settings.AppSettings
import com.kalotracker.app.core.settings.ReminderSettings
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
    val apiKeyInput: String = "",
    val modelInput: String = AppSettings.DEFAULT_MODEL,
    val aiConfigured: Boolean = false,
    val isTestingAi: Boolean = false,
    val aiTestMessage: String? = null,
    val aiTestOk: Boolean = false,
    val goal: GoalType = GoalType.MAINTAIN,
    val reminder: ReminderSettings = ReminderSettings(),
    val dataBusy: Boolean = false,
    val dataMessage: String? = null,
    val dataOk: Boolean = false
)

class SettingsViewModel(
    private val userProfileRepository: UserProfileRepository,
    private val appSettings: AppSettings,
    private val analysisService: MealAnalysisService,
    private val backupManager: BackupManager,
    private val scheduleReminder: (ReminderSettings) -> Unit
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            apiKeyInput = appSettings.ai.value.apiKey,
            modelInput = appSettings.ai.value.model,
            aiConfigured = appSettings.ai.value.isConfigured,
            reminder = appSettings.reminder.value
        )
    )
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
                        goal = profile.goal
                    )
                }
            }
        }
    }

    fun updateCalorieInput(value: String) = _uiState.update { it.copy(calorieInput = value) }
    fun updateProteinInput(value: String) = _uiState.update { it.copy(proteinInput = value) }
    fun updateCarbsInput(value: String) = _uiState.update { it.copy(carbsInput = value) }
    fun updateFatInput(value: String) = _uiState.update { it.copy(fatInput = value) }
    fun updateStepsInput(value: String) = _uiState.update { it.copy(stepsInput = value) }
    fun updateWaterInput(value: String) = _uiState.update { it.copy(waterInput = value) }
    fun updateApiKeyInput(value: String) = _uiState.update { it.copy(apiKeyInput = value, aiTestMessage = null) }
    fun updateModelInput(value: String) = _uiState.update { it.copy(modelInput = value, aiTestMessage = null) }

    fun setGoal(goal: GoalType) {
        userProfileRepository.setGoal(goal)
    }

    fun setReminderEnabled(enabled: Boolean) = updateReminder(_uiState.value.reminder.copy(enabled = enabled))

    fun setReminderTime(hour: Int, minute: Int) =
        updateReminder(_uiState.value.reminder.copy(hour = hour, minute = minute))

    private fun updateReminder(reminder: ReminderSettings) {
        appSettings.saveReminder(reminder)
        scheduleReminder(reminder)
        _uiState.update { it.copy(reminder = reminder) }
    }

    fun applyPreset(preset: MacroPreset) {
        val calories = _uiState.value.calorieInput.toIntOrNull() ?: 2200
        userProfileRepository.applyPreset(preset, calories)
    }

    fun saveGoals() {
        val s = _uiState.value
        userProfileRepository.updateTargets(
            calories = s.calorieInput.toIntOrNull() ?: 2200,
            protein = s.proteinInput.toIntOrNull() ?: 160,
            carbs = s.carbsInput.toIntOrNull() ?: 220,
            fat = s.fatInput.toIntOrNull() ?: 70,
            steps = s.stepsInput.toLongOrNull() ?: 10000L,
            waterMl = s.waterInput.toIntOrNull() ?: 2500
        )
    }

    /** Saves the key/model, then makes a tiny request so a wrong key or model is caught right away. */
    fun saveAndTestAi() {
        val s = _uiState.value
        val candidate = AiSettings(s.apiKeyInput.trim(), s.modelInput.trim().ifBlank { AppSettings.DEFAULT_MODEL })
        appSettings.saveAi(candidate.apiKey, candidate.model)
        _uiState.update { it.copy(isTestingAi = true, aiTestMessage = null, aiConfigured = candidate.isConfigured) }
        viewModelScope.launch {
            val result = analysisService.testConnection(candidate)
            _uiState.update {
                it.copy(
                    isTestingAi = false,
                    aiTestOk = result.isSuccess,
                    aiTestMessage = if (result.isSuccess) "Connected. Photo scanning is ready."
                    else result.exceptionOrNull()?.localizedMessage ?: "Test failed"
                )
            }
        }
    }

    private fun runData(successMessage: (Any?) -> String, block: suspend () -> Any?) {
        if (_uiState.value.dataBusy) return
        _uiState.update { it.copy(dataBusy = true, dataMessage = null) }
        viewModelScope.launch {
            val result = runCatching { block() }
            _uiState.update {
                it.copy(
                    dataBusy = false,
                    dataOk = result.isSuccess,
                    dataMessage = result.fold(
                        onSuccess = { value -> successMessage(value) },
                        onFailure = { e -> e.localizedMessage ?: "Something went wrong." }
                    )
                )
            }
        }
    }

    fun exportBackup(uri: Uri, resolver: ContentResolver) = runData(
        successMessage = { "Backup saved (${it as Int} entries). Photos are not included." }
    ) {
        val backup = backupManager.buildBackup()
        writeText(resolver, uri, BackupCodec.encode(backup))
        backup.meals.size + backup.workouts.size + backup.water.size + backup.weights.size
    }

    fun exportMealsCsv(uri: Uri, resolver: ContentResolver) = runData(
        successMessage = { "Exported ${it as Int} meals to CSV." }
    ) {
        val backup = backupManager.buildBackup()
        writeText(resolver, uri, BackupCodec.mealsToCsv(backup.meals))
        backup.meals.size
    }

    fun importBackup(uri: Uri, resolver: ContentResolver) = runData(
        successMessage = {
            val r = it as ImportSummary
            "Imported ${r.meals} meals, ${r.workouts} workouts, ${r.water} water and ${r.weights} weight entries."
        }
    ) {
        val text = withContext(Dispatchers.IO) {
            resolver.openInputStream(uri)?.bufferedReader()?.use { reader -> reader.readText() }
                ?: throw java.io.IOException("Couldn't open that file.")
        }
        backupManager.import(BackupCodec.decode(text))
    }

    fun deleteAllData() = runData(successMessage = { "All logs and photos were deleted." }) {
        backupManager.deleteAllData()
    }

    private suspend fun writeText(resolver: ContentResolver, uri: Uri, text: String) = withContext(Dispatchers.IO) {
        resolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(text) }
            ?: throw java.io.IOException("Couldn't write to that location.")
    }

    fun clearAiKey() {
        appSettings.saveAi("", _uiState.value.modelInput)
        _uiState.update { it.copy(apiKeyInput = "", aiConfigured = false, aiTestMessage = null) }
    }
}

class SettingsViewModelFactory(
    private val userProfileRepository: UserProfileRepository,
    private val appSettings: AppSettings,
    private val analysisService: MealAnalysisService,
    private val backupManager: BackupManager,
    private val scheduleReminder: (ReminderSettings) -> Unit
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(userProfileRepository, appSettings, analysisService, backupManager, scheduleReminder) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
