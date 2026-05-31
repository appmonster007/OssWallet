package com.osscabinet.ossWallet.domain.card

import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.merge

/**
 * Resolves the effective filter for a card computation.
 * Priority: Base Card Rules (filterRules) > Inherited filter (inheritedFilter) > Card adhoc filter (cardFilter).
 *
 * Steps:
 * 1. Merge inherited (screen scope) with card's adhoc filter. Sets intersect when both sides non-empty.
 *    Timeline: if [screenOwnsTimeline] the inherited timeline always wins (even AllTime).
 *              Otherwise the inherited timeline only wins when it is not AllTime; card's timeline is the fallback.
 * 2. Normalize the merged result against the card's rules (forced values, allowed-set constraints).
 */
fun resolveEffectiveFilter(
    inheritedFilter: TransactionFilter,
    cardFilter: TransactionFilter,
    filterRules: CardFilterRules,
    screenOwnsTimeline: Boolean = false
): TransactionFilter {
    val candidate = if (screenOwnsTimeline) {
        // Screen timeline is authoritative (central filter behavior)
        inheritedFilter.merge(cardFilter).copy(timelineRange = inheritedFilter.timelineRange)
    } else {
        // Card timeline is authoritative if specified, otherwise inherits from screen.
        // TransactionFilter.merge(other) uses other.timelineRange, which is what we want.
        inheritedFilter.merge(cardFilter)
    }
    return filterRules.normalize(candidate)
}
