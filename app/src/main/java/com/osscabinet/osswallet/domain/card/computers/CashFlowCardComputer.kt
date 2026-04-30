package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.analytics.*
import com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig
import com.osscabinet.ossWallet.domain.service.*
import java.math.RoundingMode

class CashFlowCardComputer : CardDataComputer<CashFlowCardConfig, CardRenderData.CashFlow> {
    override val supportedType = CardType.CASH_FLOW

    override fun compute(input: CardInput, config: CashFlowCardConfig): CardRenderData.CashFlow {
        val filtered = TransactionFilterService.filter(input.transactions, config.filter)
        
        val result = AggregationService.calculateCashFlowResult(
            transactions = filtered,
            baseCurrency = input.baseCurrency,
            exchangeRates = input.exchangeRates,
            precision = input.precision
        )
        
        return CardRenderData.CashFlow(config, result, input.baseCurrency, input.precision)
    }
}
