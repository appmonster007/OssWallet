package com.osscabinet.ossWallet.domain.usecase

import com.osscabinet.ossWallet.domain.repository.AccountRepository

class DeleteAccountUseCase(
    private val repository: AccountRepository
) {
    suspend operator fun invoke(accountId: String) {
        repository.deleteAccount(accountId)
    }
}
