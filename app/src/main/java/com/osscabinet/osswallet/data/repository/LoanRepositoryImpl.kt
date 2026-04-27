package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.local.dao.AccountDao
import com.osscabinet.ossWallet.data.local.dao.LoanDao
import com.osscabinet.ossWallet.data.mapper.toDomain
import com.osscabinet.ossWallet.data.mapper.toEntity
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.repository.LoanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class LoanRepositoryImpl(
    private val loanDao: LoanDao,
    private val accountDao: AccountDao
) : LoanRepository {

    override fun getAllLoans(): Flow<List<Loan>> {
        return combine(
            loanDao.getAllLoans(),
            accountDao.getAllAccounts(1)
        ) { loanEntities, accountEntities ->
            loanEntities.mapNotNull { loanEntity ->
                val accountEntity = accountEntities.find { it.id == loanEntity.accountId }
                val account = accountEntity?.toDomain()
                if (account != null) {
                    loanEntity.toDomain(account)
                } else null
            }
        }
    }

    override suspend fun getLoanByAccountId(accountId: String): Loan? {
        val loanEntity = loanDao.getLoanByAccountId(accountId)
        val account = accountDao.getAccountById(accountId)?.toDomain()
        return if (loanEntity != null && account != null) {
            loanEntity.toDomain(account)
        } else null
    }

    override suspend fun upsertLoan(loan: Loan) {
        // First ensure the account exists/is updated
        accountDao.upsertAccount(loan.account.toEntity(1))
        // Then upsert loan metadata
        loanDao.upsertLoan(loan.toEntity())
    }

    override suspend fun deleteLoan(accountId: String) {
        val entity = loanDao.getLoanByAccountId(accountId)
        entity?.let { loanDao.deleteLoan(it) }
    }
}
