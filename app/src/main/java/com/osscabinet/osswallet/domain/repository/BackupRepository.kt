package com.osscabinet.ossWallet.domain.repository

interface BackupRepository {
    suspend fun exportData(uriString: String): Result<Unit>
    suspend fun importData(uriString: String): Result<Unit>
}
