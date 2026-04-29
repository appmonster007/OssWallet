package com.osscabinet.ossWallet.domain.service

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class TransactionsUtilityServiceTest {

    private lateinit var testTransactions: List<Transaction>
    private lateinit var testAccounts: List<Account>

    @Before
    fun setup() {
        testTransactions = listOf(
            Transaction(
                id = 1L,
                title = "Salary",
                amount = BigDecimal("5000"),
                category = "Income",
                categoryId = 1L,
                timestamp = Instant.parse("2024-01-15T10:00:00Z").toEpochMilli(),
                currency = "USD",
                transactionType = "income",
                accountId = "acc1",
                status = TransactionStatus.CLEARED,
                paymentMethod = "bank_transfer"
            ),
            Transaction(
                id = 2L,
                title = "Groceries",
                amount = BigDecimal("-150"),
                category = "Food",
                categoryId = 2L,
                timestamp = Instant.parse("2024-01-16T14:00:00Z").toEpochMilli(),
                currency = "USD",
                transactionType = "expense",
                accountId = "acc1",
                status = TransactionStatus.CLEARED,
                paymentMethod = "credit_card",
                tags = listOf("essentials", "monthly")
            ),
            Transaction(
                id = 3L,
                title = "Rent",
                amount = BigDecimal("-2000"),
                category = "Housing",
                categoryId = 3L,
                timestamp = Instant.parse("2024-01-01T00:00:00Z").toEpochMilli(),
                currency = "USD",
                transactionType = "expense",
                accountId = "acc1",
                status = TransactionStatus.CLEARED,
                paymentMethod = "bank_transfer",
                recurring = "monthly"
            ),
            Transaction(
                id = 4L,
                title = "Freelance EUR",
                amount = BigDecimal("800"),
                category = "Income",
                categoryId = 1L,
                timestamp = Instant.parse("2024-01-20T16:00:00Z").toEpochMilli(),
                currency = "EUR",
                transactionType = "income",
                accountId = "acc2",
                status = TransactionStatus.RECONCILE,
                paymentMethod = "paypal"
            ),
            Transaction(
                id = 5L,
                title = "Entertainment",
                amount = BigDecimal("-50"),
                category = "Entertainment",
                categoryId = 4L,
                timestamp = Instant.parse("2024-01-25T20:00:00Z").toEpochMilli(),
                currency = "USD",
                transactionType = "expense",
                accountId = "acc2",
                status = TransactionStatus.UNCLEARED,
                paymentMethod = "cash",
                tags = listOf("leisure", "one-time")
            ),
            Transaction(
                id = 6L,
                title = "Travel Expenses",
                amount = BigDecimal("-300"),
                category = "Travel",
                categoryId = 5L,
                timestamp = Instant.parse("2024-01-10T08:00:00Z").toEpochMilli(),
                currency = "GBP",
                transactionType = "expense",
                accountId = "acc1",
                status = TransactionStatus.CLEARED,
                paymentMethod = "debit_card",
                tags = listOf("business", "travel")
            ),
            Transaction(
                id = 7L,
                title = "Investment Return",
                amount = BigDecimal("200"),
                category = "Investment",
                categoryId = 6L,
                timestamp = Instant.parse("2024-01-05T12:00:00Z").toEpochMilli(),
                currency = "USD",
                transactionType = "income",
                accountId = "acc2",
                status = TransactionStatus.CLEARED,
                paymentMethod = "bank_transfer"
            ),
            Transaction(
                id = 8L,
                title = "Restaurant",
                amount = BigDecimal("-75"),
                category = "Food",
                categoryId = 2L,
                timestamp = Instant.parse("2024-01-18T19:00:00Z").toEpochMilli(),
                currency = "EUR",
                transactionType = "expense",
                accountId = "acc1",
                status = TransactionStatus.UNCLEARED,
                paymentMethod = "credit_card",
                tags = listOf("dining")
            ),
            Transaction(
                id = 9L,
                title = "Subscription",
                amount = BigDecimal("-15"),
                category = "Entertainment",
                categoryId = 4L,
                timestamp = Instant.parse("2024-01-28T00:00:00Z").toEpochMilli(),
                currency = "USD",
                transactionType = "expense",
                accountId = "acc1",
                status = TransactionStatus.RECONCILE,
                paymentMethod = "credit_card",
                recurring = "monthly"
            ),
            Transaction(
                id = 10L,
                title = "Gift",
                amount = BigDecimal("100"),
                category = "Income",
                categoryId = 1L,
                timestamp = Instant.parse("2024-01-30T15:00:00Z").toEpochMilli(),
                currency = "GBP",
                transactionType = "income",
                accountId = "acc2",
                status = TransactionStatus.CLEARED,
                paymentMethod = "bank_transfer",
                tags = listOf("gift")
            )
        )

        testAccounts = listOf(
            Account(
                id = "acc1",
                name = "Checking",
                balance = BigDecimal("2850"),
                currencyCode = "USD",
                structureType = "bank",
                colorHex = "#000000",
                icon = "bank"
            ),
            Account(
                id = "acc2",
                name = "Savings",
                balance = BigDecimal("950"),
                currencyCode = "USD",
                structureType = "bank",
                colorHex = "#000000",
                icon = "bank"
            )
        )
    }

    // ==================== Grouping Operations ====================

    @Test
    fun `given transactions when grouped by currency then returns correct groups`() {
        val result = TransactionsUtilityService.groupByCurrency(testTransactions)
        
        assertEquals(3, result.size)
        assertTrue(result.containsKey("USD"))
        assertTrue(result.containsKey("EUR"))
        assertTrue(result.containsKey("GBP"))
        assertEquals(6, result["USD"]?.size)
        assertEquals(2, result["EUR"]?.size)
        assertEquals(2, result["GBP"]?.size)
    }

    @Test
    fun `given transactions when grouped by category then returns correct groups`() {
        val result = TransactionsUtilityService.groupByCategory(testTransactions)
        
        assertEquals(6, result.size)
        assertEquals(3, result["Income"]?.size)
        assertEquals(2, result["Food"]?.size)
        assertEquals(1, result["Housing"]?.size)
        assertEquals(2, result["Entertainment"]?.size)
        assertEquals(1, result["Travel"]?.size)
        assertEquals(1, result["Investment"]?.size)
    }

    @Test
    fun `given transactions when grouped by account then returns correct groups`() {
        val result = TransactionsUtilityService.groupByAccount(testTransactions)
        
        assertEquals(2, result.size)
        assertEquals(6, result["acc1"]?.size)
        assertEquals(4, result["acc2"]?.size)
    }

    @Test
    fun `given transactions when grouped by transaction type then returns correct groups`() {
        val result = TransactionsUtilityService.groupByTransactionType(testTransactions)
        
        assertEquals(2, result.size)
        assertEquals(4, result["income"]?.size)
        assertEquals(6, result["expense"]?.size)
    }

    @Test
    fun `given transactions when grouped by payment method then returns correct groups`() {
        val result = TransactionsUtilityService.groupByPaymentMethod(testTransactions)
        
        assertEquals(5, result.size)
        assertEquals(4, result["bank_transfer"]?.size)
        assertEquals(3, result["credit_card"]?.size)
    }

    @Test
    fun `given transactions when grouped by status then returns correct groups`() {
        val result = TransactionsUtilityService.groupByStatus(testTransactions)
        
        assertEquals(3, result.size)
        assertEquals(6, result["CLEARED"]?.size)
        assertEquals(2, result["UNCLEARED"]?.size)
        assertEquals(2, result["RECONCILE"]?.size)
    }

    @Test
    fun `given transactions when grouped by date then returns correct groups`() {
        val result = TransactionsUtilityService.groupByDate(testTransactions)
        
        assertEquals(10, result.size)
        assertEquals(10, result.keys.size)
    }

    @Test
    fun `given transactions when grouped by month then returns correct groups`() {
        val result = TransactionsUtilityService.groupByMonth(testTransactions)
        
        assertEquals(1, result.size)
        assertTrue(result.containsKey("2024-01"))
        assertEquals(10, result["2024-01"]?.size)
    }

    @Test
    fun `given transactions with tags when grouped by tags then returns correct groups`() {
        val result = TransactionsUtilityService.groupByTags(testTransactions)
        
        assertEquals(8, result.size)
        assertTrue(result.containsKey("essentials"))
        assertTrue(result.containsKey("monthly"))
        assertTrue(result.containsKey("leisure"))
        assertTrue(result.containsKey("one-time"))
        assertTrue(result.containsKey("business"))
        assertTrue(result.containsKey("travel"))
        assertTrue(result.containsKey("dining"))
        assertTrue(result.containsKey("gift"))
    }

    // ==================== Calculation Methods ====================

    @Test
    fun `given single currency transactions when calculating transaction summary then returns correct summary`() {
        val usdTransactions = testTransactions.filter { it.currency == "USD" }
        val result = TransactionsUtilityService.calculateTransactionSummary(usdTransactions)
        
        assertEquals(BigDecimal("2985"), result.totalAmount)
        assertEquals(BigDecimal("5200"), result.income)
        assertEquals(BigDecimal("-2215"), result.expense)
        assertEquals(6, result.transactionCount)
    }

    @Test
    fun `given multi-currency transactions when calculating transaction summary then throws exception`() {
        try {
            TransactionsUtilityService.calculateTransactionSummary(testTransactions)
            fail("Should throw IllegalArgumentException for multi-currency transactions")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("same currency"))
        }
    }

    @Test
    fun `given transactions when calculating summary by currency then returns correct breakdown`() {
        val result = TransactionsUtilityService.calculateSummaryByCurrency(testTransactions)
        
        assertEquals(3, result.size)
        assertEquals(BigDecimal("2985"), result["USD"]?.totalAmount)
        assertEquals(BigDecimal("725"), result["EUR"]?.totalAmount)
        assertEquals(BigDecimal("-200"), result["GBP"]?.totalAmount)
    }

    @Test
    fun `given transactions when calculating total by currency then returns correct totals`() {
        val result = TransactionsUtilityService.calculateTotalByCurrency(testTransactions)
        
        assertEquals(3, result.size)
        assertEquals(BigDecimal("2985"), result["USD"])
        assertEquals(BigDecimal("725"), result["EUR"])
        assertEquals(BigDecimal("-200"), result["GBP"])
    }

    @Test
    fun `given transactions when separating income and expense then partitions correctly`() {
        val (income, expenses) = TransactionsUtilityService.separateIncomeExpense(testTransactions)
        
        assertEquals(4, income.size)
        assertEquals(6, expenses.size)
        assertTrue(income.all { it.amount.signum() >= 0 })
        assertTrue(expenses.all { it.amount.signum() < 0 })
    }

    @Test
    fun `given transactions when calculating income expense breakdown then returns correct breakdown`() {
        val result = TransactionsUtilityService.calculateIncomeExpenseBreakdown(testTransactions)
        
        assertEquals(4, result.incomeTransactions.size)
        assertEquals(6, result.expenseTransactions.size)
        assertTrue(result.incomeCurrencyBreakdown.containsKey("USD"))
        assertTrue(result.expenseCurrencyBreakdown.containsKey("USD"))
    }

    @Test
    fun `given multi-currency transactions with exchange rates when calculating income expense breakdown then converts correctly`() {
        val exchangeRates = mapOf("EUR" to BigDecimal("1.1"), "GBP" to BigDecimal("1.3"))
        val result = TransactionsUtilityService.calculateIncomeExpenseBreakdown(
            testTransactions,
            exchangeRates,
            "USD"
        )
        
        assertEquals(4, result.incomeTransactions.size)
        assertEquals(6, result.expenseTransactions.size)
    }

    @Test
    fun `given exchange rates without base currency when calculating income expense breakdown then throws exception`() {
        val exchangeRates = mapOf("EUR" to BigDecimal("1.1"), "GBP" to BigDecimal("1.3"))
        try {
            TransactionsUtilityService.calculateIncomeExpenseBreakdown(
                testTransactions,
                exchangeRates,
                null
            )
            fail("Should throw IllegalArgumentException when baseCurrency is null with exchangeRates")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("baseCurrency"))
        }
    }

    // ==================== Breakdown Methods ====================

    @Test
    fun `given transactions when broken down by currency then returns correct breakdown`() {
        val result = TransactionsUtilityService.breakdownByCurrency(testTransactions)
        
        assertEquals(3, result.size)
        assertEquals(6, result.find { it.currency == "USD" }?.transactionCount)
        assertEquals(2, result.find { it.currency == "EUR" }?.transactionCount)
        assertEquals(2, result.find { it.currency == "GBP" }?.transactionCount)
    }

    @Test
    fun `given transactions when broken down by account then returns correct breakdown`() {
        val result = TransactionsUtilityService.breakdownByAccount(testTransactions, testAccounts)
        
        assertEquals(2, result.size)
        val acc1Breakdown = result.find { it.key == "acc1" }
        val acc2Breakdown = result.find { it.key == "acc2" }
        
        assertNotNull(acc1Breakdown)
        assertNotNull(acc2Breakdown)

        // Key is now just the accountId String
        assertEquals("acc1", acc1Breakdown?.key)
        assertEquals("acc2", acc2Breakdown?.key)

        assertEquals(6, acc1Breakdown?.transactionCount)
        assertEquals(4, acc2Breakdown?.transactionCount)
    }

    @Test
    fun `given transactions when broken down by category then returns correct breakdown`() {
        val result = TransactionsUtilityService.breakdownByCategory(testTransactions)
        
        assertEquals(6, result.size)
        val incomeBreakdown = result.find { it.key == "Income" }
        
        assertNotNull(incomeBreakdown)
        assertEquals(3, incomeBreakdown?.transactionCount)
    }

    @Test
    fun `given transactions when broken down by date then returns correct breakdown`() {
        val result = TransactionsUtilityService.breakdownByDate(testTransactions)
        
        assertEquals(10, result.size)
    }

    @Test
    fun `given transactions when broken down by month then returns correct breakdown`() {
        val result = TransactionsUtilityService.breakdownByMonth(testTransactions)
        
        assertEquals(1, result.size)
        val janBreakdown = result[0]
        
        assertEquals("2024-01", janBreakdown.key)
        assertEquals(10, janBreakdown.transactionCount)
    }

    @Test
    fun `given transactions when broken down by transaction type then returns correct breakdown`() {
        val result = TransactionsUtilityService.breakdownByTransactionType(testTransactions)
        
        assertEquals(2, result.size)
        val incomeBreakdown = result.find { it.key == "income" }
        val expenseBreakdown = result.find { it.key == "expense" }
        
        assertNotNull(incomeBreakdown)
        assertNotNull(expenseBreakdown)
        
        assertEquals(4, incomeBreakdown?.transactionCount)
        assertEquals(6, expenseBreakdown?.transactionCount)
    }

    @Test
    fun `given transactions when broken down by payment method then returns correct breakdown`() {
        val result = TransactionsUtilityService.breakdownByPaymentMethod(testTransactions)
        
        assertEquals(5, result.size)
        assertEquals(10, result.sumOf { it.transactionCount })
    }

    @Test
    fun `given transactions when broken down by status then returns correct breakdown`() {
        val result = TransactionsUtilityService.breakdownByStatus(testTransactions)
        
        assertEquals(3, result.size)
        val clearedBreakdown = result.find { it.key == "CLEARED" }
        val unclearedBreakdown = result.find { it.key == "UNCLEARED" }
        val reconcileBreakdown = result.find { it.key == "RECONCILE" }
        
        assertNotNull(clearedBreakdown)
        assertNotNull(unclearedBreakdown)
        assertNotNull(reconcileBreakdown)
        
        assertEquals(6, clearedBreakdown?.transactionCount)
        assertEquals(2, unclearedBreakdown?.transactionCount)
        assertEquals(2, reconcileBreakdown?.transactionCount)
    }

    @Test
    fun `given transactions when broken down by tags then returns correct breakdown`() {
        val result = TransactionsUtilityService.breakdownByTags(testTransactions)
        
        assertEquals(8, result.size)
        assertEquals(8, result.sumOf { it.transactionCount })
    }

    @Test
    fun `given transactions when broken down by label then returns correct breakdown`() {
        val result = TransactionsUtilityService.breakdownByLabel(testTransactions, { tx -> tx.category })
        
        assertEquals(6, result.size)
        assertEquals(10, result.sumOf { it.transactionCount })
    }

    // ==================== Combination Breakdown Methods ====================

    @Test
    fun `given transactions when broken down by account for currency then returns correct breakdown`() {
        val result = TransactionsUtilityService.breakdownByAccountForCurrency(testTransactions, testAccounts, "USD")
        
        assertEquals(2, result.size)
        assertEquals(6, result.sumOf { it.transactionCount })
    }

    @Test
    fun `given transactions when broken down by currency for account then returns correct breakdown`() {
        val result = TransactionsUtilityService.breakdownByCurrencyForAccount(testTransactions, testAccounts, "acc1")
        
        assertEquals(3, result.size)
        assertEquals(6, result.sumOf { it.transactionCount })
    }

    @Test
    fun `given transactions when broken down by account and currency then returns correct nested breakdown`() {
        val result = TransactionsUtilityService.breakdownByAccountAndCurrency(testTransactions, testAccounts)
        
        assertEquals(2, result.size)
        assertTrue(result.containsKey("acc1"))
        assertTrue(result.containsKey("acc2"))
    }

    @Test
    fun `given transactions when broken down by currency and account then returns correct nested breakdown`() {
        val result = TransactionsUtilityService.breakdownByCurrencyAndAccount(testTransactions, testAccounts)
        
        assertEquals(3, result.size)
        assertTrue(result.containsKey("USD"))
        assertTrue(result.containsKey("EUR"))
        assertTrue(result.containsKey("GBP"))
    }

    @Test
    fun `given transactions when broken down by category for account then returns correct breakdown`() {
        val result = TransactionsUtilityService.breakdownByCategoryForAccount(testTransactions, "acc1")
        
        assertEquals(5, result.size)
    }

    @Test
    fun `given transactions when broken down by account and category then returns correct nested breakdown`() {
        val result = TransactionsUtilityService.breakdownByAccountAndCategory(testTransactions, testAccounts)
        
        assertEquals(2, result.size)
        assertTrue(result.containsKey("acc1"))
        assertTrue(result.containsKey("acc2"))
    }

    @Test
    fun `given transactions when broken down by category and account then returns correct nested breakdown`() {
        val result = TransactionsUtilityService.breakdownByCategoryAndAccount(testTransactions, testAccounts)
        
        assertEquals(6, result.size)
    }

    @Test
    fun `given transactions when broken down by date for account then returns correct breakdown`() {
        val result = TransactionsUtilityService.breakdownByDateForAccount(testTransactions, "acc1")
        
        assertEquals(6, result.size)
    }

    @Test
    fun `given transactions when broken down by month for account then returns correct breakdown`() {
        val result = TransactionsUtilityService.breakdownByMonthForAccount(testTransactions, "acc1")
        
        assertEquals(1, result.size)
    }

    // ==================== Filtering Operations ====================

    @Test
    fun `given transactions and filter when filtering then returns filtered transactions`() {
        val filter = TransactionFilter(categoryIds = listOf(2L))
        val result = TransactionsUtilityService.filter(testTransactions, filter)
        
        assertEquals(2, result.size)
        assertEquals(2, result.count { it.categoryId == 2L })
    }

    @Test
    fun `given transactions when filtering income then returns only income transactions`() {
        val result = TransactionsUtilityService.filterIncome(testTransactions)
        
        assertEquals(4, result.size)
        assertTrue(result.all { it.transactionType == "income" })
    }

    @Test
    fun `given transactions when filtering expense then returns only expense transactions`() {
        val result = TransactionsUtilityService.filterExpense(testTransactions)
        
        assertEquals(6, result.size)
        assertTrue(result.all { it.transactionType == "expense" })
    }

    @Test
    fun `given transactions when filtering by currency then returns only that currency`() {
        val result = TransactionsUtilityService.filterByCurrency(testTransactions, "USD")
        
        assertEquals(6, result.size)
        assertTrue(result.all { it.currency == "USD" })
    }

    @Test
    fun `given transactions when filtering by currencies then returns only those currencies`() {
        val result = TransactionsUtilityService.filterByCurrencies(testTransactions, setOf("USD", "EUR"))
        
        assertEquals(8, result.size)
        assertTrue(result.all { it.currency in setOf("USD", "EUR") })
    }

    @Test
    fun `given transactions when filtering by category then returns only that category`() {
        val result = TransactionsUtilityService.filterByCategory(testTransactions, "Food")
        
        assertEquals(2, result.size)
        assertTrue(result.all { it.category == "Food" })
    }

    @Test
    fun `given transactions when filtering by categories then returns only those categories`() {
        val result = TransactionsUtilityService.filterByCategories(testTransactions, setOf("Food", "Housing"))
        
        assertEquals(3, result.size)
        assertTrue(result.all { it.category in setOf("Food", "Housing") })
    }

    @Test
    fun `given transactions when filtering by account then returns only that account`() {
        val result = TransactionsUtilityService.filterByAccount(testTransactions, "acc1")
        
        assertEquals(6, result.size)
        assertTrue(result.all { it.accountId == "acc1" })
    }

    @Test
    fun `given transactions when filtering by accounts then returns only those accounts`() {
        val result = TransactionsUtilityService.filterByAccounts(testTransactions, setOf("acc1"))
        
        assertEquals(6, result.size)
        assertTrue(result.all { it.accountId in setOf("acc1") })
    }

    @Test
    fun `given transactions when filtering by status then returns only that status`() {
        val result = TransactionsUtilityService.filterByStatus(testTransactions, TransactionStatus.CLEARED)
        
        assertEquals(6, result.size)
        assertTrue(result.all { it.status == TransactionStatus.CLEARED })
    }

    @Test
    fun `given transactions when filtering by statuses then returns only those statuses`() {
        val result = TransactionsUtilityService.filterByStatuses(testTransactions, setOf(TransactionStatus.CLEARED, TransactionStatus.RECONCILE))
        
        assertEquals(8, result.size)
        assertTrue(result.all { it.status in setOf(TransactionStatus.CLEARED, TransactionStatus.RECONCILE) })
    }

    @Test
    fun `given transactions when filtering by transaction type then returns only that type`() {
        val result = TransactionsUtilityService.filterByTransactionType(testTransactions, "income")
        
        assertEquals(4, result.size)
        assertTrue(result.all { it.transactionType == "income" })
    }

    @Test
    fun `given transactions when filtering by amount greater than then returns matching transactions`() {
        val result = TransactionsUtilityService.filterByAmountGreaterThan(testTransactions, BigDecimal("100"))
        
        assertEquals(4, result.size)
        assertTrue(result.all { it.amount >= BigDecimal("100") })
    }

    @Test
    fun `given transactions when filtering by amount less than then returns matching transactions`() {
        val result = TransactionsUtilityService.filterByAmountLessThan(testTransactions, BigDecimal("100"))
        
        assertEquals(7, result.size)
        assertTrue(result.all { it.amount <= BigDecimal("100") })
    }

    @Test
    fun `given transactions when filtering by amount range then returns matching transactions`() {
        val result = TransactionsUtilityService.filterByAmountRange(testTransactions, BigDecimal("50"), BigDecimal("200"))
        
        assertEquals(2, result.size)
        assertTrue(result.all { it.amount >= BigDecimal("50") && it.amount <= BigDecimal("200") })
    }

    @Test
    fun `given transactions when filtering by date range then returns matching transactions`() {
        val startDate = Instant.parse("2024-01-10T00:00:00Z").toEpochMilli()
        val endDate = Instant.parse("2024-01-20T23:59:59Z").toEpochMilli()
        val result = TransactionsUtilityService.filterByDateRange(testTransactions, startDate, endDate)
        
        assertEquals(5, result.size)
        assertTrue(result.all { it.timestamp in startDate..endDate })
    }

    @Test
    fun `given transactions when filtering by tags then returns transactions with any tag`() {
        val result = TransactionsUtilityService.filterByTags(testTransactions, setOf("essentials", "travel"))
        
        assertEquals(2, result.size)
        assertTrue(result.all { it.tags.intersect(setOf("essentials", "travel")).isNotEmpty() })
    }

    @Test
    fun `given transactions when filtering by all tags then returns transactions with all tags`() {
        val result = TransactionsUtilityService.filterByAllTags(testTransactions, setOf("business", "travel"))
        
        assertEquals(1, result.size)
        assertEquals("Travel Expenses", result[0].title)
    }

    // ==================== Additional Grouping Tests ====================

    @Test
    fun `given transactions when grouped by payee then returns correct groups`() {
        val result = TransactionsUtilityService.groupByPayee(testTransactions)
        
        assertTrue(result.size >= 1)
    }

    @Test
    fun `given transactions when grouped by category id then returns correct groups`() {
        val result = TransactionsUtilityService.groupByCategoryId(testTransactions)
        
        assertEquals(6, result.size)
        assertEquals(3, result[1L]?.size)
        assertEquals(2, result[2L]?.size)
    }

    @Test
    fun `given transactions when grouped by title then returns correct groups`() {
        val result = TransactionsUtilityService.groupByTitle(testTransactions)
        
        assertEquals(10, result.size)
    }

    // ==================== Additional Breakdown Tests ====================

    @Test
    fun `given transactions when broken down by account for category then returns correct breakdown`() {
        val result = TransactionsUtilityService.breakdownByAccountForCategory(testTransactions, testAccounts, "Food")
        
        assertEquals(1, result.size)
    }

    @Test
    fun `given transactions when broken down by account for tag then returns correct breakdown`() {
        val result = TransactionsUtilityService.breakdownByAccountForTag(testTransactions, testAccounts, "essentials")
        
        assertEquals(1, result.size)
    }

    @Test
    fun `given transactions when broken down by transaction type for account then returns correct breakdown`() {
        val result = TransactionsUtilityService.breakdownByTransactionTypeForAccount(testTransactions, "acc1")
        
        assertEquals(2, result.size)
    }

    @Test
    fun `given transactions when broken down by payment method for account then returns correct breakdown`() {
        val result = TransactionsUtilityService.breakdownByPaymentMethodForAccount(testTransactions, "acc1")
        
        assertEquals(3, result.size)
    }

    @Test
    fun `given transactions when broken down by status for account then returns correct breakdown`() {
        val result = TransactionsUtilityService.breakdownByStatusForAccount(testTransactions, "acc1")
        
        assertEquals(3, result.size)
    }

    @Test
    fun `given transactions when broken down by tags for account then returns correct breakdown`() {
        val result = TransactionsUtilityService.breakdownByTagsForAccount(testTransactions, "acc1")
        
        assertEquals(5, result.size)
    }

    @Test
    fun `given transactions when broken down by account and date then returns correct nested breakdown`() {
        val result = TransactionsUtilityService.breakdownByAccountAndDate(testTransactions)
        
        assertEquals(2, result.size)
        assertTrue(result.containsKey("acc1"))
        assertTrue(result.containsKey("acc2"))
    }

    @Test
    fun `given transactions when broken down by account and month then returns correct nested breakdown`() {
        val result = TransactionsUtilityService.breakdownByAccountAndMonth(testTransactions)
        
        assertEquals(2, result.size)
        assertTrue(result.containsKey("acc1"))
        assertTrue(result.containsKey("acc2"))
    }

    @Test
    fun `given transactions when broken down by account and transaction type then returns correct nested breakdown`() {
        val result = TransactionsUtilityService.breakdownByAccountAndTransactionType(testTransactions)
        
        assertEquals(2, result.size)
        assertTrue(result.containsKey("acc1"))
        assertTrue(result.containsKey("acc2"))
    }

    @Test
    fun `given transactions when broken down by account and payment method then returns correct nested breakdown`() {
        val result = TransactionsUtilityService.breakdownByAccountAndPaymentMethod(testTransactions)
        
        assertEquals(2, result.size)
        assertTrue(result.containsKey("acc1"))
        assertTrue(result.containsKey("acc2"))
    }

    @Test
    fun `given transactions when broken down by account and status then returns correct nested breakdown`() {
        val result = TransactionsUtilityService.breakdownByAccountAndStatus(testTransactions)
        
        assertEquals(2, result.size)
        assertTrue(result.containsKey("acc1"))
        assertTrue(result.containsKey("acc2"))
    }

    @Test
    fun `given transactions when broken down by account and tags then returns correct nested breakdown`() {
        val result = TransactionsUtilityService.breakdownByAccountAndTags(testTransactions)
        
        assertEquals(2, result.size)
        assertTrue(result.containsKey("acc1"))
        assertTrue(result.containsKey("acc2"))
    }

    @Test
    fun `given transactions when broken down by date and account then returns correct nested breakdown`() {
        val result = TransactionsUtilityService.breakdownByDateAndAccount(testTransactions, testAccounts)
        
        assertEquals(10, result.size)
    }

    @Test
    fun `given transactions when broken down by month and account then returns correct nested breakdown`() {
        val result = TransactionsUtilityService.breakdownByMonthAndAccount(testTransactions, testAccounts)
        
        assertEquals(1, result.size)
        assertTrue(result.containsKey("2024-01"))
    }

    @Test
    fun `given transactions when broken down by transaction type and account then returns correct nested breakdown`() {
        val result = TransactionsUtilityService.breakdownByTransactionTypeAndAccount(testTransactions, testAccounts)
        
        assertEquals(2, result.size)
        assertTrue(result.containsKey("income"))
        assertTrue(result.containsKey("expense"))
    }

    @Test
    fun `given transactions when broken down by payment method and account then returns correct nested breakdown`() {
        val result = TransactionsUtilityService.breakdownByPaymentMethodAndAccount(testTransactions, testAccounts)
        
        assertEquals(5, result.size)
    }

    @Test
    fun `given transactions when broken down by status and account then returns correct nested breakdown`() {
        val result = TransactionsUtilityService.breakdownByStatusAndAccount(testTransactions, testAccounts)
        
        assertEquals(3, result.size)
        assertTrue(result.containsKey("CLEARED"))
        assertTrue(result.containsKey("UNCLEARED"))
        assertTrue(result.containsKey("RECONCILE"))
    }

    @Test
    fun `given transactions when broken down by tags and account then returns correct nested breakdown`() {
        val result = TransactionsUtilityService.breakdownByTagsAndAccount(testTransactions, testAccounts)
        
        assertEquals(8, result.size)
    }

    // ==================== Calculation Method Tests ====================

    @Test
    fun `given transactions when calculating transaction summary with single currency then returns correct summary`() {
        val singleCurrencyTransactions = testTransactions.filter { it.currency == "USD" }
        val result = TransactionsUtilityService.calculateTransactionSummary(singleCurrencyTransactions)
        
        assertNotNull(result)
        assertEquals(6, result.transactionCount)
        assertEquals("USD", singleCurrencyTransactions.first().currency)
    }

    @Test
    fun `given transactions when calculating transaction summary with multi-currency then throws exception`() {
        var exceptionThrown = false
        var exceptionMessage = ""
        
        try {
            TransactionsUtilityService.calculateTransactionSummary(testTransactions)
        } catch (e: IllegalArgumentException) {
            exceptionThrown = true
            exceptionMessage = e.message ?: ""
        }
        
        assertTrue(exceptionThrown)
        assertTrue(exceptionMessage.contains("same currency"))
    }

    @Test
    fun `given transactions when calculating summary by currency then returns correct summaries`() {
        val result = TransactionsUtilityService.calculateSummaryByCurrency(testTransactions)
        
        assertEquals(3, result.size)
        assertTrue(result.containsKey("USD"))
        assertTrue(result.containsKey("EUR"))
        assertTrue(result.containsKey("GBP"))
    }

    @Test
    fun `given transactions when calculating income expense breakdown without conversion then returns correct breakdown`() {
        val result = TransactionsUtilityService.calculateIncomeExpenseBreakdown(testTransactions)
        
        assertEquals(4, result.incomeTransactions.size)
        assertEquals(6, result.expenseTransactions.size)
        assertEquals(3, result.incomeCurrencyBreakdown.size)
        assertEquals(3, result.expenseCurrencyBreakdown.size)
    }

    @Test
    fun `given transactions when calculating income expense breakdown with exchange rates then returns converted breakdown`() {
        val exchangeRates = mapOf("EUR" to BigDecimal("1.1"), "GBP" to BigDecimal("1.25"))
        val result = TransactionsUtilityService.calculateIncomeExpenseBreakdown(
            testTransactions,
            exchangeRates,
            "USD"
        )
        
        assertEquals(4, result.incomeTransactions.size)
        assertEquals(6, result.expenseTransactions.size)
        assertNotNull(result.totalIncome)
        assertNotNull(result.totalExpense)
    }

    @Test
    fun `given transactions when calculating income expense breakdown with exchange rates but no base currency then throws exception`() {
        val exchangeRates = mapOf("EUR" to BigDecimal("1.1"), "GBP" to BigDecimal("1.25"))
        
        var exceptionThrown = false
        var exceptionMessage = ""
        
        try {
            TransactionsUtilityService.calculateIncomeExpenseBreakdown(testTransactions, exchangeRates, null)
        } catch (e: IllegalArgumentException) {
            exceptionThrown = true
            exceptionMessage = e.message ?: ""
        }
        
        assertTrue(exceptionThrown)
        assertTrue(exceptionMessage.contains("baseCurrency must be provided"))
    }

    @Test
    fun `given summaries when calculating total in base currency then returns aggregated summary`() {
        val summaries = mapOf(
            "USD" to FinancialSummary(BigDecimal("100"), BigDecimal("200"), BigDecimal("-100"), 2),
            "EUR" to FinancialSummary(BigDecimal("55"), BigDecimal("110"), BigDecimal("-55"), 1)
        )
        // Rate: 1 USD = 1.1 EUR. So 55 EUR = 55 / 1.1 = 50 USD
        val exchangeRates = mapOf("EUR" to BigDecimal("1.1"))
        
        val result = TransactionsUtilityService.calculateTotalInBaseCurrency(summaries, "USD", exchangeRates)
        
        // 100 USD + (55 EUR / 1.1) = 150 USD
        assertEquals(0, BigDecimal("150.0").compareTo(result.totalAmount.setScale(1, java.math.RoundingMode.HALF_UP)))
        assertEquals(0, BigDecimal("300.0").compareTo(result.income.setScale(1, java.math.RoundingMode.HALF_UP)))
        assertEquals(0, BigDecimal("-150.0").compareTo(result.expense.setScale(1, java.math.RoundingMode.HALF_UP)))
        assertEquals(3, result.transactionCount)
    }

    @Test
    fun `given breakdowns when calculating cumulative sum then returns running balance`() {
        val breakdowns = listOf(
            DateBreakdown("2024-01-01", FinancialSummary(BigDecimal("100"), BigDecimal("100"), BigDecimal.ZERO, 1), emptyMap()),
            DateBreakdown("2024-01-02", FinancialSummary(BigDecimal("-50"), BigDecimal.ZERO, BigDecimal("-50"), 1), emptyMap()),
            DateBreakdown("2024-01-03", FinancialSummary(BigDecimal("200"), BigDecimal("200"), BigDecimal.ZERO, 1), emptyMap())
        )
        
        val result = TransactionsUtilityService.calculateCumulativeSum(breakdowns, BigDecimal("500"))
        
        assertEquals(3, result.size)
        assertEquals(BigDecimal("600"), result[0].cumulativeBalance)
        assertEquals(BigDecimal("550"), result[1].cumulativeBalance)
        assertEquals(BigDecimal("750"), result[2].cumulativeBalance)
    }

    @Test
    fun `given summaries when calculating delta then returns correct changes`() {
        val current = FinancialSummary(BigDecimal("150"), BigDecimal("200"), BigDecimal("-50"), 2)
        val previous = FinancialSummary(BigDecimal("100"), BigDecimal("150"), BigDecimal("-50"), 2)
        
        val result = TransactionsUtilityService.calculateSummaryDelta(current, previous)
        
        assertEquals(BigDecimal("50"), result.absoluteChange)
        assertEquals(50.0, result.percentageChange, 0.001)
        assertEquals(BigDecimal("50"), result.incomeChange)
        assertEquals(BigDecimal.ZERO, result.expenseChange)
    }

    @Test
    fun `given transactions when filtering with empty filter criteria then returns all transactions`() {
        val filter = TransactionFilter()
        val result = TransactionsUtilityService.filter(testTransactions, filter)
        
        assertEquals(10, result.size)
    }

    @Test
    fun `given transactions when filtering by non-existent category then returns empty list`() {
        val result = TransactionsUtilityService.filterByCategory(testTransactions, "NonExistent")
        
        assertTrue(result.isEmpty())
    }

    @Test
    fun `given transactions when filtering by non-existent account then returns empty list`() {
        val result = TransactionsUtilityService.filterByAccount(testTransactions, "nonexistent")
        
        assertTrue(result.isEmpty())
    }

    @Test
    fun `given transactions when filtering by non-existent currency then returns empty list`() {
        val result = TransactionsUtilityService.filterByCurrency(testTransactions, "JPY")
        
        assertTrue(result.isEmpty())
    }

    @Test
    fun `given transactions when broken down by label with empty tags then returns correct breakdown`() {
        val transactionsWithoutTags = testTransactions.filter { it.tags.isEmpty() }
        val result = TransactionsUtilityService.breakdownByLabel(transactionsWithoutTags, { tx -> tx.category })
        
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `given transactions when calculating total by currency with empty list then returns empty map`() {
        val result = TransactionsUtilityService.calculateTotalByCurrency(emptyList())
        
        assertTrue(result.isEmpty())
    }

    @Test
    fun `given transactions when separating income expense with empty list then returns empty pair`() {
        val (income, expenses) = TransactionsUtilityService.separateIncomeExpense(emptyList())
        
        assertTrue(income.isEmpty())
        assertTrue(expenses.isEmpty())
    }

    @Test
    fun `given transactions when calculating summary by currency with empty list then returns empty map`() {
        val result = TransactionsUtilityService.calculateSummaryByCurrency(emptyList())
        
        assertTrue(result.isEmpty())
    }

    @Test
    fun `given income and expense when calculating savings rate then returns correct fraction`() {
        // (1000 - 600) / 1000 = 0.4
        val result = TransactionsUtilityService.calculateSavingsRate(BigDecimal("1000"), BigDecimal("-600"))
        assertEquals(0.4, result, 0.001)
        
        // Zero income
        assertEquals(0.0, TransactionsUtilityService.calculateSavingsRate(BigDecimal.ZERO, BigDecimal("-600")), 0.0)
        
        // Negative net flow
        assertEquals(0.0, TransactionsUtilityService.calculateSavingsRate(BigDecimal("500"), BigDecimal("-600")), 0.0)
    }

    @Test
    fun `given total expense and days active when calculating burn rate then returns monthly average`() {
        // (1000 / 10) * 30 = 3000
        val result = TransactionsUtilityService.calculateBurnRate(BigDecimal("-1000"), 10)
        assertEquals(0, BigDecimal("3000.00").compareTo(result))
    }

    @Test
    fun `given transactions when calculating daily net flow then returns correctly bucketed amounts`() {
        val result = TransactionsUtilityService.calculateDailyNetFlow(testTransactions, 1000) // 1000 days to catch 2024
        
        assertTrue(result.isNotEmpty())
        // All test transactions are on different days in Jan 2024
        assertEquals(10, result.size)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `given empty list when performing operations then returns empty results`() {
        val emptyList = emptyList<Transaction>()
        
        val currencyGroups = TransactionsUtilityService.groupByCurrency(emptyList)
        assertTrue(currencyGroups.isEmpty())
        
        val categoryGroups = TransactionsUtilityService.groupByCategory(emptyList)
        assertTrue(categoryGroups.isEmpty())
        
        val aggregate = TransactionsUtilityService.calculateTotalByCurrency(emptyList)
        assertTrue(aggregate.isEmpty())
        
        val (income, expenses) = TransactionsUtilityService.separateIncomeExpense(emptyList)
        assertTrue(income.isEmpty())
        assertTrue(expenses.isEmpty())
        
        val breakdown = TransactionsUtilityService.breakdownByCategory(emptyList)
        assertTrue(breakdown.isEmpty())
    }

    @Test
    fun `given multi-currency transactions when grouped then handles correctly`() {
        val multiCurrencyTransactions = listOf(
            Transaction(
                id = 1L,
                title = "USD Transaction",
                amount = BigDecimal("100"),
                category = "Test",
                categoryId = 1L,
                timestamp = System.currentTimeMillis(),
                currency = "USD",
                transactionType = "income",
                accountId = "acc1"
            ),
            Transaction(
                id = 2L,
                title = "EUR Transaction",
                amount = BigDecimal("50"),
                category = "Test",
                categoryId = 1L,
                timestamp = System.currentTimeMillis(),
                currency = "EUR",
                transactionType = "income",
                accountId = "acc1"
            )
        )
        
        val currencyGroups = TransactionsUtilityService.groupByCurrency(multiCurrencyTransactions)
        
        assertEquals(2, currencyGroups.size)
        assertTrue(currencyGroups.containsKey("USD"))
        assertTrue(currencyGroups.containsKey("EUR"))
    }
}
