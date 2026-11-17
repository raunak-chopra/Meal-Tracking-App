package com.kalotracker.app.core.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kalotracker.app.MainActivity
import com.kalotracker.app.R
import com.kalotracker.app.core.database.KaloDatabase
import com.kalotracker.app.core.settings.AppSettings
import java.time.LocalDate
import java.time.ZoneId

class ReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = AppSettings(context).reminder.value
        if (!settings.enabled) return Result.success()

        try {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now()
            val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
            val database = KaloDatabase.getInstance(context)
            val meals = database.mealDao().getMealsForDayOnce(start, end)
            val complete = database.personalDao().days().any { it.date == today.toString() && it.complete }

            // Match Settings: a meal-only nudge, never pressure to exercise on a rest day.
            if (shouldRemindForMeals(meals.size, complete)) notifyUser()
        } finally {
            // Always queue tomorrow's reminder, even if today's check failed.
            ReminderScheduler.schedule(context, settings, replace = true)
        }
        return Result.success()
    }

    private fun notifyUser() {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Logging reminders", NotificationManager.IMPORTANCE_DEFAULT)
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // No counts or meal names: the lock screen may show this.
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Want to add anything from today?")
            .setContentText("Open your log when you're ready.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Notification permission revoked between the check and the call; nothing to do.
        }
    }

    companion object {
        private const val CHANNEL_ID = "logging_reminders"
        private const val NOTIFICATION_ID = 1001
    }
}

/** A reminder threshold is not evidence that intake is complete. */
internal fun shouldRemindForMeals(mealCount: Int, dayComplete: Boolean): Boolean =
    !dayComplete && mealCount < 2
