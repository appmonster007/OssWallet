package com.osscabinet.ossWallet.data.service

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.osscabinet.ossWallet.data.local.AppDatabase
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.data.local.entity.AccountEntity
import com.osscabinet.ossWallet.data.local.entity.CategoryEntity
import com.osscabinet.ossWallet.data.local.entity.TransactionEntity
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.import_csv.CsvImportSession
import com.osscabinet.ossWallet.domain.model.import_csv.DuplicatePolicy
import com.osscabinet.ossWallet.domain.model.import_csv.EntityMapping
import com.osscabinet.ossWallet.domain.model.import_csv.ImportSourceApp
import com.osscabinet.ossWallet.domain.model.import_csv.OssWalletField
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class CsvImportServiceInstrumentedTest {

    private lateinit var db: AppDatabase
    private lateinit var service: CsvImportServiceImpl
    private lateinit var settingsDataStore: SettingsDataStore

    // BudgetBakers-style semicolon-delimited CSV headers
    private val budgetBakersHeader =
        "account;category;currency;amount;ref_currency_amount;type;payment_type;payment_type_local;note;date;gps_latitude;gps_longitude;gps_accuracy_in_meters;warranty_in_month;transfer;payee;labels"

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        settingsDataStore = SettingsDataStore(context)
        service = CsvImportServiceImpl(
            accountDao = db.accountDao(),
            categoryDao = db.categoryDao(),
            transactionDao = db.transactionDao(),
            database = db,
            settingsDataStore = settingsDataStore
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    // ──────────────────────────────────────────────
    // parseHeaders
    // ──────────────────────────────────────────────

    @Test
    fun parseHeaders_budgetBakers_detectsSourceApp() = runBlocking {
        val csv = "$budgetBakersHeader\nSavings;Food;INR;-500;;Expenses;Cash;;Groceries;2024-03-15 10:00:00;;;;;0;false;;"
        val session = service.parseHeaders(ByteArrayInputStream(csv.toByteArray()))

        assertEquals(ImportSourceApp.BUDGET_BAKERS, session.sourceApp)
        assertEquals(';', session.delimiter)
    }

    @Test
    fun parseHeaders_budgetBakers_mapsKeyFields() = runBlocking {
        val csv = "$budgetBakersHeader\nSavings;Food;INR;-500;;Expenses;Cash;;Groceries;2024-03-15 10:00:00;;;;;0;false;;"
        val session = service.parseHeaders(ByteArrayInputStream(csv.toByteArray()))

        assertTrue("DATE should be mapped", session.mapping[OssWalletField.DATE] != null)
        assertTrue("AMOUNT should be mapped", session.mapping[OssWalletField.AMOUNT] != null)
        assertTrue("ACCOUNT should be mapped", session.mapping[OssWalletField.ACCOUNT] != null)
        assertTrue("NOTE should be mapped", session.mapping[OssWalletField.NOTE] != null)
    }

    @Test
    fun parseHeaders_generic_detectsCommaDelimiter() = runBlocking {
        val csv = "date,account,amount,note\n2024-01-01,Checking,100.00,Test"
        val session = service.parseHeaders(ByteArrayInputStream(csv.toByteArray()))

        assertEquals(ImportSourceApp.GENERIC, session.sourceApp)
        assertEquals(',', session.delimiter)
    }

    @Test
    fun parseHeaders_emptyStream_returnsEmptySession() = runBlocking {
        val session = service.parseHeaders(ByteArrayInputStream("".toByteArray()))

        assertTrue(session.headers.isEmpty())
        assertTrue(session.mapping.isEmpty())
    }

    // ──────────────────────────────────────────────
    // generatePreview
    // ──────────────────────────────────────────────

    @Test
    fun generatePreview_findsNewAccounts() = runBlocking {
        // Insert one existing account
        db.accountDao().upsertAccount(
            AccountEntity(
                id = "acc-existing",
                name = "Savings",
                currencyCode = "INR",
                structureType = "regular",
                colorHex = "#FF0000",
                icon = "wallet",
                allowedCurrencies = "INR",
                profileId = 1
            )
        )

        // Insert a category so the transaction foreign key is satisfied later
        db.categoryDao().insertCategory(
            CategoryEntity(id = 11000L, name = "Uncategorized", iconName = "question", colorHex = 0)
        )

        // CSV with two accounts: "Savings" (exists) and "NewWallet" (new)
        val csv = "account,date,amount,note\nSavings,2024-03-15,-100,Coffee\nNewWallet,2024-03-16,-200,Lunch"
        val session = service.parseHeaders(ByteArrayInputStream(csv.toByteArray()))
        val preview = service.generatePreview(session, ByteArrayInputStream(csv.toByteArray()))

        assertEquals(1, preview.newAccounts.size)
        assertEquals("NewWallet", preview.newAccounts.first())
    }

    @Test
    fun generatePreview_findsDuplicates() = runBlocking {
        // Date string we'll put in CSV
        val dateStr = "2024-03-15 10:00:00"
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val timestamp = sdf.parse(dateStr)!!.time

        // Insert account and category
        db.accountDao().upsertAccount(
            AccountEntity(
                id = "acc1",
                name = "Savings",
                currencyCode = "INR",
                structureType = "regular",
                colorHex = "#000000",
                icon = "wallet",
                allowedCurrencies = "INR",
                profileId = 1
            )
        )
        db.categoryDao().insertCategory(
            CategoryEntity(id = 11000L, name = "Uncategorized", iconName = "question", colorHex = 0)
        )

        // Insert a transaction that matches what the CSV will contain
        db.transactionDao().insertTransaction(
            TransactionEntity(
                id = 0,
                name = "Coffee",
                amount = BigDecimal("-100"),
                timestamp = timestamp,
                currency = "INR",
                transactionType = TransactionType.EXPENSE,
                accountId = "acc1",
                categoryId = 11000L,
                note = "Coffee",
                profileId = 1
            )
        )

        val csv = "account,date,amount,note\nSavings,$dateStr,-100,Coffee"
        val session = service.parseHeaders(ByteArrayInputStream(csv.toByteArray()))
        val preview = service.generatePreview(session, ByteArrayInputStream(csv.toByteArray()))

        assertEquals(1, preview.duplicates.size)
        assertEquals(0, preview.validRows)
    }

    @Test
    fun generatePreview_validRowCount_duplicateReducesValidCount() = runBlocking {
        val dateStr = "2024-03-15 10:00:00"
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val timestamp = sdf.parse(dateStr)!!.time

        db.accountDao().upsertAccount(
            AccountEntity(
                id = "acc1",
                name = "Savings",
                currencyCode = "INR",
                structureType = "regular",
                colorHex = "#000000",
                icon = "wallet",
                allowedCurrencies = "INR",
                profileId = 1
            )
        )
        db.categoryDao().insertCategory(
            CategoryEntity(id = 11000L, name = "Uncategorized", iconName = "question", colorHex = 0)
        )

        // Insert 1 transaction that matches row 1 of CSV — rows 2..5 are fresh
        db.transactionDao().insertTransaction(
            TransactionEntity(
                id = 0,
                name = "Dup",
                amount = BigDecimal("-100"),
                timestamp = timestamp,
                currency = "INR",
                transactionType = TransactionType.EXPENSE,
                accountId = "acc1",
                categoryId = 11000L,
                note = "DupNote",
                profileId = 1
            )
        )

        val rows = buildString {
            appendLine("account,date,amount,note")
            appendLine("Savings,$dateStr,-100,DupNote") // duplicate
            repeat(4) { i -> appendLine("Savings,2024-04-0${i + 1},-${(i + 1) * 10},Row$i") }
        }
        val session = service.parseHeaders(ByteArrayInputStream(rows.toByteArray()))
        val preview = service.generatePreview(session, ByteArrayInputStream(rows.toByteArray()))

        assertEquals(5, preview.totalRows)
        assertEquals(1, preview.duplicates.size)
        assertEquals(4, preview.validRows)
    }

    // ──────────────────────────────────────────────
    // executeImport
    // ──────────────────────────────────────────────

    @Test
    fun executeImport_importsRows() = runBlocking {
        // Pre-insert account and category required for FK constraints
        db.accountDao().upsertAccount(
            AccountEntity(
                id = "acc1",
                name = "Savings",
                currencyCode = "INR",
                structureType = "regular",
                colorHex = "#000000",
                icon = "wallet",
                allowedCurrencies = "INR",
                profileId = 1
            )
        )
        db.categoryDao().insertCategory(
            CategoryEntity(id = 11000L, name = "Uncategorized", iconName = "question", colorHex = 0)
        )

        val csv = buildString {
            appendLine("account,date,amount,note")
            appendLine("Savings,2024-03-01 00:00:00,-100,Row1")
            appendLine("Savings,2024-03-02 00:00:00,-200,Row2")
            appendLine("Savings,2024-03-03 00:00:00,-300,Row3")
        }

        val session = service.parseHeaders(ByteArrayInputStream(csv.toByteArray()))
        val result = service.executeImport(
            session = session,
            inputStream = ByteArrayInputStream(csv.toByteArray()),
            duplicatePolicy = DuplicatePolicy.SKIP,
            entityMappings = emptyList(),
            selectedDuplicateRowIndices = emptySet()
        )

        assertTrue(result.isSuccess)
        val summary = result.getOrThrow()
        assertEquals(3, summary.imported)
        assertEquals(0, summary.skipped)

        val txInDb = db.transactionDao().getAllTransactionsSync()
        assertEquals(3, txInDb.size)
    }

    @Test
    fun executeImport_skipsUnmappedAccounts() = runBlocking {
        // Insert only "Savings", not "Ghost"
        db.accountDao().upsertAccount(
            AccountEntity(
                id = "acc1",
                name = "Savings",
                currencyCode = "INR",
                structureType = "regular",
                colorHex = "#000000",
                icon = "wallet",
                allowedCurrencies = "INR",
                profileId = 1
            )
        )
        db.categoryDao().insertCategory(
            CategoryEntity(id = 11000L, name = "Uncategorized", iconName = "question", colorHex = 0)
        )

        val csv = buildString {
            appendLine("account,date,amount,note")
            appendLine("Savings,2024-03-01 00:00:00,-100,Kept")
            appendLine("Ghost,2024-03-02 00:00:00,-200,Skipped") // no account mapping
        }

        val session = service.parseHeaders(ByteArrayInputStream(csv.toByteArray()))
        val result = service.executeImport(
            session = session,
            inputStream = ByteArrayInputStream(csv.toByteArray()),
            duplicatePolicy = DuplicatePolicy.SKIP,
            entityMappings = emptyList(),
            selectedDuplicateRowIndices = emptySet()
        )

        assertTrue(result.isSuccess)
        val summary = result.getOrThrow()
        assertEquals(1, summary.imported)
        assertEquals(1, summary.skipped)
    }

    @Test
    fun executeImport_entityMappingCreatesNewAccount() = runBlocking {
        db.categoryDao().insertCategory(
            CategoryEntity(id = 11000L, name = "Uncategorized", iconName = "question", colorHex = 0)
        )

        val csv = buildString {
            appendLine("account,date,amount,note")
            appendLine("BrandNewWallet,2024-03-01 00:00:00,-500,Test")
        }

        val session = service.parseHeaders(ByteArrayInputStream(csv.toByteArray()))

        // Provide an EntityMapping that tells the service to create a new account
        val entityMappings = listOf(
            EntityMapping(
                unknownName = "BrandNewWallet",
                existingId = null, // null = create new
                entityType = EntityMapping.EntityType.ACCOUNT
            )
        )

        val result = service.executeImport(
            session = session,
            inputStream = ByteArrayInputStream(csv.toByteArray()),
            duplicatePolicy = DuplicatePolicy.SKIP,
            entityMappings = entityMappings,
            selectedDuplicateRowIndices = emptySet()
        )

        assertTrue(result.isSuccess)
        val summary = result.getOrThrow()
        assertEquals(1, summary.imported)

        // Confirm the account was created
        val accounts = db.accountDao().getAllAccountsSync()
        assertTrue(accounts.any { it.name == "BrandNewWallet" })
    }
}
