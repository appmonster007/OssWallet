package com.osscabinet.ossWallet.domain.usecase

import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository

class UpdateTransactionUseCase(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(oldTransaction: Transaction, newTransaction: Transaction, profileId: Int) {
        transactionRepository.updateTransaction(oldTransaction, newTransaction, profileId)
    }
}
