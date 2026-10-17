package com.osscabinet.ossWallet.domain.service.search

import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import com.osscabinet.ossWallet.domain.service.filter.TransactionFilterService
import jakarta.inject.Inject
import jakarta.inject.Singleton

/** Text/tag/amount search over transactions, shared by the Search screen and the assistant functions. */
@Singleton
class TransactionSearcher @Inject constructor(
    private val transactions: TransactionRepository
) {
    /**
     * Newest-first matches for [query], narrowed by [extra] (period, account, type, a saved filter...) when given.
     * The database returns at most [candidateLimit] candidates; the tag/amount rules and [extra] are applied after.
     */
    suspend fun search(
        profileId: Int,
        query: ParsedSearch,
        extra: TransactionFilter? = null,
        candidateLimit: Int = DEFAULT_CANDIDATE_LIMIT
    ): List<Transaction> {
        val matched = transactions.searchForGlobal(profileId, query, candidateLimit)
            .filter { GlobalSearchService.matchesTransaction(query, it) }
        return if (extra != null) TransactionFilterService.filterTransactions(matched, extra) else matched
    }

    companion object {
        const val DEFAULT_CANDIDATE_LIMIT = 1000
    }
}
