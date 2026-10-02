package com.kalotracker.app.core.ai

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class CompleteDayTest {
    private val today=LocalDate.of(2026,10,2)
    @Test fun partialTodayAndMissingDaysDoNotLowerAverage() {
        val days=listOf(DayTotals(today.minusDays(2),2000,100f,mealCount=3,complete=true,historicalTargets=NutritionTargets(2000,100,2000)),
            DayTotals(today.minusDays(1),300,10f,mealCount=1,complete=false),DayTotals(today,200,5f,mealCount=1,complete=true))
        val stats=TrendAnalyzer.stats(days,NutritionTargets(2500,160,2500),today,true)
        assertEquals(2000,stats.avgCalories);assertEquals(1,stats.loggedDays);assertEquals(1,stats.proteinHitDays);assertEquals(1,stats.calorieOnTargetDays)
    }
    @Test fun historicalTargetsAreUsedAndUnknownHistoryIsNotInvented() {
        val d=DayTotals(today.minusDays(1),1800,80f,mealCount=3,historicalTargets=NutritionTargets(1800,80,2000))
        assertEquals(1,TrendAnalyzer.stats(listOf(d),NutritionTargets(2500,200,2500),today,true).calorieOnTargetDays)
        assertEquals(0,TrendAnalyzer.stats(listOf(d.copy(historicalTargets=null)),NutritionTargets(1800,80,2000),today,true).calorieOnTargetDays)
    }
    @Test fun targetChangesAreGatedByCompleteLoggingAndConsistency() {
        val thin=listOf(DayTotals(today.minusDays(1),2000,mealCount=3))
        assertNull(GoalAdvisor.withEvidence(GoalType.LOSE,emptyList(),thin,2000).newCalorieTarget)
        val incomplete=(1..15).map { DayTotals(today.minusDays(it.toLong()),300,mealCount=1,complete=false) }
        assertNull(GoalAdvisor.withEvidence(GoalType.LOSE,emptyList(),incomplete,2000).newCalorieTarget)
    }
    private fun evidence() = (1..15).map { DayTotals(today.minusDays(it.toLong()),2000,mealCount=3,
        complete=true,historicalTargets=NutritionTargets(2000,100,2000)) }
    private fun weights() = listOf(15L,8L,1L).map {
        WeightPoint(today.minusDays(it).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),80f)
    }
    @Test fun sufficientConsistentEvidenceAllowsAnExplicitSuggestion() {
        assertEquals(1850,GoalAdvisor.withEvidence(GoalType.LOSE,weights(),evidence(),2000).newCalorieTarget)
    }
    @Test fun changedGoalsVariableIntakeAndOutOfWindowWeightsSuppressAdjustment() {
        assertNull(GoalAdvisor.withEvidence(GoalType.LOSE,weights(),evidence().mapIndexed { i,d ->
            if(i==0) d.copy(historicalTargets=NutritionTargets(2200,100,2000)) else d },2000).newCalorieTarget)
        assertNull(GoalAdvisor.withEvidence(GoalType.LOSE,weights(),evidence().map { it.copy(calories=1000) },2000).newCalorieTarget)
        assertNull(GoalAdvisor.withEvidence(GoalType.LOSE,weights().map { it.copy(epochMillis=it.epochMillis-60L*86400000) },evidence(),2000).newCalorieTarget)
    }
}
