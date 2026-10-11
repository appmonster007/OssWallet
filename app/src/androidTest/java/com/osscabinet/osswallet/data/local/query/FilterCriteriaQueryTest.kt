package com.osscabinet.ossWallet.data.local.query

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.osscabinet.ossWallet.data.local.AppDatabase
import com.osscabinet.ossWallet.data.local.entity.AccountEntity
import com.osscabinet.ossWallet.data.local.entity.CategoryEntity
import com.osscabinet.ossWallet.data.local.entity.TransactionEntity
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.query.AggregationDimension
import com.osscabinet.ossWallet.domain.model.query.StandardMetrics
import com.osscabinet.ossWallet.domain.service.query.AggregationPlanner
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

/**
 * Every filter criterion must reach SQLite and select the same rows as the in-memory filter.
 * Before the fix, amount and payment-method criteria threw while binding (and card totals silently
 * became zero), tags always matched with OR, regex criteria matched nothing and linked ids were ignored.
 */
@RunWith(AndroidJUnit4::class)
class FilterCriteriaQueryTest {

    private lateinit var db: AppDatabase
    private lateinit var service: DatabaseQueryServiceImpl
    private val profileId = 1

    @Before
    fun setup() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        service = DatabaseQueryServiceImpl(
            transactionDao = db.transactionDao(),
            queryBuilder = TransactionQueryBuilder(),
            planner = AggregationPlanner(),
            cache = QueryCache(),
            json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true },
            invalidationTracker = db.invalidationTracker
        )
        db.accountDao().upsertAccount(
            AccountEntity(
                id = "acc1", name = "Checking", currencyCode = "USD",
                structureType = "STANDARD", colorHex = "#fff", icon = "bank",
                allowedCurrencies = "", profileId = profileId
            )
        )
        db.categoryDao().insertCategory(
            CategoryEntity(id = 1L, name = "Food", iconName = "food", colorHex = 0, isDefault = false)
        )
        insert("Coffee", "-5.00", payee = "Starbucks", note = "latte", tags = setOf("Food", "Social"), pm = PaymentMethod.CASH)
        insert("Lunch", "-25.00", payee = "Subway", note = "50% off", tags = setOf("Food"), pm = PaymentMethod.CREDIT_CARD)
        insert("Rent", "-1000.00", payee = "Landlord", note = "oct", tags = setOf("Housing"), pm = PaymentMethod.BANK_TRANSFER)
        insert("Car", "-450.00", payee = "Chase Auto", note = "", tags = setOf("CarLoan"), pm = PaymentMethod.BANK_TRANSFER)
        insert("Salary", "5000.00", payee = "Acme", note = "", tags = setOf("Work"), pm = PaymentMethod.BANK_TRANSFER, type = TransactionType.INCOME)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun names(filter: TransactionFilter): Set<String> =
        service.observeFilteredTransactions(profileId, filter).first().map { it.name }.toSet()

    private suspend fun expenseTotal(filter: TransactionFilter): BigDecimal =
        service.aggregate(
            profileId = profileId,
            dimensions = listOf(AggregationDimension.CURRENCY),
            metrics = listOf(StandardMetrics.EXPENSE_SUM),
            filter = filter
        ).getOrThrow().firstOrNull()?.metrics?.get("expense") ?: BigDecimal.ZERO

    @Test
    fun amountRange_usesAbsoluteValue() = runBlocking {
        val f = TransactionFilter(minAmount = BigDecimal("20"), maxAmount = BigDecimal("500"))
        assertEquals(setOf("Lunch", "Car"), names(f))
        assertEquals(0, BigDecimal("-475.00").compareTo(expenseTotal(f)))
    }

    @Test
    fun paymentMethods_filterRows() = runBlocking {
        val f = TransactionFilter(paymentMethods = setOf(PaymentMethod.CASH, PaymentMethod.CREDIT_CARD))
        assertEquals(setOf("Coffee", "Lunch"), names(f))
        assertEquals(0, BigDecimal("-30.00").compareTo(expenseTotal(f)))
    }

    @Test
    fun tags_matchAll_requiresEveryTag_andMatchesExactNames() = runBlocking {
        assertEquals(setOf("Coffee"), names(TransactionFilter(tags = setOf("Food", "Social"), matchAllTags = true)))
        assertEquals(setOf("Coffee", "Lunch"), names(TransactionFilter(tags = setOf("Food", "Social"), matchAllTags = false)))
        // "Car" is a substring of "CarLoan" but not a tag
        assertEquals(emptySet<String>(), names(TransactionFilter(tags = setOf("Car"))))
        assertEquals(setOf("Car"), names(TransactionFilter(tags = setOf("CarLoan"))))
    }

    @Test
    fun textCriteria_areLiteralNotWildcards() = runBlocking {
        assertEquals(setOf("Lunch"), names(TransactionFilter(queryNote = "50%")))
        assertEquals(emptySet<String>(), names(TransactionFilter(queryNote = "5_ off")))
        assertEquals(setOf("Coffee"), names(TransactionFilter(queryPayee = "starbucks")))
    }

    @Test
    fun regexCriteria_areApplied() = runBlocking {
        val f = TransactionFilter(queryPayee = "^(Star|Sub)", isPayeeRegex = true)
        assertEquals(setOf("Coffee", "Lunch"), names(f))
        assertEquals(0, BigDecimal("-30.00").compareTo(expenseTotal(f)))
        assertEquals(setOf("Coffee"), names(TransactionFilter(queryPayee = "^(Star|Sub)", isPayeeRegex = true, queryNote = "lat+e", isNoteRegex = true)))
    }

    @Test
    fun regexCriteria_limitAppliesAfterMatching() = runBlocking {
        val rows = service.observeFilteredTransactions(
            profileId, TransactionFilter(queryPayee = "^(Star|Sub|Land)", isPayeeRegex = true), limit = 2
        ).first()
        assertEquals(2, rows.size)
    }

    @Test
    fun linkedTransactionIds_restrictRows() = runBlocking {
        val coffeeId = db.transactionDao().getAllTransactionsSync(profileId).first { it.name == "Coffee" }.id
        val f = TransactionFilter(linkedTransactionIds = setOf(coffeeId))
        assertEquals(setOf("Coffee"), names(f))
        assertEquals(0, BigDecimal("-5.00").compareTo(expenseTotal(f)))
    }

    private suspend fun insert(
        name: String, amount: String, payee: String, note: String,
        tags: Set<String>, pm: PaymentMethod, type: TransactionType = TransactionType.EXPENSE
    ) {
        db.transactionDao()._insertTransaction(
            TransactionEntity(
                name = name, amount = BigDecimal(amount), timestamp = System.currentTimeMillis() - 1000,
                currency = "USD", transactionType = type, accountId = "acc1", categoryId = 1L,
                profileId = profileId, payee = payee, note = note, tags = tags, paymentMethod = pm
            )
        )
    }
}
