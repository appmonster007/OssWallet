package com.osscabinet.ossWallet.domain.usecase

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.util.FilterUtils
import com.osscabinet.ossWallet.ui.util.calculateTotalInCurrency
import java.math.BigDecimal

data class AccountSummaryItem(
    val account: Account,
    val currencyBalances: Map<String, BigDecimal>,
    val balanceInBase: BigDecimal,
    val incomeInAccountCurrency: BigDecimal,
    val expenseInAccountCurrency: BigDecimal
)

class GetAccountSummariesUseCase {
    operator fun invoke(
        accounts: List<Account>,
        allTransactions: List<Transaction>,
        timelineRange: TimelineRange,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): Map<String, AccountSummaryItem> {
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

        return accounts.associate { account ->
            val accountTransactions = transactionsByAccount[account.id] ?: emptyList()
            val breakdown = mutableMapOf<String, BigDecimal>()
            
            if (includeInitial) {
                breakdown[account.currencyCode] = account.initialBalance
            }

            val incomeBreakdown = mutableMapOf<String, BigDecimal>()
            val expenseBreakdown = mutableMapOf<String, BigDecimal>()
            
            accountTransactions.forEach { tx ->
                val current = breakdown[tx.currency] ?: BigDecimal.ZERO
                breakdown[tx.currency] = if (tx.isIncome) current.add(tx.amount) else current.subtract(tx.amount)
                
                if (tx.isIncome) {
                    incomeBreakdown[tx.currency] = (incomeBreakdown[tx.currency] ?: BigDecimal.ZERO).add(tx.amount)
                } else {
                    expenseBreakdown[tx.currency] = (expenseBreakdown[tx.currency] ?: BigDecimal.ZERO).add(tx.amount)
                }
            }
            
            val incomeInAccountCurrency = calculateTotalInCurrency(incomeBreakdown, account.currencyCode, exchangeRates, precision)
            val expenseInAccountCurrency = calculateTotalInCurrency(expenseBreakdown, account.currencyCode, exchangeRates, precision)
            val balanceInBase = calculateTotalInCurrency(breakdown, baseCurrency, exchangeRates, precision)
            
            account.id to AccountSummaryItem(
                account = account,
                currencyBalances = breakdown.toMap(),
                balanceInBase = balanceInBase,
                incomeInAccountCurrency = incomeInAccountCurrency,
                expenseInAccountCurrency = expenseInAccountCurrency
            )
        }
    }
}
