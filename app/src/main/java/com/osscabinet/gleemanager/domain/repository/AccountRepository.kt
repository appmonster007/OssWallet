package com.osscabinet.gleemanager.domain.repository

import com.osscabinet.gleemanager.domain.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAllAccounts(profileId: Int): Flow<List<Account>>
    suspend fun insertAccount(account: Account, profileId: Int)
    suspend fun updateBalance(accountId: String, amount: Double)
    suspend fun deleteAccount(accountId: String)
}
