package com.osscabinet.ossWallet.domain.repository

import com.osscabinet.ossWallet.domain.model.Loan
import kotlinx.coroutines.flow.Flow

interface LoanRepository {
    fun getAllLoans(): Flow<List<Loan>>
    suspend fun insertLoan(loan: Loan)
    suspend fun updateLoan(loan: Loan)
    suspend fun deleteLoan(loan: Loan)
}
