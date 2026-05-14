package com.osscabinet.ossWallet.domain.service.management

import com.osscabinet.ossWallet.domain.model.AmortizationEntry
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.LoanPayment
import com.osscabinet.ossWallet.domain.model.LoanStatus
import com.osscabinet.ossWallet.domain.model.LoanSummary
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.service.filter.TransactionFilterService
import java.math.BigDecimal
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

    fun buildSummary(
        loan: Loan,
        payments: List<LoanPayment>,
        now: Long,
        precision: Int = 2
    ): LoanSummary {
        val totalPaid = payments.fold(BigDecimal.ZERO) { acc, p -> acc + p.amount }
        val remaining = (loan.principalAmount - totalPaid).coerceAtLeast(BigDecimal.ZERO)
        val interest = calculateInterestAccrued(loan, now)
        return LoanSummary(
            loan = loan,
            totalPaid = totalPaid.setScale(precision, RoundingMode.HALF_UP),
            remainingBalance = remaining.setScale(precision, RoundingMode.HALF_UP),
            interestAccrued = interest.setScale(precision, RoundingMode.HALF_UP)
        )
    }

    /**
     * Calculate loan summary from transactions instead of loan payments.
     * Filters transactions based on loan's linked filter.
     */
    fun buildSummaryFromTransactions(
        loan: Loan,
        transactions: List<Transaction>,
        now: Long,
        precision: Int = 2
    ): LoanSummary {
        val filteredTransactions = filterTransactionsForLoan(loan, transactions)
        val totalPaid = filteredTransactions.fold(BigDecimal.ZERO) { acc, tx ->
            acc + tx.amount.abs()
        }
        val remaining = (loan.principalAmount - totalPaid).coerceAtLeast(BigDecimal.ZERO)
        val interest = calculateInterestAccrued(loan, now)
        return LoanSummary(
            loan = loan,
            totalPaid = totalPaid.setScale(precision, RoundingMode.HALF_UP),
            remainingBalance = remaining.setScale(precision, RoundingMode.HALF_UP),
            interestAccrued = interest.setScale(precision, RoundingMode.HALF_UP)
        )
    }

    /**
     * Filter transactions for a specific loan based on its filter details.
     */
    fun filterTransactionsForLoan(loan: Loan, transactions: List<Transaction>): List<Transaction> {
        val filter = TransactionFilter(
            categoryIds = loan.categoryIds,
            tags = loan.tags,
            accountIds = if (loan.linkedAccountId.isBlank()) emptyList() else listOf(loan.linkedAccountId),
            paymentMethods = loan.paymentMethods,
            statuses = loan.statuses,
            linkedTransactionIds = loan.linkedTransactionIds
        )
        return TransactionFilterService.filterTransactions(transactions, filter)
    }

    fun settledCopy(loan: Loan): Loan = loan.copy(status = LoanStatus.SETTLED)
}
