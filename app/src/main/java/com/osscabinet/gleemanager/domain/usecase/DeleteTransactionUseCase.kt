package com.osscabinet.gleemanager.domain.usecase

import com.osscabinet.gleemanager.domain.repository.TransactionRepository

class DeleteTransactionUseCase(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(id: Long) {
        repository.deleteTransaction(id)
    }
}
