package com.kalotracker.app.feature.trends

import com.kalotracker.app.core.database.dao.*
import com.kalotracker.app.core.database.entity.*
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class HabitSummaryTest {
    @Test fun countsLoggingDaysAndOnlyCompletedRepsInsideTheSelectedRange() {
        val zone = ZoneId.of("Asia/Kolkata")
        val date = LocalDate.of(2026, 10, 2)
        fun timestamp(day: LocalDate) = day.atStartOfDay(zone).toInstant().toEpochMilli()
        fun meal(day: LocalDate) = MealWithItems(MealEntity(title = "Lunch", totalCalories = 100,
            totalProteinGrams = 1f, totalCarbsGrams = 1f, totalFatGrams = 1f, timestamp = timestamp(day)), emptyList())
        fun workout(day: LocalDate) = WorkoutWithSets(WorkoutEntity(id = "session", title = "Daily fitness", type = "STRENGTH",
            durationMinutes = 7, timestamp = timestamp(day)), listOf(
                ExerciseSetEntity(workoutId = "session", exerciseName = "Push-ups", setNumber = 1, weightKg = 0f, reps = 20),
                ExerciseSetEntity(workoutId = "session", exerciseName = "Push-ups", setNumber = 2, weightKg = 0f, reps = 10, isCompleted = false)))
        val stats = habitSummary(listOf(meal(date), meal(date), meal(date.minusDays(10))),
            listOf(workout(date), workout(date.minusDays(10))), date.minusDays(6), date, zone)
        assertEquals(1, stats.mealDays)
        assertEquals(1, stats.sessions)
        assertEquals(7, stats.minutes)
        assertEquals(20, stats.reps["Push-ups"])
    }
}
