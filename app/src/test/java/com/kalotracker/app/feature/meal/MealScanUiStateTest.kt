package com.kalotracker.app.feature.meal

import org.junit.Assert.assertEquals
import org.junit.Test

class MealScanUiStateTest {

    @Test
    fun testEmptyStateTotalsAreZero() {
        val state = MealScanUiState()
        assertEquals(0, state.totalCalories)
        assertEquals(0f, state.totalProtein, 0.01f)
        assertEquals(0f, state.totalCarbs, 0.01f)
        assertEquals(0f, state.totalFat, 0.01f)
    }

    @Test
    fun testAddedOilIncreasesCaloriesAndFat() {
        val state = MealScanUiState(hasAddedOil = true)
        assertEquals(120, state.totalCalories)
        assertEquals(0f, state.totalProtein, 0.01f)
        assertEquals(0f, state.totalCarbs, 0.01f)
        assertEquals(14f, state.totalFat, 0.01f)
    }

    @Test
    fun testItemSummationWithAddedOil() {
        val item1 = EditableFoodItem(
            name = "Chicken Breast",
            portionGrams = 200f,
            baseCaloriesPerGram = 1.65f,
            baseProteinPerGram = 0.31f,
            baseCarbsPerGram = 0f,
            baseFatPerGram = 0.036f,
            confidence = 0.95f
        )
        val item2 = EditableFoodItem(
            name = "Brown Rice",
            portionGrams = 150f,
            baseCaloriesPerGram = 1.23f,
            baseProteinPerGram = 0.027f,
            baseCarbsPerGram = 0.25f,
            baseFatPerGram = 0.01f,
            confidence = 0.9f
        )

        val state = MealScanUiState(
            items = listOf(item1, item2),
            hasAddedOil = true
        )

        // item1: 330 kcal, 62g P, 0g C, 7.2g F
        // item2: 184 kcal, 4.05g P, 37.5g C, 1.5g F
        // addedOil: 120 kcal, 0g P, 0g C, 14g F
        // Total kcal = 330 + 184 + 120 = 634
        assertEquals(634, state.totalCalories)
        assertEquals(66.05f, state.totalProtein, 0.1f)
        assertEquals(37.5f, state.totalCarbs, 0.1f)
        assertEquals(22.7f, state.totalFat, 0.1f)
    }
}
