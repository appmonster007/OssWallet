package com.osscabinet.ossWallet.domain.service

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.settings.AccountsCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig
import com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig
import com.osscabinet.ossWallet.domain.usecase.AccountSummaryItem
import com.osscabinet.ossWallet.domain.usecase.CashFlowResult
import com.osscabinet.ossWallet.domain.usecase.CurrencyAggregationItem
import com.osscabinet.ossWallet.domain.usecase.FinancialUseCases
import com.osscabinet.ossWallet.ui.components.cards.filterAccounts
import com.osscabinet.ossWallet.ui.components.cards.filterTransactions
import java.math.BigDecimal

/**
 * Adapter that implements CardComputationEngine using FinancialUseCases.
 * This allows FinancialUseCases to be used interchangeably with CardComputationService
 * for card data computation.
 * 
 * Note: Filtering is handled by CardFilter extension functions on CardConfig,
 * so this adapter does not need to implement filtering logic.
 */
class FinancialUseCasesCardComputationAdapter(
    private val financialUseCases: FinancialUseCases
) : CardComputationEngine {

    override suspend fun computeNetWorth(
        config: NetWorthCardConfig,
        accounts: List<Account>,
        transactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): Pair<BigDecimal, CashFlowResult> {
        val netWorth = financialUseCases.calculateNetWorth(
            accounts = accounts,
            allTransactions = transactions,
            baseCurrency = baseCurrency,
            exchangeRates = exchangeRates,
            precision = precision,
            timelineRange = config.timelineRange
        )
        val cashFlow = financialUseCases.calculateCashFlow(
            allTransactions = transactions,
            timelineRange = config.timelineRange,
            baseCurrency = baseCurrency,
            exchangeRates = exchangeRates,
            precision = precision
        )
        return Pair(netWorth, cashFlow)
    }

    override suspend fun computeCurrencyTotals(
        config: CurrencyBalancesCardConfig,
        accounts: List<Account>,
        transactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): List<CurrencyAggregationItem> {
        return financialUseCases.getCurrencyAggregation(
            accounts = accounts,
            allTransactions = transactions,
            timelineRange = config.timelineRange,
            baseCurrency = baseCurrency,
            exchangeRates = exchangeRates,
            precision = precision
        )
    }

    override suspend fun computeCashFlow(
        config: CashFlowCardConfig,
        transactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): CashFlowResult {
        return financialUseCases.calculateCashFlow(
            allTransactions = transactions,
            timelineRange = config.timelineRange,
            baseCurrency = baseCurrency,
            exchangeRates = exchangeRates,
            precision = precision
        )
    }

    override suspend fun computeAccountStats(
        config: AccountsCardConfig,
        accounts: List<Account>,
        transactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): Pair<List<Account>, Map<String, AccountSummaryItem>> {
        val accountStats = financialUseCases.getAccountSummaries(
            accounts = accounts,
            allTransactions = transactions,
            timelineRange = config.timelineRange,
            baseCurrency = baseCurrency,
            exchangeRates = exchangeRates,
            precision = precision
        )
        val updatedAccounts = financialUseCases.getTotalBalances(
            accounts = accounts,
            allTransactions = transactions,
            exchangeRates = exchangeRates,
            precision = precision,
            timelineRange = config.timelineRange
        )
        return Pair(updatedAccounts, accountStats)
    }
}
