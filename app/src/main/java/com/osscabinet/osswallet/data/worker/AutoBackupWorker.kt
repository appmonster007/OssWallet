package com.osscabinet.ossWallet.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.osscabinet.ossWallet.di.AutoBackupWorkerEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class AutoBackupWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val ep = EntryPointAccessors.fromApplication(appContext, AutoBackupWorkerEntryPoint::class.java)
        val backupRepo = ep.backupRepository()
        val settingsDataStore = ep.settingsDataStore()
        val config = settingsDataStore.autoBackupConfig.first()

        val result = backupRepo.exportToPrivateStorage()
        if (result.isSuccess) {
            backupRepo.pruneOldBackups(config.maxRetained)
            if (config.notifyMode != com.osscabinet.ossWallet.domain.repository.AutoBackupConfig.NotifyMode.NEVER) {
                notify(appContext, "Backup completed", "Your data has been backed up successfully.", false)
            }
        } else {
            if (config.notifyMode != com.osscabinet.ossWallet.domain.repository.AutoBackupConfig.NotifyMode.NEVER) {
                notify(appContext, "Backup failed", result.exceptionOrNull()?.message ?: "Unknown error", true)
            }
        }
        return if (result.isSuccess) Result.success() else Result.retry()
    }

    private fun notify(context: Context, title: String, message: String, isError: Boolean) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "auto_backup"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(channelId, "Auto Backup", NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(ch)
        }
        val notif = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .build()
        nm.notify(if (isError) 2002 else 2001, notif)
    }

    companion object {
        const val WORK_NAME = "auto_backup_worker"

        fun schedule(context: Context, intervalHours: Int) {
            val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(
                intervalHours.toLong(), TimeUnit.HOURS
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
