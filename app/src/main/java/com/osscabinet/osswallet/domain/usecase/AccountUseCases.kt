package com.osscabinet.ossWallet.domain.usecase

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import java.math.BigDecimal
import java.util.UUID

class AccountUseCases(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend fun deleteAccount(accountId: String) {
        accountRepository.deleteAccount(accountId)
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
    ) {
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
            if (amount.compareTo(BigDecimal.ZERO) != 0) {
                val isInflow = amount.signum() >= 0
                transactionRepository.insertTransaction(
                    Transaction(
                        name = "Initial Balance",
                        accountId = accountId,
                        amount = amount,
                        currency = curr,
                        categoryId = Category.SYSTEM_INITIAL_BALANCE,
                        note = "Initial Balance",
                        timestamp = System.currentTimeMillis(),
                        transactionType = if (isInflow) TransactionType.TRANSFER_INFLOW else TransactionType.TRANSFER_OUTFLOW
                    ),
                    profileId
                )
            }
        }
    }

    suspend fun upsertInitialBalance(
        accountId: String,
        currency: String,
        amount: BigDecimal,
        profileId: Int
    ) {
        val existing = transactionRepository.getInitialBalanceTransaction(accountId, currency, profileId)
        when {
            amount.compareTo(BigDecimal.ZERO) == 0 && existing != null ->
                transactionRepository.deleteTransaction(existing.id)
            amount.compareTo(BigDecimal.ZERO) != 0 && existing != null -> {
                val isInflow = amount.signum() >= 0
                transactionRepository.updateTransaction(
                    existing,
                    existing.copy(
                        amount = amount,
                        categoryId = Category.SYSTEM_INITIAL_BALANCE,
                        transactionType = if (isInflow) TransactionType.TRANSFER_INFLOW else TransactionType.TRANSFER_OUTFLOW
                    ),
                    profileId
                )
            }
            amount.compareTo(BigDecimal.ZERO) != 0 -> {
                val isInflow = amount.signum() >= 0
                transactionRepository.insertTransaction(
                    Transaction(
                        name = "Initial Balance",
                        accountId = accountId,
                        amount = amount,
                        currency = currency,
                        categoryId = Category.SYSTEM_INITIAL_BALANCE,
                        note = "Initial Balance",
                        timestamp = System.currentTimeMillis(),
                        transactionType = if (isInflow) TransactionType.TRANSFER_INFLOW else TransactionType.TRANSFER_OUTFLOW
                    ),
                    profileId
                )
            }
        }
    }
}
