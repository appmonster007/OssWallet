package com.osscabinet.ossWallet.domain.service.aggregation

import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.NetWorthMetrics
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.merge
import com.osscabinet.ossWallet.domain.model.analytics.CashFlowBreakdownItem
import com.osscabinet.ossWallet.domain.model.analytics.CashFlowResult
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.financial.FinancialSummary
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService
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
        
        val currentSummary = TransactionAggregationService.getSummary(queryService, profileId, currentFilter)
            .getOrDefault(FinancialSummary.EMPTY)
        
        val currentTotalsByCurrency = TransactionAggregationService.getSummaryByCurrency(queryService, profileId, currentFilter)
            .getOrDefault(emptyMap())
            .mapValues { it.value.totalAmount }

        val netWorth = CurrencyService.calculateTotalInCurrency(
            currencyTotals = currentTotalsByCurrency,
            targetCurrency = input.baseCurrency,
            exchangeRates = input.exchangeRates,
            precision = input.precision
        )

        // For CashFlowResult in NetWorthCard
        val cashFlow = computeCashFlowResult(queryService, profileId, input, currentFilter)

        val currentMetrics = NetWorthMetrics(netWorth, cashFlow)

        return CardRenderData.NetWorth(
            config = config,
            currentMetrics = currentMetrics,
            baseCurrency = input.baseCurrency,
            precision = input.precision,
            previousMetrics = null // Skipping previous period for first iteration
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
            baseCurrency = input.baseCurrency,
            precision = input.precision
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

        val breakdownItems = summariesByCurrency.map { (curr, summary) ->
            val incomeInBase = CurrencyService.convertAmount(summary.income, curr, input.baseCurrency, input.exchangeRates, 10)
            val expenseInBase = CurrencyService.convertAmount(summary.expense.abs(), curr, input.baseCurrency, input.exchangeRates, 10)

            totalIncomeInBase = totalIncomeInBase.add(incomeInBase)
            totalExpenseInBase = totalExpenseInBase.add(expenseInBase)

            CashFlowBreakdownItem(
                currency = curr,
                income = summary.income.setScale(input.precision, RoundingMode.HALF_UP),
                incomeInBase = incomeInBase.setScale(input.precision, RoundingMode.HALF_UP),
                expense = summary.expense.abs().setScale(input.precision, RoundingMode.HALF_UP),
                expenseInBase = expenseInBase.setScale(input.precision, RoundingMode.HALF_UP)
            )
        }

        return CashFlowResult(
            totalIncomeInBase = totalIncomeInBase.setScale(input.precision, RoundingMode.HALF_UP),
            totalExpensesInBase = totalExpenseInBase.setScale(input.precision, RoundingMode.HALF_UP),
            netInBase = totalIncomeInBase.subtract(totalExpenseInBase).setScale(input.precision, RoundingMode.HALF_UP),
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
            com.osscabinet.ossWallet.domain.model.analytics.CurrencyAggregationItem(
                currency = curr,
                amount = summary.totalAmount.setScale(input.precision, RoundingMode.HALF_UP),
                amountInBase = CurrencyService.convertAmount(summary.totalAmount, curr, input.baseCurrency, input.exchangeRates, 10)
                    .setScale(input.precision, RoundingMode.HALF_UP)
            )
        }.sortedByDescending { it.amountInBase.abs() }

        return CardRenderData.CurrencyBalances(config, balances, input.baseCurrency, input.precision)
    }

    suspend fun computeAccountsCard(
        queryService: DatabaseQueryService,
        profileId: Int,
        input: CardInput,
        config: AccountsCardConfig
    ): CardRenderData.Accounts {
        val mergedFilter = input.filter.merge(config.filter)
        val summariesByAccount = TransactionAggregationService.getSummaryByAccount(queryService, profileId, mergedFilter)
            .getOrDefault(emptyMap())

        val accountStats = input.accounts.associate { account ->
            val summary = summariesByAccount[account.id] ?: FinancialSummary.EMPTY
            
            // For now, let's assume one main currency balance per account for simplicity in this summary
            // In a real multi-currency account, we'd query by (Account, Currency)
            val currencyBalances = mapOf(account.currencyCode to summary.totalAmount)
            
            val balanceInBase = CurrencyService.convertAmount(
                summary.totalAmount, 
                account.currencyCode, 
                input.baseCurrency, 
                input.exchangeRates, 
                10
            )

            account.id to com.osscabinet.ossWallet.domain.model.analytics.AccountSummaryItem(
                account = account,
                currencyBalances = currencyBalances,
                balance = summary.totalAmount,
                balanceInBase = balanceInBase,
                incomeInAccountCurrency = summary.income,
                expenseInAccountCurrency = summary.expense.abs()
            )
        }

        return CardRenderData.Accounts(
            config,
            input.accounts,
            accountStats,
            input.baseCurrency,
            "light",
            input.precision
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
        val baseFilter = input.filter.merge(config.filter).copy(excludeFuture = true, timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.AllTime)
        val lifetimeSummary = TransactionAggregationService.getSummary(queryService, profileId, baseFilter)
            .getOrDefault(FinancialSummary.EMPTY)

        // Period summary
        val periodFilter = input.filter.merge(config.filter).copy(excludeFuture = true)
        val periodSummary = TransactionAggregationService.getSummary(queryService, profileId, periodFilter)
            .getOrDefault(FinancialSummary.EMPTY)

        val balancesByCurrency = TransactionAggregationService.getSummaryByCurrency(queryService, profileId, baseFilter)
            .getOrDefault(emptyMap())
            .mapValues { it.value.totalAmount.setScale(input.precision, RoundingMode.HALF_UP) }

        return CardRenderData.AccountHero(
            config = config,
            account = account,
            balance = lifetimeSummary.totalAmount.setScale(input.precision, RoundingMode.HALF_UP),
            income = periodSummary.income.setScale(input.precision, RoundingMode.HALF_UP),
            expense = periodSummary.expense.abs().setScale(input.precision, RoundingMode.HALF_UP),
            baseCurrency = input.baseCurrency,
            exchangeRates = input.exchangeRates,
            currencyBalances = balancesByCurrency,
            precision = input.precision
        )
    }
}
