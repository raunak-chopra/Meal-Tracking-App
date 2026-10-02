package com.kalotracker.app.core.data.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.work.*
import com.kalotracker.app.KaloApplication
import com.kalotracker.app.core.settings.AppSettings
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

object BackupScheduler {
    fun schedule(context: Context, enabled: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (!enabled) { wm.cancelUniqueWork("kalo_daily_backup"); return }
        wm.enqueueUniquePeriodicWork("kalo_daily_backup", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build()).build())
    }
}

class BackupWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val settings = AppSettings(context)
        val schedule = settings.backup.value
        if (!schedule.enabled || schedule.folder.isBlank()) return Result.success()
        var created: Uri? = null
        return try {
            val tree = Uri.parse(schedule.folder)
            val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
            val name = "kalo-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))}.zip"
            created = DocumentsContract.createDocument(context.contentResolver, parent, "application/zip", name)
                ?: throw java.io.IOException("Could not create backup in selected folder.")
            val output = context.contentResolver.openOutputStream(created!!, "wt")
                ?: throw java.io.IOException("Backup folder is not writable.")
            (context.applicationContext as KaloApplication).backupManager.exportArchive(output)
            settings.backupResult(System.currentTimeMillis(), null)
            Result.success()
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            created?.let { runCatching { DocumentsContract.deleteDocument(context.contentResolver, it) } }
            throw cancelled
        } catch (e: Exception) {
            created?.let { runCatching { DocumentsContract.deleteDocument(context.contentResolver, it) } }
            settings.backupResult(0, e.localizedMessage ?: "Automatic backup failed. Choose the folder again.")
            Result.failure()
        }
    }
}
