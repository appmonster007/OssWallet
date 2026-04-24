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
import com.osscabinet.ossWallet.domain.usecase.AccountSummaryItem
import com.osscabinet.ossWallet.domain.usecase.CashFlowResult
import com.osscabinet.ossWallet.domain.usecase.CurrencyAggregationItem
import com.osscabinet.ossWallet.data.local.dao.CategoryDao
import java.math.BigDecimal

/**
 * Service for computing card data with scope-aware filtering.
 * Provides unified computation logic for all card types across different screens.
 */
interface CardComputationService {
    
    /**
     * Computes cash flow data for a cash flow card.
     * Filters transactions based on card scope (GLOBAL or ACCOUNT).
     */
    suspend fun computeCashFlow(
        config: CashFlowCardConfig,
        transactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): CashFlowResult

    /**
     * Computes currency totals for a currency balances card.
     * Filters transactions based on card scope (GLOBAL or ACCOUNT).
     */
    suspend fun computeCurrencyTotals(
        config: CurrencyBalancesCardConfig,
        accounts: List<Account>,
        transactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): List<CurrencyAggregationItem>

    /**
     * Computes net worth and cash flow for a net worth card.
     * Filters transactions based on card scope (GLOBAL or ACCOUNT).
     */
    suspend fun computeNetWorth(
        config: NetWorthCardConfig,
        accounts: List<Account>,
        transactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): Pair<BigDecimal, CashFlowResult>

    /**
     * Computes account statistics for an accounts card.
     * Filters transactions based on card scope (GLOBAL or ACCOUNT).
     */
    suspend fun computeAccountStats(
        config: AccountsCardConfig,
        accounts: List<Account>,
        transactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): Pair<List<Account>, Map<String, AccountSummaryItem>>

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
        // Determine scope based on accountId
        val scope = if (config.accountId != null) CardScope.ACCOUNT else CardScope.GLOBAL
        
        // Filter transactions based on scope
        val filteredTransactions = filterTransactionsByScope(
            transactions = transactions,
            scope = scope,
            accountId = config.accountId
        )
        
