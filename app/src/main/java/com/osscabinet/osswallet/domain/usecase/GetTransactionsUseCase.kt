package com.osscabinet.ossWallet.domain.usecase

import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow

class GetTransactionsUseCase(
    private val repository: TransactionRepository
) {
    operator fun invoke(profileId: Int = 1): Flow<List<Transaction>> {
        return repository.getAllTransactions(profileId)
    }
}
