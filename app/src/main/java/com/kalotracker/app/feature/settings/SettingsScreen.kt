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
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kalotracker.app.core.data.repository.MacroPreset
import com.kalotracker.app.core.designsystem.*
import com.kalotracker.app.core.designsystem.components.KaloButton

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onNavigateToAuth: () -> Unit,
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

            // Section: Cloud Sync
            item {
                Column(
                    modifier = Modifier
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
                        Text(
                            text = "SUPABASE CLOUD SYNC",
                            style = KaloTypography.labelSmall,
                            color = KaloTextSecondary
                        )

                        Text(
                            text = if (state.isSupabaseConfigured) "Configured" else "Mock / Offline",
                            style = KaloTypography.labelSmall,
                            color = if (state.isSupabaseConfigured) KaloSteps else KaloCarbs
                        )
                    }

                    Text(
                        text = if (state.isSupabaseConfigured)
                            "Your meals and workouts automatically sync to Postgres when online."
                        else
                            "Running in local-first offline mode. Set your Supabase URL & anon key in SupabaseModule to activate cloud sync.",
                        style = KaloTypography.bodyMedium,
                        color = KaloTextMuted
                    )

                    if (!state.syncMessage.isNullOrBlank()) {
                        Text(
                            text = state.syncMessage ?: "",
                            style = KaloTypography.bodyMedium,
                            color = KaloProtein
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedButton(
                        onClick = { viewModel.syncCloudNow() },
                        enabled = !state.isSyncing,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = KaloTextPrimary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, KaloBorder)
                    ) {
                        Icon(
                            imageVector = if (state.isSyncing) Icons.Default.CloudSync else Icons.Default.CloudDone,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (state.isSyncing) "Syncing..." else "Sync Now",
                            style = KaloTypography.titleMedium
                        )
                    }
                }
            }

            // Section: Account / Auth
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(KaloSurface, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "ACCOUNT & SESSION",
                        style = KaloTypography.labelSmall,
                        color = KaloTextSecondary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(KaloSurfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = KaloTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = if (state.isGuestMode) "Guest User (Offline Mode)" else (state.userEmail ?: "Signed In"),
                                style = KaloTypography.titleMedium,
                                color = KaloTextPrimary
                            )
                            Text(
                                text = if (state.isGuestMode) "Sign in to backup data to the cloud" else "Authenticated with Supabase",
                                style = KaloTypography.bodyMedium,
                                color = KaloTextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (state.isGuestMode) {
                        Button(
                            onClick = onNavigateToAuth,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = KaloSurfaceElevated,
                                contentColor = KaloProtein
                            )
                        ) {
                            Text("Sign In or Register", style = KaloTypography.titleMedium)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.signOut(onNavigateToAuth) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = KaloSurfaceElevated,
                                contentColor = KaloFat
                            )
                        ) {
                            Text("Sign Out", style = KaloTypography.titleMedium)
                        }
                    }
                }
            }
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
