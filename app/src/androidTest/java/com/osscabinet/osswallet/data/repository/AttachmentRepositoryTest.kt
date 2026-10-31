package com.osscabinet.ossWallet.data.repository

import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.osscabinet.ossWallet.data.local.AppDatabase
import com.osscabinet.ossWallet.data.local.AttachmentStore
import com.osscabinet.ossWallet.data.local.BackupKeyVault
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.domain.model.AttachmentLimits
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AttachmentRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private lateinit var db: AppDatabase
    private lateinit var store: AttachmentStore
    private lateinit var repo: AttachmentRepositoryImpl

    // Not a decodable JPEG; the repository treats the payload as opaque bytes.
    private fun payload(seed: Int) = ByteArray(2048) { ((it * 31 + seed) and 0xFF).toByte() }
        .also { it[0] = 0xFF.toByte(); it[1] = 0xD8.toByte() }

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        store = AttachmentStore(context)
        store.deleteAll()
        repo = AttachmentRepositoryImpl(db.attachmentDao(), store)
    }

    @After
    fun teardown() {
        store.deleteAll()
        db.close()
    }

    @Test
    fun add_thenRead_roundTrips_andFileIsNotPlaintext() = runBlocking<Unit> {
        val bytes = payload(1)
        val saved = repo.add(transactionId = 7, profileId = 1, bytes = bytes)

        assertArrayEquals(bytes, repo.readBytes(saved))
        val onDisk = File(context.filesDir, "attachments/${saved.fileName}").readBytes()
        assertFalse("stored file must not start with the JPEG magic", onDisk[0] == 0xFF.toByte() && onDisk[1] == 0xD8.toByte())
        assertFalse("stored file must not contain the plaintext", String(onDisk, Charsets.ISO_8859_1).contains(String(bytes, Charsets.ISO_8859_1)))
    }

    @Test
    fun tamperedFile_failsToRead_insteadOfReturningGarbage() = runBlocking<Unit> {
        val saved = repo.add(7, 1, payload(2))
        val f = File(context.filesDir, "attachments/${saved.fileName}")
        f.readBytes().also { it[it.size / 2] = (it[it.size / 2].toInt() xor 0x01).toByte() }.let { f.writeBytes(it) }

        assertNull(repo.readBytes(saved))
    }

    @Test
    fun cap_isEnforcedPerTransaction() = runBlocking<Unit> {
        repeat(AttachmentLimits.MAX_PER_TRANSACTION) { repo.add(7, 1, payload(it)) }
        try {
            repo.add(7, 1, payload(99)); fail("expected the cap to be enforced")
        } catch (_: IllegalStateException) {
        }
        // another transaction is unaffected
        assertNotNull(repo.add(8, 1, payload(100)))
    }

    @Test
    fun delete_removesRowAndFile() = runBlocking<Unit> {
        val a = repo.add(7, 1, payload(3))
        repo.delete(a.id, 1)

        assertTrue(repo.getForTransaction(7, 1).isEmpty())
        assertFalse(File(context.filesDir, "attachments/${a.fileName}").exists())
    }

    @Test
    fun otherProfile_cannotDeleteOrSee() = runBlocking<Unit> {
        val a = repo.add(7, 1, payload(4))
        repo.delete(a.id, 2)

        assertEquals(1, repo.getForTransaction(7, 1).size)
        assertTrue(repo.getForTransaction(7, 2).isEmpty())
        assertTrue(File(context.filesDir, "attachments/${a.fileName}").exists())
    }

    @Test
    fun deleteForTransaction_removesAllItsFiles() = runBlocking<Unit> {
        val a = repo.add(7, 1, payload(5)); val b = repo.add(7, 1, payload(6)); val keep = repo.add(8, 1, payload(7))
        repo.deleteForTransaction(7, 1)

        assertTrue(repo.getForTransaction(7, 1).isEmpty())
        assertFalse(File(context.filesDir, "attachments/${a.fileName}").exists())
        assertFalse(File(context.filesDir, "attachments/${b.fileName}").exists())
        assertEquals(listOf(keep.id), repo.getForTransaction(8, 1).map { it.id })
    }

    @Test
    fun removeOrphans_dropsRowsWithoutTransaction_strayFiles_andRowsWithoutFiles() = runBlocking<Unit> {
        // No transaction rows exist, so everything here is an orphan row.
        val orphanRow = repo.add(404, 1, payload(8))
        val stray = File(context.filesDir, "attachments/stray.bin").apply { parentFile?.mkdirs(); writeBytes(byteArrayOf(1, 2, 3)) }

        repo.removeOrphans()

        assertTrue(repo.getForTransaction(404, 1).isEmpty())
        assertFalse(File(context.filesDir, "attachments/${orphanRow.fileName}").exists())
        assertFalse(stray.exists())
    }

    @Test
    fun clearAll_removesEverything() = runBlocking<Unit> {
        repo.add(1, 1, payload(9)); repo.add(2, 1, payload(10))
        repo.clearAll()

        assertTrue(store.listNames().isEmpty())
        assertTrue(db.attachmentDao().getAllForBackup().isEmpty())
    }

    @Test
    fun addShared_storesOneFile_andBothLegsSeeIt() = runBlocking<Unit> {
        val bytes = payload(21)
        val rows = repo.addShared(listOf(30, 31), 1, bytes, "application/pdf", "Invoice.pdf")

        assertEquals(2, rows.size)
        assertEquals("one file on disk", 1, store.listNames().size)
        assertArrayEquals(bytes, repo.readBytes(repo.getForTransaction(30, 1).single()))
        assertArrayEquals(bytes, repo.readBytes(repo.getForTransaction(31, 1).single()))
        assertEquals("Invoice.pdf", repo.getForTransaction(31, 1).single().displayName)
    }

    @Test
    fun delete_removesTheCopyOnTheOtherLegToo_andTheFile() = runBlocking<Unit> {
        val rows = repo.addShared(listOf(30, 31), 1, payload(22), "image/jpeg", "a.jpg")
        repo.delete(rows.first().id, 1)

        assertTrue(repo.getForTransaction(30, 1).isEmpty())
        assertTrue(repo.getForTransaction(31, 1).isEmpty())
        assertTrue(store.listNames().isEmpty())
    }

    @Test
    fun deleteForTransaction_keepsTheFileForTheOtherLeg() = runBlocking<Unit> {
        val bytes = payload(23)
        repo.addShared(listOf(30, 31), 1, bytes, "image/jpeg", "a.jpg")
        repo.deleteForTransaction(30, 1)

        assertTrue(repo.getForTransaction(30, 1).isEmpty())
        val remaining = repo.getForTransaction(31, 1).single()
        assertArrayEquals("the surviving leg can still read it", bytes, repo.readBytes(remaining))

        repo.deleteForTransaction(31, 1)
        assertTrue("last reference gone, file gone", store.listNames().isEmpty())
    }

    @Test
    fun shareWith_givesTheOtherLegACopyOfOneSidedAttachments_once() = runBlocking<Unit> {
        val old = repo.add(30, 1, payload(24), "image/jpeg", "old.jpg")
        repo.shareWith(listOf(old), listOf(30, 31), 1)
        repo.shareWith(listOf(old), listOf(30, 31), 1)

        assertEquals(1, repo.getForTransaction(30, 1).size)
        assertEquals("no duplicates on repeat", 1, repo.getForTransaction(31, 1).size)
        assertEquals(1, store.listNames().size)
    }

    @Test
    fun addShared_respectsTheCapOnEitherLeg_andStoresNothing() = runBlocking<Unit> {
        repeat(AttachmentLimits.MAX_PER_TRANSACTION) { repo.add(31, 1, payload(it)) }
        val before = store.listNames().size
        try {
            repo.addShared(listOf(30, 31), 1, payload(99), "image/jpeg", "x.jpg"); fail("expected the cap")
        } catch (_: IllegalStateException) {
        }
        assertTrue(repo.getForTransaction(30, 1).isEmpty())
        assertEquals(before, store.listNames().size)
    }

    @Test
    fun backup_roundTrip_restoresAttachments_andClearAllLeavesNone() = runBlocking<Unit> {
        val dir = File(context.filesDir, "backups").apply { deleteRecursively(); mkdirs() }
        // The export honours the device's real backup settings; force plaintext for this test, then put them back.
        val settings = SettingsDataStore(context)
        val originalConfig = settings.autoBackupConfig.first()
        settings.saveAutoBackupConfig(originalConfig.copy(encrypt = false))
        val backup = BackupRepositoryImpl(
            profileDao = db.profileDao(), accountDao = db.accountDao(), transactionDao = db.transactionDao(),
            categoryDao = db.categoryDao(), tagDao = db.tagDao(), templateDao = db.templateDao(),
            transactionLinkDao = db.transactionLinkDao(), budgetDao = db.budgetDao(), loanDao = db.loanDao(),
            loanPaymentDao = db.loanPaymentDao(), plannedPaymentDao = db.plannedPaymentDao(),
            plannedPaymentOccurrenceDao = db.plannedPaymentOccurrenceDao(), filterDao = db.filterDao(),
            currencyDao = db.currencyDao(), goalDao = db.goalDao(), attachmentDao = db.attachmentDao(),
            attachmentStore = store, backupKeyVault = BackupKeyVault(context), database = db,
            settingsDataStore = settings,
            dispatcherProvider = com.osscabinet.ossWallet.core.util.RealDispatcherProvider(),
            json = Json, context = context
        )
        try {
        val first = payload(11); val second = payload(12)
        repo.add(55, 1, first); repo.add(55, 1, second, "application/pdf", "Invoice March.pdf")

        val export = backup.exportToPrivateStorage()
        assertTrue("export failed: ${export.exceptionOrNull()}", export.isSuccess)
        val zip = dir.listFiles()!!.single { it.name.endsWith(".zip") }

        repo.clearAll()
        assertTrue(repo.getForTransaction(55, 1).isEmpty())

        val restored = backup.importData(Uri.fromFile(zip).toString(), com.osscabinet.ossWallet.domain.repository.BackupType.CSV_ZIP)
        assertTrue("restore failed: ${restored.exceptionOrNull()}", restored.isSuccess)

        val back = repo.getForTransaction(55, 1)
        assertEquals(2, back.size)
        assertEquals(setOf(first.toList(), second.toList()), back.map { repo.readBytes(it)!!.toList() }.toSet())
        val pdf = back.single { it.mimeType == "application/pdf" }
        assertEquals("Invoice March.pdf", pdf.displayName)
        assertFalse(pdf.isImage)
        } finally {
            settings.saveAutoBackupConfig(originalConfig)
            dir.deleteRecursively()
        }
    }

    /**
     * Database-file backups carry the sealed attachment files. The restore swaps the real database file,
     * so this round-trips the app's own database (identical bytes) and puts everything back afterwards.
     */
    @Test
    fun sqliteBackup_bundlesAttachmentFiles_andRestoreBringsThemBack() = runBlocking<Unit> {
        val dbFile = context.getDatabasePath("osswallet_manager_db")
        val dbCopy = File(context.cacheDir, "db_before_test").also { if (dbFile.exists()) dbFile.copyTo(it, overwrite = true) }
        val dir = File(context.filesDir, "backups").apply { deleteRecursively(); mkdirs() }
        val settings = SettingsDataStore(context)
        val originalConfig = settings.autoBackupConfig.first()
        settings.saveAutoBackupConfig(originalConfig.copy(encrypt = false))
        val backup = BackupRepositoryImpl(
            profileDao = db.profileDao(), accountDao = db.accountDao(), transactionDao = db.transactionDao(),
            categoryDao = db.categoryDao(), tagDao = db.tagDao(), templateDao = db.templateDao(),
            transactionLinkDao = db.transactionLinkDao(), budgetDao = db.budgetDao(), loanDao = db.loanDao(),
            loanPaymentDao = db.loanPaymentDao(), plannedPaymentDao = db.plannedPaymentDao(),
            plannedPaymentOccurrenceDao = db.plannedPaymentOccurrenceDao(), filterDao = db.filterDao(),
            currencyDao = db.currencyDao(), goalDao = db.goalDao(), attachmentDao = db.attachmentDao(),
            attachmentStore = store, backupKeyVault = BackupKeyVault(context), database = db,
            settingsDataStore = settings,
            dispatcherProvider = com.osscabinet.ossWallet.core.util.RealDispatcherProvider(),
            json = Json, context = context
        )
        try {
            val first = payload(41); val second = payload(42)
            val a = repo.add(60, 1, first); val b = repo.add(61, 1, second, "application/pdf", "x.pdf")
            val out = File(dir, "bundle.bin")
            val exported = backup.exportData(Uri.fromFile(out).toString(), com.osscabinet.ossWallet.domain.repository.BackupType.SQLITE,
                com.osscabinet.ossWallet.domain.repository.BackupScope.ALL_PROFILES, null)
            assertTrue("export failed: ${exported.exceptionOrNull()}", exported.isSuccess)

            val names = java.util.zip.ZipInputStream(out.inputStream()).use { zis ->
                generateSequence { zis.nextEntry }.map { it.name }.toList()
            }
            assertTrue("has the database", "database.db" in names || !dbFile.exists())
            assertTrue("has both sealed files", names.contains("attachments/${a.fileName}") && names.contains("attachments/${b.fileName}"))

            store.deleteAll()
            assertTrue(store.listNames().isEmpty())
            val restored = backup.importData(Uri.fromFile(out).toString(), com.osscabinet.ossWallet.domain.repository.BackupType.SQLITE, null)
            assertTrue("restore failed: ${restored.exceptionOrNull()}", restored.isSuccess)

            assertEquals(setOf(a.fileName, b.fileName), store.listNames().toSet())
            assertArrayEquals(first, repo.readBytes(a))
            assertArrayEquals(second, repo.readBytes(b))
        } finally {
            if (dbCopy.exists()) dbCopy.copyTo(dbFile, overwrite = true)
            dbCopy.delete()
            settings.saveAutoBackupConfig(originalConfig)
            dir.deleteRecursively()
        }
    }

    @Test
    fun sqliteRestore_ofAnOlderBareDatabaseFile_leavesAttachmentsAlone() = runBlocking<Unit> {
        val dbFile = context.getDatabasePath("osswallet_manager_db")
        if (!dbFile.exists()) return@runBlocking
        val dbCopy = File(context.cacheDir, "db_before_legacy_test").also { dbFile.copyTo(it, overwrite = true) }
        val dir = File(context.filesDir, "backups").apply { deleteRecursively(); mkdirs() }
        val settings = SettingsDataStore(context)
        val backup = BackupRepositoryImpl(
            profileDao = db.profileDao(), accountDao = db.accountDao(), transactionDao = db.transactionDao(),
            categoryDao = db.categoryDao(), tagDao = db.tagDao(), templateDao = db.templateDao(),
            transactionLinkDao = db.transactionLinkDao(), budgetDao = db.budgetDao(), loanDao = db.loanDao(),
            loanPaymentDao = db.loanPaymentDao(), plannedPaymentDao = db.plannedPaymentDao(),
            plannedPaymentOccurrenceDao = db.plannedPaymentOccurrenceDao(), filterDao = db.filterDao(),
            currencyDao = db.currencyDao(), goalDao = db.goalDao(), attachmentDao = db.attachmentDao(),
            attachmentStore = store, backupKeyVault = BackupKeyVault(context), database = db,
            settingsDataStore = settings,
            dispatcherProvider = com.osscabinet.ossWallet.core.util.RealDispatcherProvider(),
            json = Json, context = context
        )
        try {
            val kept = repo.add(70, 1, payload(51))
            val bare = File(dir, "old.db").also { dbCopy.copyTo(it, overwrite = true) }
            val restored = backup.importData(Uri.fromFile(bare).toString(), com.osscabinet.ossWallet.domain.repository.BackupType.SQLITE, null)
            assertTrue("restore failed: ${restored.exceptionOrNull()}", restored.isSuccess)
            assertEquals("files untouched by a bare-database restore", listOf(kept.fileName), store.listNames())
        } finally {
            dbCopy.copyTo(dbFile, overwrite = true)
            dbCopy.delete()
            dir.deleteRecursively()
        }
    }

    @Test
    fun sqliteRestore_ignoresForeignNamesInTheBundle() = runBlocking<Unit> {
        // Names that this store didn't generate (path tricks, odd files) must never land in the store.
        assertTrue(AttachmentStore.isStoreFileName("0f8fad5b-d9cb-469f-a165-70867728950e.bin"))
        listOf("../evil.bin", "x.bin", "0f8fad5b-d9cb-469f-a165-70867728950e.bin.tmp", "0F8FAD5B-D9CB-469F-A165-70867728950E.bin", "a/b.bin")
            .forEach { assertFalse(it, AttachmentStore.isStoreFileName(it)) }
    }
}
