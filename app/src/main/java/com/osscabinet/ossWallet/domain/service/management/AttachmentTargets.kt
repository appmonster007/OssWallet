package com.osscabinet.ossWallet.domain.service.management

import com.osscabinet.ossWallet.domain.model.isTransfer
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import kotlinx.coroutines.flow.first

/**
 * The transactions an attachment on [id] belongs to: the transaction itself, plus the other leg of a
 * transfer, so both legs show the same files.
 */
suspend fun TransactionUseCases.attachmentTargets(id: Long, profileId: Int): List<Long> {
    val tx = getTransactionById(id, profileId) ?: return listOf(id)
    if (!tx.transactionType.isTransfer) return listOf(id)
    val counterparts = getLinkedTransactions(id).first()
        .filter { it.transactionType.isTransfer }.map { it.id }
    return listOf(id) + counterparts
}
