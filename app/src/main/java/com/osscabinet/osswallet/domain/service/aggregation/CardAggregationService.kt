package com.osscabinet.ossWallet.domain.service.aggregation

import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.NetWorthMetrics
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
        // excludeInternalTransfers is never applied to balance queries — balance reflects all transactions.
        val balanceFilter = currentFilter.toBalanceFilter()

        val currentTotalsByCurrency = TransactionAggregationService.getSummaryByCurrency(queryService, profileId, balanceFilter)
            .getOrDefault(emptyMap())
            .mapValues { it.value.totalAmount }

        val netWorth = CurrencyService.calculateTotalInCurrency(
            currencyTotals = currentTotalsByCurrency,
            targetCurrency = input.targetCurrency,
            exchangeRates = input.exchangeRates
        )

        val cashFlow = computeCashFlowResult(queryService, profileId, input, currentFilter, excludeInternal).toPerspective(config.perspective)
        val currentPeriodMetrics = NetWorthMetrics(netWorth, cashFlow)

        val previousPeriodMetrics: NetWorthMetrics? = if (config.showComparison) {
            val (prevStart, prevEnd) = TimelineService.getPreviousRange(currentFilter.timelineRange)
            if (prevStart != null && prevEnd != null) {
                val prevFilter = currentFilter.copy(
                    timelineRange = TimelineRange.CustomRange(prevStart, prevEnd)
                )
                val prevBalanceFilter = prevFilter.toBalanceFilter()

                val prevTotalsByCurrency = TransactionAggregationService.getSummaryByCurrency(queryService, profileId, prevBalanceFilter)
                    .getOrDefault(emptyMap())
                    .mapValues { it.value.totalAmount }
                val prevNetWorth = CurrencyService.calculateTotalInCurrency(
                    currencyTotals = prevTotalsByCurrency,
                    targetCurrency = input.targetCurrency,
                    exchangeRates = input.exchangeRates
                )
                val prevCashFlow = computeCashFlowResult(queryService, profileId, input, prevFilter, excludeInternal).toPerspective(config.perspective)
                NetWorthMetrics(prevNetWorth, prevCashFlow)
            } else null
        } else null

        return CardRenderData.NetWorth(
            config = config,
            currentPeriodMetrics = currentPeriodMetrics,
            currency = input.targetCurrency,
            previousPeriodMetrics = previousPeriodMetrics
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
        val result = computeCashFlowResult(queryService, profileId, input, mergedFilter, excludeInternal).toPerspective(config.perspective)

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

        var totalIncomeInBase = BigDecimal.ZERO
        var totalExpenseInBase = BigDecimal.ZERO
        var totalTransferInBase = BigDecimal.ZERO
        var totalTransferInInBase = BigDecimal.ZERO
        var totalTransferOutInBase = BigDecimal.ZERO
        var totalCrossCcyTransferInInBase = BigDecimal.ZERO
        var totalCrossCcyTransferOutInBase = BigDecimal.ZERO

        val breakdownItems = summariesByCurrency.map { (curr, summary) ->
            val incomeInBase = CurrencyService.convertAmount(summary.income, curr, input.targetCurrency, input.exchangeRates, 10)
            val expenseInBase = CurrencyService.convertAmount(summary.expense, curr, input.targetCurrency, input.exchangeRates, 10)
            val transferInBase = CurrencyService.convertAmount(summary.transfer, curr, input.targetCurrency, input.exchangeRates, 10)
            val transferInInBase = CurrencyService.convertAmount(summary.transferIn, curr, input.targetCurrency, input.exchangeRates, 10)
            val transferOutInBase = CurrencyService.convertAmount(summary.transferOut, curr, input.targetCurrency, input.exchangeRates, 10)
            val crossCcyInInBase = CurrencyService.convertAmount(summary.crossCcyTransferIn, curr, input.targetCurrency, input.exchangeRates, 10)
            val crossCcyOutInBase = CurrencyService.convertAmount(summary.crossCcyTransferOut, curr, input.targetCurrency, input.exchangeRates, 10)

            totalIncomeInBase = totalIncomeInBase.add(incomeInBase)
            totalExpenseInBase = totalExpenseInBase.add(expenseInBase)
            totalTransferInBase = totalTransferInBase.add(transferInBase)
            totalTransferInInBase = totalTransferInInBase.add(transferInInBase)
            totalTransferOutInBase = totalTransferOutInBase.add(transferOutInBase)
            totalCrossCcyTransferInInBase = totalCrossCcyTransferInInBase.add(crossCcyInInBase)
            totalCrossCcyTransferOutInBase = totalCrossCcyTransferOutInBase.add(crossCcyOutInBase)

            CashFlowBreakdownItem(
                currency = curr,
                income = summary.income.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                incomeInBase = incomeInBase.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                expense = summary.expense.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                expenseInBase = expenseInBase.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                transfer = summary.transfer.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                transferInBase = transferInBase.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                transferIn = summary.transferIn.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                transferInInBase = transferInInBase.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                transferOut = summary.transferOut.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                transferOutInBase = transferOutInBase.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                crossCcyTransferIn = summary.crossCcyTransferIn.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                crossCcyTransferInInBase = crossCcyInInBase.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                crossCcyTransferOut = summary.crossCcyTransferOut.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                crossCcyTransferOutInBase = crossCcyOutInBase.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
            )
        }

        return CashFlowResult(
            totalIncomeInBase = totalIncomeInBase.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
            totalExpensesInBase = totalExpenseInBase.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
            totalTransfersInBase = totalTransferInBase.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
            totalTransferInInBase = totalTransferInInBase.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
            totalTransferOutInBase = totalTransferOutInBase.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
            totalCrossCcyTransferInInBase = totalCrossCcyTransferInInBase.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
            totalCrossCcyTransferOutInBase = totalCrossCcyTransferOutInBase.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
            netInBase = totalIncomeInBase
                .add(totalExpenseInBase)
                .add(totalTransferInBase)
                .setScale(CurrencyService.precision, RoundingMode.HALF_UP),
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
                amount = summary.totalAmount.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                amountInBase = CurrencyService.convertAmount(summary.totalAmount, curr, input.targetCurrency, input.exchangeRates, 10)
                    .setScale(CurrencyService.precision, RoundingMode.HALF_UP)
            )
        }.sortedByDescending { it.amountInBase.abs() }

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

        // Launch current and previous period queries in parallel when comparison is enabled.
        // supervisorScope so a failure in the previous-period query degrades to current-only
        // rather than cancelling the current-period query and surfacing a card error.
        val accountSummaries = supervisorScope {
            val currentDeferred = async {
                computeAccountStats(queryService, profileId, input, currentFilter, targetAccounts, excludeInternal)
                    .mapValues { it.value.copy(currentPeriodFlow = it.value.currentPeriodFlow.toPerspective(config.perspective)) }
            }
            val previousDeferred = if (prevFilter != null) async {
                runCatching {
                    computeAccountStats(queryService, profileId, input, prevFilter, targetAccounts, excludeInternal)
                        .mapValues { it.value.copy(currentPeriodFlow = it.value.currentPeriodFlow.toPerspective(config.perspective)) }
                }.getOrNull()
            } else null

            val current = currentDeferred.await()
            val previous = previousDeferred?.await()
            if (previous != null) {
                current.mapValues { (id, summary) ->
                    summary.copy(previousPeriodFlow = previous[id]?.currentPeriodFlow)
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

        // Budgets should NOT follow the global timeline filter (e.g. Dashboard "Today")
        // but should respect global Account/Category filters if explicitly merged.
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

        val items = budgets.take(config.maxItems).map { budget ->
            val budgetFilter = BudgetService.buildTransactionFilter(budget, now, input.categories, effectiveFilter.timelineRange.excludeFuture)
            val finalFilter = budgetFilter.copy(
                accountIds = if (budget.accountIds.isEmpty()) effectiveFilter.accountIds else budget.accountIds
            )

            // Current spent
            val spentSummaries = TransactionAggregationService.getSummaryByCurrency(queryService, profileId, finalFilter)
                .getOrDefault(emptyMap())
            val spentSummary = CurrencyService.calculateTotalFinancialSummary(spentSummaries, budget.currency, input.exchangeRates)
            val spent = signedSpent(spentSummary.totalAmount, budget)

            val rollover = if (budget.rollover) {
                computeBudgetRollover(queryService, profileId, budget, input, effectiveFilter.accountIds, now, effectiveFilter.timelineRange.excludeFuture)
            } else BigDecimal.ZERO

            BudgetService.buildProgress(budget, spent, rollover)
        }

        return CardRenderData.Budgets(
            config = config,
            items = items,
            currency = input.targetCurrency
        )
    }

    // Replaces the old recursive approach: collects up to 12 previous period ranges, queries
    // all of them in parallel, then folds rollover in-memory from oldest period to newest.
    // This reduces worst-case sequential DB round trips from 12×N to a single parallel batch.
    private suspend fun computeBudgetRollover(
        queryService: DatabaseQueryService,
        profileId: Int,
        budget: Budget,
        input: CardInput,
        effectiveAccountIds: Set<String>,
        now: Long,
        excludeFuture: Boolean
    ): BigDecimal {
        // Walk backwards to collect up to 12 previous period ranges (index 0 = most recent past).
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

        // Query all periods in parallel.
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
                    signedSpent(CurrencyService.calculateTotalFinancialSummary(summaries, budget.currency, input.exchangeRates).totalAmount, budget)
                }
            }.awaitAll()
        }

        // Fold from oldest period to most recent (periodRanges[0] is most recent, so reverse).
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
            (mergedFilter.accountIds.isEmpty() || loan.linkedAccountId.isBlank() || loan.linkedAccountId in mergedFilter.accountIds) &&
                    (mergedFilter.currencies.isEmpty() || loan.currency in mergedFilter.currencies)
        }

        val items = filtered.take(config.maxItems).map { loan: Loan ->
            val loanFilter = LoanService.buildTransactionFilter(loan, input.categories)
            
            val summaryByCurrency = TransactionAggregationService.getSummaryByCurrency(queryService, profileId, loanFilter)
                .getOrDefault(emptyMap())

            val totalPaid = summaryByCurrency.entries.fold(BigDecimal.ZERO) { acc, entry ->
                val (curr, summary) = entry
                // Match old amount.signum() logic: positive amounts (income + transferIn) repay
                // LENT loans; negative amounts (expense + transferOut) repay BORROWED loans.
                val repaymentAmount = when (loan.loanType) {
                    LoanType.LENT -> summary.income.abs() + summary.transferIn.abs()
                    LoanType.BORROWED -> summary.expense.abs() + summary.transferOut.abs()
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

        // Fetch account balances at window start using the same account/currency scope as the card
        // filter so baseline and deltas are computed on the same transaction population.
        val startingBalances: Map<String, BigDecimal> = if (txs.isNotEmpty()) {
            val windowStart = txs.minOf { it.timestamp }
            val balanceFilter = effectiveFilter.toBalanceFilter().copy(
                timelineRange = TimelineRange.CustomRange(end = windowStart - 1)
            )
            TransactionAggregationService.getSummaryByAccountAndCurrency(
                queryService, profileId, balanceFilter
            ).getOrDefault(emptyMap()).mapValues { (accountId, byCurrency) ->
                val accountCurrency = input.accounts.find { it.id == accountId }?.currencyCode ?: input.targetCurrency
                CurrencyService.calculateTotalFinancialSummary(byCurrency, accountCurrency, input.exchangeRates).totalAmount
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
        val (tree, totals) = computeCategoryChartData(queryService, profileId, input, config.filter, config.filterRules, config.perspective, config.showComparison, config.excludeInternalTransfers)
        return CardRenderData.CategoryPieChart(
            config = config,
            items = tree,
            currency = input.targetCurrency,
            previousTotal = totals.previousTotal,
            incomeTotal = totals.incomeTotal,
            expenseTotal = totals.expenseTotal,
            transferTotal = totals.transferTotal,
            previousIncomeTotal = totals.previousIncomeTotal,
            previousExpenseTotal = totals.previousExpenseTotal,
            previousTransferTotal = totals.previousTransferTotal
        )
    }

    suspend fun computeCategoryTreemapCard(
        queryService: DatabaseQueryService,
        profileId: Int,
        input: CardInput,
        config: CategoryTreemapCardConfig
    ): CardRenderData.CategoryTreemap {
        val (tree, totals) = computeCategoryChartData(queryService, profileId, input, config.filter, config.filterRules, config.perspective, config.showComparison, config.excludeInternalTransfers)
        return CardRenderData.CategoryTreemap(
            config = config,
            items = tree,
            currency = input.targetCurrency,
            previousTotal = totals.previousTotal,
            incomeTotal = totals.incomeTotal,
            expenseTotal = totals.expenseTotal,
            transferTotal = totals.transferTotal,
            previousIncomeTotal = totals.previousIncomeTotal,
            previousExpenseTotal = totals.previousExpenseTotal,
            previousTransferTotal = totals.previousTransferTotal
        )
    }

    private data class CategoryChartTotals(
        val incomeTotal: BigDecimal,
        val expenseTotal: BigDecimal,
        val transferTotal: BigDecimal,
        val previousTotal: BigDecimal?,
        val previousIncomeTotal: BigDecimal?,
        val previousExpenseTotal: BigDecimal?,
        val previousTransferTotal: BigDecimal?
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
    ): Pair<List<CategoryTreeMetrics>, CategoryChartTotals> {
        val mergedFilter = resolveEffectiveFilter(input.filter, cardFilter ?: TransactionFilter(), filterRules, input.screenOwnsTimeline)
        val expandedCategoryIds = input.categoryHierarchy.expand(mergedFilter.categoryIds)
        val excludeInternal = excludeInternalTransfers && perspective is AccountingPerspective.CashFlow
        val finalFilter = mergedFilter.copy(
            accountIds = mergedFilter.accountIds.ifEmpty { input.effectiveAccountIds },
            categoryIds = expandedCategoryIds,
            transactionTypes = emptySet() // Include all types to support Transfers-as-categories
        )
        val tree = computeCategoryTree(queryService, profileId, input, finalFilter, perspective, showComparison, excludeInternal)
        val totals = CategoryChartTotals(
            incomeTotal = tree.sumOf { it.income },
            expenseTotal = tree.sumOf { it.expense },
            transferTotal = tree.sumOf { it.transfer },
            previousTotal = if (showComparison) tree.sumOf { it.previousNetBalance ?: BigDecimal.ZERO } else null,
            previousIncomeTotal = if (showComparison) tree.sumOf { it.previousIncome ?: BigDecimal.ZERO } else null,
            previousExpenseTotal = if (showComparison) tree.sumOf { it.previousExpense ?: BigDecimal.ZERO } else null,
            previousTransferTotal = if (showComparison) tree.sumOf { it.previousTransfer ?: BigDecimal.ZERO } else null
        )
        return tree to totals
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
                val perspectiveSummaries = converted.mapValues { it.value.toPerspective(perspective) }
                FinancialMetrics(
                    key = categoryId,
                    financialSummary = FinancialSummary.aggregate(perspectiveSummaries.values),
                    currencyBreakdown = perspectiveSummaries
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

        // Run balance and period aggregation queries in parallel — they are independent.
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
                currencyTotals = balanceSummaries.mapValues { it.value.totalAmount },
                targetCurrency = input.targetCurrency,
                exchangeRates = input.exchangeRates,
                precision = 10
            )

            val currencyBalances = balanceSummaries.mapValues { it.value.totalAmount }

            account.id to AccountSummaryItem(
                account = account,
                currencyBalances = currencyBalances,
                balance = totalBalanceSummaryInAccountCurrency.totalAmount,
                balanceInBase = balanceInBase,
                currentPeriodFlow = AccountFlowMetrics(
                    income = totalPeriodSummaryInAccountCurrency.income,
                    expense = totalPeriodSummaryInAccountCurrency.expense,
                    transferIn = totalPeriodSummaryInAccountCurrency.transferIn,
                    transferOut = totalPeriodSummaryInAccountCurrency.transferOut,
                    crossCcyTransferIn = totalPeriodSummaryInAccountCurrency.crossCcyTransferIn,
                    crossCcyTransferOut = totalPeriodSummaryInAccountCurrency.crossCcyTransferOut
                )
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

        // Period summary
        val periodFilter = resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline)
        val excludeInternal = config.excludeInternalTransfers && config.perspective is AccountingPerspective.CashFlow
        
        // Lifetime balance (base scope) - Always show the true account balance.
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
        )

        val currentFlow = AccountFlowMetrics(
            income = periodSummary.income.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
            expense = periodSummary.expense.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
            transferIn = periodSummary.transferIn.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
            transferOut = periodSummary.transferOut.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
            crossCcyTransferIn = periodSummary.crossCcyTransferIn.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
            crossCcyTransferOut = periodSummary.crossCcyTransferOut.setScale(CurrencyService.precision, RoundingMode.HALF_UP)
        )

        var previousFlow: AccountFlowMetrics? = null
        if (config.showComparison) {
            val (prevStart, prevEnd) = TimelineService.getPreviousRange(periodFilter.timelineRange)
            if (prevStart != null && prevEnd != null) {
                val prevFilter = periodFilter.copy(
                    timelineRange = TimelineRange.CustomRange(prevStart, prevEnd)
                )
                val prevSummariesByCurrency = TransactionAggregationService.getSummaryByCurrency(queryService, profileId, prevFilter, excludeInternal)
                    .getOrDefault(emptyMap())
                val prevSummary = CurrencyService.calculateTotalFinancialSummary(
                    prevSummariesByCurrency,
                    account.currencyCode,
                    input.exchangeRates
                )
                previousFlow = AccountFlowMetrics(
                    income = prevSummary.income.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                    expense = prevSummary.expense.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                    transferIn = prevSummary.transferIn.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                    transferOut = prevSummary.transferOut.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                    crossCcyTransferIn = prevSummary.crossCcyTransferIn.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                    crossCcyTransferOut = prevSummary.crossCcyTransferOut.setScale(CurrencyService.precision, RoundingMode.HALF_UP)
                )
            }
        }

        val balancesByCurrency = lifetimeSummariesByCurrency
            .mapValues { it.value.totalAmount.setScale(CurrencyService.precision, RoundingMode.HALF_UP) }

        val summary = AccountSummaryItem(
            account = account,
            currencyBalances = balancesByCurrency,
            balance = lifetimeSummary.totalAmount.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
            balanceInBase = CurrencyService.calculateTotalInCurrency(
                currencyTotals = balancesByCurrency,
                targetCurrency = input.targetCurrency,
                exchangeRates = input.exchangeRates,
                precision = 10
            ),
            currentPeriodFlow = currentFlow.toPerspective(config.perspective),
            previousPeriodFlow = previousFlow?.toPerspective(config.perspective)
        )

        return CardRenderData.AccountHero(
            config = config,
            summary = summary,
            currency = input.targetCurrency,
            exchangeRates = input.exchangeRates
        )
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
