package com.osscabinet.ossWallet.domain.model

data class Loan(
    val id: Long = 0,
    val title: String,
    val principal: Double,
    val interestRate: Double,
    val dueDate: Long,
    val repayments: Double = 0.0,
    val type: LoanType,
    val currency: String = "USD"
)

enum class LoanType {
    LOAN, DEBT
}
