package com.osscabinet.ossWallet.domain.service.management

import com.osscabinet.ossWallet.domain.model.AccountRef
import com.osscabinet.ossWallet.domain.model.AmountEditingSide
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.TransactionSide
import com.osscabinet.ossWallet.domain.model.isOutside
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
import com.osscabinet.ossWallet.domain.model.toDbTransferSide
import com.osscabinet.ossWallet.domain.model.resolve
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Domain service to handle complex state transitions and calculations for transaction entry.
 * Centralizes the business logic used by both full Entry screens and Quick Add sheets.
 */
object TransactionEntryService {

    data class TypeTransitionResult(
        val newMode: EntryMode,
        val source: TransactionSide,
        val destination: TransactionSide,
        val isSplitModeAllowed: Boolean
    )

    /**
     * Rules for switching modes:
     * - Idempotency: If newMode == oldMode, return current state.
     * - Expense -> Transfer: Original account stays as SOURCE (Giver).
     * - Income -> Transfer: Original account moves to DESTINATION (Receiver).
     * - Transfer -> Normal: Revert to single side based on which side holds a managed account.
     * - Income <-> Expense: Reset category to SYSTEM_UNKNOWN.
     */
    fun transitionType(
        oldMode: EntryMode,
        newMode: EntryMode,
        currentSource: TransactionSide,
        currentDestination: TransactionSide,
        isSplitMode: Boolean
    ): TypeTransitionResult {
        // 1. Idempotency: Prevent state reset when clicking the already-active type
        if (oldMode == newMode) {
            return TypeTransitionResult(newMode, currentSource, currentDestination, isSplitMode)
        }

        val isTransferToNormal = oldMode == EntryMode.TRANSFER && newMode != EntryMode.TRANSFER

        // Determine the "Primary" account from the user's perspective.
        val primarySide = if (isTransferToNormal) {
            when (newMode) {
                EntryMode.INCOME -> {
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

        when (newMode) {
            EntryMode.TRANSFER -> {
                if (oldMode == EntryMode.INCOME) {
                    // Income account was receiving money → moves to Target (Receiver).
                    // We keep the amount on the new Source side for consistency in editing.
                    newSource = TransactionSide(
                        account = otherSideRef,
                        categoryId = Category.SYSTEM_TRANSFER,
                        amount = primarySide.amount,
                        currency = primarySide.currency
                    )
                    newDestination = primarySide.copy(
                        categoryId = Category.SYSTEM_TRANSFER,
                        amount = ""
                    )
                } else {
                    // Expense account was giving money → remains the Source.
                    newSource = primarySide.copy(
                        categoryId = Category.SYSTEM_TRANSFER,
                        amount = if (primarySide.amount.isEmpty()) "" else primarySide.amountValue.abs().stripTrailingZeros().toPlainString()
                    )
                    newDestination = TransactionSide(
                        account = otherSideRef,
                        categoryId = Category.SYSTEM_TRANSFER,
                        amount = "",
                        currency = primarySide.currency
                    )
                }
            }
            else -> {
                // Moving between Income/Expense or from Transfer to Normal.
                // Reset category if switching natures or leaving Transfer.
                val needsCategoryReset = isTransferToNormal || oldMode != newMode

                newSource = primarySide.copy(
                    amount = if (primarySide.amount.isEmpty()) "" else primarySide.amountValue.abs().stripTrailingZeros().toPlainString(),
                    categoryId = if (needsCategoryReset) Category.SYSTEM_UNKNOWN else primarySide.categoryId
                )
                newDestination = TransactionSide()
            }
        }

        return TypeTransitionResult(
            newMode = newMode,
            source = newSource,
            destination = newDestination,
            isSplitModeAllowed = newMode != EntryMode.TRANSFER && isSplitMode
        )
    }

    /**
     * Resolves the correct currency for a transaction side after an account change.
     * Implementation of the smart fallback chain:
     * 1. Account's native currency (if managed wallet).
     * 2. Current inherited currency (if external).
     * 3. System base currency.
     */
    fun resolveSideCurrency(
        account: AccountRef,
        accountCurrency: String?,
        currentCurrency: String,
        baseCurrency: String
    ): String {
        return if (account.isOutside) {
            currentCurrency.ifBlank { baseCurrency }
        } else {
            accountCurrency?.ifBlank { null } ?: currentCurrency.ifBlank { baseCurrency }
        }
    }

    /**
     * Recalculates the destination amount based on the source amount and exchange rate.
     */
    fun calculateTargetAmount(
        sourceAmount: BigDecimal,
        rate: BigDecimal
    ): String {
        return calculateTargetAmountValue(sourceAmount, rate)
            .stripTrailingZeros()
            .toPlainString()
    }

    fun calculateTargetAmountValue(
        sourceAmount: BigDecimal,
        rate: BigDecimal
    ): BigDecimal {
        return sourceAmount.multiply(rate)
            .setScale(CurrencyService.precision, RoundingMode.HALF_UP)
    }

    /**
     * Recalculates the source amount based on the destination amount and exchange rate.
     */
    fun calculateSourceAmount(
        targetAmount: BigDecimal,
        rate: BigDecimal
    ): String {
        return calculateSourceAmountValue(targetAmount, rate)
            .stripTrailingZeros()
            .toPlainString()
    }

    fun calculateSourceAmountValue(
        targetAmount: BigDecimal,
        rate: BigDecimal
    ): BigDecimal {
        return if (rate.signum() != 0) {
            targetAmount.divide(rate, CurrencyService.precision, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
    }

    /**
     * Fetches the market exchange rate for a currency pair.
     */
    fun getMarketRate(
        fromCurrency: String,
        toCurrency: String,
        exchangeRates: ExchangeRateSnapshot
    ): BigDecimal {
        if (fromCurrency.isBlank() || toCurrency.isBlank()) return BigDecimal.ONE
        return CurrencyService.convertAmount(
            amount = BigDecimal.ONE,
            fromCurrency = fromCurrency,
            toCurrency = toCurrency,
            exchangeRates = exchangeRates,
            precision = 8
        )
    }

    /**
     * Calculates the signed amount based on entry mode.
     * Managed by TransactionManager for transfers, but needed for normal transactions.
     */
    fun getSignedAmount(amount: BigDecimal, entryMode: EntryMode): BigDecimal {
        return if (entryMode == EntryMode.INCOME) amount.abs() else amount.abs().negate()
    }

    /**
     * Unified transfer rate calculation.
     */
    fun calculateTransferRate(
        sourceAmount: BigDecimal,
        targetAmount: BigDecimal
    ): BigDecimal {
        if (sourceAmount.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ONE
        return targetAmount.divide(sourceAmount, 8, RoundingMode.HALF_UP).stripTrailingZeros()
    }

    /**
     * Unified builder for TransactionRequest.
     * Centralizes currency fallback, signed amounts, and transfer rate calculation.
     */
    fun createRequest(
        name: String,
        amount: BigDecimal,
        entryMode: EntryMode,
        sourceAccount: AccountRef,
        sourceCurrency: String,
        targetAccount: AccountRef = AccountRef.Outside,
        targetCurrency: String = "",
        categoryId: Long,
        note: String = "",
        payee: String = "",
        tags: Set<String> = emptySet(),
        status: TransactionStatus = TransactionStatus.CLEARED,
        amountOverride: BigDecimal? = null,
        exchangeRates: ExchangeRateSnapshot,
        accounts: List<com.osscabinet.ossWallet.domain.model.Account>,
        baseCurrency: String,
        timestamp: Long = System.currentTimeMillis()
    ): TransactionRequest {
        val account = sourceAccount.resolve(accounts)
        val toAccount = targetAccount.resolve(accounts)

        val finalSourceCurrency = sourceCurrency.ifBlank { account?.currencyCode ?: baseCurrency }
        val finalTargetCurrency = targetCurrency.ifBlank { toAccount?.currencyCode ?: "" }

        val transferRate = if (entryMode == EntryMode.TRANSFER) {
            val destAmount = amountOverride ?: getMarketRate(
                fromCurrency = finalSourceCurrency,
                toCurrency = finalTargetCurrency,
                exchangeRates = exchangeRates
            ).multiply(amount)

            calculateTransferRate(amount, destAmount)
        } else {
            BigDecimal.ONE
        }

        return TransactionRequest(
            name = name,
            amount = getSignedAmount(amount, entryMode),
            entryMode = entryMode,
            accountId = sourceAccount.toDbTransferSide(),
            currency = finalSourceCurrency,
            targetAccountId = targetAccount.toDbTransferSide(),
            targetCurrency = finalTargetCurrency.ifBlank { null },
            categoryId = categoryId,
            note = note,
            payee = payee,
            status = status,
            tags = tags,
            transferRate = transferRate,
            timestamp = timestamp
        )
    }

    /**
     * Unified logic for mapping a prefill (Template/Transaction) to the entry state.
     */
    data class PrefillResult(
        val account: AccountRef,
        val targetAccount: AccountRef,
        val currency: String,
        val targetCurrency: String,
        val amount: BigDecimal,
        val categoryId: Long,
        val entryMode: EntryMode,
        val note: String,
        val payee: String,
        val tags: Set<String>
    )

    fun resolvePrefill(
        entryMode: EntryMode,
        accountId: String?,
        targetAccountId: String?,
        currency: String,
        targetCurrency: String?,
        amount: BigDecimal,
        categoryId: Long,
        note: String?,
        payee: String?,
        tags: Set<String>?,
        accounts: List<com.osscabinet.ossWallet.domain.model.Account>
    ): PrefillResult {
        val sourceRef = accountId?.let { AccountRef.fromStored(it) } ?: if (entryMode == EntryMode.TRANSFER) AccountRef.Outside else AccountRef.Unselected
        val targetRef = targetAccountId?.let { AccountRef.fromStored(it) } ?: if (entryMode == EntryMode.TRANSFER) AccountRef.Outside else AccountRef.Unselected

        val sourceAcc = sourceRef.resolve(accounts)
        val targetAcc = targetRef.resolve(accounts)

        return PrefillResult(
            account = sourceRef,
            targetAccount = targetRef,
            currency = sourceAcc?.currencyCode ?: currency,
            targetCurrency = targetAcc?.currencyCode ?: targetCurrency ?: "",
            amount = amount.abs(),
            categoryId = categoryId,
            entryMode = entryMode,
            note = note ?: "",
            payee = payee ?: "",
            tags = tags ?: emptySet()
        )
    }

    /**
     * Validates that at least one side of a transfer is a managed wallet,
     * and both sides are selected.
     */
    fun validateTransferAccounts(
        source: AccountRef,
        destination: AccountRef
    ): Boolean {
        if (source is AccountRef.Unselected || destination is AccountRef.Unselected) return false
        // Block Outside -> Outside
        if (source.isOutside && destination.isOutside) return false
        return true
    }

    /**
     * Validates that a managed account is selected for normal transactions.
     */
    fun validateNormalAccount(account: AccountRef): Boolean {
        return account is AccountRef.Wallet
    }

    data class AccountValidationResult(
        val isSourceValid: Boolean,
        val isTargetValid: Boolean
    ) {
        val isValid: Boolean get() = isSourceValid && isTargetValid
    }

    /**
     * Intelligent account/currency swap for transfers.
     */
    fun swapAccounts(
        source: TransactionSide,
        destination: TransactionSide,
        transferRate: BigDecimal
    ): Triple<TransactionSide, TransactionSide, BigDecimal> {
        val effectiveDstAmount = destination.amount.ifBlank {
            source.amountValue.multiply(transferRate)
                .setScale(8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
        }
        val newRate = if (transferRate.signum() != 0)
            BigDecimal.ONE.divide(transferRate, 8, RoundingMode.HALF_UP).stripTrailingZeros()
        else BigDecimal.ONE

        val newSource = destination.copy(
            amount = effectiveDstAmount,
            categoryId = source.categoryId // Category stays pinned to the conceptual source
        )
        val newDestination = source.copy(
            amount = source.amount,
            categoryId = destination.categoryId
        )

        return Triple(newSource, newDestination, newRate)
    }

    /**
     * Standardised result for dynamic transfer state updates.
     */
    data class TransferSyncResult(
        val sourceAmount: BigDecimal,
        val targetAmount: BigDecimal,
        val impliedRate: BigDecimal
    )

    /**
     * Centralized synchronization logic for transfer legs.
     * Resolves the linked values of a transfer when one component (amount, currency, mode) changes.
     */
    fun syncTransferState(
        sourceAmount: BigDecimal,
        targetAmount: BigDecimal,
        sourceCurrency: String,
        targetCurrency: String,
        isFixed: Boolean,
        activeSide: AmountEditingSide,
        exchangeRates: ExchangeRateSnapshot
    ): TransferSyncResult {
        val marketRate = getMarketRate(sourceCurrency, targetCurrency, exchangeRates)
        
        return if (isFixed) {
            // FIXED MODE: Amounts are decoupled. Implied rate is calculated from current amounts.
            val implied = if (sourceAmount.signum() != 0) {
                targetAmount.divide(sourceAmount, 8, RoundingMode.HALF_UP).stripTrailingZeros()
            } else BigDecimal.ONE
            
            TransferSyncResult(sourceAmount, targetAmount, implied)
        } else {
            // AUTO MODE: Target is projected from Source (or vice-versa) using market rate.
            if (activeSide == AmountEditingSide.TARGET) {
                TransferSyncResult(
                    sourceAmount = calculateSourceAmountValue(targetAmount, marketRate),
                    targetAmount = targetAmount,
                    impliedRate = marketRate
                )
            } else {
                TransferSyncResult(
                    sourceAmount = sourceAmount,
                    targetAmount = calculateTargetAmountValue(sourceAmount, marketRate),
                    impliedRate = marketRate
                )
            }
        }
    }

    /**
     * Unified validation for account selection across all entry forms.
     */
    fun validateAccountSelection(
        entryMode: EntryMode,
        source: AccountRef,
        target: AccountRef
    ): AccountValidationResult {
        return if (entryMode == EntryMode.TRANSFER) {
            val transferValid = validateTransferAccounts(source, target)
            if (!transferValid) {
                val bothOutside = source.isOutside && target.isOutside
                val sourceInvalid = source is AccountRef.Unselected || bothOutside
                val targetInvalid = target is AccountRef.Unselected || bothOutside
                AccountValidationResult(!sourceInvalid, !targetInvalid)
            } else {
                AccountValidationResult(true, true)
            }
        } else {
            AccountValidationResult(validateNormalAccount(source), true)
        }
    }

    /**
     * Centralized logic to update source/target amounts based on exchange rate.
     */
    fun updateAmounts(
        source: TransactionSide,
        destination: TransactionSide,
        rate: BigDecimal,
        updateSource: Boolean = false // If true, target is master; if false, source is master
    ): Pair<TransactionSide, TransactionSide> {
        return if (updateSource) {
            // Guard: only update the source side if the exchange rate is non-zero.
            // If the rate is zero (e.g., fallback on error), we avoid destructive recalculation.
            if (rate.signum() != 0) {
                val newSourceAmt = calculateSourceAmount(destination.amountValue, rate)
                source.copy(amount = newSourceAmt) to destination
            } else {
                source to destination
            }
        } else {
            val newTargetAmt = calculateTargetAmount(source.amountValue, rate)
            source to destination.copy(amount = newTargetAmt)
        }
    }
}
