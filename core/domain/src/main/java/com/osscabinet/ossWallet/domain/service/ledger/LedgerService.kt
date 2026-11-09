package com.osscabinet.ossWallet.domain.service.ledger

import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * A single row to fold into a running balance. Rows with the same [timestamp] are applied in
 * [tieBreaker] order, which defaults to [id].
 */
data class LedgerRow(
    val id: Long,
    val timestamp: Long,
    val amount: BigDecimal,
    val tieBreaker: Long = id
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
    /**
     * Running balances for [transactions], one independent ledger per account. Each account starts
     * from its entry in [startingBalances] (zero when absent), amounts are brought into the
     * account's currency by [amountInAccountCurrency], and balances are rounded to [scale] places.
     * Rows come back grouped by account, ordered within it; sort them for display.
     */
    fun withBalances(
        transactions: List<Transaction>,
        startingBalances: Map<String, BigDecimal>,
        scale: Int,
        amountInAccountCurrency: (Transaction) -> BigDecimal
    ): List<TransactionWithBalance> {
        // Rows are keyed by their position, not their id: unsaved or projected transactions can share
        // an id (0), and keying by it would fold them into one.
        return transactions
            .withIndex()
            .groupBy { it.value.accountId }
            .flatMap { (accountId, rows) ->
                val ledgerRows = rows.map { (index, tx) ->
                    LedgerRow(index.toLong(), tx.timestamp, amountInAccountCurrency(tx), tieBreaker = tx.id)
                }
                rebuild(ledgerRows, startingBalances[accountId ?: ""] ?: BigDecimal.ZERO)
            }
            .map { entry ->
                TransactionWithBalance(
                    transaction = transactions[entry.id.toInt()],
                    balanceBefore = entry.balanceBefore.setScale(scale, RoundingMode.HALF_UP),
                    balanceAfter = entry.balanceAfter.setScale(scale, RoundingMode.HALF_UP)
                )
            }
    }

    fun rebuild(rows: List<LedgerRow>, startingBalance: BigDecimal = BigDecimal.ZERO): List<LedgerEntry> {
        val ordered = rows.sortedWith(compareBy({ it.timestamp }, { it.tieBreaker }, { it.id }))
        var running = startingBalance
        return ordered.map { row ->
            val before = running
            running = running.add(row.amount)
            LedgerEntry(row.id, before, running)
        }
    }
}
