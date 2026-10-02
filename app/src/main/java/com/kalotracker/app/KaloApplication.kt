package com.kalotracker.app

import android.app.Application
import androidx.room.InvalidationTracker
import com.kalotracker.app.core.data.backup.BackupManager
import com.kalotracker.app.feature.widget.KaloWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import com.kalotracker.app.core.data.repository.MealRepository
import com.kalotracker.app.core.data.repository.UserProfileRepository
import com.kalotracker.app.core.data.repository.WeightRepository
import com.kalotracker.app.core.data.repository.WorkoutRepository
import com.kalotracker.app.core.database.KaloDatabase
import com.kalotracker.app.core.health.HealthConnectManager
import com.kalotracker.app.core.network.MealAnalysisService
import com.kalotracker.app.core.reminder.ReminderScheduler
import com.kalotracker.app.core.settings.AppSettings

class KaloApplication : Application() {

    val database by lazy { KaloDatabase.getInstance(this) }
    val healthConnectManager by lazy { HealthConnectManager(this) }

    val mealRepository by lazy { MealRepository(database.mealDao()) }
    val workoutRepository by lazy { WorkoutRepository(database.workoutDao()) }
    val userProfileRepository by lazy { UserProfileRepository(this) }
    val appSettings by lazy { AppSettings(this) }
    val analysisService by lazy { MealAnalysisService { appSettings.ai.value } }
    val weightRepository by lazy { WeightRepository(database.weightDao()) }
    val backupManager by lazy { BackupManager(database, userProfileRepository, java.io.File(filesDir, "meals"), appSettings, java.io.File(noBackupFilesDir, "meal-draft.json")) }
    val waterRepository by lazy { com.kalotracker.app.core.data.repository.WaterRepository(database.waterDao()) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Keep the home-screen widget in sync with any change to meals, workouts, water or goals.
        database.invalidationTracker.addObserver(object : InvalidationTracker.Observer("meals", "workouts", "water_logs") {
            override fun onInvalidated(tables: Set<String>) {
                KaloWidgetUpdater.update(this@KaloApplication)
            }
        })
        appScope.launch { userProfileRepository.history.collect { database.personalDao().putGoals(it) } }
        appScope.launch {
            userProfileRepository.profile.drop(1).collect { KaloWidgetUpdater.update(this@KaloApplication) }
        }

        appScope.launch { appSettings.appearance.drop(1).collect { KaloWidgetUpdater.update(this@KaloApplication) } }

        // Keep an already-scheduled reminder on its exact time; only creates one if missing.
        ReminderScheduler.schedule(this, appSettings.reminder.value, replace = false)
        com.kalotracker.app.core.data.backup.BackupScheduler.schedule(this, appSettings.backup.value.enabled)
    }
}
