package com.osscabinet.ossWallet.domain.service.aggregation

import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.NetWorthMetrics
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.merge
import com.osscabinet.ossWallet.domain.model.analytics.AccountSummaryItem
import com.osscabinet.ossWallet.domain.model.analytics.CashFlowBreakdownItem
import com.osscabinet.ossWallet.domain.model.analytics.CashFlowResult
import com.osscabinet.ossWallet.domain.model.analytics.CurrencyAggregationItem
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
        val currentFilter = input.filter.merge(config.filter)
        
        val currentTotalsByCurrency = TransactionAggregationService.getSummaryByCurrency(queryService, profileId, currentFilter)
            .getOrDefault(emptyMap())
            .mapValues { it.value.totalAmount }

        val netWorth = CurrencyService.calculateTotalInCurrency(
            currencyTotals = currentTotalsByCurrency,
            targetCurrency = input.baseCurrency,
            exchangeRates = input.exchangeRates
        )

        val cashFlow = computeCashFlowResult(queryService, profileId, input, currentFilter)
        val currentMetrics = NetWorthMetrics(netWorth, cashFlow)

        val previousMetrics: NetWorthMetrics? = if (config.showComparison) {
            val (prevStart, prevEnd) = TimelineService.getPreviousRange(currentFilter.timelineRange)
            if (prevStart != null && prevEnd != null) {
                val prevFilter = currentFilter.copy(
                    timelineRange = TimelineRange.CustomRange(prevStart, prevEnd)
                )
                val prevTotalsByCurrency = TransactionAggregationService.getSummaryByCurrency(queryService, profileId, prevFilter)
                    .getOrDefault(emptyMap())
                    .mapValues { it.value.totalAmount }
                val prevNetWorth = CurrencyService.calculateTotalInCurrency(
                    currencyTotals = prevTotalsByCurrency,
                    targetCurrency = input.baseCurrency,
                    exchangeRates = input.exchangeRates
                )
                val prevCashFlow = computeCashFlowResult(queryService, profileId, input, prevFilter)
                NetWorthMetrics(prevNetWorth, prevCashFlow)
            } else null
        } else null

        return CardRenderData.NetWorth(
            config = config,
            currentMetrics = currentMetrics,
            baseCurrency = input.baseCurrency,
            previousMetrics = previousMetrics
        )
    }

    suspend fun computeCashFlowCard(
        queryService: DatabaseQueryService,
        profileId: Int,
        input: CardInput,
        config: CashFlowCardConfig
    ): CardRenderData.CashFlow {
        val mergedFilter = input.filter.merge(config.filter)
        val result = computeCashFlowResult(queryService, profileId, input, mergedFilter)

        return CardRenderData.CashFlow(
            config = config,
            result = result,
            baseCurrency = input.baseCurrency
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

        val breakdownItems = summariesByCurrency.map { (curr, summary) ->
            val incomeInBase = CurrencyService.convertAmount(summary.income, curr, input.baseCurrency, input.exchangeRates, 10)
            val expenseInBase = CurrencyService.convertAmount(summary.expense.abs(), curr, input.baseCurrency, input.exchangeRates, 10)
            val transferInBase = CurrencyService.convertAmount(summary.transfer, curr, input.baseCurrency, input.exchangeRates, 10)

            totalIncomeInBase = totalIncomeInBase.add(incomeInBase)
            totalExpenseInBase = totalExpenseInBase.add(expenseInBase)
            totalTransferInBase = totalTransferInBase.add(transferInBase)

            CashFlowBreakdownItem(
                currency = curr,
                income = summary.income.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                incomeInBase = incomeInBase.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                expense = summary.expense.abs().setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                expenseInBase = expenseInBase.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                transfer = summary.transfer.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                transferInBase = transferInBase.setScale(CurrencyService.precision, RoundingMode.HALF_UP)
            )
        }

        return CashFlowResult(
            totalIncomeInBase = totalIncomeInBase.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
            totalExpensesInBase = totalExpenseInBase.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
            totalTransfersInBase = totalTransferInBase.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
            netInBase = totalIncomeInBase.subtract(totalExpenseInBase).setScale(CurrencyService.precision, RoundingMode.HALF_UP),
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
        val mergedFilter = input.filter.merge(config.filter)
        val summariesByCurrency = TransactionAggregationService.getSummaryByCurrency(queryService, profileId, mergedFilter)
            .getOrDefault(emptyMap())

        val balances = summariesByCurrency.map { (curr, summary) ->
            CurrencyAggregationItem(
                currency = curr,
                amount = summary.totalAmount.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                amountInBase = CurrencyService.convertAmount(summary.totalAmount, curr, input.baseCurrency, input.exchangeRates, 10)
                    .setScale(CurrencyService.precision, RoundingMode.HALF_UP)
            )
        }.sortedByDescending { it.amountInBase.abs() }

        return CardRenderData.CurrencyBalances(config, balances, input.baseCurrency)
    }

    suspend fun computeAccountsCard(
        queryService: DatabaseQueryService,
        profileId: Int,
        input: CardInput,
        config: AccountsCardConfig
    ): CardRenderData.Accounts {
        val mergedFilter = input.filter.merge(config.filter)
        val summariesByAccountAndCurrency = TransactionAggregationService.getSummaryByAccountAndCurrency(queryService, profileId, mergedFilter)
            .getOrDefault(emptyMap())

        val accountStats = input.accounts.associate { account ->
            val currencySummaries = summariesByAccountAndCurrency[account.id] ?: emptyMap()
            
            // Multi-currency aware aggregation for the account
            val totalSummaryInAccountCurrency = CurrencyService.calculateTotalFinancialSummary(
                currencySummaries,
                account.currencyCode,
                input.exchangeRates
            )

            val balanceInBase = CurrencyService.calculateTotalInCurrency(
                currencyTotals = currencySummaries.mapValues { it.value.totalAmount },
                targetCurrency = input.baseCurrency,
                exchangeRates = input.exchangeRates,
                precision = 10
            )

            val currencyBalances = currencySummaries.mapValues { it.value.totalAmount }

            account.id to AccountSummaryItem(
                account = account,
                currencyBalances = currencyBalances,
                balance = totalSummaryInAccountCurrency.totalAmount,
                balanceInBase = balanceInBase,
                incomeInAccountCurrency = totalSummaryInAccountCurrency.income,
                expenseInAccountCurrency = totalSummaryInAccountCurrency.expense.abs(),
                transferInAccountCurrency = totalSummaryInAccountCurrency.transfer
            )
        }

        return CardRenderData.Accounts(
            config,
            input.accounts,
            accountStats,
            input.baseCurrency,
            "light"
        )
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

        // Lifetime balance (base scope)
        val baseFilter = input.filter.merge(config.filter).copy(excludeFuture = true, timelineRange = TimelineRange.AllTime)
        val lifetimeSummariesByCurrency = TransactionAggregationService.getSummaryByCurrency(queryService, profileId, baseFilter)
            .getOrDefault(emptyMap())
        
        val lifetimeSummary = CurrencyService.calculateTotalFinancialSummary(
            lifetimeSummariesByCurrency,
            account.currencyCode,
            input.exchangeRates
        )

        // Period summary
        val periodFilter = input.filter.merge(config.filter).copy(excludeFuture = true)
        val periodSummariesByCurrency = TransactionAggregationService.getSummaryByCurrency(queryService, profileId, periodFilter)
            .getOrDefault(emptyMap())

        val periodSummary = CurrencyService.calculateTotalFinancialSummary(
            periodSummariesByCurrency,
            account.currencyCode,
            input.exchangeRates
        )

        val balancesByCurrency = lifetimeSummariesByCurrency
            .mapValues { it.value.totalAmount.setScale(CurrencyService.precision, RoundingMode.HALF_UP) }

        return CardRenderData.AccountHero(
            config = config,
            account = account,
            balance = lifetimeSummary.totalAmount.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
            income = periodSummary.income.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
            expense = periodSummary.expense.abs().setScale(CurrencyService.precision, RoundingMode.HALF_UP),
            baseCurrency = input.baseCurrency,
            exchangeRates = input.exchangeRates,
            currencyBalances = balancesByCurrency
        )
    }
}
