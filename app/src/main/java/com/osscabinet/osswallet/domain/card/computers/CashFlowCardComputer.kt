package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.analytics.*
import com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig
import com.osscabinet.ossWallet.domain.service.*

class CashFlowCardComputer : CardDataComputer<CashFlowCardConfig, CardRenderData.CashFlow> {
    override val supportedType = CardType.CASH_FLOW

    override fun compute(input: CardInput, config: CashFlowCardConfig): CardRenderData.CashFlow {
        // Step 1: Apply base scope filter (screen-level: profile, account group, etc.)
        val baseTx = TransactionFilterService.filter(input.transactions, input.filter)

        // Step 2: Apply card config filter (user-selected: timeline, accounts, categories)
        val filteredTx = TransactionFilterService.filter(baseTx, config.filter)

        // Step 3: Compute all values from filteredTx
        val result = AggregationService.calculateCashFlowResult(
            transactions = filteredTx,
            baseCurrency = input.baseCurrency,
            exchangeRates = input.exchangeRates,
            precision = input.precision
        )

        return CardRenderData.CashFlow(config, result, input.baseCurrency, input.precision)
    }
}
