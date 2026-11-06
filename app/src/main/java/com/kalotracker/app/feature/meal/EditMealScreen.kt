package com.kalotracker.app.feature.meal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kalotracker.app.core.designsystem.*
import com.kalotracker.app.core.designsystem.components.DateTimeChip
import com.kalotracker.app.core.designsystem.components.KaloButton
import java.io.File

@Composable
fun EditMealScreen(
    viewModel: EditMealViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var showFoodSearch by rememberSaveable { mutableStateOf(false) }

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
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(KaloSurfaceElevated)
                ) { Icon(Icons.Default.Close, contentDescription = "Close", tint = KaloTextPrimary) }
                Text("Edit meal", style = KaloTypography.titleMedium, color = KaloTextPrimary)
                Box(Modifier.size(48.dp))
            }
        },
        bottomBar = {
            if (!state.isLoading && !state.notFound) {
                Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp)) {
                    KaloButton(
                        text = if (state.items.any { !it.hasValidPortion }) "Check food amounts" else if (state.items.any { !it.isReadyToSave }) "Review food nutrition" else "Save changes (${state.totalCalories} kcal)",
                        onClick = { viewModel.save(onClose) },
                        loading = state.isSaving,
                        enabled = state.items.isNotEmpty() && state.items.all { it.isReadyToSave } && !state.isSaving && state.estimatingItemId == null
                    )
                }
            }
        }
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = KaloProtein)
            }
            state.notFound -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(state.errorMessage ?: "This meal no longer exists.", style = KaloTypography.bodyLarge, color = KaloTextSecondary)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                state.errorMessage?.let { message ->
                    item { Text(message, color = MaterialTheme.colorScheme.error) }
                }
                item {
                    val photoFile = state.imageLocalUri?.let(::File)?.takeIf { it.exists() }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(KaloSurface, RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        if (photoFile != null) {
                            AsyncImage(
                                model = photoFile,
                                contentDescription = "Meal photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(KaloSurfaceElevated)
                            )
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(if (state.items.any { !it.hasValidPortion }) "Check food amounts" else if (state.items.any { !it.isReadyToSave }) "Review food nutrition" else "${state.totalCalories} kcal", style = KaloTypography.displayMedium, color = KaloCalories)
                            if (state.items.all { it.isReadyToSave }) Text(
                                "${state.totalProtein.toInt()}g P • ${state.totalCarbs.toInt()}g C • ${state.totalFat.toInt()}g F",
                                style = KaloTypography.titleMedium.copy(fontFeatureSettings = TabularFontFeature),
                                color = KaloTextSecondary
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = state.title,
                        onValueChange = viewModel::setTitle,
                        label = { Text("Meal name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        DateTimeChip(millis = state.timestamp, onChange = viewModel::setTimestamp)
                    }
                }
                items(state.items, key = { it.id }) { item ->
                    EditableItemRow(
                        item = item,
                        onNameChange = { viewModel.setItemName(item.id, it) },
                        onAmountInputChange = { viewModel.setItemAmountInput(item.id, it) },
                        onRemove = { viewModel.removeItem(item.id) },
                        onNutritionChange = { k, p, c, f -> viewModel.correctNutrition(item.id, k, p, c, f) },
                        onKeepNutrition = { viewModel.keepItemNutrition(item.id) },
                        enabled = !state.isSaving && state.estimatingItemId == null,
                        onReestimate = { viewModel.reestimateItem(item.id) },
                        estimating = state.estimatingItemId == item.id
                    )
                }
                item {
                    OutlinedButton(onClick = { showFoodSearch = true }) { Text("+ Add food") }
                }
                item {
                    OutlinedTextField(
                        value = state.notes,
                        onValueChange = viewModel::setNotes,
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showFoodSearch) {
        FoodSearchDialog(
            onSelect = { viewModel.addCatalogItem(it) },
            onDismiss = { showFoodSearch = false }
        )
    }
}
