package com.kalotracker.app.core.data.food

import com.kalotracker.app.core.database.dao.MealWithItems
import com.kalotracker.app.core.database.entity.MealEntity
import com.kalotracker.app.core.database.entity.FoodItemEntity
import com.kalotracker.app.core.database.entity.SavedFoodEntity
import com.kalotracker.app.core.util.SearchText
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class FoodSearchTest {
    private fun food(id: String = "recipe", kind: String = "RECIPE", favorite: Boolean = true) =
        SavedFoodEntity(id = id, name = "Weekday bowl", kind = kind, calories = 300, protein = 10f,
            carbs = 40f, fat = 10f, favorite = favorite,
            ingredientsJson = PersonalFood.json.encodeToString(listOf(
                Ingredient("Paneer curry", 100f, 150, 5f, 20f, 5f),
                Ingredient("Jeera rice", 100f, 150, 5f, 20f, 5f))))

    @Test fun diaryMatchesTitleAndIngredientsTogetherWithoutChangingProvidedDayScope() {
        val meal = MealWithItems(MealEntity(id = "today", title = "Weekday bowl", totalCalories = 300,
            totalProteinGrams = 10f, totalCarbsGrams = 40f, totalFatGrams = 10f),
            listOf(FoodItemEntity(mealId = "today", name = "Paneer curry", portionGrams = 100f, calories = 300,
                protein = 10f, carbs = 40f, fat = 10f)))
        assertTrue(meal.matchesSearch("  CURRY  weekday "))
        assertFalse(meal.matchesSearch("paneer chicken"))
        assertTrue(meal.copy(items = emptyList()).matchesSearch("weekday"))
        assertTrue(meal.matchesSearch("  "))
        assertFalse(meal.matchesSearch("%_"))
    }

    @Test fun savedRecipesMatchReorderedNamesAndDifferentIngredients() {
        val index = SavedFoodSearch(listOf(food()))
        assertEquals("recipe", index.search(" RICE   Paneer ", "ALL").single().id)
        assertEquals("recipe", index.search("bowl weekday", "ALL").single().id)
        assertTrue(index.search("paneer chicken", "ALL").isEmpty())
    }

    @Test fun librarySearchKeepsTypeAndFavoriteFiltersAndClearsWithoutLosingEntries() {
        val entries = listOf(food(), food("plain", "FOOD", false), food("usual", "TEMPLATE", false))
        val index = SavedFoodSearch(entries)
        assertEquals(listOf("recipe"), index.search("paneer", "FAVORITES").map { it.id })
        assertEquals(listOf("usual"), index.search("paneer", "TEMPLATE").map { it.id })
        assertEquals(listOf("plain"), index.search("", "FOOD").map { it.id })
        assertEquals(entries, index.search(" ", "ALL"))
        assertTrue(index.search("%_", "ALL").isEmpty())
        assertTrue(SavedFoodSearch(emptyList()).search("", "ALL").isEmpty())
    }

    @Test fun malformedStoredIngredientsRemainFindableByNameWithoutCrashingSearch() {
        val broken = food().copy(ingredientsJson = "not json")
        val index = SavedFoodSearch(listOf(broken))
        assertEquals(broken, index.search("bowl", "ALL").single())
        assertTrue(index.search("paneer", "ALL").isEmpty())
        assertEquals("not json", broken.ingredientsJson)
    }

    @Test fun searchIsLocaleIndependentAndPreservesNonLatinWords() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertTrue(SearchText.matches("RICE", listOf("rice")))
            assertTrue(SearchText.matches("चावल", listOf("जीरा चावल")))
            assertTrue(SearchText.matches("rice", listOf("ＲＩＣＥ")))
        } finally { Locale.setDefault(previous) }
    }

    @Test fun commonCatalogHasConsistentReorderedCategoryMatchingAndHonestMisses() {
        assertTrue(FoodCatalog.search("  rice  cooked ").any { it.id == "jasmine_rice" })
        assertEquals(FoodCatalog.commonFoods, FoodCatalog.search(""))
        assertTrue(FoodCatalog.search("zzqqnothere").isEmpty())
        assertTrue(FoodCatalog.search("!!!").isEmpty())
    }
}
