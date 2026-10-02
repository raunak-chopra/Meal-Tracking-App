package com.kalotracker.app.feature.meal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalotracker.app.core.data.food.*
import com.kalotracker.app.core.data.repository.MealRepository
import com.kalotracker.app.core.database.dao.PersonalDao
import com.kalotracker.app.core.database.entity.*
import com.kalotracker.app.core.util.SaveOperation
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import java.util.UUID

class PersonalFoodsViewModel(private val dao: PersonalDao, private val meals: MealRepository) : ViewModel() {
    val foods = dao.observeFoods().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val operation = SaveOperation()
    val logged = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)
    val recent = MutableStateFlow(emptyList<com.kalotracker.app.core.database.dao.MealWithItems>())
    init { viewModelScope.launch {
        try { recent.value = meals.getRecentDistinctMeals() }
        catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (_: Exception) { message.value = "Could not read recent meals. Your library is still available." }
    } }
    fun save(food: SavedFoodEntity) = operation.launch(viewModelScope, { message.value = "Saved to your library." }) {
        PersonalFood.validate(food); dao.putFoods(listOf(food))
    }
    fun delete(food: SavedFoodEntity) = operation.launch(viewModelScope, { message.value = "Removed from library." }) { dao.deleteFood(food.id) }
    fun log(food: SavedFoodEntity, grams: Float, timestamp: Long) = operation.launch(viewModelScope, { logged.value = true }) {
        val id = UUID.randomUUID().toString()
        val items = PersonalFood.portion(food, grams, id)
        meals.saveMeal(MealEntity(id, food.name, items.sumOf { it.calories }, items.sumOf { it.protein.toDouble() }.toFloat(),
            items.sumOf { it.carbs.toDouble() }.toFloat(), items.sumOf { it.fat.toDouble() }.toFloat(), timestamp = timestamp), items)
    }
}

