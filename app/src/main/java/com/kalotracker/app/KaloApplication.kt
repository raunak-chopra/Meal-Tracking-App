package com.kalotracker.app

import android.app.Application
import com.kalotracker.app.core.data.repository.MealRepository
import com.kalotracker.app.core.data.repository.UserProfileRepository
import com.kalotracker.app.core.data.repository.WorkoutRepository
import com.kalotracker.app.core.database.KaloDatabase
import com.kalotracker.app.core.health.HealthConnectManager
import com.kalotracker.app.core.network.MealAnalysisService
import com.kalotracker.app.core.settings.AppSettings

class KaloApplication : Application() {

    val database by lazy { KaloDatabase.getInstance(this) }
    val healthConnectManager by lazy { HealthConnectManager(this) }

    val mealRepository by lazy { MealRepository(database.mealDao()) }
    val workoutRepository by lazy { WorkoutRepository(database.workoutDao()) }
    val userProfileRepository by lazy { UserProfileRepository(this) }
    val appSettings by lazy { AppSettings(this) }
    val analysisService by lazy { MealAnalysisService { appSettings.ai.value } }
    val waterRepository by lazy { com.kalotracker.app.core.data.repository.WaterRepository(database.waterDao()) }

    override fun onCreate() {
        super.onCreate()
    }
}
