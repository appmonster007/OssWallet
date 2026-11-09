package com.osscabinet.osswallet.domain.service.search

import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.service.search.SearchQueryParser
import com.osscabinet.ossWallet.domain.service.search.TransactionSearchMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class TransactionSearchMatcherTest {

    private fun tx(
        id: Long = 1,
        name: String = "",
        payee: String = "",
        note: String = "",
        amount: String = "10.00",
        tags: Set<String> = emptySet(),
        categoryId: Long = 1
    ) = Transaction(
        id = id, name = name, amount = BigDecimal(amount), timestamp = id, currency = "USD",
        transactionType = TransactionType.EXPENSE, accountId = "a", categoryId = categoryId,
        tags = tags, payee = payee, note = note
    )

    private fun matches(query: String, tx: Transaction, category: String? = null) =
        TransactionSearchMatcher.matches(SearchQueryParser.parse(query), tx, category)

    @Test fun `an empty query matches nothing`() {
        assertFalse(matches("   ", tx(note = "anything")))
    }

    @Test fun `a word matches note payee name or category case-insensitively`() {
        assertTrue(matches("laptop", tx(note = "New LAPTOP bag")))
        assertTrue(matches("starbucks", tx(payee = "Starbucks")))
        assertTrue(matches("rent", tx(name = "Monthly rent")))
        assertTrue(matches("groceries", tx(), category = "Groceries"))
        assertFalse(matches("laptop", tx(note = "phone")))
    }

    @Test fun `every word must be found but not necessarily in the same field`() {
        val t = tx(payee = "Starbucks", note = "team coffee")
        assertTrue(matches("starbucks coffee", t))
        assertFalse(matches("starbucks lunch", t))
    }

    @Test fun `a tag matches by prefix and all tags are required`() {
        val t = tx(tags = setOf("holiday", "family"))
        assertTrue(matches("#hol", t))
        assertTrue(matches("#holiday #fam", t))
        assertFalse(matches("#holiday #work", t))
        assertFalse(matches("#holiday", tx()))
    }

    @Test fun `words and a tag must both fit`() {
        val t = tx(note = "flights", tags = setOf("holiday"))
        assertTrue(matches("flights #holiday", t))
        assertFalse(matches("hotel #holiday", t))
    }

    @Test fun `an amount spec ignores sign and respects bounds`() {
        assertTrue(matches(">100", tx(amount = "-150.00")))
        assertFalse(matches(">100", tx(amount = "100.00")))
        assertTrue(matches("20-50", tx(amount = "50.00")))
        assertFalse(matches("20-50", tx(amount = "51.00")))
    }

    @Test fun `words combined with an amount need both`() {
        assertTrue(matches("rent 1850", tx(name = "rent", amount = "1850.00")))
        assertFalse(matches("rent 1850", tx(name = "rent", amount = "900.00")))
        assertFalse(matches("rent 1850", tx(name = "gym", amount = "1850.00")))
    }

    @Test fun `a lone number matches as amount or as part of a name`() {
        assertTrue(matches("49.99", tx(amount = "49.99")))
        assertTrue(matches("15", tx(name = "iPhone 15", amount = "999.00")))
        assertFalse(matches("15", tx(name = "iPhone", amount = "999.00")))
    }

    @Test fun `a year next to words is text not an amount`() {
        assertTrue(matches("rent 2026", tx(name = "rent 2026 deposit", amount = "10.00")))
    }

    @Test fun `filter keeps order and resolves categories by id`() {
        val rows = listOf(
            TransactionWithBalance(tx(id = 1, categoryId = 7), BigDecimal.ZERO, BigDecimal.ZERO),
            TransactionWithBalance(tx(id = 2, categoryId = 8), BigDecimal.ZERO, BigDecimal.ZERO),
            TransactionWithBalance(tx(id = 3, categoryId = 7), BigDecimal.ZERO, BigDecimal.ZERO)
        )
        val result = TransactionSearchMatcher.filter(
            SearchQueryParser.parse("food"), rows, mapOf(7L to "Food", 8L to "Travel")
        )
        assertEquals(listOf(1L, 3L), result.map { it.transaction.id })
    }

    private fun row(id: Long, note: String) = TransactionWithBalance(tx(id = id, note = note), BigDecimal.ZERO, BigDecimal.ZERO)

    @Test fun `select searches everything when the view has no filter of its own`() {
        val all = listOf(row(1, "laptop"), row(2, "laptop old"), row(3, "phone"))
        var viewScopedCalled = false
        val result = TransactionSearchMatcher.select(
            SearchQueryParser.parse("laptop"), scopedToFilter = false, all = all,
            viewScoped = { viewScopedCalled = true; all.take(1) }, categoryNames = emptyMap()
        )
        assertEquals(listOf(1L, 2L), result.map { it.transaction.id })
        assertFalse(viewScopedCalled)
    }

    @Test fun `select stays inside the view's filter when it has one`() {
        val all = listOf(row(1, "laptop"), row(2, "laptop old"), row(3, "phone"))
        val result = TransactionSearchMatcher.select(
            SearchQueryParser.parse("laptop"), scopedToFilter = true, all = all,
            viewScoped = { all.take(1) }, categoryNames = emptyMap()
        )
        assertEquals(listOf(1L), result.map { it.transaction.id })
    }

    @Test fun `select with no terms yet returns nothing`() {
        val all = listOf(row(1, "laptop"))
        assertTrue(
            TransactionSearchMatcher.select(null, scopedToFilter = false, all = all, viewScoped = { all }, categoryNames = emptyMap())
                .isEmpty()
        )
    }

    @Test fun `filter on an empty query returns nothing`() {
        val rows = listOf(TransactionWithBalance(tx(), BigDecimal.ZERO, BigDecimal.ZERO))
        assertTrue(TransactionSearchMatcher.filter(SearchQueryParser.parse(""), rows, emptyMap()).isEmpty())
    }
}
