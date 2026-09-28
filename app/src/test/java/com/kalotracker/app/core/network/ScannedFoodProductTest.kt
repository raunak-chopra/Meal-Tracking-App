package com.kalotracker.app.core.network

import org.junit.Assert.assertEquals
import org.junit.Test

class ScannedFoodProductTest {

    private val sampleProduct = ScannedFoodProduct(
        barcode = "737628064502",
        name = "Pure Whey Protein",
        brand = "Optimum Nutrition",
        servingSizeGrams = 32f,
        caloriesPer100g = 375,
        proteinPer100g = 78.0f,
        carbsPer100g = 6.0f,
        fatPer100g = 3.0f
    )

    @Test
    fun testBase100GramCalculations() {
        assertEquals(375, sampleProduct.calculateCalories(100f))
        assertEquals(78.0f, sampleProduct.calculateProtein(100f), 0.01f)
        assertEquals(6.0f, sampleProduct.calculateCarbs(100f), 0.01f)
        assertEquals(3.0f, sampleProduct.calculateFat(100f), 0.01f)
    }

    @Test
    fun testSingleScoopPortionScaling() {
        // 32 grams scoop
        val calories = sampleProduct.calculateCalories(32f)
        val protein = sampleProduct.calculateProtein(32f)
        val carbs = sampleProduct.calculateCarbs(32f)
        val fat = sampleProduct.calculateFat(32f)

        assertEquals(120, calories)
        assertEquals(24.96f, protein, 0.01f)
        assertEquals(1.92f, carbs, 0.01f)
        assertEquals(0.96f, fat, 0.01f)
    }

    @Test
    fun testDoublePortionScaling() {
        // 200 grams
        assertEquals(750, sampleProduct.calculateCalories(200f))
        assertEquals(156.0f, sampleProduct.calculateProtein(200f), 0.01f)
        assertEquals(12.0f, sampleProduct.calculateCarbs(200f), 0.01f)
        assertEquals(6.0f, sampleProduct.calculateFat(200f), 0.01f)
    }

    @Test
    fun testZeroGramHandling() {
        assertEquals(0, sampleProduct.calculateCalories(0f))
        assertEquals(0f, sampleProduct.calculateProtein(0f), 0.01f)
    }
}
