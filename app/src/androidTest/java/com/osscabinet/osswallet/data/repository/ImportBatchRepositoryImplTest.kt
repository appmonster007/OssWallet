package com.osscabinet.ossWallet.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.osscabinet.ossWallet.data.local.AppDatabase
import com.osscabinet.ossWallet.data.local.entity.AccountEntity
import com.osscabinet.ossWallet.data.local.entity.CategoryEntity
import com.osscabinet.ossWallet.data.local.entity.TransactionEntity
import com.osscabinet.ossWallet.domain.model.TransactionType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

/**
 * Integration tests for [ImportBatchRepositoryImpl.undoImport] against a real (in-memory) Room
 * database — verifies the core safety property: an account/category created by an import is only
 * deleted on undo if it has zero remaining transactions, so anything the user has since used
 * elsewhere survives.
 *
 * No SQLCipher passphrase is needed for in-memory databases.
 */
@RunWith(AndroidJUnit4::class)
class ImportBatchRepositoryImplTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: ImportBatchRepositoryImpl

    private val profileId = 1

    // Existing (pre-import) account/category — never candidates for deletion.
    private val existingAccountId = "acc-existing"
    private val existingCategoryId = 10L

    // Created-by-import account/category — candidates for deletion, conditional on usage.
    private val createdAccountId = "acc-created"
    private val createdCategoryId = 20L

    @Before
    fun setup() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ImportBatchRepositoryImpl(db.importBatchDao())

        db.accountDao().upsertAccount(
            AccountEntity(
                id = existingAccountId, name = "Existing Account", currencyCode = "USD",
                structureType = "BANK", colorHex = "#fff", icon = "bank",
                allowedCurrencies = "USD", profileId = profileId
            )
        )
        db.accountDao().upsertAccount(
            AccountEntity(
                id = createdAccountId, name = "Created Account", currencyCode = "USD",
                structureType = "BANK", colorHex = "#fff", icon = "wallet",
                allowedCurrencies = "USD", profileId = profileId
            )
        )
        db.categoryDao().insertCategory(
            CategoryEntity(
                id = existingCategoryId, name = "Existing Category",
                iconName = "food", colorHex = 0, isDefault = false
            )
        )
        db.categoryDao().insertCategory(
            CategoryEntity(
                id = createdCategoryId, name = "Created Category",
                iconName = "food", colorHex = 0, isDefault = false
            )
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun insertTx(
        name: String,
        accountId: String,
        categoryId: Long,
        importBatchId: String?
    ): Long = db.transactionDao()._insertTransaction(
        TransactionEntity(
            name = name,
            amount = BigDecimal("10.00"),
            timestamp = System.currentTimeMillis(),
            currency = "USD",
            transactionType = TransactionType.EXPENSE,
            accountId = accountId,
            categoryId = categoryId,
            profileId = profileId,
            importBatchId = importBatchId
        )
    )

    @Test
    fun undoImport_returnsFalse_whenBatchIsUnknown() = runBlocking {
        val result = repository.undoImport("no-such-batch")

        assertFalse(result)
    }

    @Test
    fun undoImport_deletesOnlyBatchTaggedTransactions() = runBlocking {
        val keptTxId = insertTx("Pre-existing", existingAccountId, existingCategoryId, importBatchId = null)
        val batchTxId = insertTx("Imported", existingAccountId, existingCategoryId, importBatchId = "batch-1")

        repository.recordBatch(
            batchId = "batch-1",
            sourceApp = "GENERIC",
            transactionCount = 1,
            createdAccountIds = emptyList(),
            createdCategoryIds = emptyList()
        )

        val result = repository.undoImport("batch-1")

        assertTrue(result)
        assertNotNull(db.transactionDao().getTransactionById(keptTxId, profileId))
        assertNull(db.transactionDao().getTransactionById(batchTxId, profileId))
    }

    @Test
    fun undoImport_deletesCreatedAccountAndCategory_whenNoTransactionsRemain() = runBlocking {
        insertTx("Imported", createdAccountId, createdCategoryId, importBatchId = "batch-2")
        repository.recordBatch(
            batchId = "batch-2",
            sourceApp = "GENERIC",
            transactionCount = 1,
            createdAccountIds = listOf(createdAccountId),
            createdCategoryIds = listOf(createdCategoryId)
        )

        repository.undoImport("batch-2")

        assertTrue(db.accountDao().getAllAccountsSync(profileId).none { it.id == createdAccountId })
        assertTrue(db.categoryDao().getAllCategoriesSync().none { it.id == createdCategoryId })
    }

    @Test
    fun undoImport_keepsCreatedAccountAndCategory_whenUserAddedAnotherTransactionSince() = runBlocking {
        insertTx("Imported", createdAccountId, createdCategoryId, importBatchId = "batch-3")
        // Simulates the user adding a manual transaction to the newly-created account/category
        // after the import but before undo.
        insertTx("Added by user", createdAccountId, createdCategoryId, importBatchId = null)

        repository.recordBatch(
            batchId = "batch-3",
            sourceApp = "GENERIC",
            transactionCount = 1,
            createdAccountIds = listOf(createdAccountId),
            createdCategoryIds = listOf(createdCategoryId)
        )

        repository.undoImport("batch-3")

        assertNotNull(db.accountDao().getAllAccountsSync(profileId).find { it.id == createdAccountId })
        assertNotNull(db.categoryDao().getAllCategoriesSync().find { it.id == createdCategoryId })
    }

    @Test
    fun undoImport_neverConsidersExistingAccountsOrCategoriesForDeletion() = runBlocking {
        insertTx("Imported", existingAccountId, existingCategoryId, importBatchId = "batch-4")
        repository.recordBatch(
            batchId = "batch-4",
            sourceApp = "GENERIC",
            transactionCount = 1,
            createdAccountIds = emptyList(),
            createdCategoryIds = emptyList()
        )

        repository.undoImport("batch-4")

        assertNotNull(db.accountDao().getAllAccountsSync(profileId).find { it.id == existingAccountId })
        assertNotNull(db.categoryDao().getAllCategoriesSync().find { it.id == existingCategoryId })
    }

    @Test
    fun undoImport_removesTheBatchRecordItself() = runBlocking {
        repository.recordBatch(
            batchId = "batch-5",
            sourceApp = "GENERIC",
            transactionCount = 0,
            createdAccountIds = emptyList(),
            createdCategoryIds = emptyList()
        )

        repository.undoImport("batch-5")

        assertNull(db.importBatchDao().getBatchById("batch-5"))
        // A second undo of the same (now-forgotten) batch is a safe no-op.
        assertFalse(repository.undoImport("batch-5"))
    }
}
