package com.kalotracker.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.kalotracker.app.core.database.dao.MealWithItems
import com.kalotracker.app.feature.meal.EditMealScreen
import com.kalotracker.app.feature.meal.EditMealViewModel
import com.kalotracker.app.feature.meal.EditMealViewModelFactory
import com.kalotracker.app.core.data.repository.MealRepository
import com.kalotracker.app.core.data.repository.UserProfileRepository
import com.kalotracker.app.core.data.backup.BackupManager
import com.kalotracker.app.core.data.repository.WaterRepository
import com.kalotracker.app.core.data.repository.WeightRepository
import com.kalotracker.app.core.reminder.ReminderScheduler
import com.kalotracker.app.feature.trends.TrendsScreen
import com.kalotracker.app.feature.trends.TrendsViewModel
import com.kalotracker.app.feature.trends.TrendsViewModelFactory
import androidx.compose.ui.platform.LocalContext
import com.kalotracker.app.core.data.repository.WorkoutRepository
import com.kalotracker.app.core.health.HealthConnectManager
import com.kalotracker.app.core.network.MealAnalysisService
import com.kalotracker.app.core.settings.AppSettings
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
    appSettings: AppSettings,
    analysisService: MealAnalysisService,
    waterRepository: WaterRepository,
    weightRepository: WeightRepository,
    backupManager: BackupManager,
    healthConnectManager: HealthConnectManager,
    onOpenHealthPermissions: () -> Unit,
    modifier: Modifier = Modifier,
    startDestination: String = KaloDestinations.DASHBOARD,
    navController: NavHostController = rememberNavController()
) {
    val coroutineScope = rememberCoroutineScope()
    val appContext = LocalContext.current.applicationContext

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
                onNavigateToSettings = { navController.navigate(KaloDestinations.SETTINGS) },
                onNavigateToEditMeal = { id -> navController.navigate(KaloDestinations.editMeal(id)) },
                onNavigateToTrends = { navController.navigate(KaloDestinations.TRENDS) }
            )
        }

        composable(KaloDestinations.CAMERA) {
            val mealViewModel: MealViewModel = viewModel(
                factory = MealViewModelFactory(mealRepository, analysisService)
            )

            CameraScreen(
                viewModel = mealViewModel,
                onClose = { navController.popBackStack() },
                onMealSaved = { navController.popBackStack() },
                onOpenSettings = { navController.navigate(KaloDestinations.SETTINGS) }
            )
        }

        composable(
            route = KaloDestinations.EDIT_MEAL,
            arguments = listOf(navArgument("mealId") { type = NavType.StringType })
        ) { entry ->
            val mealId = entry.arguments?.getString("mealId").orEmpty()
            val editViewModel: EditMealViewModel = viewModel(
                key = "edit_$mealId",
                factory = EditMealViewModelFactory(mealId, mealRepository)
            )
            EditMealScreen(
                viewModel = editViewModel,
                onClose = { navController.popBackStack() }
            )
        }

        composable(KaloDestinations.MANUAL_MEAL) {
            var recentMeals by remember { mutableStateOf(emptyList<MealWithItems>()) }
            LaunchedEffect(Unit) { recentMeals = mealRepository.getRecentDistinctMeals() }
            ManualMealScreen(
                recentMeals = recentMeals,
                onLogAgain = { meal, time ->
                    coroutineScope.launch { mealRepository.duplicateMeal(meal, time) }
                },
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

        composable(KaloDestinations.TRENDS) {
            val trendsViewModel: TrendsViewModel = viewModel(
                factory = TrendsViewModelFactory(
                    mealRepository = mealRepository,
                    workoutRepository = workoutRepository,
                    waterRepository = waterRepository,
                    weightRepository = weightRepository,
                    userProfileRepository = userProfileRepository,
                    appSettings = appSettings,
                    analysisService = analysisService
                )
            )
            TrendsScreen(
                viewModel = trendsViewModel,
                onBack = { navController.popBackStack() },
                onOpenSettings = { navController.navigate(KaloDestinations.SETTINGS) }
            )
        }

        composable(KaloDestinations.SETTINGS) {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(
                    userProfileRepository = userProfileRepository,
                    appSettings = appSettings,
                    analysisService = analysisService,
                    backupManager = backupManager,
                    scheduleReminder = { ReminderScheduler.schedule(appContext, it, replace = true) }
                )
            )

            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
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

        composable(KaloDestinations.BARCODE_SCANNER) {
            val barcodeViewModel: BarcodeScannerViewModel = viewModel(
                factory = BarcodeScannerViewModelFactory(mealRepository)
            )

            BarcodeScannerScreen(
                viewModel = barcodeViewModel,
                onClose = { navController.popBackStack() },
                onMealSaved = { navController.popBackStack() },
                onEnterManually = {
                    navController.navigate(KaloDestinations.MANUAL_MEAL) {
                        popUpTo(KaloDestinations.BARCODE_SCANNER) { inclusive = true }
                    }
                }
            )
        }
    }
}
