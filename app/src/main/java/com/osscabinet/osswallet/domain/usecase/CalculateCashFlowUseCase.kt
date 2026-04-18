package com.osscabinet.ossWallet.domain.usecase

import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.util.FilterUtils
import com.osscabinet.ossWallet.ui.util.convertAmount
import java.math.BigDecimal
import java.math.RoundingMode

data class CashFlowBreakdownItem(
    val currency: String,
    val income: BigDecimal,
    val incomeInBase: BigDecimal,
    val expense: BigDecimal,
    val expenseInBase: BigDecimal
)

data class CashFlowResult(
    val totalIncomeInBase: BigDecimal,
    val totalExpensesInBase: BigDecimal,
    val netInBase: BigDecimal,
    val breakdown: List<CashFlowBreakdownItem>,
    val filteredTransactions: List<Transaction>
)

class CalculateCashFlowUseCase {
    operator fun invoke(
        allTransactions: List<Transaction>,
        timelineRange: TimelineRange,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): CashFlowResult {
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

        val currencies = filteredTransactions.map { it.currency }.distinct().sorted()
        
        val breakdown = currencies.mapNotNull { curr ->
            val txsForCurr = filteredTransactions.filter { it.currency == curr }
            val income = txsForCurr.filter { it.isIncome }
                .fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount) }
            val expense = txsForCurr.filter { !it.isIncome }
                .fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount) }

            if (income.signum() == 0 && expense.signum() == 0) null
            else {
                val incomeInBase = convertAmount(income, curr, baseCurrency, exchangeRates, precision + 4)
                val expenseInBase = convertAmount(expense, curr, baseCurrency, exchangeRates, precision + 4)
                CashFlowBreakdownItem(
                    currency = curr,
                    income = income,
                    incomeInBase = incomeInBase,
                    expense = expense,
                    expenseInBase = expenseInBase
                )
            }
        }

        val totalIncomeInBase = breakdown.fold(BigDecimal.ZERO) { acc, item -> 
            acc.add(item.incomeInBase) 
        }.setScale(precision, RoundingMode.HALF_UP)
        
        val totalExpensesInBase = breakdown.fold(BigDecimal.ZERO) { acc, item -> 
            acc.add(item.expenseInBase) 
        }.setScale(precision, RoundingMode.HALF_UP)

        return CashFlowResult(
            totalIncomeInBase = totalIncomeInBase,
            totalExpensesInBase = totalExpensesInBase,
            netInBase = totalIncomeInBase.subtract(totalExpensesInBase),
            breakdown = breakdown,
            filteredTransactions = filteredTransactions
        )
    }
}
