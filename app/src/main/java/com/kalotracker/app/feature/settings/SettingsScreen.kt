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
import androidx.compose.foundation.clickable
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
                    text = "Save targets",
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
            item { AboutYouSection(state = state, viewModel = viewModel) }

            // Section: Macro Split Presets
            item {
                Column {
                    Text(
                        text = "Macro split",
                        style = KaloTypography.titleMedium,
                        color = KaloTextPrimary
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
                        text = "Daily targets",
                        style = KaloTypography.titleMedium,
                        color = KaloTextPrimary
                    )

                    // Calories
                    GoalInputField(
                        label = "Daily Calorie Target",
                        value = state.calorieInput,
                        onValueChange = { viewModel.updateCalorieInput(it) },
                        unit = "kcal",
                        accentColor = KaloCalories
                    )

                    Text(
                        text = "Calories follow your macros: protein and carbs 4 kcal/g, fat 9 kcal/g." +
                            (state.splitText?.let { "\n$it" } ?: ""),
                        style = KaloTypography.bodyMedium,
                        color = KaloTextSecondary
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
    var showBackupKey by remember { mutableStateOf(false) }
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
            Text(text = "Photo logging", style = KaloTypography.titleMedium, color = KaloTextPrimary)
            Text(
                text = if (state.aiConfigured) "Key saved" else "Not set up",
                style = KaloTypography.labelSmall,
                color = if (state.aiConfigured) KaloSteps else KaloCarbs
            )
        }

        Text(
            text = "Photos are read with your own Google Gemini API key (free at aistudio.google.com). " +
                "Keys stay on this phone. Without one, use barcode or search instead.",
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
            value = state.backupKeyInput,
            onValueChange = { viewModel.updateBackupKeyInput(it) },
            label = { Text("Backup key (optional)") },
            supportingText = { Text("Used when the main key hits its limit or stops working.") },
            singleLine = true,
            visualTransformation = if (showBackupKey) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                TextButton(onClick = { showBackupKey = !showBackupKey }) { Text(if (showBackupKey) "Hide" else "Show") }
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
                color = if (state.aiTestOk) KaloTextSecondary else KaloError
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = { viewModel.saveAndTestAi() },
                enabled = !state.isTestingAi && (state.apiKeyInput.isNotBlank() || state.backupKeyInput.isNotBlank()),
                modifier = Modifier.weight(1f)
            ) { Text(if (state.isTestingAi) "Testing..." else "Save & test") }

            if (state.aiConfigured) {
                OutlinedButton(
                    onClick = { viewModel.clearAiKey() },
                    modifier = Modifier.weight(1f)
                ) { Text("Remove keys") }
            }
        }
    }
}

/** Asks for the details needed to suggest calorie, macro and water targets. */
@Composable
private fun AboutYouSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    val body = state.body
    val number = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
    Column(
        modifier = Modifier.fillMaxWidth().background(KaloSurface, RoundedCornerShape(20.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("About you", style = KaloTypography.titleMedium, color = KaloTextPrimary)
        Text(
            "Used to work out a starting calorie, protein and water target. Stays on this phone.",
            style = KaloTypography.bodyMedium,
            color = KaloTextSecondary
        )

        Text("Goal", style = KaloTypography.labelMedium, color = KaloTextPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            com.kalotracker.app.core.ai.GoalType.entries.forEach { goal ->
                FilterChip(selected = state.goal == goal, onClick = { viewModel.setGoal(goal) }, label = { Text(goal.label) })
            }
        }

        Text("Sex", style = KaloTypography.labelMedium, color = KaloTextPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            com.kalotracker.app.core.ai.Sex.entries.forEach { sex ->
                FilterChip(selected = body.sex == sex, onClick = { viewModel.updateBody { it.copy(sex = sex) } }, label = { Text(sex.label) })
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(body.age, { v -> viewModel.updateBody { it.copy(age = v.filter(Char::isDigit).take(3)) } },
                label = { Text("Age") }, suffix = { Text("yrs") }, singleLine = true, keyboardOptions = number, modifier = Modifier.weight(1f))
            OutlinedTextField(body.heightCm, { v -> viewModel.updateBody { it.copy(heightCm = v.take(5)) } },
                label = { Text("Height") }, suffix = { Text("cm") }, singleLine = true, keyboardOptions = number, modifier = Modifier.weight(1f))
            OutlinedTextField(body.weightKg, { v -> viewModel.updateBody { it.copy(weightKg = v.take(5)) } },
                label = { Text("Weight") }, suffix = { Text("kg") }, singleLine = true, keyboardOptions = number, modifier = Modifier.weight(1f))
        }

        Text("Activity", style = KaloTypography.labelMedium, color = KaloTextPrimary)
        Column {
            com.kalotracker.app.core.ai.ActivityLevel.entries.forEach { level ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .clickable { viewModel.updateBody { it.copy(activity = level) } }
                ) {
                    RadioButton(selected = body.activity == level, onClick = { viewModel.updateBody { it.copy(activity = level) } })
                    Column {
                        Text(level.label, style = KaloTypography.bodyLarge, color = KaloTextPrimary)
                        Text(level.detail, style = KaloTypography.bodyMedium, color = KaloTextSecondary)
                    }
                }
            }
        }

        state.bodyError?.let { Text(it, style = KaloTypography.bodyMedium, color = KaloError) }

        OutlinedButton(onClick = viewModel::calculateTargets, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Work out my targets")
        }

        state.suggestion?.let { t ->
            Column(
                modifier = Modifier.fillMaxWidth().background(KaloSurfaceElevated, RoundedCornerShape(16.dp)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("${t.calories} kcal a day", style = KaloTypography.titleLarge, color = KaloTextPrimary)
                Text(
                    "Protein ${t.protein} g  ·  Carbs ${t.carbs} g  ·  Fat ${t.fat} g  ·  Water ${t.waterMl} ml",
                    style = KaloTypography.bodyMedium.copy(fontFeatureSettings = TabularFontFeature),
                    color = KaloTextPrimary
                )
                Text(
                    "Maintenance is about ${t.maintenanceCalories} kcal; at rest about ${t.restingCalories} kcal.",
                    style = KaloTypography.bodyMedium,
                    color = KaloTextSecondary
                )
                KaloButton(text = "Use these targets", onClick = viewModel::applySuggestion, modifier = Modifier.padding(top = 6.dp))
            }
        }
        state.targetsMessage?.let { Text(it, style = KaloTypography.bodyMedium, color = KaloTextSecondary) }
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
                Text("Daily reminder", style = KaloTypography.titleMedium, color = KaloTextPrimary)
                Text(
                    "Around your chosen time, only with fewer than 2 meals and an open day's log.",
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
        Text("Your data", style = KaloTypography.titleMedium, color = KaloTextPrimary)
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
                color = if (state.dataOk) KaloTextSecondary else KaloError
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
            colors = ButtonDefaults.outlinedButtonColors(contentColor = KaloError)
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
                }) { Text("Delete", color = KaloError) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}
