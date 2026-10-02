package com.kalotracker.app.feature.trends

import com.kalotracker.app.core.database.dao.MealWithItems
import com.kalotracker.app.core.database.dao.WorkoutWithSets
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class HabitSummary(val mealDays: Int = 0, val sessions: Int = 0, val minutes: Int = 0,
    val reps: Map<String, Int> = emptyMap())

internal fun habitSummary(meals: List<MealWithItems>, workouts: List<WorkoutWithSets>,
    start: LocalDate, end: LocalDate, zone: ZoneId): HabitSummary {
    fun within(timestamp: Long) = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate() in start..end
    val sessions = workouts.filter { within(it.workout.timestamp) }
    return HabitSummary(meals.filter { within(it.meal.timestamp) }.map { Instant.ofEpochMilli(it.meal.timestamp).atZone(zone).toLocalDate() }.distinct().size,
        sessions.size, sessions.sumOf { it.workout.durationMinutes },
        sessions.flatMap { it.sets }.filter { it.isCompleted }.groupBy { it.exerciseName }.mapValues { (_, sets) -> sets.sumOf { it.reps } })
}
