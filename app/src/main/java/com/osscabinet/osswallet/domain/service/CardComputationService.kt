package com.osscabinet.ossWallet.domain.service

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.analytics.CategoryBreakdownData
import com.osscabinet.ossWallet.domain.model.analytics.CategoryExpenseTreemapData
import com.osscabinet.ossWallet.domain.model.analytics.TimelineTrajectoryData
import com.osscabinet.ossWallet.domain.model.analytics.ComputationContext
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.model.analytics.AccountSummaryItem
import com.osscabinet.ossWallet.domain.model.analytics.CashFlowResult
import com.osscabinet.ossWallet.domain.model.analytics.CashFlowBreakdownItem
import com.osscabinet.ossWallet.domain.model.analytics.CurrencyAggregationItem
import com.osscabinet.ossWallet.data.local.dao.CategoryDao
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.osscabinet.ossWallet.ui.analytics.components.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.Locale
import javax.inject.Inject
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

/**
 * Service for computing card data with scope-aware filtering.
 * Provides unified computation logic for all card types across different screens.
 */
interface CardComputationService : CardComputationEngine {
    
    suspend fun computeCategoryBreakdown(
        config: CategoryBreakdownCardConfig,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): List<CategoryBreakdownData>

    suspend fun computeTimelineTrajectory(
        config: TimelineTrajectoryCardConfig,
        allTransactions: List<Transaction>,
        accounts: List<Account>,
        context: ComputationContext
    ): List<TimelineTrajectoryData>

    suspend fun computeCategoryExpenseTreemap(
        config: CategoryExpenseTreemapCardConfig,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): List<CategoryExpenseTreemapData>

    suspend fun computeIncomeExpensesSummary(
        config: IncomeExpensesSummaryCardConfig,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): IncomeExpensesSummaryData

    suspend fun computePeriodComparison(
        config: PeriodComparisonCardConfig,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): PeriodComparisonData

    suspend fun computeCashFlowTrend(
        config: CashFlowTrendCardConfig,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): CashFlowTrendData

    suspend fun computeCashFlowProgress(
        config: CashFlowProgressCardConfig,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): CashFlowProgressData

    suspend fun computeBalanceForecast(
        config: BalanceForecastCardConfig,
        allTransactions: List<Transaction>,
        accounts: List<Account>,
        context: ComputationContext
    ): BalanceForecastData

    suspend fun computePlannedPayments(
        config: PlannedPaymentsCardConfig,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): PlannedPaymentsData

    suspend fun computeIncomeExpensesBook(
        config: IncomeExpensesBookCardConfig,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): IncomeExpensesBookData

    suspend fun computeCashFlowTable(
        config: CashFlowTableCardConfig,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): CashFlowTableData

    suspend fun computePlannedPaymentsTimeline(
        config: PlannedPaymentsTimelineCardConfig,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): PlannedPaymentsTimelineData

    suspend fun computeBalanceByAccounts(
        config: BalanceByAccountsCardConfig,
        allTransactions: List<Transaction>,
        accounts: List<Account>,
        context: ComputationContext
    ): BalanceByAccountsData

    suspend fun computeBalanceTrend(
        config: BalanceTrendCardConfig,
        allTransactions: List<Transaction>,
        accounts: List<Account>,
        context: ComputationContext
    ): BalanceTrendData
}

/**
 * Implementation of CardComputationService using TransactionsUtilityService.
 */
class CardComputationServiceImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CardComputationService {

    override suspend fun computeCashFlow(
        config: CashFlowCardConfig,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): CashFlowResult {
        val filtered = TransactionsUtilityService.filter(allTransactions, config.filter)
        val breakdown = TransactionsUtilityService.calculateIncomeExpenseBreakdown(
            transactions = filtered,
            exchangeRates = context.exchangeRates,
            baseCurrency = context.baseCurrency
        )
        
        val cashFlowBreakdown = (breakdown.incomeCurrencyBreakdown.keys + breakdown.expenseCurrencyBreakdown.keys)
            .distinct()
            .sorted()
            .map { curr ->
                val inc = breakdown.incomeCurrencyBreakdown[curr] ?: FinancialSummary.EMPTY
                val exp = breakdown.expenseCurrencyBreakdown[curr] ?: FinancialSummary.EMPTY
                
                CashFlowBreakdownItem(
                    currency = curr,
                    income = inc.totalAmount,
                    incomeInBase = TransactionsUtilityService.convertToBaseCurrency(inc.totalAmount, curr, context.baseCurrency, context.exchangeRates),
                    expense = exp.totalAmount,
                    expenseInBase = TransactionsUtilityService.convertToBaseCurrency(exp.totalAmount, curr, context.baseCurrency, context.exchangeRates)
                )
            }

        return CashFlowResult(
            totalIncomeInBase = breakdown.totalIncome.setScale(context.precision, RoundingMode.HALF_UP),
            totalExpensesInBase = breakdown.totalExpense.setScale(context.precision, RoundingMode.HALF_UP),
            netInBase = breakdown.netFlow.setScale(context.precision, RoundingMode.HALF_UP),
            breakdown = cashFlowBreakdown,
            filteredTransactions = filtered
        )
    }

