package com.osscabinet.gleemanager.domain.usecase

import com.osscabinet.gleemanager.domain.model.Transaction
import com.osscabinet.gleemanager.domain.repository.AccountRepository
import com.osscabinet.gleemanager.domain.repository.TransactionRepository

class UpdateTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(oldTransaction: Transaction, newTransaction: Transaction, profileId: Int) {
        // Impact management now handled in UI layer to use exchange rates
        transactionRepository.insertTransaction(newTransaction, profileId)
    }
}
