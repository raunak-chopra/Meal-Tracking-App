package com.kalotracker.app.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionInsightEngineTest {

    private fun insight(kcal: Int, protein: Int, water: Int, isToday: Boolean = true) =
        NutritionInsightEngine.generateDailyInsight(2000, kcal, 150, protein, 2500, water, isToday)

    @Test fun nothingIsSaidEarlyInTheDayOrOnAnEmptyDay() {
        assertNull(insight(kcal = 0, protein = 0, water = 0))
        assertNull(insight(kcal = 900, protein = 10, water = 0))
    }

    @Test fun lowProteinIsReportedAsPlainNumbers() {
        val i = insight(kcal = 1200, protein = 40, water = 2000)!!
        assertEquals(InsightType.PROTEIN, i.type)
        assertEquals("Protein is behind", i.title)
        assertTrue(i.description.contains("40 g of your 150 g target so far"))
    }

    @Test fun pastDaysAreNotDescribedAsInProgress() {
        val i = insight(kcal = 1800, protein = 40, water = 2000, isToday = false)!!
        assertFalse(i.description.contains("so far"))
    }

    @Test fun lowWaterComesAfterProtein() {
        val i = insight(kcal = 1200, protein = 140, water = 500)!!
        assertEquals(InsightType.HYDRATION, i.type)
        assertTrue(i.description.contains("500 ml of your 2500 ml target"))
    }

    @Test fun goingOverTargetNeverSuggestsExercise() {
        assertNull(insight(kcal = 2600, protein = 150, water = 2500))
    }

    @Test fun noTargetMeansNoCard() {
        assertNull(NutritionInsightEngine.generateDailyInsight(0, 1500, 0, 20, 0, 0))
    }
}
