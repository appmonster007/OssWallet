package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.CardDataComputer
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.model.settings.RecentActivitiesCardConfig
import com.osscabinet.ossWallet.domain.service.aggregation.CardAggregationService
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService

class RecentActivitiesCardComputer(
    private val queryService: DatabaseQueryService
) : CardDataComputer<RecentActivitiesCardConfig, CardRenderData.RecentActivities> {
    override val supportedType = CardType.RECENT_ACTIVITIES

    override suspend fun compute(input: CardInput, config: RecentActivitiesCardConfig): CardRenderData.RecentActivities {
        return CardAggregationService.computeRecentActivitiesCard(
            queryService,
            input.profileId,
            input,
            config
        )
    }
}
