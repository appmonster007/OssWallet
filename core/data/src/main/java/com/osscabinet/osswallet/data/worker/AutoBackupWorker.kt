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
import com.osscabinet.ossWallet.core.data.R
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.data.notification.NotificationPermissions
import com.osscabinet.ossWallet.domain.repository.AutoBackupConfig
import com.osscabinet.ossWallet.domain.repository.BackupRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
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
        val wantsPrivate = config.storageMode == AutoBackupConfig.StorageMode.PRIVATE ||
            config.storageMode == AutoBackupConfig.StorageMode.BOTH
        val wantsCustomFolder = config.storageMode == AutoBackupConfig.StorageMode.CUSTOM_FOLDER ||
            config.storageMode == AutoBackupConfig.StorageMode.BOTH

        val privateResult = if (wantsPrivate) backupRepo.exportToPrivateStorage(config.scope) else null
        val customFolderResult = if (wantsCustomFolder) {
            val folderUri = config.customFolderUri
            if (folderUri == null) {
                kotlin.Result.failure(IllegalStateException("No custom folder is configured"))
            } else {
                backupRepo.exportToCustomFolder(folderUri, config.scope)
            }
        } else null

        // The job only succeeds if every destination the config actually asked for succeeded —
        // a custom-folder failure (lost SAF permission, folder deleted, ...) must not be masked
        // by a private copy the user didn't ask to rely on, and vice versa.
        val requiredResults = listOfNotNull(privateResult, customFolderResult)
        val allSucceeded = requiredResults.isNotEmpty() && requiredResults.all { it.isSuccess }

        if (allSucceeded) {
            backupRepo.pruneOldBackups(
                config.maxRetained,
                customFolderUri = if (wantsCustomFolder) config.customFolderUri else null
            )
            if (config.notifyMode == AutoBackupConfig.NotifyMode.ALWAYS) {
                notify(appContext, "Backup completed", "Your data has been backed up successfully.", false)
            }
            return Result.success()
        }

        val failureMessage = requiredResults.firstOrNull { it.isFailure }
            ?.exceptionOrNull()?.message ?: "Unknown error"
        if (config.notifyMode != AutoBackupConfig.NotifyMode.NEVER) {
            notify(appContext, "Backup failed", failureMessage, true)
        }
        return Result.retry()
    }

    @Suppress("MissingPermission")
    private fun notify(context: Context, title: String, message: String, isError: Boolean) {
        if (!NotificationPermissions.canPost(context)) return

        val channelId = "auto_backup"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val ch =
                NotificationChannel(channelId, "Auto Backup", NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(ch)
        }

        val notif = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(if (isError) 2002 else 2001, notif)
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
