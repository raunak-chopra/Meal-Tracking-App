package com.kalotracker.app.core.reminder

import org.junit.Assert.*
import org.junit.Test

class MealReminderPolicyTest {
    @Test fun twoMealsSuppressMealNudgesWithoutDependingOnExercise() {
        assertTrue(shouldRemindForMeals(0, false))
        assertTrue(shouldRemindForMeals(1, false))
        assertFalse(shouldRemindForMeals(2, false))
        assertFalse(shouldRemindForMeals(5, false))
    }

    @Test fun explicitlyCompleteDaysNeverReceiveMealNudges() {
        for (count in 0..5) assertFalse(shouldRemindForMeals(count, true))
    }
}
