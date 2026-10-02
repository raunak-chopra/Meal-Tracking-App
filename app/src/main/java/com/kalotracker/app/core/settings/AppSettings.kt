package com.kalotracker.app.core.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Appearance { SYSTEM, LIGHT, DARK }

data class BackupSchedule(val folder: String = "", val enabled: Boolean = false, val lastSuccess: Long = 0, val error: String? = null)

data class AiSettings(
    val apiKey: String = "",
    val model: String = AppSettings.DEFAULT_MODEL
) {
    val isConfigured: Boolean get() = apiKey.isNotBlank()
}

data class ReminderSettings(
    val enabled: Boolean = false,
    val hour: Int = 20,
    val minute: Int = 0
)

/**
 * Device-local settings that are not nutrition goals: the user's own Gemini key/model
 * and reminder preferences. API keys and device-specific backup-folder grants are excluded from exports.
 */
class AppSettings(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("kalo_app_settings", Context.MODE_PRIVATE)

    private val _appearance = MutableStateFlow(runCatching { Appearance.valueOf(prefs.getString("appearance", "SYSTEM")!!) }.getOrDefault(Appearance.SYSTEM))
    val appearance = _appearance.asStateFlow()
    fun saveAppearance(value: Appearance) { prefs.edit().putString("appearance",value.name).apply(); _appearance.value = value }

    private val _ai = MutableStateFlow(
        AiSettings(
            apiKey = prefs.getString(KEY_API, "") ?: "",
            model = prefs.getString(KEY_MODEL, DEFAULT_MODEL)?.ifBlank { DEFAULT_MODEL } ?: DEFAULT_MODEL
        )
    )
    val ai: StateFlow<AiSettings> = _ai.asStateFlow()

    fun saveAi(apiKey: String, model: String) {
        val cleaned = AiSettings(apiKey.trim(), model.trim().ifBlank { DEFAULT_MODEL })
        prefs.edit().putString(KEY_API, cleaned.apiKey).putString(KEY_MODEL, cleaned.model).apply()
        _ai.value = cleaned
    }

    private val _reminder = MutableStateFlow(
        ReminderSettings(
            enabled = prefs.getBoolean(KEY_REMINDER_ON, false),
            hour = prefs.getInt(KEY_REMINDER_HOUR, 20),
            minute = prefs.getInt(KEY_REMINDER_MINUTE, 0)
        )
    )
    val reminder: StateFlow<ReminderSettings> = _reminder.asStateFlow()

    fun saveReminder(settings: ReminderSettings) {
        prefs.edit()
            .putBoolean(KEY_REMINDER_ON, settings.enabled)
            .putInt(KEY_REMINDER_HOUR, settings.hour)
            .putInt(KEY_REMINDER_MINUTE, settings.minute)
            .apply()
        _reminder.value = settings
    }

    private val _backup = MutableStateFlow(BackupSchedule(prefs.getString("backup_folder", "")!!,
        prefs.getBoolean("backup_enabled", false), prefs.getLong("backup_last", 0), prefs.getString("backup_error", null)))
    val backup = _backup.asStateFlow()
    private val backupListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key?.startsWith("backup_") == true) _backup.value = BackupSchedule(prefs.getString("backup_folder", "")!!,
            prefs.getBoolean("backup_enabled", false), prefs.getLong("backup_last", 0), prefs.getString("backup_error", null))
    }
    init { prefs.registerOnSharedPreferenceChangeListener(backupListener) }
    fun saveBackup(folder: String, enabled: Boolean) {
        prefs.edit().putString("backup_folder", folder).putBoolean("backup_enabled", enabled).apply()
        _backup.value = _backup.value.copy(folder = folder, enabled = enabled)
    }
    fun backupResult(time: Long, error: String?) {
        val editor = prefs.edit().putString("backup_error", error)
        if (time > 0) editor.putLong("backup_last", time)
        editor.apply()
    }

    fun fitnessDefaults(): List<String> = listOf(
        prefs.getString("fitness_minutes", "7") ?: "7",
        prefs.getString("fitness_weight", "") ?: "",
        prefs.getString("fitness_effort", "MODERATE") ?: "MODERATE",
        prefs.getString("fitness_pushups", "20") ?: "20",
        prefs.getString("fitness_situps", "20") ?: "20",
        prefs.getString("fitness_crunches", "20") ?: "20"
    )

    fun saveFitnessDefaults(values: List<String>) {
        require(values.size == 6)
        val editor = prefs.edit()
        listOf("minutes", "weight", "effort", "pushups", "situps", "crunches").zip(values).forEach { (key, value) ->
            editor.putString("fitness_$key", value)
        }
        editor.apply()
    }

    companion object {
        const val DEFAULT_MODEL = "gemini-flash-latest"
        private const val KEY_API = "gemini_api_key"
        private const val KEY_MODEL = "gemini_model"
        private const val KEY_REMINDER_ON = "reminder_enabled"
        private const val KEY_REMINDER_HOUR = "reminder_hour"
        private const val KEY_REMINDER_MINUTE = "reminder_minute"
    }
}
