package com.osscabinet.ossWallet.ui.components.cards

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.domain.usecase.AccountSummaryItem
import com.osscabinet.ossWallet.domain.usecase.CashFlowResult
import com.osscabinet.ossWallet.domain.usecase.CurrencyAggregationItem
import com.osscabinet.ossWallet.domain.service.CardComputationEngine
import java.math.BigDecimal

/**
 * Unified compute logic for cards.
 * Extension functions on CardConfig types to compute data for rendering.
 * This centralizes compute logic and makes it easier to maintain.
 * 
 * These extension functions use the CardComputationEngine interface for computation,
 * allowing different implementations (CardComputationService, FinancialUseCases) to be used interchangeably.
 * Filtering is handled by the CardFilter extension functions.
 */

/**
 * Compute NetWorth data for NetWorthCardConfig
 * Filters accounts and transactions based on selectedAccountIds if specified
 */
suspend fun NetWorthCardConfig.computeNetWorth(
    accounts: List<Account>,
    transactions: List<Transaction>,
    engine: CardComputationEngine,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    precision: Int
): Pair<BigDecimal, CashFlowResult> {
    return engine.computeNetWorth(
        config = this,
        accounts = this.filterAccounts(accounts),
        allTransactions = this.filterTransactions(transactions),
        baseCurrency = baseCurrency,
        exchangeRates = exchangeRates,
        precision = precision
    )
}

/**
 * Compute Currency Totals data for CurrencyBalancesCardConfig
 * Filters accounts and transactions based on accountId if specified (for Account Detail screen)
 * For Accounts View, accountId is null so no filtering is applied
 */
suspend fun CurrencyBalancesCardConfig.computeCurrencyTotals(
    accounts: List<Account>,
    transactions: List<Transaction>,
    engine: CardComputationEngine,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    precision: Int
): List<CurrencyAggregationItem> {
    return engine.computeCurrencyTotals(
        config = this,
        accounts = this.filterAccounts(accounts),
        allTransactions = this.filterTransactions(transactions),
        baseCurrency = baseCurrency,
        exchangeRates = exchangeRates,
        precision = precision
    )
}

/**
 * Compute Cash Flow data for CashFlowCardConfig
 * Filters transactions based on accountId if specified (for Account Detail screen)
 * For Accounts View, accountId is null so no filtering is applied
 */
suspend fun CashFlowCardConfig.computeCashFlow(
    transactions: List<Transaction>,
    engine: CardComputationEngine,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    precision: Int
): CashFlowResult {
    return engine.computeCashFlow(
        config = this,
        allTransactions = this.filterTransactions(transactions),
        baseCurrency = baseCurrency,
        exchangeRates = exchangeRates,
        precision = precision
    )
}

/**
 * Compute Account Stats data for AccountsCardConfig
 */
suspend fun AccountsCardConfig.computeAccountStats(
    accounts: List<Account>,
    transactions: List<Transaction>,
    engine: CardComputationEngine,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    precision: Int
): Pair<List<Account>, Map<String, AccountSummaryItem>> {
    return engine.computeAccountStats(
        config = this,
        accounts = accounts,
        allTransactions = transactions,
        baseCurrency = baseCurrency,
        exchangeRates = exchangeRates,
        precision = precision
    )
}
