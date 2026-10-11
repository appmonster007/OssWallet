package com.osscabinet.ossWallet.domain.service.management

import com.osscabinet.ossWallet.domain.model.AmortizationEntry
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.LoanPayment
import com.osscabinet.ossWallet.domain.model.LoanStatus
import com.osscabinet.ossWallet.domain.model.LoanSummary
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
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

    /** [Loan.interestRate] is stored as a percentage (3.5 = 3.5%); calculations need the fraction. */
    private fun annualRateFraction(loan: Loan): BigDecimal? =
        loan.interestRate?.let { BigDecimal(it.toString()).divide(BigDecimal(100)) }

    fun calculateInterestAccrued(loan: Loan, now: Long): BigDecimal {
        val annualRate = annualRateFraction(loan) ?: return BigDecimal.ZERO
        val zone = ZoneId.systemDefault()
        val startDay = Instant.ofEpochMilli(loan.startDate).atZone(zone).toLocalDate()
        val nowDay = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val days = java.time.temporal.ChronoUnit.DAYS.between(startDay, nowDay).coerceAtLeast(0)
        return loan.principalAmount
            .multiply(annualRate)
            .multiply(BigDecimal(days))
            .divide(BigDecimal(365), 10, RoundingMode.HALF_UP)
    }

    /** Loans with an expected settlement date first, earliest first; the rest keep their relative order. */
    fun sortByDueDate(loans: List<Loan>): List<Loan> =
        loans.sortedWith(compareBy<Loan>({ it.expectedSettlementDate == null }, { it.expectedSettlementDate }))

    fun sortByName(loans: List<Loan>): List<Loan> = loans.sortedBy { it.name.lowercase() }

    /** Largest amount still owed first (balance plus interest), compared in [targetCurrency]. */
    fun sortByOwed(
        summaries: List<LoanSummary>,
        targetCurrency: String,
        exchangeRates: ExchangeRateSnapshot
    ): List<LoanSummary> = summaries.sortedByDescending {
        CurrencyService.convertAmount(
            amount = it.remainingBalance + it.interestAccrued,
            fromCurrency = it.loan.currency,
            toCurrency = targetCurrency,
            exchangeRates = exchangeRates
        )
    }

    fun getAmortizationSchedule(
        loan: Loan,
        monthlyPayment: BigDecimal,
        startDate: Long
    ): List<AmortizationEntry> {
        val monthlyRate = (annualRateFraction(loan) ?: return emptyList())
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
        precision: Int = com.osscabinet.ossWallet.domain.service.currency.CurrencyService.precision
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
     * Filters transactions based on loan's filter.
     */
    fun buildSummaryFromTransactions(
        loan: Loan,
        transactions: List<Transaction>,
        exchangeRates: ExchangeRateSnapshot,
        now: Long,
        precision: Int = CurrencyService.precision,
        allCategories: List<com.osscabinet.ossWallet.domain.model.Category> = emptyList()
    ): LoanSummary {
        val filteredTransactions = filterTransactionsForLoan(loan, transactions, allCategories)
        val totalPaid = filteredTransactions.fold(BigDecimal.ZERO) { acc, tx ->
            val isRepayment = when (loan.loanType) {
                LoanType.LENT -> tx.amount.signum() > 0 // Received money (Income)
                LoanType.BORROWED -> tx.amount.signum() < 0 // Paid money (Expense)
            }
            if (isRepayment) {
                val converted = CurrencyService.convertAmount(
                    amount = tx.amount.abs(),
                    fromCurrency = tx.currency,
                    toCurrency = loan.currency,
                    exchangeRates = exchangeRates,
                    precision = 10
                )
                acc + converted
            } else acc
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
     * Builds the canonical TransactionFilter for a loan. Use this everywhere transactions
     * need to be filtered for a loan — service layer and UI — to guarantee consistency.
     */
    fun buildTransactionFilter(
        loan: Loan,
        allCategories: List<com.osscabinet.ossWallet.domain.model.Category> = emptyList()
    ): TransactionFilter {
        val expandedCategoryIds = if (allCategories.isNotEmpty() && loan.categoryIds.isNotEmpty()) {
            TransactionFilterService.expandCategoryIds(loan.categoryIds.toList(), allCategories).toSet()
        } else loan.categoryIds

        return TransactionFilter(
            categoryIds = expandedCategoryIds,
            tags = loan.tags,
            accountIds = if (loan.linkedAccountId.isBlank()) emptySet() else setOf(loan.linkedAccountId),
            paymentMethods = loan.paymentMethods,
            statuses = loan.statuses,
            currencies = loan.currencies,
            transactionTypes = loan.trackTypes,
            linkedTransactionIds = loan.linkedTransactionIds,
            timelineRange = loan.timelineRange
        ).let { loan.advanced.applyTo(it) }
    }

    /**
     * Filter transactions for a specific loan based on its filter details.
     */
    fun filterTransactionsForLoan(
        loan: Loan,
        transactions: List<Transaction>,
        allCategories: List<com.osscabinet.ossWallet.domain.model.Category> = emptyList()
    ): List<Transaction> {
        return TransactionFilterService.filterTransactions(
            transactions,
            buildTransactionFilter(loan, allCategories)
        )
    }

    fun settledCopy(loan: Loan): Loan = loan.copy(status = LoanStatus.SETTLED)
}
