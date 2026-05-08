package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.CardDataComputer
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig
import com.osscabinet.ossWallet.domain.service.aggregation.CardAggregationService
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService

class CurrencyBalancesCardComputer(
    private val queryService: DatabaseQueryService
) : CardDataComputer<CurrencyBalancesCardConfig, CardRenderData.CurrencyBalances> {
    override val supportedType = CardType.CURRENCY_BALANCES

    override suspend fun compute(input: CardInput, config: CurrencyBalancesCardConfig): CardRenderData.CurrencyBalances {
        return CardAggregationService.computeCurrencyBalancesCard(queryService, input.profileId, input, config)
    }
}
