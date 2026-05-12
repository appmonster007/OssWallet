package com.osscabinet.ossWallet.domain.repository

enum class BackupType {
    SQLITE, CSV_ZIP
}

interface BackupRepository {
    suspend fun exportData(uriString: String, type: BackupType): Result<Unit>
    suspend fun importData(uriString: String, type: BackupType): Result<Unit>
}
