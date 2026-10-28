package com.osscabinet.ossWallet.data.worker

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.domain.repository.AutoBackupConfig
import com.osscabinet.ossWallet.domain.repository.BackupMeta
import com.osscabinet.ossWallet.domain.repository.BackupRepository
import com.osscabinet.ossWallet.domain.repository.BackupScope
import com.osscabinet.ossWallet.domain.repository.BackupType
import com.osscabinet.ossWallet.domain.repository.BackupVerification
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression coverage for the two root-cause bugs this worker used to have: it reported success
 * even when the custom-folder write failed, and it always wrote a private copy regardless of
 * [AutoBackupConfig.StorageMode].
 */
@RunWith(AndroidJUnit4::class)
class AutoBackupWorkerTest {

    private class FakeBackupRepository(
        private val privateResult: Result<String> = Result.success("content://fake/private"),
        private val customFolderResult: Result<String> = Result.success("content://fake/custom")
    ) : BackupRepository {
        var privateCalls = 0
        var customFolderCalls = 0
        var pruneCalls = 0

        override suspend fun exportData(uriString: String, type: BackupType, scope: BackupScope, passphrase: CharArray?) =
            Result.success(Unit)
        override suspend fun importData(uriString: String, type: BackupType, passphrase: CharArray?) = Result.success(Unit)
        override suspend fun importDataSelective(uriString: String, type: BackupType, entities: Set<com.osscabinet.ossWallet.domain.repository.RestorableEntity>, passphrase: CharArray?) = Result.success(Unit)
        override suspend fun isEncryptedBackup(uriString: String) = false
        override suspend fun detectBackupEntity(uriString: String, passphrase: CharArray?) = null
        override suspend fun verifyBackup(uriString: String, type: BackupType, passphrase: CharArray?) =
            Result.success(BackupVerification(encrypted = false, entryCount = 1, sizeBytes = 1L))
        override suspend fun enableBackupEncryption(passphrase: CharArray) = Result.success(Unit)
        override suspend fun disableBackupEncryption() = Unit
        override suspend fun hasBackupEncryptionKey() = false
        override suspend fun deleteAllData() = Result.success(Unit)

        override suspend fun exportToPrivateStorage(scope: BackupScope): Result<String> {
            privateCalls++
            return privateResult
        }

        override suspend fun exportToCustomFolder(treeUriString: String, scope: BackupScope): Result<String> {
            customFolderCalls++
            return customFolderResult
        }

        override suspend fun listBackups(includeCustomFolder: String?): List<BackupMeta> = emptyList()
        override suspend fun deleteBackup(backup: BackupMeta) = Result.success(Unit)
        override suspend fun pruneOldBackups(maxRetained: Int, customFolderUri: String?) { pruneCalls++ }
    }

    private lateinit var context: Context
    private lateinit var settingsDataStore: SettingsDataStore

    private fun buildWorker(repo: FakeBackupRepository): AutoBackupWorker =
        TestListenableWorkerBuilder<AutoBackupWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters) =
                    AutoBackupWorker(appContext, workerParameters, repo, settingsDataStore)
            })
            .build()

    private fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        settingsDataStore = SettingsDataStore(context)
    }

    @Test
    fun privateOnly_succeeds_andNeverTouchesCustomFolder() = runBlocking {
        setup()
        settingsDataStore.saveAutoBackupConfig(AutoBackupConfig(storageMode = AutoBackupConfig.StorageMode.PRIVATE))
        val repo = FakeBackupRepository()

        val result = buildWorker(repo).doWork()

        assertTrue(result is androidx.work.ListenableWorker.Result.Success)
        assertEquals(1, repo.privateCalls)
        assertEquals(0, repo.customFolderCalls)
        assertEquals(1, repo.pruneCalls)
    }

    @Test
    fun customFolderOnly_failure_isNotMaskedByNeedlessPrivateWrite() = runBlocking {
        setup()
        settingsDataStore.saveAutoBackupConfig(
            AutoBackupConfig(
                storageMode = AutoBackupConfig.StorageMode.CUSTOM_FOLDER,
                customFolderUri = "content://fake/tree"
            )
        )
        val repo = FakeBackupRepository(customFolderResult = Result.failure(SecurityException("permission lost")))

        val result = buildWorker(repo).doWork()

        // The regression this guards against: the old worker always wrote a private copy and
        // reported success even though the only destination the user asked for failed.
        assertTrue(result is androidx.work.ListenableWorker.Result.Retry)
        assertEquals(0, repo.privateCalls)
        assertEquals(1, repo.customFolderCalls)
        assertEquals(0, repo.pruneCalls)
    }

    @Test
    fun both_requiresBothToSucceed() = runBlocking {
        setup()
        settingsDataStore.saveAutoBackupConfig(
            AutoBackupConfig(
                storageMode = AutoBackupConfig.StorageMode.BOTH,
                customFolderUri = "content://fake/tree"
            )
        )
        val repo = FakeBackupRepository(customFolderResult = Result.failure(IllegalStateException("folder gone")))

        val result = buildWorker(repo).doWork()

        assertTrue(result is androidx.work.ListenableWorker.Result.Retry)
        assertEquals(1, repo.privateCalls)
        assertEquals(1, repo.customFolderCalls)
        assertEquals(0, repo.pruneCalls)
    }

    @Test
    fun customFolder_withoutConfiguredUri_fails() = runBlocking {
        setup()
        settingsDataStore.saveAutoBackupConfig(
            AutoBackupConfig(storageMode = AutoBackupConfig.StorageMode.CUSTOM_FOLDER, customFolderUri = null)
        )
        val repo = FakeBackupRepository()

        val result = buildWorker(repo).doWork()

        assertTrue(result is androidx.work.ListenableWorker.Result.Retry)
        assertEquals(0, repo.customFolderCalls)
    }
}
