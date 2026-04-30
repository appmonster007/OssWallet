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
        // Use either config's timelineRange or input's global timelineRange
        val timelineRange = config.timelineRange
        val filter = TransactionFilter(timelineRange = timelineRange)
        val filteredTx = TransactionFilterService.filter(input.transactions, filter)
        
        val accountBreakdowns = AggregationService.breakdownByAccount(
            transactions = filteredTx,
            accounts = input.accounts,
            exchangeRates = input.exchangeRates,
            baseCurrency = input.baseCurrency
        )
        val netWorth = accountBreakdowns.sumOf { it.totalAmount }

        val breakdown = AggregationService.calculateIncomeExpenseBreakdown(
            transactions = filteredTx,
            exchangeRates = input.exchangeRates,
            baseCurrency = input.baseCurrency
        )
        
        val cashFlowBreakdown = (breakdown.incomeCurrencyBreakdown.keys + breakdown.expenseCurrencyBreakdown.keys)
            .distinct().sorted().map { curr ->
                val inc = breakdown.incomeCurrencyBreakdown[curr] ?: FinancialSummary.EMPTY
                val exp = breakdown.expenseCurrencyBreakdown[curr] ?: FinancialSummary.EMPTY
                CashFlowBreakdownItem(
                    currency = curr,
                    income = inc.totalAmount,
                    incomeInBase = CurrencyConversionService.convertToBaseCurrency(inc.totalAmount, curr, input.baseCurrency, input.exchangeRates),
                    expense = exp.totalAmount,
                    expenseInBase = CurrencyConversionService.convertToBaseCurrency(exp.totalAmount, curr, input.baseCurrency, input.exchangeRates)
                )
            }
            
        val cashFlowResult = CashFlowResult(
            totalIncomeInBase = breakdown.totalIncome.setScale(input.precision, RoundingMode.HALF_UP),
            totalExpensesInBase = breakdown.totalExpense.setScale(input.precision, RoundingMode.HALF_UP),
            netInBase = breakdown.netFlow.setScale(input.precision, RoundingMode.HALF_UP),
            breakdown = cashFlowBreakdown,
            filteredTransactions = filteredTx
        )
        
        return CardRenderData.NetWorth(config, netWorth, cashFlowResult, input.baseCurrency, input.precision)
    }
}
