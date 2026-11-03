package com.osscabinet.ossWallet.domain.usecase

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.CreditCardDetails
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import java.math.BigDecimal
import java.util.UUID

class AccountUseCases(
    private val accountRepository: AccountRepository,
    private val transactionManager: TransactionManager
) {
    suspend fun deleteAccount(accountId: String, profileId: Int) {
        accountRepository.deleteAccount(accountId, profileId)
    }

    suspend fun createAccountWithInitialBalances(
        name: String,
        initialBalances: Map<String, String>,
        currency: String,
        structureType: AccountStructureType,
        colorHex: String,
        icon: String,
        allowedCurrencies: List<String>,
        profileId: Int,
        accountId: String = UUID.randomUUID().toString(),
        creditDetails: CreditCardDetails? = null
    ): String = transactionManager.transactionUseCases.runAtomically {
        accountRepository.insertAccount(
            Account(
                id = accountId,
                name = name,
                currencyCode = currency,
                structureType = structureType,
                colorHex = colorHex,
                icon = icon,
                allowedCurrencies = withBalanceCurrencies(allowedCurrencies + currency, initialBalances)
            ),
            profileId
        )
        creditDetails?.let { accountRepository.saveCreditCardDetails(it) }

        initialBalances.forEach { (curr, balance) ->
            val amount = balance.toBigDecimalOrNull() ?: BigDecimal.ZERO
            if (amount.signum() != 0) {
                val request = TransactionRequest(
                    name = "Initial Balance",
                    amount = amount.abs(),
                    entryMode = EntryMode.TRANSFER,
                    accountId = outsideWalletAccountId(amount, accountId),
                    targetAccountId = outsideWalletTargetId(amount, accountId),
                    currency = curr,
                    categoryId = Category.SYSTEM_INITIAL_BALANCE,
                    note = "Initial Balance",
                    timestamp = openingBalanceTimestamp(accountId, null, profileId)
                )
                transactionManager.handleSaveTransaction(null, request, profileId)
            }
        }
        accountId
    }

    /**
     * Writes all of an account edit as one unit, so a failure part-way leaves nothing behind.
     */
    suspend fun saveAccountEdits(
        account: Account,
        creditDetails: CreditCardDetails?,
        openingBalances: Map<String, BigDecimal>,
        pendingAdjustments: Map<String, BigDecimal>,
        profileId: Int
    ) {
        transactionManager.transactionUseCases.runAtomically {
            accountRepository.updateAccount(account, profileId)
            creditDetails?.let { accountRepository.saveCreditCardDetails(it) }
            pendingAdjustments.forEach { (currency, delta) ->
                createBalanceAdjustment(account.id, currency, delta, profileId)
            }
            openingBalances.forEach { (currency, amount) ->
                upsertInitialBalance(account.id, currency, amount, profileId)
            }
        }
    }

    /**
     * A recorded change in balance. Stored as a transfer leg against Outside Wallet, like an
     * opening balance - it moves the account's balance without counting as real income/expense.
     */
    suspend fun createBalanceAdjustment(
        accountId: String,
        currency: String,
        delta: BigDecimal,
        profileId: Int
    ) {
        if (delta.signum() == 0) return
        val request = TransactionRequest(
            name = "Balance Adjustment",
            amount = delta.abs(),
            entryMode = EntryMode.TRANSFER,
            accountId = outsideWalletAccountId(delta, accountId),
            targetAccountId = outsideWalletTargetId(delta, accountId),
            currency = currency,
            categoryId = Category.SYSTEM_RECONCILIATION_ADJUSTMENT,
            note = "Balance adjustment",
            timestamp = System.currentTimeMillis()
        )
        transactionManager.handleSaveTransaction(null, request, profileId)
    }

    suspend fun upsertInitialBalance(
        accountId: String,
        currency: String,
        amount: BigDecimal,
        profileId: Int
    ) {
        val existing = transactionManager.transactionUseCases.getInitialBalanceTransaction(
            accountId,
            currency,
            profileId
        )
        when {
            amount.signum() == 0 && existing != null ->
                transactionManager.deleteTransaction(existing.id, profileId)

            amount.signum() != 0 -> {
                val timestamp = openingBalanceTimestamp(accountId, existing, profileId)
                // Skip the write when nothing changed, so unrelated account edits leave the row alone
                if (existing != null &&
                    existing.amount.compareTo(amount) == 0 &&
                    existing.timestamp == timestamp
                ) return

                val request = TransactionRequest(
                    name = "Initial Balance",
                    amount = amount.abs(),
                    entryMode = EntryMode.TRANSFER,
                    accountId = outsideWalletAccountId(amount, accountId),
                    targetAccountId = outsideWalletTargetId(amount, accountId),
                    currency = currency,
                    categoryId = Category.SYSTEM_INITIAL_BALANCE,
                    note = "Initial Balance",
                    timestamp = timestamp
                )
                transactionManager.handleSaveTransaction(existing, request, profileId)
            }
        }
    }

    // A gain is Outside Wallet -> account (source blank, so TransactionManager treats it as the
    // inflow leg); a loss is account -> Outside Wallet (target blank, the outflow leg).
    private fun outsideWalletAccountId(signedAmount: BigDecimal, accountId: String): String? =
        if (signedAmount.signum() >= 0) null else accountId

    private fun outsideWalletTargetId(signedAmount: BigDecimal, accountId: String): String? =
        if (signedAmount.signum() >= 0) accountId else null

    // Every currency that holds an opening balance must be allowed, or the edit screen cannot show or remove it.
    private fun withBalanceCurrencies(allowed: List<String>, initialBalances: Map<String, String>): List<String> {
        val withBalance = initialBalances
            .filterValues { (it.toBigDecimalOrNull() ?: BigDecimal.ZERO).signum() != 0 }
            .keys
        return (allowed + withBalance).distinct()
    }

    /**
     * Opening balances sit one day before the account's earliest non-initial-balance row.
     * With no other rows, keep the existing date; for a brand-new account, use now minus a day.
     */
    private suspend fun openingBalanceTimestamp(
        accountId: String,
        existing: Transaction?,
        profileId: Int
    ): Long {
        val earliest = transactionManager.transactionUseCases
            .getEarliestNonInitialBalanceTimestamp(accountId, profileId)
        val base = earliest
            ?: existing?.let { it.timestamp + ONE_DAY_MILLIS }
            ?: System.currentTimeMillis()
        return base - ONE_DAY_MILLIS
    }

    private companion object {
        const val ONE_DAY_MILLIS = 24L * 60 * 60 * 1000
    }
}
