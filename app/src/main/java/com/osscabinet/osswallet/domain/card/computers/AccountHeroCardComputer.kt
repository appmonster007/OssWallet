package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.CardDataComputer
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.model.settings.AccountHeroCardConfig
import com.osscabinet.ossWallet.domain.service.aggregation.CardAggregationService
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService

class AccountHeroCardComputer(
    private val queryService: DatabaseQueryService
) : CardDataComputer<AccountHeroCardConfig, CardRenderData.AccountHero> {
    override val supportedType = CardType.ACCOUNT_HERO

    override suspend fun compute(input: CardInput, config: AccountHeroCardConfig): CardRenderData.AccountHero {
        return CardAggregationService.computeAccountHeroCard(queryService, input.profileId, input, config)
    }
}
