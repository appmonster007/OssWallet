package com.osscabinet.ossWallet.ui.components.cards

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.domain.model.analytics.ComputationContext
import com.osscabinet.ossWallet.domain.model.analytics.AccountSummaryItem
import com.osscabinet.ossWallet.domain.model.analytics.CashFlowResult
import com.osscabinet.ossWallet.domain.model.analytics.CurrencyAggregationItem
import com.osscabinet.ossWallet.domain.service.CardComputationEngine
import java.math.BigDecimal

/**
 * Unified compute logic for cards.
 * Extension functions on CardConfig types to compute data for rendering.
 */

/**
 * Compute NetWorth data for NetWorthCardConfig
 */
suspend fun NetWorthCardConfig.computeNetWorth(
    accounts: List<Account>,
    transactions: List<Transaction>,
    engine: CardComputationEngine,
    context: ComputationContext
): Pair<BigDecimal, CashFlowResult> {
    return engine.computeNetWorth(
        config = this,
        accounts = accounts,
        allTransactions = transactions,
        context = context
    )
}

/**
 * Compute Currency Totals data for CurrencyBalancesCardConfig
 */
suspend fun CurrencyBalancesCardConfig.computeCurrencyTotals(
    accounts: List<Account>,
    transactions: List<Transaction>,
    engine: CardComputationEngine,
    context: ComputationContext
): List<CurrencyAggregationItem> {
    return engine.computeCurrencyTotals(
        config = this,
        accounts = accounts,
        allTransactions = transactions,
        context = context
    )
}

/**
 * Compute Cash Flow data for CashFlowCardConfig
 */
suspend fun CashFlowCardConfig.computeCashFlow(
    transactions: List<Transaction>,
    engine: CardComputationEngine,
    context: ComputationContext
): CashFlowResult {
    return engine.computeCashFlow(
        config = this,
        allTransactions = transactions,
        context = context
    )
}

/**
 * Compute Account Stats data for AccountsCardConfig
 */
suspend fun AccountsCardConfig.computeAccountStats(
    accounts: List<Account>,
    transactions: List<Transaction>,
    engine: CardComputationEngine,
    context: ComputationContext
): Pair<List<Account>, Map<String, AccountSummaryItem>> {
    return engine.computeAccountStats(
        config = this,
        accounts = accounts,
        allTransactions = transactions,
        context = context
    )
}
