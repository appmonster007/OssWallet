package com.osscabinet.ossWallet.domain.calculator

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.analytics.*
import com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig
import com.osscabinet.ossWallet.domain.service.AggregationService
import com.osscabinet.ossWallet.domain.service.CurrencyConversionService
import com.osscabinet.ossWallet.domain.service.FinancialSummary
import com.osscabinet.ossWallet.domain.service.TransactionFilterService
import java.math.RoundingMode

class CashFlowCalculator : CardCalculator<CashFlowCardConfig, CardRenderData.CashFlow> {
    override fun compute(
        config: CashFlowCardConfig,
        accounts: List<Account>,
        transactions: List<Transaction>,
        categories: List<Category>,
        context: ComputationContext
    ): CardRenderData.CashFlow {
        val filtered = TransactionFilterService.filter(transactions, config.filter)
        val breakdown = AggregationService.calculateIncomeExpenseBreakdown(
            transactions = filtered,
            exchangeRates = context.exchangeRates,
            baseCurrency = context.baseCurrency
        )
        
        val cashFlowBreakdown = (breakdown.incomeCurrencyBreakdown.keys + breakdown.expenseCurrencyBreakdown.keys)
            .distinct().sorted().map { curr ->
                val inc = breakdown.incomeCurrencyBreakdown[curr] ?: FinancialSummary.EMPTY
                val exp = breakdown.expenseCurrencyBreakdown[curr] ?: FinancialSummary.EMPTY
                CashFlowBreakdownItem(
                    currency = curr,
                    income = inc.totalAmount,
                    incomeInBase = CurrencyConversionService.convertToBaseCurrency(inc.totalAmount, curr, context.baseCurrency, context.exchangeRates),
                    expense = exp.totalAmount,
                    expenseInBase = CurrencyConversionService.convertToBaseCurrency(exp.totalAmount, curr, context.baseCurrency, context.exchangeRates)
                )
            }
            
        val result = CashFlowResult(
            totalIncomeInBase = breakdown.totalIncome.setScale(context.precision, RoundingMode.HALF_UP),
            totalExpensesInBase = breakdown.totalExpense.setScale(context.precision, RoundingMode.HALF_UP),
            netInBase = breakdown.netFlow.setScale(context.precision, RoundingMode.HALF_UP),
            breakdown = cashFlowBreakdown,
            filteredTransactions = filtered
        )
        
        return CardRenderData.CashFlow(result)
    }
}
