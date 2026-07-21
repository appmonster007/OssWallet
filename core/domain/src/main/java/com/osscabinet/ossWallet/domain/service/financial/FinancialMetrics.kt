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
data class FinancialSummary(
    val totalAmount: BigDecimal,
    val income: BigDecimal,
    val expense: BigDecimal,
    val transfer: BigDecimal,
    val transactionCount: Int,
    val transferIn: BigDecimal = BigDecimal.ZERO,
    val transferOut: BigDecimal = BigDecimal.ZERO,
    val crossCcyTransferIn: BigDecimal = BigDecimal.ZERO,
    val crossCcyTransferOut: BigDecimal = BigDecimal.ZERO,
) {
    /**
     * Net flow (income + expense)
     */
    val netFlow: BigDecimal get() = income + expense

    /**
     * Transforms this summary based on the requested perspective.
     * Nature (ACCOUNTING): keeps raw metrics.
     * Directional (CASH_FLOW): merges transfers into income/expense as inflow/outflow.
     */
    fun toPerspective(perspective: AccountingPerspective): FinancialSummary {
        return when (perspective) {
            AccountingPerspective.Accounting -> this
            is AccountingPerspective.CashFlow -> this.copy(
                income = income.add(transferIn),
                expense = expense.add(transferOut),
                transfer = BigDecimal.ZERO // Transfers are absorbed into inflow/outflow
            )
        }
    }

    companion object {
        /**
         * Empty financial summary for use when no transactions are available.
         */
        val EMPTY = FinancialSummary(
            totalAmount = BigDecimal.ZERO,
            income = BigDecimal.ZERO,
            expense = BigDecimal.ZERO,
            transfer = BigDecimal.ZERO,
            transferIn = BigDecimal.ZERO,
            transferOut = BigDecimal.ZERO,
            crossCcyTransferIn = BigDecimal.ZERO,
            crossCcyTransferOut = BigDecimal.ZERO,
            transactionCount = 0
        )

        /**
         * Aggregates a collection of FinancialSummary objects into a single one.
         */
        fun aggregate(summaries: Collection<FinancialSummary>): FinancialSummary {
            if (summaries.isEmpty()) return EMPTY
            return FinancialSummary(
                totalAmount = summaries.fold(BigDecimal.ZERO) { acc, s -> acc.add(s.totalAmount) },
                income = summaries.fold(BigDecimal.ZERO) { acc, s -> acc.add(s.income) },
                expense = summaries.fold(BigDecimal.ZERO) { acc, s -> acc.add(s.expense) },
                transfer = summaries.fold(BigDecimal.ZERO) { acc, s -> acc.add(s.transfer) },
                transferIn = summaries.fold(BigDecimal.ZERO) { acc, s -> acc.add(s.transferIn) },
                transferOut = summaries.fold(BigDecimal.ZERO) { acc, s -> acc.add(s.transferOut) },
                crossCcyTransferIn = summaries.fold(BigDecimal.ZERO) { acc, s -> acc.add(s.crossCcyTransferIn) },
                crossCcyTransferOut = summaries.fold(BigDecimal.ZERO) { acc, s -> acc.add(s.crossCcyTransferOut) },
                transactionCount = summaries.sumOf { it.transactionCount }
            )
        }
    }
}

/**
 * Extracts a scalar value for this metric from a perspective-resolved [FinancialSummary].
 * Always call [FinancialSummary.toPerspective] before calling this — [ChartMetric.INCOME]
 * and [ChartMetric.EXPENSE] already include absorbed transfers in CashFlow perspective,
 * and [ChartMetric.TRANSFER] is zero in CashFlow perspective.
 */
