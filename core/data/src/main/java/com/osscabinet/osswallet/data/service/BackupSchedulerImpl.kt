package com.osscabinet.ossWallet.data.service

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.osscabinet.ossWallet.data.worker.AutoBackupWorker
import com.osscabinet.ossWallet.domain.repository.AutoBackupConfig
import com.osscabinet.ossWallet.domain.service.BackupRunState
import com.osscabinet.ossWallet.domain.service.BackupScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.map

@Singleton
class BackupSchedulerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : BackupScheduler {
    override fun schedule(config: AutoBackupConfig) {
        if (config.enabled) {
            AutoBackupWorker.schedule(context, config.intervalHours)
        } else {
            cancel()
        }
    }

    override fun cancel() = AutoBackupWorker.cancel(context)

    override fun observeStatus() =
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(AutoBackupWorker.WORK_NAME)
            .map { infos ->
                // Periodic work keeps exactly one WorkInfo per enqueue generation; take the most recent.
                val info = infos.maxByOrNull { it.generation }
                val state = when (info?.state) {
                    null -> BackupRunState.NOT_SCHEDULED
                    WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> BackupRunState.PENDING
                    WorkInfo.State.RUNNING -> BackupRunState.RUNNING
                    WorkInfo.State.SUCCEEDED -> BackupRunState.SUCCEEDED
                    WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> BackupRunState.FAILED
                }
                state to System.currentTimeMillis()
            }
}
