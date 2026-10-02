package com.kalotracker.app.core.reminder

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.kalotracker.app.core.settings.ReminderSettings
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Schedules a once-a-day "did you log your meals?" nudge. Each run re-schedules the next day,
 * so the time stays exact (periodic work would drift by up to its flex window).
 */
object ReminderScheduler {

    private const val WORK_NAME = "daily_log_reminder"

    /**
     * @param replace true when the user just changed the setting; false at app start so an
     * already-scheduled reminder keeps its time.
     */
    fun schedule(context: Context, settings: ReminderSettings, replace: Boolean) {
        val workManager = WorkManager.getInstance(context)
        if (!settings.enabled) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }
        val delay = delayUntilNext(LocalDateTime.now(), settings.hour, settings.minute)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniqueWork(
            WORK_NAME,
            if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request
        )
    }

    /** Time from [now] until the next occurrence of hour:minute (tomorrow if that time has passed). */
    internal fun delayUntilNext(now: LocalDateTime, hour: Int, minute: Int): Duration {
        var next = now.toLocalDate().atTime(hour, minute)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next)
    }
}
