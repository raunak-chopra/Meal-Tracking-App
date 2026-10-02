package com.kalotracker.app.feature.workout

import org.junit.Assert.*
import org.junit.Test

class WorkoutValidationTest {
    private val strength = WorkoutUiState(sets = listOf(EditableSet(1, "0", "8")))

    @Test fun acceptsBodyweightAndCardioWithoutStrengthSets() {
        assertNull(validateWorkout(strength))
        assertNull(validateWorkout(WorkoutUiState(isCardio = true, sets = emptyList())))
    }
    @Test fun rejectsUnfilledSetsNegativeAndNonFiniteValues() {
        assertNotNull(validateWorkout(WorkoutUiState()))
        for (weight in listOf("-1", "NaN", "Infinity", "abc")) {
            assertNotNull(validateWorkout(strength.copy(sets = listOf(EditableSet(1, weight, "8")))))
        }
        assertNotNull(validateWorkout(strength.copy(durationMinutes = "")))
        assertNotNull(validateWorkout(strength.copy(estimatedCalories = "-1")))
        assertNotNull(validateWorkout(strength.copy(sets = listOf(EditableSet(1, "20", "0")))))
    }
}
