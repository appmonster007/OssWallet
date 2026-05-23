package com.osscabinet.ossWallet.data.web.dto

import kotlinx.serialization.Serializable

@Serializable
data class DashboardDto(
    val netWorth: Double,
    val baseCurrency: String,
    val monthlyIncome: Double,
    val monthlyExpenses: Double,
    val cashFlow: Double,
    val month: String,
    val accounts: List<AccountDto>,
    val budgets: List<BudgetDto>,
    val loans: List<LoanDto>,
    val upcomingPayments: List<PlannedPaymentDto>,
    val overduePayments: List<PlannedPaymentDto>,
    val recentTransactions: List<TransactionDto>,
    val categoryBreakdown: List<CategoryBreakdownDto>,
    val cashFlowPoints: List<CashFlowPointDto>
)
