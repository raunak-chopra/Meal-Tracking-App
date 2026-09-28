package com.kalotracker.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kalotracker.app.core.data.repository.AuthRepository
import com.kalotracker.app.core.data.repository.MealRepository
import com.kalotracker.app.core.data.repository.UserProfileRepository
import com.kalotracker.app.core.data.repository.WaterRepository
import com.kalotracker.app.core.data.repository.WorkoutRepository
import com.kalotracker.app.core.health.HealthConnectManager
import com.kalotracker.app.feature.auth.AuthScreen
import com.kalotracker.app.feature.auth.AuthViewModel
import com.kalotracker.app.feature.auth.AuthViewModelFactory
import com.kalotracker.app.feature.barcode.BarcodeScannerScreen
import com.kalotracker.app.feature.barcode.BarcodeScannerViewModel
import com.kalotracker.app.feature.barcode.BarcodeScannerViewModelFactory
import com.kalotracker.app.feature.dashboard.DashboardScreen
import com.kalotracker.app.feature.dashboard.DashboardViewModel
import com.kalotracker.app.feature.dashboard.DashboardViewModelFactory
import com.kalotracker.app.feature.health.HealthPermissionsScreen
import com.kalotracker.app.feature.meal.CameraScreen
import com.kalotracker.app.feature.meal.ManualMealScreen
import com.kalotracker.app.feature.meal.MealViewModel
import com.kalotracker.app.feature.meal.MealViewModelFactory
import com.kalotracker.app.feature.settings.SettingsScreen
import com.kalotracker.app.feature.settings.SettingsViewModel
import com.kalotracker.app.feature.settings.SettingsViewModelFactory
import com.kalotracker.app.feature.workout.WorkoutScreen
import com.kalotracker.app.feature.workout.WorkoutViewModel
import com.kalotracker.app.feature.workout.WorkoutViewModelFactory
import kotlinx.coroutines.launch

@Composable
fun KaloNavHost(
    mealRepository: MealRepository,
    workoutRepository: WorkoutRepository,
    userProfileRepository: UserProfileRepository,
    authRepository: AuthRepository,
    waterRepository: WaterRepository,
    healthConnectManager: HealthConnectManager,
    onOpenHealthPermissions: () -> Unit,
    modifier: Modifier = Modifier,
    startDestination: String = KaloDestinations.DASHBOARD,
    navController: NavHostController = rememberNavController()
) {
    val coroutineScope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(KaloDestinations.DASHBOARD) {
            val dashboardViewModel: DashboardViewModel = viewModel(
                factory = DashboardViewModelFactory(
                    mealRepository = mealRepository,
                    workoutRepository = workoutRepository,
                    userProfileRepository = userProfileRepository,
                    waterRepository = waterRepository,
                    healthConnectManager = healthConnectManager
                )
            )

            DashboardScreen(
                viewModel = dashboardViewModel,
                onNavigateToCamera = { navController.navigate(KaloDestinations.CAMERA) },
                onNavigateToManualMeal = { navController.navigate(KaloDestinations.MANUAL_MEAL) },
                onNavigateToBarcode = { navController.navigate(KaloDestinations.BARCODE_SCANNER) },
                onNavigateToWorkout = { navController.navigate(KaloDestinations.WORKOUT) },
                onNavigateToHealthPermissions = { navController.navigate(KaloDestinations.HEALTH_PERMISSIONS) },
                onNavigateToSettings = { navController.navigate(KaloDestinations.SETTINGS) }
            )
        }

        composable(KaloDestinations.CAMERA) {
            val mealViewModel: MealViewModel = viewModel(
                factory = MealViewModelFactory(mealRepository)
            )

            CameraScreen(
                viewModel = mealViewModel,
                onClose = { navController.popBackStack() },
                onMealSaved = { navController.popBackStack() }
            )
        }

        composable(KaloDestinations.MANUAL_MEAL) {
            ManualMealScreen(
                onClose = { navController.popBackStack() },
                onSaveMeal = { meal, items ->
                    coroutineScope.launch {
                        mealRepository.saveMeal(meal, items)
                    }
                },
                onScanBarcode = { navController.navigate(KaloDestinations.BARCODE_SCANNER) }
            )
        }

        composable(KaloDestinations.WORKOUT) {
            val workoutViewModel: WorkoutViewModel = viewModel(
                factory = WorkoutViewModelFactory(workoutRepository)
            )

            WorkoutScreen(
                viewModel = workoutViewModel,
                onClose = { navController.popBackStack() },
                onWorkoutSaved = { navController.popBackStack() }
            )
        }

        composable(KaloDestinations.SETTINGS) {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(
                    userProfileRepository = userProfileRepository,
                    mealRepository = mealRepository,
                    workoutRepository = workoutRepository,
                    authRepository = authRepository
                )
            )

            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() },
                onNavigateToAuth = { navController.navigate(KaloDestinations.AUTH) }
            )
        }

        composable(KaloDestinations.HEALTH_PERMISSIONS) {
            HealthPermissionsScreen(
                onRequestPermissions = {
                    onOpenHealthPermissions()
                    navController.popBackStack()
                },
                onClose = { navController.popBackStack() }
            )
        }

        composable(KaloDestinations.AUTH) {
            val authViewModel: AuthViewModel = viewModel(
                factory = AuthViewModelFactory(authRepository)
            )

            AuthScreen(
                viewModel = authViewModel,
                onAuthSuccess = {
                    navController.navigate(KaloDestinations.DASHBOARD) {
                        popUpTo(KaloDestinations.DASHBOARD) { inclusive = true }
                    }
                }
            )
        }

        composable(KaloDestinations.BARCODE_SCANNER) {
            val barcodeViewModel: BarcodeScannerViewModel = viewModel(
                factory = BarcodeScannerViewModelFactory(mealRepository)
            )

            BarcodeScannerScreen(
                viewModel = barcodeViewModel,
                onClose = { navController.popBackStack() },
                onMealSaved = { navController.popBackStack() }
            )
        }
    }
}
