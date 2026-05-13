package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.local.dao.LoanDao
import com.osscabinet.ossWallet.data.local.dao.LoanPaymentDao
import com.osscabinet.ossWallet.data.mapper.toDomain
import com.osscabinet.ossWallet.data.mapper.toEntity
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.LoanPayment
import com.osscabinet.ossWallet.domain.repository.LoanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LoanRepositoryImpl(
    private val loanDao: LoanDao,
    private val loanPaymentDao: LoanPaymentDao
) : LoanRepository {

    override fun getActiveLoans(profileId: Int): Flow<List<Loan>> =
        loanDao.getActiveLoans(profileId).map { list -> list.map { it.toDomain() } }

    override fun getAllLoans(profileId: Int): Flow<List<Loan>> =
        loanDao.getAllLoans(profileId).map { list -> list.map { it.toDomain() } }

    override suspend fun getActiveLoansSync(profileId: Int): List<Loan> =
        loanDao.getActiveLoansSync(profileId).map { it.toDomain() }

    override suspend fun getLoanById(id: Long): Loan? =
        loanDao.getLoanById(id)?.toDomain()

    override fun getPaymentsForLoan(loanId: Long): Flow<List<LoanPayment>> =
        loanPaymentDao.getPaymentsForLoan(loanId).map { list -> list.map { it.toDomain() } }

    override suspend fun getPaymentsForLoanSync(loanId: Long): List<LoanPayment> =
        loanPaymentDao.getPaymentsForLoanSync(loanId).map { it.toDomain() }

    override suspend fun insertLoan(loan: Loan): Long = loanDao.insert(loan.toEntity())

    override suspend fun updateLoan(loan: Loan) = loanDao.update(loan.toEntity())

    override suspend fun deleteLoan(loanId: Long) = loanDao.deleteById(loanId)

    override suspend fun insertPayment(payment: LoanPayment): Long =
        loanPaymentDao.insert(payment.toEntity())

    override suspend fun updatePayment(payment: LoanPayment) =
        loanPaymentDao.update(payment.toEntity())

    override suspend fun deletePayment(paymentId: Long) = loanPaymentDao.deleteById(paymentId)
}
