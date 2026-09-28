package com.kalotracker.app.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val dateFormatted = state.selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = KaloBackground,
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick Workout Trigger
                FloatingActionButton(
                    onClick = onNavigateToWorkout,
                    containerColor = KaloSurfaceElevated,
                    contentColor = KaloTextPrimary,
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = "Log Workout",
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Quick Manual Food Trigger
                FloatingActionButton(
                    onClick = onNavigateToManualMeal,
                    containerColor = KaloSurfaceElevated,
                    contentColor = KaloProtein,
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = "Manual Food",
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Quick Barcode Scanner Trigger
                FloatingActionButton(
                    onClick = onNavigateToBarcode,
                    containerColor = KaloSurfaceElevated,
                    contentColor = KaloCarbs,
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CropFree,
                        contentDescription = "Scan Barcode",
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Quick AI Meal Scan Trigger (Primary)
                ExtendedFloatingActionButton(
                    onClick = onNavigateToCamera,
                    containerColor = KaloTextPrimary,
                    contentColor = KaloBackground,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Scan Meal",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Snap Meal",
                        style = KaloTypography.titleMedium
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 100.dp)
        ) {
            // Header with Date Stepper & Settings Icon
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (state.isToday) "TODAY'S HORIZON" else "HISTORICAL HORIZON",
                            style = KaloTypography.labelSmall,
                            color = KaloTextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = dateFormatted,
                            style = KaloTypography.headlineMedium,
                            color = KaloTextPrimary
                        )
                    }

                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(KaloSurfaceElevated)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings & Goals",
                            tint = KaloTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Date Navigation Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(KaloSurface, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.goToPreviousDay() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Previous Day",
                            tint = KaloTextPrimary
                        )
                    }

                    Text(
                        text = if (state.isToday) "Today" else state.selectedDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                        style = KaloTypography.titleMedium,
                        color = if (state.isToday) KaloProtein else KaloTextPrimary
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!state.isToday) {
                            TextButton(
                                onClick = { viewModel.goToToday() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Jump to Today", style = KaloTypography.labelSmall, color = KaloProtein)
                            }
                        }

                        IconButton(
                            onClick = { viewModel.goToNextDay() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Next Day",
                                tint = KaloTextPrimary
                            )
                        }
                    }
                }
            }

            // AI Nutrition Insight Card
            state.dailyInsight?.let { insight ->
                item {
                    DailyInsightCard(insight = insight)
                }
            }

            // Macro Concentric Rings & Remaining Energy
            item {
                MacroSummaryCard(
                    currentCalories = state.currentCalories,
                    targetCalories = state.targetCalories,
                    proteinGrams = state.proteinGrams,
                    targetProtein = state.targetProtein,
                    carbsGrams = state.carbsGrams,
                    targetCarbs = state.targetCarbs,
                    fatGrams = state.fatGrams,
                    targetFat = state.targetFat
                )
            }

            // Health Connect Steps Gauge
            item {
                StepGaugeCard(
                    currentSteps = state.healthData.steps,
                    stepGoal = state.targetSteps,
                    activeCaloriesBurned = state.healthData.activeCaloriesBurned,
                    syncSource = state.healthData.syncSource,
                    isHealthConnected = state.healthData.isConnected,
                    onConnectHealthClick = onNavigateToHealthPermissions
                )
            }

            // Hydration Tracker
            item {
                WaterIntakeCard(
                    currentWaterMl = state.currentWaterMl,
                    targetWaterMl = state.targetWaterMl,
                    onAddWater = { ml -> viewModel.logWater(ml) },
                    onUndoWater = { viewModel.undoLastWaterLog() }
                )
            }

            // Activity Stream Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTIVITY STREAM",
                        style = KaloTypography.labelSmall,
                        color = KaloTextSecondary
                    )
                    Text(
                        text = "${state.todayMeals.size} meals • ${state.todayWorkouts.size} workouts",
                        style = KaloTypography.labelSmall,
                        color = KaloTextMuted
                    )
                }
            }

            // Empty State
            if (state.todayMeals.isEmpty() && state.todayWorkouts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(KaloSurface, RoundedCornerShape(16.dp))
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No entries logged for this day",
                                style = KaloTypography.titleMedium,
                                color = KaloTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Snap a photo of your meal or log a workout",
                                style = KaloTypography.bodyMedium,
                                color = KaloTextMuted
                            )
                        }
                    }
                }
            }

            // Meals Stream
            items(state.todayMeals) { mealWithItems ->
                MealItemCard(
                    mealWithItems = mealWithItems,
                    onDelete = { viewModel.deleteMeal(mealWithItems.meal) }
                )
            }

            // Workouts Stream
            items(state.todayWorkouts) { workoutWithSets ->
                WorkoutItemCard(
                    workoutWithSets = workoutWithSets,
                    onDelete = { viewModel.deleteWorkout(workoutWithSets.workout) }
                )
            }
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
    modifier: Modifier = Modifier
) {
    val meal = mealWithItems.meal
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
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(KaloSurfaceElevated)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = meal.title,
                            style = KaloTypography.titleMedium,
                            color = KaloTextPrimary
                        )
                        Text(
                            text = "${meal.totalCalories} kcal",
                            style = KaloTypography.bodyLarge,
                            color = KaloCalories
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
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
                text = "${meal.totalProteinGrams.toInt()}g P • ${meal.totalCarbsGrams.toInt()}g C • ${meal.totalFatGrams.toInt()}g F",
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
                        text = "${workout.estimatedCaloriesBurned} kcal burned • ${workout.durationMinutes} min",
                        style = KaloTypography.bodyMedium,
                        color = KaloSteps
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
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
                val setsSummary = workoutWithSets.sets.joinToString(" | ") { "Set ${it.setNumber}: ${it.weightKg.toInt()}kg × ${it.reps}" }
                Text(
                    text = setsSummary,
                    style = KaloTypography.bodyMedium,
                    color = KaloTextSecondary
                )
            }
        }
    }
}
