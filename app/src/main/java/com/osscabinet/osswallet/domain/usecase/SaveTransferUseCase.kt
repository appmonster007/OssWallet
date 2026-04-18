package com.osscabinet.ossWallet.domain.usecase

import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import javax.inject.Inject

class SaveTransferUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(
        sourceTransaction: Transaction,
        targetTransaction: Transaction,
        profileId: Int
    ): Pair<Long, Long> {
        return transactionRepository.saveTransfer(sourceTransaction, targetTransaction, profileId)
    }
}
