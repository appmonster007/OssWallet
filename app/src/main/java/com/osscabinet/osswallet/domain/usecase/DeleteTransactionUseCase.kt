package com.osscabinet.ossWallet.domain.usecase

import com.osscabinet.ossWallet.domain.repository.TransactionRepository

class DeleteTransactionUseCase(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(id: Long) {
        repository.deleteTransaction(id)
    }
}
