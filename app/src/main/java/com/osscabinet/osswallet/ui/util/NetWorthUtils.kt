package com.osscabinet.ossWallet.ui.util

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.ui.viewmodel.AccountStats
import java.math.BigDecimal
import java.math.RoundingMode

fun calculateNetWorth(
    accounts: List<Account>,
    allTransactions: List<Transaction>,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    precision: Int
): BigDecimal {
    val transactionsByAccount = allTransactions.groupBy { it.accountId }
    
    val accountStats = accounts.associate { account ->
        val accountTransactions = transactionsByAccount[account.id] ?: emptyList()
        val breakdown = mutableMapOf<String, BigDecimal>()
        
        breakdown[account.currencyCode] = account.initialBalance
        account.allowedCurrencies.forEach { curr ->
            breakdown.getOrPut(curr) { BigDecimal.ZERO }
        }
        
        accountTransactions.forEach { tx ->
            val current = breakdown[tx.currency] ?: BigDecimal.ZERO
            breakdown[tx.currency] = if (tx.isIncome) current.add(tx.amount) else current.subtract(tx.amount)
        }
        
        account.id to breakdown.toMap()
    }

    val globalCurrencyTotals = mutableMapOf<String, BigDecimal>()
    accountStats.values.forEach { balances ->
        balances.forEach { (curr, amount) ->
            globalCurrencyTotals[curr] = (globalCurrencyTotals[curr] ?: BigDecimal.ZERO).add(amount)
        }
    }

    return calculateTotalInCurrency(globalCurrencyTotals, baseCurrency, exchangeRates, precision)
}
