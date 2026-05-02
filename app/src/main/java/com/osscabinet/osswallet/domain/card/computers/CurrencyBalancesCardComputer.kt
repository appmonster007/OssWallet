package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.analytics.*
import com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig
import com.osscabinet.ossWallet.domain.service.AggregationService
import com.osscabinet.ossWallet.domain.service.TransactionFilterService

class CurrencyBalancesCardComputer : CardDataComputer<CurrencyBalancesCardConfig, CardRenderData.CurrencyBalances> {
    override val supportedType = CardType.CURRENCY_BALANCES

    override fun compute(input: CardInput, config: CurrencyBalancesCardConfig): CardRenderData.CurrencyBalances {
        // Apply base scope
        val baseTx = TransactionFilterService.filter(input.transactions, input.filter)
        
        // Further filter by config accountIds if specified (ignoring timeline for balance).
        // Explicitly exclude futures since input.filter no longer does so.
        val now = System.currentTimeMillis()
        val targetAccountIds = config.filter.accountIds
        val balanceTransactions = if (targetAccountIds.isEmpty()) {
            baseTx.filter { it.timestamp <= now }
        } else {
            baseTx.filter { it.timestamp <= now && targetAccountIds.contains(it.accountId) }
        }

        val items = AggregationService.calculateCurrencyBalances(
            transactions = balanceTransactions,
            baseCurrency = input.baseCurrency,
            exchangeRates = input.exchangeRates
        )
        
        return CardRenderData.CurrencyBalances(config, items, input.baseCurrency, input.precision)
    }
}
