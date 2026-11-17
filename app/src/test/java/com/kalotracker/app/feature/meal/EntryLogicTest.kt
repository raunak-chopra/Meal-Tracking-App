package com.kalotracker.app.feature.meal

import com.kalotracker.app.core.database.entity.FoodItemEntity
import com.kalotracker.app.core.network.OpenFoodFactsService
import com.kalotracker.app.feature.workout.workoutEnergy
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fictional foods; checks the arithmetic behind each way of adding food. */
class EntryLogicTest {

    @Test fun quickEntryRejectsProteinWorthMoreThanTheFood() {
        assertNull(quickEntryProblem("Protein bar", 200, 20f))
        assertNull(quickEntryProblem("Black coffee", 5, 0f))
        assertNotNull(quickEntryProblem("Snack", 100, 50f)) // 50 g protein is 200 kcal
        assertNotNull(quickEntryProblem("", 100, 0f))
        assertNotNull(quickEntryProblem("Snack", null, 0f))
    }

    @Test fun macroMismatchIsFlaggedOnlyWhenItMatters() {
        // 20 g protein, 30 g carbs, 10 g fat = 290 kcal
        assertNull(macroCalorieMismatch(290, 20f, 30f, 10f))
        assertNull(macroCalorieMismatch(300, 20f, 30f, 10f)) // label rounding
        assertEquals(290, macroCalorieMismatch(150, 20f, 30f, 10f))
    }

    @Test fun changingADraftAmountScalesItsNutrition() {
        val rice = FoodItemEntity("r", "", "Rice", 150f, 195, 4f, 42f, 0.5f)
        val more = rice.withAmount(300f)
        assertEquals(300f, more.portionGrams, 0f)
        assertEquals(390, more.calories)
        assertEquals(8f, more.protein, 0.001f)
        assertEquals(84f, more.carbs, 0.001f)
    }

    @Test fun workoutEnergyFollowsBodyWeightAndTimeNotSetCount() {
        // Resistance training 3.5 MET, 70 kg, 45 min = 193 kcal
        assertEquals(193, workoutEnergy("Bench Press", false, 45, 70.0))
        assertEquals(276, workoutEnergy("Bench Press", false, 45, 100.0))
        assertTrue(workoutEnergy("Outdoor Run", true, 30, 70.0) > workoutEnergy("Bench Press", false, 30, 70.0))
        assertEquals(0, workoutEnergy("Bench Press", false, 0, 70.0))
    }

    @Test fun drinksFromOpenFoodFactsAreMeasuredInMillilitres() {
        fun unit(json: String) = OpenFoodFactsService.productUnit(JSONObject(json))
        assertEquals("ml", unit("""{"serving_quantity_unit":"ml"}"""))
        assertEquals("ml", unit("""{"nutrition_data_per":"100ml"}"""))
        assertEquals("ml", unit("""{"quantity":"1.5 l"}"""))
        assertEquals("ml", unit("""{"quantity":"330ml"}"""))
        assertEquals("g", unit("""{"quantity":"200 g"}"""))
        assertEquals("g", unit("""{"serving_quantity_unit":"g","quantity":"1 l"}"""))
        assertEquals("g", unit("""{}"""))
    }

    @Test fun barcodeCaloriesRoundToTheNearestKcal() {
        val p = com.kalotracker.app.core.network.ScannedFoodProduct("1", "Biscuit", caloriesPer100g = 487)
        assertEquals(49, p.calculateCalories(10f)) // 48.7
    }

    @Test fun mealSourceComesFromWhatWasSaved() {
        val base = com.kalotracker.app.core.database.entity.MealEntity(title = "Lunch", totalCalories = 500,
            totalProteinGrams = 20f, totalCarbsGrams = 60f, totalFatGrams = 15f)
        assertEquals("Barcode", com.kalotracker.app.feature.dashboard.mealSource(base.copy(notes = "Barcode: 123")))
        assertEquals("Photo estimate", com.kalotracker.app.feature.dashboard.mealSource(base.copy(imageLocalUri = "/x.jpg")))
        assertNull(com.kalotracker.app.feature.dashboard.mealSource(base))
    }
}
