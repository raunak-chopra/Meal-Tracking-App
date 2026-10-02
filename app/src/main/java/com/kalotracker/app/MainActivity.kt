package com.kalotracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.lifecycleScope
import com.kalotracker.app.core.designsystem.KaloTheme
import com.kalotracker.app.navigation.KaloNavHost
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val app by lazy { application as KaloApplication }

    // Health Connect Permission Launcher
    private val healthPermissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        // Permissions updated - will be reflected in Compose flows on next read
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val startDestination = when (intent?.getStringExtra("EXTRA_START_DESTINATION")) {
            "camera" -> com.kalotracker.app.navigation.KaloDestinations.CAMERA
            else -> com.kalotracker.app.navigation.KaloDestinations.DASHBOARD
        }

        setContent {
            val appearance by app.appSettings.appearance.collectAsState()
            KaloTheme(appearance) {
                KaloNavHost(
                    personalDao = app.database.personalDao(),
                    mealRepository = app.mealRepository,
                    workoutRepository = app.workoutRepository,
                    userProfileRepository = app.userProfileRepository,
                    appSettings = app.appSettings,
                    analysisService = app.analysisService,
                    waterRepository = app.waterRepository,
                    weightRepository = app.weightRepository,
                    backupManager = app.backupManager,
                    healthConnectManager = app.healthConnectManager,
                    onOpenHealthPermissions = { requestHealthPermissions() },
                    startDestination = startDestination
                )
            }
        }
    }

    private fun requestHealthPermissions() {
        lifecycleScope.launch {
            if (!app.healthConnectManager.hasAllPermissions()) {
                healthPermissionLauncher.launch(app.healthConnectManager.permissions)
            }
        }
    }
}
