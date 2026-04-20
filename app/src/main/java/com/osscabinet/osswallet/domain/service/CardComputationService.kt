package com.osscabinet.ossWallet.domain.service

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.settings.CardConfig
import com.osscabinet.ossWallet.domain.model.settings.CardScope
import com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig
import com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig
import com.osscabinet.ossWallet.domain.usecase.CashFlowResult
import com.osscabinet.ossWallet.domain.usecase.CurrencyAggregationItem
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
}

/**
 * Implementation of CardComputationService using FinancialUseCases.
 * Handles scope-aware filtering and delegates to appropriate use cases.
 */
class CardComputationServiceImpl(
    private val financialUseCases: com.osscabinet.ossWallet.domain.usecase.FinancialUseCases
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
}
