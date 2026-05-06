package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.CardDataComputer
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig
import com.osscabinet.ossWallet.domain.service.aggregation.AggregationService
import com.osscabinet.ossWallet.domain.service.filter.TransactionFilterService

class CurrencyBalancesCardComputer :
    CardDataComputer<CurrencyBalancesCardConfig, CardRenderData.CurrencyBalances> {
    override val supportedType = CardType.CURRENCY_BALANCES

    override fun compute(
        input: CardInput,
        config: CurrencyBalancesCardConfig
    ): CardRenderData.CurrencyBalances {
        // Step 1: Apply base scope filter (screen-level: profile, account group, etc.)
        val baseTx = TransactionFilterService.filter(input.transactions, input.filter)

        // Step 2: Apply card config filter (user-selected: timeline, accounts, categories)
        val filteredTx = TransactionFilterService.filter(baseTx, config.filter)

        // Step 3: Compute all values from filteredTx
        val items = AggregationService.calculateCurrencyBalances(
            transactions = filteredTx,
            baseCurrency = input.baseCurrency,
            exchangeRates = input.exchangeRates,
            precision = input.precision
        )

        return CardRenderData.CurrencyBalances(config, items, input.baseCurrency, input.precision)
    }
}
