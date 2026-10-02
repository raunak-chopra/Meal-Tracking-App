package com.kalotracker.app.feature.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.kalotracker.app.core.util.validateGoalInputs
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
    val dataOk: Boolean = false,
    val goalError: String? = null,
    val backupSchedule: com.kalotracker.app.core.settings.BackupSchedule = com.kalotracker.app.core.settings.BackupSchedule(),
    val restorePreview: String? = null
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

    private var pendingRestore: com.kalotracker.app.core.data.backup.ArchiveContents? = null
    private var pendingJson: com.kalotracker.app.core.data.backup.BackupFile? = null
    init {
        viewModelScope.launch { appSettings.backup.collect { s -> _uiState.update { it.copy(backupSchedule = s) } } }
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
        val calories = _uiState.value.calorieInput.toIntOrNull()
        if (calories == null || calories !in 1..100_000) {
            _uiState.update { it.copy(goalError = "Enter valid calories before applying a preset.") }
            return
        }
        userProfileRepository.applyPreset(preset, calories)
    }

    fun saveGoals(): Boolean {
        val s = _uiState.value
        val error = validateGoalInputs(s.calorieInput, s.proteinInput, s.carbsInput,
            s.fatInput, s.stepsInput, s.waterInput)
        _uiState.update { it.copy(goalError = error) }
        if (error != null) return false
        userProfileRepository.updateTargets(
            calories = s.calorieInput.toInt(),
            protein = s.proteinInput.toInt(),
            carbs = s.carbsInput.toInt(),
            fat = s.fatInput.toInt(),
            steps = s.stepsInput.toLong(),
            waterMl = s.waterInput.toInt()
        )
        return true
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

    fun chooseBackupFolder(uri: Uri, resolver: ContentResolver, context: android.content.Context) {
        try {
            resolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            appSettings.saveBackup(uri.toString(), true)
            com.kalotracker.app.core.data.backup.BackupScheduler.schedule(context, true)
        } catch (e: Exception) { _uiState.update { it.copy(dataMessage = "Could not keep folder access. Choose another folder.", dataOk = false) } }
    }
    fun enableBackup(enabled: Boolean, context: android.content.Context) {
        if (enabled && appSettings.backup.value.folder.isBlank()) return
        appSettings.saveBackup(appSettings.backup.value.folder, enabled)
        com.kalotracker.app.core.data.backup.BackupScheduler.schedule(context, enabled)
    }
    fun exportArchive(uri: Uri, resolver: ContentResolver) = runData({ "Archive saved, including photos and library. API key excluded." }) {
        val output = withContext(Dispatchers.IO) { resolver.openOutputStream(uri, "wt") } ?: throw java.io.IOException("Couldn't write archive.")
        backupManager.exportArchive(output)
    }
    fun previewImport(uri: Uri, resolver: ContentResolver) = runData({ "Review the restore preview before importing." }) {
        cancelRestore()
        withContext(Dispatchers.IO) {
            val stream = java.io.BufferedInputStream(resolver.openInputStream(uri) ?: throw java.io.IOException("Couldn't open backup."))
            stream.use {
                it.mark(4); val first = it.read(); val second = it.read(); it.reset()
                if (first == 80 && second == 75) {
                    pendingRestore = com.kalotracker.app.core.data.backup.ArchiveCodec.read(it); pendingJson = null
                } else {
                    val out = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192)
                    while (out.size() <= 30 * 1024 * 1024) { val n = it.read(buffer); if (n < 0) break; out.write(buffer, 0, n) }
                    val text = out.toByteArray()
                    if (text.size > 30 * 1024 * 1024) throw java.io.IOException("Backup is too large.")
                    pendingJson = BackupCodec.decode(text.toString(Charsets.UTF_8)); pendingRestore = null
                }
            }
            val f = pendingRestore?.backup ?: pendingJson!!
            _uiState.update { it.copy(restorePreview = "${f.meals.size} meals, ${f.workouts.size} workouts, ${f.water.size} water, ${f.weights.size} weights; ${f.savedFoods.size} foods/recipes, ${f.routines.size} routines, ${f.dayStatus.size} day statuses, ${f.goalHistory.size} goal dates, ${pendingRestore?.photos?.size ?: 0} photos.\nMerge by id; matching records are updated, other local records are kept. Goals and non-secret settings may be replaced. Your API key and backup folder stay.") }
        }
    }
    fun cancelRestore() { pendingJson = null; pendingRestore = null; _uiState.update { it.copy(restorePreview = null) } }
    fun confirmRestore() = runData({ "Restore completed." }) {
        val archive = pendingRestore
        val json = pendingJson
        if (archive != null) backupManager.restoreArchive(archive) else if (json != null) backupManager.import(json) else throw java.io.IOException("Choose a backup first.")
        scheduleReminder(appSettings.reminder.value)
        cancelRestore()
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
