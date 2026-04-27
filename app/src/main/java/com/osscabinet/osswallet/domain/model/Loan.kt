package com.osscabinet.ossWallet.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Loan model that integrates with the Account system.
 * A Loan is essentially an Account with metadata for debt tracking.
 */
data class Loan(
    val account: Account,
    val principal: BigDecimal,
    val interestRate: BigDecimal,
    val maturityDate: Long,
    val loanType: LoanType
) {
    val id: String get() = account.id
    val title: String get() = account.name
    val currency: String get() = account.currencyCode
    
    // Balance is usually negative for a debt (Credit) and positive for a receivable (Debit)
    val currentBalance: BigDecimal get() = account.balance
    
    val totalPaid: BigDecimal get() = principal.subtract(currentBalance.abs()) // Simplified for now

    val progress: Float
        get() = if (principal.signum() > 0) {
            val paid = totalPaid
            (paid.divide(principal, 4, RoundingMode.HALF_UP)).toFloat().coerceIn(0f, 1f)
        } else 0f
}

enum class LoanType {
    CREDIT, // We owe money
    DEBIT   // Money owed to us
}
