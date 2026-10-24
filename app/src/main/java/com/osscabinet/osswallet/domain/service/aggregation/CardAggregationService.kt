package com.osscabinet.ossWallet.domain.service.aggregation

import com.osscabinet.ossWallet.domain.model.LoanStatus
import com.osscabinet.ossWallet.domain.card.CardComputerUtils
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.NetWorthMetrics
import com.osscabinet.ossWallet.domain.card.NetWorthTrend
import com.osscabinet.ossWallet.domain.card.NetWorthTrendPoint
import com.osscabinet.ossWallet.domain.card.resolveEffectiveFilter
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.LoanSummary
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.model.analytics.*
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.domain.service.financial.CategoryTreeMetrics
import com.osscabinet.ossWallet.domain.service.financial.FinancialMetrics
import com.osscabinet.ossWallet.domain.service.financial.FinancialSummary
import com.osscabinet.ossWallet.domain.service.management.BudgetService
import com.osscabinet.ossWallet.domain.service.management.LoanService
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Service for computing card data directly from the database using DatabaseQueryService.
 */
object CardAggregationService {

    suspend fun computeNetWorthCard(
        queryService: DatabaseQueryService,
        profileId: Int,
        input: CardInput,
        config: NetWorthCardConfig
    ): CardRenderData.NetWorth {
        val rawFilter = resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline)
        val currentFilter = rawFilter.copy(
            accountIds = rawFilter.accountIds.ifEmpty { input.effectiveAccountIds }
        )
        val excludeInternal = config.excludeInternalTransfers && config.perspective is AccountingPerspective.CashFlow

        // For absolute balances, we need cumulative totals up to the end of the period.
        val balanceFilter = currentFilter.toBalanceFilter()

        val currentSummariesByCurrency = TransactionAggregationService.getSummaryByCurrency(queryService, profileId, balanceFilter)
            .getOrDefault(emptyMap())

        val netWorth = CurrencyService.calculateTotalInCurrency(
            currencyTotals = currentSummariesByCurrency.mapValues { it.value.netFlow },
            targetCurrency = input.targetCurrency,
            exchangeRates = input.exchangeRates
        )

        val cashFlow = computeCashFlowResult(queryService, profileId, input, currentFilter, excludeInternal)
        val currentPeriodMetrics = NetWorthMetrics(netWorth, cashFlow)

        val previousPeriodMetrics: NetWorthMetrics? = if (config.showComparison) {
            val (prevStart, prevEnd) = TimelineService.getPreviousRange(currentFilter.timelineRange)
            if (prevStart != null && prevEnd != null) {
                val prevFilter = currentFilter.copy(
                    timelineRange = TimelineRange.CustomRange(prevStart, prevEnd)
                )
                val prevBalanceFilter = prevFilter.toBalanceFilter()

                val prevSummariesByCurrency = TransactionAggregationService.getSummaryByCurrency(queryService, profileId, prevBalanceFilter)
                    .getOrDefault(emptyMap())
                val prevNetWorth = CurrencyService.calculateTotalInCurrency(
                    currencyTotals = prevSummariesByCurrency.mapValues { it.value.netFlow },
                    targetCurrency = input.targetCurrency,
                    exchangeRates = input.exchangeRates
                )
                val prevCashFlow = computeCashFlowResult(queryService, profileId, input, prevFilter, excludeInternal)
                NetWorthMetrics(prevNetWorth, prevCashFlow)
            } else null
        } else null

        val trend = if (config.showTrendLine) {
            computeNetWorthTrend(queryService, profileId, input, currentFilter)
        } else emptyList()

