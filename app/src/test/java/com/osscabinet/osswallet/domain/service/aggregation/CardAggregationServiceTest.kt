package com.osscabinet.ossWallet.domain.service.aggregation

import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.settings.AccountHeroCardConfig
import com.osscabinet.ossWallet.domain.model.settings.AccountsCardConfig
import com.osscabinet.ossWallet.domain.model.settings.BudgetsCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CategoryPieChartCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CategoryTreemapCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig
import com.osscabinet.ossWallet.domain.model.settings.LoansCardConfig
import com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig
import com.osscabinet.ossWallet.domain.model.settings.RecentActivitiesCardConfig
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.domain.service.financial.FinancialSummary
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CardAggregationServiceTest {

    private val queryService: DatabaseQueryService = mockk()
    private val profileId = 1
    private val baseCurrency = "USD"
    private val exchangeRates = ExchangeRateSnapshot(
        anchor = baseCurrency,
        rates = mapOf(baseCurrency to BigDecimal.ONE)
    )

    private val sampleAccount = Account(
        id = "acc1",
        name = "Checking",
        currencyCode = "USD",
        structureType = AccountStructureType.BANK,
        colorHex = "#000000",
        icon = "bank"
    )

    @Before
    fun setup() {
        mockkObject(TransactionAggregationService)
        mockkObject(TimelineService)
        // Defensive defaults so timeline helpers never throw when a test does not care
        // about period boundaries. Individual tests override these as needed.
        every { TimelineService.getDateRange(any(), any()) } returns Pair<Long?, Long?>(null, null)
        every { TimelineService.getPreviousRange(any(), any()) } returns Pair<Long?, Long?>(null, null)
    }

    private fun fs(
        total: String? = null,
        income: String = "0",
        expense: String = "0",
        transferIn: String = "0",
        transferOut: String = "0",
    ) = FinancialSummary(
        income = BigDecimal(income),
        expense = BigDecimal(expense),
        transferIn = BigDecimal(transferIn),
        transferOut = BigDecimal(transferOut),
        transactionCount = 1
    )

    private fun baseInput(
        filter: TransactionFilter = TransactionFilter(timelineRange = TimelineRange.AllTime()),
        accounts: List<Account> = listOf(sampleAccount),
        budgets: List<Budget> = emptyList(),
        loans: List<Loan> = emptyList(),
    ) = CardInput(
        profileId = profileId,
        targetCurrency = baseCurrency,
        exchangeRates = exchangeRates,
        filter = filter,
        accounts = accounts,
        categories = emptyList(),
        budgets = budgets,
        loans = loans,
    )

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `computeAccountsCard populates current and previous flow metrics correctly`() = runBlocking {
        val config = AccountsCardConfig(
            cardId = "test",
            showComparison = true,
            filter = TransactionFilter(timelineRange = TimelineRange.ThisPeriod(ThisPeriodType.MONTH))
        )
        
        val input = CardInput(
            profileId = profileId,
            targetCurrency = baseCurrency,
            exchangeRates = exchangeRates,
            filter = TransactionFilter(timelineRange = TimelineRange.ThisPeriod(ThisPeriodType.MONTH)),
            accounts = listOf(sampleAccount),
            categories = emptyList()
        )

        // Mock current period summary
        val currentSummary = mapOf(
            "acc1" to mapOf(
                "USD" to FinancialSummary(
                    income = BigDecimal("5000.00"), // This will be the balance for balanceFilter
                    expense = BigDecimal("-200.00"),
                    transferIn = BigDecimal.ZERO,
                    transferOut = BigDecimal.ZERO,
                    transactionCount = 5
                )
            )
        )
        
        // Mock current period ONLY summary (for flow)
        val currentFlowSummary = mapOf(
            "acc1" to mapOf(
                "USD" to FinancialSummary(
                    income = BigDecimal("1000.00"),
                    expense = BigDecimal("-200.00"),
                    transferIn = BigDecimal.ZERO,
                    transferOut = BigDecimal.ZERO,
                    transactionCount = 5
                )
            )
        )
        
        // Mock previous period summary
        val previousSummary = mapOf(
            "acc1" to mapOf(
                "USD" to FinancialSummary(
                    income = BigDecimal("4200.00"),
                    expense = BigDecimal("-150.00"),
                    transferIn = BigDecimal.ZERO,
                    transferOut = BigDecimal.ZERO,
                    transactionCount = 4
                )
            )
        )
        
        // Mock previous period ONLY summary (for flow)
        val previousFlowSummary = mapOf(
            "acc1" to mapOf(
                "USD" to FinancialSummary(
                    income = BigDecimal("800.00"),
                    expense = BigDecimal("-150.00"),
                    transferIn = BigDecimal.ZERO,
                    transferOut = BigDecimal.ZERO,
                    transactionCount = 4
                )
            )
        )

        coEvery { 
            TransactionAggregationService.getSummaryByAccountAndCurrency(queryService, profileId, any(), any()) 
        } returnsMany listOf(
            Result.success(currentSummary), // Balance filter call (current)
            Result.success(currentFlowSummary), // Current period call
            Result.success(previousSummary), // Balance filter call (previous)
            Result.success(previousFlowSummary)  // Previous period call
        )

        every { TimelineService.getPreviousRange(any()) } returns Pair(1000L, 2000L)

        val result = CardAggregationService.computeAccountsCard(queryService, profileId, input, config)

        val summary = result.accountSummaries["acc1"]
        assertNotNull(summary)
        
        // Current Period Flow
        assertEquals(0, BigDecimal("1000.00").compareTo(summary!!.currentPeriodMetrics.income))
        assertEquals(0, BigDecimal("-200.00").compareTo(summary.currentPeriodMetrics.expense))
        
        // Previous Period Flow
        assertNotNull(summary.previousPeriodMetrics)
        assertEquals(0, BigDecimal("800.00").compareTo(summary.previousPeriodMetrics?.income))
        assertEquals(0, BigDecimal("-150.00").compareTo(summary.previousPeriodMetrics?.expense))
        
        // Balance (Cumulative) - 5000 + (-200) = 4800
        assertEquals(0, BigDecimal("4800.00").compareTo(summary.balance))
    }

    @Test
    fun `computeNetWorthCard computes net worth from cumulative currency balances`() = runBlocking {
        val config = NetWorthCardConfig(
            cardId = "t",
            showComparison = false,
            showTrendLine = false,
            filter = TransactionFilter(timelineRange = TimelineRange.AllTime())
        )
        coEvery { TransactionAggregationService.getSummaryByCurrency(any(), any(), any(), any()) } returns
            Result.success(mapOf("USD" to fs(income = "5200.00", expense = "-200.00")))

        val result = CardAggregationService.computeNetWorthCard(queryService, profileId, baseInput(), config)

        assertEquals(0, BigDecimal("5000.00").compareTo(result.currentPeriodMetrics.netWorth))
        assertNull(result.previousPeriodMetrics)
    }

    @Test
    fun `computeCashFlowCard aggregates income and expense into base currency`() = runBlocking {
        val config = CashFlowCardConfig(
            cardId = "t",
            filter = TransactionFilter(timelineRange = TimelineRange.AllTime())
        )
        coEvery { TransactionAggregationService.getSummaryByCurrency(any(), any(), any(), any()) } returns
            Result.success(mapOf("USD" to fs(total = "700.00", income = "1000.00", expense = "-300.00")))

        val result = CardAggregationService.computeCashFlowCard(queryService, profileId, baseInput(), config)

        assertEquals(0, BigDecimal("1000.00").compareTo(result.result.metricsInBase.income))
        assertEquals(0, BigDecimal("-300.00").compareTo(result.result.metricsInBase.expense))
    }

    @Test
    fun `computeCurrencyBalancesCard maps cumulative balance per currency`() = runBlocking {
        val config = CurrencyBalancesCardConfig(
            cardId = "t",
            filter = TransactionFilter(timelineRange = TimelineRange.AllTime())
        )
        coEvery { TransactionAggregationService.getSummaryByCurrency(any(), any(), any(), any()) } returns
            Result.success(mapOf("USD" to fs(income = "5000.00")))

        val result = CardAggregationService.computeCurrencyBalancesCard(queryService, profileId, baseInput(), config)

        assertEquals(1, result.items.size)
        assertEquals("USD", result.items[0].currency)
        assertEquals(0, BigDecimal("5000.00").compareTo(result.items[0].amount))
    }

    @Test
    fun `computeLoansCard computes total paid and remaining for a lent loan`() = runBlocking {
        val loan = Loan(
            name = "Friend loan",
            principalAmount = BigDecimal("1000.00"),
            currency = "USD",
            startDate = 0L,
            lenderOrBorrowerName = "Alex",
            loanType = LoanType.LENT,
            linkedAccountId = "acc1",
            profileId = profileId
        )
        val config = LoansCardConfig(
            cardId = "t",
            showType = "ALL",
            filter = TransactionFilter(timelineRange = TimelineRange.AllTime())
        )
        coEvery { TransactionAggregationService.getSummaryByCurrency(any(), any(), any(), any()) } returns
            Result.success(mapOf("USD" to fs(total = "400.00", income = "400.00")))

        val result = CardAggregationService.computeLoansCard(queryService, profileId, baseInput(loans = listOf(loan)), config)

        assertEquals(1, result.items.size)
        // LENT repayment = income + transferIn = 400; remaining = principal - paid.
        assertEquals(0, BigDecimal("400.00").compareTo(result.items[0].totalPaid))
        assertEquals(0, BigDecimal("600.00").compareTo(result.items[0].remainingBalance))
    }

    @Test
    fun `computeBudgetsCard builds progress with signed spent for expense budgets`() = runBlocking {
        val budget = Budget(
            name = "Groceries",
            currency = "USD",
            limitAmount = BigDecimal("500.00"),
            timelineRange = TimelineRange.AllTime(),
            profileId = profileId
        )
        val config = BudgetsCardConfig(cardId = "t")
        coEvery { TransactionAggregationService.getSummaryByCurrency(any(), any(), any(), any()) } returns
            Result.success(mapOf("USD" to fs(total = "-200.00", expense = "-200.00")))

        val result = CardAggregationService.computeBudgetsCard(queryService, profileId, baseInput(budgets = listOf(budget)), config)

        assertEquals(1, result.items.size)
        // Expense budget: spent is the negated (positive) total amount.
        assertEquals(0, BigDecimal("200.00").compareTo(result.items[0].spent))
        assertEquals(0, BigDecimal("500.00").compareTo(result.items[0].limit))
    }

    @Test
    fun `computeAccountHeroCard reports lifetime balance and current period flow`() = runBlocking {
        val config = AccountHeroCardConfig(cardId = "t", showComparison = false)
        val input = baseInput(
            filter = TransactionFilter(accountIds = setOf("acc1"), timelineRange = TimelineRange.AllTime())
        )
        coEvery { TransactionAggregationService.getSummaryByCurrency(any(), any(), any(), any()) } returns
            Result.success(mapOf("USD" to fs(income = "5200.00", expense = "-200.00")))
        coEvery { queryService.getMinTransactionTimestamp(any()) } returns 0L
        coEvery { queryService.aggregate(any(), any(), any(), any(), any(), any(), any()) } returns Result.success(emptyList())

        val result = CardAggregationService.computeAccountHeroCard(queryService, profileId, input, config)

        assertEquals(0, BigDecimal("5000.00").compareTo(result.summary.balance))
        assertEquals(0, BigDecimal("5200.00").compareTo(result.summary.currentPeriodMetrics.income))
    }

    @Test
    fun `computeRecentBalances accumulates running balance per account from starting balance`() {
        val t1 = Transaction(
            name = "Salary", amount = BigDecimal("100.00"), timestamp = 1L,
            currency = "USD", transactionType = TransactionType.INCOME, accountId = "acc1", categoryId = 1L
        )
        val t2 = Transaction(
            name = "Coffee", amount = BigDecimal("-30.00"), timestamp = 2L,
            currency = "USD", transactionType = TransactionType.EXPENSE, accountId = "acc1", categoryId = 1L
        )

        // Pass newest-first to confirm the method sorts chronologically before folding.
        val result = CardAggregationService.computeRecentBalances(
            transactions = listOf(t2, t1),
            accounts = listOf(sampleAccount),
            baseCurrency = baseCurrency,
            rates = exchangeRates,
            startingBalances = mapOf("acc1" to BigDecimal("1000.00"))
        )

        val oldest = result.first { it.transaction.timestamp == 1L }
        val newest = result.first { it.transaction.timestamp == 2L }
        assertEquals(0, BigDecimal("1100.00").compareTo(oldest.balanceAfter)) // 1000 + 100
        assertEquals(0, BigDecimal("1070.00").compareTo(newest.balanceAfter)) // 1100 - 30
        // Result is ordered newest-first.
        assertEquals(2L, result.first().transaction.timestamp)
    }

    @Test
    fun `computeNetWorthCard includes previous period metrics when comparison enabled`() = runBlocking {
        val config = NetWorthCardConfig(
            cardId = "t",
            showComparison = true,
            showTrendLine = false,
            filter = TransactionFilter(timelineRange = TimelineRange.AllTime())
        )
        every { TimelineService.getPreviousRange(any(), any()) } returns Pair(1000L, 2000L)
        coEvery { TransactionAggregationService.getSummaryByCurrency(any(), any(), any(), any()) } returns
            Result.success(mapOf("USD" to fs(income = "5200.00", expense = "-200.00")))

        val result = CardAggregationService.computeNetWorthCard(queryService, profileId, baseInput(), config)

        assertNotNull(result.previousPeriodMetrics)
        assertEquals(0, BigDecimal("5000.00").compareTo(result.previousPeriodMetrics!!.netWorth))
    }

    @Test
    fun `computeNetWorthCard builds the trend from opening balance plus daily net flow`() = runBlocking {
        val zone = TimelineService.systemZoneId
        val start = java.time.LocalDate.of(2025, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = java.time.LocalDate.of(2025, 1, 3).atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
        val config = NetWorthCardConfig(
            cardId = "t",
            showComparison = false,
            showTrendLine = true,
            filter = TransactionFilter(timelineRange = TimelineRange.CustomRange(start, end))
        )
        every { TimelineService.getDateRange(any(), any()) } returns Pair<Long?, Long?>(start, end)
        // Opening balance (everything before Jan 1) is 5000 and Jan 2 adds 100.
        coEvery { TransactionAggregationService.getSummaryByCurrency(any(), any(), any(), any()) } returns
            Result.success(mapOf("USD" to fs(income = "5200.00", expense = "-200.00")))
        coEvery { TransactionAggregationService.getSummaryByCurrencyByDay(any(), any(), any(), any()) } returns
            Result.success(mapOf("2025-01-02" to mapOf("USD" to fs(income = "100.00"))))

        val result = CardAggregationService.computeNetWorthCard(queryService, profileId, baseInput(), config)

        assertEquals(
            listOf("5000.00", "5100.00", "5100.00"),
            result.trend.map { it.value.setScale(2).toPlainString() }
        )
    }

    @Test
    fun `computeNetWorthCard skips the trend when the setting is off`() = runBlocking {
        val config = NetWorthCardConfig(
            cardId = "t",
            showComparison = false,
            showTrendLine = false,
            filter = TransactionFilter(timelineRange = TimelineRange.AllTime())
        )
        coEvery { TransactionAggregationService.getSummaryByCurrency(any(), any(), any(), any()) } returns
            Result.success(mapOf("USD" to fs(income = "5200.00", expense = "-200.00")))

        val result = CardAggregationService.computeNetWorthCard(queryService, profileId, baseInput(), config)

        assertEquals(emptyList<Any>(), result.trend)
        coVerify(exactly = 0) { TransactionAggregationService.getSummaryByCurrencyByDay(any(), any(), any(), any()) }
    }

    @Test
    fun `computeCategoryPieChartCard sums category expense into totals`() = runBlocking {
        val category = Category(id = 100L, name = "Food", iconName = "food", colorHex = 0, isDefault = false, parentId = null)
        val config = CategoryPieChartCardConfig(
            cardId = "t",
            showComparison = false,
            filter = TransactionFilter(timelineRange = TimelineRange.AllTime())
        )
        val input = CardInput(
            profileId = profileId,
            targetCurrency = baseCurrency,
            exchangeRates = exchangeRates,
            filter = TransactionFilter(timelineRange = TimelineRange.AllTime()),
            accounts = listOf(sampleAccount),
            categories = listOf(category)
        )
        coEvery { TransactionAggregationService.getSummaryByCategoryAndCurrency(any(), any(), any(), any()) } returns
            Result.success(mapOf(100L to mapOf("USD" to fs(total = "-300.00", expense = "-300.00"))))

        val result = CardAggregationService.computeCategoryPieChartCard(queryService, profileId, input, config)

        assertEquals(0, BigDecimal("-300.00").compareTo(result.expenseTotal))
    }

    @Test
    fun `computeCategoryTreemapCard sums category expense into totals`() = runBlocking {
        val category = Category(id = 100L, name = "Food", iconName = "food", colorHex = 0, isDefault = false, parentId = null)
        val config = CategoryTreemapCardConfig(
            cardId = "t",
            showComparison = false,
            filter = TransactionFilter(timelineRange = TimelineRange.AllTime())
        )
        val input = CardInput(
            profileId = profileId,
            targetCurrency = baseCurrency,
            exchangeRates = exchangeRates,
            filter = TransactionFilter(timelineRange = TimelineRange.AllTime()),
            accounts = listOf(sampleAccount),
            categories = listOf(category)
        )
        coEvery { TransactionAggregationService.getSummaryByCategoryAndCurrency(any(), any(), any(), any()) } returns
            Result.success(mapOf(100L to mapOf("USD" to fs(total = "-300.00", expense = "-300.00"))))

        val result = CardAggregationService.computeCategoryTreemapCard(queryService, profileId, input, config)

        assertEquals(0, BigDecimal("-300.00").compareTo(result.expenseTotal))
    }

    @Test
    fun `computeRecentActivitiesCard builds grouped list state from filtered transactions`() = runBlocking {
        val tx = Transaction(
            name = "Coffee", amount = BigDecimal("-10.00"), timestamp = 5L,
            currency = "USD", transactionType = TransactionType.EXPENSE, accountId = "acc1", categoryId = 1L
        )
        val config = RecentActivitiesCardConfig(
            cardId = "t",
            filter = TransactionFilter(timelineRange = TimelineRange.AllTime())
        )
        every { queryService.observeFilteredTransactions(any(), any(), any()) } returns flowOf(listOf(tx))
        coEvery { TransactionAggregationService.getSummaryByAccountAndCurrency(any(), any(), any(), any()) } returns
            Result.success(mapOf("acc1" to mapOf("USD" to fs(total = "1000.00"))))
        every {
            TimelineService.getGroupedData(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
            )
        } returns emptyList()

        val result = CardAggregationService.computeRecentActivitiesCard(queryService, profileId, baseInput(), config)

        assertEquals(baseCurrency, result.currency)
        assertEquals(true, result.listState.isEmpty)
        verify { queryService.observeFilteredTransactions(any(), any(), any()) }
    }
}
