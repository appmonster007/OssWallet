package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.analytics.*
import com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig
import com.osscabinet.ossWallet.domain.service.*

class CashFlowCardComputer : CardDataComputer<CashFlowCardConfig, CardRenderData.CashFlow> {
    override val supportedType = CardType.CASH_FLOW

    override fun compute(input: CardInput, config: CashFlowCardConfig): CardRenderData.CashFlow {
        // Step 1: Filter
        val baseTx = TransactionFilterService.filter(input.transactions, input.filter)
        val filteredTx = TransactionFilterService.filter(baseTx, config.filter)

        // Step 2: Compute
        val result = AggregationService.calculateCashFlowResult(
            transactions = filteredTx,
            baseCurrency = input.baseCurrency,
            exchangeRates = input.exchangeRates,
            precision = input.precision
        )

        // Step 3: Build render data
        return CardRenderData.CashFlow(config, result, input.baseCurrency, input.precision)
    }
}
