package com.osscabinet.ossWallet.domain.repository

import com.osscabinet.ossWallet.domain.model.Account
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

interface AccountRepository {
    fun getAllAccounts(profileId: Int): Flow<List<Account>>
    suspend fun getAllAccountsSync(profileId: Int): List<Account>
    suspend fun insertAccount(account: Account, profileId: Int)
    suspend fun deleteAccount(accountId: String)
    suspend fun updateAccounts(accounts: List<Account>, profileId: Int)
}
