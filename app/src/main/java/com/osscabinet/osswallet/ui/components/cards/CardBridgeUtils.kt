package com.osscabinet.ossWallet.ui.components.cards

import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.resolveEffectiveFilter
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.merge
import com.osscabinet.ossWallet.domain.model.settings.CardConfig

/**
 * Shared helpers for bridging [CardConfig] and [CardRenderData] to [CardWithCallbacks].
 */

internal fun resolveDrillDown(
    input: CardInput,
    config: CardConfig,
    drillDown: TransactionFilter
): TransactionFilter {
    return resolveEffectiveFilter(
        input.filter,
        config.filter,
        config.filterRules,
        input.screenOwnsTimeline
    ).merge(drillDown)
}

internal fun resolveSeeAll(
    input: CardInput,
    config: CardConfig
): TransactionFilter {
    return resolveEffectiveFilter(
        input.filter,
        config.filter,
        config.filterRules,
        input.screenOwnsTimeline
    )
}
