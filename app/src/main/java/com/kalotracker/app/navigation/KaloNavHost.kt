package com.kalotracker.app.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Insights
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import com.kalotracker.app.core.designsystem.components.KaloButton
import com.kalotracker.app.core.util.LogDate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.kalotracker.app.feature.meal.ManualMealViewModel
import com.kalotracker.app.feature.meal.ManualMealScreen
import com.kalotracker.app.feature.meal.MealViewModel
import com.kalotracker.app.feature.meal.MealViewModelFactory
import com.kalotracker.app.feature.settings.SettingsScreen
import com.kalotracker.app.feature.settings.SettingsViewModel
import com.kalotracker.app.feature.settings.SettingsViewModelFactory
import com.kalotracker.app.feature.workout.WorkoutScreen
import com.kalotracker.app.feature.workout.WorkoutViewModel
import com.kalotracker.app.feature.workout.WorkoutViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KaloNavHost(
    personalDao: com.kalotracker.app.core.database.dao.PersonalDao,
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
    val appContext = LocalContext.current.applicationContext

    val dashboardViewModel: DashboardViewModel = viewModel(key = "daily", factory = DashboardViewModelFactory(
        personalDao, mealRepository, workoutRepository, userProfileRepository, waterRepository, healthConnectManager))
    val day by dashboardViewModel.uiState.collectAsState()
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: KaloDestinations.DASHBOARD
    val rootRoutes = listOf(KaloDestinations.DASHBOARD, KaloDestinations.MEALS, KaloDestinations.TRENDS)
    var addMeal by rememberSaveable { mutableStateOf(startDestination == KaloDestinations.CAMERA) }
    var photoDisclosure by rememberSaveable { mutableStateOf(false) }
    var logTimestamp by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    fun openLog(destination: String) {
        logTimestamp = LogDate.timestamp(day.selectedDate)
        addMeal = false
        navController.navigate(destination)
    }
    fun navigateRoot(destination: String) { navController.navigate(destination) {
        popUpTo(KaloDestinations.DASHBOARD) { saveState = true }
        launchSingleTop = true; restoreState = true
    } }
    if (addMeal) ModalBottomSheet(onDismissRequest = { addMeal = false }) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text("Add a meal", style = MaterialTheme.typography.titleLarge)
            Text("Choose how to log. Review portions and time before saving.")
            TextButton(onClick = { addMeal = false; photoDisclosure = true }, modifier = Modifier.fillMaxWidth().heightIn(min=56.dp)) { Text("Photo") }
            TextButton(onClick = { openLog(KaloDestinations.BARCODE_SCANNER) }, modifier = Modifier.fillMaxWidth().heightIn(min=56.dp)) { Text("Barcode · Open Food Facts") }
            TextButton(onClick = { openLog(KaloDestinations.MANUAL_MEAL) }, modifier = Modifier.fillMaxWidth().heightIn(min=56.dp)) { Text("Search & quick log") }
            TextButton(onClick = { openLog(KaloDestinations.MANUAL_MEAL) }, modifier = Modifier.fillMaxWidth().heightIn(min=56.dp)) { Text("Recent meals · review before saving") }
            TextButton(onClick = { openLog(KaloDestinations.PERSONAL_FOODS) }, modifier = Modifier.fillMaxWidth().heightIn(min=56.dp)) { Text("My foods, favorites & recipes") }
        }
    }
    if (photoDisclosure) AlertDialog(onDismissRequest = { photoDisclosure = false },
        title = { Text("Photo estimate") }, text = { Text("A photo you capture or choose is sent to Google Gemini using your own API key. Check the estimated foods and portions before saving. Your saved meal stays on this device.") },
        confirmButton = { TextButton(onClick = { photoDisclosure = false; openLog(KaloDestinations.CAMERA) }) { Text("Continue to photo") } },
        dismissButton = { TextButton(onClick = { photoDisclosure = false }) { Text("Cancel") } })
    Scaffold(contentWindowInsets = WindowInsets(0), bottomBar = {
        if (route in rootRoutes) Column {
            Box(Modifier.fillMaxWidth().padding(horizontal=20.dp, vertical=8.dp)) { KaloButton("Add meal", { addMeal = true }) }
            NavigationBar {
                val labels = listOf("Today", "Meals", "Progress")
                val icons = listOf(Icons.Default.Home, Icons.Default.Restaurant, Icons.Default.Insights)
                rootRoutes.forEachIndexed { i, destination -> NavigationBarItem(selected = route == destination,
                    onClick = { navigateRoot(destination) }, icon = { Icon(icons[i], contentDescription = null) }, label = { Text(labels[i]) }) }
            }
        }
    }) { rootPadding ->
    NavHost(navController = navController, startDestination = KaloDestinations.DASHBOARD,
        modifier = modifier.padding(rootPadding)) {
        composable(KaloDestinations.MEALS) {
            com.kalotracker.app.feature.dashboard.MealsScreen(dashboardViewModel,
                onEdit = { navController.navigate(KaloDestinations.editMeal(it)) },
                onOpenLibrary = { openLog(KaloDestinations.PERSONAL_FOODS) })
        }
        composable(KaloDestinations.DASHBOARD) {
            DashboardScreen(
                viewModel = dashboardViewModel,
                onNavigateToCamera = { openLog(KaloDestinations.CAMERA) },
                onNavigateToManualMeal = { openLog(KaloDestinations.MANUAL_MEAL) },
                onNavigateToBarcode = { openLog(KaloDestinations.BARCODE_SCANNER) },
                onNavigateToWorkout = { openLog(KaloDestinations.WORKOUT) },
                onNavigateToHealthPermissions = { navController.navigate(KaloDestinations.HEALTH_PERMISSIONS) },
                onNavigateToSettings = { navController.navigate(KaloDestinations.SETTINGS) },
                onNavigateToEditWorkout = { id -> navController.navigate(KaloDestinations.editWorkout(id)) },
                onNavigateToEditMeal = { id -> navController.navigate(KaloDestinations.editMeal(id)) },
                onAddMeal = { addMeal = true }, onViewMeals = { navigateRoot(KaloDestinations.MEALS) },
                onNavigateToTrends = { navigateRoot(KaloDestinations.TRENDS) }
            )
        }

        composable(KaloDestinations.CAMERA) {
            val mealViewModel: MealViewModel = viewModel(
                factory = MealViewModelFactory(mealRepository, analysisService, logTimestamp,
                    com.kalotracker.app.feature.meal.FileMealDraftStore(java.io.File(appContext.noBackupFilesDir, "meal-draft.json")))
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
            LaunchedEffect(Unit) { recentMeals = try { mealRepository.getRecentDistinctMeals() } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel } catch (_: Exception) { emptyList() } }
            val manualViewModel: ManualMealViewModel = viewModel()
            ManualMealScreen(
                viewModel = manualViewModel, initialTimestamp = logTimestamp,
                onOpenLibrary = { navController.navigate(KaloDestinations.PERSONAL_FOODS) },
                recentMeals = recentMeals,
                onLogAgain = { meal, time ->
                    mealRepository.duplicateMeal(meal, time)
                    Unit
                },
                onClose = { navController.popBackStack() },
                onSaveMeal = { meal, items ->
                    mealRepository.saveMeal(meal, items)
                },
                onScanBarcode = { navController.navigate(KaloDestinations.BARCODE_SCANNER) }
            )
        }

        composable(KaloDestinations.PERSONAL_FOODS) {
            val vm: com.kalotracker.app.feature.meal.PersonalFoodsViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST") override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                    com.kalotracker.app.feature.meal.PersonalFoodsViewModel(personalDao, mealRepository) as T
            })
            com.kalotracker.app.feature.meal.PersonalFoodsScreen(vm, logTimestamp) { navController.popBackStack() }
        }
        composable(KaloDestinations.WORKOUT) {
            val workoutViewModel: WorkoutViewModel = viewModel(
                factory = WorkoutViewModelFactory(workoutRepository, personalDao, initialTimestamp = LogDate.timestamp(day.selectedDate))
            )

            WorkoutScreen(
                viewModel = workoutViewModel,
                onClose = { navController.popBackStack() },
                onWorkoutSaved = { navController.popBackStack() },
                quickFitness = true, fitnessDefaults = appSettings.fitnessDefaults(),
                onFitnessDefaultsSaved = appSettings::saveFitnessDefaults
            )
        }

        composable(KaloDestinations.EDIT_WORKOUT, arguments = listOf(navArgument("workoutId") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString("workoutId") ?: return@composable
            val vm: WorkoutViewModel = viewModel(key = "workout_$id", factory = WorkoutViewModelFactory(workoutRepository, personalDao, id))
            WorkoutScreen(vm, { navController.popBackStack() }, { navController.popBackStack() })
        }
        composable(KaloDestinations.TRENDS) {
            val trendsViewModel: TrendsViewModel = viewModel(
                factory = TrendsViewModelFactory(
                    personalDao = personalDao,
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
                appSettings = appSettings,
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
                factory = BarcodeScannerViewModelFactory(mealRepository, personalDao, logTimestamp) {
                    appContext.assets.open("indian_snacks_100.json").bufferedReader().use {
                        com.kalotracker.app.core.network.IndianSnackCatalog.parse(it.readText())
                    }
                }
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
}
