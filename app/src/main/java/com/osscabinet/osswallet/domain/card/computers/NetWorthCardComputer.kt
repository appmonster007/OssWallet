package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.CardDataComputer
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.card.NetWorthMetrics
import com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.aggregation.AggregationService
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.filter.TransactionFilterService
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import java.math.BigDecimal

class NetWorthCardComputer : CardDataComputer<NetWorthCardConfig, CardRenderData.NetWorth> {
    override val supportedType = CardType.NET_WORTH

    override fun compute(input: CardInput, config: NetWorthCardConfig): CardRenderData.NetWorth {
        // Step 1: Apply base scope filter (screen-level: profile, account group, etc.)
        val baseTx = TransactionFilterService.filter(input.transactions, input.filter)

        // Step 2: Compute current period values
        val currentMetrics = computeMetrics(baseTx, config.filter, input)

        // Step 3: Compute previous period values if timeline is not AllTime
        var previousMetrics: NetWorthMetrics? = null

        if (config.filter.timelineRange != TimelineRange.AllTime) {
            val (prevStart, prevEnd) = TimelineService.getPreviousRange(config.filter.timelineRange)
            if (prevStart != null && prevEnd != null) {
                val prevFilter = config.filter.copy(
                    timelineRange = TimelineRange.CustomRange(prevStart, prevEnd),
                    startDate = null, // Clear explicit dates to use range
                    endDate = null
                )
                previousMetrics = computeMetrics(baseTx, prevFilter, input)
            }
        }

        return CardRenderData.NetWorth(
            config = config,
            currentMetrics = currentMetrics,
            baseCurrency = input.baseCurrency,
            precision = input.precision,
            previousMetrics = previousMetrics
        )
    }

    private fun computeMetrics(
        baseTransactions: List<com.osscabinet.ossWallet.domain.model.Transaction>,
        filter: com.osscabinet.ossWallet.domain.model.TransactionFilter,
        input: CardInput
    ): NetWorthMetrics {
        val filteredTx = TransactionFilterService.filter(baseTransactions, filter)
        val totalsByCurrency = AggregationService.calculateTotalByCurrency(filteredTx)
        val netWorth = CurrencyService.calculateTotalInCurrency(
            currencyTotals = totalsByCurrency,
            targetCurrency = input.baseCurrency,
            exchangeRates = input.exchangeRates,
            precision = input.precision
        )
        val cashFlow = AggregationService.calculateCashFlowResult(
            transactions = filteredTx,
            baseCurrency = input.baseCurrency,
            exchangeRates = input.exchangeRates,
            precision = input.precision
        )
        return NetWorthMetrics(netWorth, cashFlow)
    }
}