    override suspend fun computeCurrencyTotals(
        config: CurrencyBalancesCardConfig,
        accounts: List<Account>,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): List<CurrencyAggregationItem> {
        val filteredTx = TransactionsUtilityService.filter(allTransactions, config.filter)
        val currencySummaries = TransactionsUtilityService.calculateSummaryByCurrency(filteredTx)
        
        return currencySummaries.map { (curr, summary) ->
            CurrencyAggregationItem(
                currency = curr,
                amount = summary.totalAmount,
                amountInBase = TransactionsUtilityService.convertToBaseCurrency(summary.totalAmount, curr, context.baseCurrency, context.exchangeRates)
            )
        }.sortedByDescending { it.amountInBase.abs() }
    }

    override suspend fun computeNetWorth(
        config: NetWorthCardConfig,
        accounts: List<Account>,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): Pair<BigDecimal, CashFlowResult> {
        val filter = TransactionFilter(
            timelineRange = config.timelineRange,
            accountIds = config.selectedAccountIds ?: emptyList()
        )
        val filteredTx = TransactionsUtilityService.filter(allTransactions, filter)
        val filteredAccounts = if (config.selectedAccountIds != null) {
            accounts.filter { it.id in config.selectedAccountIds }
        } else {
            accounts
        }

        val accountBreakdowns = TransactionsUtilityService.breakdownByAccount(
            transactions = filteredTx,
            accounts = filteredAccounts,
            exchangeRates = context.exchangeRates,
            baseCurrency = context.baseCurrency
        )
        
        val totalNetWorth = accountBreakdowns.sumOf { it.totalAmount }
        val cashFlow = computeCashFlow(CashFlowCardConfig(cardId = config.cardId, filter = filter), allTransactions, context)

        return Pair(totalNetWorth, cashFlow)
    }

    override suspend fun computeAccountStats(
        config: AccountsCardConfig,
        accounts: List<Account>,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): Pair<List<Account>, Map<String, AccountSummaryItem>> {
        val filter = TransactionFilter(timelineRange = config.timelineRange)
        val filteredTx = TransactionsUtilityService.filter(allTransactions, filter)
        
        val accountBreakdowns = TransactionsUtilityService.breakdownByAccount(
            transactions = filteredTx,
            accounts = accounts,
            exchangeRates = context.exchangeRates,
            baseCurrency = context.baseCurrency
        )
        
        val accountStats = accountBreakdowns.associate { breakdown ->
            val accountId = breakdown.key.accountId
            val account = accounts.find { it.id == accountId }!!
            val accountCurrencySummary = breakdown.currencyBreakdown[account.currencyCode] ?: FinancialSummary.EMPTY
            
            accountId to AccountSummaryItem(
                account = account,
                currencyBalances = breakdown.currencyBreakdown.mapValues { it.value.totalAmount },
                balanceInBase = breakdown.totalAmount,
                incomeInAccountCurrency = accountCurrencySummary.income,
                expenseInAccountCurrency = accountCurrencySummary.expense
            )
        }
        
        val updatedAccounts = accounts.map { account ->
            val stats = accountStats[account.id]
            if (stats != null) {
                val breakdown = accountBreakdowns.find { it.key.accountId == account.id }
                if (breakdown != null) {
                    val totalInAccountCurrency = breakdown.currencyBreakdown.map { (curr, summary) ->
                        TransactionsUtilityService.convertToBaseCurrency(summary.totalAmount, curr, account.currencyCode, context.exchangeRates)
                    }.fold(BigDecimal.ZERO) { acc, amount -> acc.add(amount) }
                    
                    account.copy(balance = totalInAccountCurrency)
                } else account
            } else account
        }

        return Pair(updatedAccounts, accountStats)
    }

