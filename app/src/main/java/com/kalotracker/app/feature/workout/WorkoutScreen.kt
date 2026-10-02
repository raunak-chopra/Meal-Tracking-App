package com.kalotracker.app.feature.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kalotracker.app.core.designsystem.*
import com.kalotracker.app.core.designsystem.components.DateTimeChip
import com.kalotracker.app.core.designsystem.components.KaloButton

@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel,
    onClose: () -> Unit,
    onWorkoutSaved: () -> Unit,
    modifier: Modifier = Modifier,
    quickFitness: Boolean = false,
    fitnessDefaults: List<String> = listOf("7", "", "MODERATE", "20", "20", "20"),
    onFitnessDefaultsSaved: (List<String>) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    androidx.activity.compose.BackHandler(enabled = state.isSaving) {}
    val routines by viewModel.routines.collectAsState()
    var detailed by rememberSaveable { mutableStateOf(!quickFitness) }
    val quickStateHolder = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    if (!detailed) {
        quickStateHolder.SaveableStateProvider("quickFitness") {
        DailyFitnessScreen(state, fitnessDefaults, onClose, { detailed = true }, viewModel::setTimestamp) { minutes, weight, effort, reps ->
            viewModel.saveDailyFitness(minutes, weight, effort, reps) {
                onFitnessDefaultsSaved(listOf(minutes, weight, effort.name) + reps)
                onWorkoutSaved()
            }
        }
        }
        return
    }
    var fitnessMinutes by rememberSaveable { mutableStateOf("5") }
    var fitnessWeight by rememberSaveable { mutableStateOf("70") }
    val totalCalories = state.otherExercises.sumOf { it.calories } +
        if (state.exerciseName.isBlank()) 0 else (state.estimatedCalories.toIntOrNull() ?: 0)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = KaloBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(KaloSurfaceElevated)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = KaloTextPrimary
                    )
                }

                Text(
                    text = "LOG SESSION",
                    style = KaloTypography.labelSmall,
                    color = KaloTextSecondary
                )

                Box(modifier = Modifier.size(48.dp))
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(20.dp)
            ) {
                KaloButton(
                    text = "Complete Workout (~$totalCalories kcal)",
                    onClick = { viewModel.saveWorkout(onWorkoutSaved) },
                    loading = state.isSaving,
                    enabled = !state.isSaving && !state.isLoading
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            state.errorMessage?.let { message ->
                item { Text(message, color = MaterialTheme.colorScheme.error) }
            }
            if (quickFitness) item { TextButton(onClick = { detailed = false }, enabled = !state.isSaving) { Text("Back to quick daily fitness") } }
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = KaloSurfaceElevated) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("DAILY FITNESS", style = KaloTypography.labelSmall, color = KaloProtein)
                        Text("20 push-ups · 20 sit-ups · 20 crunches", color = KaloTextPrimary)
                        Text("A quick 5–10 minute log. Change reps or remove exercises below after loading.", color = KaloTextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(fitnessMinutes, { fitnessMinutes = it },
                                label = { Text("Total minutes") }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            OutlinedTextField(fitnessWeight, { fitnessWeight = it },
                                label = { Text("Body weight (kg)") }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                        }
                        Text("Rough calorie estimate from time and body weight, including resting energy. Rest and pace affect the result. 70 kg is an example; enter your weight.", color = KaloTextSecondary, style = KaloTypography.bodyMedium)
                        OutlinedButton(onClick = { viewModel.loadDailyFitness(fitnessMinutes, fitnessWeight) },
                            enabled = !state.isSaving && !state.isLoading) {
                            Text(if (state.otherExercises.isNotEmpty() || state.exerciseName.isNotBlank())
                                "Replace draft with daily fitness" else "Load daily fitness")
                        }
                    }
                }
            }
            item { OutlinedTextField(state.sessionTitle, viewModel::setSessionTitle, label = { Text("Session / routine name") }, modifier = Modifier.fillMaxWidth()) }
            item { Row {
                TextButton(onClick = viewModel::saveRoutine, enabled = !state.isSaving) { Text("Save routine") }
                TextButton(onClick = { viewModel.startRest() }) { Text("90s rest") }
                if (state.restSeconds > 0) TextButton(onClick = viewModel::stopRest) { Text("${state.restSeconds}s Ã‚Â· Stop") }
            } }
            items(routines, key = { "routine" + it.id }) { routine -> Row {
                TextButton(onClick = { viewModel.loadRoutine(routine) }, enabled = !state.isSaving) { Text("Load ${routine.name}") }
                TextButton(onClick = { viewModel.deleteRoutine(routine) }, enabled = !state.isSaving) { Text("Delete") }
            } }
            itemsIndexed(state.otherExercises) { index, row -> Row {
                Text("${row.name}: ${row.sets.count { it.isCompleted }}/${row.sets.size} sets, ${row.durationMinutes} min", modifier = Modifier.weight(1f))
                TextButton(onClick = { viewModel.editSessionExercise(index) }) { Text("Edit") }
                TextButton(onClick = { viewModel.removeSessionExercise(index) }) { Text("Remove") }
            } }
            item { Text("Add each exercise below to the session before starting another. Duration and burn are per exercise; the session sums them.") }
            // Exercise Quick Chips
            item {
                Column {
                    Text(
                        text = "POPULAR EXERCISES",
                        style = KaloTypography.labelSmall,
                        color = KaloTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(viewModel.commonExercises) { exercise ->
                            val isSelected = exercise == state.exerciseName
                            Surface(
                                onClick = { viewModel.selectExercise(exercise) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) KaloProtein else KaloSurfaceElevated,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) KaloProtein else KaloBorder
                                )
                            ) {
                                Text(
                                    text = exercise,
                                    style = KaloTypography.bodyMedium,
                                    color = if (isSelected) KaloBackground else KaloTextPrimary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Exercise Name Field
            item {
                Column {
                    OutlinedTextField(
                        value = state.exerciseName,
                        onValueChange = { viewModel.updateExerciseName(it) },
                        label = { Text("Exercise Name", color = KaloTextSecondary) },
                        textStyle = KaloTypography.headlineMedium.copy(color = KaloTextPrimary),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = KaloProtein,
                            unfocusedBorderColor = KaloBorder,
                            focusedContainerColor = KaloSurface,
                            unfocusedContainerColor = KaloSurface
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )

                    if (state.previousSessionFound) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = KaloProtein,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Auto-filled from your previous session",
                                style = KaloTypography.labelSmall,
                                color = KaloProtein
                            )
                        }
                    }

                    if (!state.overloadHint.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = state.overloadHint ?: "",
                            style = KaloTypography.bodyMedium,
                            color = KaloTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    DateTimeChip(millis = state.timestamp, onChange = { viewModel.setTimestamp(it) })
                }
            }

            // Duration & Calorie estimate inputs
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = state.durationMinutes,
                        onValueChange = { viewModel.updateDuration(it) },
                        label = { Text("Duration (min)", color = KaloTextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = KaloTypography.titleMedium.copy(color = KaloTextPrimary),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = KaloProtein,
                            unfocusedBorderColor = KaloBorder,
                            focusedContainerColor = KaloSurface,
                            unfocusedContainerColor = KaloSurface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = state.estimatedCalories,
                        onValueChange = { viewModel.updateCalories(it) },
                        label = { Text("Burn estimate (kcal)", color = KaloTextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = KaloTypography.titleMedium.copy(color = KaloSteps),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = KaloSteps,
                            unfocusedBorderColor = KaloBorder,
                            focusedContainerColor = KaloSurface,
                            unfocusedContainerColor = KaloSurface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Cardio session", style = KaloTypography.bodyMedium, color = KaloTextPrimary)
                    Switch(checked = state.isCardio, onCheckedChange = viewModel::toggleCardio)
                }
            }

            if (!state.isCardio) {
                // Sets Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "SET", style = KaloTypography.labelSmall, color = KaloTextMuted)
                        Text(text = "WEIGHT (KG)", style = KaloTypography.labelSmall, color = KaloTextMuted)
                        Text(text = "REPS", style = KaloTypography.labelSmall, color = KaloTextMuted)
                        Box(modifier = Modifier.size(24.dp))
                    }
                }

                // Sets List
                itemsIndexed(state.sets) { index, set ->
                    Column {
                    Row { Checkbox(set.isCompleted, { viewModel.toggleSetComplete(index, it) }); Text("Set ${set.setNumber} completed") }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(KaloSurface, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(KaloSurfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${set.setNumber}",
                                style = KaloTypography.titleMedium,
                                color = KaloTextPrimary
                            )
                        }

                        OutlinedTextField(
                            value = set.weightKg,
                            onValueChange = { viewModel.updateSet(index, it, set.reps) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = KaloTypography.titleMedium.copy(color = KaloTextPrimary),
                            modifier = Modifier.width(86.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = KaloProtein,
                                unfocusedBorderColor = KaloBorder
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = set.reps,
                            onValueChange = { viewModel.updateSet(index, set.weightKg, it) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = KaloTypography.titleMedium.copy(color = KaloTextPrimary),
                            modifier = Modifier.width(76.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = KaloProtein,
                                unfocusedBorderColor = KaloBorder
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        IconButton(
                            onClick = { viewModel.deleteSet(index) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Set",
                                tint = if (state.sets.size > 1) KaloTextMuted else Color.Transparent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    }
                }

                // Add Set Action
                item {
                    Surface(
                        onClick = { viewModel.addSet() },
                        shape = RoundedCornerShape(14.dp),
                        color = KaloSurfaceElevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Set",
                                tint = KaloTextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Add Set",
                                style = KaloTypography.titleMedium,
                                color = KaloTextPrimary
                            )
                        }
                    }
                }
            }
            item { OutlinedButton(onClick = viewModel::addExerciseToSession, enabled = !state.isSaving && !state.isLoading) { Text("Add this exercise to session") } }
        }
    }
}
