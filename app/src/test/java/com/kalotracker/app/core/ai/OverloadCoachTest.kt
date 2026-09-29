package com.kalotracker.app.core.ai

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OverloadCoachTest {

    @Test
    fun noHistoryGivesNoHint() {
        assertNull(OverloadCoach.suggest(emptyList()))
    }

    @Test
    fun allSetsAtCeilingSuggestsMoreWeight() {
        val hint = OverloadCoach.suggest(listOf(PastSet(60f, 10), PastSet(60f, 10), PastSet(60f, 10)))!!
        assertTrue(hint, hint.contains("try 62.5kg"))
    }

    @Test
    fun belowCeilingKeepsWeightAndAddsRep() {
        val hint = OverloadCoach.suggest(listOf(PastSet(60f, 10), PastSet(60f, 9), PastSet(60f, 8)))!!
        assertTrue(hint, hint.contains("Keep 60kg"))
        assertTrue(hint, hint.contains("9+ reps"))
    }

    @Test
    fun onlyTopWeightSetsDecideProgression() {
        // Warm-up at 40kg with few reps must not block progression from the 60kg work sets.
        val hint = OverloadCoach.suggest(listOf(PastSet(40f, 5), PastSet(60f, 10), PastSet(60f, 10)))!!
        assertTrue(hint, hint.contains("try 62.5kg"))
    }

    @Test
    fun bodyweightSuggestsMoreReps() {
        val hint = OverloadCoach.suggest(listOf(PastSet(0f, 8), PastSet(0f, 7)))!!
        assertTrue(hint, hint.contains("8+ reps"))
    }
}
