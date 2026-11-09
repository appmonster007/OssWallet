package com.osscabinet.ossWallet.domain.service.ledger

import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class LedgerServiceTest {

    private val day0 = 1_700_000_000_000L
    private val day1 = day0 + 1 * 86_400_000L
    private val day2 = day0 + 2 * 86_400_000L

    private fun bd(v: String) = BigDecimal(v)

    @Test
    fun `A-USD ledger matches the golden progression`() {
        // Scenario 1: id1 (+1000, day0), id4 (-50, day1), id5 (+200, day1), id10 (-25, day1), id6 (-100, day2)
        val rows = listOf(
            LedgerRow(1, day0, bd("1000")),
            LedgerRow(4, day1, bd("-50")),
            LedgerRow(5, day1, bd("200")),
            LedgerRow(10, day1, bd("-25")),
            LedgerRow(6, day2, bd("-100"))
        )

        val entries = LedgerService.rebuild(rows)

        assertEquals(0, bd("0").compareTo(entries[0].balanceBefore))
        assertEquals(0, bd("1000").compareTo(entries[0].balanceAfter))
        assertEquals(0, bd("1000").compareTo(entries[1].balanceBefore))
        assertEquals(0, bd("950").compareTo(entries[1].balanceAfter))
        assertEquals(0, bd("950").compareTo(entries[2].balanceBefore))
        assertEquals(0, bd("1150").compareTo(entries[2].balanceAfter))
        assertEquals(0, bd("1150").compareTo(entries[3].balanceBefore))
        assertEquals(0, bd("1125").compareTo(entries[3].balanceAfter))
        assertEquals(0, bd("1125").compareTo(entries[4].balanceBefore))
        assertEquals(0, bd("1025").compareTo(entries[4].balanceAfter))
    }

    @Test
    fun `rows sharing one timestamp break ties by ascending id regardless of input order`() {
        // id4, id5, id10 all carry the exact same timestamp (day1). Feed them in a scrambled
        // order on purpose - the result must still read id4, then id5, then id10.
        val rows = listOf(
            LedgerRow(10, day1, bd("-25")),
            LedgerRow(4, day1, bd("-50")),
            LedgerRow(5, day1, bd("200"))
        )

        val entries = LedgerService.rebuild(rows, startingBalance = bd("1000"))

        assertEquals(listOf(4L, 5L, 10L), entries.map { it.id })
        assertEquals(0, bd("950").compareTo(entries[0].balanceAfter))
        assertEquals(0, bd("1150").compareTo(entries[1].balanceAfter))
        assertEquals(0, bd("1125").compareTo(entries[2].balanceAfter))
    }

    @Test
    fun `an excluded row's effect still propagates to balances after it`() {
        // This is the heart of the category-filter bug: id5 is "removed" from the input here to
        // simulate what a naive pre-filtered balance computation would see. The row AFTER id5
        // (id10) must NOT land on 925 (1000-50-25, as if id5 never happened) - proving that the
        // only correct way to get id10's balance is to feed id5 into the ledger even when it
        // won't be displayed. This test documents the wrong answer so a future regression is
        // caught immediately, by computing the correct ledger WITH id5 and asserting it differs
        // from the naive without-id5 sum.
        val withId5 = LedgerService.rebuild(
            listOf(LedgerRow(4, day1, bd("-50")), LedgerRow(5, day1, bd("200")), LedgerRow(10, day1, bd("-25"))),
            startingBalance = bd("1000")
        )
        val naiveWithoutId5 = LedgerService.rebuild(
            listOf(LedgerRow(4, day1, bd("-50")), LedgerRow(10, day1, bd("-25"))),
            startingBalance = bd("1000")
        )

        val correctClosingBalance = withId5.last().balanceAfter
        val naiveClosingBalance = naiveWithoutId5.last().balanceAfter

        assertEquals(0, bd("1125").compareTo(correctClosingBalance))
        assertEquals(0, bd("925").compareTo(naiveClosingBalance))
        assert(correctClosingBalance.compareTo(naiveClosingBalance) != 0) {
            "correct and naive closing balances must differ - if they match, the fixture is wrong"
        }
    }

    @Test
    fun `empty input returns empty output`() {
        assertEquals(emptyList<LedgerEntry>(), LedgerService.rebuild(emptyList(), bd("500")))
    }

    @Test
    fun `an internal transfer between two legs of the same account set nets to zero`() {
        // id6 (-100, A leg) and id7 (+100, B leg) folded into ONE combined ledger (as a
        // multi-account header would) must net back to the starting balance.
        val rows = listOf(LedgerRow(6, day2, bd("-100")), LedgerRow(7, day2, bd("100")))
        val entries = LedgerService.rebuild(rows, startingBalance = bd("1625"))
        assertEquals(0, bd("1625").compareTo(entries.last().balanceAfter))
    }

    @Test
    fun `withBalances keeps every row when unsaved transactions share an id`() {
        // Projected or unsaved rows carry id 0; each must still get its own balance.
        fun tx(amount: String, at: Long) = Transaction(
            id = 0, name = "t", amount = bd(amount), timestamp = at, currency = "USD",
            transactionType = TransactionType.EXPENSE, accountId = "A", categoryId = 1000L
        )
        val rows = listOf(tx("-10", day0), tx("-20", day1), tx("-30", day2))

        val result = LedgerService.withBalances(rows, mapOf("A" to bd("100")), scale = 2) { it.amount }

        assertEquals(3, result.size)
        assertEquals(listOf("90.00", "70.00", "40.00"), result.map { it.balanceAfter.toPlainString() })
        assertEquals(listOf(day0, day1, day2), result.map { it.transaction.timestamp })
    }
}
