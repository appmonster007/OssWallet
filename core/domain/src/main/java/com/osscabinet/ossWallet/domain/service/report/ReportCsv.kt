package com.osscabinet.ossWallet.domain.service.report

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** RFC 4180 CSV of the report's rows, safe to open in a spreadsheet. */
object ReportCsv {
    val HEADER = listOf(
        "Date", "Description", "Category", "Account", "Type", "Amount", "Currency",
        "Payee", "Tags", "Note", "Status"
    )

    private val DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    /** Excel needs a BOM to read UTF-8 (accents, currency symbols) correctly. */
    const val BOM = "﻿"

    fun render(data: ReportData, zone: ZoneId = ZoneId.systemDefault()): String = buildString {
        append(BOM)
        appendLine(HEADER.joinToString(",") { quote(it) })
        data.rows.forEach { r ->
            val cells = listOf(
                text(DATE_TIME.format(Instant.ofEpochMilli(r.timestamp).atZone(zone))),
                text(r.name),
                text(r.category),
                text(r.account),
                text(r.type.displayName),
                r.amount.toPlainString(), // numeric: a leading '-' is a sign, not a formula
                text(r.currency),
                text(r.payee),
                text(r.tags.joinToString("; ")),
                text(r.note),
                text(r.status.displayName)
            )
            appendLine(cells.joinToString(",") { quote(it) })
        }
    }

    /**
     * Spreadsheets run cells starting with = + - @ (or a tab/CR) as formulas, so a payee of
     * `=HYPERLINK(...)` in an exported file could do harm. Prefixing a quote makes it plain text.
     */
    internal fun text(value: String): String =
        if (value.isNotEmpty() && value[0] in FORMULA_TRIGGERS) "'$value" else value

    private val FORMULA_TRIGGERS = charArrayOf('=', '+', '-', '@', '\t', '\r')

    private fun quote(cell: String): String =
        if (cell.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + cell.replace("\"", "\"\"") + "\"" else cell
}
