package com.kalotracker.app.feature.meal

import com.kalotracker.app.core.data.food.FoodCatalog
import com.kalotracker.app.core.data.food.FoodCatalogItem
import com.kalotracker.app.core.data.food.matchesSearch
import com.kalotracker.app.core.database.dao.MealWithItems
import com.kalotracker.app.core.database.entity.FoodItemEntity
import com.kalotracker.app.core.util.SearchText
import java.util.UUID

internal data class MealSearchResults(
    val recentMeals: List<MealWithItems>,
    val commonFoods: List<FoodCatalogItem>
)

internal fun mealSearchResults(recentMeals: List<MealWithItems>, query: String) = MealSearchResults(
    recentMeals = recentMeals.asSequence()
        .filter { it.items.isNotEmpty() }
        .filter { it.matchesSearch(query) }
        .sortedByDescending { it.meal.timestamp }
        .distinctBy { it.repeatSnapshot() }
        .toList(),
    commonFoods = FoodCatalog.search(query)
)

private data class RepeatSnapshot(val title: String, val notes: String?, val items: List<FoodItemEntity>)

/** Hide exact repeats, while retaining different portions, units, notes or nutrient snapshots. */
private fun MealWithItems.repeatSnapshot() = RepeatSnapshot(
    SearchText.normalized(meal.title), meal.notes,
    items.map { it.copy(id = "", mealId = "", name = SearchText.normalized(it.name)) }
        .sortedWith(compareBy({ it.name }, { it.portionGrams }, { it.unit }, { it.calories },
            { it.protein }, { it.carbs }, { it.fat }, { it.confidence }))
)

/** Copy into an unsaved draft; the historical row and its nutrient/unit snapshot stay untouched. */
internal fun MealWithItems.copyItemsForDraft(): List<FoodItemEntity> =
    items.map { it.copy(id = UUID.randomUUID().toString(), mealId = "") }
