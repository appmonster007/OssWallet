package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.local.dao.AccountDao
import com.osscabinet.ossWallet.data.mapper.toDomain
import com.osscabinet.ossWallet.data.mapper.toEntity
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao
) : AccountRepository {

    override fun getAllAccounts(profileId: Int): Flow<List<Account>> = 
        accountDao.getAllAccounts(profileId).mapToDomain { it.toDomain() }

    override suspend fun getAllAccountsSync(profileId: Int): List<Account> {
        return accountDao.getAllAccountsSync(profileId).mapToDomain { it.toDomain() }
    }

    override suspend fun insertAccount(account: Account, profileId: Int) {
        accountDao.upsertAccount(account.toEntity(profileId))
    }

    override suspend fun updateAccount(account: Account, profileId: Int) {
        accountDao.upsertAccount(account.toEntity(profileId))
    }

    override suspend fun deleteAccount(accountId: String) {
        accountDao.getAccountById(accountId)?.let {
            accountDao.deleteAccount(it)
        }
    }

    override suspend fun updateAccounts(accounts: List<Account>, profileId: Int) {
        accountDao.upsertAccounts(accounts.map { it.toEntity(profileId) })
    }
}