        return financialUseCases.calculateCashFlow(
            allTransactions = filteredTransactions,
            timelineRange = config.timelineRange,
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
        // Determine scope based on accountId
        val scope = if (config.accountId != null) CardScope.ACCOUNT else CardScope.GLOBAL
        
        // Filter transactions based on scope
        val filteredTransactions = filterTransactionsByScope(
            transactions = transactions,
            scope = scope,
            accountId = config.accountId
        )
        
        // Filter accounts based on scope
        val filteredAccounts = if (config.accountId != null) {
            accounts.filter { it.id == config.accountId }
        } else {
            accounts
        }
        
        return financialUseCases.getCurrencyAggregation(
            accounts = filteredAccounts,
            allTransactions = filteredTransactions,
            timelineRange = config.timelineRange,
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
        
        val netWorth = financialUseCases.calculateNetWorth(
            accounts = filteredAccounts,
            allTransactions = filteredTransactions,
            baseCurrency = baseCurrency,
            exchangeRates = exchangeRates,
            precision = precision,
            timelineRange = config.timelineRange
        )
        
        val cashFlow = financialUseCases.calculateCashFlow(
            allTransactions = filteredTransactions,
            timelineRange = config.timelineRange,
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
        // Filter transactions based on timelineRange
        val accountStats = financialUseCases.getAccountSummaries(
            accounts = accounts,
            allTransactions = transactions,
            timelineRange = config.timelineRange,
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
            timelineRange = config.timelineRange
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
        val scope = if (config.accountId != null) CardScope.ACCOUNT else CardScope.GLOBAL
        val filteredByScope = filterTransactionsByScope(transactions, scope, config.accountId)
        
        // Filter by timeline range - THIS WAS MISSING
        val filteredTransactions = TimelineService.filterByTimelineRange(filteredByScope, config.timelineRange)
        
        // Get all categories for color and subcategory info
        val allCategories = categoryDao.getAllCategoriesSync()
        val categoryMap = allCategories.associateBy { it.name }
        
        // Group by category
        val transactionCategoryMap = filteredTransactions.groupBy { it.category }
        
        // Calculate total net balance for percentage calculation
        val totalNetBalance = transactionCategoryMap.values.sumOf { txs ->
            txs.sumOf { tx ->
                val amountInBase = convertAmount(tx.amount, tx.currency, baseCurrency, exchangeRates, precision)
                amountInBase // Already signed (positive for income, negative for expense)
            }
        }
        
        return transactionCategoryMap.map { (category, txs) ->
            val categoryEntity = categoryMap[category]
            val income = txs.filter { it.amount.signum() >= 0 }.sumOf { tx ->
                convertAmount(tx.amount, tx.currency, baseCurrency, exchangeRates, precision)
            }
            val expense = txs.filter { it.amount.signum() < 0 }.sumOf { tx ->
                convertAmount(tx.amount.abs(), tx.currency, baseCurrency, exchangeRates, precision)
            }
            val netBalance = income - expense
            val percentage = if (totalNetBalance.abs() > BigDecimal.ZERO) {
                (netBalance.abs() / totalNetBalance.abs()).toFloat()
            } else 0f
            
            // Get category color from database, or use default
            val colorHex = categoryEntity?.colorHex ?: 0xFF6200EE.toInt()
            
            // Check for subcategories
            val hasSubcategories = categoryEntity != null && allCategories.any { it.parentId == categoryEntity.id }
            
            // Compute subcategory data if exists
            val subcategoryData = if (hasSubcategories) {
                val subCategories = allCategories.filter { it.parentId == categoryEntity.id }
                subCategories.map { subCat ->
                    val subTxs = txs.filter { it.category == subCat.name }
                    val subIncome = subTxs.filter { it.amount.signum() >= 0 }.sumOf { tx ->
                        convertAmount(tx.amount, tx.currency, baseCurrency, exchangeRates, precision)
                    }
                    val subExpense = subTxs.filter { it.amount.signum() < 0 }.sumOf { tx ->
                        convertAmount(tx.amount.abs(), tx.currency, baseCurrency, exchangeRates, precision)
                    }
                    val subNetBalance = subIncome - subExpense
                    val subPercentage = if (netBalance.abs() > BigDecimal.ZERO) {
                        (subNetBalance.abs() / netBalance.abs()).toFloat()
                    } else 0f
                    
                    CategoryBreakdownData(
                        category = subCat.name,
                        categoryId = subCat.id,
                        netBalance = subNetBalance,
                        income = subIncome,
                        expense = subExpense,
                        colorHex = subCat.colorHex,
                        percentage = subPercentage,
                        hasSubcategories = false,
                        subcategoryData = null
                    )
                }
            } else null
            
            CategoryBreakdownData(
                category = category,
                categoryId = categoryEntity?.id,
                netBalance = netBalance,
                income = income,
                expense = expense,
                colorHex = colorHex,
                percentage = percentage,
                hasSubcategories = hasSubcategories,
                subcategoryData = subcategoryData
            )
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
        val scope = if (config.accountId != null) CardScope.ACCOUNT else CardScope.GLOBAL
        val filteredByScope = filterTransactionsByScope(transactions, scope, config.accountId)
        
        // Filter by timeline range - THIS WAS MISSING
        val filteredTransactions = TimelineService.filterByTimelineRange(filteredByScope, config.timelineRange)
        
        // Determine time buckets based on timelineRange using TimelineService
        val timeBuckets = getTimeBuckets(config.timelineRange)
        
        return timeBuckets.map { bucket ->
            val bucketTransactions = filteredTransactions.filter { tx ->
                tx.timestamp >= bucket.start && tx.timestamp < bucket.end
            }
            
            val income = bucketTransactions.filter { it.amount.signum() >= 0 }.sumOf { tx ->
                convertAmount(tx.amount, tx.currency, baseCurrency, exchangeRates, precision)
            }
            val expense = bucketTransactions.filter { it.amount.signum() < 0 }.sumOf { tx ->
                convertAmount(tx.amount.abs(), tx.currency, baseCurrency, exchangeRates, precision)
            }
            
            // Calculate running balance
            val priorTransactions = filteredTransactions.filter { it.timestamp < bucket.start }
            val priorBalance = financialUseCases.calculateNetWorth(
                accounts = if (scope == CardScope.ACCOUNT) accounts.filter { it.id == config.accountId } else accounts,
                allTransactions = priorTransactions,
                baseCurrency = baseCurrency,
                exchangeRates = exchangeRates,
                precision = precision,
                timelineRange = config.timelineRange
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
        val scope = if (config.accountId != null) CardScope.ACCOUNT else CardScope.GLOBAL
        val filteredByScope = filterTransactionsByScope(transactions, scope, config.accountId)
        
        // Filter by timeline range - THIS WAS MISSING
        val filteredTransactions = TimelineService.filterByTimelineRange(filteredByScope, config.timelineRange)
        
        // Get all categories for color and subcategory info
        val allCategories = categoryDao.getAllCategoriesSync()
        val categoryMap = allCategories.associateBy { it.name }
        
        // Filter only expenses (negative amounts)
        val expenseTransactions = filteredTransactions.filter { it.amount.signum() < 0 }
        
        // Group by category
        val transactionCategoryMap = expenseTransactions.groupBy { it.category }
        
        val totalExpense = transactionCategoryMap.values.sumOf { txs ->
            txs.sumOf { tx ->
                convertAmount(tx.amount.abs(), tx.currency, baseCurrency, exchangeRates, precision)
            }
        }
        
        return transactionCategoryMap.map { (category, txs) ->
            val categoryEntity = categoryMap[category]
            val expense = txs.sumOf { tx ->
                convertAmount(tx.amount.abs(), tx.currency, baseCurrency, exchangeRates, precision)
            }
            val percentage = if (totalExpense > BigDecimal.ZERO) {
                (expense / totalExpense).toFloat()
            } else 0f
            
            // Get category color from database, or use default
            val colorHex = categoryEntity?.colorHex ?: 0xFF6200EE.toInt()
            
            // Check for subcategories
            val hasSubcategories = categoryEntity != null && allCategories.any { it.parentId == categoryEntity.id }
            
            // Compute subcategory data if exists
            val subcategoryData = if (hasSubcategories) {
                val subCategories = allCategories.filter { it.parentId == categoryEntity.id }
                subCategories.map { subCat ->
                    val subTxs = txs.filter { it.category == subCat.name }
                    val subExpense = subTxs.sumOf { tx ->
                        convertAmount(tx.amount, tx.currency, baseCurrency, exchangeRates, precision)
                    }
                    val subPercentage = if (expense > BigDecimal.ZERO) {
                        (subExpense / expense).toFloat()
                    } else 0f
                    
                    CategoryExpenseTreemapData(
                        category = subCat.name,
                        categoryId = subCat.id,
                        expense = subExpense,
                        colorHex = subCat.colorHex,
                        percentage = subPercentage,
                        hasSubcategories = false,
                        subcategoryData = null
                    )
                }
            } else null
            
            CategoryExpenseTreemapData(
                category = category,
                categoryId = categoryEntity?.id,
                expense = expense,
                colorHex = colorHex,
                percentage = percentage,
                hasSubcategories = hasSubcategories,
                subcategoryData = subcategoryData
            )
        }.sortedByDescending { it.expense }
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
     * Helper method to convert amount to base currency.
     * TODO: This should be moved to a dedicated currency conversion service.
     */
    private fun convertAmount(
        amount: BigDecimal,
        fromCurrency: String,
        toCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): BigDecimal {
        if (fromCurrency == toCurrency) return amount
        val rate = exchangeRates[fromCurrency] ?: BigDecimal.ONE
        return (amount * rate).setScale(precision, java.math.RoundingMode.HALF_UP)
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
