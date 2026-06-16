package com.osscabinet.ossWallet.domain.model.analytics

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import java.math.BigDecimal

/**
 * Result of cash flow computation.
 */
data class CashFlowResult(
    val totalIncomeInBase: BigDecimal,
    val totalExpensesInBase: BigDecimal,
    val totalTransfersInBase: BigDecimal,
    val totalTransferInInBase: BigDecimal = BigDecimal.ZERO,
    val totalTransferOutInBase: BigDecimal = BigDecimal.ZERO,
    val totalCrossCcyTransferInInBase: BigDecimal = BigDecimal.ZERO,
    val totalCrossCcyTransferOutInBase: BigDecimal = BigDecimal.ZERO,
    val netInBase: BigDecimal,
    val breakdown: List<CashFlowBreakdownItem>,
    val filteredTransactions: List<Transaction>
) {
    companion object {
        fun empty() = CashFlowResult(
            totalIncomeInBase = BigDecimal.ZERO,
            totalExpensesInBase = BigDecimal.ZERO,
            totalTransfersInBase = BigDecimal.ZERO,
            totalTransferInInBase = BigDecimal.ZERO,
            totalTransferOutInBase = BigDecimal.ZERO,
            totalCrossCcyTransferInInBase = BigDecimal.ZERO,
            totalCrossCcyTransferOutInBase = BigDecimal.ZERO,
            netInBase = BigDecimal.ZERO,
            breakdown = emptyList(),
            filteredTransactions = emptyList()
        )
    }
}

/**
 * Breakdown of cash flow by currency.
 *
 * - [transfer] is the magnitude of all transfer activity (ABS sum).
 * - [transferIn] / [transferOut] are gross-positive / gross-magnitude signed-direction sums.
 * - [crossCcyTransferIn] / [crossCcyTransferOut] only count transfer legs where
 *   `currency != targetCurrency`. Useful at currency level where same-ccy transfers
 *   self-cancel in their bucket.
 */
data class CashFlowBreakdownItem(
    val currency: String,
    val income: BigDecimal,
    val incomeInBase: BigDecimal,
    val expense: BigDecimal,
    val expenseInBase: BigDecimal,
    val transfer: BigDecimal,
    val transferInBase: BigDecimal,
    val transferIn: BigDecimal = BigDecimal.ZERO,
    val transferInInBase: BigDecimal = BigDecimal.ZERO,
    val transferOut: BigDecimal = BigDecimal.ZERO,
    val transferOutInBase: BigDecimal = BigDecimal.ZERO,
    val crossCcyTransferIn: BigDecimal = BigDecimal.ZERO,
    val crossCcyTransferInInBase: BigDecimal = BigDecimal.ZERO,
    val crossCcyTransferOut: BigDecimal = BigDecimal.ZERO,
    val crossCcyTransferOutInBase: BigDecimal = BigDecimal.ZERO,
)

/**
 * Aggregated total for a single currency.
 */
data class CurrencyAggregationItem(
    val currency: String,
    val amount: BigDecimal,
    val amountInBase: BigDecimal
) {
    companion object {
        fun empty(currency: String = "") = CurrencyAggregationItem(
            currency = currency,
            amount = BigDecimal.ZERO,
            amountInBase = BigDecimal.ZERO
        )
    }
}

/**
 * Summary statistics for an account.
 */
data class AccountSummaryItem(
    val account: Account,
    val currencyBalances: Map<String, BigDecimal> = emptyMap(),
    val balance: BigDecimal = BigDecimal.ZERO, // Total balance in account currency
    val balanceInBase: BigDecimal = BigDecimal.ZERO,
    val currentPeriodFlow: AccountFlowMetrics = AccountFlowMetrics(),
    val previousPeriodFlow: AccountFlowMetrics? = null
)

/**
 * Encapsulates flow-based metrics for an account or group of accounts.
 */
data class AccountFlowMetrics(
    val income: BigDecimal = BigDecimal.ZERO,
    val expense: BigDecimal = BigDecimal.ZERO,
    val transferIn: BigDecimal = BigDecimal.ZERO,
    val transferOut: BigDecimal = BigDecimal.ZERO,
    val crossCcyTransferIn: BigDecimal = BigDecimal.ZERO,
    val crossCcyTransferOut: BigDecimal = BigDecimal.ZERO
) {
    val netTransfer: BigDecimal get() = transferIn + transferOut
    val netCrossCcyTransfer: BigDecimal get() = crossCcyTransferIn + crossCcyTransferOut
}
