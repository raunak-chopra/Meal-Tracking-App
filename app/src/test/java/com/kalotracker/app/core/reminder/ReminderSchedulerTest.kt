package com.kalotracker.app.core.reminder

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime

class ReminderSchedulerTest {

    private val now = LocalDateTime.of(2026, 9, 29, 15, 30)

    @Test
    fun laterTodayIsScheduledToday() {
        assertEquals(Duration.ofHours(4).plusMinutes(30), ReminderScheduler.delayUntilNext(now, 20, 0))
    }

    @Test
    fun earlierTimeRollsToTomorrow() {
        assertEquals(Duration.ofHours(17).plusMinutes(30), ReminderScheduler.delayUntilNext(now, 9, 0))
    }

    @Test
    fun exactlyNowRollsToTomorrowNotZero() {
        assertEquals(Duration.ofHours(24), ReminderScheduler.delayUntilNext(now, 15, 30))
    }
}
