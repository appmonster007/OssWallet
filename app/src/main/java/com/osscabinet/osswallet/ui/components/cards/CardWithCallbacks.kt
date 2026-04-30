package com.osscabinet.ossWallet.ui.components.cards

import com.osscabinet.ossWallet.domain.card.CardRenderData

/**
 * Data class that pairs card render data with its callbacks
 */
data class CardWithCallbacks(
    val renderData: CardRenderData,
    val callbacks: CardCallbacks
)
