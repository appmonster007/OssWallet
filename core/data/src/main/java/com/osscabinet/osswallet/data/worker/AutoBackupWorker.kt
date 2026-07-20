package com.osscabinet.ossWallet.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.domain.repository.AutoBackupConfig
import com.osscabinet.ossWallet.domain.repository.BackupRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import okio.buffer
import okio.sink
import okio.source
import java.util.concurrent.TimeUnit

@HiltWorker
class AutoBackupWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters,
    private val backupRepo: BackupRepository,
    private val settingsDataStore: SettingsDataStore
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val config = settingsDataStore.autoBackupConfig.first()

        val result = backupRepo.exportToPrivateStorage()
        if (result.isSuccess) {
            backupRepo.pruneOldBackups(config.maxRetained)
            if (config.notifyMode == AutoBackupConfig.NotifyMode.ALWAYS) {
                notify(
                    appContext,
                    "Backup completed",
                    "Your data has been backed up successfully.",
                    false
                )
            }
            if (config.storageMode == AutoBackupConfig.StorageMode.CUSTOM_FOLDER ||
                config.storageMode == AutoBackupConfig.StorageMode.BOTH
            ) {
                config.customFolderUri?.let { folderUri ->
                    try {
                        val docFolder = androidx.documentfile.provider.DocumentFile.fromTreeUri(
                            appContext, android.net.Uri.parse(folderUri)
                        )
                        val timestamp = java.time.LocalDateTime.now()
                            .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                        val fileName = "osswallet_backup_${timestamp}_all.zip"
                        val newFile = docFolder?.createFile("application/zip", fileName)
                        newFile?.uri?.let { fileUri ->
                            appContext.contentResolver.openOutputStream(fileUri)?.use { os ->
                                val privatePath = result.getOrNull()
                                if (privatePath != null) {
                                    java.io.File(privatePath).source().buffer().use { source ->
                                        os.sink().buffer().use { sink ->
                                            sink.writeAll(source)
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e(
                            "AutoBackupWorker",
                            "Failed to write to custom folder",
                            e
                        )
                    }
                }
            }
        } else {
            if (config.notifyMode != AutoBackupConfig.NotifyMode.NEVER) {
                notify(
                    appContext,
                    "Backup failed",
                    result.exceptionOrNull()?.message ?: "Unknown error",
                    true
                )
            }
        }
        return if (result.isSuccess) Result.success() else Result.retry()
    }

    private fun notify(context: Context, title: String, message: String, isError: Boolean) {
        val channelId = "auto_backup"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val ch =
                NotificationChannel(channelId, "Auto Backup", NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(ch)
        }

        val notif = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(if (isError) 2002 else 2001, notif)
        } catch (e: SecurityException) {
            // Permission not granted on Android 13+
            android.util.Log.w("AutoBackupWorker", "Notification permission not granted", e)
        }
    }

    companion object {
        const val WORK_NAME = "auto_backup_worker"

        fun schedule(context: Context, intervalHours: Int) {
            val constraints = Constraints.Builder()
                .setRequiresStorageNotLow(true)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(
                intervalHours.toLong(), TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
                .build()

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
