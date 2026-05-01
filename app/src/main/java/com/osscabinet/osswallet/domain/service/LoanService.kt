package com.osscabinet.ossWallet.domain.service

import com.osscabinet.ossWallet.domain.model.*
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId

object LoanService {

    fun calculateRemainingBalance(loan: Loan, payments: List<LoanPayment>): BigDecimal {
        val totalPaid = payments.fold(BigDecimal.ZERO) { acc, p -> acc + p.amount }
        return (loan.principalAmount - totalPaid).coerceAtLeast(BigDecimal.ZERO)
    }

    fun calculateInterestAccrued(loan: Loan, now: Long): BigDecimal {
        val rate = loan.interestRate ?: return BigDecimal.ZERO
        val zone = ZoneId.systemDefault()
        val startDay = Instant.ofEpochMilli(loan.startDate).atZone(zone).toLocalDate()
        val nowDay = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val days = java.time.temporal.ChronoUnit.DAYS.between(startDay, nowDay).coerceAtLeast(0)
        val annualRate = BigDecimal(rate.toDouble())
        return loan.principalAmount
            .multiply(annualRate)
            .multiply(BigDecimal(days))
            .divide(BigDecimal(365), 10, RoundingMode.HALF_UP)
    }

    fun getAmortizationSchedule(
        loan: Loan,
        monthlyPayment: BigDecimal,
        startDate: Long
    ): List<AmortizationEntry> {
        val rate = loan.interestRate ?: return emptyList()
        val monthlyRate = BigDecimal(rate.toDouble())
            .divide(BigDecimal(12), 10, RoundingMode.HALF_UP)
        val zone = ZoneId.systemDefault()
        var date = Instant.ofEpochMilli(startDate).atZone(zone).toLocalDate()
        var balance = loan.principalAmount
        val entries = mutableListOf<AmortizationEntry>()
        var period = 1
        while (balance > BigDecimal.ZERO) {
            val interest = balance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP)
            val principal = (monthlyPayment - interest).coerceAtMost(balance)
            balance = (balance - principal).coerceAtLeast(BigDecimal.ZERO)
            date = date.plusMonths(1)
            entries.add(
                AmortizationEntry(
                    period = period++,
                    date = date.atStartOfDay(zone).toInstant().toEpochMilli(),
                    payment = principal + interest,
                    principal = principal,
                    interest = interest,
                    balance = balance
                )
            )
            if (period > 600) break
        }
        return entries
    }

    fun buildSummary(loan: Loan, payments: List<LoanPayment>, now: Long): LoanSummary {
        val totalPaid = payments.fold(BigDecimal.ZERO) { acc, p -> acc + p.amount }
        val remaining = (loan.principalAmount - totalPaid).coerceAtLeast(BigDecimal.ZERO)
        val interest = calculateInterestAccrued(loan, now)
        return LoanSummary(
            loan = loan,
            totalPaid = totalPaid,
            remainingBalance = remaining,
            interestAccrued = interest
        )
    }

    fun settledCopy(loan: Loan): Loan = loan.copy(status = LoanStatus.SETTLED)
}
