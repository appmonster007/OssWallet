package com.osscabinet.ossWallet.domain.usecase

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.util.FilterUtils
import com.osscabinet.ossWallet.ui.util.convertAmount
import java.math.BigDecimal

data class CurrencyAggregationItem(
    val currency: String,
    val amount: BigDecimal,
    val amountInBase: BigDecimal
)

class GetCurrencyAggregationUseCase {
    operator fun invoke(
        accounts: List<Account>,
        allTransactions: List<Transaction>,
        timelineRange: TimelineRange,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): List<CurrencyAggregationItem> {
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
        val includeInitial = timelineRange is TimelineRange.AllTime
        
        val totalsMap = mutableMapOf<String, BigDecimal>()
        
        accounts.forEach { account ->
            val accountTransactions = transactionsByAccount[account.id] ?: emptyList()
            val accountBreakdown = mutableMapOf<String, BigDecimal>()
            
            if (includeInitial) {
                accountBreakdown[account.currencyCode] = account.initialBalance
            }
            
            accountTransactions.forEach { tx ->
                val current = accountBreakdown[tx.currency] ?: BigDecimal.ZERO
                accountBreakdown[tx.currency] = if (tx.isIncome) current.add(tx.amount) else current.subtract(tx.amount)
            }
            accountBreakdown.forEach { (curr, amount) ->
                totalsMap[curr] = (totalsMap[curr] ?: BigDecimal.ZERO).add(amount)
            }
        }
        
        return totalsMap.map { (currency, amount) ->
            CurrencyAggregationItem(
                currency = currency,
                amount = amount,
                amountInBase = convertAmount(amount, currency, baseCurrency, exchangeRates, precision)
            )
        }.sortedBy { it.currency }
    }
}
