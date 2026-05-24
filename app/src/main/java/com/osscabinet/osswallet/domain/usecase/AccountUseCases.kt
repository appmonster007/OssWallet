package com.osscabinet.ossWallet.domain.usecase

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
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
            if (amount != BigDecimal.ZERO) {
                transactionRepository.insertTransaction(
                    Transaction(
                        name = "Initial Balance",
                        accountId = accountId,
                        amount = amount,
                        currency = curr,
                        categoryId = 1004L,
                        note = "Initial Balance",
                        timestamp = System.currentTimeMillis(),
                        transactionType = if (amount >= BigDecimal.ZERO) TransactionType.INCOME else TransactionType.EXPENSE
                    ),
                    profileId
                )
            }
        }
    }
}
