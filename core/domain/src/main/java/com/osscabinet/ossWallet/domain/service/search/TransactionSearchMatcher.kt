package com.osscabinet.ossWallet.domain.service.search

import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance

/**
 * In-memory counterpart of the database search, for lists that already hold every transaction (Records).
 *
 * Same rules as [GlobalSearchService.matchesTransaction] plus the text part the full-text index handles in
 * SQL: every word must appear in the name, payee, note or category name, `#tags` match by prefix, and an
 * amount spec must fit (a lone number counts as either text or amount).
 */
object TransactionSearchMatcher {

    fun matches(parsed: ParsedSearch, tx: Transaction, categoryName: String?): Boolean {
        if (parsed.isEmpty) return false

        if (parsed.tags.isNotEmpty()) {
            val lower = tx.tags.map { it.lowercase() }
            if (!parsed.tags.all { wanted -> lower.any { it.startsWith(wanted) } }) return false
        }

        val spec = parsed.amount
        val amountHit = spec != null && spec.matches(tx.amount)
        val textHit = parsed.words.isEmpty() || textMatches(parsed.words, tx, categoryName)

        return if (parsed.numberOnly) {
            // "15" may be part of a name ("iPhone 15") or an amount; either is enough.
            textHit || amountHit
        } else {
            textHit && (spec == null || amountHit)
        }
    }

    /** Keeps the matching rows in their original order. */
    fun filter(
        parsed: ParsedSearch,
        transactions: List<TransactionWithBalance>,
        categoryNames: Map<Long, String>
    ): List<TransactionWithBalance> {
        if (parsed.isEmpty) return emptyList()
        return transactions.filter { matches(parsed, it.transaction, categoryNames[it.transaction.categoryId]) }
    }

    private fun textMatches(words: List<String>, tx: Transaction, categoryName: String?): Boolean {
        val fields = listOf(tx.name, tx.payee, tx.note, categoryName)
        return words.all { word -> fields.any { it?.contains(word, ignoreCase = true) == true } }
    }
}
