package com.osscabinet.ossWallet.domain.usecase

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.ui.util.calculateTotalInCurrency
import java.math.BigDecimal

class GetTotalBalancesUseCase {
    operator fun invoke(
        accounts: List<Account>,
        allTransactions: List<Transaction>,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): List<Account> {
        val allTransactionsByAccount = allTransactions.groupBy { it.accountId }
        
        return accounts.map { account ->
            val accountTransactions = allTransactionsByAccount[account.id] ?: emptyList()
            val breakdown = mutableMapOf<String, BigDecimal>()
            breakdown[account.currencyCode] = account.initialBalance
            
            accountTransactions.forEach { tx ->
                val current = breakdown[tx.currency] ?: BigDecimal.ZERO
                breakdown[tx.currency] = if (tx.isIncome) current.add(tx.amount) else current.subtract(tx.amount)
            }
            
            val totalInAccountCurrency = calculateTotalInCurrency(
                currencyTotals = breakdown,
                targetCurrency = account.currencyCode,
                exchangeRates = exchangeRates,
                precision = precision
            )
            account.copy(balance = totalInAccountCurrency)
        }
    }
}
