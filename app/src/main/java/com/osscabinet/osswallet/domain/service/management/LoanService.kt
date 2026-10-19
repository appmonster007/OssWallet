package com.osscabinet.ossWallet.domain.service.management

import com.osscabinet.ossWallet.domain.model.AmortizationEntry
import com.osscabinet.ossWallet.domain.model.AmortizationScheduleSummary
import com.osscabinet.ossWallet.domain.model.AmortizationYearlySummary
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

    fun calculateLoanTermMonths(loan: Loan): Int {
        val targetEnd = loan.expectedSettlementDate
        if (targetEnd == null || targetEnd <= loan.startDate) {
            return 12
        }
        val zone = ZoneId.systemDefault()
        val start = Instant.ofEpochMilli(loan.startDate).atZone(zone).toLocalDate()
        val end = Instant.ofEpochMilli(targetEnd).atZone(zone).toLocalDate()
        val months = java.time.temporal.ChronoUnit.MONTHS.between(start, end).toInt()
        return months.coerceIn(1, 600)
    }

    fun calculateStandardMonthlyPayment(loan: Loan): BigDecimal {
        val rateFraction = annualRateFraction(loan)
            ?: return loan.principalAmount.divide(BigDecimal(12), 2, RoundingMode.HALF_UP)
        val monthlyRate = rateFraction.divide(BigDecimal(12), 10, RoundingMode.HALF_UP)
        val termMonths = calculateLoanTermMonths(loan)

        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return loan.principalAmount.divide(BigDecimal(termMonths), 2, RoundingMode.HALF_UP)
        }

        val onePlusR = BigDecimal.ONE.add(monthlyRate)
        val onePlusRToN = onePlusR.pow(termMonths, java.math.MathContext(10))
        val numerator = loan.principalAmount.multiply(monthlyRate).multiply(onePlusRToN)
        val denominator = onePlusRToN.subtract(BigDecimal.ONE)

        return if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            loan.principalAmount.divide(BigDecimal(termMonths), 2, RoundingMode.HALF_UP)
        } else {
            numerator.divide(denominator, 2, RoundingMode.HALF_UP)
        }
    }

    fun getAmortizationSchedule(
        loan: Loan,
        monthlyPayment: BigDecimal = calculateStandardMonthlyPayment(loan),
        startDate: Long = loan.startDate
    ): List<AmortizationEntry> {
        val rateFraction = annualRateFraction(loan) ?: return emptyList()
        val monthlyRate = rateFraction.divide(BigDecimal(12), 10, RoundingMode.HALF_UP)
        val zone = ZoneId.systemDefault()
        var date = Instant.ofEpochMilli(startDate).atZone(zone).toLocalDate()
        var balance = loan.principalAmount
        val entries = mutableListOf<AmortizationEntry>()
        var period = 1
        val effectiveMonthlyPayment = if (monthlyPayment <= BigDecimal.ZERO) {
            calculateStandardMonthlyPayment(loan)
        } else monthlyPayment

        while (balance > BigDecimal.ZERO) {
            val interest = balance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP)
            val principal = (effectiveMonthlyPayment - interest).coerceAtLeast(BigDecimal.ZERO).coerceAtMost(balance)
            val actualPayment = if (balance <= principal) balance + interest else effectiveMonthlyPayment
            balance = (balance - principal).coerceAtLeast(BigDecimal.ZERO)
            date = date.plusMonths(1)
            entries.add(
                AmortizationEntry(
                    period = period++,
                    date = date.atStartOfDay(zone).toInstant().toEpochMilli(),
                    payment = actualPayment,
                    principal = principal,
                    interest = interest,
                    balance = balance
                )
            )
            if (period > 600 || (principal.compareTo(BigDecimal.ZERO) == 0 && balance > BigDecimal.ZERO)) break
        }
        return entries
    }

    fun getAmortizationYearlySummaries(entries: List<AmortizationEntry>): List<AmortizationYearlySummary> {
        val zone = ZoneId.systemDefault()
        return entries.groupBy { entry ->
            Instant.ofEpochMilli(entry.date).atZone(zone).year
        }.map { (year, yearEntries) ->
            val totalPayment = yearEntries.fold(BigDecimal.ZERO) { acc, e -> acc + e.payment }
            val totalPrincipal = yearEntries.fold(BigDecimal.ZERO) { acc, e -> acc + e.principal }
            val totalInterest = yearEntries.fold(BigDecimal.ZERO) { acc, e -> acc + e.interest }
            val endingBalance = yearEntries.last().balance
            AmortizationYearlySummary(
                year = year,
                totalPayment = totalPayment,
                totalPrincipal = totalPrincipal,
                totalInterest = totalInterest,
                endingBalance = endingBalance
            )
        }
    }

    fun calculateScheduleSummary(
        loan: Loan,
        entries: List<AmortizationEntry>,
        monthlyPayment: BigDecimal
    ): AmortizationScheduleSummary {
        val totalPrincipal = loan.principalAmount
        val totalInterest = entries.fold(BigDecimal.ZERO) { acc, e -> acc + e.interest }
        return AmortizationScheduleSummary(
            totalPrincipal = totalPrincipal,
            totalInterest = totalInterest,
            totalCost = totalPrincipal + totalInterest,
            monthlyPayment = monthlyPayment,
            totalPeriods = entries.size
        )
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

    fun getPeriodSummaryForOffset(
        loan: Loan,
        transactions: List<Transaction>,
        exchangeRates: ExchangeRateSnapshot,
        now: Long,
        offset: Int,
        allCategories: List<com.osscabinet.ossWallet.domain.model.Category> = emptyList(),
        precision: Int = CurrencyService.precision
    ): LoanSummary {
        if (loan.timelineRange is com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.AllTime) {
            return buildSummaryFromTransactions(loan, transactions, exchangeRates, now, precision, allCategories)
        }

        val shiftedRange = com.osscabinet.ossWallet.domain.service.timeline.TimelineService.shiftRange(loan.timelineRange, offset)
        val (start, end) = com.osscabinet.ossWallet.domain.service.timeline.TimelineService.getDateRange(shiftedRange, referenceTime = now)
        val evalLoan = if (start != null && end != null) {
            loan.copy(timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.CustomRange(start, end))
        } else loan.copy(timelineRange = shiftedRange)

        return buildSummaryFromTransactions(evalLoan, transactions, exchangeRates, now, precision, allCategories)
    }

    fun getPeriodHistory(
        loan: Loan,
        transactions: List<Transaction>,
        exchangeRates: ExchangeRateSnapshot,
        now: Long,
        allCategories: List<com.osscabinet.ossWallet.domain.model.Category> = emptyList(),
        offset: Int = 0,
        count: Int = 5,
        precision: Int = CurrencyService.precision
    ): List<LoanSummary> {
        if (loan.timelineRange is com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.AllTime) return emptyList()

        val endOffset = offset
        val startOffset = offset - count
        return (startOffset..endOffset).map { targetOffset ->
            getPeriodSummaryForOffset(
                loan = loan,
                transactions = transactions,
                exchangeRates = exchangeRates,
                now = now,
                offset = targetOffset,
                allCategories = allCategories,
                precision = precision
            )
        }
    }

    fun settledCopy(loan: Loan): Loan = loan.copy(status = LoanStatus.SETTLED)
}
