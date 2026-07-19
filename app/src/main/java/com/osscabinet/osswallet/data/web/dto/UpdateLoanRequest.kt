package com.osscabinet.ossWallet.data.web.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateLoanRequest(
    val name: String? = null,
    val principalAmount: Double? = null,
    val currency: String? = null,
    val loanType: String? = null,
    val lenderOrBorrowerName: String? = null,
    val startDate: Long? = null,
    val expectedSettlementDate: Long? = null,
    val interestRate: Double? = null,
    val note: String? = null,
    val linkedAccountId: String? = null
)

@Serializable
data class LoanPaymentRequest(
    val amount: Double,
    val date: Long,
    val note: String = ""
)

@Serializable
data class LoanPaymentDto(
    val id: Long,
    val loanId: Long,
    val amount: Double,
    val date: Long,
    val note: String
)
