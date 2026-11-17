package com.kalotracker.app.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kalotracker.app.core.ai.InsightType
import com.kalotracker.app.core.ai.NutritionInsight
import com.kalotracker.app.core.database.dao.MealWithItems
import com.kalotracker.app.core.database.dao.WorkoutWithSets
import com.kalotracker.app.core.database.entity.MealEntity
import com.kalotracker.app.core.database.entity.WorkoutEntity
import com.kalotracker.app.core.designsystem.*
import com.kalotracker.app.core.designsystem.components.MacroSummaryCard
import com.kalotracker.app.core.designsystem.components.StepGaugeCard
import com.kalotracker.app.core.designsystem.components.WaterIntakeCard
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToCamera: () -> Unit,
    onNavigateToManualMeal: () -> Unit,
    onNavigateToBarcode: () -> Unit,
    onNavigateToWorkout: () -> Unit,
    onNavigateToHealthPermissions: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToEditWorkout: (String) -> Unit,
    onNavigateToEditMeal: (String) -> Unit,
    onNavigateToTrends: () -> Unit,
    modifier: Modifier = Modifier,
    onAddMeal: () -> Unit = onNavigateToManualMeal,
    onViewMeals: () -> Unit = {},
    onViewWorkouts: () -> Unit = onNavigateToWorkout,
    onNavigateToDetailedWorkout: () -> Unit = onNavigateToWorkout,
    fitnessDefaults: List<String> = listOf("7", "", "MODERATE", "20", "20", "20")
) {
    val state by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    var averagesExpanded by remember { mutableStateOf(false) }
    val defaultMinutes = fitnessDefaults.getOrNull(0)?.toIntOrNull()?.coerceIn(3, 60) ?: 7
    val defaultPushups = fitnessDefaults.getOrNull(3)?.toIntOrNull()?.takeIf { it > 0 } ?: 20
    val defaultSitups = fitnessDefaults.getOrNull(4)?.toIntOrNull()?.takeIf { it > 0 } ?: 20
    val defaultCrunches = fitnessDefaults.getOrNull(5)?.toIntOrNull()?.takeIf { it > 0 } ?: 20
    fun openDatePicker() {
        val date = state.selectedDate
        android.app.DatePickerDialog(context, { _, year, month, day ->
            viewModel.loadDataForDate(LocalDate.of(year, month + 1, day))
        }, date.year, date.monthValue - 1, date.dayOfMonth).apply {
            datePicker.maxDate = System.currentTimeMillis()
        }.show()
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) viewModel.onResume() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.pendingUndo) {
        state.pendingUndo?.let { undo ->
            if (snackbar.showSnackbar(undo.message, "Undo", duration = SnackbarDuration.Long) == SnackbarResult.ActionPerformed) viewModel.undoDelete()
            else viewModel.dismissUndo()
        }
    }
    LaunchedEffect(state.repeatMessage) { state.repeatMessage?.let { snackbar.showSnackbar(it); viewModel.dismissRepeatMessage() } }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = KaloBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                com.kalotracker.app.core.designsystem.components.BrandLockup()
                IconButton(onClick = onNavigateToSettings, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Settings, "Settings", tint = KaloTextPrimary)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (state.isToday) "Today" else state.selectedDate.format(DateTimeFormatter.ofPattern("EEEE")),
                            style = KaloTypography.titleLarge,
                            color = KaloTextPrimary
                        )
                        TextButton(
                            onClick = ::openDatePicker,
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                            modifier = Modifier.heightIn(min = 40.dp)
                        ) {
                            Text(
                                state.selectedDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy")),
                                style = KaloTypography.bodyLarge,
                                color = KaloAccent
                            )
                        }
                    }
                    IconButton(onClick = viewModel::goToPreviousDay, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.ArrowBack, "Previous day", tint = KaloTextPrimary)
                    }
                    IconButton(onClick = viewModel::goToNextDay, enabled = !state.isToday, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.ArrowForward, "Next day")
                    }
                    if (!state.isToday) {
                        FilledTonalButton(
                            onClick = viewModel::goToToday,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = KaloSurfaceElevated,
                                contentColor = KaloTextPrimary
                            )
                        ) { Text("Today") }
                    }
                }
            }
            if (state.needsBodyDetails && state.isToday) item {
                Surface(onClick = onNavigateToSettings, shape = RoundedCornerShape(20.dp), color = KaloSurface, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Set targets for your body", style = KaloTypography.titleMedium, color = KaloTextPrimary)
                        Text(
                            "Add your height, weight, age and activity to get calorie and protein targets that fit you.",
                            style = KaloTypography.bodyMedium,
                            color = KaloTextSecondary
                        )
                        Text("Add details", style = KaloTypography.labelLarge, color = KaloAccent)
                    }
                }
            }
            item {
                MacroSummaryCard(
                    state.currentCalories, state.targetCalories,
                    state.proteinGrams, state.targetProtein,
                    state.carbsGrams, state.targetCarbs,
                    state.fatGrams, state.targetFat
                )
            }
            item {
                DailyMovementHabitCard(
                    todayWorkouts = state.todayWorkouts,
                    lastWorkout = state.lastWorkout,
                    defaultMinutes = defaultMinutes,
                    defaultPushups = defaultPushups,
                    defaultSitups = defaultSitups,
                    defaultCrunches = defaultCrunches,
                    isBusy = state.isLoggingWorkout,
                    onToggleHabit = { name, reps ->
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        viewModel.toggleDailyHabitExercise(name, reps, defaultMinutes)
                    },
                    onLogAllHabits = {
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        viewModel.quickLogDailyFitness(defaultMinutes, listOf(defaultPushups, defaultSitups, defaultCrunches))
                    },
                    onRepeatLastWorkout = { workout ->
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        viewModel.logWorkoutAgain(workout)
                    },
                    onOpenQuickFitness = onNavigateToWorkout,
                    onOpenDetailedWorkout = onNavigateToDetailedWorkout
                )
            }
            if (state.todayWorkouts.isNotEmpty()) {
                items(state.todayWorkouts, key = { it.workout.id }) { workout ->
                    WorkoutItemCard(
                        workout,
                        onEdit = { onNavigateToEditWorkout(workout.workout.id) },
                        onDelete = { viewModel.deleteWorkout(workout) },
                        onLogAgain = { viewModel.logWorkoutAgain(workout) },
                        canLogAgain = !state.isLoggingWorkout
                    )
                }
            }
            state.dailyInsight?.let { insight -> item { DailyInsightCard(insight) } }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Your meals", style = KaloTypography.titleMedium, modifier = Modifier.weight(1f), color = KaloTextPrimary)
                    TextButton(onClick = onViewMeals) { Text("View all") }
                }
            }
            if (state.todayMeals.isEmpty()) item {
                Surface(
                    onClick = onAddMeal,
                    shape = RoundedCornerShape(20.dp),
                    color = KaloSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            if (state.isToday) "Your day starts here" else "No meals logged for this day",
                            style = KaloTypography.titleMedium,
                            color = KaloTextPrimary
                        )
                        Text(
                            "Add a meal by photo, barcode, search or recent entry. Nothing is saved until you confirm.",
                            style = KaloTypography.bodyMedium,
                            color = KaloTextSecondary
                        )
                        Text("Add a meal →", style = KaloTypography.labelLarge, color = KaloAccent)
                    }
                }
            }
            items(state.todayMeals, key = { it.meal.id }) { meal ->
                MealItemCard(
                    meal,
                    onDelete = { viewModel.deleteMeal(meal) },
                    onEdit = { onNavigateToEditMeal(meal.meal.id) },
                    onLogAgain = { viewModel.logMealAgain(meal) },
                    canLogAgain = !state.isRepeatingMeal
                )
            }
            item {
                WaterIntakeCard(
                    state.currentWaterMl,
                    state.targetWaterMl,
                    viewModel::logWater,
                    viewModel::undoLastWaterLog,
                    onEditTarget = if (state.isToday) onNavigateToSettings else null,
                    entryDate = state.selectedDate.toString()
                )
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Steps & activity", style = KaloTypography.titleMedium, modifier = Modifier.weight(1f), color = KaloTextPrimary)
                    TextButton(onClick = onViewWorkouts) { Text("All workouts") }
                }
            }
            item {
                StepGaugeCard(
                    state.healthData.steps,
                    state.targetSteps,
                    state.healthData.activeCaloriesBurned,
                    state.healthData.syncSource,
                    onNavigateToHealthPermissions,
                    state.healthData.isConnected,
                    readingState = state.healthData.stepState,
                    onRetry = { viewModel.refreshHealthData() }
                )
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(KaloSurface, RoundedCornerShape(20.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (state.dayComplete) "Day complete" else if (state.isToday) "Today's log is open" else "This day's log is open",
                                style = KaloTypography.titleMedium,
                                color = KaloTextPrimary
                            )
                            val statusLine = buildString {
                                append("${state.todayMeals.size} ${if (state.todayMeals.size == 1) "meal" else "meals"} · ")
                                append(if (state.todayWorkouts.isEmpty()) "No exercise logged yet" else "${state.todayWorkouts.size} ${if (state.todayWorkouts.size == 1) "workout" else "workouts"}")
                            }
                            Text(statusLine, style = KaloTypography.bodyMedium, color = if (state.todayWorkouts.isEmpty()) KaloAccent else KaloProtein)
                            Text("You can still edit a completed day.", style = KaloTypography.bodyMedium, color = KaloTextSecondary)
                        }
                        Spacer(Modifier.width(12.dp))
                        FilledTonalButton(
                            onClick = {
                                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                viewModel.setDayComplete(!state.dayComplete)
                            },
                            modifier = Modifier.heightIn(min = 48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (state.dayComplete) KaloSurfaceElevated else KaloAccent,
                                contentColor = if (state.dayComplete) KaloTextPrimary else KaloOnAccent
                            )
                        ) {
                            Text(if (state.dayComplete) "Reopen day" else "Mark day complete")
                        }
                    }
                    if (state.isToday) {
                        Text(
                            "Today stays out of complete-day averages, even when marked complete.",
                            style = KaloTypography.bodyMedium,
                            color = KaloTextSecondary
                        )
                    }
                    TextButton(
                        onClick = { averagesExpanded = !averagesExpanded },
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                        modifier = Modifier.heightIn(min = 40.dp)
                    ) {
                        Text(if (averagesExpanded) "Hide averages explanation" else "How averages work")
                    }
                    if (averagesExpanded) {
                        Text(
                            "Only completed past days are included in complete-day averages. Unfinished days and today are excluded.",
                            style = KaloTypography.bodyMedium,
                            color = KaloTextSecondary
                        )
                    }
                }
            }
        }
    }
}

