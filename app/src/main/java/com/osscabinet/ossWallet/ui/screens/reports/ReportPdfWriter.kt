package com.osscabinet.ossWallet.ui.screens.reports

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.osscabinet.ossWallet.domain.service.report.ReportData
import com.osscabinet.ossWallet.domain.service.report.ReportRow
import java.io.OutputStream
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

/** Draws a [ReportData] as a paginated A4 PDF: summary, top spending categories, then every transaction. */
object ReportPdfWriter {
    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 36f
    private const val ROW_H = 15f
    private val CONTENT_W = PAGE_W - 2 * MARGIN

    private val INK = Color.rgb(0x1C, 0x1B, 0x1F)
    private val MUTED = Color.rgb(0x6B, 0x68, 0x70)
    private val RULE = Color.rgb(0xD8, 0xD4, 0xDC)
    private val BAND = Color.rgb(0xF4, 0xF2, 0xF6)
    private val NEGATIVE = Color.rgb(0xB3, 0x26, 0x1E)
    private val POSITIVE = Color.rgb(0x2E, 0x7D, 0x32)
    private val ACCENT = Color.rgb(0x8A, 0x6F, 0x3C)

    private val DATE = DateTimeFormatter.ofPattern("d MMM yyyy")
    private val STAMP = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")

    private fun paint(size: Float, bold: Boolean = false, color: Int = INK, align: Paint.Align = Paint.Align.LEFT) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            textAlign = align
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

    private val title = paint(20f, bold = true)
    private val h2 = paint(12f, bold = true)
    private val body = paint(9f)
    private val bodyBold = paint(9f, bold = true)
    private val small = paint(8f, color = MUTED)
    private val smallRight = paint(8f, color = MUTED, align = Paint.Align.RIGHT)
    private val rule = Paint().apply { color = RULE; strokeWidth = 0.6f }
    private val band = Paint().apply { color = BAND }
    private val bar = Paint().apply { color = ACCENT }

    // Table columns: x offset from the left margin and width.
    private val colDate = 0f to 64f
    private val colDesc = 70f to 168f
    private val colCat = 244f to 100f
    private val colAcct = 350f to 82f
    private val colAmt = CONTENT_W to 0f // right-aligned at the content edge

    fun write(data: ReportData, out: OutputStream, zone: ZoneId = ZoneId.systemDefault()) {
        val doc = PdfDocument()
        try {
            Pager(doc).render(data, zone)
            doc.writeTo(out)
        } finally {
            doc.close()
        }
    }

    private class Pager(private val doc: PdfDocument) {
        private var number = 0
        private lateinit var page: PdfDocument.Page
        private lateinit var canvas: Canvas
        var y = MARGIN

