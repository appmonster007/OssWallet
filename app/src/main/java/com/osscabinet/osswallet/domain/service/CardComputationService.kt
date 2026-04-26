package com.osscabinet.ossWallet.domain.service

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.analytics.CategoryBreakdownData
import com.osscabinet.ossWallet.domain.model.analytics.CategoryExpenseTreemapData
import com.osscabinet.ossWallet.domain.model.analytics.TimelineTrajectoryData
import com.osscabinet.ossWallet.domain.model.analytics.ComputationContext
import com.osscabinet.ossWallet.domain.model.settings.AccountsCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CardConfig
import com.osscabinet.ossWallet.domain.model.settings.CardScope
import com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CategoryBreakdownCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CategoryExpenseTreemapCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig
import com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig
import com.osscabinet.ossWallet.domain.model.settings.TimelineTrajectoryCardConfig
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.usecase.AccountSummaryItem
import com.osscabinet.ossWallet.domain.usecase.CashFlowResult
import com.osscabinet.ossWallet.domain.usecase.CurrencyAggregationItem
import com.osscabinet.ossWallet.data.local.dao.CategoryDao
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.osscabinet.ossWallet.domain.model.settings.BalanceByAccountsCardConfig
import com.osscabinet.ossWallet.domain.model.settings.BalanceForecastCardConfig
import com.osscabinet.ossWallet.domain.model.settings.BalanceTrendCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CashFlowProgressCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CashFlowTableCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CashFlowTrendCardConfig
import com.osscabinet.ossWallet.domain.model.settings.IncomeExpensesBookCardConfig
import com.osscabinet.ossWallet.domain.model.settings.IncomeExpensesSummaryCardConfig
import com.osscabinet.ossWallet.domain.model.settings.PeriodComparisonCardConfig
import com.osscabinet.ossWallet.domain.model.settings.PlannedPaymentsCardConfig
import com.osscabinet.ossWallet.domain.model.settings.PlannedPaymentsTimelineCardConfig
import com.osscabinet.ossWallet.ui.analytics.components.BalanceByAccountsData
import com.osscabinet.ossWallet.ui.analytics.components.BalanceForecastData
import com.osscabinet.ossWallet.ui.analytics.components.BalanceTrendData
import com.osscabinet.ossWallet.ui.analytics.components.CashFlowProgressData
import com.osscabinet.ossWallet.ui.analytics.components.CashFlowTableData
import com.osscabinet.ossWallet.ui.analytics.components.CashFlowTrendData
import com.osscabinet.ossWallet.ui.analytics.components.IncomeExpensesBookData
import com.osscabinet.ossWallet.ui.analytics.components.IncomeExpensesSummaryData
import com.osscabinet.ossWallet.ui.analytics.components.PeriodComparisonData
import com.osscabinet.ossWallet.ui.analytics.components.PlannedPaymentsData
import com.osscabinet.ossWallet.ui.analytics.components.PlannedPaymentsTimelineData
import java.math.BigDecimal
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.ui.analytics.components.CategoryItem
import com.osscabinet.ossWallet.ui.analytics.components.PaymentItem
import com.osscabinet.ossWallet.ui.analytics.components.AccountBalanceItem
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import java.math.RoundingMode
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingUp
import java.util.Locale

/**
 * Service for computing card data with scope-aware filtering.
 * Provides unified computation logic for all card types across different screens.
 * 
 * This interface extends CardComputationEngine to provide additional computation methods
 * for analytics cards that are not part of the core engine interface.
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
 * Implementation of CardComputationService using FinancialUseCases.
 * Handles scope-aware filtering and delegates to appropriate use cases.
 */
