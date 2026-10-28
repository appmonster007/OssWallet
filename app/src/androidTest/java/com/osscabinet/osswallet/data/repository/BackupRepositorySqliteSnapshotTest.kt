package com.osscabinet.ossWallet.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.osscabinet.ossWallet.data.local.AppDatabase
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.data.local.entity.CategoryEntity
import com.osscabinet.ossWallet.domain.model.CategoryNature
import com.osscabinet.ossWallet.domain.repository.BackupType
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.zip.ZipInputStream

/**
 * Regresses the "copy the live db file off disk" bug: a raw [File] copy of an actively-open Room
 * database can race a concurrent writer. [BackupRepositoryImpl.exportSqlite] instead asks SQLCipher
 * for a `VACUUM INTO` snapshot, which must still see every row committed just before the export ran.
 */
@RunWith(AndroidJUnit4::class)
class BackupRepositorySqliteSnapshotTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: BackupRepositoryImpl
    private lateinit var outputFile: File

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

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
            goalDao = db.goalDao(),
            attachmentDao = db.attachmentDao(),
            attachmentStore = com.osscabinet.ossWallet.data.local.AttachmentStore(context),
            backupKeyVault = com.osscabinet.ossWallet.data.local.BackupKeyVault(context),
            database = db,
            settingsDataStore = SettingsDataStore(context),
            dispatcherProvider = com.osscabinet.ossWallet.core.util.RealDispatcherProvider(),
            json = Json,
            context = context
        )
        outputFile = File(context.cacheDir, "snapshot_test_export.zip")
    }

    @After
    fun teardown() {
        outputFile.delete()
        db.close()
    }

    @Test
    fun exportSqlite_capturesRowCommittedJustBeforeExport() = runBlocking {
        categoryDaoInsert(9001, "Snapshot Test Category")

        val result = repository.exportData(outputFile.toURI().toString(), BackupType.SQLITE)
        assertTrue("exportData should succeed: ${result.exceptionOrNull()}", result.isSuccess)

        val extractedDb = File(outputFile.parentFile, "snapshot_test_extracted.db")
        var sawDbEntry = false
        ZipInputStream(outputFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name == "database.db") {
                    sawDbEntry = true
                    extractedDb.outputStream().use { out -> zis.copyTo(out) }
                }
                entry = zis.nextEntry
            }
        }
        assertTrue("Exported zip must contain database.db", sawDbEntry)

        // The snapshot must reflect the row committed just before export — this is the actual
        // regression check for the old "copy the live file" approach.
        android.database.sqlite.SQLiteDatabase
            .openDatabase(extractedDb.absolutePath, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY)
            .use { sqlite ->
                sqlite.rawQuery("SELECT name FROM categories WHERE id = 9001", null).use { cursor ->
                    assertTrue("Row committed before export must be present in the snapshot", cursor.moveToFirst())
                    assertTrue(cursor.getString(0) == "Snapshot Test Category")
                }
            }
        extractedDb.delete()
    }

    private suspend fun categoryDaoInsert(id: Long, name: String) {
        db.categoryDao().insertCategory(
            CategoryEntity(id, name, "icon", 0, false, null, CategoryNature.WANT)
        )
    }
}
