package com.kalotracker.app.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionInsightEngineTest {

    @Test
    fun testProteinDeficitDetected() {
        val insight = NutritionInsightEngine.generateDailyInsight(
            targetCalories = 2000,
            consumedCalories = 1000, // 50% of calories consumed
            targetProtein = 160,
            consumedProtein = 30,    // Only 30g consumed (130g remaining > 40%)
            targetCarbs = 200,
            consumedCarbs = 120,
            targetFat = 60,
            consumedFat = 30,
            workoutCount = 0,
            activeBurn = 0.0,
            targetWaterMl = 2500,
            consumedWaterMl = 1500
        )

        assertEquals(InsightType.PROTEIN, insight.type)
        assertEquals("Protein Deficit Detected", insight.title)
        assertTrue(insight.description.contains("protein left today"))
    }

    @Test
    fun testPostWorkoutRecoveryTakesPriority() {
        val insight = NutritionInsightEngine.generateDailyInsight(
            targetCalories = 2200,
            consumedCalories = 600,
            targetProtein = 150,
            consumedProtein = 120,
            targetCarbs = 250,
            consumedCarbs = 100,
            targetFat = 70,
            consumedFat = 20,
            workoutCount = 1,
            activeBurn = 350.0,
            targetWaterMl = 2500,
            consumedWaterMl = 2000
        )

        assertEquals(InsightType.WORKOUT_RECOVERY, insight.type)
        assertEquals("Post-Workout Recovery", insight.title)
        assertTrue(insight.description.contains("350 active kcal"))
    }

    @Test
    fun testHydrationLagAlert() {
        val insight = NutritionInsightEngine.generateDailyInsight(
            targetCalories = 2000,
            consumedCalories = 1200, // > 40% of calories
            targetProtein = 160,
            consumedProtein = 120,   // Sufficient protein
            targetCarbs = 200,
            consumedCarbs = 120,
            targetFat = 60,
            consumedFat = 30,
            workoutCount = 0,
            activeBurn = 0.0,
            targetWaterMl = 2500,
            consumedWaterMl = 500    // Only 500ml (< 40% of 2500ml)
        )

        assertEquals(InsightType.HYDRATION_OR_BALANCE, insight.type)
        assertEquals("Hydration Lag", insight.title)
        assertTrue(insight.description.contains("500 ml of water today"))
    }

    @Test
    fun testOptimalCaloriePacing() {
        val insight = NutritionInsightEngine.generateDailyInsight(
            targetCalories = 2000,
            consumedCalories = 1800, // 200 kcal remaining (within 1..400)
            targetProtein = 150,
            consumedProtein = 140,
            targetCarbs = 200,
            consumedCarbs = 180,
            targetFat = 65,
            consumedFat = 60,
            workoutCount = 0,
            activeBurn = 0.0,
            targetWaterMl = 2500,
            consumedWaterMl = 2200
        )

        assertEquals(InsightType.ENERGY, insight.type)
        assertEquals("Optimal Calorie Pacing", insight.title)
        assertTrue(insight.description.contains("200 kcal remaining"))
    }

    @Test
    fun testCalorieSurplusDetected() {
        val insight = NutritionInsightEngine.generateDailyInsight(
            targetCalories = 2000,
            consumedCalories = 2300, // 300 kcal over target
            targetProtein = 150,
            consumedProtein = 150,
            targetCarbs = 200,
            consumedCarbs = 250,
            targetFat = 65,
            consumedFat = 75,
            workoutCount = 0,
            activeBurn = 0.0,
            targetWaterMl = 2500,
            consumedWaterMl = 2500
        )

        assertEquals(InsightType.ENERGY, insight.type)
        assertEquals("Calorie Surplus", insight.title)
        assertTrue(insight.description.contains("300 kcal over your baseline goal"))
    }
}
