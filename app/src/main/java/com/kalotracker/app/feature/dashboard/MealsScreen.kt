package com.kalotracker.app.feature.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kalotracker.app.core.designsystem.*

@Composable
fun MealsScreen(viewModel: DashboardViewModel, onEdit: (String) -> Unit, onOpenLibrary: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.pendingUndo) { state.pendingUndo?.let {
        if (snackbar.showSnackbar(it.message, "Undo") == SnackbarResult.ActionPerformed) viewModel.undoDelete()
        else viewModel.dismissUndo()
    } }
    Scaffold(containerColor = KaloBackground, contentWindowInsets = WindowInsets(0), snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).statusBarsPadding().padding(horizontal=20.dp),
            verticalArrangement=Arrangement.spacedBy(16.dp), contentPadding=PaddingValues(vertical=20.dp)) {
            item { Text("Meals", style=KaloTypography.headlineMedium, color=KaloTextPrimary) }
            item { Row(Modifier.fillMaxWidth()) {
                TextButton(onClick=viewModel::goToPreviousDay) { Text("Previous") }
                Text(state.selectedDate.toString(), Modifier.weight(1f).padding(vertical=16.dp), color=KaloTextSecondary)
                TextButton(onClick=viewModel::goToNextDay) { Text("Next") }
            } }
            item { OutlinedTextField(query, { query=it }, label={Text("Search this day's meals")}, modifier=Modifier.fillMaxWidth()) }
            item { TextButton(onClick=onOpenLibrary) { Text("My foods, favorites & recipes") } }
            val meals = state.todayMeals.filter { it.meal.title.contains(query,true) || it.items.any { item -> item.name.contains(query,true) } }
            if(meals.isEmpty()) item { Text(if(query.isBlank()) "No meals logged for this day." else "No matching meals.", color=KaloTextSecondary) }
            items(meals,key={it.meal.id}) { meal -> MealItemCard(meal, {viewModel.deleteMeal(meal)}, {onEdit(meal.meal.id)},
                {viewModel.logMealAgain(meal)}, !state.isRepeatingMeal) }
            state.repeatMessage?.let { item { Text(it,color=MaterialTheme.colorScheme.error) } }
        }
    }
}
