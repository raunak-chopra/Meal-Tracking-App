package com.kalotracker.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import com.kalotracker.app.core.designsystem.*
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
import com.kalotracker.app.feature.meal.ManualMealViewModelFactory
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
    val aiSettings by appSettings.ai.collectAsState()
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: KaloDestinations.DASHBOARD
    val rootRoutes = listOf(KaloDestinations.DASHBOARD, KaloDestinations.MEALS, KaloDestinations.WORKOUTS_HUB, KaloDestinations.TRENDS)
    var addMeal by rememberSaveable { mutableStateOf(startDestination == KaloDestinations.CAMERA) }
    var addWorkout by rememberSaveable { mutableStateOf(false) }
    var photoDisclosure by rememberSaveable { mutableStateOf(false) }
    var aiSetupRequired by rememberSaveable { mutableStateOf(false) }
    var logTimestamp by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    fun openLog(destination: String) {
        logTimestamp = LogDate.timestamp(day.selectedDate)
        addMeal = false
        addWorkout = false
        navController.navigate(destination)
    }
    fun navigateRoot(destination: String) { navController.navigate(destination) {
        popUpTo(KaloDestinations.DASHBOARD) { saveState = true }
        launchSingleTop = true; restoreState = true
    } }
    if (addMeal) ModalBottomSheet(
        onDismissRequest = { addMeal = false },
        containerColor = KaloSurface,
        contentColor = KaloTextPrimary
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Add a meal", style = KaloTypography.titleLarge, color = KaloTextPrimary)
                Text(
                    "Choose how to log. Review portions and time before saving.",
                    style = KaloTypography.bodyMedium,
                    color = KaloTextSecondary
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AddMealHeroTile(
                    title = "Photo",
                    subtitle = if (aiSettings.isConfigured) "Gemini portion estimate" else "Optional · connect AI first",
                    icon = Icons.Default.CameraAlt,
                    accent = KaloAccent,
                    onClick = {
                        addMeal = false
                        if (!aiSettings.isConfigured) {
                            aiSetupRequired = true
                        } else if (appSettings.hasSeenPhotoDisclosure()) {
                            openLog(KaloDestinations.CAMERA)
                        } else {
                            photoDisclosure = true
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                AddMealHeroTile(
                    title = "Barcode · Open Food Facts",
                    subtitle = "Scan packet or offline cache",
                    icon = Icons.Default.CropFree,
                    accent = KaloCarbs,
                    onClick = { openLog(KaloDestinations.BARCODE_SCANNER) },
                    modifier = Modifier.weight(1f)
                )
            }
            AddMealOptionRow(
                title = "Recent meals",
                subtitle = "Find a past meal and check portions before saving",
                icon = Icons.Default.History,
                onClick = { openLog(KaloDestinations.MANUAL_MEAL) }
            )
            AddMealOptionRow(
                title = "Describe, search & quick log",
                subtitle = "Type what you ate in plain words or search common foods",
                icon = Icons.Default.Search,
                onClick = { openLog(KaloDestinations.MANUAL_MEAL) }
            )
            AddMealOptionRow(
                title = "My foods, favorites & recipes",
                subtitle = "Saved packet labels, home-cooked batches & templates",
                icon = Icons.Default.BookmarkBorder,
                onClick = { openLog(KaloDestinations.PERSONAL_FOODS) }
            )
            Spacer(Modifier.height(8.dp))
        }
    }
    if (addWorkout) ModalBottomSheet(
        onDismissRequest = { addWorkout = false },
        containerColor = KaloSurface,
        contentColor = KaloTextPrimary
    ) {
        val fitnessDefaults = appSettings.fitnessDefaults()
        val defaultMinutes = fitnessDefaults.getOrNull(0)?.toIntOrNull()?.coerceIn(3, 60) ?: 7
        val defaultPushups = fitnessDefaults.getOrNull(3)?.toIntOrNull()?.takeIf { it > 0 } ?: 20
        val defaultSitups = fitnessDefaults.getOrNull(4)?.toIntOrNull()?.takeIf { it > 0 } ?: 20
        val defaultCrunches = fitnessDefaults.getOrNull(5)?.toIntOrNull()?.takeIf { it > 0 } ?: 20
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Log exercise", style = KaloTypography.titleLarge, color = KaloTextPrimary)
                Text(
                    "Check off your daily habit in one tap, repeat your last session, or log a full workout.",
                    style = KaloTypography.bodyMedium,
                    color = KaloTextSecondary
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AddMealHeroTile(
                    title = "1-tap Daily Fitness",
                    subtitle = "$defaultPushups push · $defaultSitups sit · $defaultCrunches crunch ($defaultMinutes min)",
                    icon = Icons.Default.Check,
                    accent = KaloProtein,
                    onClick = {
                        addWorkout = false
                        dashboardViewModel.quickLogDailyFitness(
                            defaultMinutes,
                            listOf(defaultPushups, defaultSitups, defaultCrunches)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
                AddMealHeroTile(
                    title = "Gym / Cardio",
                    subtitle = "Sets, weights, cardio & rest timer",
                    icon = Icons.Default.FitnessCenter,
                    accent = KaloAccent,
                    onClick = { openLog(KaloDestinations.DETAILED_WORKOUT) },
                    modifier = Modifier.weight(1f)
                )
            }
            AddMealOptionRow(
                title = "Customize daily fitness",
                subtitle = "Adjust minutes, effort or reps before saving",
                icon = Icons.Default.FitnessCenter,
                onClick = { openLog(KaloDestinations.WORKOUT) }
            )
            day.lastWorkout?.let { last ->
                AddMealOptionRow(
                    title = "Repeat \"${last.workout.title}\"",
                    subtitle = "${last.workout.durationMinutes} min · ~${last.workout.estimatedCaloriesBurned} kcal (1-tap log)",
                    icon = Icons.Default.Refresh,
                    onClick = {
                        addWorkout = false
                        dashboardViewModel.logWorkoutAgain(last)
                    }
                )
            }
            day.routines.take(3).forEach { routine ->
                AddMealOptionRow(
                    title = "Routine: ${routine.name}",
                    subtitle = "Tap to log this saved routine now",
                    icon = Icons.Default.BookmarkBorder,
                    onClick = {
                        addWorkout = false
                        dashboardViewModel.quickLogRoutine(routine)
                    }
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
    if (aiSetupRequired) AlertDialog(
        onDismissRequest = { aiSetupRequired = false },
        title = { Text("Optional photo estimates") },
        text = { Text("Photo estimates use Google Gemini with your key. You can log meals using search without setting this up.") },
        confirmButton = { TextButton(onClick = {
            aiSetupRequired = false
            navController.navigate(KaloDestinations.SETTINGS)
        }) { Text("Set up AI estimates") } },
        dismissButton = { TextButton(onClick = {
            aiSetupRequired = false
            openLog(KaloDestinations.MANUAL_MEAL)
        }) { Text("Use search instead") } }
    )
    if (photoDisclosure) AlertDialog(
        onDismissRequest = { photoDisclosure = false },
        title = { Text("Photo estimate") },
        text = {
            Text("A photo you capture or choose is sent to Google Gemini using your own API key. Check the estimated foods and portions before saving. Your saved meal stays on this device.")
        },
        confirmButton = {
            TextButton(onClick = {
                appSettings.acknowledgePhotoDisclosure()
                photoDisclosure = false
                openLog(KaloDestinations.CAMERA)
            }) { Text("Continue to photo") }
        },
        dismissButton = { TextButton(onClick = { photoDisclosure = false }) { Text("Cancel") } }
    )
    Scaffold(contentWindowInsets = WindowInsets(0), bottomBar = {
        if (route in rootRoutes) Column(Modifier.background(KaloBackground)) {
            if (route != KaloDestinations.TRENDS) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KaloButton(
                        text = "+ Add meal",
                        onClick = { addMeal = true },
                        modifier = Modifier.weight(1f)
                    )
                    KaloButton(
                        text = if (day.todayWorkouts.isEmpty()) "+ Log exercise" else "✓ Exercise (${day.todayWorkouts.size})",
                        onClick = { addWorkout = true },
                        containerColor = if (day.todayWorkouts.isEmpty()) KaloSurfaceElevated else KaloProtein,
                        contentColor = if (day.todayWorkouts.isEmpty()) KaloTextPrimary else KaloBackground,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            NavigationBar(containerColor = KaloSurface) {
                val labels = listOf("Today", "Meals", "Workouts", "Progress")
                val icons = listOf(Icons.Default.Home, Icons.Default.Restaurant, Icons.Default.FitnessCenter, Icons.Default.Insights)
                rootRoutes.forEachIndexed { i, destination ->
                    NavigationBarItem(
                        selected = route == destination,
                        onClick = { navigateRoot(destination) },
                        icon = { Icon(icons[i], contentDescription = null) },
                        label = { Text(labels[i]) }
                    )
                }
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
        composable(KaloDestinations.WORKOUTS_HUB) {
            val currentDefaults = appSettings.fitnessDefaults().let { d ->
                val known = userProfileRepository.body.value?.weightKg
                if (d[1].isBlank() && known != null) d.toMutableList().also { it[1] = com.kalotracker.app.feature.meal.formatAmount(known, "").trim() } else d
            }
            com.kalotracker.app.feature.dashboard.WorkoutsHubScreen(
                viewModel = dashboardViewModel,
                fitnessDefaults = currentDefaults,
                onQuickFitness = { openLog(KaloDestinations.WORKOUT) },
                onDetailedWorkout = { openLog(KaloDestinations.DETAILED_WORKOUT) },
                onEditWorkout = { id -> navController.navigate(KaloDestinations.editWorkout(id)) },
                onNavigateToHealthPermissions = { navController.navigate(KaloDestinations.HEALTH_PERMISSIONS) }
            )
        }
        composable(KaloDestinations.DASHBOARD) {
            val currentDefaults = appSettings.fitnessDefaults().let { d ->
                val known = userProfileRepository.body.value?.weightKg
                if (d[1].isBlank() && known != null) d.toMutableList().also { it[1] = com.kalotracker.app.feature.meal.formatAmount(known, "").trim() } else d
            }
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
                onAddMeal = { addMeal = true },
                onViewMeals = { navigateRoot(KaloDestinations.MEALS) },
                onViewWorkouts = { navigateRoot(KaloDestinations.WORKOUTS_HUB) },
                onNavigateToDetailedWorkout = { openLog(KaloDestinations.DETAILED_WORKOUT) },
                fitnessDefaults = currentDefaults,
                onNavigateToTrends = { navigateRoot(KaloDestinations.TRENDS) }
            )
        }

        composable(KaloDestinations.CAMERA) {
            val mealViewModel: MealViewModel = viewModel(
                factory = MealViewModelFactory(mealRepository, analysisService, logTimestamp,
                    com.kalotracker.app.feature.meal.FileMealDraftStore(java.io.File(appContext.noBackupFilesDir, com.kalotracker.app.feature.meal.MEAL_DRAFT_FILE)),
                    com.kalotracker.app.feature.meal.FileMealDraftStore(java.io.File(appContext.noBackupFilesDir, com.kalotracker.app.feature.meal.PARKED_MEAL_DRAFT_FILE)))
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
                factory = EditMealViewModelFactory(mealId, mealRepository, analysisService)
            )
            EditMealScreen(
                viewModel = editViewModel,
                onClose = { navController.popBackStack() }
            )
        }

        composable(KaloDestinations.MANUAL_MEAL) {
            val manualViewModel: ManualMealViewModel = viewModel(factory = ManualMealViewModelFactory(analysisService, mealRepository))
            val recentState by manualViewModel.recentMeals.collectAsState()
            ManualMealScreen(
                viewModel = manualViewModel, initialTimestamp = logTimestamp,
                aiConfigured = aiSettings.isConfigured,
                onOpenAiSettings = { navController.navigate(KaloDestinations.SETTINGS) },
                onOpenLibrary = { navController.navigate(KaloDestinations.PERSONAL_FOODS) },
                recentMeals = recentState.meals,
                recentMealsLoading = recentState.isLoading,
                recentMealsError = recentState.error,
                onRetryRecentMeals = manualViewModel::loadRecentMeals,
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
                factory = WorkoutViewModelFactory(workoutRepository, personalDao, initialTimestamp = LogDate.timestamp(day.selectedDate),
                    bodyWeightKg = { userProfileRepository.body.value?.weightKg?.toDouble() })
            )

            WorkoutScreen(
                viewModel = workoutViewModel,
                onClose = { navController.popBackStack() },
                onWorkoutSaved = { navController.popBackStack() },
                quickFitness = true, fitnessDefaults = appSettings.fitnessDefaults().let { d ->
                    val known = userProfileRepository.body.value?.weightKg
                    if (d[1].isBlank() && known != null) d.toMutableList().also { it[1] = com.kalotracker.app.feature.meal.formatAmount(known, "").trim() } else d
                },
                onFitnessDefaultsSaved = appSettings::saveFitnessDefaults
            )
        }

        composable(KaloDestinations.DETAILED_WORKOUT) {
            val workoutViewModel: WorkoutViewModel = viewModel(
                key = "detailed_workout_${day.selectedDate}",
                factory = WorkoutViewModelFactory(workoutRepository, personalDao, initialTimestamp = LogDate.timestamp(day.selectedDate),
                    bodyWeightKg = { userProfileRepository.body.value?.weightKg?.toDouble() })
            )

            WorkoutScreen(
                viewModel = workoutViewModel,
                onClose = { navController.popBackStack() },
                onWorkoutSaved = { navController.popBackStack() },
                quickFitness = false,
                fitnessDefaults = appSettings.fitnessDefaults().let { d ->
                    val known = userProfileRepository.body.value?.weightKg
                    if (d[1].isBlank() && known != null) d.toMutableList().also { it[1] = com.kalotracker.app.feature.meal.formatAmount(known, "").trim() } else d
                },
                onFitnessDefaultsSaved = appSettings::saveFitnessDefaults
            )
        }

        composable(KaloDestinations.EDIT_WORKOUT, arguments = listOf(navArgument("workoutId") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString("workoutId") ?: return@composable
            val vm: WorkoutViewModel = viewModel(key = "workout_$id", factory = WorkoutViewModelFactory(workoutRepository, personalDao, id,
                bodyWeightKg = { userProfileRepository.body.value?.weightKg?.toDouble() }))
            WorkoutScreen(
                viewModel = vm,
                onClose = { navController.popBackStack() },
                onWorkoutSaved = { navController.popBackStack() },
                fitnessDefaults = appSettings.fitnessDefaults().let { d ->
                    val known = userProfileRepository.body.value?.weightKg
                    if (d[1].isBlank() && known != null) d.toMutableList().also { it[1] = com.kalotracker.app.feature.meal.formatAmount(known, "").trim() } else d
                },
                onFitnessDefaultsSaved = appSettings::saveFitnessDefaults
            )
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

@Composable
private fun AddMealHeroTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = KaloSurfaceElevated,
        modifier = modifier.heightIn(min = 104.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Text(title, style = KaloTypography.titleMedium, color = KaloTextPrimary)
            Text(subtitle, style = KaloTypography.labelSmall, color = KaloTextSecondary)
        }
    }
}

@Composable
private fun AddMealOptionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = KaloSurfaceElevated,
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(KaloSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = KaloAccent, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = KaloTypography.titleMedium, color = KaloTextPrimary)
                Text(subtitle, style = KaloTypography.bodyMedium, color = KaloTextSecondary)
            }
        }
    }
}
