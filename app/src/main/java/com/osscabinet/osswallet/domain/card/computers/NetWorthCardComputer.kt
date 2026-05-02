package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.analytics.*
import com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig
import com.osscabinet.ossWallet.domain.service.*

class NetWorthCardComputer : CardDataComputer<NetWorthCardConfig, CardRenderData.NetWorth> {
    override val supportedType = CardType.NET_WORTH

    override fun compute(input: CardInput, config: NetWorthCardConfig): CardRenderData.NetWorth {
        // Step 1: Filter
        val baseTx = TransactionFilterService.filter(input.transactions, input.filter)
        val filteredTx = TransactionFilterService.filter(baseTx, config.filter)

        // Step 2: Compute
        val totalsByCurrency = AggregationService.calculateTotalByCurrency(filteredTx)
        val netWorth = CurrencyService.calculateTotalInCurrency(
            currencyTotals = totalsByCurrency,
            targetCurrency = input.baseCurrency,
            exchangeRates = input.exchangeRates,
            precision = 10
        )
        val cashFlowResult = AggregationService.calculateCashFlowResult(
            transactions = filteredTx,
            baseCurrency = input.baseCurrency,
            exchangeRates = input.exchangeRates,
            precision = input.precision
        )

        // Step 3: Build render data
        return CardRenderData.NetWorth(config, netWorth, cashFlowResult, input.baseCurrency, input.precision)
    }
}
