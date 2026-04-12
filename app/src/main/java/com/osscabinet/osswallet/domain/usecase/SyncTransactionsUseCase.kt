package com.osscabinet.ossWallet.domain.usecase

import com.osscabinet.ossWallet.domain.repository.SharedWalletRepository

class SyncTransactionsUseCase(
    private val repository: SharedWalletRepository
) {
    suspend operator fun invoke(walletId: String) {
        repository.syncTransactions(walletId)
    }
}
