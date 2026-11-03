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
                val entryMode = if (amount.signum() >= 0) EntryMode.INCOME else EntryMode.EXPENSE
                val request = TransactionRequest(
                    name = "Initial Balance",
                    amount = amount.abs(),
                    entryMode = entryMode,
                    accountId = accountId,
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
     * A recorded change in balance. Stored like an opening balance: an income for a gain, an
     * expense for a loss, on the reconciliation category.
     */
    suspend fun createBalanceAdjustment(
        accountId: String,
        currency: String,
        delta: BigDecimal,
        profileId: Int
    ) {
        if (delta.signum() == 0) return
        val entryMode = if (delta.signum() > 0) EntryMode.INCOME else EntryMode.EXPENSE
        val request = TransactionRequest(
            name = "Balance Adjustment",
            amount = delta.abs(),
            entryMode = entryMode,
            accountId = accountId,
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

                val entryMode = if (amount.signum() >= 0) EntryMode.INCOME else EntryMode.EXPENSE
                val request = TransactionRequest(
                    name = "Initial Balance",
                    amount = amount.abs(),
                    entryMode = entryMode,
                    accountId = accountId,
                    currency = currency,
                    categoryId = Category.SYSTEM_INITIAL_BALANCE,
                    note = "Initial Balance",
                    timestamp = timestamp
                )
                transactionManager.handleSaveTransaction(existing, request, profileId)
            }
        }
    }

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