    override suspend fun computeCategoryBreakdown(
        config: CategoryBreakdownCardConfig,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): List<CategoryBreakdownData> {
        val filtered = TransactionsUtilityService.filter(allTransactions, config.filter)
        val allCategories = categoryDao.getAllCategoriesSync()
        val categoryMap = allCategories.associateBy { it.name }
        
        val categoryBreakdowns = TransactionsUtilityService.breakdownByCategory(
            transactions = filtered,
            exchangeRates = context.exchangeRates,
            baseCurrency = context.baseCurrency
        ).associateBy { it.key }

        val globalTotal = categoryBreakdowns.values.sumOf { it.totalAmount }

        fun buildTree(categoryName: String?, parentColor: Color? = null, index: Int = 0, totalSiblings: Int = 1): CategoryBreakdownData? {
            val entity = categoryMap[categoryName] ?: return null
            val breakdown = categoryBreakdowns[categoryName] ?: Breakdown(categoryName ?: "", FinancialSummary.EMPTY, emptyMap())
            
            val baseColor = if (parentColor == null) Color(entity.colorHex) else {
                val factor = 0.7f + (index.toFloat() / totalSiblings.coerceAtLeast(1)) * 0.4f
                parentColor.copy(
                    red = (parentColor.red * factor).coerceIn(0f, 1f),
                    green = (parentColor.green * factor).coerceIn(0f, 1f),
                    blue = (parentColor.blue * factor).coerceIn(0f, 1f)
                )
            }

            val childrenEntities = allCategories.filter { it.parentId == entity.id }
            val childrenData = childrenEntities.mapIndexedNotNull { i, child ->
                buildTree(child.name, baseColor, i, childrenEntities.size)
            }.sortedByDescending { it.netBalance.abs() }

            val totalIncome = breakdown.income + childrenData.sumOf { it.income }
            val totalExpense = breakdown.expense + childrenData.sumOf { it.expense }
            val totalNet = totalIncome + totalExpense

            return CategoryBreakdownData(
                category = entity.name,
                categoryId = entity.id,
                netBalance = totalNet,
                income = totalIncome,
                expense = totalExpense,
                colorHex = baseColor.toArgb(),
                percentage = if (globalTotal.signum() != 0) (totalNet.abs().divide(globalTotal.abs(), 4, RoundingMode.HALF_UP)).toFloat() else 0f,
                hasSubcategories = childrenData.isNotEmpty(),
                subcategoryData = childrenData
            )
        }

        val rootCategories = allCategories.filter { it.parentId == null }
        return rootCategories.mapNotNull { root ->
            buildTree(root.name)
        }.sortedByDescending { it.netBalance.abs() }
    }

    override suspend fun computeTimelineTrajectory(
        config: TimelineTrajectoryCardConfig,
        allTransactions: List<Transaction>,
        accounts: List<Account>,
        context: ComputationContext
    ): List<TimelineTrajectoryData> {
        val filtered = TransactionsUtilityService.filter(allTransactions, config.filter)
        val timeBuckets = getTimeBuckets(config.filter.timelineRange)

        val bucketed = TransactionsUtilityService.createBreakdown(
            transactions = filtered,
            selector = { tx -> timeBuckets.find { it.start <= tx.timestamp && tx.timestamp < it.end } ?: TimeBucket(0,0) },
            mapper = { bucket, summaries ->
                val baseSummary = TransactionsUtilityService.calculateTotalInBaseCurrency(summaries, context.baseCurrency, context.exchangeRates)
                Breakdown(bucket, baseSummary, summaries)
            },
            exchangeRates = context.exchangeRates,
            baseCurrency = context.baseCurrency
        ).filter { it.key.start != 0L }.sortedBy { it.key.start }

        val (startTime, _) = TimelineService.getDateRange(config.filter.timelineRange)
        val priorTransactions = allTransactions.filter { tx ->
            (config.filter.accountIds.isEmpty() || tx.accountId in config.filter.accountIds) && 
            tx.timestamp < (startTime ?: 0L)
        }
        
        val priorBalance = TransactionsUtilityService.breakdownByAccount(
            priorTransactions, accounts, context.exchangeRates, context.baseCurrency
        ).sumOf { it.totalAmount }

        val trajectory = TransactionsUtilityService.calculateCumulativeSum(bucketed, priorBalance)
        
        return trajectory.map { point ->
            TimelineTrajectoryData(
                timestamp = point.key.start,
                balance = point.cumulativeBalance,
                expense = point.expense,
                income = point.income
            )
        }
    }

