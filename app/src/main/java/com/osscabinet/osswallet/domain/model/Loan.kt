package com.osscabinet.ossWallet.domain.model

import java.math.BigDecimal

data class Loan(
    val id: Long = 0,
    val title: String,
    val principal: BigDecimal,
    val interestRate: BigDecimal,
    val dueDate: Long,
    val repayments: BigDecimal = BigDecimal.ZERO,
    val type: LoanType,
    val currency: String = "USD"
)

enum class LoanType {
    LOAN, DEBT
}
