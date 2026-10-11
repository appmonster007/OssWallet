package com.osscabinet.ossWallet.data.local.query

import androidx.sqlite.db.SupportSQLiteProgram
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/**
 * SQLite can only bind String / Long / Int / Double / Float / ByteArray. A criterion that adds
 * anything else (BigDecimal, an enum) makes the whole query throw, and card totals then silently
 * fall back to zero. Every criterion is exercised together here.
 */
class TransactionQueryBuilderBindingTest {

    private val everything = TransactionFilter(
        categoryIds = setOf(1L, 2L),
        accountIds = setOf("a1"),
        transactionTypes = setOf(TransactionType.EXPENSE),
        currencies = setOf("USD"),
        minAmount = BigDecimal("10.50"),
        maxAmount = BigDecimal("500"),
        paymentMethods = setOf(PaymentMethod.CASH, PaymentMethod.BANK_TRANSFER),
        hasWarranty = true,
        warrantyExpirationStart = 1L,
        warrantyExpirationEnd = 2L,
        tags = setOf("Food", "Social"),
        matchAllTags = true,
        queryNote = "50%",
        queryPayee = "Star_bucks",
        statuses = setOf(TransactionStatus.CLEARED),
        linkedTransactionIds = setOf(5L, 6L)
    )

    @Test
    fun `every criterion binds`() {
        val query = TransactionQueryBuilder().buildListQuery(1, everything)
        query.bindTo(mockk<SupportSQLiteProgram>(relaxed = true))
    }

    @Test
    fun `match all tags ands the tags and match any ors them`() {
        val and = TransactionQueryBuilder().buildListQuery(1, everything).sql
        val or = TransactionQueryBuilder().buildListQuery(1, everything.copy(matchAllTags = false)).sql
        assertTrue(and.contains("instr(tags, ?) > 0 AND instr(tags, ?) > 0"))
        assertTrue(or.contains("instr(tags, ?) > 0 OR instr(tags, ?) > 0"))
    }

    @Test
    fun `regex criteria are left out of the sql text conditions`() {
        val sql = TransactionQueryBuilder().buildListQuery(
            1, TransactionFilter(queryPayee = "^Star", isPayeeRegex = true, queryNote = "x+", isNoteRegex = true)
        ).sql
        assertFalse(sql.contains("payee LIKE"))
        assertFalse(sql.contains("note LIKE"))
    }

    @Test
    fun `id restriction is applied and an empty set matches nothing`() {
        val builder = TransactionQueryBuilder()
        assertTrue(builder.buildListQuery(1, TransactionFilter(), restrictToIds = setOf(3L, 4L)).sql.contains("id IN (3,4)"))
        assertTrue(builder.buildListQuery(1, TransactionFilter(), restrictToIds = emptySet()).sql.contains("AND 0"))
    }
}