    override suspend fun computeCategoryExpenseTreemap(
        config: CategoryExpenseTreemapCardConfig,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): List<CategoryExpenseTreemapData> {
        val filtered = TransactionsUtilityService.filter(allTransactions, config.filter)
        val expenseTransactions = filtered.filter { it.amount.signum() < 0 }

        val allCategories = categoryDao.getAllCategoriesSync()
        val categoryMap = allCategories.associateBy { it.name }
        
        val categoryBreakdowns = TransactionsUtilityService.breakdownByCategory(
            transactions = expenseTransactions,
            exchangeRates = context.exchangeRates,
            baseCurrency = context.baseCurrency
        ).associateBy { it.key }

        val totalExpenseGlobal = categoryBreakdowns.values.sumOf { it.expense }.abs()

        fun buildTree(categoryName: String?, parentColor: Color? = null, index: Int = 0, totalSiblings: Int = 1): CategoryExpenseTreemapData? {
            val entity = categoryMap[categoryName] ?: return null
            val breakdown = categoryBreakdowns[categoryName] ?: Breakdown(categoryName ?: "", FinancialSummary.EMPTY, emptyMap())
            
            val baseColor = if (parentColor == null) Color(entity.colorHex) else {
                val factor = 0.7f + (index.toFloat() / totalSiblings.coerceAtLeast(1)) * 0.4f
                parentColor.copy(
                    red = (parentColor.red * factor).coerceIn(0f, 1f),
                    green = (parentColor.green * factor).coerceIn(0f, 1f),
                    blue = (parentColor.blue * factor).coerceIn(0f, 1f)
                )
            }

            val childrenEntities = allCategories.filter { it.parentId == entity.id }
            val childrenData = childrenEntities.mapIndexedNotNull { i, child ->
                buildTree(child.name, baseColor, i, childrenEntities.size)
            }.sortedByDescending { it.expense.abs() }

            val totalExpense = breakdown.expense + childrenData.sumOf { it.expense }

            return CategoryExpenseTreemapData(
                category = entity.name,
                categoryId = entity.id,
                expense = totalExpense,
                colorHex = baseColor.toArgb(),
                percentage = if (totalExpenseGlobal.signum() != 0) (totalExpense.abs().divide(totalExpenseGlobal, 4, RoundingMode.HALF_UP)).toFloat() else 0f,
                hasSubcategories = childrenData.isNotEmpty(),
                subcategoryData = childrenData
            )
        }

        val rootCategories = allCategories.filter { it.parentId == null }
        return rootCategories.mapNotNull { root ->
            buildTree(root.name)
        }.sortedByDescending { it.expense.abs() }
    }

