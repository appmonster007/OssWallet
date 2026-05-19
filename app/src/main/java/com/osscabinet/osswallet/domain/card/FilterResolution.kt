package com.osscabinet.ossWallet.domain.card

import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.merge

/**
 * Resolves the effective filter for a card computation.
 * Priority: Base Card Rules (filterRules) > Inherited filter (inheritedFilter) > Card adhoc filter (cardFilter).
 *
 * Steps:
 * 1. Merge inherited (screen scope) with card's adhoc filter. Sets intersect when both sides non-empty.
 * 2. Normalize the merged result against the card's rules (forced values, allowed-set constraints).
 */
fun resolveEffectiveFilter(
    inheritedFilter: TransactionFilter,
    cardFilter: TransactionFilter,
    filterRules: CardFilterRules
): TransactionFilter {
    val candidate = inheritedFilter.merge(cardFilter)
    return filterRules.normalize(candidate)
}
