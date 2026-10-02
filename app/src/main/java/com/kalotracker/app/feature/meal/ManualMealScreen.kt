package com.kalotracker.app.feature.meal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kalotracker.app.core.data.food.FoodCatalog
import com.kalotracker.app.core.data.food.FoodCatalogItem
import com.kalotracker.app.core.database.dao.MealWithItems
import com.kalotracker.app.core.database.entity.FoodItemEntity
import com.kalotracker.app.core.designsystem.components.DateTimeChip
import com.kalotracker.app.core.database.entity.MealEntity
import com.kalotracker.app.core.designsystem.*
import com.kalotracker.app.core.designsystem.components.KaloButton
import java.util.UUID

@Composable
fun ManualMealScreen(
    viewModel: ManualMealViewModel,
    onClose: () -> Unit,
    onOpenLibrary: () -> Unit,
    onSaveMeal: suspend (MealEntity, List<FoodItemEntity>) -> Unit,
    onScanBarcode: (() -> Unit)? = null,
    recentMeals: List<MealWithItems> = emptyList(),
    onLogAgain: suspend (MealWithItems, Long) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    initialTimestamp: Long = System.currentTimeMillis()
) {
    var timestamp by rememberSaveable { mutableStateOf(initialTimestamp) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var mealTitle by rememberSaveable { mutableStateOf("") }
    val addedItems = rememberSaveable(saver = ManualItemsSaver) { mutableStateListOf<FoodItemEntity>() }
    val saveStatus by viewModel.saveOperation.status.collectAsState()
    val saved by viewModel.isSaved.collectAsState()
    LaunchedEffect(saved) { if (saved) onClose() }

    var showQuick by remember { mutableStateOf(false) }
    val filteredCatalog = remember(searchQuery) {
        FoodCatalog.search(searchQuery)
    }

    if (showQuick) QuickFoodDialog(onDismiss = { showQuick = false }, onAdd = { title, kcal, p ->
        addedItems.add(FoodItemEntity(mealId = "", name = title, portionGrams = 1f, calories = kcal, protein = p, carbs = 0f, fat = 0f))
        showQuick = false
    })
    fun scaleDraft(multiplier: Float) {
        if (saveStatus.busy) return
        val scaled = addedItems.map { it.copy(portionGrams = it.portionGrams * multiplier,
            calories = kotlin.math.round(it.calories * multiplier).toInt(), protein = it.protein * multiplier,
            carbs = it.carbs * multiplier, fat = it.fat * multiplier) }
        addedItems.clear()
        addedItems.addAll(scaled)
    }
    val totalCalories = addedItems.sumOf { it.calories }
    val totalProtein = addedItems.map { it.protein }.sum()
    val totalCarbs = addedItems.map { it.carbs }.sum()
    val totalFat = addedItems.map { it.fat }.sum()

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
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(KaloSurfaceElevated)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = KaloTextPrimary
                    )
                }

                Text(
                    text = "Log meal",
                    style = KaloTypography.titleLarge,
                    color = KaloTextSecondary
                )

                Box(modifier = Modifier.size(48.dp))
            }
        },
        bottomBar = {
            if (addedItems.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(20.dp)
                ) {
                    KaloButton(
                        text = "Log Meal ($totalCalories kcal)",
                        loading = saveStatus.busy,
                        enabled = !saveStatus.busy,
                        onClick = {
                            val mealId = UUID.randomUUID().toString()
                            val meal = MealEntity(
                                id = mealId,
                                title = mealTitle.ifBlank { "Logged Meal" },
                                totalCalories = totalCalories,
                                totalProteinGrams = totalProtein,
                                totalCarbsGrams = totalCarbs,
                                totalFatGrams = totalFat,
                                timestamp = timestamp
                            )
                            val itemsWithMealId = addedItems.map { it.copy(mealId = mealId) }
                            viewModel.save { onSaveMeal(meal, itemsWithMealId) }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            if (addedItems.isNotEmpty()) item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { scaleDraft(0.75f) }, enabled = !saveStatus.busy) { Text("Smaller meal") }
                    OutlinedButton(onClick = { scaleDraft(1.25f) }, enabled = !saveStatus.busy) { Text("Larger meal") }
                }
                Text("Adjust the whole draft, including a repeated meal. Each tap changes it by about a quarter.", style = KaloTypography.bodySmall)
            }

            item { Row {
                TextButton(onClick = onOpenLibrary) { Text("My foods & recipes") }
                TextButton(onClick = { showQuick = true }) { Text("Quick kcal / protein") }
            } }
            // Meal Title Input
            item {
                OutlinedTextField(
                    value = mealTitle,
                    onValueChange = { mealTitle = it },
                    label = { Text("Meal name (optional)", color = KaloTextSecondary) },
                    textStyle = KaloTypography.headlineMedium.copy(color = KaloTextPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KaloProtein,
                        unfocusedBorderColor = KaloBorder,
                        focusedContainerColor = KaloSurface,
                        unfocusedContainerColor = KaloSurface
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            item {
                DateTimeChip(millis = timestamp, onChange = { timestamp = it })
            }

            // Summary of Added Items
            if (addedItems.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(KaloSurfaceElevated, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CURRENT SELECTION",
                                style = KaloTypography.labelSmall,
                                color = KaloProtein
                            )
                            Text(
                                text = "$totalCalories kcal",
                                style = KaloTypography.titleMedium,
                                color = KaloCalories
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${totalProtein.toInt()}g P • ${totalCarbs.toInt()}g C • ${totalFat.toInt()}g F",
                            style = KaloTypography.bodyMedium,
                            color = KaloTextSecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        addedItems.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${item.name} (${item.portionGrams.toInt()}g)",
                                    style = KaloTypography.bodyLarge,
                                    color = KaloTextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${item.calories} kcal",
                                    style = KaloTypography.bodyMedium,
                                    color = KaloTextSecondary
                                )
                                IconButton(
                                    onClick = { addedItems.removeAt(index) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove",
                                        tint = KaloFat,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Search Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search foods...", color = KaloTextMuted) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = KaloTextSecondary
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = KaloProtein,
                            unfocusedBorderColor = KaloBorder,
                            focusedContainerColor = KaloSurface,
                            unfocusedContainerColor = KaloSurface
                        )
                    )

                    if (onScanBarcode != null) {
                        IconButton(
                            onClick = onScanBarcode,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(KaloSurfaceElevated)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CropFree,
                                contentDescription = "Scan Barcode",
                                tint = KaloCarbs,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            saveStatus.error?.let { message ->
                item { Text(message, color = MaterialTheme.colorScheme.error) }
            }
            if (searchQuery.isBlank() && recentMeals.isNotEmpty()) {
                item {
                    Text(
                        text = "RECENT MEALS (TAP TO REVIEW)",
                        style = KaloTypography.labelSmall,
                        color = KaloTextSecondary
                    )
                }
                items(recentMeals, key = { "recent_" + it.meal.id }) { recent ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(KaloSurface)
                            .clickable {
                                if (!saveStatus.busy) {
                                    if (mealTitle.isBlank()) mealTitle = recent.meal.title
                                    addedItems.addAll(recent.items.map { it.copy(id = UUID.randomUUID().toString(), mealId = "") })
                                }
                            }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(recent.meal.title, style = KaloTypography.titleMedium, color = KaloTextPrimary)
                            Text(
                                "${recent.meal.totalProteinGrams.toInt()}g P • ${recent.meal.totalCarbsGrams.toInt()}g C • ${recent.meal.totalFatGrams.toInt()}g F",
                                style = KaloTypography.bodyMedium,
                                color = KaloTextSecondary
                            )
                        }
                        Text("${recent.meal.totalCalories} kcal", style = KaloTypography.bodyLarge, color = KaloCalories)
                    }
                }
            }

            item {
                Text(
                    text = "COMMON FOODS (TAP TO ADD)",
                    style = KaloTypography.labelSmall,
                    color = KaloTextSecondary
                )
            }

            // Catalog Items
            items(filteredCatalog, key = { "catalog_" + it.id }) { catalogItem ->
                FoodCatalogRow(
                    item = catalogItem,
                    onAdd = { grams ->
                        addedItems.add(
                            FoodItemEntity(
                                id = UUID.randomUUID().toString(),
                                mealId = "",
                                name = catalogItem.name,
                                portionGrams = grams,
                                calories = catalogItem.calculateCalories(grams),
                                protein = catalogItem.calculateProtein(grams),
                                carbs = catalogItem.calculateCarbs(grams),
                                fat = catalogItem.calculateFat(grams)
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun FoodCatalogRow(
    item: FoodCatalogItem,
    onAdd: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var gramsText by rememberSaveable(item.id) { mutableStateOf(item.defaultServingGrams.toInt().toString()) }
    val grams = com.kalotracker.app.core.util.parseFoodPortion(gramsText)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(KaloSurface, RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = KaloTypography.titleMedium,
                color = KaloTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${item.caloriesPer100g.toInt()} kcal/100g • ${item.proteinPer100g.toInt()}g P • ${item.carbsPer100g.toInt()}g C • ${item.fatPer100g.toInt()}g F",
                style = KaloTypography.bodyMedium,
                color = KaloTextSecondary
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = gramsText,
                onValueChange = { gramsText = it },
                label = { Text("g") },
                isError = grams == null,
                singleLine = true,
                supportingText = if (grams == null) { { Text("0 < g ≤ 5000") } } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.width(68.dp),
                textStyle = KaloTypography.bodyMedium.copy(color = KaloTextPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = KaloProtein,
                    unfocusedBorderColor = KaloBorder
                ),
                shape = RoundedCornerShape(8.dp)
            )

            IconButton(
                onClick = {
                    grams?.let(onAdd)
                },
                enabled = grams != null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(KaloSurfaceElevated)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    tint = KaloProtein
                )
            }
        }
    }
}

/** Only small draft values go in saved state, never image bytes. */
internal val ManualItemsSaver = listSaver<SnapshotStateList<FoodItemEntity>, Any>(
    save = { items ->
        items.flatMap { listOf(it.id, it.mealId, it.name, it.portionGrams, it.calories,
            it.protein, it.carbs, it.fat, it.confidence) }
    },
    restore = { values ->
        values.chunked(9).map { v ->
            FoodItemEntity(v[0] as String, v[1] as String, v[2] as String,
                v[3] as Float, v[4] as Int, v[5] as Float, v[6] as Float,
                v[7] as Float, v[8] as Float)
        }.toMutableStateList()
    }
)
