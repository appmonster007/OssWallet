package com.osscabinet.ossWallet.domain.service

import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.service.aggregation.AggregationService
import com.osscabinet.ossWallet.domain.service.aggregation.breakdownByCategory
import com.osscabinet.ossWallet.domain.service.aggregation.breakdownByDate
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class AggregationServiceTest {

    private val exchangeRates = mapOf(
        "USD" to BigDecimal("1.0"),
        "EUR" to BigDecimal("0.85"),
        "GBP" to BigDecimal("0.75")
    )

    private fun createTransaction(
        id: Long = 0,
        amount: BigDecimal,
        currency: String,
        accountId: String = "acc1",
        categoryId: Long = 1001L
    ) = Transaction(
        id = id,
        name = "Test",
        amount = amount,
        timestamp = System.currentTimeMillis(),
        currency = currency,
        transactionType = if (amount.signum() >= 0) TransactionType.INCOME else TransactionType.EXPENSE,
        accountId = accountId,
        categoryId = categoryId,
        status = TransactionStatus.CLEARED
    )

    @Test
    fun `calculateAccountBalance handles multi-currency correctly`() {
        val transactions = listOf(
            createTransaction(amount = BigDecimal("100"), currency = "USD"),
            createTransaction(amount = BigDecimal("85"), currency = "EUR"), // Should be 100 USD
            createTransaction(amount = BigDecimal("-50"), currency = "USD")
        )

        // Total in USD = 100 + (85 / 0.85) - 50 = 100 + 100 - 50 = 150
        val balance = AggregationService.calculateAccountBalance(
            accountId = "acc1",
            allTransactions = transactions,
            accountCurrency = "USD",
            exchangeRates = exchangeRates
        )

        assertEquals(0, BigDecimal("150.00").compareTo(balance))
    }

    @Test
    fun `calculateCashFlowResult aggregates multiple currencies into base`() {
        val transactions = listOf(
            createTransaction(amount = BigDecimal("100"), currency = "USD"),
            createTransaction(amount = BigDecimal("170"), currency = "EUR"), // 200 USD
            createTransaction(amount = BigDecimal("-50"), currency = "USD"),
            createTransaction(amount = BigDecimal("-85"), currency = "EUR")  // -100 USD
        )

        val result = AggregationService.calculateCashFlowResult(
            transactions = transactions,
            baseCurrency = "USD",
            exchangeRates = exchangeRates,
            precision = 2
        )

        // Income: 100 (USD) + 200 (from EUR) = 300
        // Expense: 50 (USD) + 100 (from EUR) = 150
        // Net: 300 - 150 = 150
        assertEquals("Income should be 300", 0, BigDecimal("300.00").compareTo(result.totalIncomeInBase))
        assertEquals("Expense should be 150 (abs)", 0, BigDecimal("150.00").abs().compareTo(result.totalExpensesInBase.abs()))
        assertEquals("Net should be 150", 0, BigDecimal("150.00").compareTo(result.netInBase))
        assertEquals(2, result.breakdown.size)
    }

    @Test
    fun `calculateCurrencyBalances returns correct totals per currency`() {
        val transactions = listOf(
            createTransaction(amount = BigDecimal("100"), currency = "USD"),
            createTransaction(amount = BigDecimal("50"), currency = "USD"),
            createTransaction(amount = BigDecimal("85"), currency = "EUR")
        )

        val balances = AggregationService.calculateCurrencyBalances(
            transactions = transactions,
            baseCurrency = "USD",
            exchangeRates = exchangeRates
        )

        val usdBalance = balances.find { it.currency == "USD" }
        val eurBalance = balances.find { it.currency == "EUR" }

        assertEquals(0, BigDecimal("150").compareTo(usdBalance?.amount))
        assertEquals(0, BigDecimal("150").compareTo(usdBalance?.amountInBase))
        assertEquals(0, BigDecimal("85").compareTo(eurBalance?.amount))
        assertEquals(0, BigDecimal("100").compareTo(eurBalance?.amountInBase))
    }

    @Test
    fun `breakdownByCategory correctly aggregates multi-currency data`() {
        val transactions = listOf(
            createTransaction(amount = BigDecimal("100"), currency = "USD", categoryId = 1L),
            createTransaction(amount = BigDecimal("85"), currency = "EUR", categoryId = 1L), // 100 USD
            createTransaction(amount = BigDecimal("50"), currency = "USD", categoryId = 2L)
        )

        val breakdown = transactions.breakdownByCategory(
            exchangeRates = exchangeRates,
            baseCurrency = "USD"
        )

        val food = breakdown.find { it.key == "1" }
        val transport = breakdown.find { it.key == "2" }

        assertEquals(0, BigDecimal("200").compareTo(food?.totalAmount))
        assertEquals(0, BigDecimal("50").compareTo(transport?.totalAmount))
        assertEquals(2, food?.currencyBreakdown?.size)
    }

    @Test
    fun `breakdownByDate correctly aggregates multi-currency data`() {
        // We use a fixed date for testing
        val date = "2023-10-27"
        val timestamp = java.time.LocalDate.parse(date).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

        val transactions = listOf(
            createTransaction(amount = BigDecimal("100"), currency = "USD").copy(timestamp = timestamp),
            createTransaction(amount = BigDecimal("85"), currency = "EUR").copy(timestamp = timestamp) // 100 USD
        )

        val breakdown = transactions.breakdownByDate(
            exchangeRates = exchangeRates,
            baseCurrency = "USD"
        )

        val dayEntry = breakdown.find { it.key == date }
        assertEquals(0, BigDecimal("200").compareTo(dayEntry?.totalAmount))
    }

    @Test
    fun `buildCategoryTree aggregates correctly with hierarchy and currencies`() {
        val categories = listOf(
            com.osscabinet.ossWallet.domain.model.Category(id = 1, name = "Food", iconName = "food", colorHex = 0xFFFF0000.toInt(), isDefault = false, parentId = null),
            com.osscabinet.ossWallet.domain.model.Category(id = 2, name = "Groceries", iconName = "shopping", colorHex = 0xFF00FF00.toInt(), isDefault = false, parentId = 1)
        )

        val transactions = listOf(
            createTransaction(amount = BigDecimal("-50"), currency = "USD", categoryId = 1),
            createTransaction(amount = BigDecimal("-85"), currency = "EUR", categoryId = 2) // -100 USD
        )

        val tree = AggregationService.buildCategoryTree(
            transactions = transactions,
            allCategories = categories,
            exchangeRates = exchangeRates,
            baseCurrency = "USD"
        )

        // Root "Food" should have total of 50 + 100 = 150
        val foodRoot = tree.find { it.category == "Food" }
        assertEquals(0, BigDecimal("150").compareTo(foodRoot?.expense?.abs()))
        assertEquals(2, foodRoot?.subcategoryData?.size)
        // First subcategory is "Groceries" (100 in base)
        assertEquals("Groceries", foodRoot?.subcategoryData?.get(0)?.category)
        assertEquals(0, BigDecimal("100").compareTo(foodRoot?.subcategoryData?.get(0)?.expense?.abs()))
        // Second subcategory is pseudo-child for direct "Food" transactions (50 in base)
        assertEquals("Food", foodRoot?.subcategoryData?.get(1)?.category)
        assertEquals(0, BigDecimal("50").compareTo(foodRoot?.subcategoryData?.get(1)?.expense?.abs()))
    }
}
