package com.osscabinet.ossWallet.domain.service.financial

import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.settings.AccountingPerspective
import com.osscabinet.ossWallet.domain.model.settings.ChartMetric
import com.osscabinet.ossWallet.domain.model.util.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * Base financial summary with common metrics for transaction calculations.
 * Used as the foundation for other financial data structures.
 */
@Serializable
data class FinancialSummary(
    @Serializable(with = BigDecimalSerializer::class)
    val income: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val expense: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val transferIn: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val transferOut: BigDecimal = BigDecimal.ZERO,
    val transactionCount: Int = 0
) {
    /**
     * Net flow (income + expense + transferIn + transferOut)
     */
    val netFlow: BigDecimal get() = income.add(expense).add(transferIn).add(transferOut)

    /**
     * Total amount (alias for netFlow for backward compatibility)
     */
    val totalAmount: BigDecimal get() = netFlow

    /**
     * Net transfer (transferIn + transferOut)
     */
    val transfer: BigDecimal get() = transferIn.add(transferOut)

    /**
     * Total inflow (income + transferIn)
     */
    val inflow: BigDecimal get() = income.add(transferIn)

    /**
     * Total outflow (expense + transferOut)
     */
    val outflow: BigDecimal get() = expense.add(transferOut)

    operator fun minus(other: FinancialSummary): FinancialSummary = FinancialSummary(
        income = income.subtract(other.income),
        expense = expense.subtract(other.expense),
        transferIn = transferIn.subtract(other.transferIn),
        transferOut = transferOut.subtract(other.transferOut),
        transactionCount = transactionCount - other.transactionCount
    )

    operator fun plus(other: FinancialSummary): FinancialSummary = FinancialSummary(
        income = income.add(other.income),
        expense = expense.add(other.expense),
        transferIn = transferIn.add(other.transferIn),
        transferOut = transferOut.add(other.transferOut),
        transactionCount = transactionCount + other.transactionCount
    )

    operator fun times(rate: BigDecimal): FinancialSummary = FinancialSummary(
        income = income.multiply(rate),
        expense = expense.multiply(rate),
        transferIn = transferIn.multiply(rate),
        transferOut = transferOut.multiply(rate),
        transactionCount = transactionCount
    )

    fun abs(): FinancialSummary = FinancialSummary(
        income = income.abs(),
        expense = expense.abs(),
        transferIn = transferIn.abs(),
        transferOut = transferOut.abs(),
        transactionCount = transactionCount
    )

    fun setScale(precision: Int, roundingMode: java.math.RoundingMode = java.math.RoundingMode.HALF_UP): FinancialSummary = FinancialSummary(
        income = income.setScale(precision, roundingMode),
        expense = expense.setScale(precision, roundingMode),
        transferIn = transferIn.setScale(precision, roundingMode),
        transferOut = transferOut.setScale(precision, roundingMode),
        transactionCount = transactionCount
    )

    companion object {
        /**
         * Empty financial summary for use when no transactions are available.
         */
        val EMPTY = FinancialSummary()

        /**
         * Aggregates a collection of FinancialSummary objects into a single one.
         */
        fun aggregate(summaries: Collection<FinancialSummary>): FinancialSummary {
            if (summaries.isEmpty()) return EMPTY
            return summaries.reduce { acc, s -> acc + s }
        }
    }
}

/**
 * Extracts a scalar value for this metric from a [FinancialSummary].
 */
fun ChartMetric.extractFrom(summary: FinancialSummary): BigDecimal = when (this) {
    ChartMetric.NET          -> summary.netFlow
    ChartMetric.INCOME       -> summary.income
    ChartMetric.EXPENSE      -> summary.expense
    ChartMetric.TRANSFER     -> summary.transfer
    ChartMetric.TRANSFER_IN  -> summary.transferIn
    ChartMetric.TRANSFER_OUT -> summary.transferOut
    ChartMetric.INFLOW       -> summary.inflow
    ChartMetric.OUTFLOW      -> summary.outflow
}

