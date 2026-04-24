package com.osscabinet.ossWallet.ui.components.cards

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.domain.usecase.AccountSummaryItem
import com.osscabinet.ossWallet.domain.usecase.CashFlowResult
import com.osscabinet.ossWallet.domain.usecase.CurrencyAggregationItem
import com.osscabinet.ossWallet.domain.usecase.FinancialUseCases
import com.osscabinet.ossWallet.domain.service.CardComputationService
import java.math.BigDecimal

/**
 * Unified compute logic for cards.
 * Extension functions on CardConfig types to compute data for rendering.
 * This centralizes compute logic and makes it easier to maintain.
 */

/**
 * Compute NetWorth data for NetWorthCardConfig
 * Filters accounts and transactions based on selectedAccountIds if specified
 */
suspend fun NetWorthCardConfig.computeNetWorth(
    accounts: List<Account>,
    allTransactions: List<Transaction>,
    financialUseCases: FinancialUseCases,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    precision: Int
): Pair<BigDecimal, CashFlowResult> {
    // Filter accounts based on selectedAccountIds if specified
    val filteredAccounts = if (selectedAccountIds != null && selectedAccountIds.isNotEmpty()) {
        accounts.filter { it.id in selectedAccountIds }
    } else {
        accounts
    }
    
    // Filter transactions based on selectedAccountIds if specified
    val filteredTransactions = if (selectedAccountIds != null && selectedAccountIds.isNotEmpty()) {
        allTransactions.filter { it.accountId in selectedAccountIds }
    } else {
        allTransactions
    }
    
    val netWorth = financialUseCases.calculateNetWorth(
        accounts = filteredAccounts,
        allTransactions = filteredTransactions,
        baseCurrency = baseCurrency,
        exchangeRates = exchangeRates,
        precision = precision,
        timelineRange = timelineRange
    )
    val cashFlow = financialUseCases.calculateCashFlow(
        allTransactions = filteredTransactions,
        timelineRange = timelineRange,
        baseCurrency = baseCurrency,
        exchangeRates = exchangeRates,
        precision = precision
    )
    return Pair(netWorth, cashFlow)
}

/**
 * Compute Currency Totals data for CurrencyBalancesCardConfig
 * Filters accounts and transactions based on accountId if specified (for Account Detail screen)
 * For Accounts View, accountId is null so no filtering is applied
 */
suspend fun CurrencyBalancesCardConfig.computeCurrencyTotals(
    accounts: List<Account>,
    allTransactions: List<Transaction>,
    financialUseCases: FinancialUseCases,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    precision: Int
): List<CurrencyAggregationItem> {
    // If accountId is specified, filter to that account only (Account Detail screen)
    // If accountId is null, use all accounts (Accounts View screen)
    val filteredAccounts = if (accountId != null) {
        accounts.filter { it.id == accountId }
    } else {
        accounts
    }
    
    val filteredTransactions = if (accountId != null) {
        allTransactions.filter { it.accountId == accountId }
    } else {
        allTransactions
    }
    
    return financialUseCases.getCurrencyAggregation(
        accounts = filteredAccounts,
        allTransactions = filteredTransactions,
        timelineRange = timelineRange,
        baseCurrency = baseCurrency,
        exchangeRates = exchangeRates,
        precision = precision
    )
}

/**
 * Compute Cash Flow data for CashFlowCardConfig (using FinancialUseCases)
 * Filters transactions based on accountId if specified (for Account Detail screen)
 * For Accounts View, accountId is null so no filtering is applied
 */
suspend fun CashFlowCardConfig.computeCashFlowWithFinancialUseCases(
    allTransactions: List<Transaction>,
    financialUseCases: FinancialUseCases,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    precision: Int
): CashFlowResult {
    // If accountId is specified, filter to that account only (Account Detail screen)
    // If accountId is null, use all transactions (Accounts View screen)
    val filteredTransactions = if (accountId != null) {
        allTransactions.filter { it.accountId == accountId }
    } else {
        allTransactions
    }
    
    return financialUseCases.calculateCashFlow(
        allTransactions = filteredTransactions,
        timelineRange = timelineRange,
        baseCurrency = baseCurrency,
        exchangeRates = exchangeRates,
        precision = precision
    )
}

/**
 * Compute Currency Totals data for CurrencyBalancesCardConfig (using FinancialUseCases)
 * Filters accounts and transactions based on accountId if specified (for Account Detail screen)
 * For Accounts View, accountId is null so no filtering is applied
 */
suspend fun CurrencyBalancesCardConfig.computeCurrencyTotalsWithFinancialUseCases(
    accounts: List<Account>,
    allTransactions: List<Transaction>,
    financialUseCases: FinancialUseCases,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    precision: Int
): List<CurrencyAggregationItem> {
    // If accountId is specified, filter to that account only (Account Detail screen)
    // If accountId is null, use all accounts (Accounts View screen)
    val filteredAccounts = if (accountId != null) {
        accounts.filter { it.id == accountId }
    } else {
        accounts
    }
    
    val filteredTransactions = if (accountId != null) {
        allTransactions.filter { it.accountId == accountId }
    } else {
        allTransactions
    }
    
    return financialUseCases.getCurrencyAggregation(
        accounts = filteredAccounts,
        allTransactions = filteredTransactions,
        timelineRange = timelineRange,
        baseCurrency = baseCurrency,
        exchangeRates = exchangeRates,
        precision = precision
    )
}
suspend fun AccountsCardConfig.computeAccountStats(
    accounts: List<Account>,
    allTransactions: List<Transaction>,
    cardComputationService: CardComputationService,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    precision: Int
): Pair<List<Account>, Map<String, AccountSummaryItem>> {
    return cardComputationService.computeAccountStats(
        config = this,
        accounts = accounts,
        transactions = allTransactions,
        baseCurrency = baseCurrency,
        exchangeRates = exchangeRates,
        precision = precision
    )
}

/**
 * Compute Currency Totals data for CurrencyBalancesCardConfig (using CardComputationService)
 * This is an alternative method that uses the CardComputationService instead of FinancialUseCases
 */
suspend fun CurrencyBalancesCardConfig.computeCurrencyTotalsWithCardComputationService(
    accounts: List<Account>,
    allTransactions: List<Transaction>,
    cardComputationService: CardComputationService,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    precision: Int
): List<CurrencyAggregationItem> {
    // Filter based on accountId if specified
    val filteredAccounts = if (accountId != null) {
        accounts.filter { it.id == accountId }
    } else {
        accounts
    }
    
    val filteredTransactions = if (accountId != null) {
        allTransactions.filter { it.accountId == accountId }
    } else {
        allTransactions
    }
    
    return cardComputationService.computeCurrencyTotals(
        config = this,
        accounts = filteredAccounts,
        transactions = filteredTransactions,
        baseCurrency = baseCurrency,
        exchangeRates = exchangeRates,
        precision = precision
    )
}

/**
 * Compute Cash Flow data for CashFlowCardConfig (using CardComputationService)
 * This is an alternative method that uses the CardComputationService instead of FinancialUseCases
 */
suspend fun CashFlowCardConfig.computeCashFlowWithCardComputationService(
    allTransactions: List<Transaction>,
    cardComputationService: CardComputationService,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    precision: Int
): CashFlowResult {
    // Filter based on accountId if specified
    val filteredTransactions = if (accountId != null) {
        allTransactions.filter { it.accountId == accountId }
    } else {
        allTransactions
    }
    
    return cardComputationService.computeCashFlow(
        config = this,
        transactions = filteredTransactions,
        baseCurrency = baseCurrency,
        exchangeRates = exchangeRates,
        precision = precision
    )
}
