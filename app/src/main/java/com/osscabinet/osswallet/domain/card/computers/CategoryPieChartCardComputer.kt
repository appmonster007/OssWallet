package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.CardDataComputer
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.model.settings.CategoryPieChartCardConfig
import com.osscabinet.ossWallet.domain.service.aggregation.CardAggregationService
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService

class CategoryPieChartCardComputer(
    private val queryService: DatabaseQueryService
) : CardDataComputer<CategoryPieChartCardConfig, CardRenderData.CategoryPieChart> {
    override val supportedType = CardType.CATEGORY_PIE_CHART

    override suspend fun compute(
        input: CardInput,
        config: CategoryPieChartCardConfig
    ): CardRenderData.CategoryPieChart {
        return CardAggregationService.computeCategoryPieChartCard(
            queryService,
            input.profileId,
            input,
            config
        )
    }
}
