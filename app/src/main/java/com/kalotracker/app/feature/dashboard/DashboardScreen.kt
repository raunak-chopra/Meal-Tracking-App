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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lightbulb
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
    onViewMeals: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val lifecycleOwner=LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner,viewModel) {
        val observer=LifecycleEventObserver { _,event -> if(event==Lifecycle.Event.ON_RESUME) viewModel.onResume() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val snackbar=remember { SnackbarHostState() }
    LaunchedEffect(state.pendingUndo) {
        state.pendingUndo?.let { undo ->
            if(snackbar.showSnackbar(undo.message,"Undo",duration=SnackbarDuration.Long)==SnackbarResult.ActionPerformed) viewModel.undoDelete()
            else viewModel.dismissUndo()
        }
    }
    LaunchedEffect(state.repeatMessage) { state.repeatMessage?.let { snackbar.showSnackbar(it);viewModel.dismissRepeatMessage() } }
    Scaffold(modifier=modifier.fillMaxSize(),containerColor=KaloBackground,
        contentWindowInsets=WindowInsets(0,0,0,0),snackbarHost={SnackbarHost(snackbar)},
        topBar={ Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal=20.dp,vertical=12.dp),
            verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            com.kalotracker.app.core.designsystem.components.BrandMark()
            Text("Today",style=KaloTypography.titleLarge,modifier=Modifier.weight(1f),color=KaloTextPrimary)
            IconButton(onClick=onNavigateToSettings,modifier=Modifier.size(48.dp)) { Icon(Icons.Default.Settings,"Settings",tint=KaloTextPrimary) }
        } }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal=20.dp),
            verticalArrangement=Arrangement.spacedBy(24.dp),contentPadding=PaddingValues(bottom=24.dp)) {
            item { Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text(state.selectedDate.format(DateTimeFormatter.ofPattern("EEE, d MMMM")),Modifier.weight(1f),color=KaloTextSecondary)
                IconButton(onClick=viewModel::goToPreviousDay,modifier=Modifier.size(48.dp)) { Icon(Icons.Default.ArrowBack,"Previous day") }
                IconButton(onClick=viewModel::goToNextDay,modifier=Modifier.size(48.dp)) { Icon(Icons.Default.ArrowForward,"Next day") }
                if(!state.isToday) TextButton(onClick=viewModel::goToToday) { Text("Today") }
            } }
            item { MacroSummaryCard(state.currentCalories,state.targetCalories,state.proteinGrams,state.targetProtein,
                state.carbsGrams,state.targetCarbs,state.fatGrams,state.targetFat) }
            item { Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text("Your meals",style=KaloTypography.titleMedium,modifier=Modifier.weight(1f),color=KaloTextPrimary)
                TextButton(onClick=onViewMeals) { Text("View all") }
            } }
            if(state.todayMeals.isEmpty()) item { Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("Your day starts here",style=KaloTypography.titleMedium,color=KaloTextPrimary)
                Text("Add a meal by photo, barcode, search or recent entry. Nothing is saved until you confirm.",color=KaloTextSecondary)
                TextButton(onClick=onAddMeal) { Text("Add your first meal") }
            } }
            items(state.todayMeals,key={it.meal.id}) { meal -> MealItemCard(meal,
                onDelete={viewModel.deleteMeal(meal)},onEdit={onNavigateToEditMeal(meal.meal.id)},
                onLogAgain={viewModel.logMealAgain(meal)},canLogAgain=!state.isRepeatingMeal) }
            item { WaterIntakeCard(state.currentWaterMl,state.targetWaterMl,viewModel::logWater,viewModel::undoLastWaterLog) }
            item { StepGaugeCard(state.healthData.steps,state.targetSteps,state.healthData.activeCaloriesBurned,
                state.healthData.syncSource,onNavigateToHealthPermissions,state.healthData.isConnected) }
            item { Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text("Workouts",style=KaloTypography.titleMedium,modifier=Modifier.weight(1f),color=KaloTextPrimary)
                TextButton(onClick=onNavigateToWorkout) { Text("Log daily fitness") }
            } }
            items(state.todayWorkouts,key={it.workout.id}) { workout -> WorkoutItemCard(workout,
                onEdit={onNavigateToEditWorkout(workout.workout.id)},onDelete={viewModel.deleteWorkout(workout)}) }
            item { Column {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text(if(state.dayComplete) "Day complete" else "Partial day",Modifier.weight(1f),color=KaloTextPrimary)
                    Switch(state.dayComplete,viewModel::setDayComplete)
                }
                Text("Mark complete after logging everything. Today and unfinished days stay out of complete-day averages.",color=KaloTextSecondary)
            } }
            state.dailyInsight?.let { insight -> item { DailyInsightCard(insight) } }
        }
    }
}

