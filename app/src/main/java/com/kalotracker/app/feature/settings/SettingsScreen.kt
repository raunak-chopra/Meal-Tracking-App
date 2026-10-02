package com.kalotracker.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.kalotracker.app.core.data.repository.MacroPreset
import com.kalotracker.app.core.designsystem.*
import com.kalotracker.app.core.designsystem.components.KaloButton

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    appSettings: com.kalotracker.app.core.settings.AppSettings,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val appearance by appSettings.appearance.collectAsState()

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
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(KaloSurfaceElevated)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = KaloTextPrimary
                    )
                }

                Text(
                    text = "Settings",
                    style = KaloTypography.titleLarge,
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
                    text = "Save Goals",
                    onClick = {
                        if (viewModel.saveGoals()) onBack()
                    }
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            state.goalError?.let { message ->
                item { Text(message, color = MaterialTheme.colorScheme.error) }
            }
            item {
                Text("Appearance", style = KaloTypography.titleLarge)
                Column { com.kalotracker.app.core.settings.Appearance.entries.forEach { mode ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = appearance == mode, onClick = { appSettings.saveAppearance(mode) })
                        TextButton(onClick = { appSettings.saveAppearance(mode) }) { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    }
                }
            }
            }
            // Section: Macro Split Presets
            item {
                Column {
                    Text(
                        text = "MACRO SPLIT PRESETS",
                        style = KaloTypography.labelSmall,
                        color = KaloTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            listOf(
                                MacroPreset.BALANCED,
                                MacroPreset.HIGH_PROTEIN,
                                MacroPreset.LOW_CARB,
                                MacroPreset.KETO
                            )
                        ) { preset ->
                            Surface(
                                onClick = { viewModel.applyPreset(preset) },
                                shape = RoundedCornerShape(12.dp),
                                color = KaloSurfaceElevated,
                                border = androidx.compose.foundation.BorderStroke(1.dp, KaloBorder)
                            ) {
                                Text(
                                    text = preset.title,
                                    style = KaloTypography.bodyMedium,
                                    color = KaloTextPrimary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Section: Target Numbers
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(KaloSurface, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "DAILY NUTRITION TARGETS",
                        style = KaloTypography.labelSmall,
                        color = KaloTextSecondary
                    )

                    // Calories
                    GoalInputField(
                        label = "Daily Calorie Target",
                        value = state.calorieInput,
                        onValueChange = { viewModel.updateCalorieInput(it) },
                        unit = "kcal",
                        accentColor = KaloCalories
                    )

                    // Protein
                    GoalInputField(
                        label = "Protein Target",
                        value = state.proteinInput,
                        onValueChange = { viewModel.updateProteinInput(it) },
                        unit = "grams",
                        accentColor = KaloProtein
                    )

                    // Carbs
                    GoalInputField(
                        label = "Carbohydrates Target",
                        value = state.carbsInput,
                        onValueChange = { viewModel.updateCarbsInput(it) },
                        unit = "grams",
                        accentColor = KaloCarbs
                    )

                    // Fat
                    GoalInputField(
                        label = "Fat Target",
                        value = state.fatInput,
                        onValueChange = { viewModel.updateFatInput(it) },
                        unit = "grams",
                        accentColor = KaloFat
                    )

                    // Steps
                    GoalInputField(
                        label = "Daily Step Goal",
                        value = state.stepsInput,
                        onValueChange = { viewModel.updateStepsInput(it) },
                        unit = "steps",
                        accentColor = KaloSteps
                    )

                    // Water Goal
                    GoalInputField(
                        label = "Daily Water Goal",
                        value = state.waterInput,
                        onValueChange = { viewModel.updateWaterInput(it) },
                        unit = "ml",
                        accentColor = KaloWater
                    )
                }
            }

            item { GoalTypeSection(state = state, viewModel = viewModel) }

            item { ReminderSection(state = state, viewModel = viewModel) }

            item { DataSection(state = state, viewModel = viewModel) }

            // Section: AI meal scanning (user's own Gemini key)
            item { AiSettingsSection(state = state, viewModel = viewModel) }
        }
    }
}

