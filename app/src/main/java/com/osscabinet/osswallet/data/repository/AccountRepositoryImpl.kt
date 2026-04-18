package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.local.dao.AccountDao
import com.osscabinet.ossWallet.data.local.entity.AccountEntity
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.ui.widget.WidgetUpdateHelper
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import android.content.Context

class AccountRepositoryImpl(
    private val accountDao: AccountDao,
    private val context: Context
) : AccountRepository {

    @OptIn(DelicateCoroutinesApi::class)
    private fun updateWidgets() {
        GlobalScope.launch {
            WidgetUpdateHelper.updateAllWidgets(context)
        }
    }

    override fun getAllAccounts(profileId: Int): Flow<List<Account>> = accountDao.getAllAccounts(profileId).map { entities ->
        entities.map { it.toDomain() }
    }

    override suspend fun getAllAccountsSync(profileId: Int): List<Account> {
        return accountDao.getAllAccountsSync().filter { it.profileId == profileId }.map { it.toDomain() }
    }

    override suspend fun insertAccount(account: Account, profileId: Int) {
        accountDao.upsertAccount(account.toEntity(profileId))
        updateWidgets()
    }

    override suspend fun updateBalance(accountId: String, amount: java.math.BigDecimal) {
        accountDao.updateBalance(accountId, amount)
        updateWidgets()
    }

    override suspend fun deleteAccount(accountId: String) {
        accountDao.getAccountById(accountId)?.let {
            accountDao.deleteAccount(it)
            updateWidgets()
        }
    }

    override suspend fun updateAccounts(accounts: List<Account>, profileId: Int) {
        accountDao.upsertAccounts(accounts.map { it.toEntity(profileId) })
        updateWidgets()
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
