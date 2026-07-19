package com.osscabinet.ossWallet.domain.service.financial

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Service for core financial calculations like savings rate, burn rate, and volatility.
 */
object FinancialCalculations {

    /**
     * Calculates the delta (absolute and percentage change) between two financial summaries.
     *
     * @param current Current period summary
     * @param previous Previous period summary
     * @return FinancialDelta containing changes
     */
    fun calculateSummaryDelta(
        current: FinancialSummary,
        previous: FinancialSummary
    ): FinancialDelta {
        val amountDelta = current.totalAmount - previous.totalAmount
        val percentageDelta = if (previous.totalAmount.signum() != 0) {
            amountDelta.divide(previous.totalAmount.abs(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal("100"))
                .toDouble()
        } else if (current.totalAmount.signum() != 0) {
            100.0
        } else 0.0

        return FinancialDelta(
            absoluteChange = amountDelta,
            percentageChange = percentageDelta,
            incomeChange = current.income - previous.income,
            expenseChange = current.expense - previous.expense
        )
    }

    /**
     * Calculates the savings rate based on income and expense.
     * Savings Rate = (Income + Expense) / Income
     *
     * @param income Total income (positive)
     * @param expense Total expense (negative)
     * @return Savings rate as a fraction (0.0 to 1.0)
     */
    fun calculateSavingsRate(income: BigDecimal, expense: BigDecimal): Double {
        if (income.signum() <= 0) return 0.0
        val net = income + expense
        return if (net.signum() <= 0) 0.0 else net.divide(income, 4, RoundingMode.HALF_UP)
            .toDouble()
    }

    /**
     * Calculates the burn rate (monthly average expense).
     *
     * @param totalExpense Total absolute expense
     * @param daysActive Number of days the account has been active
     * @return Monthly burn rate
     */
    fun calculateBurnRate(totalExpense: BigDecimal, daysActive: Long): BigDecimal {
        if (daysActive <= 0) return BigDecimal.ZERO
        val dailyAvg =
            totalExpense.abs().divide(BigDecimal.valueOf(daysActive), 10, RoundingMode.HALF_UP)
        return dailyAvg.multiply(BigDecimal.valueOf(30)).setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * Calculates the standard deviation (volatility) of a list of numeric values.
     *
     * @param values List of numeric values (e.g., daily cash flows)
     * @return Standard deviation as BigDecimal
     */
    fun calculateVolatility(values: List<Double>): BigDecimal {
        if (values.isEmpty()) return BigDecimal.ZERO

        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance).toBigDecimal()
    }
}
