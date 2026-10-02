package com.kalotracker.app.feature.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kalotracker.app.core.designsystem.components.DateTimeChip
import com.kalotracker.app.core.designsystem.components.KaloButton

@Composable
fun DailyFitnessScreen(state: WorkoutUiState, defaults: List<String>,
    onClose: () -> Unit, onAdvanced: () -> Unit, onTimeChange: (Long) -> Unit,
    onSave: (String, String, FitnessEffort, List<String>) -> Unit) {
    var minutes by rememberSaveable { mutableStateOf(defaults[0]) }
    var weight by rememberSaveable { mutableStateOf(defaults[1]) }
    var effortName by rememberSaveable { mutableStateOf(defaults[2]) }
    var pushups by rememberSaveable { mutableStateOf(defaults[3]) }
    var situps by rememberSaveable { mutableStateOf(defaults[4]) }
    var crunches by rememberSaveable { mutableStateOf(defaults[5]) }
    val effort = runCatching { FitnessEffort.valueOf(effortName) }.getOrDefault(FitnessEffort.MODERATE)
    val reps = listOf(pushups, situps, crunches)
    val estimate = runCatching { dailyFitnessExercises(minutes.toInt(), weight.toDouble(), effort, reps.map { it.toInt() }).sumOf { it.calories } }.getOrNull()
    Scaffold(topBar = {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp)) {
            TextButton(onClick = onClose, enabled = !state.isSaving) { Text("Close") }
            Text("Daily fitness", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        }
    }, bottomBar = {
        Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp)) {
            KaloButton("Save session", { onSave(minutes, weight, effort, reps) }, loading = state.isSaving,
                enabled = !state.isSaving && !state.isSaved && estimate != null)
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("A few minutes for your daily habit", style = MaterialTheme.typography.titleLarge)
            Text("Your last saved choices are ready to repeat. Change only what you did differently.")
            DateTimeChip(state.timestamp, onTimeChange)
            Text("How long?", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 7, 10).forEach { n -> FilterChip(minutes == n.toString(), { minutes = n.toString() }, label = { Text("$n min") }, enabled = !state.isSaving) }
            }
            OutlinedTextField(minutes, { minutes = it }, label = { Text("Total minutes, including breaks") },
                singleLine = true, enabled = !state.isSaving, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            Text("How did it feel?", style = MaterialTheme.typography.titleMedium)
            FitnessEffort.entries.forEach { value -> FilterChip(value == effort, { effortName = value.name }, label = { Text(value.label) }, enabled = !state.isSaving) }
            Text("Reps · enter 0 to skip", style = MaterialTheme.typography.titleMedium)
            listOf(Triple("Push-ups", pushups, { value: String -> pushups = value }),
                Triple("Sit-ups", situps, { value: String -> situps = value }),
                Triple("Crunches", crunches, { value: String -> crunches = value })).forEach { (name, value, change) ->
                OutlinedTextField(value, change, label = { Text(name) }, singleLine = true, enabled = !state.isSaving,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            }
            OutlinedTextField(weight, { weight = it }, label = { Text("Body weight (kg) · remembered next time") },
                singleLine = true, enabled = !state.isSaving, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            Text(if (estimate == null) "Enter your body weight once and check the minutes and reps to save." else "About $estimate kcal · a rough estimate")
            Text("Estimate includes resting energy. Pace and breaks affect it; it stays separate from food intake.", style = MaterialTheme.typography.bodySmall)
            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            TextButton(onClick = onAdvanced, enabled = !state.isSaving) { Text("Switch to separate detailed workout") }
        }
    }
}
