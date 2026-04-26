package com.osscabinet.ossWallet.domain.service

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.analytics.CategoryBreakdownData
import com.osscabinet.ossWallet.domain.model.analytics.CategoryExpenseTreemapData
import com.osscabinet.ossWallet.domain.model.analytics.TimelineTrajectoryData
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
import java.math.BigDecimal

/**
 * Service for computing card data with scope-aware filtering.
 * Provides unified computation logic for all card types across different screens.
 * 
 * This interface extends CardComputationEngine to provide additional computation methods
 * for analytics cards that are not part of the core engine interface.
 */
interface CardComputationService : CardComputationEngine {
    
    /**
     * Computes category breakdown for pie chart card.
     * Groups transactions by category and calculates net balance per category.
     */
    suspend fun computeCategoryBreakdown(
        config: CategoryBreakdownCardConfig,
        transactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): List<CategoryBreakdownData>

    /**
     * Computes timeline trajectory for multi-line chart card.
     * Generates time-series data for balance, expense, and income.
     */
    suspend fun computeTimelineTrajectory(
        config: TimelineTrajectoryCardConfig,
        transactions: List<Transaction>,
        accounts: List<Account>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): List<TimelineTrajectoryData>

    /**
     * Computes category expense treemap data.
     * Groups expenses by category and calculates proportions.
     */
    suspend fun computeCategoryExpenseTreemap(
        config: CategoryExpenseTreemapCardConfig,
        transactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): List<CategoryExpenseTreemapData>
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
        transactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): CashFlowResult {
        // Determine scope based on accountIds in filter
        val accountId = config.filter.accountIds.firstOrNull()
        val scope = if (accountId != null) CardScope.ACCOUNT else CardScope.GLOBAL

        // Filter transactions based on scope
        val filteredTransactions = filterTransactionsByScope(
            transactions = transactions,
            scope = scope,
            accountId = accountId
        )

        // HYBRID APPROACH: No timeline filtering here - cards already filter using ViewModel helpers
        // Use TimelineRange.AllTime since filtering is done at card level
        return financialUseCases.calculateCashFlow(
            allTransactions = filteredTransactions,
            timelineRange = TimelineRange.AllTime,  // Already filtered at card level
            baseCurrency = baseCurrency,
            exchangeRates = exchangeRates,
            precision = precision
        )
    }

    override suspend fun computeCurrencyTotals(
        config: CurrencyBalancesCardConfig,
        accounts: List<Account>,
        transactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): List<CurrencyAggregationItem> {
        // Determine scope based on accountIds in filter
        val accountId = config.filter.accountIds.firstOrNull()
        val scope = if (accountId != null) CardScope.ACCOUNT else CardScope.GLOBAL
        
        // Filter transactions based on scope
        val filteredTransactions = filterTransactionsByScope(
            transactions = transactions,
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
            baseCurrency = baseCurrency,
            exchangeRates = exchangeRates,
            precision = precision
        )
    }

    override suspend fun computeNetWorth(
        config: NetWorthCardConfig,
        accounts: List<Account>,
        transactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): Pair<BigDecimal, CashFlowResult> {
        // Filter transactions based on scope (NetWorthCardConfig is currently GLOBAL only)
        val filteredTransactions = filterTransactionsByScope(
            transactions = transactions,
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
            baseCurrency = baseCurrency,
            exchangeRates = exchangeRates,
            precision = precision,
            timelineRange = TimelineRange.AllTime  // Already filtered at card level
        )

        val cashFlow = financialUseCases.calculateCashFlow(
            allTransactions = filteredTransactions,
            timelineRange = TimelineRange.AllTime,  // Already filtered at card level
            baseCurrency = baseCurrency,
            exchangeRates = exchangeRates,
            precision = precision
        )

        return Pair(netWorth, cashFlow)
    }

    override suspend fun computeAccountStats(
        config: AccountsCardConfig,
        accounts: List<Account>,
        transactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): Pair<List<Account>, Map<String, AccountSummaryItem>> {
        // HYBRID APPROACH: No timeline filtering here - cards already filter using ViewModel helpers
        // Use TimelineRange.AllTime since filtering is done at card level
        val accountStats = financialUseCases.getAccountSummaries(
            accounts = accounts,
            allTransactions = transactions,
            timelineRange = TimelineRange.AllTime,  // Already filtered at card level
            baseCurrency = baseCurrency,
            exchangeRates = exchangeRates,
            precision = precision
        )
        // Update accounts with timeline-filtered balances
        val updatedAccounts = financialUseCases.getTotalBalances(
            accounts = accounts,
            allTransactions = transactions,
            exchangeRates = exchangeRates,
            precision = precision,
            timelineRange = TimelineRange.AllTime  // Already filtered at card level
        )
        return Pair(updatedAccounts, accountStats)
    }

    override suspend fun computeCategoryBreakdown(
        config: CategoryBreakdownCardConfig,
        transactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): List<CategoryBreakdownData> {
        val accountId = config.filter.accountIds.firstOrNull()
        val scope = if (accountId != null) CardScope.ACCOUNT else CardScope.GLOBAL
        val filteredByScope = filterTransactionsByScope(transactions, scope, accountId)

        val allCategories = categoryDao.getAllCategoriesSync()
        val categoryMap = allCategories.associateBy { it.name }
        val categoryIdMap = allCategories.associateBy { it.id }

        // Recursive helper to build tree
        fun buildTree(categoryName: String?, txs: List<Transaction>, globalTotal: BigDecimal, parentColor: Color? = null, index: Int = 0, totalSiblings: Int = 1): CategoryBreakdownData? {
            val entity = categoryMap[categoryName] ?: return null
            
            // Apply shading logic for children
            val baseColor = if (parentColor == null) Color(entity.colorHex) else {
                // If it's a child, derive shade from parent
                // We use a simple HSL-like shift or just alpha/lightness variation
                // For now, let's use a variation based on index to ensure siblings are distinct
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

            // ... rest of the aggregation logic

            // Current category's OWN transactions (not its children's)
            val ownTxs = txs.filter { it.category == categoryName }
            val ownIncome = ownTxs.filter { it.amount.signum() >= 0 }.sumOf { tx ->
                CurrencyService.convertAmount(tx.amount, tx.currency, baseCurrency, exchangeRates, precision)
            }
            val ownExpense = ownTxs.filter { it.amount.signum() < 0 }.sumOf { tx ->
                CurrencyService.convertAmount(tx.amount, tx.currency, baseCurrency, exchangeRates, precision)
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
            CurrencyService.convertAmount(tx.amount, tx.currency, baseCurrency, exchangeRates, precision)
        }.abs()

        // Root Level: Categories with no parent
        val rootCategories = allCategories.filter { it.parentId == null }
        return rootCategories.mapNotNull { root ->
            buildTree(root.name, filteredByScope, totalNetGlobal)
        }.sortedByDescending { it.netBalance.abs() }
    }

    override suspend fun computeTimelineTrajectory(
        config: TimelineTrajectoryCardConfig,
        transactions: List<Transaction>,
        accounts: List<Account>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): List<TimelineTrajectoryData> {
        val accountId = config.filter.accountIds.firstOrNull()
        val scope = if (accountId != null) CardScope.ACCOUNT else CardScope.GLOBAL
        val filteredByScope = filterTransactionsByScope(transactions, scope, accountId)

        // HYBRID APPROACH: No timeline filtering here - cards already filter using ViewModel helpers
        // Use filteredByScope directly (scope filtering only, no timeline filtering)

        // Determine time buckets based on timelineRange using TimelineService
        val timeBuckets = getTimeBuckets(config.filter.timelineRange)

        return timeBuckets.map { bucket ->
            val bucketTransactions = filteredByScope.filter { tx ->
                tx.timestamp >= bucket.start && tx.timestamp < bucket.end
            }
            
            val income = bucketTransactions.filter { it.amount.signum() >= 0 }.sumOf { tx ->
                CurrencyService.convertAmount(tx.amount, tx.currency, baseCurrency, exchangeRates, precision)
            }
            val expense = bucketTransactions.filter { it.amount.signum() < 0 }.sumOf { tx ->
                CurrencyService.convertAmount(tx.amount, tx.currency, baseCurrency, exchangeRates, precision)
            }
            
            // Calculate running balance
            val priorTransactions = filteredByScope.filter { it.timestamp < bucket.start }
            val priorBalance = financialUseCases.calculateNetWorth(
                accounts = if (scope == CardScope.ACCOUNT && accountId != null) accounts.filter { it.id == accountId } else accounts,
                allTransactions = priorTransactions,
                baseCurrency = baseCurrency,
                exchangeRates = exchangeRates,
                precision = precision,
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
        transactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): List<CategoryExpenseTreemapData> {
        val accountId = config.filter.accountIds.firstOrNull()
        val scope = if (accountId != null) CardScope.ACCOUNT else CardScope.GLOBAL
        val filteredByScope = filterTransactionsByScope(transactions, scope, accountId)
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
                CurrencyService.convertAmount(tx.amount, tx.currency, baseCurrency, exchangeRates, precision)
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
            CurrencyService.convertAmount(tx.amount, tx.currency, baseCurrency, exchangeRates, precision)
        }.abs()

        val rootCategories = allCategories.filter { it.parentId == null }
        return rootCategories.mapNotNull { root ->
            buildTree(root.name, expenseTransactions, totalExpenseGlobal)
        }.sortedByDescending { it.expense.abs() }
    }

    /**
     * Filters transactions based on card scope.
     * GLOBAL: Returns all transactions
     * ACCOUNT: Returns only transactions for the specified account
     */
    private fun filterTransactionsByScope(
        transactions: List<Transaction>,
        scope: CardScope,
        accountId: String?
    ): List<Transaction> {
        return if (scope == CardScope.ACCOUNT && accountId != null) {
            transactions.filter { it.accountId == accountId }
        } else {
            transactions
        }
    }

    /**
     * Helper method to generate time buckets based on timeline range.
     * TODO: This should use TimelineService for consistency.
     */
    private fun getTimeBuckets(timelineRange: com.osscabinet.ossWallet.domain.model.timeline.TimelineRange): List<TimeBucket> {
        val now = System.currentTimeMillis()
        return when (timelineRange) {
            is com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.AllTime -> {
                // For AllTime, use monthly buckets for the last year
                val buckets = mutableListOf<TimeBucket>()
                var currentMonth = 11
                while (currentMonth >= 0) {
                    val calendar = java.util.Calendar.getInstance()
                    calendar.add(java.util.Calendar.MONTH, -currentMonth)
                    calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    calendar.set(java.util.Calendar.MINUTE, 0)
                    calendar.set(java.util.Calendar.SECOND, 0)
                    calendar.set(java.util.Calendar.MILLISECOND, 0)
                    val start = calendar.timeInMillis
                    
                    calendar.set(java.util.Calendar.DAY_OF_MONTH, calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
                    calendar.set(java.util.Calendar.MINUTE, 59)
                    calendar.set(java.util.Calendar.SECOND, 59)
                    calendar.set(java.util.Calendar.MILLISECOND, 999)
                    val end = calendar.timeInMillis
                    
                    buckets.add(TimeBucket(start, end))
                    currentMonth--
                }
                buckets
            }
            is com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod -> {
                // Generate buckets based on period type
                when (timelineRange.period) {
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_7_DAYS -> {
                        (0..6).map { day ->
                            val calendar = java.util.Calendar.getInstance()
                            calendar.add(java.util.Calendar.DAY_OF_YEAR, -day)
                            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                            calendar.set(java.util.Calendar.MINUTE, 0)
                            calendar.set(java.util.Calendar.SECOND, 0)
                            calendar.set(java.util.Calendar.MILLISECOND, 0)
                            val start = calendar.timeInMillis
                            
                            calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
                            calendar.set(java.util.Calendar.MINUTE, 59)
                            calendar.set(java.util.Calendar.SECOND, 59)
                            calendar.set(java.util.Calendar.MILLISECOND, 999)
                            val end = calendar.timeInMillis
                            
                            TimeBucket(start, end)
                        }.reversed()
                    }
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS -> {
                        (0..29).map { day ->
                            val calendar = java.util.Calendar.getInstance()
                            calendar.add(java.util.Calendar.DAY_OF_YEAR, -day)
                            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                            calendar.set(java.util.Calendar.MINUTE, 0)
                            calendar.set(java.util.Calendar.SECOND, 0)
                            calendar.set(java.util.Calendar.MILLISECOND, 0)
                            val start = calendar.timeInMillis
                            
                            calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
                            calendar.set(java.util.Calendar.MINUTE, 59)
                            calendar.set(java.util.Calendar.SECOND, 59)
                            calendar.set(java.util.Calendar.MILLISECOND, 999)
                            val end = calendar.timeInMillis
                            
                            TimeBucket(start, end)
                        }.reversed()
                    }
                    else -> {
                        // Default to 7-day buckets for other periods
                        (0..6).map { day ->
                            val calendar = java.util.Calendar.getInstance()
                            calendar.add(java.util.Calendar.DAY_OF_YEAR, -day)
                            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                            calendar.set(java.util.Calendar.MINUTE, 0)
                            calendar.set(java.util.Calendar.SECOND, 0)
                            calendar.set(java.util.Calendar.MILLISECOND, 0)
                            val start = calendar.timeInMillis
                            
                            calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
                            calendar.set(java.util.Calendar.MINUTE, 59)
                            calendar.set(java.util.Calendar.SECOND, 59)
                            calendar.set(java.util.Calendar.MILLISECOND, 999)
                            val end = calendar.timeInMillis
                            
                            TimeBucket(start, end)
                        }.reversed()
                    }
                }
            }
            else -> {
                // Default to 7-day buckets for ThisPeriod and CustomRange
                (0..6).map { day ->
                    val calendar = java.util.Calendar.getInstance()
                    calendar.add(java.util.Calendar.DAY_OF_YEAR, -day)
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    calendar.set(java.util.Calendar.MINUTE, 0)
                    calendar.set(java.util.Calendar.SECOND, 0)
                    calendar.set(java.util.Calendar.MILLISECOND, 0)
                    val start = calendar.timeInMillis
                    
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
                    calendar.set(java.util.Calendar.MINUTE, 59)
                    calendar.set(java.util.Calendar.SECOND, 59)
                    calendar.set(java.util.Calendar.MILLISECOND, 999)
                    val end = calendar.timeInMillis
                    
                    TimeBucket(start, end)
                }.reversed()
            }
        }
    }

    private data class TimeBucket(val start: Long, val end: Long)
}
