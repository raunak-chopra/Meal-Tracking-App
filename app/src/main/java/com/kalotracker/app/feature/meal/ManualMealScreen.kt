package com.kalotracker.app.feature.meal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ManualMealScreen(
    viewModel: ManualMealViewModel,
    onClose: () -> Unit,
    onOpenLibrary: () -> Unit,
    onSaveMeal: suspend (MealEntity, List<FoodItemEntity>) -> Unit,
    onScanBarcode: (() -> Unit)? = null,
    recentMeals: List<MealWithItems> = emptyList(),
    recentMealsLoading: Boolean = false,
    recentMealsError: String? = null,
    onRetryRecentMeals: () -> Unit = {},
    onLogAgain: suspend (MealWithItems, Long) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    initialTimestamp: Long = System.currentTimeMillis(),
    aiConfigured: Boolean = viewModel.canEstimate,
    onOpenAiSettings: () -> Unit = {}
) {
    var timestamp by rememberSaveable { mutableStateOf(initialTimestamp) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var mealTitle by rememberSaveable { mutableStateOf("") }
    var recentDisplayLimit by rememberSaveable(searchQuery) { mutableIntStateOf(12) }
    val snackbar = remember { SnackbarHostState() }
    val feedbackScope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val addedItems = rememberSaveable(saver = ManualItemsSaver) { mutableStateListOf<FoodItemEntity>() }
    val saveStatus by viewModel.saveOperation.status.collectAsState()
    val saved by viewModel.isSaved.collectAsState()
    LaunchedEffect(saved) { if (saved) onClose() }

    var description by rememberSaveable { mutableStateOf("") }
    var estimateNote by rememberSaveable { mutableStateOf<String?>(null) }
    val estimating by viewModel.estimating.collectAsState()
    val estimateError by viewModel.estimateError.collectAsState()
    val pendingEstimate by viewModel.estimate.collectAsState()
    LaunchedEffect(pendingEstimate) {
        val estimate = pendingEstimate ?: return@LaunchedEffect
        addedItems.addAll(estimate.items)
        if (mealTitle.isBlank()) mealTitle = estimate.title
        estimateNote = estimate.notes
        description = ""
        viewModel.consumeEstimate()
    }

    var showQuick by remember { mutableStateOf(false) }
    val searchResults = remember(searchQuery, recentMeals) {
        mealSearchResults(recentMeals, searchQuery)
    }

    if (showQuick) QuickFoodDialog(onDismiss = { showQuick = false }, onAdd = { title, kcal, p ->
        addedItems.add(FoodItemEntity(mealId = "", name = title, portionGrams = 1f, calories = kcal, protein = p, carbs = 0f, fat = 0f, unit = "serving"))
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
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    editingIndex?.let { index ->
        addedItems.getOrNull(index)?.let { item ->
            AmountDialog(item, onDismiss = { editingIndex = null }) { amount ->
                addedItems[index] = item.withAmount(amount)
                editingIndex = null
            }
        }
    }
    val totalCalories = addedItems.sumOf { it.calories }
    val totalProtein = addedItems.map { it.protein }.sum()
    val totalCarbs = addedItems.map { it.carbs }.sum()
    val totalFat = addedItems.map { it.fat }.sum()

    var draftExpanded by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = KaloBackground,
        snackbarHost = { SnackbarHost(snackbar) },
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
                    color = KaloTextPrimary
                )

                DateTimeChip(millis = timestamp, onChange = { timestamp = it })
            }
        },
        bottomBar = {
            if (addedItems.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = KaloSurfaceElevated,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { draftExpanded = !draftExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "In this meal · ${addedItems.size} ${if (addedItems.size == 1) "item" else "items"}",
                                    style = KaloTypography.titleMedium,
                                    color = KaloTextPrimary
                                )
                                Text(
                                    text = "P ${kotlin.math.round(totalProtein).toInt()} g  ·  C ${kotlin.math.round(totalCarbs).toInt()} g  ·  F ${kotlin.math.round(totalFat).toInt()} g",
                                    style = KaloTypography.bodyMedium.copy(fontFeatureSettings = TabularFontFeature),
                                    color = KaloTextSecondary
                                )
                            }
                            TextButton(onClick = { draftExpanded = !draftExpanded }) {
                                Text(if (draftExpanded) "Hide items" else "Review items")
                            }
                        }

                        if (draftExpanded) {
                            OutlinedTextField(
                                value = mealTitle,
                                onValueChange = { mealTitle = it },
                                label = { Text("Meal name (optional)", color = KaloTextSecondary) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = KaloProtein,
                                    unfocusedBorderColor = KaloBorder,
                                    focusedContainerColor = KaloSurface,
                                    unfocusedContainerColor = KaloSurface
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { scaleDraft(0.75f) }, enabled = !saveStatus.busy) { Text("Smaller (-25%)") }
                                OutlinedButton(onClick = { scaleDraft(1.25f) }, enabled = !saveStatus.busy) { Text("Larger (+25%)") }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                addedItems.forEachIndexed { index, item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(KaloSurface)
                                            .clickable(enabled = !saveStatus.busy && item.unit != "serving") { editingIndex = index }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (item.unit == "serving") item.name else "${item.name} · ${formatAmount(item.portionGrams, item.unit)}",
                                            style = KaloTypography.bodyLarge,
                                            color = KaloTextPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "${item.calories} kcal",
                                            style = KaloTypography.bodyMedium.copy(fontFeatureSettings = TabularFontFeature),
                                            color = KaloTextSecondary
                                        )
                                        IconButton(
                                            onClick = { addedItems.removeAt(index) },
                                            enabled = !saveStatus.busy,
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove ${item.name}",
                                                tint = KaloTextSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        KaloButton(
                            text = "Save meal · $totalCalories kcal",
                            loading = saveStatus.busy,
                            enabled = !saveStatus.busy && !estimating,
                            onClick = {
                                snackbar.currentSnackbarData?.dismiss()
                                val mealId = UUID.randomUUID().toString()
                                val meal = MealEntity(
                                    id = mealId,
                                    title = mealTitle.ifBlank { addedItems.first().name },
                                    totalCalories = totalCalories,
                                    totalProteinGrams = totalProtein,
                                    totalCarbsGrams = totalCarbs,
                                    totalFatGrams = totalFat,
                                    notes = estimateNote?.takeIf { it.isNotBlank() }?.let { "AI text estimate: $it" },
                                    timestamp = timestamp
                                )
                                val itemsWithMealId = addedItems.map { it.copy(mealId = mealId) }
                                viewModel.save { onSaveMeal(meal, itemsWithMealId) }
                            }
                        )
                    }
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
            stickyHeader(key = "meal-search") {
                Row(
                    modifier = Modifier.fillMaxWidth().background(KaloBackground).padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search meals & foods") },
                        placeholder = { Text("Name or ingredient") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = KaloTextSecondary) },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, "Clear search") } }
                        } else null,
                        singleLine = true,
                        enabled = !saveStatus.busy,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = KaloProtein, unfocusedBorderColor = KaloBorder,
                            focusedContainerColor = KaloSurface, unfocusedContainerColor = KaloSurface
                        )
                    )
                    if (onScanBarcode != null) {
                        IconButton(onClick = onScanBarcode, enabled = !saveStatus.busy,
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(KaloSurfaceElevated)) {
                            Icon(Icons.Default.CropFree, "Scan a barcode", tint = KaloCarbs, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = onOpenLibrary,
                        enabled = !saveStatus.busy,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = KaloSurfaceElevated,
                            contentColor = KaloTextPrimary
                        )
                    ) { Text("My foods & recipes") }
                    OutlinedButton(
                        onClick = { showQuick = true },
                        enabled = !saveStatus.busy,
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Quick kcal entry") }
                }
            }

            if (searchQuery.isBlank() && aiConfigured) item {
                DescribeMealCard(
                    description = description,
                    onDescriptionChange = { description = it.take(500) },
                    estimating = estimating,
                    error = estimateError,
                    note = estimateNote,
                    onEstimate = { viewModel.estimate(description) },
                    enabled = !saveStatus.busy
                )
            } else if (searchQuery.isBlank()) item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Photo and text estimates are optional", style = KaloTypography.titleMedium, color = KaloTextPrimary)
                    Text("Search recent meals and common foods without an AI key. Connect your Gemini key to estimate a description.", style = KaloTypography.bodyMedium, color = KaloTextSecondary)
                    TextButton(onClick = onOpenAiSettings) { Text("Set up AI estimates") }
                }
            }

            saveStatus.error?.let { message ->
                item { Text(message, color = MaterialTheme.colorScheme.error) }
            }
            if (recentMealsLoading) item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Loading recent meals…", style = KaloTypography.bodyMedium, color = KaloTextSecondary)
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
            recentMealsError?.let { message -> item {
                Column {
                    Text(message, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRetryRecentMeals, enabled = !recentMealsLoading) { Text("Retry recent meals") }
                }
            } }
            if (searchResults.recentMeals.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent meals · ${searchResults.recentMeals.size}",
                        style = KaloTypography.titleMedium,
                        color = KaloTextPrimary
                    )
                }
                items(searchResults.recentMeals.take(recentDisplayLimit), key = { "recent_" + it.meal.id }) { recent ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(KaloSurface)
                            .clickable(enabled = !saveStatus.busy, role = Role.Button,
                                onClickLabel = "Add ${recent.meal.title} to this meal") {
                                    val copied = recent.copyItemsForDraft()
                                    val ids = copied.map { it.id }.toSet()
                                    val namedAutomatically = mealTitle.isBlank()
                                    if (namedAutomatically) mealTitle = recent.meal.title
                                    addedItems.addAll(copied)
                                    keyboard?.hide()
                                    snackbar.currentSnackbarData?.dismiss()
                                    feedbackScope.launch {
                                        if (viewModel.saveOperation.status.value.busy || viewModel.isSaved.value) return@launch
                                        if (snackbar.showSnackbar("${recent.meal.title} added to this meal", actionLabel = "Undo",
                                                withDismissAction = true, duration = SnackbarDuration.Indefinite) == SnackbarResult.ActionPerformed &&
                                            !viewModel.saveOperation.status.value.busy && !viewModel.isSaved.value) {
                                            addedItems.removeAll { it.id in ids }
                                            if (namedAutomatically && addedItems.isEmpty() && mealTitle == recent.meal.title) mealTitle = ""
                                        }
                                    }
                            }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(recent.meal.title, style = KaloTypography.titleMedium, color = KaloTextPrimary,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(
                                recent.items.joinToString(" · ") { "${it.name} (${formatAmount(it.portionGrams, it.unit)})" },
                                style = KaloTypography.bodyMedium,
                                color = KaloTextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis
                            )
                            Text("Last logged ${java.time.Instant.ofEpochMilli(recent.meal.timestamp).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm"))}",
                                style = KaloTypography.bodySmall, color = KaloTextMuted)
                        }
                        Text("${recent.meal.totalCalories} kcal", style = KaloTypography.bodyLarge, color = KaloCalories)
                    }
                }
                if (searchResults.recentMeals.size > recentDisplayLimit) item {
                    TextButton(onClick = { recentDisplayLimit += 12 }) { Text("Show more recent meals") }
                }
            } else if (!recentMealsLoading && recentMealsError == null && searchResults.commonFoods.isNotEmpty()) item {
                Text(if (searchQuery.isBlank()) "Meals with food items appear here after logging."
                    else "No matching recent meals. Common foods are shown below.",
                    style = KaloTypography.bodyMedium, color = KaloTextSecondary)
            }

            if (searchResults.commonFoods.isNotEmpty()) item {
                Text(
                    text = "Common foods · ${searchResults.commonFoods.size}",
                    style = KaloTypography.titleMedium,
                    color = KaloTextPrimary
                )
            }
            if (searchResults.commonFoods.isEmpty() && searchResults.recentMeals.isEmpty() && !recentMealsLoading && recentMealsError == null) item {
                Column(Modifier.fillMaxWidth().background(KaloSurface, RoundedCornerShape(16.dp)).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("No matching meals or foods", style = KaloTypography.titleMedium)
                    Text("Try a meal name or ingredient. Recent history includes up to 200 meal records; common foods are a limited built-in list.",
                        style = KaloTypography.bodyMedium, color = KaloTextSecondary)
                    TextButton(onClick = { searchQuery = "" }) { Text("Clear search") }
                }
            }

            // Catalog Items
            items(searchResults.commonFoods, key = { "catalog_" + it.id }) { catalogItem ->
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

/** Type a meal in your own words; Gemini lists the foods with approximate nutrition. */
@Composable
private fun DescribeMealCard(
    description: String,
    onDescriptionChange: (String) -> Unit,
    estimating: Boolean,
    error: String?,
    note: String?,
    onEstimate: () -> Unit,
    enabled: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(KaloSurface, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Describe what you ate", style = KaloTypography.titleMedium, color = KaloTextPrimary)
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Food description") },
            placeholder = { Text("I ate half a chicken at Nando's with peri-peri chips", color = KaloTextMuted) },
            enabled = enabled && !estimating,
            minLines = 2,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = KaloProtein,
                unfocusedBorderColor = KaloBorder
            )
        )
        Text(
            "No photo needed. Include the restaurant, dish and amount if you know them. Your description is sent to Google Gemini to estimate calories, protein, carbs and fat; review before saving.",
            style = KaloTypography.bodySmall,
            color = KaloTextSecondary
        )
        Button(
            onClick = onEstimate,
            enabled = enabled && !estimating && description.isNotBlank(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            if (estimating) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
                Spacer(Modifier.width(8.dp))
                Text("Estimating…")
            } else {
                Text("Estimate nutrition")
            }
        }
        error?.let { Text(it, style = KaloTypography.bodyMedium, color = MaterialTheme.colorScheme.error) }
        note?.let { Text("Assumed: $it", style = KaloTypography.bodySmall, color = KaloTextSecondary) }
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
                text = "${item.caloriesPer100g.toInt()} kcal per 100 g  ·  P ${item.proteinPer100g.toInt()}  C ${item.carbsPer100g.toInt()}  F ${item.fatPer100g.toInt()}",
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
                supportingText = if (grams == null) { { Text("1–5000") } } else null,
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
                    contentDescription = "Add ${item.name}",
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
            it.protein, it.carbs, it.fat, it.confidence, it.unit) }
    },
    restore = { values ->
        values.chunked(10).map { v ->
            FoodItemEntity(v[0] as String, v[1] as String, v[2] as String,
                v[3] as Float, v[4] as Int, v[5] as Float, v[6] as Float,
                v[7] as Float, v[8] as Float, v[9] as String)
        }.toMutableStateList()
    }
)

/** Same food at a new amount; nutrition scales with it. */
internal fun FoodItemEntity.withAmount(amount: Float): FoodItemEntity {
    require(amount.isFinite() && amount > 0f && portionGrams > 0f)
    val scale = amount / portionGrams
    return copy(portionGrams = amount, calories = kotlin.math.round(calories * scale).toInt(),
        protein = protein * scale, carbs = carbs * scale, fat = fat * scale)
}

@Composable
private fun AmountDialog(item: FoodItemEntity, onDismiss: () -> Unit, onApply: (Float) -> Unit) {
    var text by remember(item.id) { mutableStateOf(formatAmount(item.portionGrams, "").trim()) }
    val amount = com.kalotracker.app.core.util.parseFoodPortion(text)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.name) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                label = { Text("Amount") },
                suffix = { Text(item.unit) },
                isError = amount == null,
                supportingText = { Text(amount?.let { "About ${item.withAmount(it).calories} kcal" } ?: "1–5000") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        },
        confirmButton = { TextButton(onClick = { amount?.let(onApply) }, enabled = amount != null) { Text("Update") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
