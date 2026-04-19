package com.osscabinet.ossWallet.domain.usecase

import com.osscabinet.ossWallet.domain.repository.AccountRepository

class AccountUseCases(
    private val repository: AccountRepository
) {
    suspend fun deleteAccount(accountId: String) {
        repository.deleteAccount(accountId)
    }
}