@Composable
fun GoalInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    unit: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = KaloTypography.titleMedium, color = KaloTextPrimary)
            Text(text = unit, style = KaloTypography.bodyMedium, color = KaloTextMuted)
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = KaloTypography.titleMedium.copy(color = accentColor),
            modifier = Modifier.width(110.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = KaloBorder,
                focusedContainerColor = KaloSurfaceElevated,
                unfocusedContainerColor = KaloSurfaceElevated
            ),
            shape = RoundedCornerShape(10.dp)
        )
    }
}

@Composable
fun AiSettingsSection(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    var showKey by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(KaloSurface, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "AI MEAL SCANNING", style = KaloTypography.labelSmall, color = KaloTextSecondary)
            Text(
                text = if (state.aiConfigured) "Key saved" else "Not set up",
                style = KaloTypography.labelSmall,
                color = if (state.aiConfigured) KaloSteps else KaloCarbs
            )
        }

        Text(
            text = "Photo scanning uses your own Google Gemini API key (free tier available at aistudio.google.com). " +
                "It is stored only on this phone. Without a key, photo scanning is off; barcode and manual logging still work.",
            style = KaloTypography.bodyMedium,
            color = KaloTextMuted
        )

        OutlinedTextField(
            value = state.apiKeyInput,
            onValueChange = { viewModel.updateApiKeyInput(it) },
            label = { Text("Gemini API key") },
            singleLine = true,
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                TextButton(onClick = { showKey = !showKey }) { Text(if (showKey) "Hide" else "Show") }
            }
        )

        OutlinedTextField(
            value = state.modelInput,
            onValueChange = { viewModel.updateModelInput(it) },
            label = { Text("Model") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (!state.aiTestMessage.isNullOrBlank()) {
            Text(
                text = state.aiTestMessage ?: "",
                style = KaloTypography.bodyMedium,
                color = if (state.aiTestOk) KaloSteps else KaloFat
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = { viewModel.saveAndTestAi() },
                enabled = !state.isTestingAi && state.apiKeyInput.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) { Text(if (state.isTestingAi) "Testing..." else "Save & test") }

            if (state.aiConfigured) {
                OutlinedButton(
                    onClick = { viewModel.clearAiKey() },
                    modifier = Modifier.weight(1f)
                ) { Text("Remove key") }
            }
        }
    }
}

@Composable
private fun GoalTypeSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth().background(KaloSurface, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("YOUR GOAL", style = KaloTypography.labelSmall, color = KaloTextSecondary)
        Text(
            "Used on the Trends screen to check whether your calorie target matches how your weight is actually moving.",
            style = KaloTypography.bodyMedium,
            color = KaloTextMuted
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            com.kalotracker.app.core.ai.GoalType.entries.forEach { goal ->
                FilterChip(
                    selected = state.goal == goal,
                    onClick = { viewModel.setGoal(goal) },
                    label = { Text(goal.label) }
                )
            }
        }
    }
}

@Composable
private fun ReminderSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showTimePicker by remember { mutableStateOf(false) }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.setReminderEnabled(granted) }

    fun onToggle(enabled: Boolean) {
        val needsPermission = enabled && android.os.Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        if (needsPermission) permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        else viewModel.setReminderEnabled(enabled)
    }

    Column(
        modifier = Modifier.fillMaxWidth().background(KaloSurface, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("DAILY REMINDER", style = KaloTypography.labelSmall, color = KaloTextSecondary)
                Text(
                    "A nudge only when fewer than 2 meals are logged that day.",
                    style = KaloTypography.bodyMedium,
                    color = KaloTextMuted
                )
            }
            Switch(checked = state.reminder.enabled, onCheckedChange = { onToggle(it) })
        }
        if (state.reminder.enabled) {
            OutlinedButton(onClick = { showTimePicker = true }) {
                Text("Remind me at %02d:%02d".format(state.reminder.hour, state.reminder.minute))
            }
        }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = state.reminder.hour,
            initialMinute = state.reminder.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setReminderTime(timeState.hour, timeState.minute)
                    showTimePicker = false
                }) { Text("Set") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
            text = { TimePicker(state = timeState) }
        )
    }
}

