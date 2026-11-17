package com.kalotracker.app.feature.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kalotracker.app.core.designsystem.*
import com.kalotracker.app.core.designsystem.components.DateTimeChip
import com.kalotracker.app.core.designsystem.components.KaloButton

@Composable
fun DailyFitnessScreen(
    state: WorkoutUiState,
    defaults: List<String>,
    onClose: () -> Unit,
    onAdvanced: () -> Unit,
    onTimeChange: (Long) -> Unit,
    onSave: (String, String, FitnessEffort, List<String>) -> Unit
) {
    var minutes by rememberSaveable { mutableStateOf(defaults[0]) }
    var weight by rememberSaveable { mutableStateOf(defaults[1]) }
    var effortName by rememberSaveable { mutableStateOf(defaults[2]) }
    var pushups by rememberSaveable { mutableStateOf(defaults[3]) }
    var situps by rememberSaveable { mutableStateOf(defaults[4]) }
    var crunches by rememberSaveable { mutableStateOf(defaults[5]) }
    val effort = runCatching { FitnessEffort.valueOf(effortName) }.getOrDefault(FitnessEffort.MODERATE)
    val reps = listOf(pushups, situps, crunches)
    val estimate = runCatching {
        dailyFitnessExercises(minutes.toInt(), weight.toDouble(), effort, reps.map { it.toInt() }).sumOf { it.calories }
    }.getOrNull()

    Scaffold(
        containerColor = KaloBackground,
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    enabled = !state.isSaving,
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(KaloSurfaceElevated)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = KaloTextPrimary)
                }
                Text("Daily fitness", style = KaloTypography.titleLarge, color = KaloTextPrimary)
                DateTimeChip(state.timestamp, onTimeChange)
            }
        },
        bottomBar = {
            Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp)) {
                KaloButton(
                    text = if (estimate != null) "Save session · ~$estimate kcal" else "Save session",
                    onClick = { onSave(minutes, weight, effort, reps) },
                    loading = state.isSaving,
                    enabled = !state.isSaving && !state.isSaved && estimate != null
                )
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("A few minutes for your daily habit", style = KaloTypography.titleMedium, color = KaloTextPrimary)
                Text(
                    "Your last saved choices are ready to repeat. Change only what you did differently.",
                    style = KaloTypography.bodyMedium,
                    color = KaloTextSecondary
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KaloSurface, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("How long?", style = KaloTypography.titleMedium, color = KaloTextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 7, 10).forEach { n ->
                        FilterChip(
                            selected = minutes == n.toString(),
                            onClick = { minutes = n.toString() },
                            label = { Text("$n min") },
                            enabled = !state.isSaving
                        )
                    }
                }
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { minutes = it },
                    label = { Text("Total minutes, including breaks") },
                    singleLine = true,
                    enabled = !state.isSaving,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("How did it feel?", style = KaloTypography.titleMedium, color = KaloTextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FitnessEffort.entries.forEach { value ->
                        FilterChip(
                            selected = value == effort,
                            onClick = { effortName = value.name },
                            label = { Text(value.label) },
                            enabled = !state.isSaving
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KaloSurface, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Reps · enter 0 to skip", style = KaloTypography.titleMedium, color = KaloTextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        Triple("Push-ups", pushups, { value: String -> pushups = value }),
                        Triple("Sit-ups", situps, { value: String -> situps = value }),
                        Triple("Crunches", crunches, { value: String -> crunches = value })
                    ).forEach { (name, value, change) ->
                        OutlinedTextField(
                            value = value,
                            onValueChange = change,
                            label = { Text(name) },
                            singleLine = true,
                            enabled = !state.isSaving,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KaloSurface, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Body weight (kg) · remembered next time") },
                    singleLine = true,
                    enabled = !state.isSaving,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    if (estimate == null) "Enter your body weight once and check the minutes and reps to save."
                    else "About $estimate kcal · a rough estimate",
                    style = KaloTypography.titleMedium,
                    color = if (estimate == null) KaloTextSecondary else KaloAccent
                )
                Text(
                    "Estimate includes resting energy. Pace and breaks affect it; it stays separate from food intake.",
                    style = KaloTypography.bodyMedium,
                    color = KaloTextSecondary
                )
            }

            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            OutlinedButton(
                onClick = onAdvanced,
                enabled = !state.isSaving,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                Text("Switch to separate detailed workout")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