        fun newPage() {
            if (number > 0) finishPage()
            number++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, number).create())
            canvas = page.canvas
            y = MARGIN
        }

        private fun finishPage() {
            canvas.drawText("OssWallet", MARGIN, PAGE_H - 20f, small)
            canvas.drawText("Page $number", PAGE_W - MARGIN, PAGE_H - 20f, smallRight)
            doc.finishPage(page)
        }

        fun close() = finishPage()

        fun ensure(height: Float, onNewPage: () -> Unit = {}) {
            if (y + height > PAGE_H - 36f) {
                newPage()
                onNewPage()
            }
        }

        fun render(data: ReportData, zone: ZoneId) {
            newPage()
            canvas.drawText(data.scope.title, MARGIN, y + 20f, title); y += 30f
            val lines = listOf(
                "Period: ${data.scope.periodLabel}${data.scope.periodRangeText.takeIf { it.isNotEmpty() }?.let { " ($it)" }.orEmpty()}",
                "Accounts: ${data.scope.accountsLabel}",
                "Categories: ${data.scope.categoriesLabel}",
                "Tags: ${data.scope.tagsLabel}",
                "Showing: ${data.scope.typesLabel}",
                "Transfers: ${if (data.scope.includesTransfers) "included" else "excluded"}",
                "Generated ${STAMP.format(Instant.ofEpochMilli(data.generatedAt).atZone(zone))}"
            )
            lines.forEach { line ->
                canvas.drawText(ellipsized(line, small, CONTENT_W), MARGIN, y + 9f, small)
                y += 12f
            }
            y += 8f; canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, rule); y += 16f

            canvas.drawText("Summary", MARGIN, y, h2); y += 16f
            data.totals.forEach { t ->
                ensure(34f)
                canvas.drawText("${t.currency}  ·  ${t.count} transactions", MARGIN, y, bodyBold); y += 13f
                val cells = listOf(
                    "Income" to money(t.income, t.currency, signed = true),
                    "Expenses" to money(t.expense, t.currency, signed = true),
                    "Net" to money(t.net, t.currency, signed = true)
                )
                val w = CONTENT_W / cells.size
                cells.forEachIndexed { i, (label, value) ->
                    canvas.drawText(label, MARGIN + i * w, y, small)
                    canvas.drawText(value, MARGIN + i * w, y + 12f, bodyBold)
                }
                y += 26f
            }

            if (data.topExpenseCategories.isNotEmpty()) {
                y += 4f
                ensure(30f)
                canvas.drawText("Where the money went", MARGIN, y, h2); y += 16f
                var lastCurrency: String? = null
                data.topExpenseCategories.forEach { cat ->
                    // Categories are per currency; label each group so repeated names aren't ambiguous.
                    if (cat.currency != lastCurrency) {
                        ensure(30f)
                        if (lastCurrency != null) y += 4f
                        canvas.drawText(cat.currency, MARGIN, y, bodyBold); y += 14f
                        lastCurrency = cat.currency
                    }
                    ensure(16f)
                    canvas.drawText(ellipsized(cat.category, body, 150f), MARGIN, y, body)
                    val barMax = 200f
                    canvas.drawRect(MARGIN + 160f, y - 8f, MARGIN + 160f + barMax * cat.share.toFloat().coerceIn(0f, 1f), y + 1f, bar)
                    val pct = "%.0f%%".format(Locale.US, cat.share * 100)
                    canvas.drawText(
                        "${money(cat.amount, cat.currency)}  ($pct)",
                        PAGE_W - MARGIN, y, paint(9f, align = Paint.Align.RIGHT)
                    )
                    y += 15f
                }
            }

            y += 10f
            ensure(60f)
            canvas.drawText("Transactions", MARGIN, y, h2); y += 14f
            tableHeader()
            data.rows.forEachIndexed { i, row ->
                ensure(ROW_H) { tableHeader() }
                tableRow(row, i % 2 == 1, zone)
            }
            close()
        }

        private fun tableHeader() {
            canvas.drawRect(MARGIN, y - 10f, PAGE_W - MARGIN, y + 4f, band)
            canvas.drawText("Date", MARGIN + colDate.first, y, bodyBold)
            canvas.drawText("Description", MARGIN + colDesc.first, y, bodyBold)
            canvas.drawText("Category", MARGIN + colCat.first, y, bodyBold)
            canvas.drawText("Account", MARGIN + colAcct.first, y, bodyBold)
            canvas.drawText("Amount", MARGIN + colAmt.first, y, paint(9f, bold = true, align = Paint.Align.RIGHT))
            y += ROW_H
        }

        private fun tableRow(r: ReportRow, shaded: Boolean, zone: ZoneId) {
            if (shaded) canvas.drawRect(MARGIN, y - 10f, PAGE_W - MARGIN, y + 4f, band)
            canvas.drawText(DATE.format(Instant.ofEpochMilli(r.timestamp).atZone(zone)), MARGIN + colDate.first, y, body)
            canvas.drawText(ellipsized(r.name, body, colDesc.second), MARGIN + colDesc.first, y, body)
            canvas.drawText(ellipsized(r.category, body, colCat.second), MARGIN + colCat.first, y, body)
            canvas.drawText(ellipsized(r.account, body, colAcct.second), MARGIN + colAcct.first, y, body)
            val color = when {
                r.amount.signum() < 0 -> NEGATIVE
                r.amount.signum() > 0 -> POSITIVE
                else -> INK
            }
            canvas.drawText(
                money(r.amount, r.currency, signed = true), MARGIN + colAmt.first, y,
                paint(9f, color = color, align = Paint.Align.RIGHT)
            )
            y += ROW_H
        }
    }

    /** Cuts [text] with an ellipsis so it fits [maxWidth] points; never draws outside its column. */
    internal fun ellipsized(text: String, paint: Paint, maxWidth: Float): String {
        val single = text.replace('\n', ' ')
        if (paint.measureText(single) <= maxWidth) return single
        val ellipsis = "…"
        val fit = paint.breakText(single, true, maxWidth - paint.measureText(ellipsis), null)
        return single.take(fit).trimEnd() + ellipsis
    }

    /** "-1,234.50 USD", using the currency's own number of decimal places (JPY has none). */
    internal fun money(amount: BigDecimal, currency: String, signed: Boolean = false): String {
        val digits = runCatching { Currency.getInstance(currency).defaultFractionDigits }.getOrDefault(2).coerceAtLeast(0)
        val fmt = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            minimumFractionDigits = digits
            maximumFractionDigits = digits
        }
        val text = fmt.format(amount.abs())
        val sign = when {
            amount.signum() < 0 -> "-"
            signed && amount.signum() > 0 -> "+"
            else -> ""
        }
        return "$sign$text $currency"
    }
}
