package com.osscabinet.gleemanager.domain.usecase

import com.osscabinet.gleemanager.domain.repository.SharedWalletRepository

class SyncTransactionsUseCase(
    private val repository: SharedWalletRepository
) {
    suspend operator fun invoke(walletId: String) {
        repository.syncTransactions(walletId)
    }
}
