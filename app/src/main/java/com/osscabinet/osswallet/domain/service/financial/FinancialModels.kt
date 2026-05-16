package com.osscabinet.ossWallet.domain.service.financial

import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionType
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
    val transactionCount: Int
) {
    /**
     * Net flow (income + expense)
     */
    val netFlow: BigDecimal get() = income + expense

    companion object {
        /**
         * Empty financial summary for use when no transactions are available.
         */
        val EMPTY = FinancialSummary(
            totalAmount = BigDecimal.ZERO,
            income = BigDecimal.ZERO,
            expense = BigDecimal.ZERO,
            transfer = BigDecimal.ZERO,
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
                transactionCount = summaries.sumOf { it.transactionCount }
            )
        }
    }
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
data class CategoryTreeMetrics(
    val category: String,
    val categoryId: Long?,
    val netBalance: BigDecimal,
    val income: BigDecimal,
    val expense: BigDecimal,
    val colorHex: Int,
    val percentage: Float,
    val hasSubcategories: Boolean = false,
    val subcategoryData: List<CategoryTreeMetrics> = emptyList()
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
