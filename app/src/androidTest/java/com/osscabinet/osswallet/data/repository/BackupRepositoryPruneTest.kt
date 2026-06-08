package com.osscabinet.ossWallet.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.osscabinet.ossWallet.data.local.AppDatabase
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.data.repository.BackupRepositoryImpl
import com.osscabinet.ossWallet.domain.repository.BackupScope
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class BackupRepositoryPruneTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: BackupRepositoryImpl
    private lateinit var backupsDir: File

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val settingsDataStore = SettingsDataStore(context)

        repository = BackupRepositoryImpl(
            profileDao = db.profileDao(),
            accountDao = db.accountDao(),
            transactionDao = db.transactionDao(),
            categoryDao = db.categoryDao(),
            tagDao = db.tagDao(),
            templateDao = db.templateDao(),
            transactionLinkDao = db.transactionLinkDao(),
            budgetDao = db.budgetDao(),
            loanDao = db.loanDao(),
            loanPaymentDao = db.loanPaymentDao(),
            plannedPaymentDao = db.plannedPaymentDao(),
            plannedPaymentOccurrenceDao = db.plannedPaymentOccurrenceDao(),
            filterDao = db.filterDao(),
            currencyDao = db.currencyDao(),
            database = db,
            settingsDataStore = settingsDataStore,
            context = context
        )

        // Clean up the backups directory before each test
        backupsDir = File(context.filesDir, "backups")
        backupsDir.deleteRecursively()
        backupsDir.mkdirs()
    }

    @After
    fun teardown() {
        backupsDir.deleteRecursively()
        db.close()
    }

    private fun createFakeZip(name: String, lastModifiedOffset: Long = 0L): File {
        val file = File(backupsDir, name)
        file.writeText("fake zip content")
        // Set distinct lastModified so sorting is predictable
        file.setLastModified(System.currentTimeMillis() + lastModifiedOffset)
        return file
    }

    @Test
    fun listPrivateBackups_returnsFiles() = runBlocking {
        createFakeZip("backup_100_all.zip", lastModifiedOffset = 0)
        createFakeZip("backup_200_all.zip", lastModifiedOffset = 1000)
        createFakeZip("backup_300_all.zip", lastModifiedOffset = 2000)

        val result = repository.listPrivateBackups()

        assertEquals(3, result.size)
    }

    @Test
    fun listPrivateBackups_returnsSortedByDateDescending() = runBlocking {
        val oldest = createFakeZip("osswallet_backup_100_all.zip", lastModifiedOffset = 0)
        val middle = createFakeZip("osswallet_backup_200_all.zip", lastModifiedOffset = 1000)
        val newest = createFakeZip("osswallet_backup_300_all.zip", lastModifiedOffset = 2000)

        val result = repository.listPrivateBackups()

        assertEquals(newest.name, result[0].id)
        assertEquals(middle.name, result[1].id)
        assertEquals(oldest.name, result[2].id)
    }

    @Test
    fun listPrivateBackups_emptyWhenNoDirExists() = runBlocking {
        backupsDir.deleteRecursively()

        val result = repository.listPrivateBackups()

        assertTrue(result.isEmpty())
    }

    @Test
    fun listPrivateBackups_scopeDetection_singleProfile() = runBlocking {
        createFakeZip("osswallet_backup_100_profile.zip", lastModifiedOffset = 0)

        val result = repository.listPrivateBackups()

        assertEquals(1, result.size)
        assertEquals(BackupScope.SINGLE_PROFILE, result[0].scope)
    }

    @Test
    fun listPrivateBackups_scopeDetection_allProfiles() = runBlocking {
        createFakeZip("osswallet_backup_100_all.zip", lastModifiedOffset = 0)

        val result = repository.listPrivateBackups()

        assertEquals(1, result.size)
        assertEquals(BackupScope.ALL_PROFILES, result[0].scope)
    }

    @Test
    fun deleteBackup_removesFile() = runBlocking {
        val file = createFakeZip("osswallet_backup_to_delete_all.zip")
        assertTrue("File should exist before deletion", file.exists())

        val result = repository.deleteBackup(file.name)

        assertTrue(result.isSuccess)
        assertFalse("File should be gone after deletion", file.exists())
    }

    @Test
    fun deleteBackup_nonExistentFile_succeeds() = runBlocking {
        val result = repository.deleteBackup("nonexistent_backup.zip")
        assertTrue(result.isSuccess)
    }

    @Test
    fun pruneOldBackups_removesExcess() = runBlocking {
        // Create 5 files with distinct timestamps so sort order is deterministic
        for (i in 1..5) {
            createFakeZip("osswallet_backup_${i * 1000}_all.zip", lastModifiedOffset = (i * 1000).toLong())
        }

        repository.pruneOldBackups(maxRetained = 3)

        val remaining = repository.listPrivateBackups()
        assertEquals(3, remaining.size)
    }

    @Test
    fun pruneOldBackups_keepsNewest() = runBlocking {
        for (i in 1..5) {
            createFakeZip("osswallet_backup_${i * 1000}_all.zip", lastModifiedOffset = (i * 1000).toLong())
        }

        repository.pruneOldBackups(maxRetained = 2)

        val remaining = repository.listPrivateBackups()
        // The newest two should be retained (highest lastModified)
        assertEquals(2, remaining.size)
        assertTrue(remaining[0].id.contains("5000"))
        assertTrue(remaining[1].id.contains("4000"))
    }

    @Test
    fun pruneOldBackups_doesNothingWhenBelowLimit() = runBlocking {
        createFakeZip("osswallet_backup_100_all.zip", lastModifiedOffset = 0)
        createFakeZip("osswallet_backup_200_all.zip", lastModifiedOffset = 1000)

        repository.pruneOldBackups(maxRetained = 5)

        val remaining = repository.listPrivateBackups()
        assertEquals(2, remaining.size)
    }
}
