package com.osscabinet.ossWallet.domain.usecase

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.EntryMode
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
        profileId: Int
    ): String {
        val accountId = UUID.randomUUID().toString()
        accountRepository.insertAccount(
            Account(
                id = accountId,
                name = name,
                currencyCode = currency,
                structureType = structureType,
                colorHex = colorHex,
                icon = icon,
                allowedCurrencies = allowedCurrencies
            ),
            profileId
        )

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
                    timestamp = System.currentTimeMillis()
                )
                transactionManager.handleSaveTransaction(null, request, profileId)
            }
        }
        return accountId
    }

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
            categoryId = Category.SYSTEM_TRANSFER,
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
                val entryMode = if (amount.signum() >= 0) EntryMode.INCOME else EntryMode.EXPENSE
                val request = TransactionRequest(
                    name = "Initial Balance",
                    amount = amount.abs(),
                    entryMode = entryMode,
                    accountId = accountId,
                    currency = currency,
                    categoryId = Category.SYSTEM_INITIAL_BALANCE,
                    note = "Initial Balance",
                    timestamp = System.currentTimeMillis()
                )
                transactionManager.handleSaveTransaction(existing, request, profileId)
            }
        }
    }
}
