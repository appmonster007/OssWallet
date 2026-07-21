package com.osscabinet.ossWallet.domain.model.analytics

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.settings.AccountingPerspective
import com.osscabinet.ossWallet.domain.model.util.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * Result of cash flow computation.
 */
@Serializable
data class CashFlowResult(
    @Serializable(with = BigDecimalSerializer::class)
    val totalIncomeInBase: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val totalExpensesInBase: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val totalTransfersInBase: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val totalTransferInInBase: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val totalTransferOutInBase: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val totalCrossCcyTransferInInBase: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val totalCrossCcyTransferOutInBase: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val netInBase: BigDecimal,
    val breakdown: List<CashFlowBreakdownItem>,
    val filteredTransactions: List<Transaction>
) {
    val totalNetTransferInBase: BigDecimal get() = totalTransferInInBase.add(totalTransferOutInBase)
    val totalInflowInBase: BigDecimal get() = totalIncomeInBase.add(totalTransferInInBase)
    val totalOutflowInBase: BigDecimal get() = totalExpensesInBase.add(totalTransferOutInBase)
    val totalNetCrossCcyTransferInBase: BigDecimal
        get() = totalCrossCcyTransferInInBase.add(
            totalCrossCcyTransferOutInBase
        )

    /**
     * Transforms this result based on the requested perspective.
     */
    fun toPerspective(perspective: AccountingPerspective): CashFlowResult {
        return when (perspective) {
            is AccountingPerspective.Accounting -> this
            is AccountingPerspective.CashFlow -> this.copy(
                totalIncomeInBase = totalIncomeInBase.add(totalTransferInInBase),
                totalExpensesInBase = totalExpensesInBase.add(totalTransferOutInBase),
                totalTransfersInBase = BigDecimal.ZERO,
                totalTransferInInBase = BigDecimal.ZERO,
                totalTransferOutInBase = BigDecimal.ZERO,
                totalCrossCcyTransferInInBase = BigDecimal.ZERO,
                totalCrossCcyTransferOutInBase = BigDecimal.ZERO,
                breakdown = breakdown.map { it.toPerspective(perspective) }
            )
        }
    }

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
@Serializable
data class CashFlowBreakdownItem(
    val currency: String,
    @Serializable(with = BigDecimalSerializer::class)
    val income: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val incomeInBase: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val expense: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val expenseInBase: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val transfer: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val transferInBase: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val transferIn: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val transferInInBase: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val transferOut: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val transferOutInBase: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val crossCcyTransferIn: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val crossCcyTransferInInBase: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val crossCcyTransferOut: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val crossCcyTransferOutInBase: BigDecimal = BigDecimal.ZERO,
) {
    val netTransfer: BigDecimal get() = transferIn.add(transferOut)
    val netTransferInBase: BigDecimal get() = transferInInBase.add(transferOutInBase)
    val netCrossCcyTransfer: BigDecimal get() = crossCcyTransferIn.add(crossCcyTransferOut)
    val netCrossCcyTransferInBase: BigDecimal
        get() = crossCcyTransferInInBase.add(
            crossCcyTransferOutInBase
        )

    /**
     * Transforms this breakdown item based on the requested perspective.
     */
    fun toPerspective(perspective: AccountingPerspective): CashFlowBreakdownItem {
        return when (perspective) {
            is AccountingPerspective.Accounting -> this
            is AccountingPerspective.CashFlow -> this.copy(
                income = income.add(transferIn),
                incomeInBase = incomeInBase.add(transferInInBase),
                expense = expense.add(transferOut),
                expenseInBase = expenseInBase.add(transferOutInBase),
                transfer = BigDecimal.ZERO,
                transferInBase = BigDecimal.ZERO,
                transferIn = BigDecimal.ZERO,
                transferInInBase = BigDecimal.ZERO,
                transferOut = BigDecimal.ZERO,
                transferOutInBase = BigDecimal.ZERO,
                crossCcyTransferIn = BigDecimal.ZERO,
                crossCcyTransferInInBase = BigDecimal.ZERO,
                crossCcyTransferOut = BigDecimal.ZERO,
                crossCcyTransferOutInBase = BigDecimal.ZERO
            )
        }
    }
}

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
    val currentPeriodFlow: AccountFlowMetrics = AccountFlowMetrics(),
    val previousPeriodFlow: AccountFlowMetrics? = null,
    // Running account balance across the filtered period, one point per time bucket
    // (in account currency). Empty when there is no history to plot.
    val balanceHistory: List<@Serializable(with = BigDecimalSerializer::class) BigDecimal> = emptyList()
)

/**
 * Encapsulates flow-based metrics for an account or group of accounts.
 */
@Serializable
data class AccountFlowMetrics(
    @Serializable(with = BigDecimalSerializer::class)
    val income: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val expense: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val transferIn: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val transferOut: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val crossCcyTransferIn: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val crossCcyTransferOut: BigDecimal = BigDecimal.ZERO
) {
    val netTransfer: BigDecimal get() = transferIn + transferOut
    val netCrossCcyTransfer: BigDecimal get() = crossCcyTransferIn + crossCcyTransferOut

    /**
     * Transforms these metrics based on the requested perspective.
     * Accounting: keeps raw metrics.
     * CASH_FLOW: merges transfers into income/expense as inflow/outflow.
     */
    fun toPerspective(perspective: AccountingPerspective): AccountFlowMetrics {
        return when (perspective) {
            is AccountingPerspective.Accounting -> this
            is AccountingPerspective.CashFlow -> AccountFlowMetrics(
                income = income.add(transferIn),
                expense = expense.add(transferOut),
                transferIn = BigDecimal.ZERO,
                transferOut = BigDecimal.ZERO,
                crossCcyTransferIn = BigDecimal.ZERO,
                crossCcyTransferOut = BigDecimal.ZERO
            )
        }
    }
}