    override suspend fun computeIncomeExpensesSummary(
        config: IncomeExpensesSummaryCardConfig,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): IncomeExpensesSummaryData {
        val currentTx = TransactionsUtilityService.filter(allTransactions, config.filter)
        val currentBreakdown = TransactionsUtilityService.calculateIncomeExpenseBreakdown(currentTx, context.exchangeRates, context.baseCurrency)
        
        val prevFilter = FilterService.getPreviousPeriodFilter(config.filter)
        val prevTx = TransactionsUtilityService.filter(allTransactions, prevFilter)
        val prevBreakdown = TransactionsUtilityService.calculateIncomeExpenseBreakdown(prevTx, context.exchangeRates, context.baseCurrency)
        
        val currentSummary = FinancialSummary(currentBreakdown.netFlow, currentBreakdown.totalIncome, currentBreakdown.totalExpense, currentTx.size)
        val prevSummary = FinancialSummary(prevBreakdown.netFlow, prevBreakdown.totalIncome, prevBreakdown.totalExpense, prevTx.size)
        
        val delta = TransactionsUtilityService.calculateSummaryDelta(currentSummary, prevSummary)
        val deltaPercentage = String.format(Locale.getDefault(), "%+1.1f%%", delta.percentageChange)
        
        val totalIncome = currentBreakdown.totalIncome
        val totalExpenses = currentBreakdown.totalExpense
        val totalAmount = currentBreakdown.netFlow

        val savingsRate = if (totalIncome.signum() > 0) {
            totalAmount.divide(totalIncome, 4, RoundingMode.HALF_UP).toFloat()
        } else 0f

        val categoryIcons = mapOf(
            "Food & Drinks" to Icons.Default.Restaurant,
            "Shopping" to Icons.Default.ShoppingCart,
            "Housing" to Icons.Default.Home,
            "Transportation" to Icons.Default.DirectionsCar,
            "Salary" to Icons.Default.AttachMoney,
            "Investments" to Icons.Default.TrendingUp,
            "Utilities" to Icons.Default.ElectricBolt,
            "Entertainment" to Icons.Default.Movie,
            "Health" to Icons.Default.LocalHospital,
            "Education" to Icons.Default.School
        )
        
        val categoryColors = listOf(
            Color(0xFFFF5722), Color(0xFF2196F3), Color(0xFFFF9800), Color(0xFF607D8B),
            Color(0xFF9C27B0), Color(0xFF4CAF50), Color(0xFFE91E63), Color(0xFF9C27B0),
            Color(0xFF00BCD4), Color(0xFFFFC107)
        )
        
        val expenseCategoryBreakdowns = TransactionsUtilityService.breakdownByCategory(currentTx, context.exchangeRates, context.baseCurrency)
            .filter { it.expense.signum() != 0 }
            .sortedByDescending { it.expense.abs() }
            .take(4)
            
        val categories = expenseCategoryBreakdowns.mapIndexed { index, b ->
            val prevCatTx = prevTx.filter { it.category == b.key }
            val prevCatSummary = TransactionsUtilityService.calculateTransactionSummary(prevCatTx)
            val prevCatBase = TransactionsUtilityService.convertFinancialSummary(prevCatSummary, b.key, context.baseCurrency, context.exchangeRates)
            val catDelta = TransactionsUtilityService.calculateSummaryDelta(b.financialSummary, prevCatBase)
            
            CategoryItem(
                name = b.key,
                amount = b.expense,
                change = String.format(Locale.getDefault(), "%+1.1f%%", catDelta.percentageChange),
                color = categoryColors.getOrElse(index) { Color(0xFF9C27B0) },
                icon = categoryIcons[b.key] ?: Icons.Default.Label
            )
        }
        
        return IncomeExpensesSummaryData(
            totalAmount = totalAmount,
            percentageChange = deltaPercentage,
            currency = context.baseCurrency,
            precision = context.precision,
            categories = categories,
            savingsRate = savingsRate,
            incomeTotal = totalIncome,
            expenseTotal = totalExpenses
        )
    }

    override suspend fun computePeriodComparison(
        config: PeriodComparisonCardConfig,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): PeriodComparisonData {
        val filtered = TransactionsUtilityService.filter(allTransactions, config.filter)
        val currentBreakdown = TransactionsUtilityService.calculateIncomeExpenseBreakdown(filtered, context.exchangeRates, context.baseCurrency)
        
        val prevFilter = FilterService.getPreviousPeriodFilter(config.filter)
        val prevTx = TransactionsUtilityService.filter(allTransactions, prevFilter)
        val prevBreakdown = TransactionsUtilityService.calculateIncomeExpenseBreakdown(prevTx, context.exchangeRates, context.baseCurrency)

        val currentValue = when (config.selectedTab) {
            "Income" -> currentBreakdown.totalIncome
            "Expenses" -> currentBreakdown.totalExpense.abs()
            else -> currentBreakdown.netFlow
        }

        val prevValue = when (config.selectedTab) {
            "Income" -> prevBreakdown.totalIncome
            "Expenses" -> prevBreakdown.totalExpense.abs()
            else -> prevBreakdown.netFlow
        }

        val deltaPercentage = if (prevValue.signum() != 0) {
            ((currentValue - prevValue).divide(prevValue.abs(), 4, RoundingMode.HALF_UP)).multiply(BigDecimal("100")).toFloat()
        } else if (currentValue.signum() != 0) 100f else 0f

        val timeBuckets = getTimeBuckets(config.filter.timelineRange)
        
        fun getChartDataFor(txs: List<Transaction>, buckets: List<TimeBucket>): List<Double> {
            return TransactionsUtilityService.createBreakdown(
                transactions = txs,
                selector = { tx -> buckets.find { it.start <= tx.timestamp && tx.timestamp < it.end } ?: TimeBucket(0,0) },
                mapper = { bucket, summaries ->
                    val summary = TransactionsUtilityService.calculateTotalInBaseCurrency(summaries, context.baseCurrency, context.exchangeRates)
                    val value = when (config.selectedTab) {
                        "Income" -> summary.income.toDouble()
                        "Expenses" -> summary.expense.abs().toDouble()
                        else -> summary.totalAmount.toDouble()
                    }
                    bucket to value
                },
                exchangeRates = context.exchangeRates,
                baseCurrency = context.baseCurrency
            ).filter { it.first.start != 0L }.sortedBy { it.first.start }.map { it.second }
        }

        val chartData = getChartDataFor(filtered, timeBuckets)
        
        // Compute ghost data (previous period)
        val prevTimeBuckets = getTimeBuckets(prevFilter.timelineRange)
        val ghostData = getChartDataFor(prevTx, prevTimeBuckets)

        return PeriodComparisonData(
            currentValue = currentValue,
            currency = context.baseCurrency,
            precision = context.precision,
            chartData = chartData,
            ghostData = ghostData,
            runRate = prevValue,
            deltaPercentage = deltaPercentage
        )
    }

