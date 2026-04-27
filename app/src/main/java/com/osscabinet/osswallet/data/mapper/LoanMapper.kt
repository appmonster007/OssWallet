package com.osscabinet.ossWallet.data.mapper

import com.osscabinet.ossWallet.data.local.entity.LoanEntity
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.domain.util.toEnum

/**
 * Mappers for Loan data.
 */
fun LoanEntity.toDomain(account: Account) = Loan(
    account = account,
    principal = principal,
    interestRate = interestRate,
    maturityDate = maturityDate,
    loanType = loanType.toEnum<LoanType>()
)

fun Loan.toEntity() = LoanEntity(
    accountId = account.id,
    principal = principal,
    interestRate = interestRate,
    maturityDate = maturityDate,
    loanType = loanType.name
)
