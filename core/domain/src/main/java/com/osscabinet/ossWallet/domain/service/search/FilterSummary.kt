package com.osscabinet.ossWallet.domain.service.search

import com.osscabinet.ossWallet.domain.model.TransactionFilter

/** A one-line description of what a saved filter selects, for lists ("This Month · Expenses · #holiday · over 100"). */
object FilterSummary {
    fun describe(f: TransactionFilter): String = buildList {
        add(f.timelineRange.toDisplayString())
        if (f.transactionTypes.isNotEmpty()) add(f.transactionTypes.joinToString("/") { it.displayName })
        if (f.accountIds.isNotEmpty()) add(count(f.accountIds.size, "account"))
        if (f.categoryIds.isNotEmpty()) add(count(f.categoryIds.size, "category", "categories"))
        if (f.tags.isNotEmpty()) add(f.tags.sorted().joinToString(if (f.matchAllTags) " + " else " or ") { "#$it" })
        f.queryNote?.takeIf { it.isNotBlank() }?.let { add("note “$it”") }
        f.queryPayee?.takeIf { it.isNotBlank() }?.let { add("payee “$it”") }
        when {
            f.minAmount != null && f.maxAmount != null -> add("${f.minAmount!!.stripTrailingZeros().toPlainString()}–${f.maxAmount!!.stripTrailingZeros().toPlainString()}")
            f.minAmount != null -> add("over ${f.minAmount!!.stripTrailingZeros().toPlainString()}")
            f.maxAmount != null -> add("under ${f.maxAmount!!.stripTrailingZeros().toPlainString()}")
        }
        if (f.currencies.isNotEmpty()) add(f.currencies.sorted().joinToString("/"))
        if (f.statuses.isNotEmpty()) add(f.statuses.joinToString("/") { it.displayName })
        if (f.paymentMethods.isNotEmpty()) add(count(f.paymentMethods.size, "payment method"))
    }.joinToString(" · ")

    private fun count(n: Int, one: String, many: String = one + "s") = if (n == 1) "1 $one" else "$n $many"
}