/** One quiet observation from the day's log; no icon or alarm styling. */
@Composable
fun DailyInsightCard(
    insight: NutritionInsight,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(KaloSurface, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(insight.title, style = KaloTypography.titleMedium, color = KaloTextPrimary)
        Text(insight.description, style = KaloTypography.bodyMedium, color = KaloTextSecondary)
        Text("Based on your log", style = KaloTypography.labelSmall, color = KaloTextSecondary)
    }
}

/** Where a meal's numbers came from, inferred from what was saved with it. */
internal fun mealSource(meal: MealEntity): String? = when {
    meal.notes?.contains("Barcode:") == true -> "Barcode"
    meal.imageLocalUri != null || meal.notes?.startsWith("From photo") == true ||
        meal.notes?.startsWith("AI estimate") == true -> "Photo estimate"
    else -> null
}

@Composable
fun MealItemCard(
    mealWithItems: MealWithItems,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onLogAgain: () -> Unit,
    canLogAgain: Boolean = true,
    modifier: Modifier = Modifier
) {
    val meal = mealWithItems.meal
    val timeText = java.time.Instant.ofEpochMilli(meal.timestamp)
        .atZone(java.time.ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm"))
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(KaloSurface)
            .clickable(onClick = onEdit)
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val photo = meal.imageLocalUri?.let(::File)?.takeIf { it.exists() }
        if (photo != null) {
            AsyncImage(
                model = photo,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(KaloSurfaceElevated)
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(meal.title, style = KaloTypography.titleMedium, color = KaloTextPrimary)
            Text(
                listOfNotNull(timeText, mealSource(meal)).joinToString(" · "),
                style = KaloTypography.bodyMedium,
                color = KaloTextSecondary
            )
            Text(
                "P ${meal.totalProteinGrams.roundToInt()} g  ·  C ${meal.totalCarbsGrams.roundToInt()} g  ·  F ${meal.totalFatGrams.roundToInt()} g",
                style = KaloTypography.bodyMedium.copy(fontFeatureSettings = TabularFontFeature),
                color = KaloTextSecondary
            )
        }
        Text(
            "${meal.totalCalories} kcal",
            style = KaloTypography.titleMedium.copy(fontFeatureSettings = TabularFontFeature),
            color = KaloTextPrimary
        )
        Box {
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = "More for ${meal.title}", tint = KaloTextSecondary)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("Edit") }, onClick = { menuOpen = false; onEdit() })
                DropdownMenuItem(text = { Text("Log again") }, enabled = canLogAgain, onClick = { menuOpen = false; onLogAgain() })
                DropdownMenuItem(text = { Text("Delete") }, onClick = { menuOpen = false; onDelete() })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyMovementHabitCard(
    todayWorkouts: List<WorkoutWithSets>,
    lastWorkout: WorkoutWithSets?,
    defaultMinutes: Int,
    defaultPushups: Int,
    defaultSitups: Int,
    defaultCrunches: Int,
    isBusy: Boolean,
    onToggleHabit: (String, Int) -> Unit,
    onLogAllHabits: () -> Unit,
    onRepeatLastWorkout: (WorkoutWithSets) -> Unit,
    onOpenQuickFitness: () -> Unit,
    onOpenDetailedWorkout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val completedSets = todayWorkouts.flatMap { it.sets }.filter { it.isCompleted }
    val pushupReps = completedSets.filter { it.exerciseName.equals("Push-ups", ignoreCase = true) }.sumOf { it.reps }
    val situpReps = completedSets.filter { it.exerciseName.equals("Sit-ups", ignoreCase = true) }.sumOf { it.reps }
    val crunchReps = completedSets.filter { it.exerciseName.equals("Crunches", ignoreCase = true) }.sumOf { it.reps }
    val allThreeChecked = pushupReps > 0 && situpReps > 0 && crunchReps > 0
    val totalWorkoutMinutes = todayWorkouts.sumOf { it.workout.durationMinutes }
    val totalWorkoutCalories = todayWorkouts.sumOf { it.workout.estimatedCaloriesBurned }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(KaloSurface, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = if (todayWorkouts.isNotEmpty()) KaloProtein else KaloAccent,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text("Exercise & daily habits", style = KaloTypography.titleMedium, color = KaloTextPrimary)
                    Text(
                        if (todayWorkouts.isEmpty()) "Tap a habit chip to check it off in 1 tap"
                        else "$totalWorkoutMinutes min logged · ~$totalWorkoutCalories kcal used",
                        style = KaloTypography.bodyMedium,
                        color = if (todayWorkouts.isEmpty()) KaloTextSecondary else KaloProtein
                    )
                }
            }
            TextButton(onClick = onOpenQuickFitness, enabled = !isBusy) {
                Text(if (todayWorkouts.any { it.workout.title.equals("Daily fitness", ignoreCase = true) }) "Adjust" else "Customize")
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val habits = listOf(
                Triple("Push-ups", defaultPushups, pushupReps),
                Triple("Sit-ups", defaultSitups, situpReps),
                Triple("Crunches", defaultCrunches, crunchReps)
            )
            habits.forEach { (name, defaultCount, loggedCount) ->
                val checked = loggedCount > 0
                val displayCount = if (checked) loggedCount else defaultCount
                FilterChip(
                    selected = checked,
                    onClick = { onToggleHabit(name, defaultCount) },
                    enabled = !isBusy,
                    leadingIcon = if (checked) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    label = {
                        Text(
                            "$name · $displayCount",
                            style = KaloTypography.labelLarge.copy(fontFeatureSettings = TabularFontFeature)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = KaloProtein.copy(alpha = 0.2f),
                        selectedLabelColor = KaloTextPrimary,
                        selectedLeadingIconColor = KaloProtein,
                        containerColor = KaloSurfaceElevated,
                        labelColor = KaloTextPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = !isBusy,
                        selected = checked,
                        borderColor = KaloBorder,
                        selectedBorderColor = KaloProtein
                    ),
                    modifier = Modifier.heightIn(min = 44.dp)
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!allThreeChecked) {
                FilledTonalButton(
                    onClick = onLogAllHabits,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = KaloProtein,
                        contentColor = KaloBackground
                    ),
                    modifier = Modifier.heightIn(min = 44.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Check all 3 ($defaultMinutes min)")
                }
            }
            if (lastWorkout != null && todayWorkouts.isEmpty()) {
                FilledTonalButton(
                    onClick = { onRepeatLastWorkout(lastWorkout) },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = KaloSurfaceElevated,
                        contentColor = KaloTextPrimary
                    ),
                    modifier = Modifier.heightIn(min = 44.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = KaloAccent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Repeat ${lastWorkout.workout.title}")
                }
            }
            OutlinedButton(
                onClick = onOpenDetailedWorkout,
                enabled = !isBusy,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.heightIn(min = 44.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Gym / Cardio session")
            }
        }
    }
}

@Composable
fun WorkoutItemCard(
    workoutWithSets: WorkoutWithSets,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onLogAgain: (() -> Unit)? = null,
    canLogAgain: Boolean = true
) {
    val workout = workoutWithSets.workout
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(KaloSurface)
            .clickable(onClick = onEdit)
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(workout.title, style = KaloTypography.titleMedium, color = KaloTextPrimary)
            Text(
                "${workout.durationMinutes} min · about ${workout.estimatedCaloriesBurned} kcal used",
                style = KaloTypography.bodyMedium,
                color = KaloTextSecondary
            )
            if (workoutWithSets.sets.isNotEmpty()) {
                Text(
                    workoutWithSets.sets.groupBy { it.exerciseName }.entries.joinToString("  ·  ") { (name, sets) ->
                        "$name " + sets.joinToString(", ") { s ->
                            if (s.weightKg > 0f) "${formatKg(s.weightKg)} kg × ${s.reps}" else "${s.reps}"
                        }
                    },
                    style = KaloTypography.bodyMedium,
                    color = KaloTextSecondary
                )
            }
        }
        Box {
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = "More for ${workout.title}", tint = KaloTextSecondary)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("Edit") }, onClick = { menuOpen = false; onEdit() })
                if (onLogAgain != null) {
                    DropdownMenuItem(text = { Text("Log again") }, enabled = canLogAgain, onClick = { menuOpen = false; onLogAgain() })
                }
                DropdownMenuItem(text = { Text("Delete") }, onClick = { menuOpen = false; onDelete() })
            }
        }
    }
}

private fun formatKg(kg: Float): String = if (kg % 1f == 0f) kg.toInt().toString() else kg.toString()
