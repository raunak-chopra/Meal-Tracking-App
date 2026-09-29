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
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

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
                        .size(40.dp)
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
                    text = "SETTINGS & GOALS",
                    style = KaloTypography.labelSmall,
                    color = KaloTextSecondary
                )

                Box(modifier = Modifier.size(40.dp))
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
                        viewModel.saveGoals()
                        onBack()
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
