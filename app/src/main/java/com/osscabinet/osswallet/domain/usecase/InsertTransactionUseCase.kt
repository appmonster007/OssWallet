package com.osscabinet.ossWallet.domain.usecase

import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.repository.TransactionRepository

class InsertTransactionUseCase(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction, profileId: Int = 1): Long {
        return repository.insertTransaction(transaction, profileId)
    }
}
