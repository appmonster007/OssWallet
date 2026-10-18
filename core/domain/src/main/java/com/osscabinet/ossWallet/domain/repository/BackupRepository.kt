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
    PLANNED_PAYMENTS, TEMPLATES, FILTERS, TAGS, CURRENCY_RATES, GOALS
}

data class BackupMeta(
    val id: String,           // filename without path
    val path: String,         // absolute path
    val createdAt: Long,      // epoch millis
    val sizeBytes: Long,
    val scope: BackupScope,
    val encrypted: Boolean = false
)

@Serializable
data class AutoBackupConfig(
    val enabled: Boolean = false,
    val intervalHours: Int = 24,
    val maxRetained: Int = 7,
    val storageMode: StorageMode = StorageMode.PRIVATE,
    val customFolderUri: String? = null,
    val notifyMode: NotifyMode = NotifyMode.ALWAYS,
    /** Encrypt scheduled backups with the key set up via [BackupRepository.enableBackupEncryption]. */
    val encrypt: Boolean = false
) {
    @Serializable
    enum class StorageMode { PRIVATE, CUSTOM_FOLDER, BOTH }

    @Serializable
    enum class NotifyMode { ALWAYS, FAILURE_ONLY, NEVER }
}

/** What [BackupRepository.verifyBackup] learned about a backup without restoring it. */
data class BackupVerification(
    val encrypted: Boolean,
    val entryCount: Int,
    val sizeBytes: Long
)

/** The backup is encrypted but no passphrase was supplied. */
class BackupPassphraseRequiredException : java.io.IOException("This backup is encrypted; a passphrase is required")

interface BackupRepository {
    /** A non-null [passphrase] encrypts the file; it is used once and not retained. */
    suspend fun exportData(
        uriString: String,
        type: BackupType,
        scope: BackupScope = BackupScope.ALL_PROFILES,
        passphrase: CharArray? = null
    ): Result<Unit>

    /** Fails with [BackupPassphraseRequiredException] if the file is encrypted and [passphrase] is null. */
    suspend fun importData(uriString: String, type: BackupType, passphrase: CharArray? = null): Result<Unit>
    suspend fun importDataSelective(
        uriString: String,
        type: BackupType,
        entities: Set<RestorableEntity>,
        passphrase: CharArray? = null
    ): Result<Unit>

    suspend fun isEncryptedBackup(uriString: String): Boolean

    /** Detects if the backup URI is a single CSV file and identifies its corresponding [RestorableEntity]. Returns null for ZIP or unknown files. */
    suspend fun detectBackupEntity(uriString: String, passphrase: CharArray? = null): RestorableEntity?

    /** Authenticates (if encrypted) and structurally checks a backup without touching live data. */
    suspend fun verifyBackup(
        uriString: String,
        type: BackupType,
        passphrase: CharArray? = null
    ): Result<BackupVerification>

    /** Derives and stores the key scheduled backups encrypt with. The passphrase is not kept. */
    suspend fun enableBackupEncryption(passphrase: CharArray): Result<Unit>
    suspend fun disableBackupEncryption()
    suspend fun hasBackupEncryptionKey(): Boolean

    suspend fun deleteAllData(): Result<Unit>
    suspend fun exportToPrivateStorage(scope: BackupScope = BackupScope.ALL_PROFILES): Result<String>
    suspend fun listPrivateBackups(): List<BackupMeta>
    suspend fun deleteBackup(id: String): Result<Unit>
    suspend fun pruneOldBackups(maxRetained: Int)
}
