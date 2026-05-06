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
        // Step 1: Create a combined filter that merges the screen context and card-specific filters.
        // This ensures that criteria like account selection are honored from both.
        val currentFilter = combineFilters(input.filter, config.filter)
        
        // Step 2: Compute current period values using the combined filter on ALL transactions.
        // We use input.transactions instead of pre-filtered baseTx to ensure we have enough
        // data for the previous period comparison.
        val currentMetrics = computeMetrics(input.transactions, currentFilter, input)

        // Step 3: Compute previous period values if timeline is not AllTime
        var previousMetrics: NetWorthMetrics? = null

        if (currentFilter.timelineRange != TimelineRange.AllTime) {
            val previousFilter = TransactionFilterService.getPreviousPeriodFilter(currentFilter)
            previousMetrics = computeMetrics(input.transactions, previousFilter, input)
        }

        return CardRenderData.NetWorth(
            config = config,
            currentMetrics = currentMetrics,
            baseCurrency = input.baseCurrency,
            precision = input.precision,
            previousMetrics = previousMetrics
        )
    }

    private fun combineFilters(
        base: com.osscabinet.ossWallet.domain.model.TransactionFilter,
        card: com.osscabinet.ossWallet.domain.model.TransactionFilter
    ): com.osscabinet.ossWallet.domain.model.TransactionFilter {
        // Timeline: Card overrides base if it's not AllTime, otherwise keep base
        val effectiveTimeline = if (card.timelineRange != TimelineRange.AllTime) {
            card.timelineRange
        } else {
            base.timelineRange
        }

        return base.copy(
            timelineRange = effectiveTimeline,
            startDate = card.startDate ?: base.startDate,
            endDate = card.endDate ?: base.endDate,
            // For lists, we intersect if both are present to honor the narrowest scope
            accountIds = intersectLists(base.accountIds, card.accountIds),
            categoryIds = intersectLists(base.categoryIds, card.categoryIds),
            currencies = intersectLists(base.currencies, card.currencies),
            tags = intersectLists(base.tags, card.tags),
            // For single values, card overrides if specified
            transactionType = card.transactionType ?: base.transactionType,
            minAmount = card.minAmount ?: base.minAmount,
            maxAmount = card.maxAmount ?: base.maxAmount,
            excludeFuture = card.excludeFuture && base.excludeFuture
        )
    }

    private fun <T> intersectLists(base: List<T>, card: List<T>): List<T> {
        return when {
            base.isEmpty() -> card
            card.isEmpty() -> base
            else -> base.intersect(card.toSet()).toList()
        }
    }

    private fun computeMetrics(
        transactions: List<com.osscabinet.ossWallet.domain.model.Transaction>,
        filter: com.osscabinet.ossWallet.domain.model.TransactionFilter,
        input: CardInput
    ): NetWorthMetrics {
        val filteredTx = TransactionFilterService.filter(transactions, filter)
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
