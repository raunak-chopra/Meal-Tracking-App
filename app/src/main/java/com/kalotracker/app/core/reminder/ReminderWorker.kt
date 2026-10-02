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
            val meals = KaloDatabase.getInstance(context).mealDao().getMealsForDayOnce(start, end)

            // Only nudge when the day looks under-logged.
            if (meals.size < MIN_MEALS_BEFORE_SILENCE) notifyUser(meals.size)
        } finally {
            // Always queue tomorrow's reminder, even if today's check failed.
            ReminderScheduler.schedule(context, settings, replace = true)
        }
        return Result.success()
    }

    private fun notifyUser(mealCount: Int) {
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
        val text = if (mealCount == 0) "Nothing logged today yet. Snap your meals while you remember them."
        else "Only $mealCount meal logged today. Anything missing?"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Kalo")
            .setContentText(text)
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
        private const val MIN_MEALS_BEFORE_SILENCE = 2
    }
}