    override suspend fun computeCashFlowTrend(
        config: CashFlowTrendCardConfig,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): CashFlowTrendData {
        val filtered = TransactionsUtilityService.filter(allTransactions, config.filter)
        val currentBreakdown = TransactionsUtilityService.calculateIncomeExpenseBreakdown(filtered, context.exchangeRates, context.baseCurrency)
        
        val prevFilter = FilterService.getPreviousPeriodFilter(config.filter)
        val prevTx = TransactionsUtilityService.filter(allTransactions, prevFilter)
        val prevBreakdown = TransactionsUtilityService.calculateIncomeExpenseBreakdown(prevTx, context.exchangeRates, context.baseCurrency)
        
        val delta = TransactionsUtilityService.calculateSummaryDelta(
            current = FinancialSummary(currentBreakdown.netFlow, currentBreakdown.totalIncome, currentBreakdown.totalExpense, filtered.size),
            previous = FinancialSummary(prevBreakdown.netFlow, prevBreakdown.totalIncome, prevBreakdown.totalExpense, prevTx.size)
        )
        
        val timeBuckets = getTimeBuckets(config.filter.timelineRange)
        
        val bucketed = TransactionsUtilityService.createBreakdown(
            transactions = filtered,
            selector = { tx -> timeBuckets.find { it.start <= tx.timestamp && tx.timestamp < it.end } ?: TimeBucket(0,0) },
            mapper = { bucket, summaries ->
                val baseSummary = TransactionsUtilityService.calculateTotalInBaseCurrency(summaries, context.baseCurrency, context.exchangeRates)
                Breakdown(bucket, baseSummary, summaries)
            },
            exchangeRates = context.exchangeRates,
            baseCurrency = context.baseCurrency
        ).filter { it.key.start != 0L }.sortedBy { it.key.start }

        val dailyData = bucketed.map { it.totalAmount.toDouble() }
        val trajectory = TransactionsUtilityService.calculateCumulativeSum(bucketed, BigDecimal.ZERO)
        val lineData = trajectory.map { it.cumulativeBalance.toDouble() }

        val mean = if (dailyData.isNotEmpty()) dailyData.average() else 0.0
        val variance = if (dailyData.isNotEmpty()) dailyData.map { (it - mean) * (it - mean) }.average() else 0.0
        val volatility = Math.sqrt(variance).toBigDecimal().setScale(context.precision, RoundingMode.HALF_UP)

        return CashFlowTrendData(
            barData = dailyData,
            lineData = lineData,
            smaData = null,
            volatility = volatility,
            percentageChange = String.format(Locale.getDefault(), "%+1.1f%%", delta.percentageChange)
        )
    }

    override suspend fun computeCashFlowProgress(
        config: CashFlowProgressCardConfig,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): CashFlowProgressData {
        val filtered = TransactionsUtilityService.filter(allTransactions, config.filter)
        val breakdown = TransactionsUtilityService.calculateIncomeExpenseBreakdown(filtered, context.exchangeRates, context.baseCurrency)
        
        return CashFlowProgressData(
            income = breakdown.totalIncome,
            expenses = breakdown.totalExpense,
            currency = context.baseCurrency,
            precision = context.precision
        )
    }

