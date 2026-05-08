package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.CardDataComputer
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig
import com.osscabinet.ossWallet.domain.service.aggregation.CardAggregationService
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService

class CashFlowCardComputer(
    private val queryService: DatabaseQueryService
) : CardDataComputer<CashFlowCardConfig, CardRenderData.CashFlow> {
    override val supportedType = CardType.CASH_FLOW

    override suspend fun compute(input: CardInput, config: CashFlowCardConfig): CardRenderData.CashFlow {
        return CardAggregationService.computeCashFlowCard(queryService, input.profileId, input, config)
    }
}
