package com.osscabinet.ossWallet.domain.service.report

import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import java.math.BigDecimal

/** What the report covers, already resolved to human-readable labels for the header. */
data class ReportScope(
    val title: String,
    val periodLabel: String,
    val accountsLabel: String,
    val tagsLabel: String,
    val includesTransfers: Boolean,
    val categoriesLabel: String = "All categories",
    /** The concrete dates the period resolved to; empty when unknown. */
    val periodRangeText: String = "",
    val typesLabel: String = "Income and expenses",
    /** Extra narrowing (search text, amount range, ...), empty when none. */
    val otherFiltersLabel: String = ""
)

/** One transaction line. [amount] is signed: expenses negative, income positive. */
data class ReportRow(
    val timestamp: Long,
    val name: String,
    val category: String,
    val account: String,
    val type: TransactionType,
    val amount: BigDecimal,
    val currency: String,
    val payee: String,
    val tags: List<String>,
    val note: String,
    val status: TransactionStatus
)

/** Totals for one currency. Money in different currencies is never added together. */
data class ReportCurrencyTotals(
    val currency: String,
    val income: BigDecimal,
    val expense: BigDecimal,
    val transferIn: BigDecimal,
    val transferOut: BigDecimal,
    val count: Int
) {
    /** Income plus (negative) expenses; transfers move money between accounts and are excluded. */
    val net: BigDecimal get() = income.add(expense)
}

/** Spending in one category, as a share of that currency's total spending. */
data class ReportCategoryTotal(
    val category: String,
    val currency: String,
    /** Positive magnitude. */
    val amount: BigDecimal,
    /** 0..1 */
    val share: Double
)

data class ReportData(
    val scope: ReportScope,
    val generatedAt: Long,
    val rows: List<ReportRow>,
    val totals: List<ReportCurrencyTotals>,
    val topExpenseCategories: List<ReportCategoryTotal>
) {
    val isEmpty: Boolean get() = rows.isEmpty()
}
