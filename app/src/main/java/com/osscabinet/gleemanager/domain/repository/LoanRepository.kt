package com.osscabinet.gleemanager.domain.repository

import com.osscabinet.gleemanager.domain.model.Loan
import kotlinx.coroutines.flow.Flow

interface LoanRepository {
    fun getAllLoans(): Flow<List<Loan>>
    suspend fun insertLoan(loan: Loan)
    suspend fun updateLoan(loan: Loan)
    suspend fun deleteLoan(loan: Loan)
}
