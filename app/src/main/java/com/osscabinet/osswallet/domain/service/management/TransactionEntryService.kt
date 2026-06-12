package com.osscabinet.ossWallet.domain.service.management

import com.osscabinet.ossWallet.domain.model.AccountRef
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
                    if (currentDestination.account is AccountRef.Wallet) currentDestination else currentSource
                }
                else -> {
                    // EXPENSE is negative impact. In a transfer, the Source side was giving.
                    if (currentSource.account is AccountRef.Wallet) currentSource else currentDestination
                }
            }
        } else {
            currentSource
        }

        // When transitioning into a transfer, the "other" side defaults to Outside Wallet
        // iff the primary side already names a real wallet, otherwise it stays Unselected
        // so the user must pick something.
        val otherSideRef: AccountRef = if (primarySide.account is AccountRef.Wallet) {
            AccountRef.Outside
        } else {
            AccountRef.Unselected
        }

        val newSource: TransactionSide
        val newDestination: TransactionSide

        when (newType) {
            TransactionType.TRANSFER -> {
                if (oldType == TransactionType.INCOME) {
                    // Income account was receiving money → moves to Target (Receiver).
                    newSource = TransactionSide(
                        account = otherSideRef,
                        categoryId = Category.SYSTEM_TRANSFER_OUTFLOW,
                        amount = primarySide.amount,
                        currency = primarySide.currency
                    )
                    newDestination = primarySide.copy(
                        categoryId = Category.SYSTEM_TRANSFER_INFLOW
                    )
                } else {
                    // Expense account was giving money → remains the Source.
                    newSource = primarySide.copy(
                        categoryId = Category.SYSTEM_TRANSFER_OUTFLOW,
                        amount = if (primarySide.amount.isEmpty()) "" else primarySide.amountValue.abs().stripTrailingZeros().toPlainString()
                    )
                    newDestination = TransactionSide(
                        account = otherSideRef,
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

        // 2. Identity Exclusion: Source != Target. Only matters when both are concrete wallets.
        val srcAcc = newSource.account
        val dstAcc = newDestination.account
        val finalDestination = if (srcAcc is AccountRef.Wallet && dstAcc is AccountRef.Wallet && srcAcc.id == dstAcc.id) {
            newDestination.copy(account = AccountRef.Unselected, currency = "")
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
     * Rule 1: Source != Target (Identity Exclusion).
     * Rule 2: At least one side must be a managed wallet (No Outside-to-Outside).
     */
    fun validateAccountSelection(
        isSourceSide: Boolean,
        newAccount: AccountRef,
        otherSide: AccountRef
    ): Boolean {
        // Identity Exclusion
        if (newAccount is AccountRef.Wallet && otherSide is AccountRef.Wallet && newAccount.id == otherSide.id) {
            return false
        }
        // Wallet Minimum Rule (No Outside-to-Outside)
        if (newAccount is AccountRef.Outside && otherSide is AccountRef.Outside) return false
        return true
    }
}