@Composable
fun PersonalFoodsScreen(vm: PersonalFoodsViewModel, initialTimestamp: Long = System.currentTimeMillis(), onClose: () -> Unit) {
    val foods by vm.foods.collectAsState()
    val recent by vm.recent.collectAsState()
    val status by vm.operation.status.collectAsState()
    val logged by vm.logged.collectAsState()
    val message by vm.message.collectAsState()
    LaunchedEffect(logged) { if (logged) onClose() }
    var id by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf("FOOD") }
    var yield by rememberSaveable { mutableStateOf("100") }
    var portion by rememberSaveable { mutableStateOf("100") }
    var editingIngredient by rememberSaveable { mutableStateOf(-1) }
    var ingredientName by rememberSaveable { mutableStateOf("") }
    var grams by rememberSaveable { mutableStateOf("100") }
    var calories by rememberSaveable { mutableStateOf("") }
    var protein by rememberSaveable { mutableStateOf("0") }
    var carbs by rememberSaveable { mutableStateOf("0") }
    var fat by rememberSaveable { mutableStateOf("0") }
    var rowsJson by rememberSaveable { mutableStateOf("[]") }
    var favorite by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    var timestamp by rememberSaveable { mutableStateOf(initialTimestamp) }
    val rows = PersonalFood.json.decodeFromString<List<Ingredient>>(rowsJson)
    fun inputRow() = Ingredient(ingredientName.ifBlank { name }, grams.toFloat(), calories.toInt(), protein.toFloat(), carbs.toFloat(), fat.toFloat())
    fun checked(action: () -> Unit) { try { action(); error = null } catch (e: Exception) { error = e.message ?: "Check the entered numbers." } }
    fun load(f: SavedFoodEntity) {
        id = f.id; name = f.name; kind = f.kind; yield = f.yieldGrams.toString(); portion = yield
        calories = f.calories.toString(); protein = f.protein.toString(); carbs = f.carbs.toString(); fat = f.fat.toString()
        grams = yield; rowsJson = f.ingredientsJson; favorite = f.favorite; ingredientName = ""; editingIngredient = -1
    }
    Scaffold(topBar = { Row(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Your foods & recipes"); TextButton(onClick = onClose) { Text("Back") }
    } }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            item { Text("Label values below are for the entered grams. Recipes use total cooked yield; templates keep their individual foods.") }
            item { Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("FOOD", "RECIPE", "TEMPLATE").forEach { k ->
                FilterChip(selected = kind == k, onClick = { kind = k; rowsJson = "[]"; id = ""; editingIngredient = -1 }, label = { Text(k.lowercase()) })
            } } }
            item { OutlinedTextField(name, { name = it }, label = { Text("Food / recipe / template name") }, modifier = Modifier.fillMaxWidth()) }
            if (kind != "FOOD") item { OutlinedTextField(yield, { yield = it }, label = { Text(if (kind == "RECIPE") "Cooked batch yield (g)" else "Whole template weight (g)") }, modifier = Modifier.fillMaxWidth()) }
            item { Row { Checkbox(favorite, { favorite = it }); Text("Favorite") } }
            item { OutlinedButton(onClick = { showSearch = true }) { Text("Fill nutrition from catalog") } }
            if (kind != "FOOD") item { OutlinedTextField(ingredientName, { ingredientName = it }, label = { Text("Ingredient name") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(grams, { grams = it }, label = { Text("Label serving / ingredient weight (g)") }, modifier = Modifier.fillMaxWidth()) }
            item { com.kalotracker.app.core.designsystem.components.AdaptiveFields(
                first={ fieldModifier -> OutlinedTextField(calories, { calories = it }, label = { Text("kcal") }, modifier = fieldModifier) },
                second={ fieldModifier -> OutlinedTextField(protein, { protein = it }, label = { Text("Protein g") }, modifier = fieldModifier) }) }
            item { com.kalotracker.app.core.designsystem.components.AdaptiveFields(
                first={ fieldModifier -> OutlinedTextField(carbs, { carbs = it }, label = { Text("Carbs g") }, modifier = fieldModifier) },
                second={ fieldModifier -> OutlinedTextField(fat, { fat = it }, label = { Text("Fat g") }, modifier = fieldModifier) }) }
            if (kind != "FOOD") {
                item { OutlinedButton(onClick = { checked {
                    val r = inputRow()
                    PersonalFood.validate(PersonalFood.recipe("Ingredient", r.grams, listOf(r)))
                    rowsJson = PersonalFood.json.encodeToString(if (editingIngredient in rows.indices) rows.mapIndexed { index, old -> if (index == editingIngredient) r else old } else rows + r)
                    editingIngredient = -1
                } }) { Text(if (editingIngredient >= 0) "Update ingredient" else "Add ingredient") } }
                items(rows.indices.toList()) { index -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${rows[index].name}: ${rows[index].grams}g, ${rows[index].calories} kcal", modifier = Modifier.weight(1f))
                    TextButton(onClick = { val r = rows[index]; ingredientName = r.name; grams = r.grams.toString(); calories = r.calories.toString(); protein = r.protein.toString(); carbs = r.carbs.toString(); fat = r.fat.toString(); editingIngredient = index }) { Text("Edit") }
                    TextButton(onClick = { rowsJson = PersonalFood.json.encodeToString(rows.filterIndexed { i, _ -> i != index }); editingIngredient = -1 }) { Text("Remove") }
                } }
            }
            item { (error ?: status.error ?: message)?.let { Text(it) } }
            item { Button(enabled = !status.busy, onClick = { checked {
                val f = if (kind == "FOOD") {
                    val r = inputRow()
                    require(r.grams.isFinite() && r.grams > 0f) { "Enter positive label grams." }
                    SavedFoodEntity(id.ifBlank { UUID.randomUUID().toString() }, name, kind, r.grams, r.calories, r.protein, r.carbs, r.fat, favorite)
                } else PersonalFood.recipe(name, yield.toFloat(), rows, id.ifBlank { UUID.randomUUID().toString() }).copy(kind = kind, favorite = favorite)
                PersonalFood.validate(f); id = f.id; vm.save(f)
            } }) { Text(if (status.busy) "Saving..." else if (id.isBlank()) "Save to library" else "Update library entry") } }
            item { TextButton(onClick = { id = ""; name = ""; rowsJson = "[]"; error = null; editingIngredient = -1 }) { Text("New entry") } }
            item { Text("Log from your library") }
            item { com.kalotracker.app.core.designsystem.components.DateTimeChip(timestamp, { timestamp = it }) }
            item { OutlinedTextField(portion, { portion = it }, label = { Text("Portion to log (g)") }, modifier = Modifier.fillMaxWidth()) }
            items(foods, key = { it.id }) { f -> Column {
                Text("${if (f.favorite) "★ " else ""}${f.name} — ${f.calories} kcal / ${f.yieldGrams}g")
                Column {
                    TextButton(enabled = !status.busy, onClick = { checked { val g = portion.toFloat(); require(g.isFinite() && g > 0); vm.log(f, g, timestamp) } }) { Text("Log") }
                    TextButton(onClick = { load(f) }) { Text("Edit") }
                    TextButton(enabled = !status.busy, onClick = { vm.save(f.copy(favorite = !f.favorite)) }) { Text(if (f.favorite) "Unfavorite" else "Favorite") }
                    TextButton(enabled = !status.busy, onClick = { vm.delete(f) }) { Text("Delete") }
                }
            } }
            item { Text("Create a template from a recent meal") }
            items(recent, key = { "recent" + it.meal.id }) { m -> TextButton(onClick = {
                val ingredients = m.items.map { Ingredient(it.name, it.portionGrams, it.calories, it.protein, it.carbs, it.fat) }
                if (ingredients.isNotEmpty()) { kind = "TEMPLATE"; id = ""; name = m.meal.title; yield = ingredients.sumOf { it.grams.toDouble() }.toString(); rowsJson = PersonalFood.json.encodeToString(ingredients) }
            }) { Text(m.meal.title) } }
        }
    }
    if (showSearch) FoodSearchDialog(onDismiss = { showSearch = false }, onSelect = { food ->
        ingredientName = food.name; if (name.isBlank()) name = food.name
        grams = "100"; calories = food.caloriesPer100g.toInt().toString(); protein = food.proteinPer100g.toString(); carbs = food.carbsPer100g.toString(); fat = food.fatPer100g.toString(); showSearch = false
    })
}
