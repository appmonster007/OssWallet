package com.osscabinet.ossWallet.domain.service

import com.osscabinet.ossWallet.domain.repository.AutoBackupConfig

interface BackupScheduler {
    fun schedule(config: AutoBackupConfig)
    fun cancel()
}
