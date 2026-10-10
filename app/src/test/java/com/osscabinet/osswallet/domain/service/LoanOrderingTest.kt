package com.osscabinet.ossWallet.domain.service

import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.LoanSummary
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.domain.service.management.LoanService
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class LoanOrderingTest {

    private fun loan(name: String, due: Long? = null, currency: String = "USD") = Loan(
        name = name,
        principalAmount = BigDecimal("1000"),
        currency = currency,
        startDate = 0L,
        lenderOrBorrowerName = "x",
        loanType = LoanType.BORROWED,
        linkedAccountId = "a",
        expectedSettlementDate = due,
        profileId = 1
    )

    private fun summary(loan: Loan, remaining: String, interest: String = "0") = LoanSummary(
        loan = loan,
        totalPaid = BigDecimal.ZERO,
        remainingBalance = BigDecimal(remaining),
        interestAccrued = BigDecimal(interest)
    )

    private val usd = ExchangeRateSnapshot(anchor = "USD", rates = emptyMap())

    @Test
    fun `due soonest puts loans with a date first and earliest first`() {
        val result = LoanService.sortByDueDate(
            listOf(loan("none"), loan("late", due = 300), loan("soon", due = 100), loan("also none"))
        )

        assertEquals(listOf("soon", "late", "none", "also none"), result.map { it.name })
    }

    @Test
    fun `name order ignores case`() {
        val result = LoanService.sortByName(listOf(loan("bike"), loan("Car"), loan("apple")))

        assertEquals(listOf("apple", "bike", "Car"), result.map { it.name })
    }

    @Test
    fun `most owed counts accrued interest`() {
        val a = summary(loan("a"), remaining = "500", interest = "0")
        val b = summary(loan("b"), remaining = "450", interest = "100")

        val result = LoanService.sortByOwed(listOf(a, b), "USD", usd)

        assertEquals(listOf("b", "a"), result.map { it.loan.name })
    }

    @Test
    fun `most owed compares loans in the card currency`() {
        val rates = ExchangeRateSnapshot(anchor = "USD", rates = mapOf("EUR" to BigDecimal("0.5")))
        // 400 EUR is 800 USD, which is more than 600 USD.
        val eur = summary(loan("eur", currency = "EUR"), remaining = "400")
        val usdLoan = summary(loan("usd"), remaining = "600")

        val result = LoanService.sortByOwed(listOf(usdLoan, eur), "USD", rates)

        assertEquals(listOf("eur", "usd"), result.map { it.loan.name })
    }
}
