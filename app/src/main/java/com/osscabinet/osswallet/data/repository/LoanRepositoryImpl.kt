package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.local.dao.LoanDao
import com.osscabinet.ossWallet.data.mapper.toDomain
import com.osscabinet.ossWallet.data.mapper.toEntity
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.repository.LoanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LoanRepositoryImpl(private val loanDao: LoanDao) : LoanRepository {
    override fun getAllLoans(): Flow<List<Loan>> {
        return loanDao.getAllLoans().map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun insertLoan(loan: Loan) {
        loanDao.insertLoan(loan.toEntity())
    }

    override suspend fun updateLoan(loan: Loan) {
        loanDao.updateLoan(loan.toEntity())
    }

    override suspend fun deleteLoan(loan: Loan) {
        loanDao.deleteLoan(loan.toEntity())
    }
}
