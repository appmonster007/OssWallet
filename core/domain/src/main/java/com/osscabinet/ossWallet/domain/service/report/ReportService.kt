package com.osscabinet.ossWallet.domain.service.report

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.isTransfer
import com.osscabinet.ossWallet.domain.service.aggregation.AggregationService
import java.math.BigDecimal
import java.math.RoundingMode

/** Turns an already-filtered transaction list into the data both the PDF and CSV are drawn from. */
object ReportService {
    const val TOP_CATEGORIES_PER_CURRENCY = 8
    const val OUTSIDE_WALLET_LABEL = "Outside wallet"
    const val UNKNOWN_ACCOUNT_LABEL = "Unknown account"
    const val UNCATEGORIZED_LABEL = "Uncategorized"

    fun build(
        transactions: List<Transaction>,
        categories: List<Category>,
        accounts: List<Account>,
        scope: ReportScope,
        generatedAt: Long
    ): ReportData {
        val included = transactions
            .filter { scope.includesTransfers || !it.transactionType.isTransfer }
            .sortedWith(compareBy<Transaction> { it.timestamp }.thenBy { it.id })

        val categoryNames = categories.associate { it.id to it.name }
        val accountNames = accounts.associate { it.id to it.name }

        val rows = included.map { tx ->
            ReportRow(
                timestamp = tx.timestamp,
                name = tx.name,
                category = categoryNames[tx.categoryId] ?: UNCATEGORIZED_LABEL,
                account = when (val id = tx.accountId) {
                    null, "" -> OUTSIDE_WALLET_LABEL
                    else -> accountNames[id] ?: UNKNOWN_ACCOUNT_LABEL
                },
                type = tx.transactionType,
                amount = tx.amount,
                currency = tx.currency,
                payee = tx.payee,
                tags = tx.tags.sorted(),
                note = tx.note,
                status = tx.status
            )
        }

        val totals = AggregationService.calculateSummaryByCurrency(included)
            .map { (currency, s) ->
                ReportCurrencyTotals(currency, s.income, s.expense, s.transferIn, s.transferOut, s.transactionCount)
            }
            .sortedBy { it.currency }

        return ReportData(scope, generatedAt, rows, totals, topExpenseCategories(rows))
    }

    private fun topExpenseCategories(rows: List<ReportRow>): List<ReportCategoryTotal> =
        rows.filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.currency }
            .toSortedMap()
            .flatMap { (currency, list) ->
                val spent = list.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.amount.abs()) }
                if (spent.signum() == 0) return@flatMap emptyList()
                list.groupBy { it.category }
                    .map { (category, items) ->
                        val sum = items.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.amount.abs()) }
                        ReportCategoryTotal(
                            category, currency, sum,
                            sum.divide(spent, 6, RoundingMode.HALF_UP).toDouble()
                        )
                    }
                    .sortedByDescending { it.amount }
                    .take(TOP_CATEGORIES_PER_CURRENCY)
            }
}
