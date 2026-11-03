package com.osscabinet.ossWallet.data.local.query

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.osscabinet.ossWallet.data.local.AppDatabase
import com.osscabinet.ossWallet.data.local.entity.AccountEntity
import com.osscabinet.ossWallet.data.local.entity.TransactionEntity
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.TransactionType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

/**
 * One opening balance per account and currency. The index is created by AppDatabase's create
 * callback, which an in-memory builder does not run, so this test creates the same index itself.
 */
@RunWith(AndroidJUnit4::class)
class OpeningBalanceUniquenessDaoTest {
    private lateinit var db: AppDatabase
    private val dao get() = db.transactionDao()

    @Before
    fun setup() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        db.accountDao().upsertAccount(
            AccountEntity(id = "acc1", name = "Cash", currencyCode = "USD", structureType = "STANDARD",
                colorHex = "#fff", icon = "bank", allowedCurrencies = "", profileId = 1)
        )
        db.openHelper.writableDatabase.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_transactions_opening_balance_unique " +
                "ON transactions(profileId, accountId, currency) " +
                "WHERE categoryId = ${Category.SYSTEM_INITIAL_BALANCE}"
        )
    }

    @After
    fun tearDown() = db.close()

    private suspend fun openingBalance(currency: String, profile: Int = 1, accountId: String = "acc1") =
        dao.insertTransaction(
            TransactionEntity(name = "Initial Balance", amount = BigDecimal("100"), timestamp = 1_000L,
                currency = currency, transactionType = TransactionType.INCOME, accountId = accountId,
                categoryId = Category.SYSTEM_INITIAL_BALANCE, profileId = profile)
        )

    @Test
    fun secondOpeningBalanceForSameAccountAndCurrencyIsRejected() = runBlocking<Unit> {
        openingBalance("USD")

        val failure = runCatching { openingBalance("USD") }.exceptionOrNull()

        assertTrue(failure is SQLiteConstraintException)
    }

    @Test
    fun sameCurrencyInAnotherProfileOrAccountIsAllowed() = runBlocking<Unit> {
        openingBalance("USD")
        openingBalance("USD", profile = 2)

        db.accountDao().upsertAccount(
            AccountEntity(id = "acc2", name = "Bank", currencyCode = "USD", structureType = "STANDARD",
                colorHex = "#fff", icon = "bank", allowedCurrencies = "", profileId = 1)
        )
        openingBalance("USD", accountId = "acc2")

        assertEquals(listOf("USD"), dao.getCurrenciesForCategory("acc1", Category.SYSTEM_INITIAL_BALANCE, 1))
    }

    @Test
    fun ordinaryTransactionsDoNotCountAsOpeningBalances() = runBlocking<Unit> {
        dao.insertTransaction(
            TransactionEntity(name = "Coffee", amount = BigDecimal("-3"), timestamp = 1_000L, currency = "EUR",
                transactionType = TransactionType.EXPENSE, accountId = "acc1",
                categoryId = Category.SYSTEM_OTHERS, profileId = 1)
        )
        openingBalance("USD")

        assertEquals(listOf("USD"), dao.getCurrenciesForCategory("acc1", Category.SYSTEM_INITIAL_BALANCE, 1))
    }
}
