package com.osscabinet.gleemanager.domain.usecase

import com.osscabinet.gleemanager.domain.repository.AccountRepository

class DeleteAccountUseCase(
    private val repository: AccountRepository
) {
    suspend operator fun invoke(accountId: String) {
        repository.deleteAccount(accountId)
    }
}
