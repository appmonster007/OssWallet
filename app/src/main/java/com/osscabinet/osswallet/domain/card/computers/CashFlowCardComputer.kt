package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.analytics.*
import com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig
import com.osscabinet.ossWallet.domain.service.*
import java.math.RoundingMode

class CashFlowCardComputer : CardDataComputer<CashFlowCardConfig, CardRenderData.CashFlow> {
    override val supportedType = CardType.CASH_FLOW

    override fun compute(input: CardInput, config: CashFlowCardConfig): CardRenderData.CashFlow {
        val filtered = TransactionFilterService.filter(input.transactions, config.filter)
        val breakdown = AggregationService.calculateIncomeExpenseBreakdown(
            transactions = filtered,
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
            
        val result = CashFlowResult(
            totalIncomeInBase = breakdown.totalIncome.setScale(input.precision, RoundingMode.HALF_UP),
            totalExpensesInBase = breakdown.totalExpense.setScale(input.precision, RoundingMode.HALF_UP),
            netInBase = breakdown.netFlow.setScale(input.precision, RoundingMode.HALF_UP),
            breakdown = cashFlowBreakdown,
            filteredTransactions = filtered
        )
        
        return CardRenderData.CashFlow(config, result, input.baseCurrency, input.precision)
    }
}
