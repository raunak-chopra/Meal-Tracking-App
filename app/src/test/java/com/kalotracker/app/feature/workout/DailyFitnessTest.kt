package com.kalotracker.app.feature.workout

import org.junit.Assert.*
import org.junit.Test

class DailyFitnessTest {
    @Test fun sessionTimeIsAllocatedOnceAndBodyweightSetsAreValid() {
        for (minutes in listOf(5, 10)) {
            val rows = dailyFitnessExercises(minutes, 70.0)
            assertEquals(minutes, rows.sumOf { it.durationMinutes })
            assertEquals(3, rows.size)
            rows.forEach {
                assertEquals("0", it.sets.single().weightKg)
                assertEquals("20", it.sets.single().reps)
            }
            assertNull(validateWorkout(WorkoutUiState(exerciseName = "", otherExercises = rows)))
        }
        assertEquals(21, dailyFitnessExercises(5, 70.0).sumOf { it.calories })
        assertEquals(43, dailyFitnessExercises(10, 70.0).sumOf { it.calories })
    }

    @Test fun estimateScalesWithBodyWeight() {
        assertTrue(dailyFitnessExercises(5, 100.0).sumOf { it.calories } >
            dailyFitnessExercises(5, 50.0).sumOf { it.calories })
    }

    @Test fun skippingExercisesReallocatesTimeAndEffortChangesTheEstimate() {
        val easy = dailyFitnessExercises(7, 70.0, FitnessEffort.EASY, listOf(20, 0, 30))
        val hard = dailyFitnessExercises(7, 70.0, FitnessEffort.HARD, listOf(20, 0, 30))
        assertEquals(2, easy.size)
        assertEquals(7, easy.sumOf { it.durationMinutes })
        assertEquals(listOf("20", "30"), easy.map { it.sets.single().reps })
        assertTrue(hard.sumOf { it.calories } > easy.sumOf { it.calories })
        assertNull(validateWorkout(WorkoutUiState(exerciseName = "", otherExercises = easy)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptyRoutineIsRejected() { dailyFitnessExercises(7, 70.0, reps = listOf(0, 0, 0)) }

    @Test(expected = IllegalArgumentException::class)
    fun invalidWeightIsRejected() { dailyFitnessExercises(5, Double.NaN) }
}
