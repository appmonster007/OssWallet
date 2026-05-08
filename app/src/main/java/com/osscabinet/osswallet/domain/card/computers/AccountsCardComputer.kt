package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.CardDataComputer
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.model.settings.AccountsCardConfig
import com.osscabinet.ossWallet.domain.service.aggregation.CardAggregationService
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService

class AccountsCardComputer(
    private val queryService: DatabaseQueryService
) : CardDataComputer<AccountsCardConfig, CardRenderData.Accounts> {
    override val supportedType = CardType.ACCOUNTS

    override suspend fun compute(input: CardInput, config: AccountsCardConfig): CardRenderData.Accounts {
        return CardAggregationService.computeAccountsCard(queryService, input.profileId, input, config)
    }
}
