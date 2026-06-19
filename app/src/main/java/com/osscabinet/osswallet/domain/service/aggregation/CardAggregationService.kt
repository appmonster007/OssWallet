package com.osscabinet.ossWallet.domain.service.aggregation

import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.NetWorthMetrics
import com.osscabinet.ossWallet.domain.card.resolveEffectiveFilter
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.analytics.*
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.financial.FinancialSummary
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
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

        // For absolute balances, we need cumulative totals up to the end of the period
        val balanceFilter = currentFilter.toBalanceFilter()
        
        val currentTotalsByCurrency = TransactionAggregationService.getSummaryByCurrency(queryService, profileId, balanceFilter)
            .getOrDefault(emptyMap())
            .mapValues { it.value.totalAmount }

        val netWorth = CurrencyService.calculateTotalInCurrency(
            currencyTotals = currentTotalsByCurrency,
            targetCurrency = input.targetCurrency,
            exchangeRates = input.exchangeRates
        )

        val cashFlow = computeCashFlowResult(queryService, profileId, input, currentFilter).toPerspective(config.perspective)
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
                val prevCashFlow = computeCashFlowResult(queryService, profileId, input, prevFilter).toPerspective(config.perspective)
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
        val result = computeCashFlowResult(queryService, profileId, input, mergedFilter).toPerspective(config.perspective)

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
        filter: TransactionFilter
    ): CashFlowResult {
        val summariesByCurrency = TransactionAggregationService.getSummaryByCurrency(queryService, profileId, filter)
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
        val rawFilter = resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline)
        val currentFilter = rawFilter.copy(
            accountIds = rawFilter.accountIds.ifEmpty { input.effectiveAccountIds }
        )

        val currentPeriodAccountStats = computeAccountStats(queryService, profileId, input, currentFilter)
            .mapValues { it.value.copy(currentPeriodFlow = it.value.currentPeriodFlow.toPerspective(config.perspective)) }

        val accountSummaries = if (config.showComparison) {
            val (prevStart, prevEnd) = TimelineService.getPreviousRange(currentFilter.timelineRange)
            if (prevStart != null && prevEnd != null) {
                val prevFilter = currentFilter.copy(
                    timelineRange = TimelineRange.CustomRange(prevStart, prevEnd)
                )
                val previousPeriodAccountStats = computeAccountStats(queryService, profileId, input, prevFilter)
                    .mapValues { it.value.copy(currentPeriodFlow = it.value.currentPeriodFlow.toPerspective(config.perspective)) }
                currentPeriodAccountStats.mapValues { (id, summary) ->
                    summary.copy(previousPeriodFlow = previousPeriodAccountStats[id]?.currentPeriodFlow)
                }
            } else currentPeriodAccountStats
        } else currentPeriodAccountStats

        return CardRenderData.Accounts(
            config = config,
            updatedAccounts = input.effectiveAccounts,
            accountSummaries = accountSummaries,
            currency = input.targetCurrency,
            currentTheme = "light",
            exchangeRates = input.exchangeRates
        )
    }

    private suspend fun computeAccountStats(
        queryService: DatabaseQueryService,
        profileId: Int,
        input: CardInput,
        filter: TransactionFilter
    ): Map<String, AccountSummaryItem> {
        val balanceFilter = filter.toBalanceFilter()

        val summariesByAccountAndCurrencyBalance = TransactionAggregationService.getSummaryByAccountAndCurrency(queryService, profileId, balanceFilter)
            .getOrDefault(emptyMap())
        
        val summariesByAccountAndCurrencyPeriod = TransactionAggregationService.getSummaryByAccountAndCurrency(queryService, profileId, filter)
            .getOrDefault(emptyMap())

        return input.effectiveAccounts.associate { account ->
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
        val periodSummariesByCurrency = TransactionAggregationService.getSummaryByCurrency(queryService, profileId, periodFilter)
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
                val prevSummariesByCurrency = TransactionAggregationService.getSummaryByCurrency(queryService, profileId, prevFilter)
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
        return when (val range = this.timelineRange) {
            is TimelineRange.AllTime -> this
            else -> {
                val (_, end) = TimelineService.getDateRange(range)
                this.copy(
                    timelineRange = if (end != null) TimelineRange.CustomRange(end = end, excludeFuture = range.excludeFuture) 
                                   else TimelineRange.AllTime(excludeFuture = range.excludeFuture)
                )
            }
        }
    }
}
