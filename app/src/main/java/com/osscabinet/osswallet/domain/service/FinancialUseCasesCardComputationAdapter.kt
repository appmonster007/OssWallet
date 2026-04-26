package com.osscabinet.ossWallet.domain.service

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.settings.AccountsCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig
import com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig
import com.osscabinet.ossWallet.domain.model.analytics.ComputationContext
import com.osscabinet.ossWallet.domain.usecase.AccountSummaryItem
import com.osscabinet.ossWallet.domain.usecase.CashFlowResult
import com.osscabinet.ossWallet.domain.usecase.CurrencyAggregationItem
import com.osscabinet.ossWallet.domain.usecase.FinancialUseCases
import java.math.BigDecimal

/**
 * Adapter that implements CardComputationEngine using FinancialUseCases.
 * This allows FinancialUseCases to be used interchangeably with CardComputationService
 * for card data computation.
 */
class FinancialUseCasesCardComputationAdapter(
    private val financialUseCases: FinancialUseCases
) : CardComputationEngine {

    override suspend fun computeNetWorth(
        config: NetWorthCardConfig,
        accounts: List<Account>,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): Pair<BigDecimal, CashFlowResult> {
        val netWorth = financialUseCases.calculateNetWorth(
            accounts = accounts,
            allTransactions = allTransactions,
            baseCurrency = context.baseCurrency,
            exchangeRates = context.exchangeRates,
            precision = context.precision,
            timelineRange = config.timelineRange
        )
        val cashFlow = financialUseCases.calculateCashFlow(
            allTransactions = allTransactions,
            timelineRange = config.timelineRange,
            baseCurrency = context.baseCurrency,
            exchangeRates = context.exchangeRates,
            precision = context.precision
        )
        return Pair(netWorth, cashFlow)
    }

    override suspend fun computeCurrencyTotals(
        config: CurrencyBalancesCardConfig,
        accounts: List<Account>,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): List<CurrencyAggregationItem> {
        return financialUseCases.getCurrencyAggregation(
            accounts = accounts,
            allTransactions = allTransactions,
            timelineRange = config.timelineRange,
            baseCurrency = context.baseCurrency,
            exchangeRates = context.exchangeRates,
            precision = context.precision
        )
    }

    override suspend fun computeCashFlow(
        config: CashFlowCardConfig,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): CashFlowResult {
        return financialUseCases.calculateCashFlow(
            allTransactions = allTransactions,
            timelineRange = config.timelineRange,
            baseCurrency = context.baseCurrency,
            exchangeRates = context.exchangeRates,
            precision = context.precision
        )
    }

    override suspend fun computeAccountStats(
        config: AccountsCardConfig,
        accounts: List<Account>,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): Pair<List<Account>, Map<String, AccountSummaryItem>> {
        val accountStats = financialUseCases.getAccountSummaries(
            accounts = accounts,
            allTransactions = allTransactions,
            timelineRange = config.timelineRange,
            baseCurrency = context.baseCurrency,
            exchangeRates = context.exchangeRates,
            precision = context.precision
        )
        val updatedAccounts = financialUseCases.getTotalBalances(
            accounts = accounts,
            allTransactions = allTransactions,
            exchangeRates = context.exchangeRates,
            precision = context.precision,
            timelineRange = config.timelineRange
        )
        return Pair(updatedAccounts, accountStats)
    }
}
