package com.osscabinet.ossWallet.domain.service

import com.osscabinet.ossWallet.domain.repository.AutoBackupConfig
import kotlinx.coroutines.flow.Flow

/** Coarse status of the scheduled backup job, independent of any Android work-manager type. */
enum class BackupRunState { NOT_SCHEDULED, PENDING, RUNNING, SUCCEEDED, FAILED }

interface BackupScheduler {
    fun schedule(config: AutoBackupConfig)
    fun cancel()

    /** The scheduled job's current/last-known state, and when that state was last observed. */
    fun observeStatus(): Flow<Pair<BackupRunState, Long>>
}
