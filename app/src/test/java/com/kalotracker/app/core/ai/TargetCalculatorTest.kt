package com.kalotracker.app.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TargetCalculatorTest {
    @Test fun storedTeenProfileRemainsReadableButCannotGenerateAutomaticTargets() {
        val teen = BodyProfile(Sex.MALE, 17, 170f, 65f, ActivityLevel.LIGHT)
        assertNull(teen.problem())
        assertNotNull(teen.automaticTargetProblem())
        for (goal in GoalType.entries) {
            try {
                TargetCalculator.suggest(teen, goal)
                org.junit.Assert.fail("Automatic targets must reject under-18 profiles")
            } catch (_: IllegalArgumentException) { }
        }
    }

    @Test fun trendAdjustmentUsesTheSameFormulaFloorAsInitialTargets() {
        val floor = TargetCalculator.minimumCalories(man)
        val adjustment = GoalAdvisor.suggest(GoalType.LOSE, 0f, floor + 50, floor)
        assertEquals(floor, adjustment.newCalorieTarget)
        assertNull(GoalAdvisor.suggest(GoalType.LOSE, 0f, floor, floor).newCalorieTarget)
    }
    // Fictional example person.
    private val man = BodyProfile(Sex.MALE, age = 30, heightCm = 178f, weightKg = 80f, activity = ActivityLevel.MODERATE)
    private val woman = BodyProfile(Sex.FEMALE, age = 45, heightCm = 160f, weightKg = 62f, activity = ActivityLevel.SEDENTARY)

    @Test fun restingEnergyFollowsMifflinStJeor() {
        assertEquals(1767.5f, TargetCalculator.restingCalories(man), 0.01f) // 800 + 1112.5 - 150 + 5
        assertEquals(1234f, TargetCalculator.restingCalories(woman), 0.01f) // 620 + 1000 - 225 - 161
    }

    @Test fun goalsMoveCaloriesFromMaintenance() {
        val maintain = TargetCalculator.suggest(man, GoalType.MAINTAIN)
        assertEquals(2740, maintain.maintenanceCalories) // 1767.5 x 1.55
        assertEquals(2740, maintain.calories)
        assertEquals(2190, TargetCalculator.suggest(man, GoalType.LOSE).calories)
        assertEquals(3010, TargetCalculator.suggest(man, GoalType.GAIN).calories)
    }

    @Test fun deficitNeverGoesBelowRestingEnergyOrSafeFloor() {
        val lose = TargetCalculator.suggest(woman, GoalType.LOSE)
        assertTrue(lose.calories >= 1234)
        assertTrue(lose.calories >= 1200)
    }

    @Test fun macrosAddUpToCalories() {
        for (goal in GoalType.entries) for (body in listOf(man, woman)) {
            val t = TargetCalculator.suggest(body, goal)
            val fromMacros = t.protein * 4 + t.carbs * 4 + t.fat * 9
            assertEquals("$goal $body", t.calories.toFloat(), fromMacros.toFloat(), 12f)
        }
    }

    @Test fun proteinUsesBodyWeightButIsCappedForHighBmi() {
        assertEquals(140, TargetCalculator.suggest(man.copy(weightKg = 70f), GoalType.LOSE).protein) // 70 kg x 2.0
        val heavy = man.copy(weightKg = 130f)
        // Capped at the weight for BMI 25 at 1.78 m (79.2 kg).
        assertEquals(158, TargetCalculator.suggest(heavy, GoalType.LOSE).protein)
    }

    @Test fun waterIsAboutThirtyFiveMlPerKg() {
        assertEquals(2800, TargetCalculator.suggest(man, GoalType.MAINTAIN).waterMl)
    }

    @Test fun implausibleInputsAreRejected() {
        assertNull(man.problem())
        assertNotNull(man.copy(age = 9).problem())
        assertNotNull(man.copy(heightCm = 17.8f).problem())
        assertNotNull(man.copy(weightKg = 800f).problem())
    }
}