/**
 * Type alias for backward compatibility. TransactionSummary is now FinancialSummary.
 */
typealias TransactionSummary = FinancialSummary

/**
 * Represents the delta (change) between two financial summaries.
 */
data class FinancialDelta(
    val absoluteChange: BigDecimal,
    val percentageChange: Double,
    val delta: FinancialSummary
)

/**
 * Financial metrics for a specific key/category with currency breakdown.
 * Used for all breakdown operations to provide consistent financial data structure.
 */
data class FinancialMetrics<K>(
    val key: K,
    val metrics: FinancialSummary,
    val currencyBreakdown: Map<String, FinancialSummary>
) {
    val totalAmount: BigDecimal get() = metrics.totalAmount
    val transactionCount: Int get() = metrics.transactionCount
}

// Type aliases for specific breakdown types (for semantic clarity)
typealias AccountBreakdown = FinancialMetrics<String>
typealias CategoryBreakdown = FinancialMetrics<String>
typealias DateBreakdown = FinancialMetrics<String>
typealias MonthBreakdown = FinancialMetrics<String>
typealias TransactionTypeBreakdown = FinancialMetrics<TransactionType>
typealias PaymentMethodBreakdown = FinancialMetrics<PaymentMethod>
typealias TagBreakdown = FinancialMetrics<String>
typealias StatusBreakdown = FinancialMetrics<String>

/**
 * Hierarchical financial metrics for categories.
 * Supports tree structures for breakdown visualizations.
 * Note: When transactionType is EXPENSE_ONLY, netBalance == expense.
 * When transactionType is INCOME_ONLY, netBalance == income.
 */
@Serializable
data class CategoryTreeMetrics(
    val category: String,
    val categoryId: Long?,
    val metrics: FinancialSummary,
    val colorHex: Int,
    val percentage: Float,
    val hasSubcategories: Boolean = false,
    val subcategoryData: List<CategoryTreeMetrics> = emptyList(),
    val previousMetrics: FinancialSummary? = null
) {
    // Accessors for core properties
    val netBalance: BigDecimal get() = metrics.netFlow
    val previousNetBalance: BigDecimal? get() = previousMetrics?.netFlow
}

/**
 * Currency breakdown with detailed financial metrics.
 * Used by breakdownByCurrency to provide per-currency breakdowns.
 */
data class CurrencyBreakdown(
    val currency: String,
    val metrics: FinancialSummary
) {
    val totalAmount: BigDecimal get() = metrics.netFlow
    val transactionCount: Int get() = metrics.transactionCount
}

/**
 * Complete breakdown of income and expense transactions with currency sub-grouping.
 * Uses FinancialSummary for totals to maintain DRY principle.
 */
data class IncomeExpenseBreakdown(
    val metrics: FinancialSummary,
    val incomeTransactions: List<Transaction>,
    val expenseTransactions: List<Transaction>,
    val incomeCurrencyBreakdown: Map<String, FinancialSummary>,
    val expenseCurrencyBreakdown: Map<String, FinancialSummary>
) {
    companion object {
        fun empty() = IncomeExpenseBreakdown(
            metrics = FinancialSummary.EMPTY,
            incomeTransactions = emptyList(),
            expenseTransactions = emptyList(),
            incomeCurrencyBreakdown = emptyMap(),
            expenseCurrencyBreakdown = emptyMap()
        )
    }

    // Convenience accessors
    val netFlow: BigDecimal get() = metrics.netFlow
    val transactionCount: Int get() = metrics.transactionCount
}

/**
 * Generic label group with transactions and currency breakdown.
 * Unlike other breakdown types, this includes the actual transactions in each group.
 * Used by breakdownByLabel for custom label-based breakdowns.
 */
data class LabelGroup<L>(
    val label: L,
    val metrics: FinancialSummary,
    val transactions: List<Transaction>,
    val currencyBreakdown: Map<String, FinancialSummary>
) {
    val transactionCount: Int get() = metrics.transactionCount
}
