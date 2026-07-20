package com.osscabinet.ossWallet.domain.card

import com.osscabinet.ossWallet.domain.model.settings.CardConfig
import com.osscabinet.ossWallet.domain.card.CardRenderData

/**
 * Strategy interface for computing card data.
 * Every card is a function from CardInput -> CardRenderData.
 * DB-optimized computers inject DatabaseQueryService at construction time.
 */
interface CardDataComputer<in C : CardConfig, out D : CardRenderData> {
    val supportedType: CardType

    suspend fun compute(input: CardInput, config: C): D
}
