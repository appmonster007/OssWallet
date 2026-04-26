package com.osscabinet.ossWallet.data.mapper

import com.osscabinet.ossWallet.data.local.entity.LoanEntity
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.domain.util.toEnum

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
    type = type.toEnum<LoanType>(),
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
