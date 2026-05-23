package com.osscabinet.ossWallet.domain.repository

import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.LoanPayment
import kotlinx.coroutines.flow.Flow

interface LoanRepository {
    fun getActiveLoans(profileId: Int): Flow<List<Loan>>
    fun getAllLoans(profileId: Int): Flow<List<Loan>>
    suspend fun getActiveLoansSync(profileId: Int): List<Loan>
    suspend fun getLoanById(id: Long): Loan?
    fun getPaymentsForLoan(loanId: Long): Flow<List<LoanPayment>>
    suspend fun getPaymentsForLoanSync(loanId: Long): List<LoanPayment>
    fun getAllPayments(): Flow<List<LoanPayment>>
    suspend fun insertLoan(loan: Loan): Long
    suspend fun updateLoan(loan: Loan)
    suspend fun deleteLoan(loanId: Long)
    suspend fun insertPayment(payment: LoanPayment): Long
    suspend fun updatePayment(payment: LoanPayment)
    suspend fun deletePayment(paymentId: Long)
}
