package com.osscabinet.ossWallet.domain.service.ledger

import java.math.BigDecimal

/**
 * A single row to fold into a running balance.
 */
data class LedgerRow(
    val id: Long,
    val timestamp: Long,
    val amount: BigDecimal
)

/**
 * The balance immediately before and after row [id] was applied.
 */
data class LedgerEntry(
    val id: Long,
    val balanceBefore: BigDecimal,
    val balanceAfter: BigDecimal
)

/**
 * The single place a running balance is ever computed. [rebuild] takes rows in any order, sorts
 * them deterministically by (timestamp, id), and folds [startingBalance] forward across all of
 * them. Callers must pass every row relevant to the ledger being computed — display filters
 * (category, tag, status, currency, ...) must be applied to the *result*, never to the input,
 * or every balance after an excluded row is wrong.
 */
object LedgerService {
    fun rebuild(rows: List<LedgerRow>, startingBalance: BigDecimal = BigDecimal.ZERO): List<LedgerEntry> {
        val ordered = rows.sortedWith(compareBy({ it.timestamp }, { it.id }))
        var running = startingBalance
        return ordered.map { row ->
            val before = running
            running = running.add(row.amount)
            LedgerEntry(row.id, before, running)
        }
    }
}
