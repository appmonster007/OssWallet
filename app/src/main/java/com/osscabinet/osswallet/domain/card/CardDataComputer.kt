package com.osscabinet.ossWallet.domain.card

import com.osscabinet.ossWallet.domain.model.settings.CardConfig
import com.osscabinet.ossWallet.domain.card.CardRenderData

/**
 * Strategy interface for computing card data.
 * Every card is a pure function from CardInput -> CardRenderData.
 */
interface CardDataComputer<in C : CardConfig, out D : CardRenderData> {
    val supportedType: CardType
    fun compute(input: CardInput, config: C): D
}
