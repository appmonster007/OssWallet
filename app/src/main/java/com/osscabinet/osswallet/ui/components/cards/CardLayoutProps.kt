package com.osscabinet.ossWallet.ui.components.cards

import androidx.compose.ui.Modifier
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.model.TransactionFilter

/**
 * Bundles shared layout and filter parameters that flow through [UnifiedCardRenderer]
 * to all individual card components.
 */
data class CardLayoutProps(
    val modifier: Modifier = Modifier,
    val headerModifier: Modifier = Modifier,
    val filterContext: FilterSelectionContext = FilterSelectionContext(),
    val inheritedFilter: TransactionFilter = TransactionFilter(),
    val baseFilter: TransactionFilter = TransactionFilter(),
    val filterRules: CardFilterRules? = null,
    val screenOwnsTimeline: Boolean = false,
    val showFilterButton: Boolean = true,
    val animate: Boolean = false,
    val scrollToId: String? = null,
    val highlightedId: String? = null,
    val onScrollComplete: () -> Unit = {}
) {
    /**
     * Creates a copy of these props with overrides from a specific [CardWithCallbacks].
     */
    fun applyCardOverrides(card: CardWithCallbacks) = copy(
        baseFilter = card.baseFilter,
        filterRules = card.filterRules,
        showFilterButton = card.filterSheetEnabled
    )

    /**
     * Reconciles the card's saved filter with inherited timeline constraints from the screen.
     */
    fun computeDisplayFilter(configFilter: TransactionFilter): TransactionFilter {
        return if (screenOwnsTimeline) {
            configFilter.copy(timelineRange = inheritedFilter.timelineRange)
        } else {
            configFilter
        }
    }
}