@Composable
fun DailyInsightCard(
    insight: NutritionInsight,
    modifier: Modifier = Modifier
) {
    val accent = when (insight.type) {
        InsightType.PROTEIN -> KaloProtein
        InsightType.WORKOUT_RECOVERY -> KaloSteps
        InsightType.ENERGY -> KaloCarbs
        InsightType.HYDRATION_OR_BALANCE -> KaloTextSecondary
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(KaloSurfaceElevated, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = insight.title.uppercase(),
                    style = KaloTypography.labelSmall,
                    color = accent
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = insight.description,
                    style = KaloTypography.bodyMedium,
                    color = KaloTextPrimary
                )
            }
        }
    }
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min=88.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(KaloSurface)
            .clickable(onClick = onEdit)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Local photo thumbnail if available
                    if (!meal.imageLocalUri.isNullOrBlank()) {
                        val file = File(meal.imageLocalUri)
                        if (file.exists()) {
                            AsyncImage(
                                model = file,
                                contentDescription = meal.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(KaloSurfaceElevated)
                            )
                        }
                    }

                    Column(Modifier.weight(1f)) {
                        Text(
                            text = meal.title,
                            style = KaloTypography.titleMedium,
                            color = KaloTextPrimary
                        )
                        Text(
                            text = "${meal.totalCalories} kcal  ·  $timeText",
                            style = KaloTypography.bodyLarge,
                            color = KaloCalories
                        )
                    }
                }

                IconButton(
                    onClick = onLogAgain,
                    enabled = canLogAgain,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Log this meal again",
                        tint = KaloTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Meal",
                        tint = KaloTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${meal.totalProteinGrams.toInt()}g P · ${meal.totalCarbsGrams.toInt()}g C · ${meal.totalFatGrams.toInt()}g F",
                style = KaloTypography.bodyMedium,
                color = KaloTextSecondary
            )

            if (mealWithItems.items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                val itemsSummary = mealWithItems.items.joinToString(", ") { "${it.name} (${it.portionGrams.toInt()}g)" }
                Text(
                    text = itemsSummary,
                    style = KaloTypography.bodyMedium,
                    color = KaloTextMuted
                )
            }
        }
    }
}

@Composable
fun WorkoutItemCard(
    workoutWithSets: WorkoutWithSets,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val workout = workoutWithSets.workout
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(KaloSurface, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = workout.title,
                        style = KaloTypography.titleMedium,
                        color = KaloTextPrimary
                    )
                    Text(
                        text = "${workout.estimatedCaloriesBurned} kcal estimated burn · ${workout.durationMinutes} min",
                        style = KaloTypography.bodyMedium,
                        color = KaloSteps
                    )
                }

                TextButton(onClick = onEdit) { Text("Edit") }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Workout",
                        tint = KaloTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (workoutWithSets.sets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                val setsSummary = workoutWithSets.sets.joinToString(" | ") { "Set ${it.setNumber}: ${it.weightKg.toInt()}kg Ã— ${it.reps}" }
                Text(
                    text = setsSummary,
                    style = KaloTypography.bodyMedium,
                    color = KaloTextSecondary
                )
            }
        }
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        containerColor = KaloSurfaceElevated,
        contentColor = tint,
        shape = RoundedCornerShape(20.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = KaloTypography.titleMedium, color = KaloTextPrimary)
    }
}
