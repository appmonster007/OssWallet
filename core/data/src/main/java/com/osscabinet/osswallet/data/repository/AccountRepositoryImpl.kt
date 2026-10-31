package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.local.dao.AccountDao
import com.osscabinet.ossWallet.data.mapper.toDomain
import com.osscabinet.ossWallet.data.mapper.toEntity
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.CreditCardDetails
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao
) : AccountRepository {

    override fun getAllAccounts(profileId: Int): Flow<List<Account>> =
        accountDao.getAllAccounts(profileId).map { entities ->
            val allCreditDetails = accountDao.getAllCreditCardDetailsSync().associateBy { it.accountId }
            entities.map { entity ->
                val creditDetails = allCreditDetails[entity.id]?.toDomain()
                entity.toDomain(creditDetails)
            }
        }

    override suspend fun getAllAccountsSync(profileId: Int): List<Account> {
        val allCreditDetails = accountDao.getAllCreditCardDetailsSync().associateBy { it.accountId }
        return accountDao.getAllAccountsSync(profileId).map { entity ->
            val creditDetails = allCreditDetails[entity.id]?.toDomain()
            entity.toDomain(creditDetails)
        }
    }

    override suspend fun insertAccount(account: Account, profileId: Int) {
        accountDao.upsertAccount(account.toEntity(profileId))
        account.creditDetails?.let {
            accountDao.upsertCreditCardDetails(it.toEntity())
        }
    }

    override suspend fun updateAccount(account: Account, profileId: Int) {
        accountDao.upsertAccount(account.toEntity(profileId))
        account.creditDetails?.let {
            accountDao.upsertCreditCardDetails(it.toEntity())
        } ?: run {
            accountDao.deleteCreditCardDetails(account.id)
        }
    }

    override suspend fun deleteAccount(accountId: String, profileId: Int) {
        accountDao.getAccountById(accountId, profileId)?.let {
            accountDao.deleteAccount(it)
        }
        accountDao.deleteCreditCardDetails(accountId)
    }

    override suspend fun updateAccounts(accounts: List<Account>, profileId: Int) {
        accountDao.upsertAccounts(accounts.map { it.toEntity(profileId) })
        accounts.forEach { account ->
            account.creditDetails?.let {
                accountDao.upsertCreditCardDetails(it.toEntity())
            }
        }
    }

    override suspend fun saveCreditCardDetails(details: CreditCardDetails) {
        accountDao.upsertCreditCardDetails(details.toEntity())
    }

    override suspend fun getCreditCardDetails(accountId: String): CreditCardDetails? {
        return accountDao.getCreditCardDetails(accountId)?.toDomain()
    }
}
