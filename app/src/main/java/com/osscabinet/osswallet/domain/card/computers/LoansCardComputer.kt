package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.CardDataComputer
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.model.settings.LoansCardConfig
import com.osscabinet.ossWallet.domain.service.aggregation.CardAggregationService
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService

class LoansCardComputer(
    private val queryService: DatabaseQueryService
) : CardDataComputer<LoansCardConfig, CardRenderData.Loans> {
    override val supportedType = CardType.LOANS

    override suspend fun compute(input: CardInput, config: LoansCardConfig): CardRenderData.Loans {
        return CardAggregationService.computeLoansCard(
            queryService,
            input.profileId,
            input,
            config
        )
    }
}
