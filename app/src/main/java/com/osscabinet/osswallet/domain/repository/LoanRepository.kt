package com.osscabinet.ossWallet.domain.repository

import com.osscabinet.ossWallet.domain.model.Loan
import kotlinx.coroutines.flow.Flow

interface LoanRepository {
    fun getAllLoans(): Flow<List<Loan>>
    suspend fun getLoanByAccountId(accountId: String): Loan?
    suspend fun upsertLoan(loan: Loan)
    suspend fun deleteLoan(accountId: String)
}
