package com.osscabinet.gleemanager.domain.usecase

import com.osscabinet.gleemanager.domain.model.Transaction
import com.osscabinet.gleemanager.domain.repository.TransactionRepository

class InsertTransactionUseCase(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction, profileId: Int = 1) {
        repository.insertTransaction(transaction, profileId)
    }
}
