package com.kalotracker.app.feature.dashboard

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import com.kalotracker.app.core.data.food.matchesSearch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kalotracker.app.core.designsystem.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun MealsScreen(viewModel: DashboardViewModel, onEdit: (String) -> Unit, onOpenLibrary: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val keyboard = LocalSoftwareKeyboardController.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.pendingUndo) {
        state.pendingUndo?.let {
            if (snackbar.showSnackbar(it.message, "Undo") == SnackbarResult.ActionPerformed) viewModel.undoDelete()
            else viewModel.dismissUndo()
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
    val meals = remember(state.todayMeals, query) { state.todayMeals.filter { it.matchesSearch(query) } }
    val groupedMeals = remember(meals) {
        meals.sortedBy { it.meal.timestamp }.groupBy { mealTimeOfDayBucket(it.meal.timestamp) }
    }

    Scaffold(
        containerColor = KaloBackground,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).statusBarsPadding().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 20.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Meals", style = KaloTypography.headlineMedium, color = KaloTextPrimary)
                    FilledTonalButton(
                        onClick = onOpenLibrary,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = KaloSurfaceElevated,
                            contentColor = KaloTextPrimary
                        )
                    ) {
                        Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = KaloAccent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("My foods & recipes")
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
            if (state.todayMeals.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(KaloSurface, RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${state.todayMeals.size} ${if (state.todayMeals.size == 1) "meal" else "meals"} logged",
                            style = KaloTypography.titleMedium,
                            color = KaloTextPrimary
                        )
                        Text(
                            "${state.currentCalories} kcal  ·  P ${state.proteinGrams}g  C ${state.carbsGrams}g  F ${state.fatGrams}g",
                            style = KaloTypography.bodyMedium.copy(fontFeatureSettings = TabularFontFeature),
                            color = KaloTextSecondary
                        )
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search this day's meals or foods", color = KaloTextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = KaloTextSecondary) },
                    trailingIcon = if (query.isNotEmpty()) {
                        {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search", tint = KaloTextSecondary)
                            }
                        }
                    } else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KaloAccent,
                        unfocusedBorderColor = KaloBorder,
                        focusedContainerColor = KaloSurface,
                        unfocusedContainerColor = KaloSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (meals.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(KaloSurface, RoundedCornerShape(20.dp))
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            if (query.isBlank()) "No meals logged for this day" else "No matching meals",
                            style = KaloTypography.titleMedium,
                            color = KaloTextPrimary
                        )
                        Text(
                            if (query.isBlank()) "Tap Add meal below to log by photo, barcode, description or from your library."
                            else "Try another dish or ingredient on this day, or choose another date.",
                            style = KaloTypography.bodyMedium,
                            color = KaloTextSecondary
                        )
                        if (query.isNotEmpty()) TextButton(onClick = { query = "" }) { Text("Clear search") }
                    }
                }
            } else {
                groupedMeals.forEach { (bucket, bucketMeals) ->
                    item(key = "header_$bucket") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(bucket, style = KaloTypography.labelLarge, color = KaloTextSecondary)
                            Text(
                                "${bucketMeals.sumOf { it.meal.totalCalories }} kcal",
                                style = KaloTypography.labelMedium.copy(fontFeatureSettings = TabularFontFeature),
                                color = KaloTextSecondary
                            )
                        }
                    }
                    items(bucketMeals, key = { it.meal.id }) { meal ->
                        MealItemCard(
                            meal,
                            onDelete = { viewModel.deleteMeal(meal) },
                            onEdit = { onEdit(meal.meal.id) },
                            onLogAgain = { viewModel.logMealAgain(meal) },
                            canLogAgain = !state.isRepeatingMeal
                        )
                    }
                }
            }
            state.repeatMessage?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        }
    }
}

private fun mealTimeOfDayBucket(timestamp: Long): String {
    val hour = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).hour
    return when (hour) {
        in 5..10 -> "Morning"
        in 11..15 -> "Midday"
        in 16..20 -> "Evening"
        else -> "Late / Night"
    }
}
