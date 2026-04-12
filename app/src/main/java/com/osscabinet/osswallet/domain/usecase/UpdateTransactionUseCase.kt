package com.osscabinet.ossWallet.domain.usecase

import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository

class UpdateTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(oldTransaction: Transaction, newTransaction: Transaction, profileId: Int) {
        // Impact management now handled in UI layer to use exchange rates
        transactionRepository.insertTransaction(newTransaction, profileId)
    }
}
