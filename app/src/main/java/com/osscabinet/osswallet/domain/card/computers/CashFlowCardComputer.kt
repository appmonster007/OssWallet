package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.CardDataComputer
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig
import com.osscabinet.ossWallet.domain.service.aggregation.CardAggregationService
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService

class CashFlowCardComputer(
    private val queryService: DatabaseQueryService
) : CardDataComputer<CashFlowCardConfig, CardRenderData.CashFlow> {
    override val supportedType = CardType.CASH_FLOW

    override suspend fun compute(
        input: CardInput,
        config: CashFlowCardConfig
    ): CardRenderData.CashFlow {
        // Normalize: empty transactionTypes on the cash-flow filter means "show nothing"
        // in the new tabular UI, but legacy configs serialized before this feature was
        // added have empty sets meaning "show all". Preserve their effective view by
        // injecting INCOME + EXPENSE when empty — matches the old 2-column behaviour.
        val effectiveConfig = if (config.filter.transactionTypes.isEmpty()) {
            config.copy(filter = config.filter.copy(transactionTypes = setOf(TransactionType.INCOME, TransactionType.EXPENSE)))
        } else config
        return CardAggregationService.computeCashFlowCard(
            queryService,
            input.profileId,
            input,
            effectiveConfig
        )
    }
}
