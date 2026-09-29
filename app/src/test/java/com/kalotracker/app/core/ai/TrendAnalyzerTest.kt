package com.kalotracker.app.core.ai

import com.kalotracker.app.core.database.dao.MealWithItems
import com.kalotracker.app.core.database.entity.MealEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class TrendAnalyzerTest {

    private val zone = ZoneId.of("UTC")
    private val targets = NutritionTargets(calories = 2000, protein = 150, waterMl = 2500)

    private fun meal(day: LocalDate, hour: Int, kcal: Int, protein: Float) = MealWithItems(
        MealEntity(
            title = "m",
            totalCalories = kcal,
            totalProteinGrams = protein,
            totalCarbsGrams = 0f,
            totalFatGrams = 0f,
            timestamp = LocalDateTime.of(day, java.time.LocalTime.of(hour, 0)).atZone(zone).toInstant().toEpochMilli()
        ),
        emptyList()
    )

    private fun days(start: LocalDate, end: LocalDate, meals: List<MealWithItems>) =
        TrendAnalyzer.buildDays(start, end, meals, emptyList(), emptyList(), zone)

    private val monday = LocalDate.of(2026, 9, 21) // a Monday

    @Test
    fun buildDaysGroupsAndFillsEmptyDays() {
        val d = days(monday, monday.plusDays(2), listOf(meal(monday, 8, 500, 30f), meal(monday, 22, 300, 10f)))
        assertEquals(3, d.size)
        assertEquals(800, d[0].calories)
        assertEquals(300, d[0].lateCalories)
        assertEquals(2, d[0].mealCount)
        assertEquals(0, d[1].calories)
        assertTrue(!d[1].isLogged)
    }

    @Test
    fun streakCountsBackFromTodayOrYesterday() {
        val meals = listOf(monday.plusDays(1), monday.plusDays(2), monday.plusDays(3)).map { meal(it, 12, 500, 20f) }
        val d = days(monday, monday.plusDays(4), meals)
        assertEquals(3, TrendAnalyzer.streak(d, monday.plusDays(3))) // logged today
        assertEquals(3, TrendAnalyzer.streak(d, monday.plusDays(4))) // nothing yet today, streak survives
        assertEquals(0, TrendAnalyzer.streak(d, monday.plusDays(6))) // gap of two days breaks it
    }

    @Test
    fun tooLittleDataSaysSo() {
        val d = days(monday, monday.plusDays(6), listOf(meal(monday, 12, 2000, 150f)))
        val insights = TrendAnalyzer.insights(TrendAnalyzer.stats(d, targets, monday.plusDays(6)), targets)
        assertEquals(1, insights.size)
        assertTrue(insights[0].contains("at least 3 days"))
    }

    @Test
    fun lowProteinAndOverEatingAreFlagged() {
        val meals = (0..4).map { meal(monday.plusDays(it.toLong()), 12, 2600, 70f) }
        val d = days(monday, monday.plusDays(6), meals)
        val insights = TrendAnalyzer.insights(TrendAnalyzer.stats(d, targets, monday.plusDays(6)), targets)
        assertTrue(insights.any { it.contains("over your 2000") })
        assertTrue(insights.any { it.contains("Protein was on target on 0 of 5") })
    }

    @Test
    fun weekendSurplusAndLateEatingAreFlagged() {
        val meals = listOf(
            meal(monday, 21, 1500, 150f), meal(monday.plusDays(1), 21, 1500, 150f), meal(monday.plusDays(2), 21, 1500, 150f),
            meal(monday.plusDays(5), 21, 2500, 150f), meal(monday.plusDays(6), 21, 2500, 150f)
        )
        val d = days(monday, monday.plusDays(6), meals)
        val insights = TrendAnalyzer.insights(TrendAnalyzer.stats(d, targets, monday.plusDays(6)), targets)
        assertTrue(insights.toString(), insights.any { it.contains("Weekends run about") })
        assertTrue(insights.toString(), insights.any { it.contains("after 9pm") })
    }

    @Test
    fun onTargetDaysGetNoNagging() {
        val meals = (0..4).map { meal(monday.plusDays(it.toLong()), 12, 2000, 150f) }
        val d = days(monday, monday.plusDays(6), meals)
        val insights = TrendAnalyzer.insights(TrendAnalyzer.stats(d, targets, monday.plusDays(6)), targets)
        assertTrue(insights.any { it.contains("Strong protein consistency") })
        assertTrue(insights.none { it.contains("over your") })
    }

    private fun weights(vararg pairs: Pair<Int, Float>) =
        pairs.map { (day, kg) -> WeightPoint(day * 24L * 60 * 60 * 1000, kg) }

    @Test
    fun weeklyChangeNeedsEnoughData() {
        assertNull(GoalAdvisor.weeklyChangeKg(weights(0 to 80f, 3 to 79.8f)))
        assertNull(GoalAdvisor.weeklyChangeKg(weights(0 to 80f, 2 to 79.9f, 4 to 79.8f))) // span < 10 days
    }

    @Test
    fun weeklyChangeIsTheTrendSlope() {
        val rate = GoalAdvisor.weeklyChangeKg(weights(0 to 80f, 7 to 79.5f, 14 to 79f))!!
        assertEquals(-0.5f, rate, 0.01f)
    }

    @Test
    fun losingGoalAdjustsTargetByTrend() {
        assertNull(GoalAdvisor.suggest(GoalType.LOSE, -0.5f, 2000).newCalorieTarget)
        assertEquals(1850, GoalAdvisor.suggest(GoalType.LOSE, 0.1f, 2000).newCalorieTarget)
        assertEquals(2150, GoalAdvisor.suggest(GoalType.LOSE, -1.4f, 2000).newCalorieTarget)
    }

    @Test
    fun maintainAndGainGoals() {
        assertEquals(1900, GoalAdvisor.suggest(GoalType.MAINTAIN, 0.5f, 2000).newCalorieTarget)
        assertNull(GoalAdvisor.suggest(GoalType.MAINTAIN, 0.1f, 2000).newCalorieTarget)
        assertEquals(2150, GoalAdvisor.suggest(GoalType.GAIN, 0.0f, 2000).newCalorieTarget)
        assertEquals(1900, GoalAdvisor.suggest(GoalType.GAIN, 0.9f, 2000).newCalorieTarget)
    }

    @Test
    fun suggestionsAreBoundedAndNoDataIsExplained() {
        assertEquals(1200, GoalAdvisor.suggest(GoalType.LOSE, 0.2f, 1250).newCalorieTarget)
        val none = GoalAdvisor.suggest(GoalType.LOSE, null, 2000)
        assertNull(none.newCalorieTarget)
        assertNotNull(none.message)
    }
}
