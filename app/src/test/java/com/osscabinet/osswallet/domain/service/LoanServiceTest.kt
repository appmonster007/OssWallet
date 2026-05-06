package com.osscabinet.ossWallet.domain.service

import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.LoanPayment
import com.osscabinet.ossWallet.domain.model.LoanStatus
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.domain.service.management.LoanService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset

class LoanServiceTest {

    private fun epochOf(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private fun makeLoan(
        principal: BigDecimal = BigDecimal("1000"),
        interestRate: Float? = null,
        startDate: Long = epochOf(2024, 1, 1)
    ) = Loan(
        id = 1L,
        name = "Test Loan",
        principalAmount = principal,
        currency = "USD",
        interestRate = interestRate,
        startDate = startDate,
        lenderOrBorrowerName = "Alice",
        loanType = LoanType.BORROWED,
        linkedAccountId = "",
        status = LoanStatus.ACTIVE,
        profileId = 1
    )

    private fun makePayment(loanId: Long = 1L, amount: BigDecimal, date: Long = System.currentTimeMillis()) =
        LoanPayment(id = 0L, loanId = loanId, amount = amount, date = date)

    @Test
    fun `calculateRemainingBalance deducts payments from principal`() {
        val loan = makeLoan(principal = BigDecimal("1000"))
        val payments = listOf(
            makePayment(amount = BigDecimal("300")),
            makePayment(amount = BigDecimal("200"))
        )
        val remaining = LoanService.calculateRemainingBalance(loan, payments)
        assertEquals(0, BigDecimal("500").compareTo(remaining))
    }

    @Test
    fun `calculateRemainingBalance does not go negative`() {
        val loan = makeLoan(principal = BigDecimal("100"))
        val payments = listOf(makePayment(amount = BigDecimal("200")))
        val remaining = LoanService.calculateRemainingBalance(loan, payments)
        assertEquals(0, BigDecimal.ZERO.compareTo(remaining))
    }

    @Test
    fun `calculateRemainingBalance with no payments returns principal`() {
        val loan = makeLoan(principal = BigDecimal("1000"))
        val remaining = LoanService.calculateRemainingBalance(loan, emptyList())
        assertEquals(0, BigDecimal("1000").compareTo(remaining))
    }

    @Test
    fun `calculateInterestAccrued returns zero when no interest rate`() {
        val loan = makeLoan(interestRate = null)
        val now = epochOf(2024, 6, 1)
        val interest = LoanService.calculateInterestAccrued(loan, now)
        assertEquals(0, BigDecimal.ZERO.compareTo(interest))
    }

    @Test
    fun `calculateInterestAccrued returns positive value when rate set`() {
        val loan = makeLoan(
            principal = BigDecimal("1000"),
            interestRate = 0.1f,
            startDate = epochOf(2024, 1, 1)
        )
        val now = epochOf(2024, 7, 1)
        val interest = LoanService.calculateInterestAccrued(loan, now)
        assertTrue("Interest should be positive", interest > BigDecimal.ZERO)
    }

    @Test
    fun `buildSummary matches individual calculations`() {
        val loan = makeLoan(principal = BigDecimal("1000"), interestRate = 0.12f)
        val payments = listOf(
            makePayment(amount = BigDecimal("200"), date = epochOf(2024, 2, 1)),
            makePayment(amount = BigDecimal("300"), date = epochOf(2024, 4, 1))
        )
        val now = epochOf(2024, 6, 1)
        val summary = LoanService.buildSummary(loan, payments, now)

        val expectedRemaining = LoanService.calculateRemainingBalance(loan, payments)
        val expectedInterest = LoanService.calculateInterestAccrued(loan, now)

        assertEquals(0, expectedRemaining.compareTo(summary.remainingBalance))
        assertEquals(0, BigDecimal("500").compareTo(summary.totalPaid))
        assertTrue("Interest should match", expectedInterest.subtract(summary.interestAccrued).abs() < BigDecimal("0.01"))
    }

    @Test
    fun `settledCopy sets status to SETTLED`() {
        val loan = makeLoan()
        val settled = LoanService.settledCopy(loan)
        assertEquals(LoanStatus.SETTLED, settled.status)
        assertEquals(loan.id, settled.id)
        assertEquals(loan.name, settled.name)
    }

    @Test
    fun `getAmortizationSchedule returns empty list when no interest rate`() {
        val loan = makeLoan(interestRate = null)
        val schedule = LoanService.getAmortizationSchedule(loan, BigDecimal("100"), epochOf(2024, 1, 1))
        assertTrue(schedule.isEmpty())
    }

    @Test
    fun `getAmortizationSchedule returns entries that sum to principal`() {
        val loan = makeLoan(principal = BigDecimal("1200"), interestRate = 0.12f)
        val monthlyPayment = BigDecimal("120")
        val schedule = LoanService.getAmortizationSchedule(loan, monthlyPayment, epochOf(2024, 1, 1))
        assertTrue(schedule.isNotEmpty())
        val totalPrincipal = schedule.fold(BigDecimal.ZERO) { acc, e -> acc + e.principal }
        assertEquals(0, BigDecimal("1200").compareTo(totalPrincipal.setScale(0, java.math.RoundingMode.HALF_UP)))
    }
}
