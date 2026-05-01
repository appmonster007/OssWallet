package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.analytics.*
import com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig
import com.osscabinet.ossWallet.domain.service.*

class CashFlowCardComputer : CardDataComputer<CashFlowCardConfig, CardRenderData.CashFlow> {
    override val supportedType = CardType.CASH_FLOW

    override fun compute(input: CardInput, config: CashFlowCardConfig): CardRenderData.CashFlow {
        // Two-step filtering
        val baseTx = TransactionFilterService.filter(input.transactions, input.filter)
        val filtered = TransactionFilterService.filter(baseTx, config.filter)
        
        val result = AggregationService.calculateCashFlowResult(
            transactions = filtered,
            baseCurrency = input.baseCurrency,
            exchangeRates = input.exchangeRates,
            precision = input.precision
        )
        
        return CardRenderData.CashFlow(config, result, input.baseCurrency, input.precision)
    }
}
