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
    val netInBase: BigDecimal,
    val breakdown: List<CashFlowBreakdownItem>,
    val filteredTransactions: List<Transaction>
) {
    companion object {
        fun empty() = CashFlowResult(
            totalIncomeInBase = BigDecimal.ZERO,
            totalExpensesInBase = BigDecimal.ZERO,
            netInBase = BigDecimal.ZERO,
            breakdown = emptyList(),
            filteredTransactions = emptyList()
        )
    }
}

/**
 * Breakdown of cash flow by currency.
 */
data class CashFlowBreakdownItem(
    val currency: String,
    val income: BigDecimal,
    val incomeInBase: BigDecimal,
    val expense: BigDecimal,
    val expenseInBase: BigDecimal
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
    val currencyBalances: Map<String, BigDecimal>,
    val balance: BigDecimal, // Total balance in account currency
    val balanceInBase: BigDecimal,
    val incomeInAccountCurrency: BigDecimal,
    val expenseInAccountCurrency: BigDecimal
)