    override suspend fun computeBalanceForecast(
        config: BalanceForecastCardConfig,
        allTransactions: List<Transaction>,
        accounts: List<Account>,
        context: ComputationContext
    ): BalanceForecastData {
        val filtered = TransactionsUtilityService.filter(allTransactions, config.filter)
        val now = System.currentTimeMillis()
        
        val currentBalance = TransactionsUtilityService.breakdownByAccount(
            allTransactions, accounts, context.exchangeRates, context.baseCurrency
        ).sumOf { it.totalAmount }
        
        val totalNetFlow = filtered.sumOf { tx ->
            TransactionsUtilityService.convertToBaseCurrency(tx.amount, tx.currency, context.baseCurrency, context.exchangeRates)
        }
        
        val daysInPeriod = 30L // simplified
        val avgDailyNetFlow = totalNetFlow.divide(BigDecimal.valueOf(daysInPeriod), context.precision, RoundingMode.HALF_UP)

        val forecastBalance = currentBalance.add(avgDailyNetFlow.multiply(BigDecimal.valueOf(config.forecastDays.toLong())))
        
        return BalanceForecastData(
            forecastBalance = forecastBalance,
            currency = context.baseCurrency,
            precision = context.precision,
            chartData = emptyList(),
            projectedZeroDate = null,
            currentLiquidCash = currentBalance
        )
    }

    override suspend fun computePlannedPayments(
        config: PlannedPaymentsCardConfig,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): PlannedPaymentsData {
        val filtered = TransactionsUtilityService.filter(allTransactions, config.filter)
        val expenseTx = filtered.filter { it.amount.signum() < 0 }
        val categoryBreakdowns = TransactionsUtilityService.breakdownByCategory(expenseTx, context.exchangeRates, context.baseCurrency)
        
        val total = categoryBreakdowns.sumOf { it.expense.abs() }
        
        val categoryColors = listOf(Color(0xFF9C27B0), Color(0xFF4CAF50), Color(0xFFFF9800))
        
        val payments = categoryBreakdowns.mapIndexed { index, b ->
            PaymentItem(
                name = b.key,
                amount = b.expense.abs(),
                progress = if (total.signum() > 0) (b.expense.abs().divide(total, 2, RoundingMode.HALF_UP).toFloat()) else 0f,
                color = categoryColors.getOrElse(index % categoryColors.size) { Color(0xFF9C27B0) }
            )
        }
        
        return PlannedPaymentsData(
            total = total,
            currency = context.baseCurrency,
            precision = context.precision,
            payments = payments
        )
    }

    override suspend fun computeIncomeExpensesBook(
        config: IncomeExpensesBookCardConfig,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): IncomeExpensesBookData {
        val filtered = TransactionsUtilityService.filter(allTransactions, config.filter)
        val breakdown = TransactionsUtilityService.calculateIncomeExpenseBreakdown(filtered, context.exchangeRates, context.baseCurrency)
        
        return IncomeExpensesBookData(
            incomeTotal = breakdown.totalIncome,
            expenseTotal = breakdown.totalExpense,
            incomeCount = breakdown.incomeTransactions.size,
            expenseCount = breakdown.expenseTransactions.size,
            currency = context.baseCurrency,
            precision = context.precision
        )
    }

    override suspend fun computeCashFlowTable(
        config: CashFlowTableCardConfig,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): CashFlowTableData {
        val filtered = TransactionsUtilityService.filter(allTransactions, config.filter)
        val breakdown = TransactionsUtilityService.calculateIncomeExpenseBreakdown(filtered, context.exchangeRates, context.baseCurrency)
        
        val incomeCount = breakdown.incomeTransactions.size
        val expenseCount = breakdown.expenseTransactions.size
        
        return CashFlowTableData(
            incomeCount = incomeCount,
            expenseCount = expenseCount,
            incomeAvgDay = breakdown.totalIncome.divide(BigDecimal.valueOf(30), context.precision, RoundingMode.HALF_UP),
            expenseAvgDay = breakdown.totalExpense.divide(BigDecimal.valueOf(30), context.precision, RoundingMode.HALF_UP),
            incomeAvgRecord = if (incomeCount > 0) breakdown.totalIncome.divide(BigDecimal.valueOf(incomeCount.toLong()), context.precision, RoundingMode.HALF_UP) else BigDecimal.ZERO,
            expenseAvgRecord = if (expenseCount > 0) breakdown.totalExpense.divide(BigDecimal.valueOf(expenseCount.toLong()), context.precision, RoundingMode.HALF_UP) else BigDecimal.ZERO,
            incomeTotal = breakdown.totalIncome,
            expenseTotal = breakdown.totalExpense,
            netCashFlow = breakdown.netFlow,
            currency = context.baseCurrency,
            precision = context.precision
        )
    }

