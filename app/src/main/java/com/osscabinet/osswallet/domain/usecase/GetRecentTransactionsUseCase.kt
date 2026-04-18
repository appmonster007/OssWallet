package com.osscabinet.ossWallet.domain.usecase

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.ui.util.convertAmount
import java.math.BigDecimal
import java.math.RoundingMode

class GetRecentTransactionsUseCase {
    operator fun invoke(
        allTransactions: List<Transaction>,
        accounts: List<Account>,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int,
        limit: Int = 20
    ): List<TransactionWithBalance> {
        val accountMap = accounts.associateBy { it.id }
        val nonArchivedAccountIds = accounts.filter { !it.isArchived }.map { it.id }.toSet()

        return allTransactions.sortedBy { it.timestamp }
            .groupBy { it.accountId }
            .flatMap { (accId, accountTransactions) ->
                val account = accountMap[accId]
                val accountCurrency = account?.currencyCode ?: "USD"

                var runningBalance = account?.initialBalance ?: BigDecimal.ZERO
                accountTransactions.map { transaction ->
                    val txAmountInAccountCurrency = convertAmount(
                        amount = transaction.amount,
                        fromCurrency = transaction.currency,
                        toCurrency = accountCurrency,
                        exchangeRates = exchangeRates,
                        precision = precision + 4
                    )
                    
                    val balanceBefore = runningBalance
                    runningBalance = if (transaction.isIncome) {
                        runningBalance.add(txAmountInAccountCurrency)
                    } else {
                        runningBalance.subtract(txAmountInAccountCurrency)
                    }
                    TransactionWithBalance(
                        transaction = transaction,
                        balanceBefore = balanceBefore.setScale(precision, RoundingMode.HALF_UP),
                        balanceAfter = runningBalance.setScale(precision, RoundingMode.HALF_UP)
                    )
                }
            }
            .filter { it.transaction.accountId in nonArchivedAccountIds }
            .sortedByDescending { it.transaction.timestamp }
            .take(limit)
    }
}
