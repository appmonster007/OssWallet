package com.osscabinet.ossWallet.domain.usecase

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.util.FilterUtils
import com.osscabinet.ossWallet.ui.util.calculateTotalInCurrency
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class CalculateNetWorthUseCase {
    operator fun invoke(
        accounts: List<Account>,
        allTransactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int,
        timelineRange: TimelineRange
    ): BigDecimal {
        val filteredTransactions = when (timelineRange) {
            is TimelineRange.AllTime -> allTransactions
            is TimelineRange.ThisPeriod -> {
                val (start, end) = FilterUtils.getRangeForThis(timelineRange.period)
                allTransactions.filter { it.timestamp in start..end }
            }
            is TimelineRange.LastPeriod -> {
                val (start, end) = FilterUtils.getRangeForLast(timelineRange.period)
                allTransactions.filter { it.timestamp in start..end }
            }
            is TimelineRange.CustomRange -> {
                allTransactions.filter { it.timestamp in timelineRange.start..timelineRange.end }
            }
        }

        val transactionsByAccount = filteredTransactions.groupBy { it.accountId }
        val includeInitialBalance = timelineRange is TimelineRange.AllTime
        
        val globalCurrencyTotals = mutableMapOf<String, BigDecimal>()
        
        accounts.forEach { account ->
            val accountTransactions = transactionsByAccount[account.id] ?: emptyList()
            val breakdown = mutableMapOf<String, BigDecimal>()
            
            if (includeInitialBalance) {
                breakdown[account.currencyCode] = account.initialBalance
            }

            accountTransactions.forEach { tx ->
                val current = breakdown[tx.currency] ?: BigDecimal.ZERO
                breakdown[tx.currency] = if (tx.isIncome) current.add(tx.amount) else current.subtract(tx.amount)
            }
            
            breakdown.forEach { (curr, amount) ->
                globalCurrencyTotals[curr] = (globalCurrencyTotals[curr] ?: BigDecimal.ZERO).add(amount)
            }
        }

        return calculateTotalInCurrency(globalCurrencyTotals, baseCurrency, exchangeRates, precision)
    }
}
