package com.kalotracker.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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

    // Camera Permission Launcher
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Handled in CameraScreen
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkPermissions()

        val startDestination = when (intent?.getStringExtra("EXTRA_START_DESTINATION")) {
            "camera" -> com.kalotracker.app.navigation.KaloDestinations.CAMERA
            else -> com.kalotracker.app.navigation.KaloDestinations.DASHBOARD
        }

        setContent {
            KaloTheme {
                KaloNavHost(
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

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
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
