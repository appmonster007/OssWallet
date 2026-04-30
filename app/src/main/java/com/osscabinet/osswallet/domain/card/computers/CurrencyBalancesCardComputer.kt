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
        // "Currency Balances" should usually reflect the current total holdings.
        // If accounts are filtered in the card config, we only sum those accounts, 
        // but we use ALL their transactions to get the correct current balance.
        val targetAccountIds = config.filter.accountIds
        val balanceTransactions = if (targetAccountIds.isEmpty()) {
            input.transactions
        } else {
            input.transactions.filter { targetAccountIds.contains(it.accountId) }
        }

        val items = AggregationService.calculateCurrencyBalances(
            transactions = balanceTransactions,
            baseCurrency = input.baseCurrency,
            exchangeRates = input.exchangeRates
        )
        
        return CardRenderData.CurrencyBalances(config, items, input.baseCurrency, input.precision)
    }
}
