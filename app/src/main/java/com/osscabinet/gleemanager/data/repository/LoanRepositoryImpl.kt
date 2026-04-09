package com.osscabinet.gleemanager.data.repository

import com.osscabinet.gleemanager.data.local.dao.LoanDao
import com.osscabinet.gleemanager.data.local.entity.LoanEntity
import com.osscabinet.gleemanager.domain.model.Loan
import com.osscabinet.gleemanager.domain.repository.LoanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LoanRepositoryImpl(private val loanDao: LoanDao) : LoanRepository {
    override fun getAllLoans(): Flow<List<Loan>> {
        return loanDao.getAllLoans().map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun insertLoan(loan: Loan) {
        loanDao.insertLoan(LoanEntity.fromDomain(loan))
    }

    override suspend fun updateLoan(loan: Loan) {
        loanDao.updateLoan(LoanEntity.fromDomain(loan))
    }

    override suspend fun deleteLoan(loan: Loan) {
        loanDao.deleteLoan(LoanEntity.fromDomain(loan))
    }
}
