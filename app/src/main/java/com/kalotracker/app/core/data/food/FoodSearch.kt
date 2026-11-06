package com.kalotracker.app.core.data.food

import com.kalotracker.app.core.database.dao.MealWithItems
import com.kalotracker.app.core.database.entity.SavedFoodEntity
import com.kalotracker.app.core.util.SearchText
import kotlinx.serialization.SerializationException

internal fun MealWithItems.matchesSearch(query: String): Boolean =
    SearchText.matches(query, listOf(meal.title) + items.map { it.name })

/** Decode ingredient names once per library update, rather than on every keystroke. */
internal class SavedFoodSearch(foods: List<SavedFoodEntity>) {
    private val entries = foods.map { food ->
        val ingredients = try { PersonalFood.ingredients(food).map { it.name } }
        catch (_: SerializationException) { emptyList() }
        catch (_: IllegalArgumentException) { emptyList() }
        food to (listOf(food.name) + ingredients)
    }

    fun search(query: String, filter: String): List<SavedFoodEntity> = entries.filter { (food, names) ->
        val matchesFilter = when (filter) {
            "FAVORITES" -> food.favorite
            "FOOD", "RECIPE", "TEMPLATE" -> food.kind == filter
            else -> true
        }
        matchesFilter && SearchText.matches(query, names)
    }.map { it.first }
}
