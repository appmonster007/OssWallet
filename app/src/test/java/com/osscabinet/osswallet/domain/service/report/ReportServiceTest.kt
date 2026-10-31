package com.osscabinet.ossWallet.domain.service.report

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class ReportServiceTest {
    private val food = Category(1, "Food", "x", 0, false, null)
    private val salary = Category(2, "Salary", "x", 0, false, null)
    private val rent = Category(3, "Rent", "x", 0, false, null)
    private val bank = Account("acc1", "Main Bank", "USD", AccountStructureType.BANK, "#000", "i")
    private val cash = Account("acc2", "Cash", "USD", AccountStructureType.CASH, "#000", "i")

    private fun tx(
        id: Long, amount: String, type: TransactionType, ts: Long, cat: Long = 1, currency: String = "USD",
        account: String? = "acc1", name: String = "t$id", tags: Set<String> = emptySet()
    ) = Transaction(
        id = id, name = name, amount = BigDecimal(amount), timestamp = ts, currency = currency,
        transactionType = type, accountId = account, categoryId = cat, tags = tags
    )

    private fun scope(transfers: Boolean = false) = ReportScope("Report", "Oct 2026", "All accounts", "All tags", transfers)

    private fun build(list: List<Transaction>, transfers: Boolean = false) =
        ReportService.build(list, listOf(food, salary, rent), listOf(bank, cash), scope(transfers), generatedAt = 0)

    @Test
    fun `totals add income and negative expenses per currency`() {
        val data = build(
            listOf(
                tx(1, "-30.50", TransactionType.EXPENSE, 10),
                tx(2, "-19.50", TransactionType.EXPENSE, 20),
                tx(3, "1000", TransactionType.INCOME, 30, cat = 2)
            )
        )
        val usd = data.totals.single()
        assertEquals(0, BigDecimal("1000").compareTo(usd.income))
        assertEquals(0, BigDecimal("-50.00").compareTo(usd.expense))
        assertEquals(0, BigDecimal("950.00").compareTo(usd.net))
        assertEquals(3, usd.count)
    }

    @Test
    fun `currencies are never mixed`() {
        val data = build(
            listOf(
                tx(1, "-10", TransactionType.EXPENSE, 1, currency = "USD"),
                tx(2, "-1000", TransactionType.EXPENSE, 2, currency = "JPY")
            )
        )
        assertEquals(listOf("JPY", "USD"), data.totals.map { it.currency })
        assertEquals(0, BigDecimal("-1000").compareTo(data.totals.first { it.currency == "JPY" }.expense))
    }

    @Test
    fun `transfers are left out by default and do not affect net`() {
        val list = listOf(
            tx(1, "-10", TransactionType.EXPENSE, 1),
            tx(2, "-500", TransactionType.TRANSFER_OUTFLOW, 2),
            tx(3, "500", TransactionType.TRANSFER_INFLOW, 3, account = "acc2")
        )
        val without = build(list)
        assertEquals(1, without.rows.size)

        val with = build(list, transfers = true)
        assertEquals(3, with.rows.size)
        val t = with.totals.single()
        assertEquals(0, BigDecimal("-10").compareTo(t.net))
        assertEquals(0, BigDecimal("-500").compareTo(t.transferOut))
        assertEquals(0, BigDecimal("500").compareTo(t.transferIn))
    }

    @Test
    fun `rows are chronological and resolve account and category names`() {
        val data = build(
            listOf(
                tx(2, "-5", TransactionType.EXPENSE, 200, account = "acc2"),
                tx(1, "-5", TransactionType.EXPENSE, 100, cat = 3),
                tx(3, "-5", TransactionType.EXPENSE, 300, account = null, cat = 99),
                tx(4, "-5", TransactionType.EXPENSE, 400, account = "gone")
            )
        )
        assertEquals(listOf(1L, 2L, 3L, 4L).map { "t$it" }, data.rows.map { it.name })
        assertEquals("Rent", data.rows[0].category)
        assertEquals("Main Bank", data.rows[0].account)
        assertEquals("Cash", data.rows[1].account)
        assertEquals(ReportService.OUTSIDE_WALLET_LABEL, data.rows[2].account)
        assertEquals(ReportService.UNCATEGORIZED_LABEL, data.rows[2].category)
        assertEquals(ReportService.UNKNOWN_ACCOUNT_LABEL, data.rows[3].account)
    }

    @Test
    fun `category shares are of spending and sum to one`() {
        val data = build(
            listOf(
                tx(1, "-75", TransactionType.EXPENSE, 1, cat = 1),
                tx(2, "-25", TransactionType.EXPENSE, 2, cat = 3),
                tx(3, "900", TransactionType.INCOME, 3, cat = 2)
            )
        )
        val cats = data.topExpenseCategories
        assertEquals(listOf("Food", "Rent"), cats.map { it.category })
        assertEquals(0.75, cats[0].share, 1e-6)
        assertEquals(0.25, cats[1].share, 1e-6)
        assertEquals(1.0, cats.sumOf { it.share }, 1e-6)
        assertTrue("income is not a spending category", cats.none { it.category == "Salary" })
    }

    @Test
    fun `an empty selection yields an empty report with no totals`() {
        val data = build(emptyList())
        assertTrue(data.isEmpty)
        assertTrue(data.totals.isEmpty())
        assertTrue(data.topExpenseCategories.isEmpty())
    }

    @Test
    fun `tags are sorted for stable output`() {
        val data = build(listOf(tx(1, "-1", TransactionType.EXPENSE, 1, tags = setOf("work", "alpha", "tax"))))
        assertEquals(listOf("alpha", "tax", "work"), data.rows.single().tags)
    }

    @Test
    fun `category list is capped per currency`() {
        val many = (1L..12L).map { tx(it, "-${it}", TransactionType.EXPENSE, it, cat = it + 100) }
        val names = (1L..12L).map { Category(it + 100, "Cat$it", "x", 0, false, null) }
        val cats = ReportService.build(many, names, listOf(bank), scope(), 0).topExpenseCategories
        assertEquals(ReportService.TOP_CATEGORIES_PER_CURRENCY, cats.size)
    }
}
