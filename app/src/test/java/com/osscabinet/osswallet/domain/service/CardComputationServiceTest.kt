package com.osscabinet.ossWallet.domain.service

import com.osscabinet.ossWallet.data.local.dao.CategoryDao
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.analytics.*
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.model.settings.ScreenContext
import com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class CardComputationServiceTest {

    private lateinit var cardComputationService: CardComputationService
    private lateinit var categoryDao: CategoryDao

    @Before
    fun setup() {
        categoryDao = object : CategoryDao {
            override fun getAllCategories() = throw NotImplementedError()
            override suspend fun insertCategory(category: com.osscabinet.ossWallet.data.local.entity.CategoryEntity) {}
            override suspend fun deleteCategory(category: com.osscabinet.ossWallet.data.local.entity.CategoryEntity) {}
            override suspend fun getAllCategoriesSync() = emptyList<com.osscabinet.ossWallet.data.local.entity.CategoryEntity>()
            override suspend fun clearAll() {}
            override suspend fun getCategoryByName(name: String) = null
            override suspend fun getSubCategories(parentId: Long) = emptyList<com.osscabinet.ossWallet.data.local.entity.CategoryEntity>()
        }
        cardComputationService = CardComputationServiceImpl(categoryDao)
    }

    @Test
    fun `computeCashFlow returns correct result`() = runBlocking {
        val now = Instant.now().toEpochMilli()
        val timelineRange = TimelineRange.AllTime
        val transactions = listOf(
            Transaction(id = 1, title = "Income", amount = BigDecimal("100"), category = "General", timestamp = now, currency = "USD", transactionType = "income", accountId = "1", status = TransactionStatus.CLEARED, paymentMethod = "cash", payee = "Boss", note = ""),
            Transaction(id = 2, title = "Expense", amount = BigDecimal("-50"), category = "General", timestamp = now, currency = "USD", transactionType = "expense", accountId = "1", status = TransactionStatus.CLEARED, paymentMethod = "cash", payee = "Store", note = "")
        )
        val config = CashFlowCardConfig(cardId = "test", title = "Cash Flow", isEnabled = true, order = 0, screenContext = ScreenContext.DASHBOARD, showBaseConversion = true, timelineRange = timelineRange, filter = TransactionFilter(timelineRange))
        val context = ComputationContext("USD", emptyMap(), 2)

        val result = cardComputationService.computeCashFlow(config, transactions, context)

        assertEquals(0, BigDecimal("100.00").compareTo(result.totalIncomeInBase))
        assertEquals(0, BigDecimal("-50.00").compareTo(result.totalExpensesInBase))
        assertEquals(0, BigDecimal("50.00").compareTo(result.netInBase))
    }

    @Test
    fun `computeCurrencyTotals returns correct aggregation`() = runBlocking {
        val now = Instant.now().toEpochMilli()
        val accounts = listOf(
            Account(id = "1", name = "Bank", balance = BigDecimal("1000"), currencyCode = "USD", structureType = "Regular", colorHex = "#000000", icon = "bank", allowedCurrencies = listOf("USD"), isArchived = false, displayOrder = 0),
            Account(id = "2", name = "Cash", balance = BigDecimal("500"), currencyCode = "EUR", structureType = "Regular", colorHex = "#000000", icon = "cash", allowedCurrencies = listOf("EUR"), isArchived = false, displayOrder = 1)
        )
        val transactions = listOf(
            Transaction(id = 1, title = "Initial USD", amount = BigDecimal("1000"), category = "System", timestamp = now, currency = "USD", transactionType = "income", accountId = "1", status = TransactionStatus.CLEARED, paymentMethod = "system", payee = "System", note = ""),
            Transaction(id = 2, title = "Initial EUR", amount = BigDecimal("500"), category = "System", timestamp = now, currency = "EUR", transactionType = "income", accountId = "2", status = TransactionStatus.CLEARED, paymentMethod = "system", payee = "System", note = "")
        )
        val timelineRange = TimelineRange.AllTime
        val config = CurrencyBalancesCardConfig(cardId = "test", title = "Balances", isEnabled = true, order = 0, screenContext = ScreenContext.DASHBOARD, layout = "Grid", showBaseConversion = true, timelineRange = timelineRange, filter = TransactionFilter(timelineRange))
        val context = ComputationContext("USD", mapOf("EUR" to BigDecimal("1.1")), 2)

        val result = cardComputationService.computeCurrencyTotals(config, accounts, transactions, context)

        assertEquals(2, result.size)
        val usdTotal = result.find { it.currency == "USD" }
        val eurTotal = result.find { it.currency == "EUR" }
        
        assertNotNull(usdTotal)
        assertEquals(0, BigDecimal("1000.00").compareTo(usdTotal?.amount))
        
        assertNotNull(eurTotal)
        assertEquals(0, BigDecimal("500.00").compareTo(eurTotal?.amount))
    }
}
