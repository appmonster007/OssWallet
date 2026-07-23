package com.osscabinet.ossWallet.domain.model.analytics

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.util.BigDecimalSerializer
import com.osscabinet.ossWallet.domain.service.financial.FinancialSummary
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * Result of cash flow computation.
 */
@Serializable
data class CashFlowResult(
    val metricsInBase: FinancialSummary,
    val breakdown: List<CashFlowBreakdownItem>,
    val filteredTransactions: List<Transaction>
) {
    companion object {
        fun empty() = CashFlowResult(
            metricsInBase = FinancialSummary.EMPTY,
            breakdown = emptyList(),
            filteredTransactions = emptyList()
        )
    }
}

/**
 * Breakdown of cash flow by currency.
 */
@Serializable
data class CashFlowBreakdownItem(
    val currency: String,
    val metrics: FinancialSummary,
    val metricsInBase: FinancialSummary
)

/**
 * Aggregated total for a single currency.
 */
@Serializable
data class CurrencyAggregationItem(
    val currency: String,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
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
@Serializable
data class AccountSummaryItem(
    val account: Account,
    val currencyBalances: Map<String, @Serializable(with = BigDecimalSerializer::class) BigDecimal> = emptyMap(),
    @Serializable(with = BigDecimalSerializer::class)
    val balance: BigDecimal = BigDecimal.ZERO, // Total balance in account currency
    @Serializable(with = BigDecimalSerializer::class)
    val balanceInBase: BigDecimal = BigDecimal.ZERO,
    val currentPeriodMetrics: FinancialSummary,
    val previousPeriodMetrics: FinancialSummary? = null,
    // Running account balance across the filtered period, one point per time bucket
    // (in account currency). Empty when there is no history to plot.
    val balanceHistory: List<@Serializable(with = BigDecimalSerializer::class) BigDecimal> = emptyList()
)
