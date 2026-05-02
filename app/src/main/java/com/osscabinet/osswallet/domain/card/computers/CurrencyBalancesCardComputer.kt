package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.analytics.*
import com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig
import com.osscabinet.ossWallet.domain.service.AggregationService
import com.osscabinet.ossWallet.domain.service.TransactionFilterService

class CurrencyBalancesCardComputer : CardDataComputer<CurrencyBalancesCardConfig, CardRenderData.CurrencyBalances> {
    override val supportedType = CardType.CURRENCY_BALANCES

    override fun compute(input: CardInput, config: CurrencyBalancesCardConfig): CardRenderData.CurrencyBalances {
        // Step 1: Filter
        val baseTx = TransactionFilterService.filter(input.transactions, input.filter)
        val filteredTx = TransactionFilterService.filter(baseTx, config.filter)

        // Step 2: Compute
        val items = AggregationService.calculateCurrencyBalances(
            transactions = filteredTx,
            baseCurrency = input.baseCurrency,
            exchangeRates = input.exchangeRates
        )

        // Step 3: Build render data
        return CardRenderData.CurrencyBalances(config, items, input.baseCurrency, input.precision)
    }
}
