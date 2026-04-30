package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.analytics.*
import com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig
import com.osscabinet.ossWallet.domain.service.AggregationService
import com.osscabinet.ossWallet.domain.service.CurrencyConversionService
import com.osscabinet.ossWallet.domain.service.TransactionFilterService

class CurrencyBalancesCardComputer : CardDataComputer<CurrencyBalancesCardConfig, CardRenderData.CurrencyBalances> {
    override val supportedType = CardType.CURRENCY_BALANCES

    override fun compute(input: CardInput, config: CurrencyBalancesCardConfig): CardRenderData.CurrencyBalances {
        val filteredTx = TransactionFilterService.filter(input.transactions, config.filter)
        val currencySummaries = AggregationService.calculateSummaryByCurrency(filteredTx)
        
        val items = currencySummaries.map { (curr, summary) ->
            CurrencyAggregationItem(
                currency = curr,
                amount = summary.totalAmount,
                amountInBase = CurrencyConversionService.convertToBaseCurrency(summary.totalAmount, curr, input.baseCurrency, input.exchangeRates)
            )
        }.sortedByDescending { it.amountInBase.abs() }
        
        return CardRenderData.CurrencyBalances(config, items, input.baseCurrency, input.precision)
    }
}
