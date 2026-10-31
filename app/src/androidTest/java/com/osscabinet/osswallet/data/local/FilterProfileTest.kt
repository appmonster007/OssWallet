package com.osscabinet.ossWallet.data.local

import android.content.Context
import androidx.room.Room
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

/** Saved filters belong to one profile: the DAO keeps them apart. */
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
}
