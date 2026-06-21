package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.CardDataComputer
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.model.settings.BudgetsCardConfig
import com.osscabinet.ossWallet.domain.service.aggregation.CardAggregationService
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService

class BudgetsCardComputer(
    private val queryService: DatabaseQueryService
) : CardDataComputer<BudgetsCardConfig, CardRenderData.Budgets> {
    override val supportedType = CardType.BUDGETS

    override suspend fun compute(
        input: CardInput,
        config: BudgetsCardConfig
    ): CardRenderData.Budgets {
        return CardAggregationService.computeBudgetsCard(
            queryService,
            input.profileId,
            input,
            config
        )
    }
}
