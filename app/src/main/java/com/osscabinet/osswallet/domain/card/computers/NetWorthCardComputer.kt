package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.analytics.*
import com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig
import com.osscabinet.ossWallet.domain.service.*
import java.math.BigDecimal

class NetWorthCardComputer : CardDataComputer<NetWorthCardConfig, CardRenderData.NetWorth> {
    override val supportedType = CardType.NET_WORTH

    override fun compute(input: CardInput, config: NetWorthCardConfig): CardRenderData.NetWorth {
        // Step 1: Apply BASE scope filter (e.g. fixed to an account or profile)
        val baseTx = TransactionFilterService.filter(input.transactions, input.filter)
        
        // Step 2: Apply CARD user filter (e.g. timeline, categories)
        val filteredTx = TransactionFilterService.filter(baseTx, config.filter)
        
        // Calculate LIFETIME net worth for the relevant accounts within the BASE scope
        // We further filter by config accountIds if specified, but ignore config timeline
        val targetAccountIds = config.filter.accountIds
        val balanceTransactions = if (targetAccountIds.isEmpty()) {
            baseTx
        } else {
            baseTx.filter { targetAccountIds.contains(it.accountId) }
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