@Composable
private fun DataSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val resolver = context.contentResolver
    var confirmDelete by remember { mutableStateOf(false) }
    val today = java.time.LocalDate.now()

    val backupLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) viewModel.exportBackup(uri, resolver) }

    val archiveLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> if (uri != null) viewModel.exportArchive(uri, resolver) }
    val folderLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
    ) { uri -> if (uri != null) viewModel.chooseBackupFolder(uri, resolver, context) }
    val csvLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> if (uri != null) viewModel.exportMealsCsv(uri, resolver) }

    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) viewModel.previewImport(uri, resolver) }

    Column(
        modifier = Modifier.fillMaxWidth().background(KaloSurface, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("YOUR DATA", style = KaloTypography.labelSmall, color = KaloTextSecondary)
        Text(
            "Logs are stored on this phone without an account. Optional Gemini analysis sends your selected photo or trend summary; barcode lookup sends the barcode to Open Food Facts. " +
                "Complete ZIP archives include photos and your library. JSON is a smaller data-only backup. Automatic backups use the folder you choose; a cloud-backed folder may be synced by its provider.",
            style = KaloTypography.bodyMedium,
            color = KaloTextMuted
        )

        if (!state.dataMessage.isNullOrBlank()) {
            Text(
                state.dataMessage ?: "",
                style = KaloTypography.bodyMedium,
                color = if (state.dataOk) KaloSteps else KaloFat
            )
        }

        OutlinedButton(onClick = { archiveLauncher.launch("kalo-complete-$today.zip") }, enabled = !state.dataBusy, modifier = Modifier.fillMaxWidth()) { Text("Export complete archive (photos included)") }
        OutlinedButton(onClick = { folderLauncher.launch(null) }, modifier = Modifier.fillMaxWidth()) { Text("Choose automatic backup folder") }
        Row { Switch(checked = state.backupSchedule.enabled, onCheckedChange = { viewModel.enableBackup(it, context) }, enabled = state.backupSchedule.folder.isNotBlank()); Text("Daily dated backups (battery permitting)") }
        Text(if (state.backupSchedule.lastSuccess > 0) "Last backup: ${java.time.Instant.ofEpochMilli(state.backupSchedule.lastSuccess).atZone(java.time.ZoneId.systemDefault())}" else "No automatic backup completed yet.")
        state.backupSchedule.error?.let { Text("Backup failed: $it") }
        OutlinedButton(
            onClick = { backupLauncher.launch("kalo-backup-$today.json") },
            enabled = !state.dataBusy,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Export backup (JSON)") }

        OutlinedButton(
            onClick = { importLauncher.launch(arrayOf("*/*")) },
            enabled = !state.dataBusy,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Import backup") }

        OutlinedButton(
            onClick = { csvLauncher.launch("kalo-meals-$today.csv") },
            enabled = !state.dataBusy,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Export meals for spreadsheet (CSV)") }

        OutlinedButton(
            onClick = { confirmDelete = true },
            enabled = !state.dataBusy,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = KaloFat)
        ) { Text("Delete all my data") }
    }

    state.restorePreview?.let { preview -> AlertDialog(onDismissRequest = viewModel::cancelRestore,
        title = { Text("Restore preview") }, text = { Text(preview) },
        confirmButton = { TextButton(onClick = { viewModel.confirmRestore() }, enabled = !state.dataBusy) { Text("Import") } },
        dismissButton = { TextButton(onClick = viewModel::cancelRestore, enabled = !state.dataBusy) { Text("Cancel") } }) }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete everything?") },
            text = {
                Text("This permanently deletes all logs, meal photos, foods, recipes, routines, barcode cache and day/goal history on this phone. Your current goals and API key stay. Existing exported backups stay in their folders. Export a backup first if you might want this back.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteAllData()
                }) { Text("Delete", color = KaloFat) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}
