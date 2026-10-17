package com.osscabinet.ossWallet.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.osscabinet.ossWallet.data.local.entity.FilterEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Saved filters belong to one profile: the DAO keeps them apart, and the v4 -> v5 migration assigns owners. */
@RunWith(AndroidJUnit4::class)
class FilterProfileTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun filtersAreListedPerProfile_andOthersCannotBeReadByIdOrCounted() = runBlocking<Unit> {
        val dao = db.filterDao()
        val mine = dao.insertFilter(FilterEntity(name = "Mine", profileId = 1))
        val theirs = dao.insertFilter(FilterEntity(name = "Theirs", profileId = 2))
        dao.insertFilter(FilterEntity(name = "Also mine", profileId = 1))

        assertEquals(listOf("Also mine", "Mine"), dao.getAllFilters(1).first().map { it.name })
        assertEquals(listOf("Theirs"), dao.getAllFilters(2).first().map { it.name })
        assertTrue(dao.getAllFilters(3).first().isEmpty())

        assertEquals("Mine", dao.getFilterById(mine, 1)?.name)
        assertNull("another profile's filter is invisible by id", dao.getFilterById(theirs, 1))
        assertEquals(0, dao.getFilterUsageCount(theirs, 1))
    }

    @Test
    fun backup_keepsFilterProfileAndTagMatching_andASingleProfileBackupTakesOnlyItsOwn() = runBlocking<Unit> {
        val dir = java.io.File(context.filesDir, "backups_filter_test").apply { deleteRecursively(); mkdirs() }
        val settings = SettingsDataStore(context)
        val originalConfig = settings.autoBackupConfig.first()
        settings.saveAutoBackupConfig(originalConfig.copy(encrypt = false))
        val currentProfile = settings.currentProfileId.first()
        val backup = com.osscabinet.ossWallet.data.repository.BackupRepositoryImpl(
            profileDao = db.profileDao(), accountDao = db.accountDao(), transactionDao = db.transactionDao(),
            categoryDao = db.categoryDao(), tagDao = db.tagDao(), templateDao = db.templateDao(),
            transactionLinkDao = db.transactionLinkDao(), budgetDao = db.budgetDao(), loanDao = db.loanDao(),
            loanPaymentDao = db.loanPaymentDao(), plannedPaymentDao = db.plannedPaymentDao(),
            plannedPaymentOccurrenceDao = db.plannedPaymentOccurrenceDao(), filterDao = db.filterDao(),
            currencyDao = db.currencyDao(), goalDao = db.goalDao(), attachmentDao = db.attachmentDao(),
            attachmentStore = AttachmentStore(context), backupKeyVault = BackupKeyVault(context), database = db,
            settingsDataStore = settings,
            dispatcherProvider = com.osscabinet.ossWallet.core.util.RealDispatcherProvider(),
            json = kotlinx.serialization.json.Json, context = context
        )
        try {
            val dao = db.filterDao()
            dao.insertFilter(FilterEntity(name = "Own", profileId = currentProfile, tags = setOf("a", "b"), matchAllTags = false))
            dao.insertFilter(FilterEntity(name = "Other", profileId = currentProfile + 1))

            // A full backup carries both, with their owners and the any/all tag choice.
            val full = java.io.File(dir, "full.zip")
            assertTrue(backup.exportData(android.net.Uri.fromFile(full).toString(),
                com.osscabinet.ossWallet.domain.repository.BackupType.CSV_ZIP,
                com.osscabinet.ossWallet.domain.repository.BackupScope.ALL_PROFILES, null).isSuccess)
            dao.clearAll()
            assertTrue(backup.importData(android.net.Uri.fromFile(full).toString(),
                com.osscabinet.ossWallet.domain.repository.BackupType.CSV_ZIP, null).isSuccess)
            val restored = dao.getAllFiltersSync().associateBy { it.name }
            assertEquals(currentProfile, restored.getValue("Own").profileId)
            assertEquals(currentProfile + 1, restored.getValue("Other").profileId)
            assertEquals(false, restored.getValue("Own").matchAllTags)
            assertEquals(setOf("a", "b"), restored.getValue("Own").tags)

            // A single-profile backup leaves the other profile's filters out.
            val single = java.io.File(dir, "single.zip")
            assertTrue(backup.exportData(android.net.Uri.fromFile(single).toString(),
                com.osscabinet.ossWallet.domain.repository.BackupType.CSV_ZIP,
                com.osscabinet.ossWallet.domain.repository.BackupScope.SINGLE_PROFILE, null).isSuccess)
            dao.clearAll()
            assertTrue(backup.importData(android.net.Uri.fromFile(single).toString(),
                com.osscabinet.ossWallet.domain.repository.BackupType.CSV_ZIP, null).isSuccess)
            assertEquals(listOf("Own"), dao.getAllFiltersSync().map { it.name })
        } finally {
            settings.saveAutoBackupConfig(originalConfig)
            dir.deleteRecursively()
        }
    }

    @Test
    fun aNewFilterDefaultsToProfileOne() = runBlocking<Unit> {
        val id = db.filterDao().insertFilter(FilterEntity(name = "Plain"))
        assertEquals(1, db.filterDao().getFilterById(id, 1)?.profileId)
    }

    @Test
    fun migration_givesBudgetAndLoanFiltersTheirOwnersProfile_andLeavesTheRestWithProfileOne() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null) // in-memory
                .callback(object : SupportSQLiteOpenHelper.Callback(4) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        // Just the columns the migration touches, as they were in schema v4.
                        db.execSQL("CREATE TABLE transaction_filters (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL)")
                        db.execSQL("CREATE TABLE budgets (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, profileId INTEGER NOT NULL, filterId INTEGER NOT NULL)")
                        db.execSQL("CREATE TABLE loans (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, profileId INTEGER NOT NULL, filterId INTEGER NOT NULL)")
                        db.execSQL("INSERT INTO transaction_filters (id, name) VALUES (1, 'budget filter'), (2, 'loan filter'), (3, 'saved by hand')")
                        db.execSQL("INSERT INTO budgets (profileId, filterId) VALUES (2, 1)")
                        db.execSQL("INSERT INTO loans (profileId, filterId) VALUES (3, 2)")
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        helper.use {
            val sql = it.writableDatabase
            AppDatabase.MIGRATION_4_5.migrate(sql)

            fun profileOf(id: Int) = sql.query("SELECT profileId FROM transaction_filters WHERE id = $id").use { c -> c.moveToFirst(); c.getInt(0) }
            assertEquals(2, profileOf(1))
            assertEquals(3, profileOf(2))
            assertEquals(1, profileOf(3))
            sql.query("SELECT name FROM sqlite_master WHERE type='index' AND name='index_transaction_filters_profileId'").use { c ->
                assertTrue("profile index is created", c.moveToFirst())
            }
        }
    }
}
