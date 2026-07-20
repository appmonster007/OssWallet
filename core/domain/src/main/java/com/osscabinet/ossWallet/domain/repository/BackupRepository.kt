package com.osscabinet.ossWallet.domain.repository

import kotlinx.serialization.Serializable

enum class BackupType {
    SQLITE, CSV_ZIP, FLAT_CSV
}

enum class BackupScope {
    ALL_PROFILES, SINGLE_PROFILE
}

enum class RestorableEntity {
    PROFILES, ACCOUNTS, CATEGORIES, TRANSACTIONS, BUDGETS, LOANS,
    PLANNED_PAYMENTS, TEMPLATES, FILTERS, TAGS, CURRENCY_RATES
}

data class BackupMeta(
    val id: String,           // filename without path
    val path: String,         // absolute path
    val createdAt: Long,      // epoch millis
    val sizeBytes: Long,
    val scope: BackupScope
)

@Serializable
data class AutoBackupConfig(
    val enabled: Boolean = false,
    val intervalHours: Int = 24,
    val maxRetained: Int = 7,
    val storageMode: StorageMode = StorageMode.PRIVATE,
    val customFolderUri: String? = null,
    val notifyMode: NotifyMode = NotifyMode.ALWAYS
) {
    @Serializable
    enum class StorageMode { PRIVATE, CUSTOM_FOLDER, BOTH }

    @Serializable
    enum class NotifyMode { ALWAYS, FAILURE_ONLY, NEVER }
}

interface BackupRepository {
    suspend fun exportData(
        uriString: String,
        type: BackupType,
        scope: BackupScope = BackupScope.ALL_PROFILES
    ): Result<Unit>

    suspend fun importData(uriString: String, type: BackupType): Result<Unit>
    suspend fun importDataSelective(
        uriString: String,
        type: BackupType,
        entities: Set<RestorableEntity>
    ): Result<Unit>

    suspend fun deleteAllData(): Result<Unit>
    suspend fun exportToPrivateStorage(scope: BackupScope = BackupScope.ALL_PROFILES): Result<String>
    suspend fun listPrivateBackups(): List<BackupMeta>
    suspend fun deleteBackup(id: String): Result<Unit>
    suspend fun pruneOldBackups(maxRetained: Int)
}
