package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.analytics.*
import com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig
import com.osscabinet.ossWallet.domain.service.*
import java.math.BigDecimal
import java.math.RoundingMode

class NetWorthCardComputer : CardDataComputer<NetWorthCardConfig, CardRenderData.NetWorth> {
    override val supportedType = CardType.NET_WORTH

    override fun compute(input: CardInput, config: NetWorthCardConfig): CardRenderData.NetWorth {
        // Filter transactions for the period (used for Flow/Delta calculation)
        val filteredTx = TransactionFilterService.filter(input.transactions, config.filter)
        
        // Calculate LIFETIME net worth for the relevant accounts
        // We filter accounts based on the card config, but use ALL their transactions for the total
        val targetAccountIds = config.filter.accountIds
        val balanceTransactions = if (targetAccountIds.isEmpty()) {
            input.transactions
        } else {
            input.transactions.filter { targetAccountIds.contains(it.accountId) }
        }

        val totalsByCurrency = AggregationService.calculateTotalByCurrency(balanceTransactions)
        val netWorth = CurrencyService.calculateTotalInCurrency(
            currencyTotals = totalsByCurrency,
            targetCurrency = input.baseCurrency,
            exchangeRates = input.exchangeRates,
            precision = 10
        )

        // Calculate flow for the SELECTED period only
        val cashFlowResult = AggregationService.calculateCashFlowResult(
            transactions = filteredTx,
            baseCurrency = input.baseCurrency,
            exchangeRates = input.exchangeRates,
            precision = input.precision
        )
        
        return CardRenderData.NetWorth(config, netWorth, cashFlowResult, input.baseCurrency, input.precision)
    }
}
