package com.osscabinet.ossWallet.domain.calculator

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.analytics.*
import com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig
import com.osscabinet.ossWallet.domain.service.AggregationService
import com.osscabinet.ossWallet.domain.service.CurrencyConversionService
import com.osscabinet.ossWallet.domain.service.TransactionFilterService

class CurrencyBalancesCalculator : CardCalculator<CurrencyBalancesCardConfig, CardRenderData.CurrencyBalances> {
    override fun compute(
        config: CurrencyBalancesCardConfig,
        accounts: List<Account>,
        transactions: List<Transaction>,
        categories: List<Category>,
        context: ComputationContext
    ): CardRenderData.CurrencyBalances {
        val filteredTx = TransactionFilterService.filter(transactions, config.filter)
        val currencySummaries = AggregationService.calculateSummaryByCurrency(filteredTx)
        
        val items = currencySummaries.map { (curr, summary) ->
            CurrencyAggregationItem(
                currency = curr,
                amount = summary.totalAmount,
                amountInBase = CurrencyConversionService.convertToBaseCurrency(summary.totalAmount, curr, context.baseCurrency, context.exchangeRates)
            )
        }.sortedByDescending { it.amountInBase.abs() }
        
        return CardRenderData.CurrencyBalances(items)
    }
}
