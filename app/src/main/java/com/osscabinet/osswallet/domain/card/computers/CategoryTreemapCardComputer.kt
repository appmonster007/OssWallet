package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.CardDataComputer
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.model.settings.CategoryTreemapCardConfig
import com.osscabinet.ossWallet.domain.service.aggregation.CardAggregationService
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService

class CategoryTreemapCardComputer(
    private val queryService: DatabaseQueryService
) : CardDataComputer<CategoryTreemapCardConfig, CardRenderData.CategoryTreemap> {
    override val supportedType = CardType.CATEGORY_TREEMAP

    override suspend fun compute(
        input: CardInput,
        config: CategoryTreemapCardConfig
    ): CardRenderData.CategoryTreemap {
        return CardAggregationService.computeCategoryTreemapCard(
            queryService,
            input.profileId,
            input,
            config
        )
    }
}
