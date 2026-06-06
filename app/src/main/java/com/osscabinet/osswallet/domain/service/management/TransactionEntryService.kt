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
     * - Idempotency: If newType == oldType, return current state.
     * - Expense -> Transfer: Original account stays as SOURCE (Giver).
     * - Income -> Transfer: Original account moves to DESTINATION (Receiver).
     * - Transfer -> Normal: Revert to single side based on which side holds a managed account.
     * - Income <-> Expense: Reset category to SYSTEM_UNKNOWN.
     */
    fun transitionType(
        oldType: TransactionType,
        newType: TransactionType,
        currentSource: TransactionSide,
        currentDestination: TransactionSide,
        isSplitMode: Boolean
    ): TypeTransitionResult {
        // 1. Idempotency: Prevent state reset when clicking the already-active type
        if (oldType == newType) {
            return TypeTransitionResult(newType, currentSource, currentDestination, isSplitMode)
        }

        val isTransferToNormal = oldType == TransactionType.TRANSFER && newType != TransactionType.TRANSFER

        // Determine the "Primary" account from the user's perspective.
        val primarySide = if (isTransferToNormal) {
            when (newType) {
                TransactionType.INCOME -> {
                    // INCOME is positive impact. In a transfer, the Target side (destination) was receiving.
                    if (!currentDestination.accountId.isNullOrBlank()) currentDestination else currentSource
                }
                else -> {
                    // EXPENSE is negative impact. In a transfer, the Source side was giving.
                    if (!currentSource.accountId.isNullOrBlank()) currentSource else currentDestination
                }
            }
        } else {
            currentSource
        }

        val newSource: TransactionSide
        val newDestination: TransactionSide

        when (newType) {
            TransactionType.TRANSFER -> {
                if (oldType == TransactionType.INCOME) {
                    // Income account was receiving money → moves to Target (Receiver).
                    // Source defaults to Outside Wallet if a real income account was selected.
                    newSource = TransactionSide(
                        accountId = if (!primarySide.accountId.isNullOrBlank()) "" else null,
                        categoryId = Category.SYSTEM_TRANSFER_OUTFLOW,
                        amount = primarySide.amount,
                        currency = primarySide.currency
                    )
                    newDestination = primarySide.copy(
                        categoryId = Category.SYSTEM_TRANSFER_INFLOW
                    )
                } else {
                    // Expense account was giving money → remains the Source.
                    // Target defaults to Outside Wallet if a real expense account was selected.
                    newSource = primarySide.copy(
                        categoryId = Category.SYSTEM_TRANSFER_OUTFLOW,
                        amount = if (primarySide.amount.isEmpty()) "" else primarySide.amountValue.abs().stripTrailingZeros().toPlainString()
                    )
                    newDestination = TransactionSide(
                        accountId = if (!primarySide.accountId.isNullOrBlank()) "" else null,
                        categoryId = Category.SYSTEM_TRANSFER_INFLOW,
                        currency = primarySide.currency
                    )
                }
            }
            else -> {
                // Moving between Income/Expense or from Transfer to Normal.
                // Reset category if switching natures or leaving Transfer.
                val needsCategoryReset = isTransferToNormal || oldType != newType
                
                newSource = primarySide.copy(
                    amount = if (primarySide.amount.isEmpty()) "" else primarySide.amountValue.abs().stripTrailingZeros().toPlainString(),
                    categoryId = if (needsCategoryReset) Category.SYSTEM_UNKNOWN else primarySide.categoryId
                )
                newDestination = TransactionSide()
            }
        }

        // 2. Identity Exclusion: Ensure Source != Target for transfers
        val finalDestination = if (newSource.accountId != null && newSource.accountId == newDestination.accountId) {
            newDestination.copy(accountId = null, currency = "")
        } else newDestination

        return TypeTransitionResult(
            newType = newType,
            source = newSource,
            destination = finalDestination,
            isSplitModeAllowed = newType != TransactionType.TRANSFER && isSplitMode
        )
    }

    /**
     * Logic for selecting a new account on either side of a transfer.
     * Rule 1: Source != Target.
     * Rule 2: At least one side must be a managed account (No Outside -> Outside).
     */
    fun validateAccountSelection(
        isSourceSide: Boolean,
        newAccountId: String?,
        otherSideAccountId: String?
    ): Boolean {
        // Identity Exclusion
        if (newAccountId != null && newAccountId == otherSideAccountId) return false
        
        // Wallet Minimum Rule (No Outside-to-Outside)
        if (newAccountId == null && otherSideAccountId == null) return false
        
        return true
    }
}