fun ChartMetric.extractFrom(summary: FinancialSummary): BigDecimal = when (this) {
    ChartMetric.NET          -> summary.income + summary.expense + summary.transfer
    ChartMetric.INCOME       -> summary.income
    ChartMetric.EXPENSE      -> summary.expense
    ChartMetric.TRANSFER     -> summary.transfer
    ChartMetric.TRANSFER_IN  -> summary.transferIn
    ChartMetric.TRANSFER_OUT -> summary.transferOut
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
    val incomeChange: BigDecimal,
    val expenseChange: BigDecimal
)

/**
 * Financial metrics for a specific key/category with currency breakdown.
 * Used for all breakdown operations to provide consistent financial data structure.
 */
data class FinancialMetrics<K>(
    val key: K,
    val financialSummary: FinancialSummary,
    val currencyBreakdown: Map<String, FinancialSummary>
) {
    val totalAmount: BigDecimal get() = financialSummary.totalAmount
    val income: BigDecimal get() = financialSummary.income
    val expense: BigDecimal get() = financialSummary.expense
    val netFlow: BigDecimal get() = financialSummary.netFlow
    val transactionCount: Int get() = financialSummary.transactionCount
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
    @Serializable(with = BigDecimalSerializer::class)
    val netBalance: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val income: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val expense: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val transfer: BigDecimal = BigDecimal.ZERO,
    val colorHex: Int,
    val percentage: Float,
    val hasSubcategories: Boolean = false,
    val subcategoryData: List<CategoryTreeMetrics> = emptyList(),
    @Serializable(with = BigDecimalSerializer::class)
    val previousNetBalance: BigDecimal? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val previousIncome: BigDecimal? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val previousExpense: BigDecimal? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val previousTransfer: BigDecimal? = null
)

/**
 * Currency breakdown with detailed financial metrics.
 * Used by breakdownByCurrency to provide per-currency breakdowns.
 */
data class CurrencyBreakdown(
    val currency: String,
    val financialSummary: FinancialSummary
) {
    val totalAmount: BigDecimal get() = financialSummary.totalAmount
    val income: BigDecimal get() = financialSummary.income
    val expense: BigDecimal get() = financialSummary.expense
    val transactionCount: Int get() = financialSummary.transactionCount
}

/**
 * Complete breakdown of income and expense transactions with currency sub-grouping.
 * Uses FinancialSummary for totals to maintain DRY principle.
 */
data class IncomeExpenseBreakdown(
    val financialSummary: FinancialSummary,
    val incomeTransactions: List<Transaction>,
    val expenseTransactions: List<Transaction>,
    val incomeCurrencyBreakdown: Map<String, FinancialSummary>,
    val expenseCurrencyBreakdown: Map<String, FinancialSummary>
) {
    companion object {
        fun empty() = IncomeExpenseBreakdown(
            financialSummary = FinancialSummary.EMPTY,
            incomeTransactions = emptyList(),
            expenseTransactions = emptyList(),
            incomeCurrencyBreakdown = emptyMap(),
            expenseCurrencyBreakdown = emptyMap()
        )
    }

    // Convenience accessors for backward compatibility
    val totalIncome: BigDecimal get() = financialSummary.income
    val totalExpense: BigDecimal get() = financialSummary.expense
    val netFlow: BigDecimal get() = financialSummary.netFlow
    val totalAmount: BigDecimal get() = financialSummary.totalAmount
    val transactionCount: Int get() = financialSummary.transactionCount
}

/**
 * Generic label group with transactions and currency breakdown.
 * Unlike other breakdown types, this includes the actual transactions in each group.
 * Used by breakdownByLabel for custom label-based breakdowns.
 */
data class LabelGroup<L>(
    val label: L,
    val financialSummary: FinancialSummary,
    val transactions: List<Transaction>,
    val currencyBreakdown: Map<String, FinancialSummary>
) {
    val totalAmount: BigDecimal get() = financialSummary.totalAmount
    val income: BigDecimal get() = financialSummary.income
    val expense: BigDecimal get() = financialSummary.expense
    val transactionCount: Int get() = financialSummary.transactionCount
}
