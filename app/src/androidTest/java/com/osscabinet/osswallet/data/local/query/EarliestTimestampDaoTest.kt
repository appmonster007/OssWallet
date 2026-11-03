package com.osscabinet.ossWallet.data.local.query

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
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

/** The earliest-date lookup that anchors opening balances, against a real (in-memory) database. */
@RunWith(AndroidJUnit4::class)
class EarliestTimestampDaoTest {
    private lateinit var db: AppDatabase
    private val dao get() = db.transactionDao()

    @Before
    fun setup() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        for (id in listOf("acc1", "acc2")) {
            db.accountDao().upsertAccount(
                AccountEntity(id = id, name = id, currencyCode = "USD", structureType = "STANDARD",
                    colorHex = "#fff", icon = "bank", allowedCurrencies = "", profileId = 1)
            )
        }
    }

    @After
    fun tearDown() = db.close()

    private suspend fun add(
        accountId: String,
        ts: Long,
        category: Long = Category.SYSTEM_TRANSFER,
        profile: Int = 1,
        type: TransactionType = TransactionType.EXPENSE
    ) = dao.insertTransaction(
        TransactionEntity(name = "row", amount = BigDecimal("-1"), timestamp = ts, currency = "USD",
            transactionType = type, accountId = accountId, categoryId = category, profileId = profile)
    )

    private suspend fun earliest(accountId: String = "acc1", profile: Int = 1) =
        dao.getEarliestTimestampExcluding(accountId, listOf(Category.SYSTEM_INITIAL_BALANCE), profile)

    @Test
    fun returnsNull_whenTheAccountHasNoRows() = runBlocking<Unit> {
        assertNull(earliest())
    }

    @Test
    fun ignoresOpeningBalanceRows_whenFindingTheEarliest() = runBlocking<Unit> {
        add("acc1", ts = 100, category = Category.SYSTEM_INITIAL_BALANCE, type = TransactionType.INCOME)
        add("acc1", ts = 500, category = Category.SYSTEM_INITIAL_BALANCE, type = TransactionType.INCOME)
        add("acc1", ts = 900)
        add("acc1", ts = 700)

        assertEquals(700L, earliest())
    }

    @Test
    fun returnsNull_whenOnlyOpeningBalanceRowsExist() = runBlocking<Unit> {
        add("acc1", ts = 100, category = Category.SYSTEM_INITIAL_BALANCE, type = TransactionType.INCOME)

        assertNull(earliest())
    }

    @Test
    fun countsBalanceAdjustments_whichAreNotOpeningBalances() = runBlocking<Unit> {
        add("acc1", ts = 300, category = Category.SYSTEM_TRANSFER, type = TransactionType.INCOME)
        add("acc1", ts = 800)

        assertEquals(300L, earliest())
    }

    @Test
    fun onlyLooksAt_theRequestedAccount() = runBlocking<Unit> {
        add("acc2", ts = 50)
        add("acc1", ts = 400)

        assertEquals(400L, earliest("acc1"))
    }

    @Test
    fun isIsolated_byProfile() = runBlocking<Unit> {
        add("acc1", ts = 50, profile = 2)
        add("acc1", ts = 400, profile = 1)

        assertEquals(400L, earliest("acc1", profile = 1))
        assertEquals(50L, earliest("acc1", profile = 2))
    }
}
