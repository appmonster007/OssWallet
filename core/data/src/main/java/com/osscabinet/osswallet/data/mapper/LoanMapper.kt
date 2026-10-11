package com.osscabinet.ossWallet.data.mapper

import com.osscabinet.ossWallet.data.local.entity.LoanEntity
import com.osscabinet.ossWallet.data.local.entity.LoanPaymentEntity
import com.osscabinet.ossWallet.data.local.entity.LoanWithFilter
import com.osscabinet.ossWallet.domain.model.AdvancedFilterCriteria
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.LoanPayment
import com.osscabinet.ossWallet.domain.model.LoanStatus
import com.osscabinet.ossWallet.domain.model.LoanType

fun LoanWithFilter.toDomain(): Loan {
    val filterDomain = filter.toDomain()
    return Loan(
        id = loan.id,
        name = loan.name,
        principalAmount = loan.principalAmount,
        currency = loan.currency,
        interestRate = loan.interestRate,
        startDate = loan.startDate,
        lenderOrBorrowerName = loan.lenderOrBorrowerName,
        loanType = LoanType.valueOf(loan.loanType),
        linkedAccountId = loan.linkedAccountId,
        expectedSettlementDate = loan.expectedSettlementDate,
        status = LoanStatus.valueOf(loan.status),
        profileId = loan.profileId,
        note = loan.note,
        categoryIds = filterDomain.filter.categoryIds,
        tags = filterDomain.filter.tags,
        trackTypes = filterDomain.filter.transactionTypes,
        currencies = filterDomain.filter.currencies,
        paymentMethods = filterDomain.filter.paymentMethods,
        statuses = filterDomain.filter.statuses,
        linkedTransactionIds = filterDomain.filter.linkedTransactionIds,
        timelineRange = filterDomain.filter.timelineRange,
        advanced = AdvancedFilterCriteria.from(filterDomain.filter),
        filterId = loan.filterId
    )
}

fun Loan.toEntity(filterId: Long): LoanEntity = LoanEntity(
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
    filterId = filterId
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
