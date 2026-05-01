package com.osscabinet.ossWallet.data.mapper

import com.osscabinet.ossWallet.data.local.entity.LoanEntity
import com.osscabinet.ossWallet.data.local.entity.LoanPaymentEntity
import com.osscabinet.ossWallet.domain.model.*

fun LoanEntity.toDomain(): Loan = Loan(
    id = id,
    name = name,
    principalAmount = principalAmount,
    currency = currency,
    interestRate = interestRate,
    startDate = startDate,
    lenderOrBorrowerName = lenderOrBorrowerName,
    loanType = LoanType.valueOf(loanType),
    linkedAccountId = linkedAccountId,
    expectedSettlementDate = expectedSettlementDate,
    status = LoanStatus.valueOf(status),
    profileId = profileId,
    note = note
)

fun Loan.toEntity(): LoanEntity = LoanEntity(
    id = id,
    name = name,
    principalAmount = principalAmount,
    currency = currency,
    interestRate = interestRate,
    startDate = startDate,
    lenderOrBorrowerName = lenderOrBorrowerName,
    loanType = loanType.name,
    linkedAccountId = linkedAccountId,
    expectedSettlementDate = expectedSettlementDate,
    status = status.name,
    profileId = profileId,
    note = note
)

fun LoanPaymentEntity.toDomain(): LoanPayment = LoanPayment(
    id = id,
    loanId = loanId,
    transactionId = transactionId,
    amount = amount,
    date = date,
    note = note
)

fun LoanPayment.toEntity(): LoanPaymentEntity = LoanPaymentEntity(
    id = id,
    loanId = loanId,
    transactionId = transactionId,
    amount = amount,
    date = date,
    note = note
)
