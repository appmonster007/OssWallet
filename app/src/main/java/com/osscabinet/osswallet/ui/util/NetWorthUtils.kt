package com.osscabinet.ossWallet.ui.util

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.service.CurrencyService
import java.math.BigDecimal
import java.math.RoundingMode

fun calculateNetWorthFromAccounts(
    accounts: List<Account>,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    precision: Int
): BigDecimal {
    val globalCurrencyTotals = mutableMapOf<String, BigDecimal>()
    
    accounts.forEach { account ->
        globalCurrencyTotals[account.currencyCode] = (globalCurrencyTotals[account.currencyCode] ?: BigDecimal.ZERO).add(account.balance)
    }

    return CurrencyService.calculateTotalInCurrency(globalCurrencyTotals, baseCurrency, exchangeRates, precision)
}

fun calculateNetWorth(
    accounts: List<Account>,
    allTransactions: List<Transaction>,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    precision: Int,
    includeInitialBalance: Boolean = true
): BigDecimal {
    val transactionsByAccount = allTransactions.groupBy { it.accountId }
    
    val accountStats = accounts.associate { account ->
        val accountTransactions = transactionsByAccount[account.id] ?: emptyList()
        val breakdown = mutableMapOf<String, BigDecimal>()
        
        if (includeInitialBalance) {
            breakdown[account.currencyCode] = account.initialBalance
        }

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

    return CurrencyService.calculateTotalInCurrency(globalCurrencyTotals, baseCurrency, exchangeRates, precision)
}
