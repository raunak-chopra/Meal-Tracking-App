package com.kalotracker.app.core.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
 * and reminder preferences. Stored in app-private preferences (never backed up or exported).
 */
class AppSettings(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("kalo_app_settings", Context.MODE_PRIVATE)

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

    companion object {
        const val DEFAULT_MODEL = "gemini-flash-latest"
        private const val KEY_API = "gemini_api_key"
        private const val KEY_MODEL = "gemini_model"
        private const val KEY_REMINDER_ON = "reminder_enabled"
        private const val KEY_REMINDER_HOUR = "reminder_hour"
        private const val KEY_REMINDER_MINUTE = "reminder_minute"
    }
}