        return CardRenderData.NetWorth(
            config = config,
            currentPeriodMetrics = currentPeriodMetrics,
            currency = input.targetCurrency,
            previousPeriodMetrics = previousPeriodMetrics,
            trend = trend
        )
    }

    /**
     * Running net worth across the card's period. Uses the same balance semantics as the headline
     * figure (all accounts in the filter, no category / tag filters, internal transfers included),
     * so the last point matches the figure when the period ends today.
     */
    private suspend fun computeNetWorthTrend(
        queryService: DatabaseQueryService,
        profileId: Int,
        input: CardInput,
        currentFilter: TransactionFilter
    ): List<NetWorthTrendPoint> {
        val balanceFilter = currentFilter.toBalanceFilter()
        val range = currentFilter.timelineRange
        val zone = TimelineService.systemZoneId
        val now = System.currentTimeMillis()

        val (rawStart, rawEnd) = TimelineService.getDateRange(range)
        val end = minOf(rawEnd ?: now, now)
        val start = rawStart ?: run {
            val minTs = queryService.getMinTransactionTimestamp(profileId) ?: return emptyList()
            Instant.ofEpochMilli(minTs).atZone(zone).toLocalDate()
                .atStartOfDay(zone).toInstant().toEpochMilli()
        }
        if (start >= end) return emptyList()

        val buckets = CardComputerUtils.generateBuckets(
            start, end, CardComputerUtils.resolveGranularity(start, end)
        )
        if (buckets.size < 2) return emptyList()

        val openingByCurrency: Map<String, BigDecimal> = if (rawStart == null) emptyMap() else {
            TransactionAggregationService.getSummaryByCurrency(
                queryService, profileId,
                balanceFilter.copy(
                    timelineRange = TimelineRange.CustomRange(end = start - 1, excludeFuture = range.excludeFuture)
                )
            ).getOrDefault(emptyMap()).mapValues { it.value.netFlow }
        }

        val dailyNet: Map<String, Map<String, BigDecimal>> = TransactionAggregationService.getSummaryByCurrencyByDay(
            queryService, profileId,
            balanceFilter.copy(timelineRange = TimelineRange.CustomRange(start, end, excludeFuture = range.excludeFuture)),
            false
        ).getOrDefault(emptyMap()).mapValues { (_, byCurrency) -> byCurrency.mapValues { it.value.netFlow } }

        return NetWorthTrend.build(
            buckets = buckets,
            openingByCurrency = openingByCurrency,
            dailyNetByCurrency = dailyNet,
            targetCurrency = input.targetCurrency,
            exchangeRates = input.exchangeRates
        )
    }

    suspend fun computeCashFlowCard(
        queryService: DatabaseQueryService,
        profileId: Int,
        input: CardInput,
        config: CashFlowCardConfig
    ): CardRenderData.CashFlow {
        val rawFilter = resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline)
        val mergedFilter = rawFilter.copy(
            accountIds = rawFilter.accountIds.ifEmpty { input.effectiveAccountIds }
        )
        val excludeInternal = config.excludeInternalTransfers && config.perspective is AccountingPerspective.CashFlow
        val result = computeCashFlowResult(queryService, profileId, input, mergedFilter, excludeInternal)

        return CardRenderData.CashFlow(
            config = config,
            result = result,
            currency = input.targetCurrency
        )
    }

    private suspend fun computeCashFlowResult(
        queryService: DatabaseQueryService,
        profileId: Int,
        input: CardInput,
        filter: TransactionFilter,
        excludeInternalTransfers: Boolean = false
    ): CashFlowResult {
        val summariesByCurrency = TransactionAggregationService.getSummaryByCurrency(queryService, profileId, filter, excludeInternalTransfers)
            .getOrDefault(emptyMap())

        if (summariesByCurrency.isEmpty()) return CashFlowResult.empty()

        val breakdownItems = summariesByCurrency.map { (curr, summary) ->
            val metricsInBase = CurrencyService.convertFinancialSummaryToBase(
                summary, curr, input.targetCurrency, input.exchangeRates
            ).setScale(CurrencyService.precision)

            CashFlowBreakdownItem(
                currency = curr,
                metrics = summary.setScale(CurrencyService.precision),
                metricsInBase = metricsInBase
            )
        }

        val totalMetricsInBase = breakdownItems.fold(FinancialSummary.EMPTY) { acc, item ->
            acc + item.metricsInBase
        }

        return CashFlowResult(
            metricsInBase = totalMetricsInBase,
            breakdown = breakdownItems,
            filteredTransactions = emptyList()
        )
    }

    suspend fun computeCurrencyBalancesCard(
        queryService: DatabaseQueryService,
        profileId: Int,
        input: CardInput,
        config: CurrencyBalancesCardConfig
    ): CardRenderData.CurrencyBalances {
        val rawFilter = resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline)
        val mergedFilter = rawFilter.copy(
            accountIds = rawFilter.accountIds.ifEmpty { input.effectiveAccountIds }
        )
        val balanceFilter = mergedFilter.toBalanceFilter()

        val summariesByCurrency = TransactionAggregationService.getSummaryByCurrency(queryService, profileId, balanceFilter)
            .getOrDefault(emptyMap())

        val balances = summariesByCurrency.map { (curr, summary) ->
            CurrencyAggregationItem(
                currency = curr,
                amount = summary.netFlow.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                amountInBase = CurrencyService.convertAmount(summary.netFlow, curr, input.targetCurrency, input.exchangeRates, 10)
                    .setScale(CurrencyService.precision, RoundingMode.HALF_UP)
            )
        }.let { items ->
            val visible = if (config.hideZeroBalances) items.filter { it.amount.signum() != 0 } else items
            when (config.sortBy) {
                CurrencySortMode.BIGGEST -> visible.sortedByDescending { it.amountInBase.abs() }
                CurrencySortMode.SMALLEST -> visible.sortedBy { it.amountInBase.abs() }
                CurrencySortMode.NAME -> visible.sortedBy { it.currency }
            }
        }

        return CardRenderData.CurrencyBalances(
            config = config,
            items = balances,
            currency = input.targetCurrency,
            exchangeRates = input.exchangeRates
        )
    }

    suspend fun computeAccountsCard(
        queryService: DatabaseQueryService,
        profileId: Int,
        input: CardInput,
        config: AccountsCardConfig
    ): CardRenderData.Accounts {
        val currentFilter = resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline)
        
        val targetAccounts = if (currentFilter.accountIds.isNotEmpty()) {
            input.accounts.filter { it.id in currentFilter.accountIds }
        } else {
            input.effectiveAccounts
        }

        val excludeInternal = config.excludeInternalTransfers && config.perspective is AccountingPerspective.CashFlow

        val (prevStart, prevEnd) = if (config.showComparison)
            TimelineService.getPreviousRange(currentFilter.timelineRange)
        else null to null
        val prevFilter = if (prevStart != null && prevEnd != null)
            currentFilter.copy(timelineRange = TimelineRange.CustomRange(prevStart, prevEnd))
        else null

        val accountSummaries = supervisorScope {
            val currentDeferred = async {
                computeAccountStats(queryService, profileId, input, currentFilter, targetAccounts, excludeInternal)
            }
            val previousDeferred = if (prevFilter != null) async {
                runCatching {
                    computeAccountStats(queryService, profileId, input, prevFilter, targetAccounts, excludeInternal)
                }.getOrNull()
            } else null

            val current = currentDeferred.await()
            val previous = previousDeferred?.await()
            if (previous != null) {
                current.mapValues { (id, summary) ->
                    summary.copy(previousPeriodMetrics = previous[id]?.currentPeriodMetrics)
                }
            } else current
        }

        return CardRenderData.Accounts(
            config = config,
            updatedAccounts = targetAccounts,
            accountSummaries = accountSummaries,
            currency = input.targetCurrency,
            currentTheme = "light",
            exchangeRates = input.exchangeRates
        )
    }

    private fun signedSpent(totalAmount: BigDecimal, budget: Budget): BigDecimal {
        val isIncomeOnly = budget.trackTypes.isNotEmpty() && budget.trackTypes.all { it == TransactionType.INCOME }
        return if (isIncomeOnly) totalAmount else totalAmount.negate()
    }

    suspend fun computeBudgetsCard(
        queryService: DatabaseQueryService,
        profileId: Int,
        input: CardInput,
        config: BudgetsCardConfig
    ): CardRenderData.Budgets {
        val now = System.currentTimeMillis()

        val baseBudgetFilter = input.filter.copy(timelineRange = TimelineRange.AllTime())
        val mergedFilter = resolveEffectiveFilter(
            baseBudgetFilter,
            config.filter,
            config.filterRules,
            input.screenOwnsTimeline
        )
        val effectiveFilter = mergedFilter.copy(
            accountIds = mergedFilter.accountIds.ifEmpty { input.effectiveAccountIds }
        )

        val baseBudgets = if (config.showArchived) input.budgets
        else input.budgets.filter { !it.isArchived }

        val accountIdSet = mergedFilter.accountIds.toHashSet()
        val categoryIdSet = mergedFilter.categoryIds.toHashSet()
        val currencySet = mergedFilter.currencies.toHashSet()
        val tagSet = mergedFilter.tags.toHashSet()

        val budgets = baseBudgets.filter { budget ->
            (accountIdSet.isEmpty() || budget.accountIds.isEmpty() || budget.accountIds.any { it in accountIdSet }) &&
                    (categoryIdSet.isEmpty() || budget.categoryIds.isEmpty() || budget.categoryIds.any { it in categoryIdSet }) &&
                    (currencySet.isEmpty() || budget.currency in currencySet) &&
                    (tagSet.isEmpty() || budget.tags.isEmpty() || budget.tags.any { it in tagSet })
        }

        // Sorting needs every budget's progress, so only a saved-order card can stop early.
        val candidates = if (config.sortBy == BudgetSortMode.SAVED) budgets.take(config.maxItems) else budgets
        val progress = candidates.map { budget ->
            // Spent never counts future-dated transactions, regardless of the dashboard's own
            // filter settings — mirrors BudgetService.calculateSpent's in-memory behavior.
            val budgetFilter = BudgetService.buildTransactionFilter(budget, now, input.categories, excludeFuture = true)
            val finalFilter = budgetFilter.copy(
                accountIds = if (budget.accountIds.isEmpty()) effectiveFilter.accountIds else budget.accountIds
            )

            // Current spent
            val spentSummaries = TransactionAggregationService.getSummaryByCurrency(queryService, profileId, finalFilter)
                .getOrDefault(emptyMap())
            val spentSummary = CurrencyService.calculateTotalFinancialSummary(spentSummaries, budget.currency, input.exchangeRates)
            val spent = signedSpent(spentSummary.netFlow, budget)

            // Future-dated transactions matching this budget's scope, within its current period —
            // derived as (whole period) - (past-only), since both share the same sign convention.
            val allFilter = finalFilter.copy(timelineRange = finalFilter.timelineRange.withExcludeFuture(false))
            val allSummaries = TransactionAggregationService.getSummaryByCurrency(queryService, profileId, allFilter)
                .getOrDefault(emptyMap())
            val allSummary = CurrencyService.calculateTotalFinancialSummary(allSummaries, budget.currency, input.exchangeRates)
            val allSigned = signedSpent(allSummary.netFlow, budget)
            val futureTransactionsAmount = allSigned.subtract(spent).coerceAtLeast(BigDecimal.ZERO)

            // Due planned payments matching this budget's scope, within its current period.
            val plannedPaymentsAmount = BudgetService.calculatePlannedPaymentsAmount(
                budget, input.plannedPayments, input.exchangeRates, budget.currency, now, input.categories
            )
            val planned = futureTransactionsAmount.add(plannedPaymentsAmount)

            val rollover = if (budget.rollover) {
                computeBudgetRollover(queryService, profileId, budget, input, effectiveFilter.accountIds, now, effectiveFilter.timelineRange.excludeFuture)
            } else BigDecimal.ZERO

            BudgetService.buildProgress(budget, spent, rollover, BudgetService.elapsedFraction(budget, now), planned)
        }

        val items = when (config.sortBy) {
            BudgetSortMode.SAVED -> progress
            BudgetSortMode.MOST_USED -> progress.sortedByDescending { it.percentage }
            BudgetSortMode.AHEAD_OF_PACE -> progress.sortedByDescending { it.percentage - (it.elapsedFraction ?: it.percentage) }
        }.take(config.maxItems)

        return CardRenderData.Budgets(
            config = config,
            items = items,
            currency = input.targetCurrency
        )
    }

    private suspend fun computeBudgetRollover(
        queryService: DatabaseQueryService,
        profileId: Int,
        budget: Budget,
        input: CardInput,
        effectiveAccountIds: Set<String>,
        now: Long,
        excludeFuture: Boolean
    ): BigDecimal {
        val periodRanges = mutableListOf<Pair<Long, Long>>()
        var referenceNow = now
        var rangeForPrev = budget.timelineRange
        repeat(12) {
            val (prevStart, prevEnd) = TimelineService.getPreviousRange(rangeForPrev, referenceNow)
            if (prevStart == null || prevEnd == null) return@repeat
            periodRanges.add(prevStart to prevEnd)
            rangeForPrev = TimelineRange.CustomRange(prevStart, prevEnd)
            referenceNow = (prevStart + prevEnd) / 2
        }
        if (periodRanges.isEmpty()) return BigDecimal.ZERO

        val spentByPeriod: List<BigDecimal> = coroutineScope {
            periodRanges.map { (start, end) ->
                async {
                    val filter = BudgetService.buildTransactionFilter(
                        budget = budget.copy(timelineRange = TimelineRange.CustomRange(start, end)),
                        now = (start + end) / 2,
                        allCategories = input.categories,
                        excludeFuture = excludeFuture
                    ).copy(accountIds = if (budget.accountIds.isEmpty()) effectiveAccountIds else budget.accountIds)
                    val summaries = TransactionAggregationService.getSummaryByCurrency(queryService, profileId, filter)
                        .getOrDefault(emptyMap())
                    signedSpent(CurrencyService.calculateTotalFinancialSummary(summaries, budget.currency, input.exchangeRates).netFlow, budget)
                }
            }.awaitAll()
        }

        var rollover = BigDecimal.ZERO
        for (spent in spentByPeriod.reversed()) {
            rollover = budget.limitAmount.add(rollover).subtract(spent).max(BigDecimal.ZERO)
        }
        return rollover
    }

    suspend fun computeLoansCard(
        queryService: DatabaseQueryService,
        profileId: Int,
        input: CardInput,
        config: LoansCardConfig
    ): CardRenderData.Loans {
        val mergedFilter = resolveEffectiveFilter(
            input.filter,
            config.filter,
            config.filterRules,
            input.screenOwnsTimeline
        )
        val now = System.currentTimeMillis()

        val baseFiltered = when (config.showType) {
            "LENT" -> input.loans.filter { it.loanType == LoanType.LENT }
            "BORROWED" -> input.loans.filter { it.loanType == LoanType.BORROWED }
            else -> input.loans
        }

        val filtered = baseFiltered.filter { loan ->
            (config.showSettled || loan.status == LoanStatus.ACTIVE) &&
                    (mergedFilter.accountIds.isEmpty() || loan.linkedAccountId.isBlank() || loan.linkedAccountId in mergedFilter.accountIds) &&
                    (mergedFilter.currencies.isEmpty() || loan.currency in mergedFilter.currencies)
        }

        // Order before the cut so the rows shown are the ones the user asked for. Only "most owed"
        // needs every loan's summary; the other orders work from the loans themselves.
        val ordered = when (config.sortBy) {
            LoanSortMode.DUE_SOONEST -> LoanService.sortByDueDate(filtered)
            LoanSortMode.NAME -> LoanService.sortByName(filtered)
            else -> filtered
        }
        val toSummarize = if (config.sortBy == LoanSortMode.MOST_OWED) ordered else ordered.take(config.maxItems)

        val summaries = toSummarize.map { loan: Loan ->
            val loanFilter = LoanService.buildTransactionFilter(loan, input.categories)
            
            val summaryByCurrency = TransactionAggregationService.getSummaryByCurrency(queryService, profileId, loanFilter)
                .getOrDefault(emptyMap())

            val totalPaid = summaryByCurrency.entries.fold(BigDecimal.ZERO) { acc, entry ->
                val (curr, summary) = entry
                val repaymentAmount = when (loan.loanType) {
                    LoanType.LENT -> summary.inflow.abs()
                    LoanType.BORROWED -> summary.outflow.abs()
                }
                val converted = CurrencyService.convertAmount(repaymentAmount, curr, loan.currency, input.exchangeRates)
                acc + converted
            }

            val remaining = (loan.principalAmount - totalPaid).coerceAtLeast(BigDecimal.ZERO)
            val interest = LoanService.calculateInterestAccrued(loan, now)

            LoanSummary(
                loan = loan,
                totalPaid = totalPaid.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                remainingBalance = remaining.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                interestAccrued = interest.setScale(CurrencyService.precision, RoundingMode.HALF_UP)
            )
        }
        val items = if (config.sortBy == LoanSortMode.MOST_OWED) {
            LoanService.sortByOwed(summaries, input.targetCurrency, input.exchangeRates).take(config.maxItems)
        } else summaries
        return CardRenderData.Loans(
            config = config,
            items = items,
            currency = input.targetCurrency,
            exchangeRates = input.exchangeRates
        )
    }

    suspend fun computeRecentActivitiesCard(
        queryService: DatabaseQueryService,
        profileId: Int,
        input: CardInput,
        config: RecentActivitiesCardConfig
    ): CardRenderData.RecentActivities {
        val mergedFilter = resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline)
        val effectiveFilter = mergedFilter.copy(
            accountIds = mergedFilter.accountIds.ifEmpty { input.effectiveAccountIds }
        )
        val txs = queryService.observeFilteredTransactions(profileId, effectiveFilter, limit = 200).first()

        val startingBalances: Map<String, BigDecimal> = if (txs.isNotEmpty()) {
            val windowStart = txs.minOf { it.timestamp }
            val balanceFilter = effectiveFilter.toBalanceFilter().copy(
                timelineRange = TimelineRange.CustomRange(end = windowStart - 1)
            )
            TransactionAggregationService.getSummaryByAccountAndCurrency(
                queryService, profileId, balanceFilter
            ).getOrDefault(emptyMap()).mapValues { (accountId, byCurrency) ->
                val accountCurrency = input.accounts.find { it.id == accountId }?.currencyCode ?: input.targetCurrency
                CurrencyService.calculateTotalFinancialSummary(byCurrency, accountCurrency, input.exchangeRates).netFlow
            }
        } else emptyMap()

        val transactionsWithBalance = computeRecentBalances(txs, input.accounts, input.targetCurrency, input.exchangeRates, startingBalances)

        val groups = TimelineService.getGroupedData(
            transactions = transactionsWithBalance.take(20),
            absoluteHistory = transactionsWithBalance,
            groupingType = config.groupBy,
            accounts = input.accounts,
            categories = input.categories,
            allTags = input.tags,
            baseCurrency = input.targetCurrency,
            exchangeRates = input.exchangeRates,
            allTxnBreakdown = config.allTxnBreakdown,
        )

        return CardRenderData.RecentActivities(
            config = config,
            listState = TransactionListState(groups = groups, isEmpty = groups.isEmpty()),
            currency = input.targetCurrency,
            exchangeRates = input.exchangeRates
        )
    }

    suspend fun computeCategoryPieChartCard(
        queryService: DatabaseQueryService,
        profileId: Int,
        input: CardInput,
        config: CategoryPieChartCardConfig
    ): CardRenderData.CategoryPieChart {
        val (tree, metrics, prevMetrics) = computeCategoryChartData(queryService, profileId, input, config.filter, config.filterRules, config.perspective, config.showComparison, config.excludeInternalTransfers)
        return CardRenderData.CategoryPieChart(
            config = config,
            items = tree,
            currency = input.targetCurrency,
            metrics = metrics,
            previousMetrics = prevMetrics
        )
    }

    suspend fun computeCategoryTreemapCard(
        queryService: DatabaseQueryService,
        profileId: Int,
        input: CardInput,
        config: CategoryTreemapCardConfig
    ): CardRenderData.CategoryTreemap {
        val (tree, metrics, prevMetrics) = computeCategoryChartData(queryService, profileId, input, config.filter, config.filterRules, config.perspective, config.showComparison, config.excludeInternalTransfers)
        return CardRenderData.CategoryTreemap(
            config = config,
            items = tree,
            currency = input.targetCurrency,
            metrics = metrics,
            previousMetrics = prevMetrics
        )
    }

    private data class CategoryChartResult(
        val tree: List<CategoryTreeMetrics>,
        val metrics: FinancialSummary,
        val previousMetrics: FinancialSummary?
    )

    private suspend fun computeCategoryChartData(
        queryService: DatabaseQueryService,
        profileId: Int,
        input: CardInput,
        cardFilter: TransactionFilter?,
        filterRules: com.osscabinet.ossWallet.domain.card.CardFilterRules,
        perspective: AccountingPerspective,
        showComparison: Boolean,
        excludeInternalTransfers: Boolean
    ): CategoryChartResult {
        val mergedFilter = resolveEffectiveFilter(input.filter, cardFilter ?: TransactionFilter(), filterRules, input.screenOwnsTimeline)
        val expandedCategoryIds = input.categoryHierarchy.expand(mergedFilter.categoryIds)
        val excludeInternal = excludeInternalTransfers && perspective is AccountingPerspective.CashFlow
        val finalFilter = mergedFilter.copy(
            accountIds = mergedFilter.accountIds.ifEmpty { input.effectiveAccountIds },
            categoryIds = expandedCategoryIds,
            transactionTypes = emptySet()
        )
        val tree = computeCategoryTree(queryService, profileId, input, finalFilter, perspective, showComparison, excludeInternal)
        
        val metrics = tree.fold(FinancialSummary.EMPTY) { acc, item -> acc + item.metrics }
        val prevMetrics = if (showComparison) {
            tree.fold(FinancialSummary.EMPTY) { acc, item -> acc + (item.previousMetrics ?: FinancialSummary.EMPTY) }
        } else null
        
        return CategoryChartResult(tree, metrics, prevMetrics)
    }

    private suspend fun computeCategoryTree(
        queryService: DatabaseQueryService,
        profileId: Int,
        input: CardInput,
        filter: TransactionFilter,
        perspective: AccountingPerspective,
        showComparison: Boolean,
        excludeInternalTransfers: Boolean = false
    ): List<CategoryTreeMetrics> {
        val currentSummaries = TransactionAggregationService.getSummaryByCategoryAndCurrency(queryService, profileId, filter, excludeInternalTransfers)
            .getOrDefault(emptyMap())

        val previousSummaries = if (showComparison && filter.timelineRange !is TimelineRange.AllTime) {
            val (prevStart, prevEnd) = TimelineService.getPreviousRange(filter.timelineRange)
            if (prevStart != null && prevEnd != null) {
                val prevFilter = filter.copy(timelineRange = TimelineRange.CustomRange(prevStart, prevEnd))
                TransactionAggregationService.getSummaryByCategoryAndCurrency(queryService, profileId, prevFilter, excludeInternalTransfers)
                    .getOrDefault(emptyMap())
            } else emptyMap()
        } else emptyMap()

        fun toBreakdowns(summaries: Map<Long, Map<String, FinancialSummary>>): Map<Long, FinancialMetrics<Long>> =
            summaries.mapValues { (categoryId, byCurrency) ->
                val converted = byCurrency.mapValues { (curr, summary) ->
                    CurrencyService.convertFinancialSummaryToBase(summary, curr, input.targetCurrency, input.exchangeRates)
                }
                FinancialMetrics(
                    key = categoryId,
                    metrics = FinancialSummary.aggregate(converted.values),
                    currencyBreakdown = converted
                )
            }

        return AggregationService.buildCategoryTreeFromBreakdowns(
            categoryBreakdowns = toBreakdowns(currentSummaries),
            previousBreakdowns = if (showComparison) toBreakdowns(previousSummaries) else null,
            allCategories = input.categories
        )
    }

    fun computeRecentBalances(
        transactions: List<Transaction>,
        accounts: List<Account>,
        baseCurrency: String,
        rates: ExchangeRateSnapshot,
        startingBalances: Map<String, BigDecimal> = emptyMap()
    ): List<TransactionWithBalance> {
        val accountById = accounts.associateBy { it.id }
        return transactions.asSequence()
            .sortedBy { it.timestamp }
            .groupBy { it.accountId }
            .flatMap { entry ->
                val (accId, accountTransactions) = entry
                val account = accountById[accId]
                val accountCurrency = account?.currencyCode ?: baseCurrency

                var runningBalance = startingBalances[accId] ?: BigDecimal.ZERO
                accountTransactions.map { transaction ->
                    val txAmountInAccountCurrency = CurrencyService.convertAmount(
                        amount = transaction.amount,
                        fromCurrency = transaction.currency,
                        toCurrency = accountCurrency,
                        exchangeRates = rates
                    )

                    val balanceBefore = runningBalance
                    runningBalance = runningBalance.add(txAmountInAccountCurrency)
                    TransactionWithBalance(
                        transaction = transaction,
                        balanceBefore = balanceBefore.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                        balanceAfter = runningBalance.setScale(CurrencyService.precision, RoundingMode.HALF_UP)
                    )
                }
            }
            .sortedByDescending { it.transaction.timestamp }
            .toList()
    }

    private suspend fun computeAccountStats(
        queryService: DatabaseQueryService,
        profileId: Int,
        input: CardInput,
        filter: TransactionFilter,
        accounts: List<Account>,
        excludeInternalTransfers: Boolean = false
    ): Map<String, AccountSummaryItem> = supervisorScope {
        val balanceFilter = filter.toBalanceFilter()

        val balanceDeferred = async {
            TransactionAggregationService.getSummaryByAccountAndCurrency(queryService, profileId, balanceFilter)
                .getOrDefault(emptyMap())
        }
        val periodDeferred = async {
            TransactionAggregationService.getSummaryByAccountAndCurrency(queryService, profileId, filter, excludeInternalTransfers)
                .getOrDefault(emptyMap())
        }

        val summariesByAccountAndCurrencyBalance = balanceDeferred.await()
        val summariesByAccountAndCurrencyPeriod = periodDeferred.await()

        accounts.associate { account ->
            val balanceSummaries = summariesByAccountAndCurrencyBalance[account.id] ?: emptyMap()
            val periodSummaries = summariesByAccountAndCurrencyPeriod[account.id] ?: emptyMap()
            
            val totalBalanceSummaryInAccountCurrency = CurrencyService.calculateTotalFinancialSummary(
                balanceSummaries,
                account.currencyCode,
                input.exchangeRates
            )
            
            val totalPeriodSummaryInAccountCurrency = CurrencyService.calculateTotalFinancialSummary(
                periodSummaries,
                account.currencyCode,
                input.exchangeRates
            )

            val balanceInBase = CurrencyService.calculateTotalInCurrency(
                currencyTotals = balanceSummaries.mapValues { it.value.netFlow },
                targetCurrency = input.targetCurrency,
                exchangeRates = input.exchangeRates,
                precision = 10
            )

            val currencyBalances = balanceSummaries.mapValues { it.value.netFlow }

            account.id to AccountSummaryItem(
                account = account,
                currencyBalances = currencyBalances,
                balance = totalBalanceSummaryInAccountCurrency.netFlow,
                balanceInBase = balanceInBase,
                currentPeriodMetrics = totalPeriodSummaryInAccountCurrency
            )
        }
    }

    suspend fun computeAccountHeroCard(
        queryService: DatabaseQueryService,
        profileId: Int,
        input: CardInput,
        config: AccountHeroCardConfig
    ): CardRenderData.AccountHero {
        val targetAccountId = input.filter.accountIds.firstOrNull()
            ?: config.filter.accountIds.firstOrNull()
            ?: error("AccountHeroCard requires an account scope")

        val account = input.accounts.find { it.id == targetAccountId }
            ?: error("Account not found: $targetAccountId")

        val periodFilter = resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline)
        val excludeInternal = config.excludeInternalTransfers && config.perspective is AccountingPerspective.CashFlow
        
        val baseFilter = TransactionFilter(
            accountIds = setOf(targetAccountId),
            timelineRange = TimelineRange.AllTime(excludeFuture = periodFilter.timelineRange.excludeFuture)
        )
        val lifetimeSummariesByCurrency = TransactionAggregationService.getSummaryByCurrency(queryService, profileId, baseFilter)
            .getOrDefault(emptyMap())

        val lifetimeSummary = CurrencyService.calculateTotalFinancialSummary(
            lifetimeSummariesByCurrency,
            account.currencyCode,
            input.exchangeRates
        )
        val periodSummariesByCurrency = TransactionAggregationService.getSummaryByCurrency(queryService, profileId, periodFilter, excludeInternal)
            .getOrDefault(emptyMap())

        val periodSummary = CurrencyService.calculateTotalFinancialSummary(
            periodSummariesByCurrency,
            account.currencyCode,
            input.exchangeRates
        ).setScale(CurrencyService.precision)

        var previousMetrics: FinancialSummary? = null
        if (config.showComparison) {
            val (prevStart, prevEnd) = TimelineService.getPreviousRange(periodFilter.timelineRange)
            if (prevStart != null && prevEnd != null) {
                val prevFilter = periodFilter.copy(
                    timelineRange = TimelineRange.CustomRange(prevStart, prevEnd)
                )
                val prevSummariesByCurrency = TransactionAggregationService.getSummaryByCurrency(queryService, profileId, prevFilter, excludeInternal)
                    .getOrDefault(emptyMap())
                previousMetrics = CurrencyService.calculateTotalFinancialSummary(
                    prevSummariesByCurrency,
                    account.currencyCode,
                    input.exchangeRates
                ).setScale(CurrencyService.precision)
            }
        }

        val balancesByCurrency = lifetimeSummariesByCurrency
            .mapValues { it.value.netFlow.setScale(CurrencyService.precision, RoundingMode.HALF_UP) }

        val balanceHistory = computeAccountBalanceHistory(
            queryService = queryService,
            profileId = profileId,
            accountId = targetAccountId,
            accountCurrency = account.currencyCode,
            periodFilter = periodFilter,
            exchangeRates = input.exchangeRates,
            excludeInternal = excludeInternal
        )

        val summary = AccountSummaryItem(
            account = account,
            currencyBalances = balancesByCurrency,
            balance = lifetimeSummary.netFlow.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
            balanceInBase = CurrencyService.calculateTotalInCurrency(
                currencyTotals = balancesByCurrency,
                targetCurrency = input.targetCurrency,
                exchangeRates = input.exchangeRates,
                precision = 10
            ),
            currentPeriodMetrics = periodSummary,
            previousPeriodMetrics = previousMetrics,
            balanceHistory = balanceHistory
        )

        return CardRenderData.AccountHero(
            config = config,
            summary = summary,
            currency = input.targetCurrency,
            exchangeRates = input.exchangeRates
        )
    }

    private suspend fun computeAccountBalanceHistory(
        queryService: DatabaseQueryService,
        profileId: Int,
        accountId: String,
        accountCurrency: String,
        periodFilter: TransactionFilter,
        exchangeRates: ExchangeRateSnapshot,
        excludeInternal: Boolean,
    ): List<BigDecimal> {
        val zone = TimelineService.systemZoneId
        val (rawStart, rawEnd) = TimelineService.getDateRange(periodFilter.timelineRange)
        val end = rawEnd ?: System.currentTimeMillis()
        val start = rawStart ?: run {
            val minTs = queryService.getMinTransactionTimestamp(profileId) ?: return emptyList()
            Instant.ofEpochMilli(minTs).atZone(zone).toLocalDate()
                .atStartOfDay(zone).toInstant().toEpochMilli()
        }
        if (start >= end) return emptyList()

        val granularity = CardComputerUtils.resolveGranularity(start, end)
        val buckets = CardComputerUtils.generateBuckets(start, end, granularity)
        if (buckets.size < 2) return emptyList()

        val opening = if (rawStart == null) BigDecimal.ZERO else {
            val beforeFilter = TransactionFilter(
                accountIds = setOf(accountId),
                timelineRange = TimelineRange.CustomRange(
                    end = start - 1,
                    excludeFuture = periodFilter.timelineRange.excludeFuture
                )
            )
            val beforeByCcy = TransactionAggregationService
                .getSummaryByCurrency(queryService, profileId, beforeFilter)
                .getOrDefault(emptyMap())
            CurrencyService.calculateTotalFinancialSummary(beforeByCcy, accountCurrency, exchangeRates)
                .netFlow
        }

        val dailyData = TransactionAggregationService.getSummaryByCurrencyByDay(
            queryService, profileId,
            periodFilter.copy(timelineRange = TimelineRange.CustomRange(start, end)),
            excludeInternal
        ).getOrDefault(emptyMap())

        val dayFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val bucketStarts = buckets.map { it.first }
        val perBucketDelta = Array(buckets.size) { BigDecimal.ZERO }

        val anchor = exchangeRates.anchor
        val toRate = if (accountCurrency == anchor) BigDecimal.ONE else exchangeRates.rates[accountCurrency]
        fun factor(ccy: String): BigDecimal? {
            if (ccy == accountCurrency) return BigDecimal.ONE
            val fromRate = if (ccy == anchor) BigDecimal.ONE else exchangeRates.rates[ccy]
            if (fromRate == null || toRate == null || fromRate.signum() == 0) return null
            return toRate.divide(fromRate, 14, RoundingMode.HALF_UP)
        }

        dailyData.forEach { (dayKey, byCcy) ->
            val dayStart = try {
                LocalDate.parse(dayKey, dayFmt).atStartOfDay(zone).toInstant().toEpochMilli()
            } catch (_: Exception) {
                return@forEach
            }
            val idx = bucketStarts.indexOfLast { it <= dayStart }
            if (idx < 0) return@forEach
            var delta = BigDecimal.ZERO
            byCcy.forEach { (ccy, summary) ->
                val f = factor(ccy) ?: return@forEach
                delta = delta.add(summary.netFlow.multiply(f))
            }
            perBucketDelta[idx] = perBucketDelta[idx].add(delta)
        }

        var running = opening
        return buckets.indices.map { i ->
            running = running.add(perBucketDelta[i])
            running.setScale(CurrencyService.precision, RoundingMode.HALF_UP)
        }
    }

    private fun TransactionFilter.toBalanceFilter(): TransactionFilter {
        val baseFilter = this.copy(
            categoryIds = emptySet(),
            tags = emptySet(),
            currencies = emptySet(),
            paymentMethods = emptySet(),
            statuses = emptySet()
        )
        return when (val range = baseFilter.timelineRange) {
            is TimelineRange.AllTime -> baseFilter
            else -> {
                val (_, end) = TimelineService.getDateRange(range)
                baseFilter.copy(
                    timelineRange = if (end != null) TimelineRange.CustomRange(end = end, excludeFuture = range.excludeFuture)
                                   else TimelineRange.AllTime(excludeFuture = range.excludeFuture)
                )
            }
        }
    }
}
