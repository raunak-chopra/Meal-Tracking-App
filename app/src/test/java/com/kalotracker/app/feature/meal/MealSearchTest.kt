package com.kalotracker.app.feature.meal

import com.kalotracker.app.core.database.dao.MealWithItems
import com.kalotracker.app.core.database.entity.FoodItemEntity
import com.kalotracker.app.core.database.entity.MealEntity
import org.junit.Assert.*
import org.junit.Test

class MealSearchTest {
    private fun lunch() = MealWithItems(
        MealEntity(id = "lunch", title = "Weekday lunch", totalCalories = 480,
            totalProteinGrams = 25f, totalCarbsGrams = 50f, totalFatGrams = 15f, timestamp = 100),
        listOf(FoodItemEntity(id = "chicken", mealId = "lunch", name = "Chicken bharta", portionGrams = 200f,
            calories = 350, protein = 22f, carbs = 22f, fat = 14f),
            FoodItemEntity(id = "rice", mealId = "lunch", name = "Jeera rice", portionGrams = 100f,
                calories = 130, protein = 3f, carbs = 28f, fat = 1f))
    )

    @Test fun searchingByIngredientKeepsTheMatchingRecentMeal() {
        val results = mealSearchResults(listOf(lunch()), "chicken")
        assertEquals(listOf("lunch"), results.recentMeals.map { it.meal.id })
        assertTrue(results.commonFoods.any { it.id == "chicken_breast" })
    }

    @Test fun searchingByMealNameKeepsRecentMealsOutsideTheCommonFoodCatalog() {
        assertEquals("lunch", mealSearchResults(listOf(lunch()), "Weekday").recentMeals.single().meal.id)
    }

    @Test fun queryWordsCanMatchDifferentIngredientsInAnyOrderWithExtraWhitespace() {
        assertEquals("lunch", mealSearchResults(listOf(lunch()), "  RICE\t chicken  ").recentMeals.single().meal.id)
        assertTrue(mealSearchResults(listOf(lunch()), "chicken banana").recentMeals.isEmpty())
        assertTrue(mealSearchResults(emptyList(), "rice cooked").commonFoods.any { it.id == "jasmine_rice" })
    }

    @Test fun matchingOlderMealsHappensBeforeAnyTwelveRowDisplayLimit() {
        val filler = (0 until 20).map { index -> lunch().let { it.copy(
            meal = it.meal.copy(id = "snack-$index", title = "Snack $index", timestamp = 1000L + index),
            items = listOf(it.items.first().copy(name = "Fixture snack"))) } }
        assertEquals(listOf("lunch"), mealSearchResults(filler + lunch(), "chicken").recentMeals.map { it.meal.id })
        assertTrue(mealSearchResults(filler + lunch(), "").recentMeals.size > 12)
    }

    @Test fun differentFoodsWithTheSameMealTitleStaySearchable() {
        val chicken = lunch()
        val paneer = chicken.copy(meal = chicken.meal.copy(id = "paneer", timestamp = 99),
            items = chicken.items.map { if (it.id == "chicken") it.copy(name = "Paneer bharta") else it })
        assertEquals(listOf("paneer"), mealSearchResults(listOf(chicken, paneer), "paneer").recentMeals.map { it.meal.id })
        assertEquals(2, mealSearchResults(listOf(chicken, paneer), "Weekday").recentMeals.size)
    }

    @Test fun exactRepeatSnapshotsCollapseToTheNewestEvenWhenItemOrderAndIdsDiffer() {
        val original = lunch()
        val repeated = original.copy(meal = original.meal.copy(id = "latest", timestamp = 101),
            items = original.items.reversed().map { it.copy(id = "new-${it.id}", mealId = "latest") })
        assertEquals(listOf("latest"), mealSearchResults(listOf(original, repeated), "rice").recentMeals.map { it.meal.id })
    }

    @Test fun differentAmountsUnitsAndNutritionDoNotCollapseIntoOneSuggestion() {
        val original = lunch()
        val variants = listOf(
            original,
            original.copy(meal = original.meal.copy(id = "amount"), items = original.items.map { it.copy(portionGrams = it.portionGrams + 1) }),
            original.copy(meal = original.meal.copy(id = "unit"), items = original.items.map { it.copy(unit = "ml") }),
            original.copy(meal = original.meal.copy(id = "nutrition"), items = original.items.map { it.copy(protein = it.protein + 1) })
        )
        assertEquals(4, mealSearchResults(variants, "rice").recentMeals.size)
    }

    @Test fun clearingQueryRestoresChoicesButPunctuationAndUnknownTermsAreNotWildcards() {
        assertTrue(mealSearchResults(listOf(lunch()), "quantum-marshmallow").let { it.recentMeals.isEmpty() && it.commonFoods.isEmpty() })
        assertTrue(mealSearchResults(listOf(lunch()), "%_#").let { it.recentMeals.isEmpty() && it.commonFoods.isEmpty() })
        val cleared = mealSearchResults(listOf(lunch()), " \t ")
        assertEquals(1, cleared.recentMeals.size)
        assertEquals(com.kalotracker.app.core.data.food.FoodCatalog.commonFoods.size, cleared.commonFoods.size)
        assertTrue(mealSearchResults(listOf(lunch().copy(items = emptyList())), "").recentMeals.isEmpty())
    }

    @Test fun draftCopiesKeepSnapshotsAndUnitsButGetIndependentIds() {
        val original = lunch().let { it.copy(items = it.items.map { row -> row.copy(unit = "ml") }) }
        val first = original.copyItemsForDraft()
        val second = original.copyItemsForDraft()
        assertTrue(first.all { it.mealId.isEmpty() })
        assertTrue(first.map { it.id }.toSet().intersect(second.map { it.id }.toSet()).isEmpty())
        first.zip(original.items).forEach { (copy, source) ->
            assertNotEquals(source.id, copy.id)
            assertEquals(source, copy.copy(id = source.id, mealId = source.mealId))
        }
    }
}