    override suspend fun computePlannedPaymentsTimeline(
        config: PlannedPaymentsTimelineCardConfig,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): PlannedPaymentsTimelineData {
        val filtered = TransactionsUtilityService.filter(allTransactions, config.filter)
        val expenseTransactions = filtered.filter { it.amount.signum() < 0 }
        
        val weeklyBreakdowns = TransactionsUtilityService.createBreakdown(
            transactions = expenseTransactions,
            selector = { tx -> 
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                "Week ${cal.get(java.util.Calendar.WEEK_OF_YEAR)} (${cal.get(java.util.Calendar.YEAR)})"
            },
            mapper = { week, summaries ->
                val baseSummary = TransactionsUtilityService.calculateTotalInBaseCurrency(summaries, context.baseCurrency, context.exchangeRates)
                week to baseSummary.expense.abs()
            },
            exchangeRates = context.exchangeRates,
            baseCurrency = context.baseCurrency
        )
        
        val total = weeklyBreakdowns.sumOf { it.second }
        
        return PlannedPaymentsTimelineData(
            total = total,
            currency = context.baseCurrency,
            precision = context.precision,
            weeklyData = weeklyBreakdowns
        )
    }

    override suspend fun computeBalanceByAccounts(
        config: BalanceByAccountsCardConfig,
        allTransactions: List<Transaction>,
        accounts: List<Account>,
        context: ComputationContext
    ): BalanceByAccountsData {
        val filtered = TransactionsUtilityService.filter(allTransactions, config.filter)
        val breakdowns = TransactionsUtilityService.breakdownByAccount(filtered, accounts, context.exchangeRates, context.baseCurrency)
        
        val accountBalances = breakdowns.map { b ->
            AccountBalanceItem(
                accountName = b.key.accountName,
                balance = b.totalAmount,
                currency = context.baseCurrency,
                isNegative = b.totalAmount < BigDecimal.ZERO
            )
        }
        
        return BalanceByAccountsData(
            totalBalance = accountBalances.sumOf { it.balance },
            currency = context.baseCurrency,
            precision = context.precision,
            accountBalances = accountBalances
        )
    }

    override suspend fun computeBalanceTrend(
        config: BalanceTrendCardConfig,
        allTransactions: List<Transaction>,
        accounts: List<Account>,
        context: ComputationContext
    ): BalanceTrendData {
        val filtered = TransactionsUtilityService.filter(allTransactions, config.filter)
        val monthlyBreakdowns = TransactionsUtilityService.breakdownByMonth(filtered, context.exchangeRates, context.baseCurrency)
            .sortedBy { it.key }
            
        val trajectory = TransactionsUtilityService.calculateCumulativeSum(monthlyBreakdowns, BigDecimal.ZERO)
        val weeklyBalances = trajectory.map { it.cumulativeBalance.toDouble() }
        
        val currentBalance = trajectory.lastOrNull()?.cumulativeBalance ?: BigDecimal.ZERO
        
        return BalanceTrendData(
            currentBalance = currentBalance,
            percentageChange = "0%", // simplified
            currency = context.baseCurrency,
            precision = context.precision,
            weeklyBalances = weeklyBalances
        )
    }

    private fun getTimeBuckets(timelineRange: TimelineRange): List<TimeBucket> {
        val (start, end) = TimelineService.getDateRange(timelineRange)
        val actualStart = start ?: 0L
        val actualEnd = end ?: System.currentTimeMillis()
        
        val numPoints = 30
        val duration = actualEnd - actualStart
        val step = if (numPoints > 0) duration / numPoints else 0L
        
        return if (step > 0) {
            (0 until numPoints).map { i ->
                TimeBucket(actualStart + (i * step), actualStart + ((i + 1) * step))
            }
        } else listOf(TimeBucket(actualStart, actualEnd))
    }

    private data class TimeBucket(val start: Long, val end: Long)
}
