package com.osscabinet.ossWallet.data.web.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoanDto(
    val id: Long,
    val name: String,
    val loanType: String,   // "LENT" or "BORROWED"
    val currency: String,
    val principalAmount: Double,
    val totalPaid: Double,
    val remainingBalance: Double,
    val interestAccrued: Double,
    val status: String,     // "ACTIVE" or "SETTLED"
    val lenderOrBorrowerName: String,
    val startDate: Long,
    val expectedSettlementDate: Long?,
    val interestRate: Double? = null,
    val linkedAccountId: String? = null
)
