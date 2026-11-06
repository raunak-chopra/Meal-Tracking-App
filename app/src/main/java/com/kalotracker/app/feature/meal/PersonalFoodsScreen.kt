package com.kalotracker.app.feature.meal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalotracker.app.core.data.food.*
import com.kalotracker.app.core.data.repository.MealRepository
import com.kalotracker.app.core.database.dao.PersonalDao
import com.kalotracker.app.core.database.entity.*
import com.kalotracker.app.core.designsystem.*
import com.kalotracker.app.core.designsystem.components.DateTimeChip
import com.kalotracker.app.core.designsystem.components.KaloButton
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
    init {
        viewModelScope.launch {
            try { recent.value = meals.getRecentDistinctMeals() }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) { message.value = "Could not read recent meals. Your library is still available." }
        }
    }
    fun save(food: SavedFoodEntity) = operation.launch(viewModelScope, { message.value = "Saved to your library." }) {
        PersonalFood.validate(food); dao.putFoods(listOf(food))
    }
    fun delete(food: SavedFoodEntity) = operation.launch(viewModelScope, { message.value = "Removed from library." }) { dao.deleteFood(food.id) }
    fun log(food: SavedFoodEntity, grams: Float, timestamp: Long) = operation.launch(viewModelScope, { logged.value = true }) {
        val id = UUID.randomUUID().toString()
        val items = PersonalFood.portion(food, grams, id)
        meals.saveMeal(
            MealEntity(
                id,
                food.name,
                items.sumOf { it.calories },
                items.sumOf { it.protein.toDouble() }.toFloat(),
                items.sumOf { it.carbs.toDouble() }.toFloat(),
                items.sumOf { it.fat.toDouble() }.toFloat(),
                timestamp = timestamp
            ),
            items
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonalFoodsScreen(
    vm: PersonalFoodsViewModel,
    initialTimestamp: Long = System.currentTimeMillis(),
    onClose: () -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val foods by vm.foods.collectAsState()
    val recent by vm.recent.collectAsState()
    val status by vm.operation.status.collectAsState()
    val logged by vm.logged.collectAsState()
    val message by vm.message.collectAsState()
    LaunchedEffect(logged) { if (logged) onClose() }

    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var filterTab by rememberSaveable { mutableStateOf("ALL") }

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
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var timestamp by rememberSaveable { mutableStateOf(initialTimestamp) }

    val rows = PersonalFood.json.decodeFromString<List<Ingredient>>(rowsJson)
    fun inputRow() = Ingredient(ingredientName.ifBlank { name }, grams.toFloat(), calories.toInt(), protein.toFloat(), carbs.toFloat(), fat.toFloat())
    fun checked(action: () -> Unit) {
        try {
            action()
            error = null
        } catch (e: Exception) {
            error = e.message ?: "Check the entered numbers."
        }
    }
    fun resetEditor(initialKind: String = "FOOD") {
        id = ""
        name = ""
        kind = initialKind
        yield = "100"
        grams = "100"
        calories = ""
        protein = "0"
        carbs = "0"
        fat = "0"
        rowsJson = "[]"
        favorite = false
        ingredientName = ""
        editingIngredient = -1
        error = null
    }
    fun load(f: SavedFoodEntity) {
        id = f.id
        name = f.name
        kind = f.kind
        yield = f.yieldGrams.toString()
        portion = yield
        calories = f.calories.toString()
        protein = f.protein.toString()
        carbs = f.carbs.toString()
        fat = f.fat.toString()
        grams = yield
        rowsJson = f.ingredientsJson
        favorite = f.favorite
        ingredientName = ""
        editingIngredient = -1
        error = null
        editorOpen = true
    }

    val searchIndex = remember(foods) { SavedFoodSearch(foods) }
    val filteredFoods = remember(searchIndex, searchQuery, filterTab) {
        searchIndex.search(searchQuery, filterTab)
    }

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
                    onClick = { if (editorOpen) editorOpen = false else onClose() },
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(KaloSurfaceElevated)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = KaloTextPrimary
                    )
                }
                Text(
                    text = if (editorOpen) (if (id.isBlank()) "New library entry" else "Edit library entry") else "Your foods & recipes",
                    style = KaloTypography.titleLarge,
                    color = KaloTextPrimary
                )
                if (!editorOpen) {
                    FilledTonalButton(
                        onClick = { resetEditor(); editorOpen = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = KaloAccent,
                            contentColor = KaloOnAccent
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New")
                    }
                } else {
                    Box(Modifier.size(48.dp))
                }
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                (error ?: status.error ?: message)?.let { msg ->
                    Surface(shape = RoundedCornerShape(14.dp), color = KaloSurfaceElevated, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            msg,
                            style = KaloTypography.bodyMedium,
                            color = if (error != null || status.error != null) KaloError else KaloAccent,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            if (editorOpen) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(KaloSurface, RoundedCornerShape(20.dp))
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Label values below are for the entered grams. Recipes use total cooked yield; templates keep their individual foods.",
                            style = KaloTypography.bodyMedium,
                            color = KaloTextSecondary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("FOOD" to "Food label", "RECIPE" to "Recipe", "TEMPLATE" to "Template").forEach { (k, label) ->
                                FilterChip(
                                    selected = kind == k,
                                    onClick = { kind = k; rowsJson = "[]"; id = ""; editingIngredient = -1 },
                                    label = { Text(label) }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Food / recipe / template name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (kind != "FOOD") {
                            OutlinedTextField(
                                value = yield,
                                onValueChange = { yield = it },
                                label = { Text(if (kind == "RECIPE") "Cooked batch yield (g)" else "Whole template weight (g)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { favorite = !favorite }
                            ) {
                                Checkbox(favorite, { favorite = it })
                                Text("Star as favorite", color = KaloTextPrimary)
                            }
                            OutlinedButton(onClick = { showSearch = true }, shape = RoundedCornerShape(12.dp)) {
                                Text("Fill from catalog")
                            }
                        }
                        if (kind != "FOOD") {
                            HorizontalDivider(color = KaloDivider)
                            Text("Ingredients", style = KaloTypography.titleMedium, color = KaloTextPrimary)
                            OutlinedTextField(
                                value = ingredientName,
                                onValueChange = { ingredientName = it },
                                label = { Text("Ingredient name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        OutlinedTextField(
                            value = grams,
                            onValueChange = { grams = it },
                            label = { Text("Label serving / ingredient weight (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        com.kalotracker.app.core.designsystem.components.AdaptiveFields(
                            first = { fieldModifier ->
                                OutlinedTextField(
                                    calories, { calories = it },
                                    label = { Text("kcal") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = fieldModifier
                                )
                            },
                            second = { fieldModifier ->
                                OutlinedTextField(
                                    protein, { protein = it },
                                    label = { Text("Protein g") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = fieldModifier
                                )
                            }
                        )
                        com.kalotracker.app.core.designsystem.components.AdaptiveFields(
                            first = { fieldModifier ->
                                OutlinedTextField(
                                    carbs, { carbs = it },
                                    label = { Text("Carbs g") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = fieldModifier
                                )
                            },
                            second = { fieldModifier ->
                                OutlinedTextField(
                                    fat, { fat = it },
                                    label = { Text("Fat g") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = fieldModifier
                                )
                            }
                        )
                        if (kind != "FOOD") {
                            OutlinedButton(
                                onClick = {
                                    checked {
                                        val r = inputRow()
                                        PersonalFood.validate(PersonalFood.recipe("Ingredient", r.grams, listOf(r)))
                                        rowsJson = PersonalFood.json.encodeToString(
                                            if (editingIngredient in rows.indices) rows.mapIndexed { index, old -> if (index == editingIngredient) r else old }
                                            else rows + r
                                        )
                                        editingIngredient = -1
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (editingIngredient >= 0) "Update ingredient" else "+ Add ingredient to list")
                            }
                            rows.forEachIndexed { index, r ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .background(KaloSurfaceElevated, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${r.name}: ${r.grams}g · ${r.calories} kcal", modifier = Modifier.weight(1f), color = KaloTextPrimary)
                                    TextButton(onClick = {
                                        ingredientName = r.name
                                        grams = r.grams.toString()
                                        calories = r.calories.toString()
                                        protein = r.protein.toString()
                                        carbs = r.carbs.toString()
                                        fat = r.fat.toString()
                                        editingIngredient = index
                                    }) { Text("Edit") }
                                    TextButton(onClick = {
                                        rowsJson = PersonalFood.json.encodeToString(rows.filterIndexed { i, _ -> i != index })
                                        editingIngredient = -1
                                    }) { Text("Remove") }
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        KaloButton(
                            text = if (id.isBlank()) "Save to library" else "Update library entry",
                            loading = status.busy,
                            enabled = !status.busy,
                            onClick = {
                                checked {
                                    val f = if (kind == "FOOD") {
                                        val r = inputRow()
                                        require(r.grams.isFinite() && r.grams > 0f) { "Enter positive label grams." }
                                        SavedFoodEntity(id.ifBlank { UUID.randomUUID().toString() }, name, kind, r.grams, r.calories, r.protein, r.carbs, r.fat, favorite)
                                    } else {
                                        PersonalFood.recipe(name, yield.toFloat(), rows, id.ifBlank { UUID.randomUUID().toString() }).copy(kind = kind, favorite = favorite)
                                    }
                                    PersonalFood.validate(f)
                                    id = f.id
                                    vm.save(f)
                                    editorOpen = false
                                }
                            }
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = { resetEditor(kind) }) { Text("Clear form") }
                            TextButton(onClick = { editorOpen = false }) { Text("Back to library") }
                        }
                    }
                }
            } else {
                // Library Browse & Log View
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(KaloSurface, RoundedCornerShape(20.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Log settings", style = KaloTypography.labelLarge, color = KaloTextSecondary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DateTimeChip(timestamp, { timestamp = it })
                            OutlinedTextField(
                                value = portion,
                                onValueChange = { portion = it },
                                label = { Text("Portion (g)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search library names or ingredients", color = KaloTextMuted) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = KaloTextSecondary) },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search", tint = KaloTextSecondary)
                                }
                            }
                        } else null,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                        singleLine = true,
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

                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val tabs = listOf(
                            "ALL" to "All (${foods.size})",
                            "FAVORITES" to "★ Favorites",
                            "FOOD" to "Foods",
                            "RECIPE" to "Recipes",
                            "TEMPLATE" to "Templates"
                        )
                        items(tabs) { (key, label) ->
                            FilterChip(
                                selected = filterTab == key,
                                onClick = { filterTab = key },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                if (filteredFoods.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(KaloSurface, RoundedCornerShape(20.dp))
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                if (foods.isEmpty()) "Your personal library is empty" else "No matching entries",
                                style = KaloTypography.titleMedium,
                                color = KaloTextPrimary
                            )
                            Text(
                                if (foods.isEmpty()) "Save nutrition labels, home-cooked batch recipes, or favorite meal templates for one-tap logging."
                                else "Try another filter or search term.",
                                style = KaloTypography.bodyMedium,
                                color = KaloTextSecondary
                            )
                            if (foods.isNotEmpty() && (searchQuery.isNotEmpty() || filterTab != "ALL")) {
                                TextButton(onClick = { searchQuery = ""; filterTab = "ALL" }) { Text("Show all entries") }
                            }
                            if (foods.isEmpty()) {
                                OutlinedButton(onClick = { resetEditor(); editorOpen = true }, shape = RoundedCornerShape(12.dp)) {
                                    Text("+ Create your first food or recipe")
                                }
                            }
                        }
                    }
                } else {
                    items(filteredFoods, key = { it.id }) { f ->
                        val logGrams = portion.toFloatOrNull()?.takeIf { it.isFinite() && it > 0f }
                        val scaledCalories = if (logGrams != null && f.yieldGrams > 0f) {
                            kotlin.math.round(f.calories * (logGrams / f.yieldGrams)).toInt()
                        } else f.calories
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(KaloSurface, RoundedCornerShape(20.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(f.name, style = KaloTypography.titleMedium, color = KaloTextPrimary)
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = KaloSurfaceElevated
                                        ) {
                                            Text(
                                                f.kind.lowercase().replaceFirstChar { it.uppercase() },
                                                style = KaloTypography.labelSmall,
                                                color = KaloTextSecondary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        "${f.calories} kcal per ${formatAmount(f.yieldGrams, "g")}  ·  P ${f.protein.toInt()}g  C ${f.carbs.toInt()}g  F ${f.fat.toInt()}g",
                                        style = KaloTypography.bodyMedium.copy(fontFeatureSettings = TabularFontFeature),
                                        color = KaloTextSecondary
                                    )
                                }
                                IconButton(
                                    enabled = !status.busy,
                                    onClick = { vm.save(f.copy(favorite = !f.favorite)) }
                                ) {
                                    Icon(
                                        imageVector = if (f.favorite) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = if (f.favorite) "Unfavorite" else "Favorite",
                                        tint = if (f.favorite) KaloAccent else KaloTextSecondary
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(onClick = { load(f) }) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Edit")
                                    }
                                    TextButton(enabled = !status.busy, onClick = { vm.delete(f) }) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Delete")
                                    }
                                }
                                FilledTonalButton(
                                    enabled = !status.busy && logGrams != null,
                                    onClick = {
                                        checked {
                                            val g = portion.toFloat()
                                            require(g.isFinite() && g > 0)
                                            vm.log(f, g, timestamp)
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = KaloAccent,
                                        contentColor = KaloOnAccent
                                    )
                                ) {
                                    Text(if (logGrams != null) "Log ${formatAmount(logGrams, "g")} · $scaledCalories kcal" else "Log")
                                }
                            }
                        }
                    }
                }

                if (recent.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(KaloSurface, RoundedCornerShape(20.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Create a template from a recent meal", style = KaloTypography.titleMedium, color = KaloTextPrimary)
                            Text(
                                "Tap any recent meal to open it in the template builder above.",
                                style = KaloTypography.bodyMedium,
                                color = KaloTextSecondary
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                recent.forEach { m ->
                                    OutlinedButton(
                                        onClick = {
                                            val ingredients = m.items.map {
                                                Ingredient(it.name, it.portionGrams, it.calories, it.protein, it.carbs, it.fat)
                                            }
                                            if (ingredients.isNotEmpty()) {
                                                kind = "TEMPLATE"
                                                id = ""
                                                name = m.meal.title
                                                yield = ingredients.sumOf { it.grams.toDouble() }.toString()
                                                rowsJson = PersonalFood.json.encodeToString(ingredients)
                                                editorOpen = true
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(m.meal.title)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (showSearch) FoodSearchDialog(onDismiss = { showSearch = false }, onSelect = { food ->
        ingredientName = food.name
        if (name.isBlank()) name = food.name
        grams = "100"
        calories = food.caloriesPer100g.toInt().toString()
        protein = food.proteinPer100g.toString()
        carbs = food.carbsPer100g.toString()
        fat = food.fatPer100g.toString()
        showSearch = false
    })
}
