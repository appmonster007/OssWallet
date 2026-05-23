package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.CardDataComputer
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig
import com.osscabinet.ossWallet.domain.service.aggregation.CardAggregationService
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService

class NetWorthCardComputer(
    private val queryService: DatabaseQueryService
) : CardDataComputer<NetWorthCardConfig, CardRenderData.NetWorth> {
    override val supportedType = CardType.NET_WORTH

    override suspend fun compute(
        input: CardInput,
        config: NetWorthCardConfig
    ): CardRenderData.NetWorth {
        return CardAggregationService.computeNetWorthCard(
            queryService,
            input.profileId,
            input,
            config
        )
    }
}
