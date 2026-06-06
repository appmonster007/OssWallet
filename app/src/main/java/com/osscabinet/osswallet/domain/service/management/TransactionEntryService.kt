package com.osscabinet.ossWallet.domain.service.management

import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.TransactionSide
import com.osscabinet.ossWallet.domain.model.TransactionType

/**
 * Domain service to handle complex state transitions for transaction entry.
 * Centralizes the business logic for switching between transaction types.
 */
object TransactionEntryService {

    data class TypeTransitionResult(
        val newType: TransactionType,
        val source: TransactionSide,
        val destination: TransactionSide,
        val isSplitModeAllowed: Boolean
    )

    /**
     * Rules for switching types:
     * - Expense -> Transfer: Original account stays as SOURCE (Giver).
     * - Income -> Transfer: Original account moves to DESTINATION (Receiver).
     * - Transfer -> Normal: Revert to single side based on whether it was an Inflow or Outflow.
     */
    fun transitionType(
        oldType: TransactionType,
        newType: TransactionType,
        currentSource: TransactionSide,
        currentDestination: TransactionSide,
        isSplitMode: Boolean
    ): TypeTransitionResult {
        val isTransferToNormal = oldType == TransactionType.TRANSFER && newType != TransactionType.TRANSFER

        // Identify which side was the "Primary" account in the previous state.
        // If it was a Transfer where we were the Receiver, the primary account is in 'destination'.
        val wasTransferInflow = oldType == TransactionType.TRANSFER && Category.isAnyTransferInflow(currentSource.categoryId)
        val primarySide = if (isTransferToNormal && wasTransferInflow) currentDestination else currentSource

        val newSource: TransactionSide
        val newDestination: TransactionSide

        when (newType) {
            TransactionType.TRANSFER -> {
                if (oldType == TransactionType.INCOME) {
                    // Refinement: Income account was receiving money, so it becomes the Receiver (Target) in a transfer.
                    // The Source (Giver) is initialized empty but with transfer outflow category.
                    newSource = TransactionSide(
                        categoryId = Category.SYSTEM_TRANSFER_OUTFLOW,
                        amount = primarySide.amount,
                        currency = primarySide.currency
                    )
                    newDestination = primarySide.copy(
                        categoryId = Category.SYSTEM_TRANSFER_INFLOW
                    )
                } else {
                    // Expense account was giving money, so it remains the Source.
                    newSource = primarySide.copy(
                        categoryId = Category.SYSTEM_TRANSFER_OUTFLOW,
                        amount = if (primarySide.amount.isEmpty()) "" else primarySide.amountValue.abs().stripTrailingZeros().toPlainString()
                    )
                    newDestination = TransactionSide()
                }
            }
            else -> {
                // Moving to Expense or Income: Normalize back to a single side.
                newSource = primarySide.copy(
                    amount = if (primarySide.amount.isEmpty()) "" else primarySide.amountValue.abs().stripTrailingZeros().toPlainString(),
                    categoryId = if (Category.isTransfer(primarySide.categoryId)) Category.SYSTEM_UNKNOWN else primarySide.categoryId
                )
                newDestination = TransactionSide()
            }
        }

        return TypeTransitionResult(
            newType = newType,
            source = newSource,
            destination = newDestination,
            isSplitModeAllowed = newType != TransactionType.TRANSFER && isSplitMode
        )
    }
}
