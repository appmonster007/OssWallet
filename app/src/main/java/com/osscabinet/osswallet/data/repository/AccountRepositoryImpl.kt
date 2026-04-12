package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.local.dao.AccountDao
import com.osscabinet.ossWallet.data.local.entity.AccountEntity
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.repository.AccountRepository
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

    override suspend fun updateAccounts(accounts: List<Account>, profileId: Int) {
        accountDao.upsertAccounts(accounts.map { it.toEntity(profileId) })
    }

    private fun AccountEntity.toDomain() = Account(
        id = id,
        name = name,
        balance = balance,
        initialBalance = initialBalance,
        currencyCode = currencyCode,
        structureType = structureType,
        colorHex = colorHex,
        icon = icon,
        allowedCurrencies = allowedCurrencies.split(",").filter { it.isNotBlank() },
        isArchived = isArchived,
        displayOrder = displayOrder
    )

    private fun Account.toEntity(profileId: Int) = AccountEntity(
        id = id,
        name = name,
        balance = balance,
        initialBalance = initialBalance,
        currencyCode = currencyCode,
        structureType = structureType,
        colorHex = colorHex,
        icon = icon,
        allowedCurrencies = allowedCurrencies.joinToString(","),
        profileId = profileId,
        isArchived = isArchived,
        displayOrder = displayOrder
    )
}
