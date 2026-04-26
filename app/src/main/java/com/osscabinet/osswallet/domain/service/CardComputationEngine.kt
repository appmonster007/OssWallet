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
import com.osscabinet.ossWallet.domain.model.analytics.ComputationContext
import java.math.BigDecimal

/**
 * Unified interface for card computation engines.
 * This abstraction allows different implementations (CardComputationService, FinancialUseCases)
 * to be used interchangeably for card data computation.
 * 
 * This interface defines the core computation methods needed for card rendering,
 * decoupling the compute logic from specific service implementations.
 */
interface CardComputationEngine {
    
    /**
     * Computes net worth and cash flow for a net worth card.
     * Filters accounts and allTransactions based on card configuration.
     */
    suspend fun computeNetWorth(
        config: NetWorthCardConfig,
        accounts: List<Account>,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): Pair<BigDecimal, CashFlowResult>
    
    /**
     * Computes currency totals for a currency balances card.
     * Filters accounts and allTransactions based on card configuration.
     */
    suspend fun computeCurrencyTotals(
        config: CurrencyBalancesCardConfig,
        accounts: List<Account>,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): List<CurrencyAggregationItem>
    
    /**
     * Computes cash flow data for a cash flow card.
     * Filters allTransactions based on card configuration.
     */
    suspend fun computeCashFlow(
        config: CashFlowCardConfig,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): CashFlowResult
    
    /**
     * Computes account statistics for an accounts card.
     * Filters allTransactions based on card configuration.
     */
    suspend fun computeAccountStats(
        config: AccountsCardConfig,
        accounts: List<Account>,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): Pair<List<Account>, Map<String, AccountSummaryItem>>
}
