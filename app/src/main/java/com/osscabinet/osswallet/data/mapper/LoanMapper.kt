package com.osscabinet.ossWallet.data.mapper

import com.osscabinet.ossWallet.data.local.entity.LoanEntity
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.LoanType

/**
 * Mappers for Loan data.
 */
fun LoanEntity.toDomain() = Loan(
    id = id,
    title = title,
    principal = principal,
    interestRate = interestRate,
    dueDate = dueDate,
    repayments = repayments,
    type = LoanType.valueOf(type),
    currency = currency
)

fun Loan.toEntity() = LoanEntity(
    id = id,
    title = title,
    principal = principal,
    interestRate = interestRate,
    dueDate = dueDate,
    repayments = repayments,
    type = type.name,
    currency = currency
)
