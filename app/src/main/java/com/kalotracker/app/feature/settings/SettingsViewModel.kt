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
import com.kalotracker.app.core.ai.ActivityLevel
import com.kalotracker.app.core.ai.BodyProfile
import com.kalotracker.app.core.ai.GoalType
import com.kalotracker.app.core.ai.Sex
import com.kalotracker.app.core.ai.SuggestedTargets
import com.kalotracker.app.core.ai.TargetCalculator
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
    val backupKeyInput: String = "",
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
    val restorePreview: String? = null,
    val body: BodyInputs = BodyInputs(),
    val bodyError: String? = null,
    val suggestion: SuggestedTargets? = null,
    val targetsMessage: String? = null,
    /** Share of calories from each macro, shown under the targets. */
    val splitText: String? = null
)

/** Text the user is typing for the "About you" form. */
data class BodyInputs(
    val sex: Sex? = null,
    val age: String = "",
    val heightCm: String = "",
    val weightKg: String = "",
    val activity: ActivityLevel? = null
) {
    companion object {
        fun from(b: BodyProfile?) = if (b == null) BodyInputs() else BodyInputs(b.sex, b.age.toString(),
            trimNumber(b.heightCm), trimNumber(b.weightKg), b.activity)
        private fun trimNumber(v: Float) = if (v % 1f == 0f) v.toInt().toString() else v.toString()
    }
}

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
            backupKeyInput = appSettings.ai.value.backupApiKey,
            modelInput = appSettings.ai.value.model,
            aiConfigured = appSettings.ai.value.isConfigured,
            reminder = appSettings.reminder.value,
            body = BodyInputs.from(userProfileRepository.body.value)
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var pendingRestore: com.kalotracker.app.core.data.backup.ArchiveContents? = null
    private var pendingJson: com.kalotracker.app.core.data.backup.BackupFile? = null

    /** Last usable macro split, so typing a calorie target digit by digit never loses it. */
    private var split: MacroSplit = MacroSplit(0.3f, 0.45f, 0.25f)
    /** Targets last copied into the form, to tell a goal-only change from a target change. */
    private var shownTargets: com.kalotracker.app.core.data.repository.UserProfile? = null

    init {
        viewModelScope.launch { appSettings.backup.collect { s -> _uiState.update { it.copy(backupSchedule = s) } } }
        viewModelScope.launch {
            userProfileRepository.profile.collect { profile ->
                // A goal-only change must not wipe target numbers the user is still typing.
                val previous = shownTargets
                shownTargets = profile
                if (previous != null && previous.copy(goal = profile.goal) == profile) {
                    _uiState.update { it.copy(profile = profile, goal = profile.goal) }
                    return@collect
                }
                _uiState.update {
                    it.copy(
                        profile = profile,
                        calorieInput = profile.targetCalories.toString(),
                        proteinInput = profile.targetProtein.toString(),
                        carbsInput = profile.targetCarbs.toString(),
                        fatInput = profile.targetFat.toString(),
                        stepsInput = profile.targetSteps.toString(),
                        waterInput = profile.targetWaterMl.toString(),
                        goal = profile.goal,
                        splitText = MacroSplit.of(profile.targetProtein, profile.targetCarbs, profile.targetFat)?.percentText()
                    )
                }
                split = MacroSplit.of(profile.targetProtein, profile.targetCarbs, profile.targetFat) ?: split
            }
        }
    }

    /** A new calorie target rescales protein, carbs and fat, keeping the current split. */
    fun updateCalorieInput(value: String) = _uiState.update { s ->
        val calories = value.toIntOrNull()?.takeIf { it in 1..100_000 } ?: return@update s.copy(calorieInput = value)
        val (p, c, f) = split.gramsFor(calories)
        s.copy(calorieInput = value, proteinInput = p.toString(), carbsInput = c.toString(), fatInput = f.toString(), goalError = null)
    }

    fun updateProteinInput(value: String) = updateMacros { it.copy(proteinInput = value) }
    fun updateCarbsInput(value: String) = updateMacros { it.copy(carbsInput = value) }
    fun updateFatInput(value: String) = updateMacros { it.copy(fatInput = value) }

    /** A macro change recalculates calories (4/4/9 kcal per gram) and the split. */
    private fun updateMacros(edit: (SettingsUiState) -> SettingsUiState) = _uiState.update { old ->
        val s = edit(old)
        val p = s.proteinInput.toIntOrNull()
        val c = s.carbsInput.toIntOrNull()
        val f = s.fatInput.toIntOrNull()
        if (p == null || c == null || f == null || p < 0 || c < 0 || f < 0) return@update s
        MacroSplit.of(p, c, f)?.let { split = it }
        s.copy(calorieInput = MacroSplit.caloriesOf(p, c, f).toString(),
            splitText = MacroSplit.of(p, c, f)?.percentText(), goalError = null)
    }
    fun updateStepsInput(value: String) = _uiState.update { it.copy(stepsInput = value) }
    fun updateWaterInput(value: String) = _uiState.update { it.copy(waterInput = value) }
    fun updateApiKeyInput(value: String) = _uiState.update { it.copy(apiKeyInput = value, aiTestMessage = null) }
    fun updateBackupKeyInput(value: String) = _uiState.update { it.copy(backupKeyInput = value, aiTestMessage = null) }
    fun updateModelInput(value: String) = _uiState.update { it.copy(modelInput = value, aiTestMessage = null) }

    fun setGoal(goal: GoalType) {
        userProfileRepository.setGoal(goal)
        if (_uiState.value.suggestion != null) calculateTargets()
    }

    fun updateBody(transform: (BodyInputs) -> BodyInputs) =
        _uiState.update { it.copy(body = transform(it.body), bodyError = null, suggestion = null, targetsMessage = null) }

    /** Saves the body details and shows suggested targets; nothing changes until the user applies them. */
    fun calculateTargets() {
        val b = _uiState.value.body
        val age = b.age.trim().toIntOrNull()
        val height = b.heightCm.trim().replace(',', '.').toFloatOrNull()
        val weight = b.weightKg.trim().replace(',', '.').toFloatOrNull()
        val error = when {
            b.sex == null -> "Choose female or male. The formula needs it."
            age == null -> "Enter your age in years."
            height == null -> "Enter your height in cm."
            weight == null -> "Enter your weight in kg."
            b.activity == null -> "Choose how active you are."
            else -> BodyProfile(b.sex, age, height, weight, b.activity).automaticTargetProblem()
        }
        if (error != null) {
            _uiState.update { it.copy(bodyError = error, suggestion = null) }
            return
        }
        val body = BodyProfile(b.sex!!, age!!, height!!, weight!!, b.activity!!)
        userProfileRepository.saveBody(body)
        val goal = userProfileRepository.profile.value.goal
        _uiState.update { it.copy(bodyError = null, targetsMessage = null, suggestion = TargetCalculator.suggest(body, goal)) }
    }

    fun applySuggestion() {
        val t = _uiState.value.suggestion ?: return
        userProfileRepository.updateTargets(t.calories, t.protein, t.carbs, t.fat,
            userProfileRepository.profile.value.targetSteps, t.waterMl)
        _uiState.update { it.copy(suggestion = null, goalError = null,
            targetsMessage = "Targets updated. Recheck after 2–3 weeks of weigh-ins.") }
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

    /** Saves the keys/model, then checks each key on its own so a wrong key or model is caught right away. */
    fun saveAndTestAi() {
        val s = _uiState.value
        val candidate = AiSettings(s.apiKeyInput.trim(), s.modelInput.trim().ifBlank { AppSettings.DEFAULT_MODEL }, s.backupKeyInput.trim())
        appSettings.saveAi(candidate.apiKey, candidate.model, candidate.backupApiKey)
        _uiState.update { it.copy(isTestingAi = true, aiTestMessage = null, aiConfigured = candidate.isConfigured) }
        viewModelScope.launch {
            val checks = listOf("Main key" to candidate.apiKey, "Backup key" to candidate.backupApiKey)
                .filter { it.second.isNotBlank() }
                .map { (label, key) -> label to analysisService.testConnection(AiSettings(key, candidate.model)) }
            val allOk = checks.all { it.second.isSuccess }
            _uiState.update {
                it.copy(
                    isTestingAi = false,
                    aiTestOk = allOk,
                    aiTestMessage = if (allOk) {
                        if (checks.size > 1) "Both keys work. Photo logging is ready." else "Connected. Photo logging is ready."
                    } else checks.joinToString("\n") { (label, result) ->
                        "$label: " + if (result.isSuccess) "works" else (result.exceptionOrNull()?.localizedMessage ?: "test failed")
                    }
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
    fun exportArchive(uri: Uri, resolver: ContentResolver) = runData({ "Archive saved, including photos and library. API keys excluded." }) {
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
            _uiState.update { it.copy(restorePreview = "${f.meals.size} meals, ${f.workouts.size} workouts, ${f.water.size} water, ${f.weights.size} weights; ${f.savedFoods.size} foods/recipes, ${f.routines.size} routines, ${f.dayStatus.size} day statuses, ${f.goalHistory.size} goal dates, ${pendingRestore?.photos?.size ?: 0} photos.\nMerge by id; matching records are updated, other local records are kept. Goals and non-secret settings may be replaced. Your API keys and backup folder stay.") }
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
        appSettings.saveAi("", _uiState.value.modelInput, "")
        _uiState.update { it.copy(apiKeyInput = "", backupKeyInput = "", aiConfigured = false, aiTestMessage = null) }
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
