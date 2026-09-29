package com.kalotracker.app.core.data.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodCatalogTest {

    @Test
    fun testEmptySearchReturnsAllCatalogItems() {
        val allItems = FoodCatalog.search("")
        assertTrue("Catalog should contain comprehensive staple items", allItems.size >= 20)
    }

    @Test
    fun testSearchByKeywordFindsRelevantFoods() {
        val eggResults = FoodCatalog.search("egg")
        assertTrue("Should return items for 'egg'", eggResults.isNotEmpty())
        assertTrue("Every result should match 'egg' case-insensitively", eggResults.all { it.name.contains("egg", ignoreCase = true) })
    }

    @Test
    fun testSearchIsCaseInsensitive() {
        val uppercaseResults = FoodCatalog.search("SALMON")
        val lowercaseResults = FoodCatalog.search("salmon")
        assertEquals("Case should not affect search count", uppercaseResults.size, lowercaseResults.size)
        assertTrue("Should find Atlantic Salmon", uppercaseResults.any { it.name.contains("Salmon", ignoreCase = true) })
    }

    @Test
    fun testNonExistentFoodReturnsEmptyList() {
        val emptyResults = FoodCatalog.search("quantum_marshmallow_999")
        assertTrue("Unknown food should return empty list", emptyResults.isEmpty())
    }

    @Test
    fun testCatalogItemPropertiesArePositive() {
        val allItems = FoodCatalog.search("")
        for (item in allItems) {
            assertTrue("Default grams must be positive for ${item.name}", item.defaultServingGrams > 0)
            assertTrue("Calories must be >= 0 for ${item.name}", item.caloriesPer100g >= 0)
            assertTrue("Protein must be >= 0 for ${item.name}", item.proteinPer100g >= 0)
            assertTrue("Carbs must be >= 0 for ${item.name}", item.carbsPer100g >= 0)
            assertTrue("Fat must be >= 0 for ${item.name}", item.fatPer100g >= 0)
        }
    }
}
