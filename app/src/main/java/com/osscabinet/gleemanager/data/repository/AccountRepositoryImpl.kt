package com.osscabinet.gleemanager.data.repository

import com.osscabinet.gleemanager.data.local.dao.AccountDao
import com.osscabinet.gleemanager.data.local.entity.AccountEntity
import com.osscabinet.gleemanager.domain.model.Account
import com.osscabinet.gleemanager.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AccountRepositoryImpl(private val accountDao: AccountDao) : AccountRepository {
    override fun getAllAccounts(profileId: Int): Flow<List<Account>> = accountDao.getAllAccounts(profileId).map { entities ->
        entities.map { it.toDomain() }
    }

    override suspend fun insertAccount(account: Account, profileId: Int) {
        accountDao.upsertAccount(account.toEntity(profileId))
    }

    override suspend fun updateBalance(accountId: String, amount: Double) {
        accountDao.updateBalance(accountId, amount)
    }

    override suspend fun deleteAccount(accountId: String) {
        accountDao.getAccountById(accountId)?.let { accountDao.deleteAccount(it) }
    }

    private fun AccountEntity.toDomain() = Account(id, name, balance, currencyCode, accountType, colorHex)
    private fun Account.toEntity(profileId: Int) = AccountEntity(id, name, balance, currencyCode, accountType, colorHex, profileId)
}
