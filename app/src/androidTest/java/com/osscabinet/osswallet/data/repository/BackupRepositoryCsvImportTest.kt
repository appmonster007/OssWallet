package com.osscabinet.ossWallet.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.osscabinet.ossWallet.data.local.AppDatabase
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.domain.repository.BackupType
import com.osscabinet.ossWallet.domain.repository.RestorableEntity
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class BackupRepositoryCsvImportTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: BackupRepositoryImpl
    private lateinit var tempDir: File

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
            goalDao = db.goalDao(),
            attachmentDao = db.attachmentDao(),
            attachmentStore = com.osscabinet.ossWallet.data.local.AttachmentStore(context),
            backupKeyVault = com.osscabinet.ossWallet.data.local.BackupKeyVault(context),
            database = db,
            settingsDataStore = settingsDataStore,
            dispatcherProvider = com.osscabinet.ossWallet.core.util.RealDispatcherProvider(),
            json = Json,
            context = context
        )

        tempDir = File(context.cacheDir, "csv_test_dir").apply { mkdirs() }
    }

    @After
    fun teardown() {
        tempDir.deleteRecursively()
        db.close()
    }

    @Test
    fun detectBackupEntity_categoriesCsv_returnsCategories() = runBlocking {
        val csvFile = File(tempDir, "categories.csv").apply {
            writeText("id,name,iconName,colorHex,isDefault,parentId,nature\n1001,Food,ic_food,-1,false,,WANT")
        }

        val detected = repository.detectBackupEntity(csvFile.toURI().toString())

        assertEquals(RestorableEntity.CATEGORIES, detected)
    }

    @Test
    fun detectBackupEntity_tagsCsv_returnsTags() = runBlocking {
        val csvFile = File(tempDir, "tags.csv").apply {
            writeText("id,name,colorHex\n1,Urgent,-65536")
        }

        val detected = repository.detectBackupEntity(csvFile.toURI().toString())

        assertEquals(RestorableEntity.TAGS, detected)
    }

    @Test
    fun verifyBackup_singleCsv_returnsValidVerification() = runBlocking {
        val csvFile = File(tempDir, "categories.csv").apply {
            writeText("id,name,iconName,colorHex,isDefault,parentId,nature\n1001,Food,ic_food,-1,false,,WANT")
        }

        val result = repository.verifyBackup(csvFile.toURI().toString(), BackupType.CSV_ZIP)

        assertTrue(result.isSuccess)
        val verification = result.getOrNull()
        assertNotNull(verification)
        assertEquals(1, verification?.entryCount)
    }

    @Test
    fun importData_singleCategoriesCsv_bulkUpdatesCategories() = runBlocking {
        val csvFile = File(tempDir, "categories.csv").apply {
            writeText("id,name,iconName,colorHex,isDefault,parentId,nature\n5001,Utilities,ic_utility,-16711936,false,,NEED")
        }

        val result = repository.importData(csvFile.toURI().toString(), BackupType.CSV_ZIP)

        assertTrue(result.isSuccess)
        val categories = db.categoryDao().getAllCategoriesSync()
        val imported = categories.find { it.id == 5001L }
        assertNotNull(imported)
        assertEquals("Utilities", imported?.name)
    }

    @Test
    fun importDataSelective_singleTagsCsv_bulkUpdatesTags() = runBlocking {
        val csvFile = File(tempDir, "tags.csv").apply {
            writeText("id,name,colorHex\n99,Subscriptions,-16776961")
        }

        val result = repository.importDataSelective(
            csvFile.toURI().toString(),
            BackupType.CSV_ZIP,
            setOf(RestorableEntity.TAGS)
        )

        assertTrue(result.isSuccess)
        val tags = db.tagDao().getAllTagsSync()
        val imported = tags.find { it.id == 99L }
        assertNotNull(imported)
        assertEquals("Subscriptions", imported?.name)
    }
}
