package com.osscabinet.ossWallet.data.web.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateLoanRequest(
    val name: String,
    val principalAmount: Double,
    val currency: String,
    val loanType: String,
    val lenderOrBorrowerName: String,
    val startDate: Long,
    val expectedSettlementDate: Long? = null,
    val interestRate: Double? = null,
    val note: String = ""
)
