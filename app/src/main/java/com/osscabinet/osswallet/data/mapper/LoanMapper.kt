package com.osscabinet.ossWallet.data.mapper

import com.osscabinet.ossWallet.data.local.entity.LoanEntity
import com.osscabinet.ossWallet.data.local.entity.LoanPaymentEntity
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.util.splitByComma
import com.osscabinet.ossWallet.domain.util.joinByComma

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
    note = note,
    categoryIds = categoryIds.splitByComma().mapNotNull { it.toLongOrNull() },
    tags = tags.splitByComma()
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
    note = note,
    categoryIds = categoryIds.map { it.toString() }.joinByComma(),
    tags = tags.joinByComma()
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