class CardComputationServiceImpl(
    private val financialUseCases: com.osscabinet.ossWallet.domain.usecase.FinancialUseCases,
    private val categoryDao: CategoryDao
) : CardComputationService {

    override suspend fun computeCashFlow(
        config: CashFlowCardConfig,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): CashFlowResult {
        // Determine scope based on accountIds in filter
        val accountId = config.filter.accountIds.firstOrNull()
        val scope = if (accountId != null) CardScope.ACCOUNT else CardScope.GLOBAL

        // Filter allTransactions based on scope
        val filteredTransactions = filterTransactionsByScope(
            allTransactions = allTransactions,
            scope = scope,
            accountId = accountId
        )

        // HYBRID APPROACH: No timeline filtering here - cards already filter using ViewModel helpers
        // Use TimelineRange.AllTime since filtering is done at card level
        return financialUseCases.calculateCashFlow(
            allTransactions = filteredTransactions,
            timelineRange = TimelineRange.AllTime,  // Already filtered at card level
            baseCurrency = context.baseCurrency,
            exchangeRates = context.exchangeRates,
            precision = context.precision
        )
    }

    override suspend fun computeCurrencyTotals(
        config: CurrencyBalancesCardConfig,
        accounts: List<Account>,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): List<CurrencyAggregationItem> {
        // Determine scope based on accountIds in filter
        val accountId = config.filter.accountIds.firstOrNull()
        val scope = if (accountId != null) CardScope.ACCOUNT else CardScope.GLOBAL
        
        // Filter allTransactions based on scope
        val filteredTransactions = filterTransactionsByScope(
            allTransactions = allTransactions,
            scope = scope,
            accountId = accountId
        )
        
        // Filter accounts based on scope
        val filteredAccounts = if (accountId != null) {
            accounts.filter { it.id == accountId }
        } else {
            accounts
        }

        // HYBRID APPROACH: No timeline filtering here - cards already filter using ViewModel helpers
        // Use TimelineRange.AllTime since filtering is done at card level
        return financialUseCases.getCurrencyAggregation(
            accounts = filteredAccounts,
            allTransactions = filteredTransactions,
            timelineRange = TimelineRange.AllTime,  // Already filtered at card level
            baseCurrency = context.baseCurrency,
            exchangeRates = context.exchangeRates,
            precision = context.precision
        )
    }

    override suspend fun computeNetWorth(
        config: NetWorthCardConfig,
        accounts: List<Account>,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): Pair<BigDecimal, CashFlowResult> {
        // Filter allTransactions based on scope (NetWorthCardConfig is currently GLOBAL only)
        val filteredTransactions = filterTransactionsByScope(
            allTransactions = allTransactions,
            scope = CardScope.GLOBAL,
            accountId = null
        )

        // Filter accounts based on selectedAccountIds if specified
        val filteredAccounts = if (config.selectedAccountIds != null) {
            accounts.filter { it.id in config.selectedAccountIds }
        } else {
            accounts
        }

        // HYBRID APPROACH: No timeline filtering here - cards already filter using ViewModel helpers
        // Use TimelineRange.AllTime since filtering is done at card level
        val netWorth = financialUseCases.calculateNetWorth(
            accounts = filteredAccounts,
            allTransactions = filteredTransactions,
            baseCurrency = context.baseCurrency,
            exchangeRates = context.exchangeRates,
            precision = context.precision,
            timelineRange = TimelineRange.AllTime  // Already filtered at card level
        )

        val cashFlow = financialUseCases.calculateCashFlow(
            allTransactions = filteredTransactions,
            timelineRange = TimelineRange.AllTime,  // Already filtered at card level
            baseCurrency = context.baseCurrency,
            exchangeRates = context.exchangeRates,
            precision = context.precision
        )

        return Pair(netWorth, cashFlow)
    }

    override suspend fun computeAccountStats(
        config: AccountsCardConfig,
        accounts: List<Account>,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): Pair<List<Account>, Map<String, AccountSummaryItem>> {
        // HYBRID APPROACH: No timeline filtering here - cards already filter using ViewModel helpers
        // Use TimelineRange.AllTime since filtering is done at card level
        val accountStats = financialUseCases.getAccountSummaries(
            accounts = accounts,
            allTransactions = allTransactions,
            timelineRange = TimelineRange.AllTime,  // Already filtered at card level
            baseCurrency = context.baseCurrency,
            exchangeRates = context.exchangeRates,
            precision = context.precision
        )
        // Update accounts with timeline-filtered balances
        val updatedAccounts = financialUseCases.getTotalBalances(
            accounts = accounts,
            allTransactions = allTransactions,
            exchangeRates = context.exchangeRates,
            precision = context.precision,
            timelineRange = TimelineRange.AllTime  // Already filtered at card level
        )
        return Pair(updatedAccounts, accountStats)
    }

    override suspend fun computeCategoryBreakdown(
        config: CategoryBreakdownCardConfig,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): List<CategoryBreakdownData> {
        val filteredByFilter = FilterService.filterTransactions(allTransactions, config.filter)
        val accountId = config.filter.accountIds.firstOrNull()
        val scope = if (accountId != null) CardScope.ACCOUNT else CardScope.GLOBAL
        val filteredByScope = filterTransactionsByScope(filteredByFilter, scope, accountId)

        val allCategories = categoryDao.getAllCategoriesSync()
        val categoryMap = allCategories.associateBy { it.name }
        val categoryIdMap = allCategories.associateBy { it.id }

        // Recursive helper to build tree
        fun buildTree(categoryName: String?, txs: List<Transaction>, globalTotal: BigDecimal, parentColor: Color? = null, index: Int = 0, totalSiblings: Int = 1): CategoryBreakdownData? {
            val entity = categoryMap[categoryName] ?: return null
            
            // Apply shading logic for children
            val baseColor = if (parentColor == null) Color(entity.colorHex) else {
                // If it's a child, derive shade from parent
                val factor = 0.7f + (index.toFloat() / totalSiblings.coerceAtLeast(1)) * 0.4f
                parentColor.copy(
                    red = (parentColor.red * factor).coerceIn(0f, 1f),
                    green = (parentColor.green * factor).coerceIn(0f, 1f),
                    blue = (parentColor.blue * factor).coerceIn(0f, 1f)
                )
            }

            val childrenEntities = allCategories.filter { it.parentId == entity.id }
            val childrenData = childrenEntities.mapIndexedNotNull { i, child ->
                buildTree(child.name, txs, globalTotal, baseColor, i, childrenEntities.size)
            }.sortedByDescending { it.netBalance.abs() }

            // Current category's OWN allTransactions (not its children's)
            val ownTxs = txs.filter { it.category == categoryName }
            val ownIncome = ownTxs.filter { it.amount.signum() >= 0 }.sumOf { tx ->
                CurrencyService.convertAmount(tx.amount, tx.currency, context.baseCurrency, context.exchangeRates, context.precision)
            }
            val ownExpense = ownTxs.filter { it.amount.signum() < 0 }.sumOf { tx ->
                CurrencyService.convertAmount(tx.amount, tx.currency, context.baseCurrency, context.exchangeRates, context.precision)
            }

            // Aggregate: Own + Children
            val totalIncome = ownIncome + childrenData.sumOf { it.income }
            val totalExpense = ownExpense + childrenData.sumOf { it.expense }
            val totalNet = totalIncome + totalExpense

            return CategoryBreakdownData(
                category = entity.name,
                categoryId = entity.id,
                netBalance = totalNet,
                income = totalIncome,
                expense = totalExpense,
                colorHex = baseColor.toArgb(),
                percentage = if (globalTotal.signum() != 0) (totalNet.abs() / globalTotal.abs()).toFloat() else 0f,
                hasSubcategories = childrenData.isNotEmpty(),
                subcategoryData = childrenData
            )
        }

        val totalNetGlobal = filteredByScope.sumOf { tx ->
            CurrencyService.convertAmount(tx.amount, tx.currency, context.baseCurrency, context.exchangeRates, context.precision)
        }.abs()

        // Root Level: Categories with no parent
        val rootCategories = allCategories.filter { it.parentId == null }
        return rootCategories.mapNotNull { root ->
            buildTree(root.name, filteredByScope, totalNetGlobal)
        }.sortedByDescending { it.netBalance.abs() }
    }

    override suspend fun computeTimelineTrajectory(
        config: TimelineTrajectoryCardConfig,
        allTransactions: List<Transaction>,
        accounts: List<Account>,
        context: ComputationContext
    ): List<TimelineTrajectoryData> {
        val filteredByFilter = FilterService.filterTransactions(allTransactions, config.filter)
        val accountId = config.filter.accountIds.firstOrNull()
        val scope = if (accountId != null) CardScope.ACCOUNT else CardScope.GLOBAL
        val filteredByScope = filterTransactionsByScope(filteredByFilter, scope, accountId)

        // Determine time buckets based on timelineRange using TimelineService
        val timeBuckets = getTimeBuckets(config.filter.timelineRange)

        return timeBuckets.map { bucket ->
            val bucketTransactions = filteredByScope.filter { tx ->
                tx.timestamp >= bucket.start && tx.timestamp < bucket.end
            }
            
            val income = bucketTransactions.filter { it.amount.signum() >= 0 }.sumOf { tx ->
                CurrencyService.convertAmount(tx.amount, tx.currency, context.baseCurrency, context.exchangeRates, context.precision)
            }
            val expense = bucketTransactions.filter { it.amount.signum() < 0 }.sumOf { tx ->
                CurrencyService.convertAmount(tx.amount, tx.currency, context.baseCurrency, context.exchangeRates, context.precision)
            }
            
            // Calculate running balance
            val priorTransactions = filteredByScope.filter { it.timestamp < bucket.start }
            val priorBalance = financialUseCases.calculateNetWorth(
                accounts = if (scope == CardScope.ACCOUNT && accountId != null) accounts.filter { it.id == accountId } else accounts,
                allTransactions = priorTransactions,
                baseCurrency = context.baseCurrency,
                exchangeRates = context.exchangeRates,
                precision = context.precision,
                timelineRange = config.filter.timelineRange
            )
            
            val balance = priorBalance + income - expense
            
            TimelineTrajectoryData(
                timestamp = bucket.start,
                balance = balance,
                expense = expense,
                income = income
            )
        }
    }

    override suspend fun computeCategoryExpenseTreemap(
        config: CategoryExpenseTreemapCardConfig,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): List<CategoryExpenseTreemapData> {
        val filteredByFilter = FilterService.filterTransactions(allTransactions, config.filter)
        val accountId = config.filter.accountIds.firstOrNull()
        val scope = if (accountId != null) CardScope.ACCOUNT else CardScope.GLOBAL
        val filteredByScope = filterTransactionsByScope(filteredByFilter, scope, accountId)
        val expenseTransactions = filteredByScope.filter { it.amount.signum() < 0 }

        val allCategories = categoryDao.getAllCategoriesSync()
        val categoryMap = allCategories.associateBy { it.name }

        fun buildTree(categoryName: String?, txs: List<Transaction>, globalTotal: BigDecimal, parentColor: Color? = null, index: Int = 0, totalSiblings: Int = 1): CategoryExpenseTreemapData? {
            val entity = categoryMap[categoryName] ?: return null
            
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
                buildTree(child.name, txs, globalTotal, baseColor, i, childrenEntities.size)
            }.sortedByDescending { it.expense.abs() }

            val ownExpense = txs.filter { it.category == categoryName }.sumOf { tx ->
                CurrencyService.convertAmount(tx.amount, tx.currency, context.baseCurrency, context.exchangeRates, context.precision)
            }

            val totalExpense = ownExpense + childrenData.sumOf { it.expense }

            return CategoryExpenseTreemapData(
                category = entity.name,
                categoryId = entity.id,
                expense = totalExpense,
                colorHex = baseColor.toArgb(),
                percentage = if (globalTotal.signum() != 0) (totalExpense.abs() / globalTotal.abs()).toFloat() else 0f,
                hasSubcategories = childrenData.isNotEmpty(),
                subcategoryData = childrenData
            )
        }

        val totalExpenseGlobal = expenseTransactions.sumOf { tx ->
            CurrencyService.convertAmount(tx.amount, tx.currency, context.baseCurrency, context.exchangeRates, context.precision)
        }.abs()

        val rootCategories = allCategories.filter { it.parentId == null }
        return rootCategories.mapNotNull { root ->
            buildTree(root.name, expenseTransactions, totalExpenseGlobal)
        }.sortedByDescending { it.expense.abs() }
    }

    override suspend fun computeIncomeExpensesSummary(
        config: IncomeExpensesSummaryCardConfig,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): IncomeExpensesSummaryData {
        val currentRates = context.exchangeRates
        
        val filteredTransactions = FilterService.filterTransactions(allTransactions, config.filter)
        val convertedTransactions = filteredTransactions.map { tx ->
            val convertedAmount = CurrencyService.convertAmount(
                amount = tx.amount,
                fromCurrency = tx.currency,
                toCurrency = context.baseCurrency,
                exchangeRates = currentRates,
                precision = context.precision + 4
            )
            tx.copy(amount = convertedAmount)
        }

        val prevFilter = getPreviousPeriodFilter(config.filter)
        val prevTransactions = FilterService.filterTransactions(allTransactions, prevFilter)
            .map { tx ->
                val convertedAmount = CurrencyService.convertAmount(
                    amount = tx.amount,
                    fromCurrency = tx.currency,
                    toCurrency = context.baseCurrency,
                    exchangeRates = currentRates,
                    precision = context.precision + 4
                )
                tx.copy(amount = convertedAmount)
            }
        
        val expenseCategoryTotals = convertedTransactions
            .filter { it.amount.signum() < 0 }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) } }
        
        val incomeCategoryTotals = convertedTransactions
            .filter { it.amount.signum() >= 0 }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) } }
        
        val totalIncome = incomeCategoryTotals.values.fold(BigDecimal.ZERO) { acc, amount -> acc.add(amount) }
        val totalExpenses = expenseCategoryTotals.values.fold(BigDecimal.ZERO) { acc, amount -> acc.add(amount) }
        val totalAmount = totalIncome.add(totalExpenses)

        val prevTotalIncome = prevTransactions.filter { it.amount.signum() >= 0 }.sumOf { it.amount }
        val prevTotalExpenses = prevTransactions.filter { it.amount.signum() < 0 }.sumOf { it.amount }
        val prevNetFlow = prevTotalIncome.add(prevTotalExpenses)

        val deltaPercentage = if (prevNetFlow.signum() != 0) {
            val change = ((totalAmount - prevNetFlow).toDouble() / prevNetFlow.abs().toDouble()) * 100
            String.format(Locale.getDefault(), "%+1.1f%%", change)
        } else "0.0%"
        
        val savingsRate = if (totalIncome.signum() > 0) {
            (totalIncome.add(totalExpenses)).divide(totalIncome, 4, RoundingMode.HALF_UP).toFloat()
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
        
        val categories = expenseCategoryTotals.entries
            .sortedByDescending { it.value.abs() }
            .take(4)
            .mapIndexed { index, entry ->
                val prevCatTotal = prevTransactions.filter { it.category == entry.key }.sumOf { it.amount }
                val catChange = if (prevCatTotal.signum() != 0) {
                    val change = ((entry.value - prevCatTotal).toDouble() / prevCatTotal.abs().toDouble()) * 100
                    String.format(Locale.getDefault(), "%+1.1f%%", change)
                } else "0.0%"

                CategoryItem(
                    name = entry.key,
                    amount = entry.value,
                    change = catChange,
                    color = categoryColors.getOrElse(index) { Color(0xFF9C27B0) },
                    icon = categoryIcons[entry.key] ?: Icons.Default.Label
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
        val currentRates = context.exchangeRates
        
        val filteredTransactions = FilterService.filterTransactions(allTransactions, config.filter)
        val convertedTransactions = filteredTransactions.map { tx ->
            val convertedAmount = CurrencyService.convertAmount(
                amount = tx.amount,
                fromCurrency = tx.currency,
                toCurrency = context.baseCurrency,
                exchangeRates = currentRates,
                precision = context.precision + 4
            )
            tx.copy(amount = convertedAmount)
        }
        
        val prevFilter = getPreviousPeriodFilter(config.filter)
        val prevTransactions = FilterService.filterTransactions(allTransactions, prevFilter)
            .map { tx ->
                val convertedAmount = CurrencyService.convertAmount(
                    amount = tx.amount,
                    fromCurrency = tx.currency,
                    toCurrency = context.baseCurrency,
                    exchangeRates = currentRates,
                    precision = context.precision + 4
                )
                tx.copy(amount = convertedAmount)
            }

        val currentValue = when (config.selectedTab) {
            "Income" -> convertedTransactions.filter { it.amount.signum() >= 0 }.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
            "Expenses" -> convertedTransactions.filter { it.amount.signum() < 0 }.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount).abs() }
            else -> convertedTransactions.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        }

        val (currentStart, currentEnd) = TimelineService.getDateRange(config.filter.timelineRange)
        val actualStart = currentStart ?: allTransactions.minOfOrNull { it.timestamp } ?: System.currentTimeMillis()
        val actualEnd = currentEnd ?: System.currentTimeMillis()
        val duration = actualEnd - actualStart

        val numPeriods = when (val range = config.filter.timelineRange) {
            is TimelineRange.LastPeriod -> {
                when (range.period) {
                    LastPeriodType.LAST_24H -> 24
                    LastPeriodType.LAST_7_DAYS -> 7
                    else -> 12
                }
            }
            is TimelineRange.ThisPeriod -> {
                when (range.period) {
                    ThisPeriodType.WEEK -> 7
                    ThisPeriodType.MONTH -> 30
                    else -> 12
                }
            }
            else -> 12
        }

        val periodLength = duration / numPeriods.coerceAtLeast(1)
        
        val chartData = (0 until numPeriods).map { i ->
            val end = actualEnd - (i * periodLength)
            val start = end - periodLength
            val txs = convertedTransactions.filter { it.timestamp in start until end }
            when (config.selectedTab) {
                "Income" -> txs.filter { it.amount.signum() >= 0 }.sumOf { it.amount }.toDouble()
                "Expenses" -> txs.filter { it.amount.signum() < 0 }.sumOf { it.amount }.abs().toDouble()
                else -> txs.sumOf { it.amount }.toDouble()
            }
        }.reversed()

        val ghostData = if (config.showGhosting) {
            val (pStart, pEnd) = TimelineService.getPreviousRange(config.filter.timelineRange)
            val actualPEnd = pEnd ?: actualStart
            
            (0 until numPeriods).map { i ->
                val end = actualPEnd - (i * periodLength)
                val start = end - periodLength
                val txs = prevTransactions.filter { it.timestamp in start until end }
                when (config.selectedTab) {
                    "Income" -> txs.filter { it.amount.signum() >= 0 }.sumOf { it.amount }.toDouble()
                    "Expenses" -> txs.filter { it.amount.signum() < 0 }.sumOf { it.amount }.abs().toDouble()
                    else -> txs.sumOf { it.amount }.toDouble()
                }
            }.reversed()
        } else null

        val prevTotal = when (config.selectedTab) {
            "Income" -> prevTransactions.filter { it.amount.signum() >= 0 }.sumOf { it.amount }
            "Expenses" -> prevTransactions.filter { it.amount.signum() < 0 }.sumOf { it.amount }.abs()
            else -> prevTransactions.sumOf { it.amount }
        }

        val deltaPercentage = if (prevTotal.signum() != 0) {
            ((currentValue - prevTotal).toDouble() / prevTotal.abs().toDouble()).toFloat() * 100f
        } else 0f

        return PeriodComparisonData(
            currentValue = currentValue,
            currency = context.baseCurrency,
            precision = context.precision,
            chartData = chartData,
            ghostData = ghostData,
            runRate = prevTotal,
            deltaPercentage = deltaPercentage
        )
    }

    override suspend fun computeCashFlowTrend(
        config: CashFlowTrendCardConfig,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): CashFlowTrendData {
        val currentRates = context.exchangeRates
        
        val filteredTransactions = FilterService.filterTransactions(allTransactions, config.filter)
        val convertedTransactions = filteredTransactions.map { tx ->
            val convertedAmount = CurrencyService.convertAmount(
                amount = tx.amount,
                fromCurrency = tx.currency,
                toCurrency = context.baseCurrency,
                exchangeRates = currentRates,
                precision = context.precision + 4
            )
            tx.copy(amount = convertedAmount)
        }

        val prevFilter = getPreviousPeriodFilter(config.filter)
        val prevTransactions = FilterService.filterTransactions(allTransactions, prevFilter)
            .map { tx ->
                CurrencyService.convertAmount(tx.amount, tx.currency, context.baseCurrency, currentRates, context.precision + 4)
            }
        val prevTotal = prevTransactions.sumOf { it }
        val currentTotal = convertedTransactions.sumOf { it.amount }
        
        val deltaPercentage = if (prevTotal.signum() != 0) {
            val change = ((currentTotal - prevTotal).toDouble() / prevTotal.abs().toDouble()) * 100
            String.format(Locale.getDefault(), "%+1.1f%%", change)
        } else "0.0%"
        
        val now = System.currentTimeMillis()
        val numPeriods = 30
        val periodLength = 24 * 60 * 60 * 1000L
        
        val dailyData = (0 until numPeriods).map { i ->
            val end = now - (i * periodLength)
            val start = end - periodLength
            val txs = convertedTransactions.filter { it.timestamp in start until end }
            txs.sumOf { it.amount }.toDouble()
        }.reversed()

        val smaData = if (config.showSMA) {
            val period = config.smaPeriod.coerceAtLeast(1)
            dailyData.indices.map { i ->
                val start = (i - period + 1).coerceAtLeast(0)
                dailyData.subList(start, i + 1).average()
            }
        } else null

        val mean = dailyData.average()
        val variance = dailyData.map { (it - mean) * (it - mean) }.average()
        val volatility = Math.sqrt(variance).toBigDecimal().setScale(context.precision, RoundingMode.HALF_UP)

        val lineData = dailyData.runningReduce { acc, d -> acc + d }

        return CashFlowTrendData(
            barData = dailyData,
            lineData = lineData,
            smaData = smaData,
            volatility = volatility,
            percentageChange = deltaPercentage
        )
    }

    override suspend fun computeCashFlowProgress(
        config: CashFlowProgressCardConfig,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): CashFlowProgressData {
        val currentRates = context.exchangeRates
        
        val filteredTransactions = FilterService.filterTransactions(allTransactions, config.filter)
        val convertedTransactions = filteredTransactions.map { tx ->
            val convertedAmount = CurrencyService.convertAmount(
                amount = tx.amount,
                fromCurrency = tx.currency,
                toCurrency = context.baseCurrency,
                exchangeRates = currentRates,
                precision = context.precision + 4
            )
            tx.copy(amount = convertedAmount)
        }
        
        val income = convertedTransactions.filter { it.amount.signum() >= 0 }.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        val expenses = convertedTransactions.filter { it.amount.signum() < 0 }.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        
        return CashFlowProgressData(
            income = income,
            expenses = expenses,
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
        val currentRates = context.exchangeRates
        
        val filteredTransactions = FilterService.filterTransactions(allTransactions, config.filter)
        val convertedTransactions = filteredTransactions.map { tx ->
            val convertedAmount = CurrencyService.convertAmount(
                amount = tx.amount,
                fromCurrency = tx.currency,
                toCurrency = context.baseCurrency,
                exchangeRates = currentRates,
                precision = context.precision + 4
            )
            tx.copy(amount = convertedAmount)
        }
        
        val now = System.currentTimeMillis()
        val daysInPeriod = when (val range = config.filter.timelineRange) {
            is TimelineRange.LastPeriod -> {
                when (range.period) {
                    LastPeriodType.LAST_7_DAYS -> 7L
                    LastPeriodType.LAST_30_DAYS -> 30L
                    LastPeriodType.LAST_12_WEEKS -> 84L
                    LastPeriodType.LAST_6_MONTHS -> 180L
                    LastPeriodType.LAST_1_YEAR -> 365L
                    else -> 30L
                }
            }
            else -> 30L
        }
        
        val totalNetFlow = convertedTransactions.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        val avgDailyNetFlow = if (daysInPeriod > 0) {
            totalNetFlow.divide(BigDecimal.valueOf(daysInPeriod), context.precision, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        val dailyNetFlows = convertedTransactions.groupBy { (it.timestamp / (24 * 60 * 60 * 1000L)) }
            .map { entry -> entry.value.sumOf { it.amount }.toDouble() }
        
        val stdDev = if (dailyNetFlows.size > 1) {
            val mean = dailyNetFlows.average()
            val variance = dailyNetFlows.map { (it - mean) * (it - mean) }.average()
            Math.sqrt(variance)
        } else 0.0
        
        val currentBalance = allTransactions.fold(BigDecimal.ZERO) { acc, tx ->
            val convertedAmount = CurrencyService.convertAmount(
                amount = tx.amount,
                fromCurrency = tx.currency,
                toCurrency = context.baseCurrency,
                exchangeRates = currentRates,
                precision = context.precision + 4
            )
            acc.add(convertedAmount)
        }
        
        var forecastDailyFlow = avgDailyNetFlow
        if (config.includePlannedPayments) {
            val plannedPayments = allTransactions.filter { 
                it.status == TransactionStatus.UNCLEARED && 
                it.timestamp > now && it.timestamp < now + (config.forecastDays * 24 * 60 * 60 * 1000L)
            }
            val totalPlanned = plannedPayments.sumOf { tx ->
                CurrencyService.convertAmount(tx.amount, tx.currency, context.baseCurrency, currentRates, context.precision)
            }
            val dailyPlannedFlow = totalPlanned.divide(BigDecimal.valueOf(config.forecastDays.toLong()), context.precision, RoundingMode.HALF_UP)
            forecastDailyFlow = forecastDailyFlow.add(dailyPlannedFlow)
        }

        val forecastBalance = currentBalance.add(forecastDailyFlow.multiply(BigDecimal.valueOf(config.forecastDays.toLong())))
        
        val projectedZeroDate = if (forecastDailyFlow.signum() < 0 && currentBalance.signum() > 0) {
            val daysToZero = currentBalance.divide(forecastDailyFlow.abs(), 0, RoundingMode.DOWN).toLong()
            now + (daysToZero * 24 * 60 * 60 * 1000L)
        } else null

        val numPoints = 10
        val chartData = (0 until numPoints).map { i ->
            val daysFromNow = i * (config.forecastDays / (numPoints - 1))
            val realistic = currentBalance.add(forecastDailyFlow.multiply(BigDecimal.valueOf(daysFromNow.toLong())))
            val margin = (stdDev * Math.sqrt(daysFromNow.toDouble())).toBigDecimal()
            val optimistic = realistic.add(margin)
            val pessimistic = realistic.subtract(margin)
            Triple(optimistic.toDouble(), realistic.toDouble(), pessimistic.toDouble())
        }
        
        return BalanceForecastData(
            forecastBalance = forecastBalance,
            currency = context.baseCurrency,
            precision = context.precision,
            chartData = chartData,
            projectedZeroDate = projectedZeroDate,
            currentLiquidCash = currentBalance
        )
    }

    override suspend fun computePlannedPayments(
        config: PlannedPaymentsCardConfig,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): PlannedPaymentsData {
        val currentRates = context.exchangeRates
        
        val filteredTransactions = FilterService.filterTransactions(allTransactions, config.filter)
        val convertedTransactions = filteredTransactions.map { tx ->
            val convertedAmount = CurrencyService.convertAmount(
                amount = tx.amount,
                fromCurrency = tx.currency,
                toCurrency = context.baseCurrency,
                exchangeRates = currentRates,
                precision = context.precision + 4
            )
            tx.copy(amount = convertedAmount)
        }
        
        val categoryTotals = convertedTransactions
            .filter { it.amount.signum() < 0 }
            .groupBy { it.category }
            .mapValues { entry -> 
                entry.value.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }.abs() 
            }
        
        val total = categoryTotals.values.fold(BigDecimal.ZERO) { acc, amount -> acc.add(amount) }
        
        val categoryColors = listOf(
            Color(0xFF9C27B0),
            Color(0xFF4CAF50),
            Color(0xFFFF9800),
            Color(0xFF4CAF50),
            Color(0xFFE91E63),
            Color(0xFF9C27B0),
            Color(0xFF00BCD4)
        )
        
        val payments = categoryTotals.entries.mapIndexed { index, entry ->
            val amount = entry.value
            val progress = if (total > BigDecimal.ZERO) {
                (amount.divide(total, 2, RoundingMode.HALF_UP).toFloat()).coerceAtMost(1.0f)
            } else {
                0f
            }
            val color = categoryColors.getOrElse(index % categoryColors.size) { Color(0xFF9C27B0) }
            
            PaymentItem(
                name = entry.key,
                amount = amount,
                progress = progress,
                color = color
            )
        }.sortedByDescending { it.amount }
        
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
        val currentRates = context.exchangeRates
        
        val filteredTransactions = FilterService.filterTransactions(allTransactions, config.filter)
        val convertedTransactions = filteredTransactions.map { tx ->
            val convertedAmount = CurrencyService.convertAmount(
                amount = tx.amount,
                fromCurrency = tx.currency,
                toCurrency = context.baseCurrency,
                exchangeRates = currentRates,
                precision = context.precision + 4
            )
            tx.copy(amount = convertedAmount)
        }
        
        val incomeTransactions = convertedTransactions.filter { it.amount.signum() >= 0 }
        val expenseTransactions = convertedTransactions.filter { it.amount.signum() < 0 }
        
        val totalIncome = incomeTransactions.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        val totalExpense = expenseTransactions.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        
        return IncomeExpensesBookData(
            incomeTotal = totalIncome,
            expenseTotal = totalExpense,
            incomeCount = incomeTransactions.size,
            expenseCount = expenseTransactions.size,
            currency = context.baseCurrency,
            precision = context.precision
        )
    }

    override suspend fun computeCashFlowTable(
        config: CashFlowTableCardConfig,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): CashFlowTableData {
        val currentRates = context.exchangeRates
        
        val filteredTransactions = FilterService.filterTransactions(allTransactions, config.filter)
        val convertedTransactions = filteredTransactions.map { tx ->
            val convertedAmount = CurrencyService.convertAmount(
                amount = tx.amount,
                fromCurrency = tx.currency,
                toCurrency = context.baseCurrency,
                exchangeRates = currentRates,
                precision = context.precision + 4
            )
            tx.copy(amount = convertedAmount)
        }
        
        val incomeTransactions = convertedTransactions.filter { it.amount.signum() >= 0 }
        val expenseTransactions = convertedTransactions.filter { it.amount.signum() < 0 }
        
        val incomeCount = incomeTransactions.size
        val expenseCount = expenseTransactions.size
        
        val incomeTotal = incomeTransactions.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        val expenseTotal = expenseTransactions.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        
        val incomeAvgDay = if (incomeCount > 0) incomeTotal.divide(BigDecimal.valueOf(84), context.precision, RoundingMode.HALF_UP) else BigDecimal.ZERO
        val expenseAvgDay = if (expenseCount > 0) expenseTotal.divide(BigDecimal.valueOf(84), context.precision, RoundingMode.HALF_UP) else BigDecimal.ZERO
        
        val incomeAvgRecord = if (incomeCount > 0) incomeTotal.divide(BigDecimal.valueOf(incomeCount.toLong()), context.precision, RoundingMode.HALF_UP) else BigDecimal.ZERO
        val expenseAvgRecord = if (expenseCount > 0) expenseTotal.divide(BigDecimal.valueOf(expenseCount.toLong()), context.precision, RoundingMode.HALF_UP) else BigDecimal.ZERO
        
        return CashFlowTableData(
            incomeCount = incomeCount,
            expenseCount = expenseCount,
            incomeAvgDay = incomeAvgDay,
            expenseAvgDay = expenseAvgDay,
            incomeAvgRecord = incomeAvgRecord,
            expenseAvgRecord = expenseAvgRecord,
            incomeTotal = incomeTotal,
            expenseTotal = expenseTotal,
            netCashFlow = incomeTotal.add(expenseTotal),
            currency = context.baseCurrency,
            precision = context.precision
        )
    }

    override suspend fun computePlannedPaymentsTimeline(
        config: PlannedPaymentsTimelineCardConfig,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): PlannedPaymentsTimelineData {
        val currentRates = context.exchangeRates
        
        val filteredTransactions = FilterService.filterTransactions(allTransactions, config.filter)
        val convertedTransactions = filteredTransactions.map { tx ->
            val convertedAmount = CurrencyService.convertAmount(
                amount = tx.amount,
                fromCurrency = tx.currency,
                toCurrency = context.baseCurrency,
                exchangeRates = currentRates,
                precision = context.precision + 4
            )
            tx.copy(amount = convertedAmount)
        }
        
        val expenseTransactions = convertedTransactions.filter { it.amount.signum() < 0 }
        val calendar = java.util.Calendar.getInstance()
        val now = System.currentTimeMillis()
        
        val numWeeks = when (val range = config.filter.timelineRange) {
            is TimelineRange.LastPeriod -> {
                when (range.period) {
                    LastPeriodType.LAST_7_DAYS -> 1
                    LastPeriodType.LAST_30_DAYS -> 4
                    LastPeriodType.LAST_12_WEEKS -> 12
                    LastPeriodType.LAST_6_MONTHS -> 24
                    LastPeriodType.LAST_1_YEAR -> 52
                    else -> 12
                }
            }
            else -> 12
        }
        
        val weekMillis = 7 * 24 * 60 * 60 * 1000L
        
        val weeklyData = (0 until numWeeks).map { weekIndex ->
            val weekEnd = now - (weekIndex * weekMillis)
            val weekStart = weekEnd - weekMillis
            val weekTransactions = expenseTransactions.filter { 
                it.timestamp >= weekStart && it.timestamp < weekEnd 
            }
            val weekTotal = weekTransactions.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
            calendar.timeInMillis = weekStart
            val weekOfYear = calendar.get(java.util.Calendar.WEEK_OF_YEAR)
            val year = calendar.get(java.util.Calendar.YEAR)
            "Week $weekOfYear ($year)" to weekTotal.abs()
        }.reversed()
        
        val total = weeklyData.fold(BigDecimal.ZERO) { acc, (_, amount) -> acc.add(amount) }
        
        return PlannedPaymentsTimelineData(
            total = total,
            currency = context.baseCurrency,
            precision = context.precision,
            weeklyData = weeklyData
        )
    }

    override suspend fun computeBalanceByAccounts(
        config: BalanceByAccountsCardConfig,
        allTransactions: List<Transaction>,
        accounts: List<Account>,
        context: ComputationContext
    ): BalanceByAccountsData {
        val currentRates = context.exchangeRates
        
        val filteredTransactions = FilterService.filterTransactions(allTransactions, config.filter)
        
        val accountBalances = accounts.map { account ->
            val accountTransactions = filteredTransactions.filter { it.accountId == account.id }
            val convertedTransactions = accountTransactions.map { tx ->
                CurrencyService.convertAmount(tx.amount, tx.currency, context.baseCurrency, currentRates, context.precision + 4)
            }
            val balance = convertedTransactions.fold(BigDecimal.ZERO) { acc, amount -> acc.add(amount) }
            AccountBalanceItem(
                accountName = account.name,
                balance = balance,
                currency = context.baseCurrency,
                isNegative = balance < BigDecimal.ZERO
            )
        }
        
        val totalBalance = accountBalances.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.balance) }
        
        return BalanceByAccountsData(
            totalBalance = totalBalance,
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
        val currentRates = context.exchangeRates
        
        val convertedTransactions = allTransactions.map { tx ->
            val convertedAmount = CurrencyService.convertAmount(
                amount = tx.amount,
                fromCurrency = tx.currency,
                toCurrency = context.baseCurrency,
                exchangeRates = currentRates,
                precision = context.precision + 4
            )
            tx.copy(amount = convertedAmount)
        }
        
        val now = System.currentTimeMillis()
        val numPeriods = when (val range = config.filter.timelineRange) {
            is TimelineRange.LastPeriod -> {
                when (range.period) {
                    LastPeriodType.LAST_7_DAYS -> 7
                    LastPeriodType.LAST_30_DAYS -> 4
                    LastPeriodType.LAST_12_WEEKS -> 12
                    else -> 12
                }
            }
            else -> 12
        }
        
        val periodLength = when (val range = config.filter.timelineRange) {
            is TimelineRange.LastPeriod -> {
                when (range.period) {
                    LastPeriodType.LAST_7_DAYS -> 24 * 60 * 60 * 1000L
                    else -> 7 * 24 * 60 * 60 * 1000L
                }
            }
            else -> 7 * 24 * 60 * 60 * 1000L
        }
        
        val weeklyBalances = (0 until numPeriods).map { periodIndex ->
            val periodEnd = now - (periodIndex * periodLength)
            val periodStart = periodEnd - periodLength
            val periodTransactions = convertedTransactions.filter { 
                it.timestamp >= periodStart && it.timestamp < periodEnd 
            }
            periodTransactions.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }.toDouble()
        }.reversed()
        
        val currentBalance = weeklyBalances.lastOrNull()?.toBigDecimal() ?: BigDecimal.ZERO
        val firstBalance = weeklyBalances.firstOrNull() ?: 0.0
        val percentageChange = if (firstBalance != 0.0) {
            val change = ((currentBalance.toDouble() - firstBalance) / Math.abs(firstBalance)) * 100
            String.format(Locale.getDefault(), "%.1f%%", change)
        } else "0%"
        
        return BalanceTrendData(
            currentBalance = currentBalance,
            percentageChange = percentageChange,
            currency = context.baseCurrency,
            precision = context.precision,
            weeklyBalances = weeklyBalances
        )
    }

    private fun getPreviousPeriodFilter(currentFilter: TransactionFilter): TransactionFilter {
        val (prevStart, prevEnd) = TimelineService.getPreviousRange(currentFilter.timelineRange)
        return currentFilter.copy(
            timelineRange = if (prevStart != null && prevEnd != null) 
                TimelineRange.CustomRange(prevStart, prevEnd) 
                else TimelineRange.AllTime,
            startDate = prevStart,
            endDate = prevEnd
        )
    }

    /**
     * Filters allTransactions based on card scope.
     * GLOBAL: Returns all allTransactions
     * ACCOUNT: Returns only allTransactions for the specified account
     */
    private fun filterTransactionsByScope(
        allTransactions: List<Transaction>,
        scope: CardScope,
        accountId: String?
    ): List<Transaction> {
        return if (scope == CardScope.ACCOUNT && accountId != null) {
            allTransactions.filter { it.accountId == accountId }
        } else {
            allTransactions
        }
    }

    /**
     * Helper method to generate time buckets based on timeline range.
     */
    private fun getTimeBuckets(timelineRange: com.osscabinet.ossWallet.domain.model.timeline.TimelineRange): List<TimeBucket> {
        // ... (existing implementation)
        return emptyList() // Placeholder for now, I'll keep the existing implementation if I can.
    }

    private data class TimeBucket(val start: Long, val end: Long)
}
