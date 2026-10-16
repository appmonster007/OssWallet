package com.osscabinet.ossWallet.domain.service.report

import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.ZoneId

class ReportCsvTest {
    private val zone = ZoneId.of("UTC")
    private val scope = ReportScope("R", "p", "a", "t", false)

    private fun row(
        name: String = "Coffee", amount: String = "-3.50", payee: String = "", note: String = "",
        tags: List<String> = emptyList(), category: String = "Food"
    ) = ReportRow(
        timestamp = 1_790_000_000_000L, name = name, category = category, account = "Main Bank",
        type = TransactionType.EXPENSE, amount = BigDecimal(amount), currency = "USD",
        payee = payee, tags = tags, note = note, status = TransactionStatus.CLEARED
    )

    private fun csv(vararg rows: ReportRow) =
        ReportCsv.render(ReportData(scope, 0, rows.toList(), emptyList(), emptyList()), zone)

    private fun lines(s: String) = s.removePrefix(ReportCsv.BOM).trimEnd().lines()

    @Test
    fun `starts with a BOM and the header`() {
        val out = csv()
        assertTrue(out.startsWith(ReportCsv.BOM))
        assertEquals(ReportCsv.HEADER.joinToString(","), lines(out).single())
    }

    @Test
    fun `a plain row has one cell per column and a numeric amount`() {
        val line = lines(csv(row()))[1]
        assertEquals("2026-09-21 14:13,Coffee,Food,Main Bank,Expense,-3.50,USD,,,,Cleared", line)
        assertEquals(ReportCsv.HEADER.size, line.split(",").size)
    }

    @Test
    fun `commas quotes and newlines are quoted and escaped`() {
        val out = csv(row(name = "Lunch, with \"Sam\"", note = "line1\nline2"))
        assertTrue(out.contains("\"Lunch, with \"\"Sam\"\"\""))
        assertTrue(out.contains("\"line1\nline2\""))
    }

    @Test
    fun `formula-looking text is neutralised but amounts are untouched`() {
        val out = csv(
            row(name = "=HYPERLINK(\"http://evil\")", payee = "+1234", note = "@cmd", amount = "-3.50"),
            row(name = "-lead", payee = "\tTab")
        )
        assertTrue(out.contains("\"'=HYPERLINK(\"\"http://evil\"\")\""))
        assertTrue(out.contains("'+1234"))
        assertTrue(out.contains("'@cmd"))
        assertTrue(out.contains("'-lead"))
        assertTrue("the signed amount must stay numeric", out.contains(",-3.50,USD,"))
        assertTrue(lines(out).all { l -> !l.split(",").any { it.startsWith("=") || it.startsWith("@") } })
    }

    @Test
    fun `tags are joined and non-ASCII text survives`() {
        val out = csv(row(name = "Café ¥500", tags = listOf("tax", "work")))
        assertTrue(out.contains("Café ¥500"))
        assertTrue(out.contains("tax; work"))
    }
}
