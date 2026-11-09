package com.osscabinet.ossWallet.domain.service.search

import com.osscabinet.ossWallet.domain.model.Transaction

/**
 * Narrowing of database candidates for a transaction search. The in-memory counterpart used by Records is
 * [TransactionSearchMatcher].
 */
object GlobalSearchService {

    /** Post-filter for database candidates: the `#tags` and amount parts, which SQL only approximates. */
    fun matchesTransaction(parsed: ParsedSearch, tx: Transaction): Boolean {
        if (parsed.tags.isNotEmpty()) {
            val lower = tx.tags.map { it.lowercase() }
            if (!parsed.tags.all { wanted -> lower.any { it.startsWith(wanted) } }) return false
        }
        val spec = parsed.amount
        // A lone number was also searched as text, so a text hit alone is enough there.
        if (spec != null && !parsed.numberOnly && !spec.matches(tx.amount)) return false
        return true
    }
}
