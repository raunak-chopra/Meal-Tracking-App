package com.kalotracker.app.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kalotracker.app.core.designsystem.*
import com.kalotracker.app.core.designsystem.components.StepGaugeCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkoutsHubScreen(
    viewModel: DashboardViewModel,
    fitnessDefaults: List<String>,
    onQuickFitness: () -> Unit,
    onDetailedWorkout: () -> Unit,
    onEditWorkout: (String) -> Unit,
    onNavigateToHealthPermissions: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.pendingUndo) {
        state.pendingUndo?.let {
            if (snackbar.showSnackbar(it.message, "Undo") == SnackbarResult.ActionPerformed) viewModel.undoDelete()
            else viewModel.dismissUndo()
        }
    }
    LaunchedEffect(state.repeatMessage) {
        state.repeatMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissRepeatMessage()
        }
    }

    fun openDatePicker() {
        val date = state.selectedDate
        android.app.DatePickerDialog(context, { _, year, month, day ->
            viewModel.loadDataForDate(LocalDate.of(year, month + 1, day))
        }, date.year, date.monthValue - 1, date.dayOfMonth).apply {
            datePicker.maxDate = System.currentTimeMillis()
        }.show()
    }

    val defaultMinutes = fitnessDefaults.getOrNull(0)?.toIntOrNull()?.coerceIn(3, 60) ?: 7
    val defaultPushups = fitnessDefaults.getOrNull(3)?.toIntOrNull()?.takeIf { it > 0 } ?: 20
    val defaultSitups = fitnessDefaults.getOrNull(4)?.toIntOrNull()?.takeIf { it > 0 } ?: 20
    val defaultCrunches = fitnessDefaults.getOrNull(5)?.toIntOrNull()?.takeIf { it > 0 } ?: 20

    Scaffold(
        containerColor = KaloBackground,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 20.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Workouts", style = KaloTypography.headlineMedium, color = KaloTextPrimary)
                    FilledTonalButton(
                        onClick = onDetailedWorkout,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = KaloSurfaceElevated,
                            contentColor = KaloTextPrimary
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = KaloProtein, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Gym / Cardio")
                    }
                }
            }

            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(KaloSurface, RoundedCornerShape(18.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = viewModel::goToPreviousDay, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Previous day", tint = KaloTextPrimary)
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            if (state.isToday) "Today" else state.selectedDate.format(DateTimeFormatter.ofPattern("EEEE")),
                            style = KaloTypography.labelMedium,
                            color = KaloTextSecondary
                        )
                        TextButton(
                            onClick = ::openDatePicker,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text(
                                state.selectedDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy")),
                                style = KaloTypography.titleMedium,
                                color = KaloAccent
                            )
                        }
                    }
                    IconButton(onClick = viewModel::goToNextDay, enabled = !state.isToday, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Next day")
                    }
                    if (!state.isToday) {
                        TextButton(onClick = viewModel::goToToday) { Text("Today") }
                    }
                }
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
                        viewModel.toggleDailyHabitExercise(name, reps, defaultMinutes)
                    },
                    onLogAllHabits = {
                        viewModel.quickLogDailyFitness(defaultMinutes, listOf(defaultPushups, defaultSitups, defaultCrunches))
                    },
                    onRepeatLastWorkout = { workout ->
                        viewModel.logWorkoutAgain(workout)
                    },
                    onOpenQuickFitness = onQuickFitness,
                    onOpenDetailedWorkout = onDetailedWorkout
                )
            }

            if (state.routines.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(KaloSurface, RoundedCornerShape(20.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Saved routines · 1-tap log", style = KaloTypography.titleMedium, color = KaloTextPrimary)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.routines.forEach { routine ->
                                FilledTonalButton(
                                    onClick = { viewModel.quickLogRoutine(routine) },
                                    enabled = !state.isLoggingWorkout,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = KaloSurfaceElevated,
                                        contentColor = KaloTextPrimary
                                    )
                                ) {
                                    Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = KaloProtein, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Log ${routine.name}")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (state.todayWorkouts.isEmpty()) "Sessions logged"
                        else "${state.todayWorkouts.size} ${if (state.todayWorkouts.size == 1) "session" else "sessions"} · ${state.todayWorkouts.sumOf { it.workout.durationMinutes }} min",
                        style = KaloTypography.titleMedium,
                        color = KaloTextPrimary
                    )
                    if (state.todayWorkouts.isNotEmpty()) {
                        Text(
                            "~${state.todayWorkouts.sumOf { it.workout.estimatedCaloriesBurned }} kcal used",
                            style = KaloTypography.bodyMedium.copy(fontFeatureSettings = TabularFontFeature),
                            color = KaloProtein
                        )
                    }
                }
            }

            if (state.todayWorkouts.isEmpty()) {
                item {
                    Surface(
                        onClick = onQuickFitness,
                        shape = RoundedCornerShape(20.dp),
                        color = KaloSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("No workout logged for this day yet", style = KaloTypography.titleMedium, color = KaloTextPrimary)
                            Text(
                                "Check off an exercise chip above in one tap, repeat your last session, or open the workout logger.",
                                style = KaloTypography.bodyMedium,
                                color = KaloTextSecondary
                            )
                        }
                    }
                }
            } else {
                items(state.todayWorkouts, key = { it.workout.id }) { workout ->
                    WorkoutItemCard(
                        workout,
                        onEdit = { onEditWorkout(workout.workout.id) },
                        onDelete = { viewModel.deleteWorkout(workout) },
                        onLogAgain = { viewModel.logWorkoutAgain(workout) },
                        canLogAgain = !state.isLoggingWorkout
                    )
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
        }
    }
}
