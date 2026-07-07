package com.osscabinet.ossWallet.data.service

import android.content.Context
import com.osscabinet.ossWallet.data.worker.AutoBackupWorker
import com.osscabinet.ossWallet.domain.repository.AutoBackupConfig
import com.osscabinet.ossWallet.domain.service.BackupScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class BackupSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BackupScheduler {
    override fun schedule(config: AutoBackupConfig) {
        if (config.enabled) {
            AutoBackupWorker.schedule(context, config.intervalHours)
        } else {
            cancel()
        }
    }

    override fun cancel() = AutoBackupWorker.cancel(context)
}
